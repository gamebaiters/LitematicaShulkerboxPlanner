package it.marco.shulkerplanner.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import it.marco.shulkerplanner.plan.PlannedBox;

public class InventoryScanner
{
    /**
     * Slots of the player inventory that are scanned: the 36 main slots and the offhand
     */
    private static final int[] SCANNED_SLOTS = createScannedSlots();

    private static int[] createScannedSlots()
    {
        int[] slots = new int[Inventory.INVENTORY_SIZE + 1];

        for (int i = 0; i < Inventory.INVENTORY_SIZE; ++i)
        {
            slots[i] = i;
        }

        slots[Inventory.INVENTORY_SIZE] = Inventory.SLOT_OFFHAND;
        return slots;
    }

    public static String getId(ItemStack stack)
    {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static boolean isShulkerBox(ItemStack stack)
    {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    /**
     * @return the item id to count map of the contents of a shulker box item, or null if it's not a shulker box
     */
    @Nullable
    public static Map<String, Integer> getShulkerContents(ItemStack stack)
    {
        if (isShulkerBox(stack) == false)
        {
            return null;
        }

        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        Map<String, Integer> map = new LinkedHashMap<>();

        if (contents != null)
        {
            contents.nonEmptyItemCopyStream().forEach(s -> map.merge(getId(s), s.getCount(), Integer::sum));
        }

        return map;
    }

    /**
     * Counts all the items in the player's inventory, including the contents of shulker boxes and bundles.
     */
    public static Map<String, Integer> countAvailableItems(@Nullable Player player)
    {
        Map<String, Integer> map = new HashMap<>();

        if (player == null)
        {
            return map;
        }

        Inventory inv = player.getInventory();

        for (int slot : SCANNED_SLOTS)
        {
            ItemStack stack = inv.getItem(slot);

            if (stack.isEmpty())
            {
                continue;
            }

            map.merge(getId(stack), stack.getCount(), Integer::sum);

            Map<String, Integer> contents = getShulkerContents(stack);

            if (contents != null)
            {
                contents.forEach((id, count) -> map.merge(id, count, Integer::sum));
                continue;
            }

            BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);

            if (bundle != null)
            {
                bundle.itemCopies().forEach(s -> map.merge(getId(s), s.getCount(), Integer::sum));
            }
        }

        return map;
    }

    /**
     * Counts only the loose items in the player's inventory, the ones that can be moved into a shulker box right away.
     */
    public static Map<String, Integer> countLooseItems(@Nullable Player player)
    {
        Map<String, Integer> map = new HashMap<>();

        if (player == null)
        {
            return map;
        }

        Inventory inv = player.getInventory();

        for (int slot : SCANNED_SLOTS)
        {
            ItemStack stack = inv.getItem(slot);

            if (stack.isEmpty() == false)
            {
                map.merge(getId(stack), stack.getCount(), Integer::sum);
            }
        }

        return map;
    }

    /**
     * @return the content signature to count map of the non-empty shulker boxes in the player's inventory
     */
    public static Map<String, Integer> getShulkerSignatures(@Nullable Player player)
    {
        Map<String, Integer> map = new HashMap<>();

        if (player == null)
        {
            return map;
        }

        Inventory inv = player.getInventory();

        for (int slot : SCANNED_SLOTS)
        {
            Map<String, Integer> contents = getShulkerContents(inv.getItem(slot));

            if (contents != null && contents.isEmpty() == false)
            {
                map.merge(PlannedBox.createSignature(contents), 1, Integer::sum);
            }
        }

        return map;
    }
}
