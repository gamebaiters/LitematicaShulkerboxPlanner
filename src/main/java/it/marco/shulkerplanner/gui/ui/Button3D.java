package it.marco.shulkerplanner.gui.ui;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import fi.dy.masa.malilib.render.GuiContext;

/**
 * A vanilla button drawn in the planner's 3D style, for the screens that use vanilla widgets (the HUD editor).
 * A selected button (the current page) is drawn pressed in, and doesn't look disabled.
 */
public class Button3D extends Button
{
    /** A button with this label shows the reset icon */
    public static final String RESET = "↺";

    private Ui.Bevel bevel = Ui.BEVEL_SECONDARY;
    private boolean selected;

    protected Button3D(int x, int y, int width, int height, Component message, OnPress onPress)
    {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public static Builder3D builder3d(Component message, OnPress onPress)
    {
        return new Builder3D(message, onPress);
    }

    public Button3D setPrimary()
    {
        this.bevel = Ui.BEVEL_PRIMARY;
        return this;
    }

    public Button3D setSelected(boolean selected)
    {
        this.selected = selected;
        return this;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
    {
        GuiContext ctx = GuiContext.fromGuiGraphics(graphics);
        Ui.Bevel bevel = this.selected ? Ui.BEVEL_ACTIVE : this.bevel;
        int color = this.bevel == Ui.BEVEL_PRIMARY ? 0xFFFFFFFF : Theme.TEXT;

        if (this.active == false)
        {
            bevel = Ui.BEVEL_DISABLED;
            color = Theme.TEXT_MUTED;
        }
        else if (this.isHoveredOrFocused())
        {
            bevel = bevel.hovered();
        }

        String label = this.getMessage().getString();

        if (RESET.equals(label))
        {
            // The "↺" glyph is tiny in the Minecraft font: draw the icon instead
            LayoutProbe.add(LayoutProbe.Kind.BUTTON, label, this.getX(), this.getY(), this.getWidth(), this.getHeight());
            Ui.button3d(ctx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), bevel, this.selected);
            int ty = Ui.buttonTextY(this.getY(), this.getHeight(), this.selected);
            Ui.resetIcon(ctx, this.getX() + (this.getWidth() - 7) / 2, ty, color);
            return;
        }

        Ui.button3dLabel(ctx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Ui.trim(label, this.getWidth() - 6), bevel, this.selected, color);
    }

    public static class Builder3D
    {
        private final Component message;
        private final OnPress onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        @Nullable private Tooltip tooltip;

        private Builder3D(Component message, OnPress onPress)
        {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder3D bounds(int x, int y, int width, int height)
        {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder3D tooltip(@Nullable Tooltip tooltip)
        {
            this.tooltip = tooltip;
            return this;
        }

        public Button3D build()
        {
            Button3D button = new Button3D(this.x, this.y, this.width, this.height, this.message, this.onPress);
            button.setTooltip(this.tooltip);
            return button;
        }
    }
}
