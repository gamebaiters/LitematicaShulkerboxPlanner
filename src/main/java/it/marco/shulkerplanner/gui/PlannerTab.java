package it.marco.shulkerplanner.gui;

import fi.dy.masa.malilib.util.StringUtils;

public enum PlannerTab
{
    MATERIALS   ("materials"),
    BOXES       ("boxes"),
    REMINDERS   ("reminders");

    private final String name;

    PlannerTab(String name)
    {
        this.name = name;
    }

    public String getDisplayName()
    {
        return StringUtils.translate("shulkerplanner.gui.tab." + this.name);
    }
}
