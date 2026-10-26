package it.marco.shulkerplanner.plan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Distributes item requirements into as few shulker boxes as possible.
 * <p>
 * Every mode respects the per-slot stack size limit of each item (64, 16 or 1 for most items)
 * and the 27 slots of a shulker box. Items that can not be put inside a shulker box are returned separately.
 */
public class ShulkerPacker
{
    private static final Comparator<ItemRequirement> ITEM_ORDER =
            Comparator.comparingInt(ItemRequirement::sortKey).thenComparing(ItemRequirement::id);

    private static final Comparator<PlannedStack> STACK_ORDER =
            Comparator.comparing(PlannedStack::spare)
                      .thenComparing(PlannedStack::item, ITEM_ORDER)
                      .thenComparing(Comparator.comparingInt(PlannedStack::count).reversed());

    public static ShulkerPlan pack(List<ItemRequirement> input, PackMode mode, boolean roundUpStacks, boolean fillSpare)
    {
        List<ItemRequirement> boxable = new ArrayList<>();
        List<ItemRequirement> loose = new ArrayList<>();
        List<ItemRequirement> all = new ArrayList<>();

        for (ItemRequirement req : input)
        {
            if (req.count() <= 0 || req.maxStack() <= 0)
            {
                continue;
            }

            if (roundUpStacks && req.getRemainder() > 0)
            {
                int count = (req.getFullStacks() + 1) * req.maxStack();
                req = new ItemRequirement(req.id(), req.proto(), count, req.maxStack(), req.boxable(), req.sortKey());
            }

            all.add(req);

            if (req.boxable())
            {
                boxable.add(req);
            }
            else
            {
                loose.add(req);
            }
        }

        boxable.sort(ITEM_ORDER);
        loose.sort(ITEM_ORDER);
        all.sort(ITEM_ORDER);

        List<List<PlannedStack>> boxes = switch (mode)
        {
            case SMART -> packSmart(boxable);
            case SEQUENTIAL -> packSequential(boxable);
            case ONE_TYPE -> packOneType(boxable);
        };

        if (fillSpare)
        {
            fillSpareSlots(boxes);
        }

        List<PlannedBox> result = new ArrayList<>(boxes.size());
        Map<String, Integer> duplicates = new HashMap<>();

        for (List<PlannedStack> slots : boxes)
        {
            PlannedBox box = new PlannedBox(slots);
            box.setIndex(result.size() + 1);
            box.setDuplicateOrdinal(duplicates.merge(box.getSignature(), 1, Integer::sum) - 1);
            result.add(box);
        }

        return new ShulkerPlan(result, loose, all);
    }

    /**
     * @return the full stacks first, then the possible partial stack
     */
    private static List<PlannedStack> createStacks(ItemRequirement req)
    {
        int full = req.getFullStacks();
        int remainder = req.getRemainder();
        List<PlannedStack> list = new ArrayList<>(full + 1);

        for (int i = 0; i < full; ++i)
        {
            list.add(new PlannedStack(req, req.maxStack()));
        }

        if (remainder > 0)
        {
            list.add(new PlannedStack(req, remainder));
        }

        return list;
    }

    private static List<List<PlannedStack>> chunk(List<PlannedStack> stacks)
    {
        List<List<PlannedStack>> boxes = new ArrayList<>();

        for (int i = 0; i < stacks.size(); i += PlannedBox.SLOTS)
        {
            boxes.add(new ArrayList<>(stacks.subList(i, Math.min(i + PlannedBox.SLOTS, stacks.size()))));
        }

        return boxes;
    }

    private static List<List<PlannedStack>> packSequential(List<ItemRequirement> items)
    {
        List<PlannedStack> stacks = new ArrayList<>();

        for (ItemRequirement req : items)
        {
            stacks.addAll(createStacks(req));
        }

        return chunk(stacks);
    }

    private static List<List<PlannedStack>> packOneType(List<ItemRequirement> items)
    {
        List<List<PlannedStack>> boxes = new ArrayList<>();

        for (ItemRequirement req : items)
        {
            boxes.addAll(chunk(createStacks(req)));
        }

        return boxes;
    }

    /**
     * 1. Every item that fills one or more whole boxes with full stacks gets those boxes for itself.
     * 2. The leftover stacks of each item form a group (at most 27 slots) which is kept together when possible.
     * 3. The groups are packed into the minimum possible amount of boxes, ceil(slots / 27),
     *    with "best fit decreasing". Groups that don't fit anywhere as a whole get split into the
     *    boxes with the most free space, so no extra box is ever used and only few items get split.
     */
    private static List<List<PlannedStack>> packSmart(List<ItemRequirement> items)
    {
        List<List<PlannedStack>> pureBoxes = new ArrayList<>();
        List<List<PlannedStack>> groups = new ArrayList<>();
        int groupSlots = 0;

        for (ItemRequirement req : items)
        {
            List<PlannedStack> stacks = createStacks(req);
            int pureBoxCount = req.getFullStacks() / PlannedBox.SLOTS;
            int pureSlots = pureBoxCount * PlannedBox.SLOTS;

            if (pureBoxCount > 0)
            {
                pureBoxes.addAll(chunk(stacks.subList(0, pureSlots)));
            }

            if (stacks.size() > pureSlots)
            {
                List<PlannedStack> group = new ArrayList<>(stacks.subList(pureSlots, stacks.size()));
                groups.add(group);
                groupSlots += group.size();
            }
        }

        int binCount = (groupSlots + PlannedBox.SLOTS - 1) / PlannedBox.SLOTS;
        List<List<PlannedStack>> bins = new ArrayList<>(binCount);

        for (int i = 0; i < binCount; ++i)
        {
            bins.add(new ArrayList<>());
        }

        // Biggest groups first, ties in item order so that the result is stable
        groups.sort(Comparator.<List<PlannedStack>>comparingInt(List::size).reversed()
                              .thenComparing(g -> g.get(0).item(), ITEM_ORDER));

        List<List<PlannedStack>> deferred = new ArrayList<>();

        for (List<PlannedStack> group : groups)
        {
            int best = -1;
            int bestFree = Integer.MAX_VALUE;

            for (int i = 0; i < bins.size(); ++i)
            {
                int free = PlannedBox.SLOTS - bins.get(i).size();

                if (free >= group.size() && free < bestFree)
                {
                    best = i;
                    bestFree = free;
                }
            }

            if (best >= 0)
            {
                bins.get(best).addAll(group);
            }
            else
            {
                deferred.add(group);
            }
        }

        for (List<PlannedStack> group : deferred)
        {
            int index = 0;

            while (index < group.size())
            {
                List<PlannedStack> target = null;
                int maxFree = 0;

                for (List<PlannedStack> bin : bins)
                {
                    int free = PlannedBox.SLOTS - bin.size();

                    if (free > maxFree)
                    {
                        maxFree = free;
                        target = bin;
                    }
                }

                if (target == null)
                {
                    // Should never happen, the bin count is calculated from the total slot count
                    target = new ArrayList<>();
                    bins.add(target);
                    maxFree = PlannedBox.SLOTS;
                }

                int count = Math.min(maxFree, group.size() - index);
                target.addAll(group.subList(index, index + count));
                index += count;
            }
        }

        for (List<PlannedStack> bin : bins)
        {
            bin.sort(STACK_ORDER);
        }

        bins.removeIf(List::isEmpty);
        bins.sort(Comparator.comparing(bin -> bin.get(0).item(), ITEM_ORDER));

        List<List<PlannedStack>> result = new ArrayList<>(pureBoxes.size() + bins.size());
        result.addAll(pureBoxes);
        result.addAll(bins);

        return result;
    }

    /**
     * Fills the empty slots of every box with extra full stacks of the item that box contains the most of.
     */
    private static void fillSpareSlots(List<List<PlannedStack>> boxes)
    {
        for (List<PlannedStack> box : boxes)
        {
            if (box.isEmpty() || box.size() >= PlannedBox.SLOTS)
            {
                continue;
            }

            Map<ItemRequirement, Integer> counts = new HashMap<>();

            for (PlannedStack stack : box)
            {
                counts.merge(stack.item(), stack.count(), Integer::sum);
            }

            ItemRequirement main = null;
            int max = -1;

            for (Map.Entry<ItemRequirement, Integer> entry : counts.entrySet())
            {
                if (main == null || entry.getValue() > max ||
                    (entry.getValue() == max && ITEM_ORDER.compare(entry.getKey(), main) < 0))
                {
                    max = entry.getValue();
                    main = entry.getKey();
                }
            }

            while (box.size() < PlannedBox.SLOTS)
            {
                box.add(new PlannedStack(main, main.maxStack(), true));
            }
        }
    }
}
