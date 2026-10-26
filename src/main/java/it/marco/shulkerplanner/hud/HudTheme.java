package it.marco.shulkerplanner.hud;

import fi.dy.masa.malilib.util.StringUtils;

/**
 * Color presets. The order of the colors follows {@link HudColor}:
 * background, accent, border, title, text, label, good, info, warn, bad.
 */
public enum HudTheme
{
    PURPLE      ("purple",   0xA8000000, 0xFFA060E0, 0xFF6A3A9A, 0xFFFF77FF, 0xFFFFFFFF, 0xFFAAAAAA, 0xFF55FF55, 0xFF55FFFF, 0xFFFFAA00, 0xFFFF5555),
    DARK        ("dark",     0xD0101010, 0xFF707070, 0xFF404040, 0xFFFFFFFF, 0xFFE0E0E0, 0xFF909090, 0xFF7CD67C, 0xFF7CC4E0, 0xFFE0B050, 0xFFE06060),
    LIGHT       ("light",    0xE0F2F2F2, 0xFF3A7BD5, 0xFFB0B0B0, 0xFF1F4E9A, 0xFF202020, 0xFF606060, 0xFF1E8A2E, 0xFF1A6FA0, 0xFFB06A00, 0xFFC02020),
    OCEAN       ("ocean",    0xB8081828, 0xFF30C0FF, 0xFF1D5C80, 0xFF6FE3FF, 0xFFEAF6FF, 0xFF8FB4C8, 0xFF5BE3A6, 0xFF6FE3FF, 0xFFFFD166, 0xFFFF6B6B),
    EMERALD     ("emerald",  0xB8061A0E, 0xFF3DDC84, 0xFF1F6B42, 0xFF8CFFB8, 0xFFF0FFF4, 0xFF94BBA2, 0xFF3DDC84, 0xFF7FD8E8, 0xFFF5C451, 0xFFFF6E6E),
    SUNSET      ("sunset",   0xB8200A12, 0xFFFF7A45, 0xFF7A2E1E, 0xFFFFB067, 0xFFFFF1E6, 0xFFC9A090, 0xFF9BE07A, 0xFF7FD1FF, 0xFFFFC14D, 0xFFFF5470),
    GLASS       ("glass",    0x40000000, 0x00000000, 0x00000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFD0D0D0, 0xFF55FF55, 0xFF55FFFF, 0xFFFFAA00, 0xFFFF5555),
    CUSTOM      ("custom",   0xA8000000, 0xFFA060E0, 0xFF6A3A9A, 0xFFFF77FF, 0xFFFFFFFF, 0xFFAAAAAA, 0xFF55FF55, 0xFF55FFFF, 0xFFFFAA00, 0xFFFF5555);

    private final String name;
    private final int[] colors;

    HudTheme(String name, int... colors)
    {
        this.name = name;
        this.colors = colors;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDisplayName()
    {
        return StringUtils.translate("shulkerplanner.hud_theme." + this.name);
    }

    public int getColor(HudColor role)
    {
        return this.colors[role.ordinal()];
    }

    /**
     * Cycles through the real presets, "custom" is only set by editing a color
     */
    public HudTheme cycle(boolean forward)
    {
        int count = values().length - 1;
        int index = this == CUSTOM ? 0 : (this.ordinal() + (forward ? 1 : count - 1)) % count;
        return values()[index];
    }

    public static HudTheme fromName(String name)
    {
        for (HudTheme theme : values())
        {
            if (theme.name.equals(name))
            {
                return theme;
            }
        }

        return PURPLE;
    }
}
