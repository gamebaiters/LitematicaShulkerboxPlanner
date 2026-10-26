package it.marco.shulkerplanner.gui;

import javax.annotation.Nullable;

import net.minecraft.client.gui.screens.Screen;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;

import it.marco.shulkerplanner.data.PlannerSession;

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
     * Opens the planner for the schematic the player is working on:
     * the placement selected in Litematica, otherwise the last viewed Litematica material list
     * as long as its schematic is still loaded, otherwise the active project if it is loaded.
     */
    public static void openPlanner(@Nullable Screen parent, @Nullable PlannerTab tab)
    {
        PlannerTab openTab = tab != null ? tab : lastTab;
        PlannerSession session = PlannerSession.INSTANCE;
        MaterialListBase materialList = null;
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null)
        {
            materialList = placement.getMaterialList();

            if (materialList.getMaterialsAll().isEmpty())
            {
                materialList.reCreateMaterialList();
            }
        }
        else
        {
            MaterialListBase last = DataManager.getMaterialList();

            if (last != null && isLoaded(last))
            {
                materialList = last;
            }
        }

        if (materialList != null)
        {
            session.openForMaterialList(materialList);
        }
        else if (session.getProject() == null || session.isLoadedInGame() == false)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "shulkerplanner.message.no_schematic");
            return;
        }

        GuiBase.openGui(create(openTab, parent));
    }

    /**
     * @return false for the material list of a placement that has been removed in the meantime
     */
    private static boolean isLoaded(MaterialListBase list)
    {
        if (list instanceof MaterialListPlacement)
        {
            String name = list.getName();

            // Only the names: getMaterialList() would start a counting task for every placement
            for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements())
            {
                if (placement.getName().equals(name))
                {
                    return true;
                }
            }

            return false;
        }

        return true;
    }
}
