package it.marco.shulkerplanner.plan;

import fi.dy.masa.malilib.util.StringUtils;

/**
 * How item stacks get distributed into the shulker boxes.
 */
public enum PackMode
{
    /** Full boxes of a single item first, then the leftovers bin-packed with as few splits as possible. */
    SMART       ("smart"),
    /** All stacks in item registry order, one box after another. Always the minimum amount of boxes. */
    SEQUENTIAL  ("sequential"),
    /** Every item type gets its own boxes, never mixed with other items. */
    ONE_TYPE    ("one_type");

    private final String name;

    PackMode(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDisplayName()
    {
        return StringUtils.translate("shulkerplanner.pack_mode." + this.name);
    }

    public String getHoverText()
    {
        return StringUtils.translate("shulkerplanner.pack_mode." + this.name + ".hover");
    }

    public PackMode cycle(boolean forward)
    {
        PackMode[] values = values();
        int id = (this.ordinal() + (forward ? 1 : values.length - 1)) % values.length;
        return values[id];
    }

    public static PackMode fromName(String name)
    {
        for (PackMode mode : values())
        {
            if (mode.name.equalsIgnoreCase(name))
            {
                return mode;
            }
        }

        return SMART;
    }
}
