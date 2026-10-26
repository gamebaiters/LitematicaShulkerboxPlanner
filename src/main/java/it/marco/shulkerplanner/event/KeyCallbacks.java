package it.marco.shulkerplanner.event;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.util.InfoUtils;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.config.Hotkeys;
import it.marco.shulkerplanner.gui.GuiPlannerConfigs;
import it.marco.shulkerplanner.gui.PlannerGuis;
import it.marco.shulkerplanner.gui.PlannerTab;

public class KeyCallbacks
{
    public static void init()
    {
        IHotkeyCallback callback = new Callback();

        for (var hotkey : Hotkeys.HOTKEY_LIST)
        {
            hotkey.getKeybind().setCallback(callback);
        }
    }

    private static class Callback implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            if (key == Hotkeys.OPEN_PLANNER.getKeybind())
            {
                PlannerGuis.openPlanner(null, PlannerTab.BOXES);
                return true;
            }
            else if (key == Hotkeys.OPEN_REMINDERS.getKeybind())
            {
                PlannerGuis.openPlanner(null, PlannerTab.REMINDERS);
                return true;
            }
            else if (key == Hotkeys.TOGGLE_HUD.getKeybind())
            {
                boolean enabled = ! Configs.Generic.HUD_ENABLED.getBooleanValue();
                Configs.Generic.HUD_ENABLED.setBooleanValue(enabled);
                InfoUtils.printActionbarMessage(enabled ? "shulkerplanner.message.hud_enabled" : "shulkerplanner.message.hud_disabled");
                return true;
            }
            else if (key == Hotkeys.OPEN_CONFIG.getKeybind())
            {
                GuiBase.openGui(new GuiPlannerConfigs());
                return true;
            }

            return false;
        }
    }
}
