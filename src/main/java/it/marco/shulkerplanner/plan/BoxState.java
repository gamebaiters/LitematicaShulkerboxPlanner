package it.marco.shulkerplanner.plan;

import fi.dy.masa.malilib.util.StringUtils;

/**
 * The state of one box (phase) of a locked plan. Only changed by the player, or by the packing assistant
 * when a shulker box linked to this phase is filled exactly as planned.
 */
public enum BoxState
{
    /** Nothing done yet */
    TODO        ("todo"),
    /** Started: a shulker box is linked to it, or the player marked it as started */
    FILLING     ("filling"),
    /** Filled as planned */
    DONE        ("done");

    private final String name;

    BoxState(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDisplayName()
    {
        return StringUtils.translate("shulkerplanner.box_state." + this.name);
    }

    public static BoxState fromName(String name)
    {
        for (BoxState state : values())
        {
            if (state.name.equalsIgnoreCase(name))
            {
                return state;
            }
        }

        return TODO;
    }
}
