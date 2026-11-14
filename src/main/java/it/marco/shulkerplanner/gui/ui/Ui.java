package it.marco.shulkerplanner.gui.ui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;

/**
 * Drawing helpers for the planner screens: rounded panels, progress bars, chips, item icons.
 */
public final class Ui
{
    private Ui()
    {
    }

    public static Font font()
    {
        return Minecraft.getInstance().font;
    }

    public static int width(String text)
    {
        return font().width(text);
    }

    public static void rect(GuiContext ctx, int x, int y, int w, int h, int color)
    {
        if (w > 0 && h > 0)
        {
            RenderUtils.drawRect(ctx, x, y, w, h, color);
        }
    }

    /**
     * A rectangle with 1 pixel cut corners, which reads as slightly rounded at Minecraft's scale
     */
    public static void panel(GuiContext ctx, int x, int y, int w, int h, int color)
    {
        if (w < 3 || h < 3)
        {
            rect(ctx, x, y, w, h, color);
            return;
        }

        rect(ctx, x + 1, y, w - 2, 1, color);
        rect(ctx, x, y + 1, w, h - 2, color);
        rect(ctx, x + 1, y + h - 1, w - 2, 1, color);
    }

    /**
     * A rounded panel with a 1 pixel border
     */
    public static void panel(GuiContext ctx, int x, int y, int w, int h, int color, int border)
    {
        panel(ctx, x, y, w, h, border);
        panel(ctx, x + 1, y + 1, w - 2, h - 2, color);
    }

    public static void hline(GuiContext ctx, int x, int y, int w, int color)
    {
        rect(ctx, x, y, w, 1, color);
    }

    public static void vline(GuiContext ctx, int x, int y, int h, int color)
    {
        rect(ctx, x, y, 1, h, color);
    }

    public static void text(GuiContext ctx, String text, int x, int y, int color)
    {
        ctx.drawString(font(), text, x, y, color, false);
    }

    public static void textShadow(GuiContext ctx, String text, int x, int y, int color)
    {
        ctx.drawString(font(), text, x, y, color, true);
    }

    public static void textCentered(GuiContext ctx, String text, int cx, int y, int color)
    {
        ctx.drawString(font(), text, cx - width(text) / 2, y, color, false);
    }

    public static void textRight(GuiContext ctx, String text, int right, int y, int color)
    {
        ctx.drawString(font(), text, right - width(text), y, color, false);
    }

    /**
     * Cuts the text to the given width, adding "..." when needed
     */
    public static String trim(String text, int maxWidth)
    {
        Font font = font();

        if (font.width(text) <= maxWidth)
        {
            return text;
        }

        String dots = "...";
        int dotsWidth = font.width(dots);

        if (maxWidth <= dotsWidth)
        {
            return "";
        }

        return font.plainSubstrByWidth(text, maxWidth - dotsWidth) + dots;
    }

    /**
     * A horizontal progress bar
     */
    public static void progress(GuiContext ctx, int x, int y, int w, int h, double fraction, int fill, int track)
    {
        double f = Math.max(0, Math.min(1, fraction));
        panel(ctx, x, y, w, h, track);
        int fw = (int) Math.round(w * f);

        if (fw > 0)
        {
            panel(ctx, x, y, Math.max(fw, Math.min(w, 3)), h, fill);
        }
    }

    /**
     * A stacked progress bar: several parts one after the other (inside, carrying, ...)
     */
    public static void progressStacked(GuiContext ctx, int x, int y, int w, int h, long total, long[] parts, int[] colors, int track)
    {
        panel(ctx, x, y, w, h, track);

        if (total <= 0)
        {
            return;
        }

        int px = x;

        for (int i = 0; i < parts.length; ++i)
        {
            int pw = (int) Math.round((double) w * Math.min(parts[i], total) / total);
            pw = Math.min(pw, x + w - px);

            if (pw > 0)
            {
                rect(ctx, px, y + (px == x ? 1 : 0), pw, h - (px == x ? 2 : 0), colors[i]);
                if (px == x)
                {
                    rect(ctx, px + 1, y, Math.max(0, pw - 1), h, colors[i]);
                }
                px += pw;
            }
        }
    }

    /**
     * A small label with a tinted background, like a tag: [ DONE ]
     * @return the width of the chip
     */
    public static int chip(GuiContext ctx, String text, int x, int y, int fg, int bg)
    {
        int w = width(text) + 8;
        panel(ctx, x, y, w, 11, bg);
        text(ctx, text, x + 4, y + 2, fg);
        return w;
    }

    public static int chipWidth(String text)
    {
        return width(text) + 8;
    }

    public static void item(GuiContext ctx, ItemStack stack, int x, int y)
    {
        ctx.renderItem(stack, x, y);
    }

    /**
     * An item drawn at another size than 16
     */
    public static void item(GuiContext ctx, ItemStack stack, int x, int y, float size, boolean decorations)
    {
        ctx.pose().pushMatrix();
        ctx.pose().translate(x, y);
        ctx.pose().scale(size / 16f, size / 16f);
        ctx.renderItem(stack, 0, 0);

        if (decorations)
        {
            ctx.renderItemDecorations(font(), stack, 0, 0);
        }

        ctx.pose().popMatrix();
    }

    /**
     * A slot like the inventory ones, flat
     */
    public static void slot(GuiContext ctx, int x, int y, int size, int tint)
    {
        rect(ctx, x, y, size, size, Theme.SLOT_BORDER);
        rect(ctx, x + 1, y + 1, size - 2, size - 2, Theme.SLOT_BG);

        if (tint != 0)
        {
            rect(ctx, x + 1, y + 1, size - 2, size - 2, tint);
        }
    }

    public static void tooltip(GuiContext ctx, int mouseX, int mouseY, List<String> lines)
    {
        RenderUtils.drawHoverText(ctx, mouseX, mouseY, lines);
    }

    /**
     * A 7x8 padlock
     */
    public static void lock(GuiContext ctx, int x, int y, int color)
    {
        rect(ctx, x + 2, y, 3, 1, color);
        rect(ctx, x + 1, y + 1, 1, 3, color);
        rect(ctx, x + 5, y + 1, 1, 3, color);
        rect(ctx, x, y + 3, 7, 5, color);
        rect(ctx, x + 3, y + 5, 1, 2, 0xFF000000);
    }

    /**
     * A chip with a padlock in front of the text
     */
    public static int lockChip(GuiContext ctx, String text, int x, int y, int fg, int bg)
    {
        int w = width(text) + 18;
        panel(ctx, x, y, w, 11, bg);
        lock(ctx, x + 4, y + 1, fg);
        text(ctx, text, x + 14, y + 2, fg);
        return w;
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h)
    {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
