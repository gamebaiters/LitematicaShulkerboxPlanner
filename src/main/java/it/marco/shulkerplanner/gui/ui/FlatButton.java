package it.marco.shulkerplanner.gui.ui;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;

/**
 * A flat button in the planner style. Variants for the main action, normal actions, quiet actions,
 * toggles (on/off with a check mark) and dangerous actions.
 */
public class FlatButton extends ButtonGeneric
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

    public FlatButton(int x, int y, int width, String text, Style style, String... hover)
    {
        super(x, y, width < 0 ? autoWidth(text, false, null) : width, HEIGHT, text, hover);
        this.style = style;
    }

    public static int autoWidth(String text, boolean toggle, @Nullable ItemStack icon)
    {
        return Ui.width(text) + 14 + (toggle ? 10 : 0) + (icon != null ? 14 : 0);
    }

    public static FlatButton toggle(String text, boolean on, String... hover)
    {
        FlatButton b = new FlatButton(0, 0, autoWidth(text, true, null), text, Style.SECONDARY, hover);
        b.toggle = on;
        return b;
    }

    public FlatButton setIcon(@Nullable ItemStack icon)
    {
        boolean hadIcon = this.icon != null;
        this.icon = icon;

        if (hadIcon == false && icon != null)
        {
            this.setWidth(this.getWidth() + 14);
        }

        return this;
    }

    public FlatButton setActive(boolean active)
    {
        this.active = active;
        return this;
    }

    public FlatButton setStyle(Style style)
    {
        this.style = style;
        return this;
    }

    public FlatButton setTextColor(int color)
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
        int x = this.x;
        int y = this.y;
        int w = this.width;
        int h = this.height;
        int bg;
        int border = 0;
        int fg = Theme.TEXT;

        switch (this.style)
        {
            case PRIMARY -> {
                bg = hover ? Theme.ACCENT_HOVER : Theme.ACCENT;
                fg = 0xFFFFFFFF;
            }
            case DANGER -> {
                bg = hover ? Theme.withAlpha(Theme.DANGER, 0x60) : Theme.DANGER_BG;
                border = Theme.withAlpha(Theme.DANGER, 0xA0);
                fg = Theme.DANGER;
            }
            case GHOST -> {
                bg = hover ? Theme.SURFACE_HOVER : 0;
                fg = hover ? Theme.TEXT : Theme.TEXT_DIM;
            }
            case TAB -> {
                bg = this.active ? Theme.ACCENT_BG : (hover ? Theme.SURFACE_HOVER : 0);
                fg = this.active ? Theme.TEXT : (hover ? Theme.TEXT : Theme.TEXT_DIM);
            }
            default -> {
                bg = this.active ? Theme.ACCENT_BG : (hover ? Theme.SURFACE_HOVER : Theme.SURFACE_3);
                border = this.active ? Theme.ACCENT : (hover ? Theme.BORDER_STRONG : Theme.BORDER);
            }
        }

        if (this.toggle != null && this.toggle && this.style == Style.SECONDARY)
        {
            border = Theme.withAlpha(Theme.ACCENT, 0xC0);
        }

        if (this.enabled == false)
        {
            bg = this.style == Style.GHOST || this.style == Style.TAB ? 0 : Theme.withAlpha(Theme.SURFACE_2, 0xA0);
            border = this.style == Style.GHOST || this.style == Style.TAB ? 0 : Theme.BORDER;
            fg = Theme.TEXT_MUTED;
        }

        if (border != 0)
        {
            Ui.panel(ctx, x, y, w, h, bg, border);
        }
        else if (bg != 0)
        {
            Ui.panel(ctx, x, y, w, h, bg);
        }

        if (this.style == Style.TAB && this.active)
        {
            Ui.rect(ctx, x, y + 2, 2, h - 4, Theme.ACCENT);
        }

        if (this.textColorOverride != 0 && this.enabled)
        {
            fg = this.textColorOverride;
        }

        int contentWidth = Ui.width(this.displayString) + (this.icon != null ? 14 : 0) + (this.toggle != null ? 10 : 0);
        int cx = this.style == Style.TAB ? x + 6 : x + (w - contentWidth) / 2;
        int ty = y + (h - 8) / 2;

        if (this.toggle != null)
        {
            boolean on = this.toggle;
            int bx = cx;
            int by = y + (h - 7) / 2;
            Ui.rect(ctx, bx, by, 7, 7, this.enabled ? (on ? Theme.ACCENT : Theme.BORDER_STRONG) : Theme.BORDER);
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
            Ui.item(ctx, this.icon, cx, y + (h - 12) / 2, 12, false);
            cx += 14;
        }

        Ui.text(ctx, this.displayString, cx, ty, fg);
    }
}
