package it.marco.shulkerplanner.gui;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.input.ScanCodes;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.render.HudRenderer;

/**
 * Shows the HUD over the game and lets the player drag it to a new position.
 */
public class GuiHudEditor extends Screen
{
    private static final int DEFAULT_X = 4;
    private static final int DEFAULT_Y = 40;

    @Nullable private final Screen parent;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private Button viewButton;
    private Button countButton;
    private Button hudButton;

    public GuiHudEditor(@Nullable Screen parent)
    {
        super(Component.literal("Shulker Planner HUD"));
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        int gap = 4;
        int w = Math.min(130, (this.width - 20 - gap * 4) / 5);
        int total = w * 5 + gap * 4;
        int x = (this.width - total) / 2;
        int y = this.height - 28;

        this.viewButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
            Configs.Generic.HUD_BOX_VIEW.setOptionListValue(Configs.Generic.getHudBoxView().cycle(true));
            this.updateLabels();
        }).bounds(x, y, w, 20).build());
        x += w + gap;

        this.countButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
            Configs.Generic.HUD_COUNT_FORMAT.setOptionListValue(Configs.Generic.getHudCountFormat().cycle(true));
            this.updateLabels();
        }).bounds(x, y, w, 20).build());
        x += w + gap;

        this.hudButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
            Configs.Generic.HUD_ENABLED.setBooleanValue(! Configs.Generic.HUD_ENABLED.getBooleanValue());
            this.updateLabels();
        }).bounds(x, y, w, 20).build());
        x += w + gap;

        this.addRenderableWidget(Button.builder(Component.literal(StringUtils.translate("shulkerplanner.hud_editor.reset")), b -> {
            Configs.Generic.HUD_X.setIntegerValue(DEFAULT_X);
            Configs.Generic.HUD_Y.setIntegerValue(DEFAULT_Y);
        }).bounds(x, y, w, 20).build());
        x += w + gap;

        this.addRenderableWidget(Button.builder(Component.literal(StringUtils.translate("shulkerplanner.hud_editor.done")), b -> this.onClose())
                                       .bounds(x, y, w, 20).build());

        this.updateLabels();
    }

    private void updateLabels()
    {
        this.viewButton.setMessage(Component.literal(StringUtils.translate("shulkerplanner.hud_editor.view", Configs.Generic.getHudBoxView().getDisplayName())));
        this.countButton.setMessage(Component.literal(StringUtils.translate("shulkerplanner.hud_editor.counts", Configs.Generic.getHudCountFormat().getDisplayName())));
        String on = StringUtils.translate(Configs.Generic.HUD_ENABLED.getBooleanValue() ? "malilib.gui.label_colored.on" : "malilib.gui.label_colored.off");
        this.hudButton.setMessage(Component.literal(StringUtils.translate("shulkerplanner.gui.button.hud", on)));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        // The world stays visible, only slightly darker
        graphics.fill(0, 0, this.width, this.height, 0x40000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        GuiContext ctx = GuiContext.fromGuiGraphics(graphics);
        HudRenderer hud = HudRenderer.INSTANCE;

        if (hud.render(ctx))
        {
            boolean hover = this.dragging || this.isOverHud(mouseX, mouseY);
            graphics.outline(hud.getLastX() - 1, hud.getLastY() - 1, hud.getLastWidth() + 2, hud.getLastHeight() + 2,
                             hover ? 0xFFFFFFFF : 0xFFA060E0);
        }
        else
        {
            graphics.centeredText(this.font, GuiBase.TXT_GOLD + StringUtils.translate("shulkerplanner.hud_editor.no_project"),
                                  this.width / 2, this.height / 2, 0xFFFFFFFF);
        }

        graphics.nextStratum();
        graphics.centeredText(this.font, StringUtils.translate("shulkerplanner.hud_editor.hint"), this.width / 2, this.height - 42, 0xFFFFFFFF);

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private boolean isOverHud(double mouseX, double mouseY)
    {
        HudRenderer hud = HudRenderer.INSTANCE;
        return hud.getLastWidth() > 0 &&
               mouseX >= hud.getLastX() && mouseX < hud.getLastX() + hud.getLastWidth() &&
               mouseY >= hud.getLastY() && mouseY < hud.getLastY() + hud.getLastHeight();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (super.mouseClicked(event, doubleClick))
        {
            return true;
        }

        if (event.button() == ScanCodes.OFFSET_MOUSE_LEFT && this.isOverHud(event.x(), event.y()))
        {
            HudRenderer hud = HudRenderer.INSTANCE;
            this.dragging = true;
            this.dragOffsetX = (int) event.x() - hud.getLastX();
            this.dragOffsetY = (int) event.y() - hud.getLastY();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy)
    {
        if (this.dragging)
        {
            HudRenderer hud = HudRenderer.INSTANCE;
            int x = Math.max(0, Math.min((int) event.x() - this.dragOffsetX, this.width - hud.getLastWidth()));
            int y = Math.max(0, Math.min((int) event.y() - this.dragOffsetY, this.height - hud.getLastHeight()));
            Configs.Generic.HUD_X.setIntegerValue(x);
            Configs.Generic.HUD_Y.setIntegerValue(y);
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event)
    {
        if (this.dragging)
        {
            this.dragging = false;
            Configs.saveToFile();
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public void onClose()
    {
        Configs.saveToFile();
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
