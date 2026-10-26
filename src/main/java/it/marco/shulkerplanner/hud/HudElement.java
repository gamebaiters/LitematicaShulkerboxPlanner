package it.marco.shulkerplanner.hud;

import fi.dy.masa.malilib.util.StringUtils;

/**
 * Every part of the HUD, in drawing order. Each one can be switched on and off per HUD.
 */
public enum HudElement
{
    TITLE       ("title"),
    PROGRESS    ("progress"),
    BOX_HEADER  ("box_header"),
    BOX_TOTALS  ("box_totals"),
    BOX_GRID    ("box_grid"),
    GRID_LEGEND ("grid_legend"),
    BOX_ITEMS   ("box_items"),
    WRONG_ITEMS ("wrong_items"),
    BOX_STATUS  ("box_status"),
    REMINDERS   ("reminders");

    private final String name;

    HudElement(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDisplayName()
    {
        return StringUtils.translate("shulkerplanner.hud_element." + this.name);
    }

    public static HudElement fromName(String name)
    {
        for (HudElement element : values())
        {
            if (element.name.equals(name))
            {
                return element;
            }
        }

        return null;
    }
}
