package it.marco.shulkerplanner.gui.widgets;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.Reminder;
import it.marco.shulkerplanner.gui.ui.PlannerButton;
import it.marco.shulkerplanner.gui.ui.Theme;
import it.marco.shulkerplanner.gui.ui.Ui;
import it.marco.shulkerplanner.gui.GuiPlannerReminders;
import it.marco.shulkerplanner.util.Format;

public class WidgetPlannerReminderEntry extends WidgetListEntryBase<Reminder>
{
    private final WidgetListPlannerReminders list;
    private final boolean isOdd;
    private final int textX;
    private int textMaxWidth;
    @Nullable private ItemStack icon;

    public WidgetPlannerReminderEntry(int x, int y, int width, int height, boolean isOdd,
                                      @Nullable Reminder entry, int listIndex, WidgetListPlannerReminders list)
    {
        super(x, y, width, height, entry, listIndex);

        this.list = list;
        this.isOdd = isOdd;

        GuiPlannerReminders gui = list.getGui();
        PlannerProject project = gui.getProject();

        if (entry == null || project == null)
        {
            this.textX = x + 4;
            return;
        }

        // Done toggle on the left
        int by = y + (height - PlannerButton.HEIGHT) / 2;
        PlannerButton toggle = new PlannerButton(x + 2, by, 20, entry.isDone() ? "✔" : "○", PlannerButton.Style.SECONDARY,
                                                 StringUtils.translate("shulkerplanner.gui.button.reminder_toggle.hover"));
        toggle.setTextColor(entry.isDone() ? Theme.SUCCESS : Theme.TEXT_DIM);
        this.addButton(toggle, (btn, mb) -> {
            entry.setDone(! entry.isDone());
            gui.onProjectChanged();
        });

        int tx = x + 26;

        if (entry.getItemId() != null)
        {
            MaterialRow row = project.getMaterial(entry.getItemId());
            this.icon = row != null ? row.getStack() : new MaterialRow(entry.getItemId(), 0, 0).getStack();
            tx += 20;
        }

        this.textX = tx;

        // Buttons on the right, placed right to left
        int xRight = x + width - 2;
        PlannerButton delete = new PlannerButton(xRight - 20, by, 20, "✖", PlannerButton.Style.DANGER,
                                                 StringUtils.translate("shulkerplanner.gui.button.reminder_delete.hover"));
        this.addButton(delete, (btn, mb) -> {
            project.getReminders().remove(entry);
            gui.onProjectChanged();
        });
        xRight = delete.getX() - 2;

        PlannerButton edit = new PlannerButton(xRight - 20, by, 20, "✎", PlannerButton.Style.SECONDARY,
                                               StringUtils.translate("shulkerplanner.gui.button.reminder_edit.hover"));
        this.addButton(edit, (btn, mb) -> gui.startEditing(entry));
        xRight = edit.getX() - 2;

        PlannerButton down = new PlannerButton(xRight - 18, by, 18, "▼", PlannerButton.Style.GHOST);
        down.setEnabled(this.canMove(project, entry, 1));
        this.addButton(down, (btn, mb) -> this.move(project, entry, 1));
        xRight = down.getX() - 2;

        PlannerButton up = new PlannerButton(xRight - 18, by, 18, "▲", PlannerButton.Style.GHOST);
        up.setEnabled(this.canMove(project, entry, -1));
        this.addButton(up, (btn, mb) -> this.move(project, entry, -1));
        xRight = up.getX() - 4;

        this.textMaxWidth = xRight - tx;
    }

    private boolean canMove(PlannerProject project, Reminder reminder, int dir)
    {
        int index = project.getReminders().indexOf(reminder);
        int target = index + dir;
        return index >= 0 && target >= 0 && target < project.getReminders().size();
    }

    private void move(PlannerProject project, Reminder reminder, int dir)
    {
        List<Reminder> reminders = project.getReminders();
        int index = reminders.indexOf(reminder);
        int target = index + dir;

        if (index >= 0 && target >= 0 && target < reminders.size())
        {
            reminders.remove(index);
            reminders.add(target, reminder);
            this.list.getGui().onProjectChanged();
        }
    }

    @Override
    public boolean canSelectAt(MouseButtonEvent click)
    {
        return false;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        Reminder reminder = this.entry;

        if (reminder == null)
        {
            return;
        }

        int bg = reminder.isDone() ? Theme.SUCCESS_BG : (this.isOdd ? Theme.withAlpha(Theme.SURFACE_2, 0x90) : 0);

        if (this.list.getGui().isEditing(reminder))
        {
            bg = Theme.WARN_BG;
        }
        else if (this.isMouseOver(mouseX, mouseY))
        {
            bg = Theme.SURFACE_3;
        }

        Ui.rect(ctx, this.x, this.y, this.width, this.height, bg);

        if (this.icon != null)
        {
            Ui.item(ctx, this.icon, this.textX - 19, this.y + (this.height - 16) / 2);
        }

        String text = Ui.trim(reminder.getText(), Math.max(20, this.textMaxWidth));
        int ty = this.y + (this.height - 8) / 2;
        Ui.text(ctx, reminder.isDone() ? GuiBase.TXT_STRIKETHROUGH + text : text, this.textX, ty, reminder.isDone() ? Theme.TEXT_MUTED : Theme.TEXT);

        super.render(ctx, mouseX, mouseY, selected);
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        super.postRenderHovered(ctx, mouseX, mouseY, selected);

        Reminder reminder = this.entry;

        if (reminder != null && mouseX >= this.textX && mouseX < this.textX + this.textMaxWidth &&
            StringUtils.getStringWidth(reminder.getText()) > this.textMaxWidth)
        {
            RenderUtils.drawHoverText(ctx, mouseX, mouseY, List.of(reminder.getText()));
        }
    }
}
