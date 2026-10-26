package it.marco.shulkerplanner.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.plan.PlannedBox;

public class Format
{
    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ITALY);
    private static final DecimalFormat INT = new DecimalFormat("#,##0", SYMBOLS);
    private static final DecimalFormat BOXES = new DecimalFormat("0.00", SYMBOLS);

    public static String number(long value)
    {
        return INT.format(value);
    }

    /**
     * @return for example "28 x 64 + 58", or just "58" if less than one stack
     */
    public static String stacks(int count, int maxStack)
    {
        if (maxStack <= 1 || count < maxStack)
        {
            return String.valueOf(count);
        }

        int stacks = count / maxStack;
        int remainder = count % maxStack;
        String str = stacks + " x " + maxStack;

        return remainder > 0 ? str + " + " + remainder : str;
    }

    /**
     * @return the number of shulker boxes the given count fills, for example "1,07"
     */
    public static String boxes(int count, int maxStack)
    {
        double boxes = (double) count / (PlannedBox.SLOTS * (double) Math.max(1, maxStack));
        return BOXES.format(boxes);
    }

    /**
     * Trims the string to fit into the given width, adding "..." to the end when trimmed
     */
    public static String trim(String str, int maxWidth)
    {
        if (StringUtils.getStringWidth(str) <= maxWidth)
        {
            return str;
        }

        String dots = "...";
        int dotsWidth = StringUtils.getStringWidth(dots);

        while (str.isEmpty() == false && StringUtils.getStringWidth(str) + dotsWidth > maxWidth)
        {
            str = str.substring(0, str.length() - 1);
        }

        return str + dots;
    }
}
