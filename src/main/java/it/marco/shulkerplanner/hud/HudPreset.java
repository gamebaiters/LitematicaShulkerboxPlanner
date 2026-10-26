package it.marco.shulkerplanner.hud;

import java.util.EnumSet;

import fi.dy.masa.malilib.util.StringUtils;

/**
 * Quick sets of visible elements. Picking one replaces the element toggles, which can then be changed one by one.
 */
public enum HudPreset
{
    FULL            ("full",        EnumSet.allOf(HudElement.class)),
    SHULKER         ("shulker",     EnumSet.of(HudElement.TITLE, HudElement.PROGRESS, HudElement.BOX_HEADER, HudElement.BOX_TOTALS,
                                               HudElement.BOX_GRID, HudElement.GRID_LEGEND, HudElement.WRONG_ITEMS, HudElement.BOX_STATUS, HudElement.REMINDERS)),
    LIST            ("list",        EnumSet.of(HudElement.TITLE, HudElement.PROGRESS, HudElement.BOX_HEADER, HudElement.BOX_TOTALS,
                                               HudElement.BOX_ITEMS, HudElement.WRONG_ITEMS, HudElement.BOX_STATUS, HudElement.REMINDERS)),
    MINIMAL         ("minimal",     EnumSet.of(HudElement.TITLE, HudElement.BOX_HEADER, HudElement.BOX_STATUS)),
    REMINDERS_ONLY  ("reminders",   EnumSet.of(HudElement.TITLE, HudElement.REMINDERS));

    private final String name;
    private final EnumSet<HudElement> elements;

    HudPreset(String name, EnumSet<HudElement> elements)
    {
        this.name = name;
        this.elements = elements;
    }

    public EnumSet<HudElement> getElements()
    {
        return EnumSet.copyOf(this.elements);
    }

    public String getDisplayName()
    {
        return StringUtils.translate("shulkerplanner.hud_preset." + this.name);
    }

    public HudPreset cycle(boolean forward)
    {
        HudPreset[] values = values();
        return values[(this.ordinal() + (forward ? 1 : values.length - 1)) % values.length];
    }

    /**
     * @return the preset matching exactly the given elements, or null if they were customized
     */
    public static HudPreset match(EnumSet<HudElement> elements)
    {
        for (HudPreset preset : values())
        {
            if (preset.elements.equals(elements))
            {
                return preset;
            }
        }

        return null;
    }
}
