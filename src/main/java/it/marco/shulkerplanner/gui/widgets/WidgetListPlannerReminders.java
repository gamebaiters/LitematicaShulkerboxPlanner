package it.marco.shulkerplanner.gui.widgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.Reminder;
import it.marco.shulkerplanner.gui.GuiPlannerReminders;

public class WidgetListPlannerReminders extends WidgetListBase<Reminder, WidgetPlannerReminderEntry>
{
    private final GuiPlannerReminders gui;

    public WidgetListPlannerReminders(int x, int y, int width, int height, GuiPlannerReminders gui)
    {
        super(x, y, width, height, null);

        this.gui = gui;
        this.browserEntryHeight = 22;
    }

    public GuiPlannerReminders getGui()
    {
        return this.gui;
    }

    @Override
    protected Collection<Reminder> getAllEntries()
    {
        PlannerProject project = this.gui.getProject();

        if (project == null)
        {
            return List.of();
        }

        List<Reminder> open = new ArrayList<>();
        List<Reminder> done = new ArrayList<>();

        // Open reminders first, then the completed ones, both in the order they were added
        for (Reminder reminder : project.getReminders())
        {
            if (reminder.isDone())
            {
                if (GuiPlannerReminders.getShowCompleted())
                {
                    done.add(reminder);
                }
            }
            else
            {
                open.add(reminder);
            }
        }

        open.addAll(done);
        return open;
    }

    @Override
    protected WidgetPlannerReminderEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, @Nullable Reminder entry)
    {
        return new WidgetPlannerReminderEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), isOdd, entry, listIndex, this);
    }
}
