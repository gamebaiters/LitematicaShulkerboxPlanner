package it.marco.shulkerplanner.gui;

import javax.annotation.Nullable;

import net.minecraft.client.gui.screens.Screen;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.data.ProjectStorage;

public class PlannerGuis
{
    private static PlannerTab lastTab = PlannerTab.BOXES;

    public static PlannerTab getLastTab()
    {
        return lastTab;
    }

    public static GuiBase create(PlannerTab tab, @Nullable Screen parent)
    {
        lastTab = tab;

        GuiBase gui = switch (tab)
        {
            case MATERIALS -> new GuiPlannerMaterials();
            case BOXES -> new GuiPlannerBoxes();
            case REMINDERS -> new GuiPlannerReminders();
        };

        gui.setParent(parent);
        return gui;
    }

    /**
     * Opens the planner for the given Litematica material list
     */
    public static void openForMaterialList(MaterialListBase materialList, @Nullable Screen parent)
    {
        PlannerSession.INSTANCE.openForMaterialList(materialList);
        GuiBase.openGui(create(lastTab, parent));
    }

    /**
     * Opens the planner for the best available source: the last viewed Litematica material list,
     * the material list of the selected placement, or the last used project.
     */
    public static void openPlanner(@Nullable Screen parent, @Nullable PlannerTab tab)
    {
        PlannerTab openTab = tab != null ? tab : lastTab;
        PlannerSession session = PlannerSession.INSTANCE;
        MaterialListBase materialList = DataManager.getMaterialList();

        if (materialList == null)
        {
            SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (placement != null)
            {
                materialList = placement.getMaterialList();
                materialList.reCreateMaterialList();
                DataManager.setMaterialList(materialList);
            }
        }

        if (materialList != null)
        {
            session.openForMaterialList(materialList);
        }
        else if (session.getProject() == null)
        {
            String last = Configs.Generic.LAST_PROJECT.getStringValue();

            if (last.isEmpty() || ProjectStorage.exists(last) == false)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "shulkerplanner.message.no_schematic");
                return;
            }

            session.openProject(last);
        }

        GuiBase.openGui(create(openTab, parent));
    }
}
