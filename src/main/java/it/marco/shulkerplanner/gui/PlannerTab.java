package it.marco.shulkerplanner.gui;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import fi.dy.masa.malilib.util.StringUtils;

public enum PlannerTab
{
    BOXES       ("boxes"),
    MATERIALS   ("materials"),
    RAW         ("raw"),
    REMINDERS   ("reminders"),
    SETTINGS    ("settings");

    private final String name;

    PlannerTab(String name)
    {
        this.name = name;
    }

    public String getDisplayName()
    {
        return StringUtils.translate("shulkerplanner.gui.tab." + this.name);
    }

    public String getDescription()
    {
        return StringUtils.translate("shulkerplanner.gui.tab." + this.name + ".hover");
    }

    public ItemStack getIcon()
    {
        return new ItemStack(switch (this)
        {
            case BOXES -> Items.SHULKER_BOX;
            case MATERIALS -> Items.CHEST;
            case RAW -> Items.CRAFTING_TABLE;
            case REMINDERS -> Items.WRITABLE_BOOK;
            case SETTINGS -> Items.COMPARATOR;
        });
    }
}
