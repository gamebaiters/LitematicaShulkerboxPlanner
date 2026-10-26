package it.marco.shulkerplanner.gui;

import java.util.ArrayList;
import java.util.List;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;

import it.marco.shulkerplanner.Reference;
import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.config.Hotkeys;

public class GuiPlannerConfigs extends GuiConfigsBase
{
    public GuiPlannerConfigs()
    {
        super(10, 50, Reference.MOD_ID, null, "shulkerplanner.gui.title.configs");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        this.clearOptions();

        ButtonGeneric button = new ButtonGeneric(10, 26, -1, false, "shulkerplanner.gui.button.open_planner");
        this.addButton(button, (btn, mb) -> PlannerGuis.openPlanner(this, null));

        ButtonGeneric hud = new ButtonGeneric(button.getX() + button.getWidth() + 4, 26, -1, false, "shulkerplanner.gui.button.edit_hud");
        this.addButton(hud, (btn, mb) -> fi.dy.masa.malilib.gui.GuiBase.openGui(new GuiHudEditor(this)));
    }

    @Override
    protected int getConfigWidth()
    {
        return 160;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        List<ConfigOptionWrapper> list = new ArrayList<>();

        list.addAll(ConfigOptionWrapper.createFor(Configs.Generic.OPTIONS));
        list.addAll(ConfigOptionWrapper.createFor(Hotkeys.HOTKEY_LIST));

        return list;
    }
}
