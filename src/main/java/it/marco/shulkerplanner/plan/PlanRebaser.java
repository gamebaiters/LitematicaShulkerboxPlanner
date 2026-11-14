package it.marco.shulkerplanner.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Updates a locked plan without touching the boxes that were already started:
 * the kept boxes stay exactly as they are (contents, id and number), and only what they don't cover
 * of the new requirements is packed into new boxes.
 */
public class PlanRebaser
{
    /**
     * @param kept         the boxes to keep unchanged, in plan order
     * @param requirements the new requirements of the whole plan
     * @param nextId       the first id to use for new boxes, must be greater than every kept id
     * @return the kept boxes followed by the new boxes. New boxes get the free box numbers.
     */
    public static List<PlannedBox> rebase(List<PlannedBox> kept, List<ItemRequirement> requirements, PackMode mode,
                                          boolean roundUpStacks, boolean fillSpare, int nextId)
    {
        Map<String, Long> covered = new HashMap<>();

        for (PlannedBox box : kept)
        {
            box.getTotals().forEach((id, count) -> covered.merge(id, (long) count, Long::sum));
        }

        List<ItemRequirement> rest = new ArrayList<>();

        for (ItemRequirement req : requirements)
        {
            if (req.boxable() == false)
            {
                continue;
            }

            long left = req.count() - covered.getOrDefault(req.id(), 0L);

            if (left > 0)
            {
                rest.add(new ItemRequirement(req.id(), req.proto(), (int) left, req.maxStack(), req.boxable(), req.sortKey()));
            }
        }

        List<PlannedBox> added = ShulkerPacker.packBoxes(rest, mode, roundUpStacks, fillSpare);
        List<PlannedBox> result = new ArrayList<>(kept.size() + added.size());
        Set<Integer> usedNumbers = new TreeSet<>();

        for (PlannedBox box : kept)
        {
            usedNumbers.add(box.getIndex());
            result.add(box);
        }

        int number = 1;
        int id = nextId;

        for (PlannedBox box : added)
        {
            while (usedNumbers.contains(number))
            {
                ++number;
            }

            box.setIndex(number);
            box.setId(id++);
            usedNumbers.add(number);
            result.add(box);
        }

        result.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
        assignDuplicateOrdinals(result);

        return result;
    }

    public static void assignDuplicateOrdinals(List<PlannedBox> boxes)
    {
        Map<String, Integer> duplicates = new HashMap<>();

        for (PlannedBox box : boxes)
        {
            box.setDuplicateOrdinal(duplicates.merge(box.getSignature(), 1, Integer::sum) - 1);
        }
    }
}
