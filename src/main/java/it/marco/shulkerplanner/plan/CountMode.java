package it.marco.shulkerplanner.plan;

import fi.dy.masa.malilib.util.StringUtils;

/**
 * Which quantity of every material list entry gets packed into shulker boxes.
 */
public enum CountMode
{
    TOTAL       ("total"),
    MISSING     ("missing");

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

        // "to_gather" (1.x) subtracted the items already packed into boxes from the plan itself, which made
        // the plan shrink while filling it. What is still to gather is now a column of the materials tab.
        return MISSING;
    }

    /**
     * The amount of one material list entry to pack.
     * With a multiplier, the copy that is being built counts what's missing, the other copies count in full.
     */
    public long required(long total, long missing, int multiplier)
    {
        int mult = Math.max(1, multiplier);
        long missingClamped = Math.max(0, Math.min(total, missing));

        return switch (this)
        {
            case TOTAL -> total * mult;
            case MISSING -> missingClamped + total * (mult - 1);
        };
    }
}
