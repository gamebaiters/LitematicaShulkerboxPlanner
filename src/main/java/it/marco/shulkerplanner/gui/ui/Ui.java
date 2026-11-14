package it.marco.shulkerplanner.gui.ui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;

import net.minecraft.client.gui.navigation.ScreenRectangle;

import fi.dy.masa.malilib.gui.GuiScrollBar;
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

    /**
     * A panel with a 1 pixel border, where only the inside gets the background color:
     * unlike {@link #panel(GuiContext, int, int, int, int, int, int)} it stays right with a translucent background
     */
    public static void framed(GuiContext ctx, int x, int y, int w, int h, int color, int border)
    {
        rect(ctx, x + 1, y, w - 2, 1, border);
        rect(ctx, x + 1, y + h - 1, w - 2, 1, border);
        rect(ctx, x, y + 1, 1, h - 2, border);
        rect(ctx, x + w - 1, y + 1, 1, h - 2, border);
        rect(ctx, x + 1, y + 1, w - 2, h - 2, color);
    }

    public static void hline(GuiContext ctx, int x, int y, int w, int color)
    {
        rect(ctx, x, y, w, 1, color);
    }

    public static void vline(GuiContext ctx, int x, int y, int h, int color)
    {
        rect(ctx, x, y, 1, h, color);
    }

    private static void drawText(GuiContext ctx, String text, int x, int y, int color, boolean shadow)
    {
        if (text.isEmpty())
        {
            return;
        }

        ctx.drawString(font(), text, x, y, color, shadow);

        if (LayoutProbe.isEnabled())
        {
            LayoutProbe.add(LayoutProbe.Kind.TEXT, text, x, y, width(text), 8);
        }
    }

    public static void text(GuiContext ctx, String text, int x, int y, int color)
    {
        drawText(ctx, text, x, y, color, false);
    }

    public static void textShadow(GuiContext ctx, String text, int x, int y, int color)
    {
        drawText(ctx, text, x, y, color, true);
    }

    public static void textCentered(GuiContext ctx, String text, int cx, int y, int color)
    {
        drawText(ctx, text, cx - width(text) / 2, y, color, false);
    }

    public static void textRight(GuiContext ctx, String text, int right, int y, int color)
    {
        drawText(ctx, text, right - width(text), y, color, false);
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
        LayoutProbe.add(LayoutProbe.Kind.BAR, null, x, y, w, h);
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
        LayoutProbe.add(LayoutProbe.Kind.BAR, null, x, y, w, h);
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
        LayoutProbe.add(LayoutProbe.Kind.CHIP, text, x, y, w, 11);
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
        LayoutProbe.add(LayoutProbe.Kind.ITEM, stack.getItem().toString(), x, y, 16, 16);
    }

    /**
     * An item drawn at another size than 16
     */
    public static void item(GuiContext ctx, ItemStack stack, int x, int y, float size, boolean decorations)
    {
        LayoutProbe.add(LayoutProbe.Kind.ITEM, stack.getItem().toString(), x, y, Math.round(size), Math.round(size));
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
        LayoutProbe.add(LayoutProbe.Kind.CHIP, text, x, y, w, 11);
        panel(ctx, x, y, w, 11, bg);
        lock(ctx, x + 4, y + 1, fg);
        text(ctx, text, x + 14, y + 2, fg);
        return w;
    }

    /**
     * A vertical gradient
     */
    public static void gradient(GuiContext ctx, int x, int y, int w, int h, int top, int bottom)
    {
        if (w > 0 && h > 0)
        {
            ctx.getGuiGraphics().fillGradient(x, y, x + w, y + h, top, bottom);
        }
    }

    /**
     * Only draws inside the given area until {@link #popClip}
     */
    public static void pushClip(GuiContext ctx, int x, int y, int w, int h)
    {
        // Both the malilib context (texts, items, rectangles) and the vanilla one it wraps (gradients)
        ctx.pushScissor(new ScreenRectangle(x, y, w, h));
        ctx.getGuiGraphics().enableScissor(x, y, x + w, y + h);
        LayoutProbe.pushClip(x, y, w, h);
    }

    public static void popClip(GuiContext ctx)
    {
        ctx.getGuiGraphics().disableScissor();
        ctx.popScissor();
        LayoutProbe.popClip();
    }

    /**
     * Gives a malilib list scroll bar the planner colors instead of plain white
     */
    public static void styleScrollbar(GuiScrollBar scrollBar)
    {
        try
        {
            java.lang.reflect.Field fg = GuiScrollBar.class.getDeclaredField("foregroundColor");
            java.lang.reflect.Field bg = GuiScrollBar.class.getDeclaredField("backgroundColor");
            fg.setAccessible(true);
            bg.setAccessible(true);
            fg.setInt(scrollBar, 0xFF7A56B0);
            bg.setInt(scrollBar, 0x60000000);
        }
        catch (ReflectiveOperationException ignore)
        {
            // Only the colors, the scroll bar works anyway
        }
    }

    /**
     * A 7x7 ring, with a dot in the middle when on: the "follow on the HUD" icon
     */
    public static void followIcon(GuiContext ctx, int x, int y, boolean on, int color)
    {
        rect(ctx, x + 2, y, 3, 1, color);
        rect(ctx, x + 2, y + 6, 3, 1, color);
        rect(ctx, x, y + 2, 1, 3, color);
        rect(ctx, x + 6, y + 2, 1, 3, color);
        rect(ctx, x + 1, y + 1, 1, 1, color);
        rect(ctx, x + 5, y + 1, 1, 1, color);
        rect(ctx, x + 1, y + 5, 1, 1, color);
        rect(ctx, x + 5, y + 5, 1, 1, color);

        if (on)
        {
            rect(ctx, x + 2, y + 2, 3, 3, color);
        }
    }

    /**
     * A 7x7 circular arrow, the "reset" icon
     */
    public static void resetIcon(GuiContext ctx, int x, int y, int color)
    {
        rect(ctx, x + 2, y, 3, 1, color);
        rect(ctx, x + 1, y + 1, 1, 1, color);
        rect(ctx, x, y + 2, 1, 3, color);
        rect(ctx, x + 1, y + 5, 1, 1, color);
        rect(ctx, x + 2, y + 6, 3, 1, color);
        rect(ctx, x + 5, y + 5, 1, 1, color);
        rect(ctx, x + 6, y + 3, 1, 2, color);
        // arrow head on the top right
        rect(ctx, x + 4, y + 1, 3, 1, color);
        rect(ctx, x + 6, y, 1, 3, color);
    }

    /**
     * The colors of a raised button: face gradient, top highlight, bottom edge (the "thickness" of the button)
     */
    public record Bevel(int faceTop, int faceBottom, int highlight, int edge, int outline)
    {
        public Bevel hovered()
        {
            return new Bevel(Theme.mix(this.faceTop, 0xFFFFFFFF, 0.12f), Theme.mix(this.faceBottom, 0xFFFFFFFF, 0.12f),
                             this.highlight, this.edge, this.outline);
        }
    }

    public static final Bevel BEVEL_PRIMARY   = new Bevel(0xFFB88AF0, 0xFF8F55D8, 0x70FFFFFF, 0xFF4E2785, 0xFF1A0F2A);
    public static final Bevel BEVEL_SECONDARY = new Bevel(0xFF453A5C, 0xFF30283F, 0x40FFFFFF, 0xFF16111F, 0xFF0B080F);
    public static final Bevel BEVEL_QUIET     = new Bevel(0xFF2E2640, 0xFF241D33, 0x28FFFFFF, 0xFF120E19, 0xFF0B080F);
    public static final Bevel BEVEL_DANGER    = new Bevel(0xFF9A3442, 0xFF742332, 0x50FFFFFF, 0xFF3A0D16, 0xFF14060A);
    public static final Bevel BEVEL_ACTIVE    = new Bevel(0xFF5A3F86, 0xFF6B4A9E, 0x00000000, 0xFF2A1C40, 0xFF0B080F);
    public static final Bevel BEVEL_DISABLED  = new Bevel(0xFF2A2433, 0xFF262030, 0x00000000, 0xFF1C1724, 0xFF0B080F);

    /** How many pixels of the bottom of a raised button are its edge */
    public static final int BEVEL_DEPTH = 2;

    /**
     * Draws a 3D button. A raised button has a lit top and a thick bottom edge, a pressed one (a selected tab,
     * an "on" toggle, or while clicking) is sunk in by the edge height, with a shadow on top instead.
     * @return the y offset of the contents: the label moves down when the button is pressed
     */
    public static int button3d(GuiContext ctx, int x, int y, int w, int h, Bevel bevel, boolean pressed)
    {
        // Outline with cut corners
        rect(ctx, x + 1, y, w - 2, 1, bevel.outline());
        rect(ctx, x + 1, y + h - 1, w - 2, 1, bevel.outline());
        rect(ctx, x, y + 1, 1, h - 2, bevel.outline());
        rect(ctx, x + w - 1, y + 1, 1, h - 2, bevel.outline());

        int ix = x + 1;
        int iw = w - 2;
        int iy = y + 1;
        int ih = h - 2;

        if (pressed)
        {
            // Sunk in: a dark line on top, the face fills the rest
            rect(ctx, ix, iy, iw, 1, bevel.edge());
            gradient(ctx, ix, iy + 1, iw, ih - 1, bevel.faceTop(), bevel.faceBottom());
            return 1;
        }

        gradient(ctx, ix, iy, iw, ih - BEVEL_DEPTH, bevel.faceTop(), bevel.faceBottom());
        rect(ctx, ix, iy + ih - BEVEL_DEPTH, iw, BEVEL_DEPTH, bevel.edge());

        if ((bevel.highlight() >>> 24) != 0)
        {
            rect(ctx, ix, iy, iw, 1, bevel.highlight());
            rect(ctx, ix, iy + 1, 1, ih - BEVEL_DEPTH - 1, Theme.withAlpha(bevel.highlight(), (bevel.highlight() >>> 24) / 2));
        }

        return 0;
    }

    /**
     * @return the y of a label of the given height, centered on the face of a button
     */
    public static int buttonTextY(int y, int h, boolean pressed)
    {
        // Centered on the face of the raised button (the button minus the outline and the bottom edge),
        // one pixel lower when pressed, like the face itself
        return y + 1 + (h - 2 - BEVEL_DEPTH - 7) / 2 + (pressed ? 1 : 0);
    }

    /**
     * A small 3D button drawn without a widget, for the panels drawn on top of vanilla screens
     */
    public static void button3dLabel(GuiContext ctx, int x, int y, int w, int h, String label, Bevel bevel, boolean pressed, int textColor)
    {
        LayoutProbe.add(LayoutProbe.Kind.BUTTON, label, x, y, w, h);
        button3d(ctx, x, y, w, h, bevel, pressed);
        int tx = x + (w - width(label)) / 2;
        textShadow(ctx, label, tx, buttonTextY(y, h, pressed), textColor);
    }

    /**
     * A vertical scroll bar with a 3D thumb
     */
    public static void scrollbar(GuiContext ctx, int x, int y, int w, int h, int scroll, int maxScroll, int visible, boolean hover)
    {
        rect(ctx, x, y, w, h, 0x50000000);

        if (maxScroll <= 0)
        {
            return;
        }

        int total = maxScroll + visible;
        int th = Math.max(10, h * visible / total);
        int ty = y + (int) ((long) (h - th) * scroll / maxScroll);
        Bevel bevel = hover ? BEVEL_SECONDARY.hovered() : BEVEL_SECONDARY;
        rect(ctx, x, ty, w, th, bevel.outline());
        gradient(ctx, x + 1, ty + 1, w - 2, th - 2, bevel.faceTop(), bevel.faceBottom());
        rect(ctx, x + 1, ty + 1, w - 2, 1, bevel.highlight());
    }

    /**
     * The diagonal grip in the corner of a resizable panel
     */
    public static void resizeGrip(GuiContext ctx, int right, int bottom, int color)
    {
        for (int i = 0; i < 3; ++i)
        {
            int d = 3 + i * 3;

            for (int k = 0; k < d; ++k)
            {
                rect(ctx, right - 1 - k, bottom - d + k, 1, 1, color);
            }
        }
    }

    /**
     * Six dots, the handle to drag a panel by
     */
    public static void dragHandle(GuiContext ctx, int x, int y, int color)
    {
        for (int r = 0; r < 3; ++r)
        {
            rect(ctx, x, y + r * 3, 1, 1, color);
            rect(ctx, x + 3, y + r * 3, 1, 1, color);
        }
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h)
    {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
