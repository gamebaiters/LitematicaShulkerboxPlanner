package it.marco.shulkerplanner.gui.widgets;

import java.util.ArrayList;
import java.util.List;
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

import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.Reminder;
import it.marco.shulkerplanner.gui.GuiPlannerMaterials;
import it.marco.shulkerplanner.util.Format;

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
            int xRight = x + width - 2;
            int by = y + 1;

            // Placed from right to left
            ButtonGeneric include = new ButtonGeneric(xRight - this.layout.widthInclude, by, this.layout.widthInclude, 20,
                                                      getIncludeLabel(project.isExcluded(id) == false),
                                                      StringUtils.translate("shulkerplanner.gui.button.include.hover"));
            this.addButton(include, (btn, mb) -> {
                project.setExcluded(id, ! project.isExcluded(id));
                this.list.getGui().onProjectChanged();
            });
            xRight = include.getX() - 2;

            ButtonGeneric gathered = new ButtonGeneric(xRight - this.layout.widthGathered, by, this.layout.widthGathered, 20,
                                                       getGatheredLabel(project.isGathered(id)),
                                                       StringUtils.translate("shulkerplanner.gui.button.gathered.hover"));
            this.addButton(gathered, (btn, mb) -> {
                project.setGathered(id, ! project.isGathered(id));
                this.list.getGui().onProjectChanged();
            });
            xRight = gathered.getX() - 2;

            ButtonGeneric note = new ButtonGeneric(xRight - this.layout.widthNote, by, this.layout.widthNote, 20,
                                                   StringUtils.translate("shulkerplanner.gui.button.add_note_short"),
                                                   StringUtils.translate("shulkerplanner.gui.button.add_note.hover"));
            this.addButton(note, (btn, mb) -> this.addReminder(project, entry));
        }
    }

    public static String getIncludeLabel(boolean included)
    {
        return included ? GuiBase.TXT_GREEN + StringUtils.translate("shulkerplanner.gui.button.included")
                        : GuiBase.TXT_RED + StringUtils.translate("shulkerplanner.gui.button.excluded");
    }

    public static String getGatheredLabel(boolean gathered)
    {
        return gathered ? GuiBase.TXT_GREEN + StringUtils.translate("shulkerplanner.gui.button.gathered")
                        : GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.gui.button.not_gathered");
    }

    private void addReminder(PlannerProject project, MaterialRow row)
    {
        int count = project.getRequiredCount(row);
        int maxStack = row.getStack().getMaxStackSize();
        String amount = count > 0 ? Format.number(count) + " (" + Format.stacks(count, maxStack) + ")" : Format.number(row.getTotal());
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

        if (header == false && this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0xA0707070);
        }
        else
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, this.isOdd ? 0xA0101010 : 0xA0303030);
        }

        int ty = this.y + 7;

        if (header)
        {
            if (this.list.getSearchBarWidget().isSearchOpen() == false)
            {
                this.drawHeader(ctx, ty);
            }

            return;
        }

        if (project == null)
        {
            return;
        }

        MaterialRow row = this.entry;
        String id = row.getId();
        boolean excluded = project.isExcluded(id);
        boolean gathered = project.isGathered(id);
        ItemStack stack = row.getStack();
        int maxStack = stack.getMaxStackSize();
        int count = project.getRequiredCount(row);
        int x0 = this.x;

        RenderUtils.drawRect(ctx, x0 + 4, this.y + 3, 16, 16, 0x20FFFFFF);
        ctx.renderItem(stack, x0 + 4, this.y + 3);

        String name = Format.trim(row.getDisplayName(), this.layout.nameWidth - 22);

        if (excluded)
        {
            name = GuiBase.TXT_DARK_GRAY + GuiBase.TXT_STRIKETHROUGH + name;
        }
        else if (gathered)
        {
            name = GuiBase.TXT_GREEN + name;
        }
        else if (stack.getItem().canFitInsideContainerItems() == false)
        {
            name = GuiBase.TXT_GOLD + name;
        }

        this.drawString(ctx, x0 + 24, ty, 0xFFFFFFFF, name);

        String color = excluded ? GuiBase.TXT_DARK_GRAY : (count == 0 ? GuiBase.TXT_GREEN : GuiBase.TXT_WHITE);
        this.drawString(ctx, x0 + this.layout.colRequired, ty, 0xFFFFFFFF, color + Format.number(count));

        if (this.layout.colAvailable >= 0)
        {
            int available = row.getAvailable();
            String c = excluded ? GuiBase.TXT_DARK_GRAY : (available >= count ? GuiBase.TXT_GREEN : (available > 0 ? GuiBase.TXT_GOLD : GuiBase.TXT_RED));
            this.drawString(ctx, x0 + this.layout.colAvailable, ty, 0xFFFFFFFF, c + Format.number(available));
        }

        if (this.layout.colBoxes >= 0)
        {
            String c = excluded ? GuiBase.TXT_DARK_GRAY : GuiBase.TXT_AQUA;
            this.drawString(ctx, x0 + this.layout.colBoxes, ty, 0xFFFFFFFF, c + Format.boxes(count, maxStack));
        }

        if (this.layout.colStacks >= 0)
        {
            String c = excluded ? GuiBase.TXT_DARK_GRAY : GuiBase.TXT_GRAY;
            this.drawString(ctx, x0 + this.layout.colStacks, ty, 0xFFFFFFFF, c + Format.stacks(count, maxStack));
        }

        super.render(ctx, mouseX, mouseY, selected);
    }

    private void drawHeader(GuiContext ctx, int y)
    {
        String b = GuiBase.TXT_BOLD;
        String k = WidgetListPlannerMaterials.Layout.KEY_HEADER;
        int x0 = this.x;

        this.drawString(ctx, x0 + 4, y, 0xFFFFFFFF, b + StringUtils.translate(k + "item"));
        this.drawString(ctx, x0 + this.layout.colRequired, y, 0xFFFFFFFF, b + StringUtils.translate(k + "required"));

        if (this.layout.colAvailable >= 0)
        {
            this.drawString(ctx, x0 + this.layout.colAvailable, y, 0xFFFFFFFF, b + StringUtils.translate(k + "available"));
        }

        if (this.layout.colBoxes >= 0)
        {
            this.drawString(ctx, x0 + this.layout.colBoxes, y, 0xFFFFFFFF, b + StringUtils.translate(k + "boxes"));
        }

        if (this.layout.colStacks >= 0)
        {
            this.drawString(ctx, x0 + this.layout.colStacks, y, 0xFFFFFFFF, b + StringUtils.translate(k + "stacks"));
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
        ItemStack stack = row.getStack();
        int maxStack = stack.getMaxStackSize();
        int count = project.getRequiredCount(row);
        List<String> lines = new ArrayList<>();
        String k = "shulkerplanner.gui.hover.material.";

        lines.add(GuiBase.TXT_BOLD + row.getDisplayName() + GuiBase.TXT_RST + GuiBase.TXT_DARK_GRAY + "  " + row.getId());
        lines.add(StringUtils.translate(k + "total", Format.number(row.getTotal()), Format.stacks(row.getTotal(), maxStack)));
        lines.add(StringUtils.translate(k + "missing", Format.number(row.getMissing()), Format.stacks(row.getMissing(), maxStack)));
        lines.add(StringUtils.translate(k + "available", Format.number(row.getAvailable())));
        lines.add(StringUtils.translate(k + "required", project.getCountMode().getDisplayName(), Format.number(count),
                                        Format.stacks(count, maxStack), Format.boxes(count, maxStack)));
        lines.add(StringUtils.translate(k + "stack_size", maxStack));

        if (stack.getItem().canFitInsideContainerItems() == false)
        {
            lines.add(GuiBase.TXT_GOLD + StringUtils.translate(k + "not_boxable"));
        }

        if (project.isExcluded(row.getId()))
        {
            lines.add(GuiBase.TXT_RED + StringUtils.translate(k + "excluded"));
        }

        if (project.isGathered(row.getId()))
        {
            lines.add(GuiBase.TXT_GREEN + StringUtils.translate(k + "gathered"));
        }

        RenderUtils.drawHoverText(ctx, mouseX, mouseY, lines);
    }
}
