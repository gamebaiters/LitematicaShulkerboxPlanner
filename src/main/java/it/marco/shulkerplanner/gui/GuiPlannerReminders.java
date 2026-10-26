package it.marco.shulkerplanner.gui;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.client.input.KeyEvent;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.input.ScanCodes;

import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.Reminder;
import it.marco.shulkerplanner.gui.widgets.WidgetListPlannerReminders;
import it.marco.shulkerplanner.gui.widgets.WidgetPlannerReminderEntry;

public class GuiPlannerReminders extends GuiPlannerBase<Reminder, WidgetPlannerReminderEntry, WidgetListPlannerReminders>
{
    private static boolean showCompleted = true;
    private static String draft = "";
    @Nullable private static Reminder editing;

    @Nullable private GuiTextFieldGeneric textField;

    public GuiPlannerReminders()
    {
        super(PlannerTab.REMINDERS);
    }

    public static boolean getShowCompleted()
    {
        return showCompleted;
    }

    public boolean isEditing(Reminder reminder)
    {
        return editing == reminder;
    }

    public void startEditing(Reminder reminder)
    {
        editing = reminder;
        draft = reminder.getText();
        this.initGui();

        if (this.textField != null)
        {
            this.textField.setFocused(true);
        }
    }

    @Override
    protected WidgetListPlannerReminders createListWidget(int listX, int listY)
    {
        return new WidgetListPlannerReminders(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    @Override
    public void initGui()
    {
        PlannerProject project = this.getProject();

        // The edited reminder may have been deleted, or the project changed
        if (editing != null && (project == null || project.getReminders().contains(editing) == false))
        {
            editing = null;
            draft = "";
        }

        super.initGui();
    }

    @Override
    protected void createOptionButtons()
    {
        PlannerProject project = this.getProject();

        if (project == null)
        {
            return;
        }

        int width = Math.max(120, Math.min(320, this.getScreenWidth() - 260));
        this.textField = this.addFlowTextField((x, y, w, h) -> new GuiTextFieldGeneric(x, y, w, h, this.font), width, textField -> {
            draft = textField.getValueWrapper();
            return true;
        });
        this.textField.setMaxLengthWrapper(256);
        this.textField.setValueWrapper(draft);

        String key = editing != null ? "shulkerplanner.gui.button.save_reminder" : "shulkerplanner.gui.button.add_reminder";
        ButtonGeneric button = new ButtonGeneric(0, 0, -1, 20, StringUtils.translate(key),
                                                 StringUtils.translate("shulkerplanner.gui.button.add_reminder.hover"));
        this.addFlowButton(button, (btn, mb) -> this.submit());

        if (editing != null)
        {
            button = new ButtonGeneric(0, 0, -1, 20, StringUtils.translate("shulkerplanner.gui.button.cancel"));
            this.addFlowButton(button, (btn, mb) -> {
                editing = null;
                draft = "";
                this.initGui();
            });
        }

        button = new ButtonGeneric(0, 0, -1, 20, StringUtils.translate("shulkerplanner.gui.button.remove_completed"));
        this.addFlowButton(button, (btn, mb) -> {
            project.getReminders().removeIf(Reminder::isDone);
            this.onProjectChanged();
        });

        button = this.createOnOffButton("shulkerplanner.gui.button.show_completed", showCompleted, null);
        this.addFlowButton(button, (btn, mb) -> {
            showCompleted = ! showCompleted;
            this.initGui();
        });
    }

    private void submit()
    {
        PlannerProject project = this.getProject();
        String text = draft.trim();

        if (project == null || text.isEmpty())
        {
            return;
        }

        if (editing != null)
        {
            editing.setText(text);
            editing = null;
        }
        else if (project.hasOpenReminder(text))
        {
            this.addMessage(MessageType.WARNING, 2000, "shulkerplanner.message.reminder_exists");
            return;
        }
        else
        {
            project.addReminder(new Reminder(text, null));
            this.addMessage(MessageType.SUCCESS, 1500, "shulkerplanner.message.reminder_added", text);
        }

        draft = "";
        this.onProjectChanged();

        if (this.textField != null)
        {
            this.textField.setFocused(true);
        }
    }

    @Override
    public boolean onKeyTyped(KeyEvent input)
    {
        if (this.textField != null && this.textField.isFocused() &&
            (input.key() == ScanCodes.SCAN_RETURN || input.key() == ScanCodes.SCAN_KP_ENTER))
        {
            this.submit();
            return true;
        }

        return super.onKeyTyped(input);
    }

    @Override
    protected List<String> getSummaryLines()
    {
        List<String> lines = new ArrayList<>();
        PlannerProject project = this.getProject();

        if (project == null)
        {
            return lines;
        }

        int total = project.getReminders().size();
        int open = project.getOpenReminderCount();

        if (total == 0)
        {
            lines.add(GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.gui.label.reminders.empty"));
        }
        else
        {
            lines.add(StringUtils.translate("shulkerplanner.gui.label.reminders.summary", total - open, total, open));
        }

        return lines;
    }
}
