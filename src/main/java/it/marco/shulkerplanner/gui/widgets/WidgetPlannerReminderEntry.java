package it.marco.shulkerplanner.gui.widgets;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.Reminder;
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
        String check = entry.isDone() ? GuiBase.TXT_GREEN + "✔" : GuiBase.TXT_GRAY + "○";
        ButtonGeneric toggle = new ButtonGeneric(x + 2, y + 1, 20, 20, check, StringUtils.translate("shulkerplanner.gui.button.reminder_toggle.hover"));
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
        ButtonGeneric delete = new ButtonGeneric(xRight - 20, y + 1, 20, 20, GuiBase.TXT_RED + "✖",
                                                 StringUtils.translate("shulkerplanner.gui.button.reminder_delete.hover"));
        this.addButton(delete, (btn, mb) -> {
            project.getReminders().remove(entry);
            gui.onProjectChanged();
        });
        xRight = delete.getX() - 2;

        ButtonGeneric edit = new ButtonGeneric(xRight - 20, y + 1, 20, 20, "✎",
                                               StringUtils.translate("shulkerplanner.gui.button.reminder_edit.hover"));
        this.addButton(edit, (btn, mb) -> gui.startEditing(entry));
        xRight = edit.getX() - 2;

        ButtonGeneric down = new ButtonGeneric(xRight - 16, y + 1, 16, 20, "▼");
        down.setEnabled(this.canMove(project, entry, 1));
        this.addButton(down, (btn, mb) -> this.move(project, entry, 1));
        xRight = down.getX() - 1;

        ButtonGeneric up = new ButtonGeneric(xRight - 16, y + 1, 16, 20, "▲");
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

        int bg = reminder.isDone() ? 0xA0102010 : (this.isOdd ? 0xA0101010 : 0xA0303030);

        if (this.list.getGui().isEditing(reminder))
        {
            bg = 0xA0404010;
        }
        else if (this.isMouseOver(mouseX, mouseY))
        {
            bg = 0xA0505050;
        }

        RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, bg);

        if (this.icon != null)
        {
            ctx.renderItem(this.icon, this.textX - 19, this.y + 3);
        }

        String text = Format.trim(reminder.getText(), Math.max(20, this.textMaxWidth));
        String color = reminder.isDone() ? GuiBase.TXT_DARK_GRAY + GuiBase.TXT_STRIKETHROUGH : GuiBase.TXT_WHITE;
        this.drawString(ctx, this.textX, this.y + 7, 0xFFFFFFFF, color + text);

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
