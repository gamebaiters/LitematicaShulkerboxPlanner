package it.marco.shulkerplanner.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanRebaserTest
{
    private static ItemRequirement req(String id, int count, int sortKey)
    {
        return new ItemRequirement(id, null, count, 64, true, sortKey);
    }

    private static Map<String, Long> totals(List<PlannedBox> boxes)
    {
        Map<String, Long> map = new HashMap<>();
        boxes.forEach(b -> b.getTotals().forEach((id, c) -> map.merge(id, (long) c, Long::sum)));
        return map;
    }

    @Test
    void keptBoxesStayAndTheRestIsRepacked()
    {
        List<ItemRequirement> reqs = List.of(req("stone", 27 * 64 * 2, 1), req("glass", 500, 2), req("torch", 100, 3));
        List<PlannedBox> plan = ShulkerPacker.packBoxes(reqs, PackMode.SMART, false, false);
        assertEquals(3, plan.size());

        // Box #2 was started, then the material list changed: less stone, more glass
        PlannedBox started = plan.get(1);
        List<ItemRequirement> changed = List.of(req("stone", 27 * 64 + 100, 1), req("glass", 900, 2), req("torch", 100, 3));
        List<PlannedBox> rebased = PlanRebaser.rebase(List.of(started), changed, PackMode.SMART, false, false, 100);

        assertTrue(rebased.contains(started));
        assertSame(started, rebased.stream().filter(b -> b.getId() == started.getId()).findFirst().orElseThrow());
        assertEquals(2, started.getIndex(), "number kept");

        Map<String, Long> t = totals(rebased);
        assertTrue(t.get("stone") >= 27 * 64 + 100);
        assertEquals(900L, t.get("glass"));
        assertEquals(100L, t.get("torch"));

        List<Integer> numbers = new ArrayList<>();
        for (PlannedBox b : rebased)
        {
            numbers.add(b.getIndex());
            if (b != started)
            {
                assertTrue(b.getId() >= 100, "new ids");
            }
        }
        for (int i = 0; i < numbers.size(); ++i)
        {
            assertEquals(i + 1, numbers.get(i), "numbers 1..n without gaps: " + numbers);
        }
    }

    @Test
    void expandableBoxAbsorbsAddedItemsIntoPartialStacksAndFreeSlots()
    {
        List<ItemRequirement> initial = List.of(req("stone", 32, 1), req("glass", 64, 2));
        List<PlannedBox> plan = ShulkerPacker.packBoxes(initial, PackMode.SMART, false, false);
        assertEquals(1, plan.size());

        PlannedBox filling = plan.get(0);
        // Material list updated: +48 stone (total 80) and +100 lantern
        List<ItemRequirement> updated = List.of(req("stone", 80, 1), req("glass", 64, 2), req("lantern", 100, 3));
        List<PlannedBox> rebased = PlanRebaser.rebase(List.of(), List.of(filling), updated, PackMode.SMART, false, false, 100);

        assertEquals(1, rebased.size(), "new items fit inside the existing box's free slots");
        PlannedBox box = rebased.get(0);
        assertEquals(filling.getId(), box.getId());
        assertEquals(filling.getIndex(), box.getIndex());
        assertEquals(80, box.getTotals().get("stone"));
        assertEquals(64, box.getTotals().get("glass"));
        assertEquals(100, box.getTotals().get("lantern"));
    }

    @Test
    void ignoredItemLeavesTheStartedBoxesButNotTheDoneOnes()
    {
        // #1 done (stone + glass), #2 being filled (glass + torch + lantern)
        PlannedBox done = new PlannedBox(List.of(new PlannedStack(req("stone", 64, 1), 64), new PlannedStack(req("glass", 64, 2), 64)));
        done.setId(1);
        done.setIndex(1);
        PlannedBox filling = new PlannedBox(List.of(new PlannedStack(req("glass", 64, 2), 64), new PlannedStack(req("glass", 64, 2), 36),
                                                    new PlannedStack(req("torch", 64, 3), 64), new PlannedStack(req("lantern", 64, 4), 20)));
        filling.setId(2);
        filling.setIndex(2);

        // Glass ignored in Litematica: it's not needed anymore
        List<ItemRequirement> now = List.of(req("stone", 64, 1), req("torch", 64, 3), req("lantern", 20, 4));
        List<PlannedBox> rebased = PlanRebaser.rebase(List.of(done), List.of(filling), now, PackMode.SMART, false, false, 100);

        assertEquals(2, rebased.size(), "no new box: " + rebased);
        assertSame(done, rebased.get(0), "the done box stays exactly as it is");
        PlannedBox box2 = rebased.get(1);
        assertEquals(2, box2.getId());
        assertEquals(2, box2.getIndex());
        assertEquals(null, box2.getTotals().get("glass"), "ignored glass is taken out of the started box");
        assertEquals(64, box2.getTotals().get("torch"));
        assertEquals(20, box2.getTotals().get("lantern"));
    }

    @Test
    void lessOfAnItemStillNeededKeepsWhatIsPacked()
    {
        // "Missing" count mode while building: fewer torches missing, but the packed ones stay in the plan
        PlannedBox filling = new PlannedBox(List.of(new PlannedStack(req("torch", 64, 1), 64), new PlannedStack(req("torch", 64, 1), 64)));
        filling.setId(5);
        filling.setIndex(1);
        List<PlannedBox> rebased = PlanRebaser.rebase(List.of(), List.of(filling), List.of(req("torch", 40, 1)), PackMode.SMART, false, false, 100);

        assertEquals(1, rebased.size());
        assertSame(filling, rebased.get(0), "nothing changed in the started box");
    }

    @Test
    void startedBoxWithNothingLeftIsDropped()
    {
        PlannedBox only = new PlannedBox(List.of(new PlannedStack(req("glass", 64, 1), 64)));
        only.setId(3);
        only.setIndex(1);
        PlannedBox other = new PlannedBox(List.of(new PlannedStack(req("stone", 64, 2), 64)));
        other.setId(4);
        other.setIndex(2);
        List<PlannedBox> rebased = PlanRebaser.rebase(List.of(), List.of(only, other), List.of(req("stone", 64, 2)), PackMode.SMART, false, false, 100);

        assertEquals(1, rebased.size(), "" + rebased);
        assertEquals(4, rebased.get(0).getId());
    }

    @Test
    void countModeRequired()
    {
        assertEquals(100, CountMode.TOTAL.required(100, 40, 1));
        assertEquals(40, CountMode.MISSING.required(100, 40, 1));
        // 3 copies: the one being built is missing 40, two more copies in full
        assertEquals(240, CountMode.MISSING.required(100, 40, 3));
        assertEquals(300, CountMode.TOTAL.required(100, 40, 3));
        assertEquals(0, CountMode.MISSING.required(100, -5, 1));
        assertEquals(100, CountMode.MISSING.required(100, 500, 1));
    }
}
