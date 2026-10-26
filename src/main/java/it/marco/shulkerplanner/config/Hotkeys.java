package it.marco.shulkerplanner.config;

import java.util.List;
import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.config.options.ConfigHotkey;

import it.marco.shulkerplanner.Reference;

public class Hotkeys
{
    private static final String HOTKEYS_KEY = Reference.MOD_ID + ".config.hotkeys";

    public static final ConfigHotkey OPEN_PLANNER           = new ConfigHotkey("openShulkerPlanner",    "M,B").apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_REMINDERS         = new ConfigHotkey("openReminders",         "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_HUD             = new ConfigHotkey("toggleReminderHud",     "M,H").apply(HOTKEYS_KEY);
    public static final ConfigHotkey EDIT_HUD               = new ConfigHotkey("editHudPosition",       "M,J").apply(HOTKEYS_KEY);
    public static final ConfigHotkey CYCLE_HUD_VIEW         = new ConfigHotkey("cycleHudView",          "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_CONFIG            = new ConfigHotkey("openConfigGui",         "").apply(HOTKEYS_KEY);

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            OPEN_PLANNER,
            OPEN_REMINDERS,
            TOGGLE_HUD,
            EDIT_HUD,
            CYCLE_HUD_VIEW,
            OPEN_CONFIG
    );
}
