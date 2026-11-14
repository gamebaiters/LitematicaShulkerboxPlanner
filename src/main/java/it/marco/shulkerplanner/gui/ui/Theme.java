package it.marco.shulkerplanner.gui.ui;

/**
 * The colors of the planner screens. One dark palette with the shulker purple as accent,
 * every state color is always paired with an icon or a label, never color alone.
 */
public final class Theme
{
    public static final int SCRIM          = 0xD8100C16;
    public static final int SURFACE        = 0xF0181320;
    public static final int SURFACE_2      = 0xF0211A2C;
    public static final int SURFACE_3      = 0xF02B2238;
    public static final int SURFACE_HOVER  = 0xF0342A44;
    public static final int BORDER         = 0xFF33294A;
    public static final int BORDER_STRONG  = 0xFF4A3D66;

    public static final int ACCENT         = 0xFFA463E6;
    public static final int ACCENT_HOVER   = 0xFFB97FF2;
    public static final int ACCENT_DIM     = 0xFF5B3A85;
    public static final int ACCENT_BG      = 0x40A463E6;

    public static final int TEXT           = 0xFFEDE8F5;
    public static final int TEXT_DIM       = 0xFFB0A6C2;
    public static final int TEXT_MUTED     = 0xFF746A88;

    public static final int SUCCESS        = 0xFF4ADE80;
    public static final int SUCCESS_BG     = 0x3022C55E;
    public static final int WARN           = 0xFFFBBF24;
    public static final int WARN_BG        = 0x30F59E0B;
    public static final int DANGER         = 0xFFF87171;
    public static final int DANGER_BG      = 0x30EF4444;
    public static final int INFO           = 0xFF60A5FA;
    public static final int INFO_BG        = 0x303B82F6;

    public static final int SLOT_BG        = 0xFF0F0C15;
    public static final int SLOT_BORDER    = 0xFF2A2236;

    /** Spacing unit: everything is laid out in multiples of 4 */
    public static final int GAP = 4;

    private Theme()
    {
    }

    public static int withAlpha(int rgb, int alpha)
    {
        return (rgb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /**
     * Mixes two colors, t = 0 gives a, t = 1 gives b
     */
    public static int mix(int a, int b, float t)
    {
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
               | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
