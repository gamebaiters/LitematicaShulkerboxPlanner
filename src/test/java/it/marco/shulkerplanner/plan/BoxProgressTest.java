package it.marco.shulkerplanner.plan;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxProgressTest
{
    private static ItemRequirement req(String id, int count, int maxStack)
    {
        return new ItemRequirement(id, null, count, maxStack, true, 0);
    }

    /** A mixed box: 3 stacks of stone (64), 1 of glass (20), 2 of pearls (16 + 4) */
    private static PlannedBox box()
    {
        ItemRequirement stone = req("stone", 192, 64);
        ItemRequirement glass = req("glass", 20, 64);
        ItemRequirement pearl = req("pearl", 20, 16);

        return new PlannedBox(List.of(
                new PlannedStack(stone, 64), new PlannedStack(stone, 64), new PlannedStack(stone, 64),
                new PlannedStack(glass, 20),
                new PlannedStack(pearl, 16), new PlannedStack(pearl, 4)));
    }

    @Test
    void emptyBoxNothingCarried()
    {
        BoxProgress p = BoxProgress.compute(box(), Map.of(), Map.of(), false);

        assertEquals(232, p.getTotal());
        assertEquals(0, p.getInBox());
        assertEquals(232, p.getMissing());
        assertEquals(3, p.getMissingTypes());
        assertFalse(p.isContentsKnown());
        p.getSlots().forEach(s -> assertEquals(BoxProgress.State.MISSING, s.state()));
    }

    @Test
    void slotsFillInOrderBoxFirstThenInventory()
    {
        // 100 stone already inside, 70 more carried: slot 1 inside, slot 2 partly inside + carried, slot 3 partial
        BoxProgress p = BoxProgress.compute(box(), Map.of("stone", 100), Map.of("stone", 70, "glass", 64), true);
        List<BoxProgress.Slot> slots = p.getSlots();

        assertEquals(BoxProgress.State.IN_BOX, slots.get(0).state());
        assertEquals(36, slots.get(1).inBox());
        assertEquals(28, slots.get(1).inInventory());
        assertEquals(BoxProgress.State.IN_INVENTORY, slots.get(1).state());
        assertEquals(42, slots.get(2).inInventory());
        assertEquals(BoxProgress.State.PARTIAL, slots.get(2).state());
        assertEquals(22, slots.get(2).missing());

        // Glass: more carried than needed counts only what's needed
        assertEquals(BoxProgress.State.IN_INVENTORY, slots.get(3).state());
        assertEquals(20, slots.get(3).inInventory());

        assertEquals(100, p.getInBox());
        assertEquals(70 + 20, p.getInInventory());
        assertEquals(232 - 100 - 90, p.getMissing());
    }

    @Test
    void wrongAndExtraItemsAreReported()
    {
        BoxProgress p = BoxProgress.compute(box(), Map.of("stone", 200, "dirt", 5), Map.of(), true);

        assertEquals(Map.of("stone", 8, "dirt", 5), p.getExtra());
        assertFalse(p.isComplete());
    }

    @Test
    void completeBox()
    {
        BoxProgress p = BoxProgress.compute(box(), Map.of("stone", 192, "glass", 20, "pearl", 20), Map.of(), true);

        assertTrue(p.isComplete());
        assertEquals(0, p.getMissing());
        p.getSlots().forEach(s -> assertEquals(BoxProgress.State.IN_BOX, s.state()));
    }

    @Test
    void itemsAreSortedMostUrgentFirst()
    {
        BoxProgress p = BoxProgress.compute(box(), Map.of("glass", 20), Map.of("pearl", 20), true);

        assertEquals("stone", p.getItems().get(0).item().id());
        assertEquals(BoxProgress.State.MISSING, p.getItems().get(0).state());
        assertEquals(BoxProgress.State.IN_INVENTORY, p.getItems().get(1).state());
        assertEquals(BoxProgress.State.IN_BOX, p.getItems().get(2).state());
    }
}
