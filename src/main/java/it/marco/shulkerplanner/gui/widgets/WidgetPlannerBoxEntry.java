package it.marco.shulkerplanner.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.data.Reminder;
import it.marco.shulkerplanner.data.ShulkerBinding;
import it.marco.shulkerplanner.data.ShulkerTracker;
import it.marco.shulkerplanner.gui.GuiPlannerBoxes;
import it.marco.shulkerplanner.gui.ui.FlatButton;
import it.marco.shulkerplanner.gui.ui.Theme;
import it.marco.shulkerplanner.gui.ui.Ui;
import it.marco.shulkerplanner.plan.BoxProgress;
import it.marco.shulkerplanner.plan.BoxState;
import it.marco.shulkerplanner.plan.ItemRequirement;
import it.marco.shulkerplanner.plan.PlannedBox;
import it.marco.shulkerplanner.plan.PlannedStack;
import it.marco.shulkerplanner.util.BoxItemFactory;
import it.marco.shulkerplanner.util.Format;

/**
 * One box of the plan as a card: number and state on the left, the 27 slots, the contents, and the actions.
 * The colored stripe on the left and the state chip always say the same thing, in color and in words.
 */
public class WidgetPlannerBoxEntry extends WidgetListEntryBase<PlannedBox>
{
    public static final int HEIGHT = 66;
    private static final int SLOT = 18;
    private static final int INFO_WIDTH = 92;

    private final WidgetListPlannerBoxes list;
    private final int gridX;
    private final int gridY;
    private final int textX;
    private int buttonsX;

    public WidgetPlannerBoxEntry(int x, int y, int width, int height, boolean isOdd,
                                 @Nullable PlannedBox entry, int listIndex, WidgetListPlannerBoxes list)
    {
        super(x, y, width, height, entry, listIndex);

        this.list = list;
        this.gridX = x + 6 + INFO_WIDTH;
        this.gridY = y + 5;
        this.textX = this.gridX + 9 * SLOT + 8;
        this.buttonsX = x + width;

        GuiPlannerBoxes gui = list.getGui();
        PlannerProject project = gui.getProject();
        PlannerSession session = PlannerSession.INSTANCE;

        if (entry == null || project == null)
        {
            return;
        }

        BoxState state = session.getBoxState(entry);
        ShulkerBinding binding = session.getBinding(entry);
        boolean followed = session.isFollowed(entry);

        String labelDone = state == BoxState.DONE ? StringUtils.translate("shulkerplanner.gui.button.box_undo")
                                                  : StringUtils.translate("shulkerplanner.gui.button.box_done");
        String labelFollow = (followed ? "◉ " : "○ ") + StringUtils.translate("shulkerplanner.gui.button.follow");
        String labelSecond = binding != null ? "✕ " + StringUtils.translate("shulkerplanner.gui.button.unlink")
                                             : (BoxItemFactory.canGiveItems() ? StringUtils.translate("shulkerplanner.gui.button.give")
                                                                              : StringUtils.translate("shulkerplanner.gui.button.add_note_short"));
        int bw = Math.max(Ui.width(labelDone), Math.max(Ui.width("○ " + StringUtils.translate("shulkerplanner.gui.button.follow")), Ui.width(labelSecond))) + 14;
        bw = Math.max(bw, Ui.width(StringUtils.translate("shulkerplanner.gui.button.box_done")) + 14);
        int bx = x + width - bw - 4;
        this.buttonsX = bx;

        FlatButton button = new FlatButton(bx, y + 4, bw, labelDone,
                                           state == BoxState.DONE ? FlatButton.Style.SECONDARY : FlatButton.Style.PRIMARY,
                                           StringUtils.translate("shulkerplanner.gui.button.box_done.hover"));
        this.addButton(button, (btn, mb) -> {
            session.setBoxState(entry, state == BoxState.DONE ? (binding != null ? BoxState.FILLING : BoxState.TODO) : BoxState.DONE);
            gui.initGui();
        });

        button = new FlatButton(bx, y + 24, bw, labelFollow, FlatButton.Style.SECONDARY, StringUtils.translate("shulkerplanner.gui.button.follow.hover"));
        button.setActive(followed);
        this.addButton(button, (btn, mb) -> {
            session.setFollowedBox(followed ? null : entry);
            gui.initGui();
        });

        if (binding != null)
        {
            button = new FlatButton(bx, y + 44, bw, labelSecond, FlatButton.Style.GHOST, StringUtils.translate("shulkerplanner.gui.button.unlink.hover"));
            this.addButton(button, (btn, mb) -> {
                if (GuiBase.isShiftDown())
                {
                    session.unlink(entry);
                    gui.initGui();
                }
                else
                {
                    gui.addMessage(MessageType.WARNING, 2500, "shulkerplanner.message.hold_shift_to_unlink");
                }
            });
        }
        else if (BoxItemFactory.canGiveItems())
        {
            button = new FlatButton(bx, y + 44, bw, labelSecond, FlatButton.Style.GHOST, StringUtils.translate("shulkerplanner.gui.button.give.hover"));
            this.addButton(button, (btn, mb) -> {
                ItemStack stack = BoxItemFactory.createBoxItem(project, entry, gui.getPlanBoxCount());

                if (BoxItemFactory.giveToPlayer(stack))
                {
                    gui.addMessage(MessageType.SUCCESS, 2000, "shulkerplanner.message.box_given", entry.getIndex());
                }
                else
                {
                    gui.addMessage(MessageType.ERROR, 3000, "shulkerplanner.message.inventory_full");
                }
            });
        }
        else
        {
            button = new FlatButton(bx, y + 44, bw, labelSecond, FlatButton.Style.GHOST, StringUtils.translate("shulkerplanner.gui.button.add_note_box.hover"));
            this.addButton(button, (btn, mb) -> this.addReminder(gui, project, entry));
        }
    }

    private void addReminder(GuiPlannerBoxes gui, PlannerProject project, PlannedBox entry)
    {
        String text = StringUtils.translate("shulkerplanner.reminder.fill_box", entry.getIndex(), gui.getPlanBoxCount(), this.getContentsShort(entry, 3));

        if (project.hasOpenReminder(text))
        {
            gui.addMessage(MessageType.WARNING, 2000, "shulkerplanner.message.reminder_exists");
            return;
        }

        project.addReminder(new Reminder(text, entry.getSlots().isEmpty() ? null : entry.getSlots().getFirst().id()));
        gui.addMessage(MessageType.SUCCESS, 2000, "shulkerplanner.message.reminder_added", "#" + entry.getIndex());
        gui.onProjectChanged();
    }

    private String getContentsShort(PlannedBox box, int maxItems)
    {
        List<String> names = new ArrayList<>();
        int i = 0;

        for (Map.Entry<String, Integer> e : box.getTotals().entrySet())
        {
            if (i++ >= maxItems)
            {
                names.add("+" + (box.getTotals().size() - maxItems));
                break;
            }

            names.add(e.getValue() + " " + box.getItems().get(e.getKey()).proto().getHoverName().getString());
        }

        return String.join(", ", names);
    }

    @Override
    public boolean canSelectAt(MouseButtonEvent click)
    {
        return false;
    }

    private static int stateColor(BoxState state, boolean inInventory)
    {
        if (state == BoxState.DONE)
        {
            return Theme.SUCCESS;
        }

        if (inInventory)
        {
            return Theme.INFO;
        }

        return state == BoxState.FILLING ? Theme.ACCENT : Theme.BORDER_STRONG;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        PlannedBox box = this.entry;
        PlannerProject project = this.list.getGui().getProject();

        if (box == null || project == null)
        {
            return;
        }

        PlannerSession session = PlannerSession.INSTANCE;
        BoxState state = session.getBoxState(box);
        boolean inInventory = state != BoxState.DONE && session.isBoxInInventory(box);
        ShulkerBinding binding = session.getBinding(box);
        boolean hover = this.isMouseOver(mouseX, mouseY);
        int stripe = stateColor(state, inInventory);
        int x = this.x;
        int y = this.y;

        Ui.panel(ctx, x, y + 1, this.width, this.height - 2, hover ? Theme.SURFACE_3 : Theme.SURFACE_2,
                 session.isFollowed(box) ? Theme.ACCENT_DIM : Theme.BORDER);
        Ui.rect(ctx, x + 1, y + 3, 3, this.height - 6, stripe);

        // Info column
        int ix = x + 9;
        int iy = y + 6;
        ItemStack shulker = new ItemStack(project.getShulkerItem(box.getIndex()));
        Ui.item(ctx, shulker, ix, iy);
        Ui.textShadow(ctx, "#" + box.getIndex(), ix + 20, iy + 1, Theme.TEXT);
        Ui.text(ctx, "/" + this.list.getGui().getPlanBoxCount(), ix + 20 + Ui.width("#" + box.getIndex()) + 1, iy + 1, Theme.TEXT_MUTED);
        Ui.text(ctx, box.isPure() ? StringUtils.translate("shulkerplanner.gui.label.box.pure")
                                  : StringUtils.translate("shulkerplanner.gui.label.box.mixed", box.getTotals().size()), ix + 20, iy + 10, Theme.TEXT_MUTED);

        String chip;

        if (state == BoxState.DONE)
        {
            chip = "✔ " + StringUtils.translate("shulkerplanner.gui.label.box.status_done");
        }
        else if (inInventory)
        {
            chip = StringUtils.translate("shulkerplanner.gui.label.box.status_inventory");
        }
        else if (state == BoxState.FILLING)
        {
            chip = StringUtils.translate("shulkerplanner.gui.label.box.status_filling");
        }
        else
        {
            chip = StringUtils.translate("shulkerplanner.gui.label.box.status_todo");
        }

        Ui.chip(ctx, Ui.trim(chip, INFO_WIDTH - 12), ix, iy + 22, stripe == Theme.BORDER_STRONG ? Theme.TEXT_DIM : stripe,
                Theme.withAlpha(stripe, 0x30));

        if (binding != null)
        {
            Ui.lock(ctx, ix, iy + 37, Theme.ACCENT_HOVER);
            Ui.text(ctx, Ui.trim(ShulkerTracker.describeLocation(binding), INFO_WIDTH - 12), ix + 10, iy + 37, Theme.ACCENT_HOVER);
        }
        else if (session.isFollowed(box))
        {
            Ui.text(ctx, "◉ " + StringUtils.translate("shulkerplanner.gui.label.box.followed"), ix, iy + 37, Theme.INFO);
        }
        else
        {
            Ui.text(ctx, StringUtils.translate("shulkerplanner.gui.label.box.not_linked"), ix, iy + 37, Theme.TEXT_MUTED);
        }

        // Slot grid: tinted by what's already inside the linked box
        BoxProgress progress = binding != null ? session.getProgress(box) : null;
        List<PlannedStack> stacks = box.getSlots();

        for (int i = 0; i < PlannedBox.SLOTS; ++i)
        {
            int sx = this.gridX + (i % 9) * SLOT;
            int sy = this.gridY + (i / 9) * SLOT;
            PlannedStack planned = i < stacks.size() ? stacks.get(i) : null;
            int tint = 0;

            if (planned != null && planned.spare())
            {
                tint = 0x402060C0;
            }

            if (progress != null && i < progress.getSlots().size())
            {
                BoxProgress.Slot s = progress.getSlots().get(i);
                tint = switch (s.state())
                {
                    case IN_BOX -> 0x5022C55E;
                    case IN_INVENTORY -> 0x403B82F6;
                    case PARTIAL -> 0x40F59E0B;
                    case MISSING -> 0;
                };
            }

            Ui.slot(ctx, sx, sy, SLOT, tint);

            if (planned != null)
            {
                ItemStack stack = planned.createStack();
                ctx.renderItem(stack, sx + 1, sy + 1);
                ctx.renderItemDecorations(this.textRenderer, stack, sx + 1, sy + 1);
            }
        }

        // Contents summary and progress
        int maxTextWidth = this.buttonsX - this.textX - 8;

        if (maxTextWidth > 40)
        {
            int ty = y + 6;
            int lines = 0;
            int maxLines = progress != null ? 4 : 5;

            for (Map.Entry<String, Integer> e : box.getTotals().entrySet())
            {
                if (lines == maxLines - 1 && box.getTotals().size() > maxLines)
                {
                    Ui.text(ctx, StringUtils.translate("shulkerplanner.gui.label.box.more", box.getTotals().size() - lines), this.textX, ty, Theme.TEXT_MUTED);
                    break;
                }

                ItemRequirement req = box.getItems().get(e.getKey());
                String count = Format.number(e.getValue());
                Ui.text(ctx, count, this.textX, ty, Theme.TEXT);
                Ui.text(ctx, Ui.trim(req.proto().getHoverName().getString(), maxTextWidth - Ui.width(count) - 4),
                        this.textX + Ui.width(count) + 4, ty, Theme.TEXT_DIM);
                ty += 10;
                ++lines;
            }

            if (progress != null)
            {
                long total = progress.getTotal();
                int barY = y + this.height - 12;
                Ui.progressStacked(ctx, this.textX, barY, Math.min(maxTextWidth, 140), 4, total,
                                   new long[] { progress.getInBox(), progress.getInInventory() },
                                   new int[] { Theme.SUCCESS, Theme.INFO }, Theme.SURFACE_HOVER);
                String pct = total > 0 ? (100 * progress.getInBox() / total) + "%" : "";
                Ui.text(ctx, pct, this.textX + Math.min(maxTextWidth, 140) + 4, barY - 2, Theme.TEXT_DIM);
            }
        }

        super.render(ctx, mouseX, mouseY, selected);
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        super.postRenderHovered(ctx, mouseX, mouseY, selected);

        PlannedBox box = this.entry;

        if (box == null || mouseX >= this.buttonsX)
        {
            return;
        }

        int col = (mouseX - this.gridX) / SLOT;
        int row = (mouseY - this.gridY) / SLOT;

        if (mouseX >= this.gridX && mouseY >= this.gridY && col < 9 && row < 3)
        {
            int index = row * 9 + col;

            if (index < box.getSlots().size())
            {
                PlannedStack planned = box.getSlots().get(index);
                ItemRequirement req = planned.item();
                List<String> lines = new ArrayList<>();
                lines.add(GuiBase.TXT_BOLD + req.proto().getHoverName().getString());
                lines.add(StringUtils.translate("shulkerplanner.gui.hover.slot.count", planned.count(), req.maxStack()));
                lines.add(StringUtils.translate("shulkerplanner.gui.hover.slot.in_box", Format.number(box.getTotals().get(req.id())),
                                                Format.stacks(box.getTotals().get(req.id()), req.maxStack())));

                if (planned.spare())
                {
                    lines.add(GuiBase.TXT_AQUA + StringUtils.translate("shulkerplanner.gui.hover.slot.spare"));
                }

                Ui.tooltip(ctx, mouseX, mouseY, lines);
            }
        }
        else if (mouseX < this.gridX)
        {
            PlannerSession session = PlannerSession.INSTANCE;
            List<String> lines = new ArrayList<>();
            lines.add(GuiBase.TXT_BOLD + BoxItemFactory.getBoxName(box, this.list.getGui().getPlanBoxCount()));
            lines.add(StringUtils.translate("shulkerplanner.gui.hover.box.items", Format.number(box.getItemCount()), box.getUsedSlots()));

            for (Map.Entry<String, Integer> e : box.getTotals().entrySet())
            {
                ItemRequirement req = box.getItems().get(e.getKey());
                lines.add(GuiBase.TXT_GRAY + " " + Format.number(e.getValue()) + " " + req.proto().getHoverName().getString()
                          + GuiBase.TXT_DARK_GRAY + " (" + Format.stacks(e.getValue(), req.maxStack()) + ")");
            }

            ShulkerBinding binding = session.getBinding(box);
            lines.add("");
            lines.add(binding != null ? GuiBase.TXT_LIGHT_PURPLE + StringUtils.translate("shulkerplanner.gui.hover.box.linked", ShulkerTracker.describeLocation(binding))
                                      : GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.gui.hover.box.how_to_link"));
            Ui.tooltip(ctx, mouseX, mouseY, lines);
        }
    }
}
