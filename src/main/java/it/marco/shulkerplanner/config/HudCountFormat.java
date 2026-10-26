package it.marco.shulkerplanner.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.util.Format;

/**
 * How amounts are written on the HUD
 */
public enum HudCountFormat implements IConfigOptionListEntry
{
    /** 2 x 64 + 12 */
    STACKS  ("stacks"),
    /** 140 */
    ITEMS   ("items");

    private final String name;

    HudCountFormat(String name)
    {
        this.name = name;
    }

    public String format(int count, int maxStack)
    {
        return this == STACKS ? Format.stacks(count, maxStack) : Format.number(count);
    }

    @Override
    public String getStringValue()
    {
        return this.name;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate("shulkerplanner.hud_count." + this.name);
    }

    @Override
    public HudCountFormat cycle(boolean forward)
    {
        return this == STACKS ? ITEMS : STACKS;
    }

    @Override
    public HudCountFormat fromString(String value)
    {
        for (HudCountFormat format : values())
        {
            if (format.name.equalsIgnoreCase(value))
            {
                return format;
            }
        }

        return STACKS;
    }
}
