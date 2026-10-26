package it.marco.shulkerplanner.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * How the box being filled is shown on the HUD
 */
public enum HudBoxView implements IConfigOptionListEntry
{
    /** Only the list of the items still missing */
    COMPACT ("compact"),
    /** The shulker box interface, 9x3 slots with colors */
    SHULKER ("shulker"),
    /** The shulker box interface and the list below it */
    BOTH    ("both");

    private final String name;

    HudBoxView(String name)
    {
        this.name = name;
    }

    public boolean showGrid()
    {
        return this != COMPACT;
    }

    public boolean showList()
    {
        return this != SHULKER;
    }

    @Override
    public String getStringValue()
    {
        return this.name;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate("shulkerplanner.hud_view." + this.name);
    }

    @Override
    public HudBoxView cycle(boolean forward)
    {
        HudBoxView[] values = values();
        return values[(this.ordinal() + (forward ? 1 : values.length - 1)) % values.length];
    }

    @Override
    public HudBoxView fromString(String value)
    {
        for (HudBoxView view : values())
        {
            if (view.name.equalsIgnoreCase(value))
            {
                return view;
            }
        }

        return BOTH;
    }
}
