package it.marco.shulkerplanner.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import it.marco.shulkerplanner.gui.GuiPlannerConfigs;

public class ModMenuImpl implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return (screen) -> {
            GuiPlannerConfigs gui = new GuiPlannerConfigs();
            gui.setParent(screen);
            return gui;
        };
    }
}
