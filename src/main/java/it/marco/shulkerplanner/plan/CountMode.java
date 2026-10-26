package it.marco.shulkerplanner.plan;

import fi.dy.masa.malilib.util.StringUtils;

/**
 * Which quantity of every material list entry gets packed into shulker boxes.
 */
public enum CountMode
{
    TOTAL       ("total"),
    MISSING     ("missing"),
    TO_GATHER   ("to_gather");

    private final String name;

    CountMode(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDisplayName()
    {
        return StringUtils.translate("shulkerplanner.count_mode." + this.name);
    }

    public String getHoverText()
    {
        return StringUtils.translate("shulkerplanner.count_mode." + this.name + ".hover");
    }

    public CountMode cycle(boolean forward)
    {
        CountMode[] values = values();
        int id = (this.ordinal() + (forward ? 1 : values.length - 1)) % values.length;
        return values[id];
    }

    public static CountMode fromName(String name)
    {
        for (CountMode mode : values())
        {
            if (mode.name.equalsIgnoreCase(name))
            {
                return mode;
            }
        }

        return MISSING;
    }
}
