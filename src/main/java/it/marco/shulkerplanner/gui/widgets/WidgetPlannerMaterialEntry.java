package it.marco.shulkerplanner.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.data.Reminder;
import it.marco.shulkerplanner.gui.GuiPlannerMaterials;
import it.marco.shulkerplanner.gui.ui.FlatButton;
import it.marco.shulkerplanner.gui.ui.Theme;
import it.marco.shulkerplanner.gui.ui.Ui;
import it.marco.shulkerplanner.util.Format;

/**
 * One material: how many are needed, already packed in boxes, carried, and still to get.
 */
public class WidgetPlannerMaterialEntry extends WidgetListEntryBase<MaterialRow>
{
    private final WidgetListPlannerMaterials list;
    private final WidgetListPlannerMaterials.Layout layout;
    private final boolean isOdd;

    public WidgetPlannerMaterialEntry(int x, int y, int width, int height, boolean isOdd,
                                      @Nullable MaterialRow entry, int listIndex, WidgetListPlannerMaterials list)
    {
        super(x, y, width, height, entry, listIndex);

        this.list = list;
        this.layout = list.getLayout();
        this.isOdd = isOdd;

        PlannerProject project = list.getGui().getProject();

        if (entry != null && project != null)
        {
            String id = entry.getId();
            int xRight = x + width - 4;
            int by = y + 2;

            FlatButton include = FlatButton.toggle(StringUtils.translate("shulkerplanner.gui.button.include"), project.isExcluded(id) == false,
                                                   StringUtils.translate("shulkerplanner.gui.button.include.hover"));
            include.setPosition(xRight - this.layout.widthInclude, by);
            include.setWidth(this.layout.widthInclude);
            this.addButton(include, (btn, mb) -> {
                project.setExcluded(id, ! project.isExcluded(id));
                this.list.getGui().onPackingOptionsChanged();
            });
            xRight = include.getX() - 4;

            FlatButton gathered = FlatButton.toggle(StringUtils.translate("shulkerplanner.gui.button.gathered_short"), project.isGathered(id),
                                                    StringUtils.translate("shulkerplanner.gui.button.gathered.hover"));
            gathered.setPosition(xRight - this.layout.widthGathered, by);
            gathered.setWidth(this.layout.widthGathered);
            this.addButton(gathered, (btn, mb) -> {
                project.setGathered(id, ! project.isGathered(id));
                this.list.getGui().onProjectChanged();
            });
            xRight = gathered.getX() - 4;

            FlatButton note = new FlatButton(xRight - this.layout.widthNote, by, this.layout.widthNote, "+", FlatButton.Style.GHOST,
                                             StringUtils.translate("shulkerplanner.gui.button.add_note.hover"));
            this.addButton(note, (btn, mb) -> this.addReminder(project, entry));
        }
    }

    private void addReminder(PlannerProject project, MaterialRow row)
    {
        long toGet = this.layout.stillToGet(row.getId());
        int count = (int) Math.min(Integer.MAX_VALUE, toGet > 0 ? toGet : this.layout.needed(row.getId()));
        int maxStack = row.getStack().getMaxStackSize();
        String amount = Format.number(count) + " (" + Format.stacks(count, maxStack) + ")";
        String text = StringUtils.translate("shulkerplanner.reminder.gather_item", amount, row.getDisplayName());

        GuiPlannerMaterials gui = this.list.getGui();

        if (project.hasOpenReminder(text))
        {
            gui.addMessage(MessageType.WARNING, 2000, "shulkerplanner.message.reminder_exists");
            return;
        }

        project.addReminder(new Reminder(text, row.getId()));
        gui.addMessage(MessageType.SUCCESS, 2000, "shulkerplanner.message.reminder_added", row.getDisplayName());
        gui.onProjectChanged();
    }

    @Override
    public boolean canSelectAt(MouseButtonEvent click)
    {
        return false;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        PlannerProject project = this.list.getGui().getProject();
        boolean header = this.entry == null;
        boolean hover = header == false && this.isMouseOver(mouseX, mouseY);

        if (header)
        {
            Ui.rect(ctx, this.x, this.y + this.height - 1, this.width, 1, Theme.BORDER);

            if (this.list.getSearchBarWidget().isSearchOpen() == false)
            {
                this.drawHeader(ctx, this.y + 8);
            }

            return;
        }

        Ui.rect(ctx, this.x, this.y, this.width, this.height, hover ? Theme.SURFACE_3 : (this.isOdd ? Theme.withAlpha(Theme.SURFACE_2, 0x90) : 0));

        if (project == null)
        {
            return;
        }

        PlannerSession session = PlannerSession.INSTANCE;
        MaterialRow row = this.entry;
        String id = row.getId();
        boolean ignored = session.isIgnoredInLitematica(id);
        boolean excluded = project.isExcluded(id) || ignored;
        boolean gathered = project.isGathered(id);
        ItemStack stack = row.getStack();
        int maxStack = stack.getMaxStackSize();
        long needed = this.layout.needed(id);
        long packed = this.layout.packed(id);
        long carrying = this.layout.carrying(id);
        long toGet = this.layout.stillToGet(id);
        boolean complete = excluded == false && (toGet <= 0 || gathered);
        int ty = this.y + 7;
        int x0 = this.x;

        // Status stripe: done / in progress / missing
        int stripe = excluded ? 0 : (complete ? Theme.SUCCESS : (packed + carrying > 0 ? Theme.WARN : Theme.DANGER));

        if (stripe != 0)
        {
            Ui.rect(ctx, x0, this.y + 3, 2, this.height - 6, stripe);
        }

        Ui.item(ctx, stack, x0 + 6, this.y + 3);

        String name = Ui.trim(row.getDisplayName(), this.layout.nameWidth - 24);
        int nameColor = excluded ? Theme.TEXT_MUTED : (complete ? Theme.SUCCESS : (stack.getItem().canFitInsideContainerItems() ? Theme.TEXT : Theme.WARN));
        Ui.text(ctx, excluded ? GuiBase.TXT_STRIKETHROUGH + name : name, x0 + 26, ty, nameColor);

        int dim = excluded ? Theme.TEXT_MUTED : Theme.TEXT;
        Ui.text(ctx, Format.number(excluded ? project.getRequiredCount(row) : needed), x0 + this.layout.colNeeded, ty, dim);

        if (this.layout.colPacked >= 0)
        {
            Ui.text(ctx, Format.number(packed), x0 + this.layout.colPacked, ty, excluded ? Theme.TEXT_MUTED : (packed > 0 ? Theme.SUCCESS : Theme.TEXT_MUTED));
            Ui.text(ctx, Format.number(carrying), x0 + this.layout.colCarrying, ty, excluded ? Theme.TEXT_MUTED : (carrying > 0 ? Theme.INFO : Theme.TEXT_MUTED));
        }

        if (this.layout.colToGet >= 0)
        {
            String str;
            int color;

            if (excluded)
            {
                str = "—";
                color = Theme.TEXT_MUTED;
            }
            else if (toGet <= 0)
            {
                str = "✔ 0";
                color = Theme.SUCCESS;
            }
            else
            {
                str = Format.number(toGet);
                color = gathered ? Theme.TEXT_DIM : Theme.DANGER;
            }

            Ui.text(ctx, str, x0 + this.layout.colToGet, ty, color);
        }

        if (this.layout.colBar >= 0 && excluded == false)
        {
            Ui.progressStacked(ctx, x0 + this.layout.colBar, this.y + 9, this.layout.barWidth, 4, needed,
                               new long[] { packed, carrying }, new int[] { Theme.SUCCESS, Theme.INFO }, Theme.SURFACE_HOVER);
        }

        if (this.layout.colStacks >= 0 && excluded == false)
        {
            Ui.text(ctx, toGet > 0 ? Format.stacks((int) Math.min(Integer.MAX_VALUE, toGet), maxStack) : "", x0 + this.layout.colStacks, ty, Theme.TEXT_DIM);
        }

        super.render(ctx, mouseX, mouseY, selected);
    }

    private void drawHeader(GuiContext ctx, int y)
    {
        String k = WidgetListPlannerMaterials.Layout.KEY_HEADER;
        int x0 = this.x;
        int c = Theme.TEXT_DIM;

        Ui.text(ctx, StringUtils.translate(k + "item"), x0 + 6, y, c);
        Ui.text(ctx, StringUtils.translate(k + "needed"), x0 + this.layout.colNeeded, y, c);

        if (this.layout.colPacked >= 0)
        {
            Ui.text(ctx, StringUtils.translate(k + "packed"), x0 + this.layout.colPacked, y, Theme.SUCCESS);
            Ui.text(ctx, StringUtils.translate(k + "carrying"), x0 + this.layout.colCarrying, y, Theme.INFO);
        }

        if (this.layout.colToGet >= 0)
        {
            Ui.text(ctx, StringUtils.translate(k + "to_get"), x0 + this.layout.colToGet, y, Theme.DANGER);
        }

        if (this.layout.colStacks >= 0)
        {
            Ui.text(ctx, StringUtils.translate(k + "stacks"), x0 + this.layout.colStacks, y, c);
        }
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        super.postRenderHovered(ctx, mouseX, mouseY, selected);

        PlannerProject project = this.list.getGui().getProject();

        // Only show the details when hovering the item part of the row, not the buttons
        if (this.entry == null || project == null || mouseX > this.x + this.width - this.layout.buttonsWidth - 4)
        {
            return;
        }

        MaterialRow row = this.entry;
        String id = row.getId();
        ItemStack stack = row.getStack();
        int maxStack = stack.getMaxStackSize();
        List<String> lines = new ArrayList<>();
        String k = "shulkerplanner.gui.hover.material.";
        long needed = this.layout.needed(id);

        lines.add(GuiBase.TXT_BOLD + row.getDisplayName() + GuiBase.TXT_RST + GuiBase.TXT_DARK_GRAY + "  " + id);
        lines.add(StringUtils.translate(k + "total", Format.number(row.getTotal()), Format.stacks(row.getTotal(), maxStack)));
        lines.add(StringUtils.translate(k + "missing", Format.number(row.getMissing()), Format.stacks(row.getMissing(), maxStack)));
        lines.add(StringUtils.translate(k + "required", project.getCountMode().getDisplayName(), Format.number(needed),
                                        Format.stacks((int) Math.min(Integer.MAX_VALUE, needed), maxStack), Format.boxes((int) Math.min(Integer.MAX_VALUE, needed), maxStack)));
        lines.add(GuiBase.TXT_GREEN + StringUtils.translate(k + "packed", Format.number(this.layout.packed(id))));
        lines.add(GuiBase.TXT_AQUA + StringUtils.translate(k + "carrying", Format.number(this.layout.carrying(id))));
        lines.add(GuiBase.TXT_RED + StringUtils.translate(k + "to_get", Format.number(this.layout.stillToGet(id))));
        lines.add(GuiBase.TXT_DARK_GRAY + StringUtils.translate(k + "stack_size", maxStack));

        if (stack.getItem().canFitInsideContainerItems() == false)
        {
            lines.add(GuiBase.TXT_GOLD + StringUtils.translate(k + "not_boxable"));
        }

        if (PlannerSession.INSTANCE.isIgnoredInLitematica(id))
        {
            lines.add(GuiBase.TXT_GRAY + StringUtils.translate(k + "ignored_litematica"));
        }

        if (project.isExcluded(id))
        {
            lines.add(GuiBase.TXT_RED + StringUtils.translate(k + "excluded"));
        }

        if (project.isGathered(id))
        {
            lines.add(GuiBase.TXT_GREEN + StringUtils.translate(k + "gathered"));
        }

        Ui.tooltip(ctx, mouseX, mouseY, lines);
    }
}
