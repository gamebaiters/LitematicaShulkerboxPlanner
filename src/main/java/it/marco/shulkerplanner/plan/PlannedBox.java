package it.marco.shulkerplanner.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * One planned shulker box with up to 27 slots.
 */
public class PlannedBox
{
    public static final int SLOTS = 27;

    private final List<PlannedStack> slots;
    private final Map<String, Integer> totals = new LinkedHashMap<>();
    private final Map<String, ItemRequirement> items = new LinkedHashMap<>();
    private final String signature;
    private final boolean pure;
    private final long itemCount;
    private int index;
    private int duplicateOrdinal;

    public PlannedBox(List<PlannedStack> slots)
    {
        this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
        long count = 0;

        for (PlannedStack stack : this.slots)
        {
            this.totals.merge(stack.id(), stack.count(), Integer::sum);
            this.items.putIfAbsent(stack.id(), stack.item());
            count += stack.count();
        }

        this.itemCount = count;
        this.pure = this.totals.size() == 1;
        this.signature = createSignature(this.totals);
    }

    public static String createSignature(Map<String, Integer> totals)
    {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Integer> entry : new TreeMap<>(totals).entrySet())
        {
            if (sb.length() > 0)
            {
                sb.append(',');
            }

            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }

        return sb.toString();
    }

    public List<PlannedStack> getSlots()
    {
        return this.slots;
    }

    public int getUsedSlots()
    {
        return this.slots.size();
    }

    public int getFreeSlots()
    {
        return SLOTS - this.slots.size();
    }

    /**
     * @return item id to the total item count of that item in this box, in slot order
     */
    public Map<String, Integer> getTotals()
    {
        return this.totals;
    }

    public Map<String, ItemRequirement> getItems()
    {
        return this.items;
    }

    public String getSignature()
    {
        return this.signature;
    }

    public boolean isPure()
    {
        return this.pure;
    }

    public long getItemCount()
    {
        return this.itemCount;
    }

    /**
     * @return the 1-based box number
     */
    public int getIndex()
    {
        return this.index;
    }

    void setIndex(int index)
    {
        this.index = index;
    }

    /**
     * @return how many earlier boxes in the plan have the exact same contents
     */
    public int getDuplicateOrdinal()
    {
        return this.duplicateOrdinal;
    }

    void setDuplicateOrdinal(int ordinal)
    {
        this.duplicateOrdinal = ordinal;
    }
}
