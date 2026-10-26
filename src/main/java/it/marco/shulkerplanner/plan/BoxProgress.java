package it.marco.shulkerplanner.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The state of one planned box compared to what is already inside it and what the player carries.
 * Every planned slot gets a state, so the HUD can draw the box like the real shulker box screen.
 */
public class BoxProgress
{
    public enum State
    {
        /** Already inside the box */
        IN_BOX,
        /** Not in the box yet, but the player carries all of it */
        IN_INVENTORY,
        /** Only a part is in the box or in the inventory */
        PARTIAL,
        /** Nothing of it yet */
        MISSING
    }

    public record Slot(PlannedStack stack, int inBox, int inInventory, State state)
    {
        public int missing()
        {
            return Math.max(0, this.stack.count() - this.inBox - this.inInventory);
        }
    }

    public record Item(ItemRequirement item, int need, int inBox, int inInventory, State state)
    {
        public int missing()
        {
            return Math.max(0, this.need - this.inBox - this.inInventory);
        }

        /**
         * @return how many still have to be put into the box (from the inventory or still to gather)
         */
        public int toPutIn()
        {
            return Math.max(0, this.need - this.inBox);
        }
    }

    private final PlannedBox box;
    private final List<Slot> slots;
    private final List<Item> items;
    private final Map<String, Integer> extra;
    private final boolean contentsKnown;
    private final int total;
    private final int inBox;
    private final int inInventory;

    private BoxProgress(PlannedBox box, List<Slot> slots, List<Item> items, Map<String, Integer> extra, boolean contentsKnown)
    {
        this.box = box;
        this.slots = Collections.unmodifiableList(slots);
        this.items = Collections.unmodifiableList(items);
        this.extra = Collections.unmodifiableMap(extra);
        this.contentsKnown = contentsKnown;

        int total = 0;
        int inBox = 0;
        int inInv = 0;

        for (Item item : items)
        {
            total += item.need();
            inBox += item.inBox();
            inInv += item.inInventory();
        }

        this.total = total;
        this.inBox = inBox;
        this.inInventory = inInv;
    }

    /**
     * @param boxContents the items already inside the box (item id to count), empty if unknown
     * @param inventory the loose items the player carries (item id to count)
     */
    public static BoxProgress compute(PlannedBox box, Map<String, Integer> boxContents, Map<String, Integer> inventory, boolean contentsKnown)
    {
        Map<String, Integer> boxLeft = new HashMap<>();
        Map<String, Integer> invLeft = new HashMap<>();
        List<Slot> slots = new ArrayList<>(box.getUsedSlots());
        List<Item> items = new ArrayList<>();
        Map<String, Integer> extra = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> e : box.getTotals().entrySet())
        {
            String id = e.getKey();
            int need = e.getValue();
            int have = boxContents.getOrDefault(id, 0);
            int inBox = Math.min(need, have);
            int inInv = Math.min(need - inBox, inventory.getOrDefault(id, 0));

            boxLeft.put(id, inBox);
            invLeft.put(id, inInv);
            items.add(new Item(box.getItems().get(id), need, inBox, inInv, state(need, inBox, inInv)));

            if (have > need)
            {
                extra.put(id, have - need);
            }
        }

        for (Map.Entry<String, Integer> e : boxContents.entrySet())
        {
            if (box.getTotals().containsKey(e.getKey()) == false && e.getValue() > 0)
            {
                extra.put(e.getKey(), e.getValue());
            }
        }

        // Distribute the counts over the planned stacks in slot order, filling the box first
        for (PlannedStack stack : box.getSlots())
        {
            String id = stack.id();
            int b = Math.min(stack.count(), boxLeft.getOrDefault(id, 0));
            boxLeft.merge(id, -b, Integer::sum);
            int i = Math.min(stack.count() - b, invLeft.getOrDefault(id, 0));
            invLeft.merge(id, -i, Integer::sum);
            slots.add(new Slot(stack, b, i, state(stack.count(), b, i)));
        }

        // Most urgent first: missing, partial, in inventory, done
        items.sort((a, c) -> {
            int cmp = Integer.compare(c.state().ordinal(), a.state().ordinal());
            return cmp != 0 ? cmp : Integer.compare(c.missing(), a.missing());
        });

        return new BoxProgress(box, slots, items, extra, contentsKnown);
    }

    private static State state(int need, int inBox, int inInventory)
    {
        if (inBox >= need)
        {
            return State.IN_BOX;
        }
        else if (inBox + inInventory >= need)
        {
            return State.IN_INVENTORY;
        }
        else if (inBox + inInventory > 0)
        {
            return State.PARTIAL;
        }

        return State.MISSING;
    }

    public PlannedBox getBox()
    {
        return this.box;
    }

    public List<Slot> getSlots()
    {
        return this.slots;
    }

    public List<Item> getItems()
    {
        return this.items;
    }

    /**
     * @return items inside the box that don't belong there (or too many of them)
     */
    public Map<String, Integer> getExtra()
    {
        return this.extra;
    }

    /**
     * @return true if the current contents of the box are known (seen in its screen, or the box is in the inventory)
     */
    public boolean isContentsKnown()
    {
        return this.contentsKnown;
    }

    public int getTotal()
    {
        return this.total;
    }

    public int getInBox()
    {
        return this.inBox;
    }

    public int getInInventory()
    {
        return this.inInventory;
    }

    public int getMissing()
    {
        return Math.max(0, this.total - this.inBox - this.inInventory);
    }

    public int getMissingTypes()
    {
        int count = 0;

        for (Item item : this.items)
        {
            if (item.missing() > 0)
            {
                ++count;
            }
        }

        return count;
    }

    public boolean isComplete()
    {
        return this.inBox >= this.total && this.extra.isEmpty();
    }
}
