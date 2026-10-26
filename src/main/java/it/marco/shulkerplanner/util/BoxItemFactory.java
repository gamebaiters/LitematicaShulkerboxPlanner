package it.marco.shulkerplanner.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.plan.PlannedBox;
import it.marco.shulkerplanner.plan.PlannedStack;
import it.marco.shulkerplanner.plan.ShulkerPlan;

/**
 * Creates the actual filled shulker box items of a plan, and gives them to the player in creative mode.
 */
public class BoxItemFactory
{
    public static ItemStack createBoxItem(PlannerProject project, PlannedBox box, int boxCount)
    {
        ItemStack stack = new ItemStack(project.getShulkerItem(box.getIndex()));
        List<ItemStack> items = new ArrayList<>(PlannedBox.SLOTS);

        for (PlannedStack planned : box.getSlots())
        {
            items.add(planned.createStack());
        }

        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(getBoxName(box, boxCount)).withStyle(ChatFormatting.LIGHT_PURPLE));

        return stack;
    }

    public static String getBoxName(PlannedBox box, int boxCount)
    {
        String first = box.getSlots().isEmpty() ? "" : box.getSlots().getFirst().item().proto().getHoverName().getString();
        int types = box.getTotals().size();
        String name = "#" + box.getIndex() + "/" + boxCount + " " + first;

        return types > 1 ? name + " +" + (types - 1) : name;
    }

    public static boolean canGiveItems()
    {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.gameMode != null && mc.player.hasInfiniteMaterials();
    }

    /**
     * Puts the given stack into the first empty main inventory slot, using the creative mode inventory packet.
     * @return true on success, false if not in creative mode or the inventory is full
     */
    public static boolean giveToPlayer(ItemStack stack)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (canGiveItems() == false || player == null || mc.gameMode == null)
        {
            return false;
        }

        Inventory inv = player.getInventory();

        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; ++slot)
        {
            if (inv.getItem(slot).isEmpty())
            {
                // Inventory slots 0-8 are the hotbar, which are slots 36-44 in the player's inventory menu
                int menuSlot = slot < Inventory.SELECTION_SIZE ? slot + 36 : slot;
                inv.setItem(slot, stack.copy());
                mc.gameMode.handleCreativeModeItemAdd(stack, menuSlot);
                return true;
            }
        }

        return false;
    }

    /**
     * Gives all the boxes that are not done/ready yet, as long as there is space in the inventory.
     * @return the number of boxes given
     */
    public static int giveAllMissing(PlannerSession session)
    {
        PlannerProject project = session.getProject();

        if (project == null || canGiveItems() == false)
        {
            return 0;
        }

        ShulkerPlan plan = session.getPlan();
        int given = 0;

        for (PlannedBox box : plan.getBoxes())
        {
            if (session.isBoxReady(box))
            {
                continue;
            }

            if (giveToPlayer(createBoxItem(project, box, plan.getBoxCount())) == false)
            {
                break;
            }

            ++given;
        }

        session.refreshInventory(false);

        return given;
    }

    /**
     * @return a human readable list of the boxes and their contents
     */
    public static String createExportText(PlannerProject project, ShulkerPlan plan)
    {
        StringBuilder sb = new StringBuilder();
        String nl = System.lineSeparator();

        sb.append("Litematica Shulker Planner - ").append(project.getName()).append(nl);
        sb.append(StringUtils.translate("shulkerplanner.export.header",
                                        plan.getBoxCount(), plan.getFullBoxCount(), Format.number(plan.getTotalItems()), plan.getTotalStacks()))
          .append(nl);
        sb.append(StringUtils.translate("shulkerplanner.export.options",
                                        project.getCountMode().getDisplayName(), project.getPackMode().getDisplayName(), project.getMultiplier()))
          .append(nl).append(nl);

        for (PlannedBox box : plan.getBoxes())
        {
            String done = project.isBoxDone(box) ? " [OK]" : "";
            sb.append(StringUtils.translate("shulkerplanner.export.box", box.getIndex(), plan.getBoxCount(), box.getUsedSlots())).append(done).append(nl);

            for (Map.Entry<String, Integer> entry : box.getTotals().entrySet())
            {
                var req = box.getItems().get(entry.getKey());
                int count = entry.getValue();
                sb.append("    ").append(String.format("%6d", count)).append("  ")
                  .append(req.proto().getHoverName().getString())
                  .append("  (").append(Format.stacks(count, req.maxStack())).append(")").append(nl);
            }

            sb.append(nl);
        }

        if (plan.getLooseItems().isEmpty() == false)
        {
            sb.append(StringUtils.translate("shulkerplanner.export.loose")).append(nl);

            for (var req : plan.getLooseItems())
            {
                sb.append("    ").append(String.format("%6d", req.count())).append("  ").append(req.proto().getHoverName().getString()).append(nl);
            }
        }

        return sb.toString();
    }
}
