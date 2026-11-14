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
        return rebase(kept, List.of(), requirements, mode, roundUpStacks, fillSpare, nextId);
    }

    /**
     * Updates a locked plan:
     * <ul>
     *   <li>{@code frozen} boxes (e.g. DONE) stay completely unchanged (same instance, contents, id and number).</li>
     *   <li>{@code expandable} boxes (e.g. FILLING or linked TODO) keep their id and number and the stacks of
     *       every item that is still needed (even if less of it is needed now: in "missing" count mode the counts
     *       go down while building, and what's packed must not disappear from the plan). Items that aren't needed
     *       at all anymore (removed from the list, ignored in Litematica) are taken out. Then their partial
     *       stacks and free slots absorb newly added requirements before new boxes are made.
     *       A started box left with nothing in it is dropped.</li>
     * </ul>
     */
    public static List<PlannedBox> rebase(List<PlannedBox> frozen, List<PlannedBox> expandable,
                                          List<ItemRequirement> requirements, PackMode mode,
                                          boolean roundUpStacks, boolean fillSpare, int nextId)
    {
        List<ItemRequirement> normalized = new ArrayList<>();

        for (ItemRequirement req : requirements)
        {
            if (req.count() <= 0 || req.maxStack() <= 0 || req.boxable() == false)
            {
                continue;
            }

            if (roundUpStacks && req.getRemainder() > 0)
            {
                int count = (req.getFullStacks() + 1) * req.maxStack();
                req = new ItemRequirement(req.id(), req.proto(), count, req.maxStack(), req.boxable(), req.sortKey());
            }

            normalized.add(req);
        }

        Set<String> needed = new java.util.HashSet<>();
        normalized.forEach(req -> needed.add(req.id()));

        // Started boxes without the items that aren't needed anymore
        List<PlannedBox> trimmed = new ArrayList<>(expandable.size());

        for (PlannedBox box : expandable)
        {
            List<PlannedStack> kept = new ArrayList<>();

            for (PlannedStack stack : box.getSlots())
            {
                if (needed.contains(stack.id()))
                {
                    kept.add(stack);
                }
            }

            PlannedBox b = box;

            if (kept.size() != box.getSlots().size())
            {
                b = new PlannedBox(kept);
                b.setId(box.getId());
                b.setIndex(box.getIndex());
            }

            trimmed.add(b);
        }

        Map<String, Long> covered = new HashMap<>();

        for (PlannedBox box : frozen)
        {
            box.getTotals().forEach((id, count) -> covered.merge(id, (long) count, Long::sum));
        }

        for (PlannedBox box : trimmed)
        {
            box.getTotals().forEach((id, count) -> covered.merge(id, (long) count, Long::sum));
        }

        Map<String, Long> remaining = new HashMap<>();

        for (ItemRequirement req : normalized)
        {
            long left = req.count() - covered.getOrDefault(req.id(), 0L);

            if (left > 0)
            {
                remaining.merge(req.id(), left, Long::sum);
            }
        }

        List<PlannedBox> result = new ArrayList<>(frozen.size() + expandable.size());
        Set<Integer> usedNumbers = new TreeSet<>();

        for (PlannedBox box : frozen)
        {
            usedNumbers.add(box.getIndex());
            result.add(box);
        }

        for (int b = 0; b < trimmed.size(); ++b)
        {
            PlannedBox box = trimmed.get(b);
            List<PlannedStack> slots = new ArrayList<>(box.getSlots());
            boolean changed = false;

            for (int i = 0; i < slots.size(); ++i)
            {
                PlannedStack stack = slots.get(i);
                long rem = remaining.getOrDefault(stack.id(), 0L);
                int space = stack.item().maxStack() - stack.count();

                if (rem > 0 && space > 0)
                {
                    int add = (int) Math.min(space, rem);
                    slots.set(i, new PlannedStack(stack.item(), stack.count() + add, stack.spare()));
                    remaining.put(stack.id(), rem - add);
                    changed = true;
                }
            }

            for (ItemRequirement req : normalized)
            {
                if (slots.size() >= PlannedBox.SLOTS)
                {
                    break;
                }

                long rem = remaining.getOrDefault(req.id(), 0L);

                while (rem > 0 && slots.size() < PlannedBox.SLOTS)
                {
                    int take = (int) Math.min(req.maxStack(), rem);
                    slots.add(new PlannedStack(req, take));
                    rem -= take;
                    remaining.put(req.id(), rem);
                    changed = true;
                }
            }

            PlannedBox updated = box;   // the same instance as before when nothing changed

            if (changed)
            {
                updated = new PlannedBox(slots);
                updated.setId(box.getId());
                updated.setIndex(box.getIndex());
            }

            if (updated.getSlots().isEmpty())
            {
                // Everything it had isn't needed anymore, and nothing new fits in it: no box
                continue;
            }

            usedNumbers.add(updated.getIndex());
            result.add(updated);
        }

        List<ItemRequirement> rest = new ArrayList<>();

        for (ItemRequirement req : normalized)
        {
            long left = remaining.getOrDefault(req.id(), 0L);

            if (left > 0)
            {
                rest.add(new ItemRequirement(req.id(), req.proto(), (int) left, req.maxStack(), req.boxable(), req.sortKey()));
                remaining.remove(req.id());
            }
        }

        List<PlannedBox> added = ShulkerPacker.packBoxes(rest, mode, roundUpStacks, fillSpare);
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
