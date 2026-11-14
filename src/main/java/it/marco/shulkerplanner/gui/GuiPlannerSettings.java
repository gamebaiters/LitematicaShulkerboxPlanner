package it.marco.shulkerplanner.gui;

import java.util.ArrayList;
import java.util.List;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.gui.ui.FlatButton;
import it.marco.shulkerplanner.gui.widgets.WidgetListPlannerSettings;
import it.marco.shulkerplanner.gui.widgets.WidgetPlannerSettingEntry;

/**
 * Planner settings page: warnings/highlights toggles and general options
 */
public class GuiPlannerSettings extends GuiPlannerBase<SettingRow, WidgetPlannerSettingEntry, WidgetListPlannerSettings>
{
    public GuiPlannerSettings()
    {
        super(PlannerTab.SETTINGS);
    }

    @Override
    protected WidgetListPlannerSettings createListWidget(int listX, int listY)
    {
        return new WidgetListPlannerSettings(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    public List<SettingRow> getRows()
    {
        List<SettingRow> rows = new ArrayList<>();

        rows.add(SettingRow.header(StringUtils.translate("shulkerplanner.gui.settings.section.warnings")));

        for (IConfigBase config : Configs.Warnings.OPTIONS)
        {
            if (config instanceof ConfigBoolean bool)
            {
                rows.add(SettingRow.option(bool));
            }
        }

        rows.add(SettingRow.header(StringUtils.translate("shulkerplanner.gui.settings.section.generic")));

        for (IConfigBase config : Configs.Generic.OPTIONS)
        {
            if (config instanceof ConfigBoolean bool)
            {
                rows.add(SettingRow.option(bool));
            }
        }

        return rows;
    }

    @Override
    protected void createOptionButtons()
    {
        FlatButton enableAll = new FlatButton(0, 0, -1, StringUtils.translate("shulkerplanner.gui.button.warnings_all_on"),
                                              FlatButton.Style.SECONDARY,
                                              StringUtils.translate("shulkerplanner.gui.button.warnings_all_on.hover"));
        this.addFlowButton(enableAll, (btn, mb) -> {
            Configs.Warnings.setAll(true);
            Configs.saveToFile();
            this.initGui();
        });

        FlatButton disableAll = new FlatButton(0, 0, -1, StringUtils.translate("shulkerplanner.gui.button.warnings_all_off"),
                                               FlatButton.Style.SECONDARY,
                                               StringUtils.translate("shulkerplanner.gui.button.warnings_all_off.hover"));
        this.addFlowButton(disableAll, (btn, mb) -> {
            Configs.Warnings.setAll(false);
            Configs.saveToFile();
            this.initGui();
        });

        this.addFlowGap();

        FlatButton hotkeys = new FlatButton(0, 0, -1, StringUtils.translate("shulkerplanner.gui.button.open_hotkeys"),
                                            FlatButton.Style.GHOST,
                                            StringUtils.translate("shulkerplanner.gui.button.open_hotkeys.hover"));
        this.addFlowButton(hotkeys, (btn, mb) -> {
            GuiPlannerConfigs gui = new GuiPlannerConfigs();
            gui.setParent(this);
            GuiBase.openGui(gui);
        });
    }

    @Override
    protected List<String> getSummaryLines()
    {
        int enabled = 0;

        for (IConfigBase config : Configs.Warnings.OPTIONS)
        {
            if (config instanceof ConfigBoolean bool && bool.getBooleanValue())
            {
                enabled++;
            }
        }

        return List.of(StringUtils.translate("shulkerplanner.gui.summary.settings", enabled, Configs.Warnings.OPTIONS.size()));
    }
}
