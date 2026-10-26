package it.marco.shulkerplanner.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.input.ScanCodes;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.hud.HudColor;
import it.marco.shulkerplanner.hud.HudElement;
import it.marco.shulkerplanner.hud.HudPreset;
import it.marco.shulkerplanner.hud.HudStyle;
import it.marco.shulkerplanner.hud.HudStyles;
import it.marco.shulkerplanner.hud.HudTheme;
import it.marco.shulkerplanner.render.HudRenderer;

/**
 * Customizes the HUD of the active project: drag it to move it, drag the corner (or scroll on it) to resize it,
 * and change the title, theme, every color, every element and the layout in the side panel.
 */
public class GuiHudEditor extends Screen
{
    private static final int PANEL_WIDTH = 214;
    private static final int ROW = 22;
    private static final int HANDLE = 7;
    private static final int[] PALETTE = {
            0xFFFFFFFF, 0xFFF9801D, 0xFFC74EBD, 0xFF3AB3DA, 0xFFFED83D, 0xFF80C71F, 0xFFF38BAA, 0xFF474F52,
            0xFF9D9D97, 0xFF169C9C, 0xFF8932B8, 0xFF3C44AA, 0xFF835432, 0xFF5E7C16, 0xFFB02E26, 0xFF1D1D21,
            0xFFFF77FF, 0xFF55FFFF, 0xFF55FF55, 0xFFFFAA00, 0xFFFF5555, 0xFF000000 };

    private enum Page { LOOK, ELEMENTS, LAYOUT }

    private static Page page = Page.LOOK;

    @Nullable private final Screen parent;
    private final List<Label> labels = new ArrayList<>();
    private final List<Swatch> swatches = new ArrayList<>();
    private int panelX;
    private int panelScroll;
    private int contentHeight;
    private boolean dragging;
    private boolean resizing;
    private int dragOffsetX;
    private int dragOffsetY;
    private float resizeStartScale;
    private int resizeStartWidth;
    private int resizeStartMouseX;
    @Nullable private Button themeButton;

    public GuiHudEditor(@Nullable Screen parent)
    {
        super(Component.literal("Shulker Planner HUD"));
        this.parent = parent;
    }

    private HudStyle style()
    {
        return PlannerSession.INSTANCE.getHudStyle();
    }

    private static String tr(String key, Object... args)
    {
        return StringUtils.translate("shulkerplanner.hud_editor." + key, args);
    }

    private static String onOff(boolean value)
    {
        return StringUtils.translate(value ? "malilib.gui.label_colored.on" : "malilib.gui.label_colored.off");
    }

    // ---------------------------------------------------------------- widgets

    @Override
    protected void init()
    {
        this.labels.clear();
        this.swatches.clear();

        HudRenderer hud = HudRenderer.INSTANCE;
        boolean hudOnLeft = hud.getLastWidth() == 0 || hud.getLastX() + hud.getLastWidth() / 2 < this.width / 2;
        this.panelX = hudOnLeft ? this.width - PANEL_WIDTH - 6 : 6;

        int x = this.panelX;
        int tabWidth = (PANEL_WIDTH - 8) / 3;

        for (Page p : Page.values())
        {
            Button tab = Button.builder(Component.literal(tr("page." + p.name().toLowerCase())), b -> {
                page = p;
                this.panelScroll = 0;
                this.rebuildWidgets();
            }).bounds(x + 2 + p.ordinal() * (tabWidth + 2), 6, tabWidth, 20).build();
            tab.active = page != p;
            this.addRenderableWidget(tab);
        }

        // Rows of the current page, scrolled inside the panel
        List<RowBuilder> rows = switch (page)
        {
            case LOOK -> this.lookRows();
            case ELEMENTS -> this.elementRows();
            case LAYOUT -> this.layoutRows();
        };

        int top = 32;
        int bottom = this.height - 32;
        this.contentHeight = rows.size() * ROW;
        this.panelScroll = Math.max(0, Math.min(this.panelScroll, Math.max(0, this.contentHeight - (bottom - top))));
        int y = top - this.panelScroll;

        for (RowBuilder row : rows)
        {
            if (y >= top && y + 20 <= bottom)
            {
                row.build(x + 4, y);
            }

            y += ROW;
        }

        this.addRenderableWidget(Button.builder(Component.literal(tr("done")), b -> this.onClose())
                                       .bounds(x + 4, this.height - 26, PANEL_WIDTH - 8, 20).build());
    }

    @FunctionalInterface
    private interface RowBuilder
    {
        void build(int x, int y);
    }

    private List<RowBuilder> lookRows()
    {
        List<RowBuilder> rows = new ArrayList<>();
        PlannerProject project = PlannerSession.INSTANCE.getProject();
        String schematic = project != null ? project.getSchematicName() : "";

        rows.add((x, y) -> {
            this.labels.add(new Label(tr("title"), x, y + 6));
            EditBox box = new EditBox(this.font, x + 70, y + 1, PANEL_WIDTH - 98, 18, Component.literal("title"));
            box.setMaxLength(64);
            box.setValue(this.style().getTitle());
            box.setHint(Component.literal(schematic));
            box.setResponder(s -> this.style().setTitle(s));
            this.addRenderableWidget(box);
            this.addRenderableWidget(Button.builder(Component.literal("↺"), b -> {
                this.style().setTitle("");
                this.rebuildWidgets();
            }).bounds(x + PANEL_WIDTH - 26, y, 20, 20).build());
        });

        rows.add((x, y) -> {
            this.labels.add(new Label(tr("theme"), x, y + 6));
            this.themeButton = this.addRenderableWidget(Button.builder(Component.literal(this.style().getTheme().getDisplayName()), b -> {
                this.style().applyTheme(this.style().getTheme().cycle(fi.dy.masa.malilib.gui.GuiBase.isShiftDown() == false));
                this.rebuildWidgets();
            }).bounds(x + 70, y, PANEL_WIDTH - 78, 20).build());
        });

        rows.add((x, y) -> this.addRenderableWidget(new Slider(x, y, PANEL_WIDTH - 8,
                this.style().getBackgroundOpacity() / 100.0,
                v -> tr("opacity", Math.round(v * 100)),
                v -> this.style().setBackgroundOpacity((int) Math.round(v * 100)))));

        for (HudColor role : HudColor.values())
        {
            rows.add((x, y) -> {
                this.labels.add(new Label(role.getDisplayName(), x, y + 6));
                this.swatches.add(new Swatch(role, x + 104, y + 3));
                int argb = this.style().getColor(role);
                EditBox box = new EditBox(this.font, x + 124, y + 1, PANEL_WIDTH - 132, 18, Component.literal(role.getName()));
                box.setMaxLength(9);
                box.setValue(role.hasAlpha() ? String.format("#%08X", argb) : String.format("#%06X", argb & 0xFFFFFF));
                box.setResponder(s -> {
                    Integer parsed = HudStyle.parseColor(s);

                    if (parsed != null && parsed != this.style().getColor(role))
                    {
                        this.style().setColor(role, parsed);

                        if (this.themeButton != null)
                        {
                            this.themeButton.setMessage(Component.literal(this.style().getTheme().getDisplayName()));
                        }
                    }
                });
                this.addRenderableWidget(box);
            });
        }

        return rows;
    }

    private List<RowBuilder> elementRows()
    {
        List<RowBuilder> rows = new ArrayList<>();

        rows.add((x, y) -> {
            HudPreset preset = HudPreset.match(this.style().getElements());
            String name = preset != null ? preset.getDisplayName() : tr("preset_custom");
            this.addRenderableWidget(Button.builder(Component.literal(tr("preset", name)), b -> {
                HudPreset current = HudPreset.match(this.style().getElements());
                this.style().applyPreset(current != null ? current.cycle(fi.dy.masa.malilib.gui.GuiBase.isShiftDown() == false) : HudPreset.FULL);
                this.rebuildWidgets();
            }).bounds(x, y, PANEL_WIDTH - 8, 20).build());
        });

        for (HudElement element : HudElement.values())
        {
            rows.add((x, y) -> {
                boolean shown = this.style().isShown(element);
                String label = (shown ? "§a✔ " : "§7✘ ") + element.getDisplayName();
                this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    this.style().setShown(element, this.style().isShown(element) == false);
                    this.rebuildWidgets();
                }).bounds(x, y, PANEL_WIDTH - 8, 20).build());
            });
        }

        return rows;
    }

    private List<RowBuilder> layoutRows()
    {
        List<RowBuilder> rows = new ArrayList<>();
        int w = PANEL_WIDTH - 8;

        rows.add((x, y) -> this.addRenderableWidget(new Slider(x, y, w,
                (this.style().getScale() - HudStyle.MIN_SCALE) / (HudStyle.MAX_SCALE - HudStyle.MIN_SCALE),
                v -> tr("scale", Math.round((HudStyle.MIN_SCALE + v * (HudStyle.MAX_SCALE - HudStyle.MIN_SCALE)) * 100)),
                v -> this.style().setScale((float) (HudStyle.MIN_SCALE + v * (HudStyle.MAX_SCALE - HudStyle.MIN_SCALE))))));

        rows.add((x, y) -> this.addToggle(x, y, w, tr("compact", onOff(this.style().isCompact())),
                () -> this.style().setCompact(this.style().isCompact() == false)));

        rows.add((x, y) -> this.addRenderableWidget(new Slider(x, y, w, this.style().getPadding() / 12.0,
                v -> tr("padding", Math.round(v * 12)),
                v -> this.style().setPadding((int) Math.round(v * 12)))));

        rows.add((x, y) -> this.addToggle(x, y, w, tr("shadow", onOff(this.style().hasTextShadow())),
                () -> this.style().setTextShadow(this.style().hasTextShadow() == false)));

        rows.add((x, y) -> this.addToggle(x, y, w, tr("accent_bar", onOff(this.style().hasAccentBar())),
                () -> this.style().setAccentBar(this.style().hasAccentBar() == false)));

        rows.add((x, y) -> this.addToggle(x, y, w, tr("border", onOff(this.style().hasBorder())),
                () -> this.style().setBorder(this.style().hasBorder() == false)));

        rows.add((x, y) -> this.addRenderableWidget(new Slider(x, y, w, (this.style().getMaxBoxItems() - 1) / 26.0,
                v -> tr("max_box_items", 1 + Math.round(v * 26)),
                v -> this.style().setMaxBoxItems(1 + (int) Math.round(v * 26)))));

        rows.add((x, y) -> this.addRenderableWidget(new Slider(x, y, w, (this.style().getMaxReminders() - 1) / 29.0,
                v -> tr("max_reminders", 1 + Math.round(v * 29)),
                v -> this.style().setMaxReminders(1 + (int) Math.round(v * 29)))));

        rows.add((x, y) -> this.addToggle(x, y, w, tr("counts", this.style().getCountFormat().getDisplayName()),
                () -> this.style().setCountFormat(this.style().getCountFormat().cycle(true))));

        rows.add((x, y) -> this.addToggle(x, y, w, StringUtils.translate("shulkerplanner.gui.button.hud", onOff(Configs.Generic.HUD_ENABLED.getBooleanValue())),
                () -> Configs.Generic.HUD_ENABLED.setBooleanValue(Configs.Generic.HUD_ENABLED.getBooleanValue() == false)));

        rows.add((x, y) -> this.addToggle(x, y, w, tr("reset_position"), () -> this.style().setPosition(4, 40)));

        rows.add((x, y) -> this.addToggle(x, y, w, tr("save_default"), () -> HudStyles.setDefault(this.style())));

        rows.add((x, y) -> this.addToggle(x, y, w, tr("load_default"), () -> {
            PlannerProject project = PlannerSession.INSTANCE.getProject();

            if (project != null)
            {
                project.setHudStyle(HudStyles.createForProject());
            }
        }));

        return rows;
    }

    private void addToggle(int x, int y, int width, String label, Runnable action)
    {
        this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
            action.run();
            this.rebuildWidgets();
        }).bounds(x, y, width, 20).build());
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        // The world stays visible, only slightly darker
        graphics.fill(0, 0, this.width, this.height, 0x30000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        GuiContext ctx = GuiContext.fromGuiGraphics(graphics);
        HudRenderer hud = HudRenderer.INSTANCE;

        if (hud.render(ctx))
        {
            int hx = hud.getLastX();
            int hy = hud.getLastY();
            int hw = hud.getLastWidth();
            int hh = hud.getLastHeight();
            boolean hover = this.dragging || this.resizing || this.isOverHud(mouseX, mouseY);
            graphics.nextStratum();
            graphics.outline(hx - 1, hy - 1, hw + 2, hh + 2, hover ? 0xFFFFFFFF : 0xA0FFFFFF);
            graphics.fill(hx + hw - HANDLE + 1, hy + hh - HANDLE + 1, hx + hw + 1, hy + hh + 1, this.isOverHandle(mouseX, mouseY) || this.resizing ? 0xFFFFFFFF : 0xFFA060E0);
        }
        else
        {
            graphics.centeredText(this.font, "§6" + tr("no_project"), this.width / 2, this.height / 2, 0xFFFFFFFF);
        }

        graphics.nextStratum();

        // Side panel
        graphics.fill(this.panelX, 0, this.panelX + PANEL_WIDTH, this.height, 0xE0101018);
        graphics.fill(this.panelX + (this.panelX < this.width / 2 ? PANEL_WIDTH - 1 : 0), 0,
                      this.panelX + (this.panelX < this.width / 2 ? PANEL_WIDTH : 1), this.height, 0xFFA060E0);

        for (Label label : this.labels)
        {
            graphics.text(this.font, label.text(), label.x(), label.y(), 0xFFE0E0E0, false);
        }

        for (Swatch swatch : this.swatches)
        {
            int argb = this.style().getColor(swatch.role());
            graphics.fill(swatch.x() - 1, swatch.y() - 1, swatch.x() + 15, swatch.y() + 15, 0xFF000000);
            // Checkerboard under transparent colors
            graphics.fill(swatch.x(), swatch.y(), swatch.x() + 7, swatch.y() + 7, 0xFFFFFFFF);
            graphics.fill(swatch.x() + 7, swatch.y() + 7, swatch.x() + 14, swatch.y() + 14, 0xFFFFFFFF);
            graphics.fill(swatch.x() + 7, swatch.y(), swatch.x() + 14, swatch.y() + 7, 0xFFBBBBBB);
            graphics.fill(swatch.x(), swatch.y() + 7, swatch.x() + 7, swatch.y() + 14, 0xFFBBBBBB);
            graphics.fill(swatch.x(), swatch.y(), swatch.x() + 14, swatch.y() + 14, argb);
        }

        if (this.contentHeight > this.height - 64)
        {
            graphics.text(this.font, "§7" + tr("scroll"), this.panelX + 6, this.height - 40, 0xFFFFFFFF, false);
        }

        String hint = tr("hint");
        int hintX = this.panelX < this.width / 2 ? this.panelX + PANEL_WIDTH + (this.width - this.panelX - PANEL_WIDTH) / 2 : this.panelX / 2;
        graphics.centeredText(this.font, hint, hintX, 8, 0xFFFFFFFF);

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    // ---------------------------------------------------------------- mouse

    private boolean isOverHud(double mouseX, double mouseY)
    {
        HudRenderer hud = HudRenderer.INSTANCE;
        return hud.getLastWidth() > 0 &&
               mouseX >= hud.getLastX() && mouseX < hud.getLastX() + hud.getLastWidth() &&
               mouseY >= hud.getLastY() && mouseY < hud.getLastY() + hud.getLastHeight();
    }

    private boolean isOverHandle(double mouseX, double mouseY)
    {
        HudRenderer hud = HudRenderer.INSTANCE;
        int hx = hud.getLastX() + hud.getLastWidth();
        int hy = hud.getLastY() + hud.getLastHeight();
        return hud.getLastWidth() > 0 && mouseX >= hx - HANDLE - 1 && mouseX <= hx + 2 && mouseY >= hy - HANDLE - 1 && mouseY <= hy + 2;
    }

    private boolean isOverPanel(double mouseX)
    {
        return mouseX >= this.panelX && mouseX < this.panelX + PANEL_WIDTH;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (super.mouseClicked(event, doubleClick))
        {
            return true;
        }

        for (Swatch swatch : this.swatches)
        {
            if (event.x() >= swatch.x() && event.x() < swatch.x() + 14 && event.y() >= swatch.y() && event.y() < swatch.y() + 14)
            {
                this.cycleSwatch(swatch.role(), event.button() != ScanCodes.OFFSET_MOUSE_RIGHT);
                return true;
            }
        }

        if (this.isOverPanel(event.x()) || event.button() != ScanCodes.OFFSET_MOUSE_LEFT)
        {
            return false;
        }

        HudRenderer hud = HudRenderer.INSTANCE;

        if (this.isOverHandle(event.x(), event.y()))
        {
            this.resizing = true;
            this.resizeStartScale = this.style().getScale();
            this.resizeStartWidth = hud.getLastWidth();
            this.resizeStartMouseX = (int) event.x();
            return true;
        }

        if (this.isOverHud(event.x(), event.y()))
        {
            this.dragging = true;
            this.dragOffsetX = (int) event.x() - hud.getLastX();
            this.dragOffsetY = (int) event.y() - hud.getLastY();
            return true;
        }

        return false;
    }

    private void cycleSwatch(HudColor role, boolean forward)
    {
        int current = this.style().getColor(role) | 0xFF000000;
        int index = 0;

        for (int i = 0; i < PALETTE.length; ++i)
        {
            if (PALETTE[i] == current)
            {
                index = i;
                break;
            }
        }

        int next = PALETTE[Math.floorMod(index + (forward ? 1 : -1), PALETTE.length)];

        if (role.hasAlpha())
        {
            next = (this.style().getColor(role) & 0xFF000000) | (next & 0x00FFFFFF);
        }

        this.style().setColor(role, next);
        this.rebuildWidgets();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy)
    {
        HudRenderer hud = HudRenderer.INSTANCE;

        if (this.resizing && this.resizeStartWidth > 0)
        {
            int startRight = hud.getLastX() + this.resizeStartWidth;
            int newWidth = this.resizeStartWidth + ((int) event.x() - this.resizeStartMouseX);
            this.style().setScale(this.resizeStartScale * Math.max(10, newWidth) / this.resizeStartWidth);
            return startRight > 0;
        }

        if (this.dragging)
        {
            int x = Math.max(0, Math.min((int) event.x() - this.dragOffsetX, this.width - hud.getLastWidth()));
            int y = Math.max(0, Math.min((int) event.y() - this.dragOffsetY, this.height - hud.getLastHeight()));
            this.style().setPosition(x, y);
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event)
    {
        if (this.dragging || this.resizing)
        {
            this.dragging = false;
            this.resizing = false;
            this.save();
            // The panel moves to the other side if the HUD now sits under it
            this.rebuildWidgets();
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (this.isOverPanel(mouseX))
        {
            this.panelScroll -= (int) Math.signum(scrollY) * ROW;
            this.rebuildWidgets();
            return true;
        }

        if (this.isOverHud(mouseX, mouseY))
        {
            this.style().setScale(this.style().getScale() + (float) Math.signum(scrollY) * 0.05f);

            if (page == Page.LAYOUT)
            {
                this.rebuildWidgets();
            }

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ---------------------------------------------------------------- closing

    private void save()
    {
        if (PlannerSession.INSTANCE.getProject() != null)
        {
            PlannerSession.INSTANCE.save();
        }
        else
        {
            HudStyles.setDefault(this.style());
        }

        Configs.saveToFile();
    }

    @Override
    public void onClose()
    {
        this.save();
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    private record Label(String text, int x, int y) {}

    private record Swatch(HudColor role, int x, int y) {}

    /**
     * A slider with the label and the value mapping given as lambdas
     */
    private static class Slider extends AbstractSliderButton
    {
        private final DoubleFunction<String> label;
        private final DoubleConsumer apply;

        Slider(int x, int y, int width, double value, DoubleFunction<String> label, DoubleConsumer apply)
        {
            super(x, y, width, 20, Component.empty(), Math.max(0, Math.min(1, value)));
            this.label = label;
            this.apply = apply;
            this.updateMessage();
        }

        @Override
        protected void updateMessage()
        {
            this.setMessage(Component.literal(this.label.apply(this.value)));
        }

        @Override
        protected void applyValue()
        {
            this.apply.accept(this.value);
        }
    }
}
