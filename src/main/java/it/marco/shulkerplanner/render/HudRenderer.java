package it.marco.shulkerplanner.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.Reference;
import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.config.HudCountFormat;
import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.data.Reminder;
import it.marco.shulkerplanner.hud.HudColor;
import it.marco.shulkerplanner.hud.HudElement;
import it.marco.shulkerplanner.hud.HudStyle;
import it.marco.shulkerplanner.plan.BoxProgress;
import it.marco.shulkerplanner.plan.PlannedBox;
import it.marco.shulkerplanner.plan.ShulkerPlan;
import it.marco.shulkerplanner.util.Format;

/**
 * The in-game HUD. Every project has its own {@link HudStyle}: title, colors, visible elements,
 * size, compact layout and position. Everything is rebuilt every frame from the session,
 * which follows the inventory every tick.
 */
public class HudRenderer implements IRenderer
{
    public static final HudRenderer INSTANCE = new HudRenderer();

    private static final int MAX_TEXT_WIDTH = 250;

    /** Bounds of the last drawn HUD, in screen coordinates (scale included) */
    private int lastX;
    private int lastY;
    private int lastWidth;
    private int lastHeight;
    /** Width of the last drawn HUD before scaling */
    private int lastBaseWidth;

    @Override
    public Supplier<String> getProfilerSectionSupplier()
    {
        return () -> Reference.MOD_ID + "_hud";
    }

    @Override
    public void onExtractGuiOverlayPost(GuiContext ctx, float partialTicks, ProfilerFiller profiler)
    {
        if (Configs.Generic.HUD_ENABLED.getBooleanValue() == false ||
            ctx.mc().player == null ||
            GuiUtils.getCurrentScreen() != null ||
            ctx.mc().gui.hud.isHidden() ||
            ctx.mc().debugEntries.isOverlayVisible())
        {
            return;
        }

        this.render(ctx);
    }

    public int getLastX()
    {
        return this.lastX;
    }

    public int getLastY()
    {
        return this.lastY;
    }

    public int getLastWidth()
    {
        return this.lastWidth;
    }

    public int getLastHeight()
    {
        return this.lastHeight;
    }

    public int getLastBaseWidth()
    {
        return this.lastBaseWidth;
    }

    /**
     * @return true if something was drawn
     */
    public boolean render(GuiContext ctx)
    {
        PlannerSession session = PlannerSession.INSTANCE;
        PlannerProject project = session.getProject();
        HudStyle style = session.getHudStyle();
        Layout lay = new Layout(style);
        List<Element> elements = new ArrayList<>();

        if (session.getUnplannedSelection() != null)
        {
            // The selected placement has no plan yet: say how to make one, instead of showing another schematic
            elements.add(Line.of(lay, Seg.of(Format.trim(session.getUnplannedSelection(), MAX_TEXT_WIDTH), HudColor.TITLE, true)));
            elements.add(Line.of(lay, Seg.of(StringUtils.translate("shulkerplanner.hud.no_plan"), HudColor.LABEL)));
            return this.draw(ctx, style, lay, elements);
        }

        if (project == null || session.isLoadedInGame() == false)
        {
            this.lastWidth = 0;
            this.lastHeight = 0;
            return false;
        }

        this.build(elements, lay, style, project, session);

        if (elements.isEmpty())
        {
            this.lastWidth = 0;
            this.lastHeight = 0;
            return false;
        }

        return this.draw(ctx, style, lay, elements);
    }

    private void build(List<Element> elements, Layout lay, HudStyle style, PlannerProject project, PlannerSession session)
    {
        ShulkerPlan plan = session.getPlan();

        if (style.isShown(HudElement.TITLE))
        {
            elements.add(Line.of(lay, Seg.of(Format.trim(session.getDisplayName(), MAX_TEXT_WIDTH), HudColor.TITLE, true)));
        }

        if (style.isShown(HudElement.PROGRESS))
        {
            this.addProgress(elements, lay, project, session, plan);
        }

        this.addCurrentBox(elements, lay, style, session, plan);

        if (style.isShown(HudElement.REMINDERS))
        {
            this.addReminders(elements, lay, style, project);
        }
    }

    private boolean draw(GuiContext ctx, HudStyle style, Layout lay, List<Element> elements)
    {
        int width = 0;
        int height = 0;

        for (Element element : elements)
        {
            width = Math.max(width, element.width());
            height += element.height();
        }

        int bar = style.hasAccentBar() ? 2 : 0;
        int pad = style.getPadding();
        int baseWidth = width + pad * 2 + bar;
        int baseHeight = height + pad * 2 - (lay.compact ? 1 : 2);
        float scale = style.getScale();
        int screenWidth = Math.round(baseWidth * scale);
        int screenHeight = Math.round(baseHeight * scale);

        // Keep the HUD on the screen when the window gets smaller
        int x = Math.max(0, Math.min(style.getX(), ctx.guiWidth() - screenWidth));
        int y = Math.max(0, Math.min(style.getY(), ctx.guiHeight() - screenHeight));

        this.lastX = x;
        this.lastY = y;
        this.lastWidth = screenWidth;
        this.lastHeight = screenHeight;
        this.lastBaseWidth = baseWidth;

        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        ctx.pose().scale(scale, scale);

        RenderUtils.drawRect(ctx, 0, 0, baseWidth, baseHeight, style.getColor(HudColor.BACKGROUND));

        if (bar > 0)
        {
            RenderUtils.drawRect(ctx, 0, 0, bar, baseHeight, style.getColor(HudColor.ACCENT));
        }

        if (style.hasBorder())
        {
            RenderUtils.drawOutline(ctx, 0, 0, baseWidth, baseHeight, style.getColor(HudColor.BORDER));
        }

        int ly = pad;

        for (Element element : elements)
        {
            element.draw(ctx, style, bar + pad, ly);
            ly += element.height();
        }

        ctx.pose().popMatrix();

        return true;
    }

    private void addProgress(List<Element> elements, Layout lay, PlannerProject project, PlannerSession session, ShulkerPlan plan)
    {
        int ready = session.getReadyBoxCount();
        int count = plan.getBoxCount();
        int included = 0;
        int gathered = 0;

        for (MaterialRow row : project.getMaterials())
        {
            if (project.isExcluded(row.getId()) == false)
            {
                ++included;

                if (project.isGathered(row.getId()))
                {
                    ++gathered;
                }
            }
        }

        elements.add(Line.of(lay,
                Seg.of(StringUtils.translate("shulkerplanner.hud.progress_boxes") + " ", HudColor.LABEL),
                Seg.of(ready + "/" + count, ready >= count ? HudColor.GOOD : HudColor.WARN),
                Seg.of("  " + StringUtils.translate("shulkerplanner.hud.progress_materials") + " ", HudColor.LABEL),
                Seg.of(gathered + "/" + included, gathered >= included ? HudColor.GOOD : HudColor.WARN)));
    }

    private void addCurrentBox(List<Element> elements, Layout lay, HudStyle style, PlannerSession session, ShulkerPlan plan)
    {
        if (plan.getBoxes().isEmpty())
        {
            return;
        }

        boolean anyBoxElement = style.isShown(HudElement.BOX_HEADER) || style.isShown(HudElement.BOX_TOTALS) ||
                                style.isShown(HudElement.BOX_GRID) || style.isShown(HudElement.BOX_ITEMS) ||
                                style.isShown(HudElement.WRONG_ITEMS) || style.isShown(HudElement.BOX_STATUS);

        if (anyBoxElement == false)
        {
            return;
        }

        BoxProgress progress = session.getCurrentProgress();

        if (progress == null)
        {
            if (style.isShown(HudElement.BOX_HEADER) || style.isShown(HudElement.BOX_STATUS))
            {
                elements.add(Line.of(lay, Seg.of(StringUtils.translate("shulkerplanner.hud.all_boxes_ready"), HudColor.GOOD)));
            }

            return;
        }

        PlannedBox box = progress.getBox();
        HudCountFormat format = style.getCountFormat();

        if (style.isShown(HudElement.BOX_HEADER))
        {
            String type = box.isPure() ? StringUtils.translate("shulkerplanner.gui.label.box.pure")
                                       : StringUtils.translate("shulkerplanner.gui.label.box.mixed", box.getTotals().size());
            List<Seg> segs = new ArrayList<>();
            segs.add(Seg.of(StringUtils.translate("shulkerplanner.hud.current_box", box.getIndex(), plan.getBoxCount()), HudColor.INFO));
            segs.add(Seg.of("  " + type, HudColor.LABEL));

            if (progress.isContentsKnown())
            {
                segs.add(Seg.of("  ● " + StringUtils.translate("shulkerplanner.hud.live"), HudColor.GOOD));
            }

            elements.add(new Line(lay, null, segs));
        }

        if (style.isShown(HudElement.BOX_TOTALS))
        {
            elements.add(Line.of(lay,
                    Seg.of(StringUtils.translate("shulkerplanner.hud.totals_inside") + " ", HudColor.LABEL),
                    Seg.of(Format.number(progress.getInBox()) + "/" + Format.number(progress.getTotal()), HudColor.GOOD),
                    Seg.of("  " + StringUtils.translate("shulkerplanner.hud.totals_inventory") + " ", HudColor.LABEL),
                    Seg.of(Format.number(progress.getInInventory()), HudColor.INFO),
                    Seg.of("  " + StringUtils.translate("shulkerplanner.hud.totals_missing") + " ", HudColor.LABEL),
                    Seg.of(Format.number(progress.getMissing()), progress.getMissing() > 0 ? HudColor.BAD : HudColor.GOOD)));
        }

        if (style.isShown(HudElement.BOX_GRID))
        {
            elements.add(new Grid(lay, progress));
        }

        if (style.isShown(HudElement.GRID_LEGEND))
        {
            elements.add(Line.of(lay,
                    Seg.of("■", HudColor.GOOD), Seg.of(StringUtils.translate("shulkerplanner.hud.legend.in_box") + "  ", HudColor.LABEL),
                    Seg.of("■", HudColor.INFO), Seg.of(StringUtils.translate("shulkerplanner.hud.legend.in_inventory") + "  ", HudColor.LABEL),
                    Seg.of("■", HudColor.WARN), Seg.of(StringUtils.translate("shulkerplanner.hud.legend.partial") + "  ", HudColor.LABEL),
                    Seg.of("■", HudColor.BAD), Seg.of(StringUtils.translate("shulkerplanner.hud.legend.missing"), HudColor.LABEL)));
        }

        if (style.isShown(HudElement.BOX_ITEMS))
        {
            int maxLines = style.getMaxBoxItems();
            int shown = 0;

            for (BoxProgress.Item item : progress.getItems())
            {
                if (shown >= maxLines)
                {
                    break;
                }

                elements.add(new Line(lay, item.item().proto(), this.formatItem(item, format)));
                ++shown;
            }

            if (progress.getItems().size() > shown)
            {
                elements.add(Line.of(lay, Seg.of("  " + StringUtils.translate("shulkerplanner.gui.label.box.more", progress.getItems().size() - shown), HudColor.LABEL)));
            }
        }

        if (style.isShown(HudElement.WRONG_ITEMS))
        {
            for (Map.Entry<String, Integer> e : progress.getExtra().entrySet())
            {
                ItemStack stack = new MaterialRow(e.getKey(), 0, 0).getStack();
                elements.add(new Line(lay, stack, List.of(Seg.of(StringUtils.translate("shulkerplanner.hud.remove",
                                                                                        Format.number(e.getValue()), stack.getHoverName().getString()), HudColor.BAD))));
            }
        }

        if (style.isShown(HudElement.BOX_STATUS))
        {
            if (progress.getMissing() == 0 && progress.getInBox() < progress.getTotal())
            {
                elements.add(Line.of(lay, Seg.of(StringUtils.translate("shulkerplanner.hud.box_items_ready"), HudColor.GOOD)));
            }
            else if (progress.getMissing() > 0)
            {
                elements.add(Line.of(lay, Seg.of(StringUtils.translate("shulkerplanner.hud.box_items_missing",
                                                                       Format.number(progress.getMissing()), progress.getMissingTypes()), HudColor.WARN)));
            }
        }
    }

    private List<Seg> formatItem(BoxProgress.Item item, HudCountFormat format)
    {
        int maxStack = item.item().maxStack();
        String name = Format.trim(item.item().proto().getHoverName().getString(), 110);
        List<Seg> segs = new ArrayList<>();

        switch (item.state())
        {
            case IN_BOX -> segs.add(Seg.of("✔ " + name, HudColor.GOOD));
            case IN_INVENTORY -> {
                segs.add(Seg.of(name, HudColor.INFO));
                segs.add(Seg.of("  " + StringUtils.translate("shulkerplanner.hud.put_in_label") + " ", HudColor.LABEL));
                segs.add(Seg.of(format.format(item.toPutIn(), maxStack), HudColor.INFO));
            }
            case PARTIAL -> {
                segs.add(Seg.of(name, HudColor.WARN));
                segs.add(Seg.of("  " + StringUtils.translate("shulkerplanner.hud.missing_label") + " ", HudColor.LABEL));
                segs.add(Seg.of(format.format(item.missing(), maxStack), HudColor.BAD));
                segs.add(Seg.of(" (" + StringUtils.translate("shulkerplanner.hud.have_label") + " ", HudColor.LABEL));
                segs.add(Seg.of(format.format(item.inBox() + item.inInventory(), maxStack), HudColor.INFO));
                segs.add(Seg.of(")", HudColor.LABEL));
            }
            case MISSING -> {
                segs.add(Seg.of(name, HudColor.BAD));
                segs.add(Seg.of("  " + StringUtils.translate("shulkerplanner.hud.missing_label") + " ", HudColor.LABEL));
                segs.add(Seg.of(format.format(item.missing(), maxStack), HudColor.BAD));
            }
        }

        return segs;
    }

    private void addReminders(List<Element> elements, Layout lay, HudStyle style, PlannerProject project)
    {
        int max = style.getMaxReminders();
        List<Reminder> open = new ArrayList<>();

        for (Reminder reminder : project.getReminders())
        {
            if (reminder.isDone() == false)
            {
                open.add(reminder);
            }
        }

        if (open.isEmpty())
        {
            if (project.getReminders().isEmpty() == false)
            {
                elements.add(Line.of(lay, Seg.of(StringUtils.translate("shulkerplanner.hud.all_done"), HudColor.GOOD)));
            }

            return;
        }

        elements.add(Line.of(lay, Seg.of(StringUtils.translate("shulkerplanner.hud.reminders", open.size()), HudColor.WARN)));

        for (int i = 0; i < open.size() && i < max; ++i)
        {
            Reminder reminder = open.get(i);
            Seg text = Seg.of("○ " + Format.trim(reminder.getText(), MAX_TEXT_WIDTH - 12), HudColor.TEXT);
            ItemStack icon = reminder.getItemId() != null ? new MaterialRow(reminder.getItemId(), 0, 0).getStack() : null;
            elements.add(new Line(lay, icon, List.of(text)));
        }

        if (open.size() > max)
        {
            elements.add(Line.of(lay, Seg.of("  " + StringUtils.translate("shulkerplanner.hud.more", open.size() - max), HudColor.LABEL)));
        }
    }

    public static int getStateColor(HudStyle style, BoxProgress.State state)
    {
        return switch (state)
        {
            case IN_BOX -> style.getColor(HudColor.GOOD);
            case IN_INVENTORY -> style.getColor(HudColor.INFO);
            case PARTIAL -> style.getColor(HudColor.WARN);
            case MISSING -> style.getColor(HudColor.BAD);
        };
    }

    private static void drawItem(GuiContext ctx, ItemStack stack, int x, int y, float size, boolean decorations)
    {
        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        ctx.pose().scale(size / 16f, size / 16f);
        ctx.renderItem(stack, 0, 0);

        if (decorations)
        {
            ctx.renderItemDecorations(ctx.fontRenderer(), stack, 0, 0);
        }

        ctx.pose().popMatrix();
    }

    // ---------------------------------------------------------------- layout and elements

    /**
     * Sizes for the normal and the compact layout
     */
    private static class Layout
    {
        final boolean compact;
        final int textLine;
        final int itemLine;
        final int iconSize;
        final int slot;
        final int gridPad;

        Layout(HudStyle style)
        {
            this.compact = style.isCompact();
            this.textLine = this.compact ? 9 : 10;
            this.itemLine = this.compact ? 10 : 17;
            this.iconSize = this.compact ? 9 : 16;
            this.slot = this.compact ? 12 : 18;
            this.gridPad = this.compact ? 2 : 4;
        }
    }

    private interface Element
    {
        int width();

        int height();

        void draw(GuiContext ctx, HudStyle style, int x, int y);
    }

    private record Seg(String text, HudColor color, boolean bold)
    {
        static Seg of(String text, HudColor color)
        {
            return new Seg(text, color, false);
        }

        static Seg of(String text, HudColor color, boolean bold)
        {
            return new Seg(text, color, bold);
        }

        String rendered()
        {
            return this.bold ? "§l" + this.text : this.text;
        }
    }

    private record Line(Layout lay, @Nullable ItemStack icon, List<Seg> segs) implements Element
    {
        static Line of(Layout lay, Seg... segs)
        {
            return new Line(lay, null, List.of(segs));
        }

        private int textWidth()
        {
            int w = 0;

            for (Seg s : this.segs)
            {
                w += StringUtils.getStringWidth(s.rendered());
            }

            return w;
        }

        @Override
        public int width()
        {
            return this.textWidth() + (this.icon != null ? this.lay.iconSize + 2 : 0);
        }

        @Override
        public int height()
        {
            return this.icon != null ? this.lay.itemLine : this.lay.textLine;
        }

        @Override
        public void draw(GuiContext ctx, HudStyle style, int x, int y)
        {
            Font font = ctx.fontRenderer();
            int tx = x;
            int ty = y;

            if (this.icon != null)
            {
                drawItem(ctx, this.icon, x, y, this.lay.iconSize, false);
                tx += this.lay.iconSize + 2;
                ty += (this.lay.itemLine - 8) / 2;
            }

            for (Seg s : this.segs)
            {
                String str = s.rendered();
                ctx.drawString(font, str, tx, ty, style.getColor(s.color()), style.hasTextShadow());
                tx += font.width(str);
            }
        }
    }

    /**
     * The box drawn like the vanilla shulker box screen, every slot tinted by its state
     */
    private record Grid(Layout lay, BoxProgress progress) implements Element
    {
        private int gridWidth()
        {
            return 9 * this.lay.slot + this.lay.gridPad * 2;
        }

        private int gridHeight()
        {
            return 3 * this.lay.slot + this.lay.gridPad * 2;
        }

        @Override
        public int width()
        {
            return this.gridWidth();
        }

        @Override
        public int height()
        {
            return this.gridHeight() + (this.lay.compact ? 2 : 3);
        }

        @Override
        public void draw(GuiContext ctx, HudStyle style, int x, int y)
        {
            int w = this.gridWidth();
            int h = this.gridHeight();
            int slot = this.lay.slot;
            int gy = y + 1;

            RenderUtils.drawRect(ctx, x, gy, w, h, 0xFFC6C6C6);
            RenderUtils.drawRect(ctx, x, gy, w, 1, 0xFFFFFFFF);
            RenderUtils.drawRect(ctx, x, gy, 1, h, 0xFFFFFFFF);
            RenderUtils.drawRect(ctx, x, gy + h - 1, w, 1, 0xFF555555);
            RenderUtils.drawRect(ctx, x + w - 1, gy, 1, h, 0xFF555555);

            List<BoxProgress.Slot> slots = this.progress.getSlots();

            for (int i = 0; i < PlannedBox.SLOTS; ++i)
            {
                int sx = x + this.lay.gridPad + (i % 9) * slot;
                int sy = gy + this.lay.gridPad + (i / 9) * slot;

                RenderUtils.drawRect(ctx, sx, sy, slot, slot, 0xFF8B8B8B);
                RenderUtils.drawRect(ctx, sx, sy, slot - 1, 1, 0xFF373737);
                RenderUtils.drawRect(ctx, sx, sy, 1, slot - 1, 0xFF373737);
                RenderUtils.drawRect(ctx, sx + 1, sy + slot - 1, slot - 1, 1, 0xFFFFFFFF);
                RenderUtils.drawRect(ctx, sx + slot - 1, sy + 1, 1, slot - 1, 0xFFFFFFFF);

                if (i < slots.size())
                {
                    BoxProgress.Slot s = slots.get(i);
                    int color = getStateColor(style, s.state());
                    RenderUtils.drawRect(ctx, sx + 1, sy + 1, slot - 2, slot - 2, (color & 0x00FFFFFF) | 0xC0000000);
                    drawItem(ctx, s.stack().createStack(), sx + 1, sy + 1, slot - 2, this.lay.compact == false);
                }
            }
        }
    }
}
