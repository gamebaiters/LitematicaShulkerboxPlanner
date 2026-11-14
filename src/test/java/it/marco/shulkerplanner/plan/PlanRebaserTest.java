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
