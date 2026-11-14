package it.marco.shulkerplanner.gui.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Records where texts, icons, buttons and chips are drawn during one frame, to find overlapping
 * or cut off elements automatically. Only enabled by the layout checks of the client game test.
 */
public final class LayoutProbe
{
    public enum Kind
    {
        TEXT,
        ITEM,
        BUTTON,
        CHIP,
        BAR,
        FIELD;

        /** Elements that are allowed to have other elements drawn inside them */
        boolean isContainer()
        {
            return this == BUTTON || this == CHIP || this == FIELD;
        }
    }

    public record Box(Kind kind, String label, int x, int y, int w, int h)
    {
        public boolean intersects(Box o)
        {
            return this.x < o.x + o.w && o.x < this.x + this.w && this.y < o.y + o.h && o.y < this.y + this.h;
        }

        public boolean contains(Box o)
        {
            return o.x >= this.x && o.y >= this.y && o.x + o.w <= this.x + this.w && o.y + o.h <= this.y + this.h;
        }

        @Override
        public String toString()
        {
            return this.kind + " '" + this.label + "' [" + this.x + "," + this.y + " " + this.w + "x" + this.h + "]";
        }
    }

    private static boolean enabled;
    private static final List<Box> BOXES = new ArrayList<>();
    private static final Deque<int[]> CLIPS = new ArrayDeque<>();

    private LayoutProbe()
    {
    }

    public static boolean isEnabled()
    {
        return enabled;
    }

    public static synchronized void start()
    {
        BOXES.clear();
        CLIPS.clear();
        enabled = true;
    }

    public static synchronized List<Box> stop()
    {
        enabled = false;
        List<Box> list = List.copyOf(new java.util.LinkedHashSet<>(BOXES));
        BOXES.clear();
        return list;
    }

    static void pushClip(int x, int y, int w, int h)
    {
        if (enabled)
        {
            CLIPS.push(new int[] { x, y, w, h });
        }
    }

    static void popClip()
    {
        if (enabled && CLIPS.isEmpty() == false)
        {
            CLIPS.pop();
        }
    }

    public static synchronized void add(Kind kind, @Nullable String label, int x, int y, int w, int h)
    {
        if (enabled == false || w <= 0 || h <= 0)
        {
            return;
        }

        int[] clip = CLIPS.peek();

        if (clip != null)
        {
            int x1 = Math.max(x, clip[0]);
            int y1 = Math.max(y, clip[1]);
            int x2 = Math.min(x + w, clip[0] + clip[2]);
            int y2 = Math.min(y + h, clip[1] + clip[3]);

            if (x2 <= x1 || y2 <= y1)
            {
                return;
            }

            x = x1;
            y = y1;
            w = x2 - x1;
            h = y2 - y1;
        }

        BOXES.add(new Box(kind, label != null ? label : "", x, y, w, h));
    }

    /**
     * @return a description of every pair of overlapping elements, and of every element outside the screen
     */
    public static List<String> findProblems(List<Box> boxes, int screenWidth, int screenHeight)
    {
        List<String> problems = new ArrayList<>();

        for (Box b : boxes)
        {
            if (b.x < 0 || b.y < 0 || b.x + b.w > screenWidth || b.y + b.h > screenHeight)
            {
                problems.add("outside the screen: " + b);
            }
        }

        for (int i = 0; i < boxes.size(); ++i)
        {
            Box a = boxes.get(i);

            for (int j = i + 1; j < boxes.size(); ++j)
            {
                Box b = boxes.get(j);

                if (a.intersects(b) == false || a.equals(b))
                {
                    continue;
                }

                if ((a.kind.isContainer() && a.contains(b)) || (b.kind.isContainer() && b.contains(a)))
                {
                    continue;
                }

                problems.add("overlap: " + a + " / " + b);
            }
        }

        return problems;
    }
}
