package it.marco.shulkerplanner.hud;

import fi.dy.masa.malilib.util.StringUtils;

/**
 * The color roles of the HUD. A theme gives every role a color, and each one can be changed by hand.
 */
public enum HudColor
{
    BACKGROUND  ("background"),
    ACCENT      ("accent"),
    BORDER      ("border"),
    TITLE       ("title"),
    TEXT        ("text"),
    LABEL       ("label"),
    GOOD        ("good"),
    INFO        ("info"),
    WARN        ("warn"),
    BAD         ("bad");

    private final String name;

    HudColor(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDisplayName()
    {
        return StringUtils.translate("shulkerplanner.hud_color." + this.name);
    }

    /**
     * @return true for the roles where the alpha channel matters
     */
    public boolean hasAlpha()
    {
        return this == BACKGROUND;
    }
}
