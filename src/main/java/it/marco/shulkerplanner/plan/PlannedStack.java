package it.marco.shulkerplanner.plan;

import net.minecraft.world.item.ItemStack;

/**
 * One slot of a planned shulker box.
 *
 * @param spare true if this stack was only added to fill up otherwise empty slots
 */
public record PlannedStack(ItemRequirement item, int count, boolean spare)
{
    public PlannedStack(ItemRequirement item, int count)
    {
        this(item, count, false);
    }

    public ItemStack createStack()
    {
        return this.item.proto().copyWithCount(this.count);
    }

    public String id()
    {
        return this.item.id();
    }
}
