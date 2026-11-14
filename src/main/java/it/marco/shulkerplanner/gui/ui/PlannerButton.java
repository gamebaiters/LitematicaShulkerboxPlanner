package it.marco.shulkerplanner.gui.ui;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;

/**
 * A 3D button in the planner style: raised with a lit top and a thick bottom edge, pressed in when it's
 * the selected tab or a toggle that is on. Variants for the main action, normal actions, quiet actions,
 * toggles (on/off with a check mark) and dangerous actions.
 */
public class PlannerButton extends ButtonGeneric
{
    public enum Style
    {
        PRIMARY,
        SECONDARY,
        GHOST,
        DANGER,
        TAB
    }

    public static final int HEIGHT = 18;

    private Style style;
    @Nullable private ItemStack icon;
    private boolean active;
    @Nullable private Boolean toggle;
    private int textColorOverride;

    public PlannerButton(int x, int y, int width, String text, Style style, String... hover)
    {
        super(x, y, width < 0 ? autoWidth(text, false, null) : width, HEIGHT, text, hover);
        this.style = style;
    }

    public static int autoWidth(String text, boolean toggle, @Nullable ItemStack icon)
    {
        return Ui.width(text) + 14 + (toggle ? 10 : 0) + (icon != null ? 14 : 0);
    }

    public static PlannerButton toggle(String text, boolean on, String... hover)
    {
        PlannerButton b = new PlannerButton(0, 0, autoWidth(text, true, null), text, Style.SECONDARY, hover);
        b.toggle = on;
        return b;
    }

    public PlannerButton setIcon(@Nullable ItemStack icon)
    {
        boolean hadIcon = this.icon != null;
        this.icon = icon;

        if (hadIcon == false && icon != null)
        {
            this.setWidth(this.getWidth() + 14);
        }

        return this;
    }

    public PlannerButton setActive(boolean active)
    {
        this.active = active;
        return this;
    }

    public PlannerButton setStyle(Style style)
    {
        this.style = style;
        return this;
    }

    public PlannerButton setTextColor(int color)
    {
        this.textColorOverride = color;
        return this;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        if (this.visible == false)
        {
            return;
        }

        this.hovered = this.isMouseOver(mouseX, mouseY);
        boolean hover = this.hovered && this.enabled;
        boolean on = this.toggle != null && this.toggle;
        boolean pressed = this.enabled && (this.active || on);
        int x = this.x;
        int y = this.y;
        int w = this.width;
        int h = this.height;
        Ui.Bevel bevel;
        int fg;

        switch (this.style)
        {
            case PRIMARY -> {
                bevel = Ui.BEVEL_PRIMARY;
                fg = 0xFFFFFFFF;
            }
            case DANGER -> {
                bevel = Ui.BEVEL_DANGER;
                fg = 0xFFFFE0E0;
            }
            case GHOST, TAB -> {
                bevel = Ui.BEVEL_QUIET;
                fg = hover ? Theme.TEXT : Theme.TEXT_DIM;
            }
            default -> {
                bevel = Ui.BEVEL_SECONDARY;
                fg = Theme.TEXT;
            }
        }

        if (pressed && this.style != Style.PRIMARY && this.style != Style.DANGER)
        {
            bevel = Ui.BEVEL_ACTIVE;
            fg = Theme.TEXT;
        }

        if (hover)
        {
            bevel = bevel.hovered();
        }

        if (this.enabled == false)
        {
            bevel = Ui.BEVEL_DISABLED;
            fg = Theme.TEXT_MUTED;
        }

        LayoutProbe.add(LayoutProbe.Kind.BUTTON, this.displayString, x, y, w, h);
        Ui.button3d(ctx, x, y, w, h, bevel, pressed);

        if (this.style == Style.TAB && this.active)
        {
            Ui.rect(ctx, x + 2, y + 3, 2, h - 5, Theme.ACCENT_HOVER);
        }

        if (this.textColorOverride != 0 && this.enabled)
        {
            fg = this.textColorOverride;
        }

        int contentWidth = Ui.width(this.displayString) + (this.icon != null ? 14 : 0) + (this.toggle != null ? 10 : 0);
        int cx = this.style == Style.TAB ? x + 7 : x + (w - contentWidth) / 2;
        int ty = Ui.buttonTextY(y, h, pressed);

        if (this.toggle != null)
        {
            int bx = cx;
            int by = ty;
            Ui.rect(ctx, bx, by, 7, 7, this.enabled ? (on ? Theme.ACCENT_HOVER : Theme.BORDER_STRONG) : Theme.BORDER);
            Ui.rect(ctx, bx + 1, by + 1, 5, 5, on ? (this.enabled ? Theme.ACCENT : Theme.ACCENT_DIM) : Theme.SLOT_BG);

            if (on)
            {
                // check mark
                Ui.rect(ctx, bx + 1, by + 3, 1, 2, 0xFFFFFFFF);
                Ui.rect(ctx, bx + 2, by + 4, 1, 2, 0xFFFFFFFF);
                Ui.rect(ctx, bx + 3, by + 3, 1, 2, 0xFFFFFFFF);
                Ui.rect(ctx, bx + 4, by + 2, 1, 2, 0xFFFFFFFF);
                Ui.rect(ctx, bx + 5, by + 1, 1, 2, 0xFFFFFFFF);
            }

            cx += 10;
        }

        if (this.icon != null)
        {
            Ui.item(ctx, this.icon, cx, ty - 2, 12, false);
            cx += 14;
        }

        if (this.displayString.isEmpty() == false)
        {
            Ui.textShadow(ctx, this.displayString, cx, ty, fg);
        }
    }
}
