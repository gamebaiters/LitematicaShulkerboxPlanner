package it.marco.shulkerplanner.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.data.Reminder;
import it.marco.shulkerplanner.gui.GuiPlannerBoxes;
import it.marco.shulkerplanner.plan.ItemRequirement;
import it.marco.shulkerplanner.plan.PlannedBox;
import it.marco.shulkerplanner.plan.PlannedStack;
import it.marco.shulkerplanner.util.BoxItemFactory;
import it.marco.shulkerplanner.util.Format;

public class WidgetPlannerBoxEntry extends WidgetListEntryBase<PlannedBox>
{
    public static final int HEIGHT = 62;
    private static final int SLOT = 18;
    private static final int INFO_WIDTH = 82;

    private final WidgetListPlannerBoxes list;
    private final boolean isOdd;
    private final int gridX;
    private final int gridY;
    private final int textX;
    private int buttonsX;

    public WidgetPlannerBoxEntry(int x, int y, int width, int height, boolean isOdd,
                                 @Nullable PlannedBox entry, int listIndex, WidgetListPlannerBoxes list)
    {
        super(x, y, width, height, entry, listIndex);

        this.list = list;
        this.isOdd = isOdd;
        this.gridX = x + 4 + INFO_WIDTH;
        this.gridY = y + 4;
        this.textX = this.gridX + 9 * SLOT + 8;
        this.buttonsX = x + width;

        GuiPlannerBoxes gui = list.getGui();
        PlannerProject project = gui.getProject();

        if (entry == null || project == null)
        {
            return;
        }

        boolean done = project.isBoxDone(entry);

        String labelDone = done ? GuiBase.TXT_GREEN + StringUtils.translate("shulkerplanner.gui.button.box_done")
                                : GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.gui.button.box_not_done");
        String labelGive = StringUtils.translate("shulkerplanner.gui.button.give");
        String labelNote = StringUtils.translate("shulkerplanner.gui.button.add_note_short");
        int bw = Math.max(StringUtils.getStringWidth(StringUtils.translate("shulkerplanner.gui.button.box_done")),
                          StringUtils.getStringWidth(StringUtils.translate("shulkerplanner.gui.button.box_not_done")));
        bw = Math.max(bw, Math.max(StringUtils.getStringWidth(labelGive), StringUtils.getStringWidth(labelNote))) + 10;
        int bx = x + width - bw - 2;
        this.buttonsX = bx;

        ButtonGeneric button = new ButtonGeneric(bx, y + 1, bw, 19, labelDone, StringUtils.translate("shulkerplanner.gui.button.box_done.hover"));
        this.addButton(button, (btn, mb) -> {
            project.setBoxDone(entry, ! project.isBoxDone(entry));
            gui.onProjectChanged();
        });

        button = new ButtonGeneric(bx, y + 21, bw, 19, labelGive, StringUtils.translate("shulkerplanner.gui.button.give.hover"));
        button.setEnabled(BoxItemFactory.canGiveItems());
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

        button = new ButtonGeneric(bx, y + 41, bw, 19, labelNote, StringUtils.translate("shulkerplanner.gui.button.add_note_box.hover"));
        this.addButton(button, (btn, mb) -> {
            String text = StringUtils.translate("shulkerplanner.reminder.fill_box", entry.getIndex(), gui.getPlanBoxCount(), this.getContentsShort(entry, 3));

            if (project.hasOpenReminder(text))
            {
                gui.addMessage(MessageType.WARNING, 2000, "shulkerplanner.message.reminder_exists");
                return;
            }

            project.addReminder(new Reminder(text, entry.getSlots().isEmpty() ? null : entry.getSlots().getFirst().id()));
            gui.addMessage(MessageType.SUCCESS, 2000, "shulkerplanner.message.reminder_added", "#" + entry.getIndex());
            gui.onProjectChanged();
        });
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

    @Override
    protected boolean onMouseClickedImpl(MouseButtonEvent click, boolean doubleClick)
    {
        // Clicking the info column makes this the box followed on the HUD
        if (this.entry != null && click.x() < this.gridX)
        {
            PlannerSession session = PlannerSession.INSTANCE;
            session.setFocusedBox(session.isFocused(this.entry) ? -1 : this.entry.getIndex() - 1);
            this.list.getGui().initGui();
            return true;
        }

        return super.onMouseClickedImpl(click, doubleClick);
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
        boolean done = project.isBoxDone(box);
        boolean inInventory = session.isBoxInInventory(box);
        int bg;

        if (done)
        {
            bg = 0xA0143A14;
        }
        else if (inInventory)
        {
            bg = 0xA0143440;
        }
        else
        {
            bg = this.isOdd ? 0xA0101010 : 0xA0282828;
        }

        RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, bg);

        if (this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawOutline(ctx, this.x, this.y, this.width, this.height, 0xFF909090);
        }

        // Info column
        int boxCount = this.list.getGui().getPlanBoxCount();
        int x = this.x + 4;
        int y = this.y + 5;
        this.drawStringWithShadow(ctx, x, y, 0xFFFFFFFF, GuiBase.TXT_BOLD + "#" + box.getIndex() + GuiBase.TXT_RST + GuiBase.TXT_GRAY + " / " + boxCount);

        ItemStack shulker = new ItemStack(project.getShulkerItem(box.getIndex()));
        ctx.renderItem(shulker, this.x + INFO_WIDTH - 18, this.y + 3);

        String type = box.isPure() ? StringUtils.translate("shulkerplanner.gui.label.box.pure")
                                   : StringUtils.translate("shulkerplanner.gui.label.box.mixed", box.getTotals().size());
        this.drawString(ctx, x, y + 12, 0xFFFFFFFF, GuiBase.TXT_GRAY + type);

        int used = box.getUsedSlots();
        String slots = (used == PlannedBox.SLOTS ? GuiBase.TXT_GREEN : GuiBase.TXT_GOLD) + StringUtils.translate("shulkerplanner.gui.label.box.slots", used);
        this.drawString(ctx, x, y + 23, 0xFFFFFFFF, slots);

        String status;

        if (done)
        {
            status = GuiBase.TXT_GREEN + StringUtils.translate("shulkerplanner.gui.label.box.status_done");
        }
        else if (inInventory)
        {
            status = GuiBase.TXT_AQUA + StringUtils.translate("shulkerplanner.gui.label.box.status_inventory");
        }
        else
        {
            status = GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.gui.label.box.status_todo");
        }

        this.drawString(ctx, x, y + 35, 0xFFFFFFFF, status);

        if (session.isFocused(box))
        {
            this.drawString(ctx, x, y + 46, 0xFFFFFFFF, GuiBase.TXT_LIGHT_PURPLE + StringUtils.translate("shulkerplanner.gui.label.box.followed"));
        }
        else if (this.isMouseOver(mouseX, mouseY) && mouseX < this.gridX)
        {
            this.drawString(ctx, x, y + 46, 0xFFFFFFFF, GuiBase.TXT_DARK_GRAY + StringUtils.translate("shulkerplanner.gui.label.box.click_to_follow"));
        }

        // Slot grid
        List<PlannedStack> stacks = box.getSlots();

        for (int i = 0; i < PlannedBox.SLOTS; ++i)
        {
            int sx = this.gridX + (i % 9) * SLOT;
            int sy = this.gridY + (i / 9) * SLOT;
            PlannedStack planned = i < stacks.size() ? stacks.get(i) : null;
            int slotColor = planned == null ? 0x60000000 : (planned.spare() ? 0x80203C60 : 0x80505050);

            RenderUtils.drawRect(ctx, sx, sy, SLOT - 1, SLOT - 1, slotColor);

            if (planned != null)
            {
                ItemStack stack = planned.createStack();
                ctx.renderItem(stack, sx, sy);
                ctx.renderItemDecorations(this.textRenderer, stack, sx, sy);
            }
        }

        // Contents summary
        int maxTextWidth = this.buttonsX - this.textX - 6;

        if (maxTextWidth > 40)
        {
            int ty = this.y + 5;
            int lines = 0;
            int maxLines = 5;

            for (Map.Entry<String, Integer> e : box.getTotals().entrySet())
            {
                if (lines == maxLines - 1 && box.getTotals().size() > maxLines)
                {
                    String more = StringUtils.translate("shulkerplanner.gui.label.box.more", box.getTotals().size() - lines);
                    this.drawString(ctx, this.textX, ty, 0xFFFFFFFF, GuiBase.TXT_GRAY + more);
                    break;
                }

                ItemRequirement req = box.getItems().get(e.getKey());
                String line = GuiBase.TXT_WHITE + Format.number(e.getValue()) + GuiBase.TXT_GRAY + " " + req.proto().getHoverName().getString();
                this.drawString(ctx, this.textX, ty, 0xFFFFFFFF, Format.trim(line, maxTextWidth));
                ty += 11;
                ++lines;
            }
        }

        super.render(ctx, mouseX, mouseY, selected);
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        super.postRenderHovered(ctx, mouseX, mouseY, selected);

        PlannedBox box = this.entry;

        if (box == null)
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

                RenderUtils.drawHoverText(ctx, mouseX, mouseY, lines);
            }
        }
        else if (mouseX < this.gridX)
        {
            List<String> lines = new ArrayList<>();
            lines.add(GuiBase.TXT_BOLD + BoxItemFactory.getBoxName(box, this.list.getGui().getPlanBoxCount()));
            lines.add(StringUtils.translate("shulkerplanner.gui.hover.box.items", Format.number(box.getItemCount()), box.getUsedSlots()));

            for (Map.Entry<String, Integer> e : box.getTotals().entrySet())
            {
                ItemRequirement req = box.getItems().get(e.getKey());
                lines.add(GuiBase.TXT_GRAY + " " + Format.number(e.getValue()) + " " + req.proto().getHoverName().getString()
                          + GuiBase.TXT_DARK_GRAY + " (" + Format.stacks(e.getValue(), req.maxStack()) + ")");
            }

            RenderUtils.drawHoverText(ctx, mouseX, mouseY, lines);
        }
    }
}
