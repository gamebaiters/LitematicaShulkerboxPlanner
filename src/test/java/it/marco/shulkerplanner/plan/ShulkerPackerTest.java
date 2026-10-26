package it.marco.shulkerplanner.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the packing algorithms without Minecraft: the item stack prototype is not used by the packer.
 */
class ShulkerPackerTest
{
    private static ItemRequirement req(String id, int count, int maxStack, int sortKey)
    {
        return new ItemRequirement(id, null, count, maxStack, true, sortKey);
    }

    private static int slotsNeeded(List<ItemRequirement> reqs)
    {
        int slots = 0;

        for (ItemRequirement r : reqs)
        {
            if (r.boxable())
            {
                slots += r.getSlotCount();
            }
        }

        return slots;
    }

    private static void assertValid(List<ItemRequirement> input, ShulkerPlan plan, PackMode mode, boolean roundUp, boolean spare)
    {
        Map<String, Long> expected = new HashMap<>();

        for (ItemRequirement r : input)
        {
            if (r.boxable() && r.count() > 0)
            {
                long count = roundUp ? (long) Math.ceilDiv(r.count(), r.maxStack()) * r.maxStack() : r.count();
                expected.merge(r.id(), count, Long::sum);
            }
        }

        Map<String, Long> packed = new HashMap<>();
        Map<String, Long> spares = new HashMap<>();

        for (PlannedBox box : plan.getBoxes())
        {
            assertTrue(box.getUsedSlots() <= PlannedBox.SLOTS, "box over 27 slots");
            assertTrue(box.getUsedSlots() > 0, "empty box");

            for (PlannedStack stack : box.getSlots())
            {
                assertTrue(stack.count() > 0, "empty stack");
                assertTrue(stack.count() <= stack.item().maxStack(), "stack over the max stack size");

                if (stack.spare())
                {
                    spares.merge(stack.id(), (long) stack.count(), Long::sum);
                }
                else
                {
                    packed.merge(stack.id(), (long) stack.count(), Long::sum);
                }
            }

            if (spare)
            {
                assertEquals(PlannedBox.SLOTS, box.getUsedSlots(), "spare filling should fill every box");
            }

            if (mode == PackMode.ONE_TYPE)
            {
                assertTrue(box.isPure(), "one type mode must not mix items");
            }
        }

        assertEquals(expected, packed, "items lost or duplicated");

        if (spare == false)
        {
            assertTrue(spares.isEmpty());
        }

        if (mode != PackMode.ONE_TYPE)
        {
            List<ItemRequirement> effective = new ArrayList<>();

            for (ItemRequirement r : input)
            {
                int count = roundUp ? Math.ceilDiv(r.count(), r.maxStack()) * r.maxStack() : r.count();
                effective.add(req(r.id(), count, r.maxStack(), r.sortKey()));
            }

            int minBoxes = Math.ceilDiv(slotsNeeded(effective), PlannedBox.SLOTS);
            assertEquals(minBoxes, plan.getBoxCount(), "not the minimum amount of boxes for " + mode);

            // Every box except at most one... in sequential mode only the last one may be partial
            if (mode == PackMode.SEQUENTIAL && spare == false)
            {
                for (int i = 0; i < plan.getBoxCount() - 1; ++i)
                {
                    assertEquals(PlannedBox.SLOTS, plan.getBoxes().get(i).getUsedSlots(), "only the last box may be partial");
                }
            }

            // Total free slots across all boxes must be less than one box
            if (spare == false)
            {
                assertTrue(plan.getFreeSlots() < PlannedBox.SLOTS, "more than one box worth of free slots");
            }
        }
    }

    @Test
    void exactlyOneFullBoxOfStone()
    {
        ShulkerPlan plan = ShulkerPacker.pack(List.of(req("stone", 27 * 64, 64, 1)), PackMode.SMART, false, false);

        assertEquals(1, plan.getBoxCount());
        assertEquals(27, plan.getBoxes().get(0).getUsedSlots());
        assertTrue(plan.getBoxes().get(0).isPure());
        assertEquals(27 * 64, plan.getTotalItems());
    }

    @Test
    void respectsStackSizes()
    {
        // 20 eggs (max 16) = 2 slots, 3 beds (max 1) = 3 slots, 70 stone = 2 slots
        List<ItemRequirement> input = List.of(req("egg", 20, 16, 3), req("bed", 3, 1, 2), req("stone", 70, 64, 1));
        ShulkerPlan plan = ShulkerPacker.pack(input, PackMode.SMART, false, false);

        assertEquals(1, plan.getBoxCount());
        assertEquals(7, plan.getTotalStacks());
        assertValid(input, plan, PackMode.SMART, false, false);
    }

    @Test
    void smartKeepsBulkItemsInPureBoxes()
    {
        // 2.5 boxes of stone, 1.2 boxes of dirt, some glass
        List<ItemRequirement> input = List.of(req("stone", 64 * 67 + 10, 64, 1), req("dirt", 64 * 33, 64, 2), req("glass", 300, 64, 3));
        ShulkerPlan plan = ShulkerPacker.pack(input, PackMode.SMART, false, false);

        assertValid(input, plan, PackMode.SMART, false, false);

        int pureStone = 0;
        int pureDirt = 0;

        for (PlannedBox box : plan.getBoxes())
        {
            if (box.isPure() && box.getUsedSlots() == 27)
            {
                if (box.getTotals().containsKey("stone")) ++pureStone;
                if (box.getTotals().containsKey("dirt")) ++pureDirt;
            }
        }

        assertEquals(2, pureStone);
        assertEquals(1, pureDirt);
    }

    @Test
    void roundUpMakesFullStacks()
    {
        List<ItemRequirement> input = List.of(req("stone", 70, 64, 1), req("pearl", 5, 16, 2));
        ShulkerPlan plan = ShulkerPacker.pack(input, PackMode.SMART, true, false);

        assertValid(input, plan, PackMode.SMART, true, false);
        assertEquals(128 + 16, plan.getTotalItems());

        for (PlannedBox box : plan.getBoxes())
        {
            for (PlannedStack stack : box.getSlots())
            {
                assertEquals(stack.item().maxStack(), stack.count());
            }
        }
    }

    @Test
    void spareFillsEveryBox()
    {
        List<ItemRequirement> input = List.of(req("stone", 1000, 64, 1), req("glass", 100, 64, 2));
        ShulkerPlan plan = ShulkerPacker.pack(input, PackMode.SMART, false, true);

        assertValid(input, plan, PackMode.SMART, false, true);
    }

    @Test
    void unboxableItemsAreLoose()
    {
        List<ItemRequirement> input = List.of(new ItemRequirement("shulker_box", null, 3, 1, false, 5), req("stone", 64, 64, 1));
        ShulkerPlan plan = ShulkerPacker.pack(input, PackMode.SMART, false, false);

        assertEquals(1, plan.getLooseItems().size());
        assertEquals(1, plan.getBoxCount());
        assertEquals(64, plan.getTotalItems());
    }

    @Test
    void identicalBoxesGetDuplicateOrdinals()
    {
        ShulkerPlan plan = ShulkerPacker.pack(List.of(req("stone", 27 * 64 * 3, 64, 1)), PackMode.SMART, false, false);

        assertEquals(3, plan.getBoxCount());
        assertEquals(0, plan.getBoxes().get(0).getDuplicateOrdinal());
        assertEquals(1, plan.getBoxes().get(1).getDuplicateOrdinal());
        assertEquals(2, plan.getBoxes().get(2).getDuplicateOrdinal());
        assertEquals(plan.getBoxes().get(0).getSignature(), plan.getBoxes().get(2).getSignature());
    }

    @Test
    void randomizedInvariants()
    {
        Random rand = new Random(12345);
        int[] stackSizes = { 64, 64, 64, 16, 1 };

        for (int run = 0; run < 2000; ++run)
        {
            int types = 1 + rand.nextInt(40);
            List<ItemRequirement> input = new ArrayList<>();

            for (int i = 0; i < types; ++i)
            {
                int maxStack = stackSizes[rand.nextInt(stackSizes.length)];
                int count = switch (rand.nextInt(4))
                {
                    case 0 -> 1 + rand.nextInt(maxStack * 2);
                    case 1 -> 1 + rand.nextInt(maxStack * 27);
                    case 2 -> 1 + rand.nextInt(maxStack * 27 * 4);
                    default -> maxStack * (1 + rand.nextInt(60));
                };

                input.add(req("item" + i, count, maxStack, rand.nextInt(1000)));
            }

            for (PackMode mode : PackMode.values())
            {
                boolean roundUp = rand.nextBoolean();
                boolean spare = rand.nextInt(4) == 0;
                ShulkerPlan plan = ShulkerPacker.pack(input, mode, roundUp, spare);
                assertValid(input, plan, mode, roundUp, spare);
            }
        }
    }

    @Test
    void smartSplitsFewItems()
    {
        // Compare how many item types get split across boxes: smart should never be worse than sequential
        Random rand = new Random(777);
        long smartSplits = 0;
        long seqSplits = 0;

        for (int run = 0; run < 300; ++run)
        {
            List<ItemRequirement> input = new ArrayList<>();
            int types = 5 + rand.nextInt(30);

            for (int i = 0; i < types; ++i)
            {
                input.add(req("item" + i, 1 + rand.nextInt(64 * 20), 64, i));
            }

            smartSplits += countSplits(ShulkerPacker.pack(input, PackMode.SMART, false, false));
            seqSplits += countSplits(ShulkerPacker.pack(input, PackMode.SEQUENTIAL, false, false));
        }

        assertTrue(smartSplits <= seqSplits, "smart " + smartSplits + " vs sequential " + seqSplits);
    }

    /**
     * @return the number of item types whose non-pure stacks are spread over more than one mixed box
     */
    private static int countSplits(ShulkerPlan plan)
    {
        Map<String, Integer> boxesPerItem = new HashMap<>();

        for (PlannedBox box : plan.getBoxes())
        {
            if (box.isPure() && box.getUsedSlots() == PlannedBox.SLOTS)
            {
                continue;
            }

            for (String id : box.getTotals().keySet())
            {
                boxesPerItem.merge(id, 1, Integer::sum);
            }
        }

        int splits = 0;

        for (int count : boxesPerItem.values())
        {
            if (count > 1)
            {
                splits += count - 1;
            }
        }

        return splits;
    }
}
