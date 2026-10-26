package it.marco.shulkerplanner.plan;

import net.minecraft.world.item.ItemStack;

/**
 * One item type that has to be packed.
 *
 * @param id        the item registry id
 * @param proto     a single item stack of the item, used for rendering and for creating stacks
 * @param count     the amount of items to pack
 * @param maxStack  the stack size limit inside one shulker box slot
 * @param boxable   false for items that can't be put inside a shulker box (shulker boxes themselves)
 * @param sortKey   the item registry raw id, which groups related items (wood types, stone types...) together
 */
public record ItemRequirement(String id, ItemStack proto, int count, int maxStack, boolean boxable, int sortKey)
{
    public int getFullStacks()
    {
        return this.count / this.maxStack;
    }

    public int getRemainder()
    {
        return this.count % this.maxStack;
    }

    public int getSlotCount()
    {
        return this.getFullStacks() + (this.getRemainder() > 0 ? 1 : 0);
    }
}
