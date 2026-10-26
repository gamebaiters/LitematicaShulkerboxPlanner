package it.marco.shulkerplanner.plan;

import java.util.Collections;
import java.util.List;

/**
 * The result of packing a list of item requirements into shulker boxes.
 */
public class ShulkerPlan
{
    public static final ShulkerPlan EMPTY = new ShulkerPlan(List.of(), List.of(), List.of());

    private final List<PlannedBox> boxes;
    private final List<ItemRequirement> loose;
    private final List<ItemRequirement> requirements;
    private final long totalItems;
    private final int totalStacks;
    private final int fullBoxes;

    public ShulkerPlan(List<PlannedBox> boxes, List<ItemRequirement> loose, List<ItemRequirement> requirements)
    {
        this.boxes = Collections.unmodifiableList(boxes);
        this.loose = Collections.unmodifiableList(loose);
        this.requirements = Collections.unmodifiableList(requirements);

        long items = 0;
        int stacks = 0;
        int full = 0;

        for (PlannedBox box : boxes)
        {
            items += box.getItemCount();
            stacks += box.getUsedSlots();

            if (box.getFreeSlots() == 0)
            {
                ++full;
            }
        }

        this.totalItems = items;
        this.totalStacks = stacks;
        this.fullBoxes = full;
    }

    public List<PlannedBox> getBoxes()
    {
        return this.boxes;
    }

    /**
     * @return the items that can't be put into a shulker box (for example shulker boxes)
     */
    public List<ItemRequirement> getLooseItems()
    {
        return this.loose;
    }

    /**
     * @return all the requirements that were used to create this plan, including the loose ones
     */
    public List<ItemRequirement> getRequirements()
    {
        return this.requirements;
    }

    public int getBoxCount()
    {
        return this.boxes.size();
    }

    public int getFullBoxCount()
    {
        return this.fullBoxes;
    }

    public long getTotalItems()
    {
        return this.totalItems;
    }

    public int getTotalStacks()
    {
        return this.totalStacks;
    }

    public int getFreeSlots()
    {
        return this.boxes.size() * PlannedBox.SLOTS - this.totalStacks;
    }

    public boolean isEmpty()
    {
        return this.boxes.isEmpty() && this.loose.isEmpty();
    }
}
