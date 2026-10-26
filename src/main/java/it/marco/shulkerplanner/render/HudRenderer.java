package it.marco.shulkerplanner.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.Reference;
import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.config.HudBoxView;
import it.marco.shulkerplanner.config.HudCountFormat;
import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.data.Reminder;
import it.marco.shulkerplanner.plan.BoxProgress;
import it.marco.shulkerplanner.plan.PlannedBox;
import it.marco.shulkerplanner.plan.ShulkerPlan;
import it.marco.shulkerplanner.util.Format;

/**
 * The in-game HUD: the schematic, the progress, the shulker box being filled (as the real
 * box interface and/or a compact list of what's missing) and the open reminders.
 * Everything is re-calculated every frame from the session, which follows the inventory every tick.
 */
public class HudRenderer implements IRenderer
{
    public static final HudRenderer INSTANCE = new HudRenderer();

    public static final int COLOR_IN_BOX = 0xFF4CA64C;
    public static final int COLOR_IN_INVENTORY = 0xFF3F8FBF;
    public static final int COLOR_PARTIAL = 0xFFC9A227;
    public static final int COLOR_MISSING = 0xFFB34444;

    private static final int TEXT_LINE = 10;
    private static final int ITEM_LINE = 17;
    private static final int PADDING = 4;
    private static final int MAX_TEXT_WIDTH = 250;
    private static final int SLOT = 18;
    private static final int GRID_WIDTH = 9 * SLOT + 8;
    private static final int GRID_HEIGHT = 3 * SLOT + 8;

    private int lastX;
    private int lastY;
    private int lastWidth;
    private int lastHeight;

    @Override
    public Supplier<String> getProfilerSectionSupplier()
    {
        return () -> Reference.MOD_ID + "_hud";
    }

    @Override
    public void onExtractGuiOverlayPost(GuiContext ctx, float partialTicks, ProfilerFiller profiler)
    {
        if (Configs.Generic.HUD_ENABLED.getBooleanValue() == false ||
            ctx.mc().player == null ||
            GuiUtils.getCurrentScreen() != null ||
            ctx.mc().gui.hud.isHidden() ||
            ctx.mc().debugEntries.isOverlayVisible())
        {
            return;
        }

        this.render(ctx);
    }

    /**
     * @return true if something was drawn
     */
    public boolean render(GuiContext ctx)
    {
        PlannerSession session = PlannerSession.INSTANCE;
        PlannerProject project = session.getProject();
        List<Element> elements = new ArrayList<>();

        if (session.getUnplannedSelection() != null)
        {
            // The selected placement has no plan yet: say how to make one, instead of showing another schematic
            elements.add(Line.text(GuiBase.TXT_LIGHT_PURPLE + GuiBase.TXT_BOLD + Format.trim(session.getUnplannedSelection(), MAX_TEXT_WIDTH)));
            elements.add(Line.text(GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.hud.no_plan")));
            return this.draw(ctx, elements);
        }

        if (project == null || session.isLoadedInGame() == false)
        {
            this.lastWidth = 0;
            this.lastHeight = 0;
            return false;
        }

        ShulkerPlan plan = session.getPlan();

        elements.add(Line.text(GuiBase.TXT_LIGHT_PURPLE + GuiBase.TXT_BOLD + Format.trim(session.getDisplayName(), MAX_TEXT_WIDTH)));

        if (Configs.Generic.HUD_SHOW_PROGRESS.getBooleanValue())
        {
            this.addProgress(elements, project, session, plan);
        }

        if (Configs.Generic.HUD_SHOW_CURRENT_BOX.getBooleanValue())
        {
            this.addCurrentBox(elements, session, plan);
        }

        if (Configs.Generic.HUD_MAX_LINES.getIntegerValue() > 0)
        {
            this.addReminders(elements, project);
        }

        return this.draw(ctx, elements);
    }

    private boolean draw(GuiContext ctx, List<Element> elements)
    {
        int width = 0;
        int height = 0;

        for (Element element : elements)
        {
            width = Math.max(width, element.width());
            height += element.height();
        }

        int totalWidth = width + PADDING * 2 + 2;
        int totalHeight = height + PADDING * 2 - 2;
        int x = Configs.Generic.HUD_X.getIntegerValue();
        int y = Configs.Generic.HUD_Y.getIntegerValue();

        // Keep the HUD on the screen when the window gets smaller
        x = Math.max(0, Math.min(x, ctx.guiWidth() - totalWidth));
        y = Math.max(0, Math.min(y, ctx.guiHeight() - totalHeight));

        this.lastX = x;
        this.lastY = y;
        this.lastWidth = totalWidth;
        this.lastHeight = totalHeight;

        RenderUtils.drawRect(ctx, x, y, totalWidth, totalHeight, 0xA8000000);
        RenderUtils.drawRect(ctx, x, y, 2, totalHeight, 0xFFA060E0);

        int ly = y + PADDING;

        for (Element element : elements)
        {
            element.draw(ctx, x + PADDING + 2, ly);
            ly += element.height();
        }

        return true;
    }

    public int getLastX()
    {
        return this.lastX;
    }

    public int getLastY()
    {
        return this.lastY;
    }

    public int getLastWidth()
    {
        return this.lastWidth;
    }

    public int getLastHeight()
    {
        return this.lastHeight;
    }

    private void addProgress(List<Element> elements, PlannerProject project, PlannerSession session, ShulkerPlan plan)
    {
        int ready = session.getReadyBoxCount();
        int count = plan.getBoxCount();
        int included = 0;
        int gathered = 0;

        for (MaterialRow row : project.getMaterials())
        {
            if (project.isExcluded(row.getId()) == false)
            {
                ++included;

                if (project.isGathered(row.getId()))
                {
                    ++gathered;
                }
            }
        }

        String c1 = ready >= count ? GuiBase.TXT_GREEN : GuiBase.TXT_YELLOW;
        String c2 = gathered >= included ? GuiBase.TXT_GREEN : GuiBase.TXT_YELLOW;
        elements.add(Line.text(GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.hud.progress",
                                                                        c1 + ready + "/" + count + GuiBase.TXT_GRAY,
                                                                        c2 + gathered + "/" + included + GuiBase.TXT_GRAY)));
    }

    private void addCurrentBox(List<Element> elements, PlannerSession session, ShulkerPlan plan)
    {
        if (plan.getBoxes().isEmpty())
        {
            return;
        }

        BoxProgress progress = session.getCurrentProgress();

        if (progress == null)
        {
            elements.add(Line.text(GuiBase.TXT_GREEN + StringUtils.translate("shulkerplanner.hud.all_boxes_ready")));
            return;
        }

        PlannedBox box = progress.getBox();
        HudBoxView view = Configs.Generic.getHudBoxView();
        HudCountFormat format = Configs.Generic.getHudCountFormat();
        String type = box.isPure() ? StringUtils.translate("shulkerplanner.gui.label.box.pure")
                                   : StringUtils.translate("shulkerplanner.gui.label.box.mixed", box.getTotals().size());
        String status = progress.isContentsKnown() ? GuiBase.TXT_GREEN + "  ● " + StringUtils.translate("shulkerplanner.hud.live") : "";

        elements.add(Line.text(GuiBase.TXT_AQUA + StringUtils.translate("shulkerplanner.hud.current_box", box.getIndex(), plan.getBoxCount())
                               + GuiBase.TXT_GRAY + "  " + type + status));

        // Totals: in the box / in the inventory / still missing
        elements.add(Line.text(StringUtils.translate("shulkerplanner.hud.box_totals",
                                                     GuiBase.TXT_GREEN + Format.number(progress.getInBox()) + "/" + Format.number(progress.getTotal()) + GuiBase.TXT_GRAY,
                                                     GuiBase.TXT_AQUA + Format.number(progress.getInInventory()) + GuiBase.TXT_GRAY,
                                                     (progress.getMissing() > 0 ? GuiBase.TXT_RED : GuiBase.TXT_GREEN) + Format.number(progress.getMissing()) + GuiBase.TXT_GRAY)));

        if (view.showGrid())
        {
            elements.add(new Grid(progress));
            elements.add(Line.text(GuiBase.TXT_GREEN + "■" + GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.hud.legend.in_box") + "  "
                                   + GuiBase.TXT_AQUA + "■" + GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.hud.legend.in_inventory") + "  "
                                   + GuiBase.TXT_GOLD + "■" + GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.hud.legend.partial") + "  "
                                   + GuiBase.TXT_RED + "■" + GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.hud.legend.missing")));
        }

        if (view.showList())
        {
            int maxLines = Configs.Generic.HUD_MAX_BOX_LINES.getIntegerValue();
            int shown = 0;

            for (BoxProgress.Item item : progress.getItems())
            {
                if (shown >= maxLines)
                {
                    break;
                }

                elements.add(Line.item(item.item().proto(), this.formatItem(item, format)));
                ++shown;
            }

            if (progress.getItems().size() > shown)
            {
                elements.add(Line.text(GuiBase.TXT_GRAY + "  " + StringUtils.translate("shulkerplanner.gui.label.box.more", progress.getItems().size() - shown)));
            }
        }

        for (Map.Entry<String, Integer> e : progress.getExtra().entrySet())
        {
            ItemStack stack = new MaterialRow(e.getKey(), 0, 0).getStack();
            elements.add(Line.item(stack, GuiBase.TXT_RED + StringUtils.translate("shulkerplanner.hud.remove", Format.number(e.getValue()), stack.getHoverName().getString())));
        }

        if (progress.getMissing() == 0 && progress.getInBox() < progress.getTotal())
        {
            elements.add(Line.text(GuiBase.TXT_GREEN + StringUtils.translate("shulkerplanner.hud.box_items_ready")));
        }
        else if (progress.getMissing() > 0)
        {
            elements.add(Line.text(GuiBase.TXT_GOLD + StringUtils.translate("shulkerplanner.hud.box_items_missing",
                                                                            Format.number(progress.getMissing()), progress.getMissingTypes())));
        }
    }

    private String formatItem(BoxProgress.Item item, HudCountFormat format)
    {
        int maxStack = item.item().maxStack();
        String name = Format.trim(item.item().proto().getHoverName().getString(), 110);

        return switch (item.state())
        {
            case IN_BOX -> GuiBase.TXT_GREEN + "✔ " + name;
            case IN_INVENTORY -> GuiBase.TXT_AQUA + name + GuiBase.TXT_GRAY + "  "
                                 + StringUtils.translate("shulkerplanner.hud.put_in", GuiBase.TXT_AQUA + format.format(item.toPutIn(), maxStack));
            case PARTIAL -> GuiBase.TXT_YELLOW + name + GuiBase.TXT_GRAY + "  "
                            + StringUtils.translate("shulkerplanner.hud.missing_have", GuiBase.TXT_RED + format.format(item.missing(), maxStack) + GuiBase.TXT_GRAY,
                                                    GuiBase.TXT_AQUA + format.format(item.inBox() + item.inInventory(), maxStack));
            case MISSING -> GuiBase.TXT_RED + name + GuiBase.TXT_GRAY + "  "
                            + StringUtils.translate("shulkerplanner.hud.missing", GuiBase.TXT_RED + format.format(item.missing(), maxStack));
        };
    }

    private void addReminders(List<Element> elements, PlannerProject project)
    {
        int max = Configs.Generic.HUD_MAX_LINES.getIntegerValue();
        List<Reminder> open = new ArrayList<>();

        for (Reminder reminder : project.getReminders())
        {
            if (reminder.isDone() == false)
            {
                open.add(reminder);
            }
        }

        if (open.isEmpty())
        {
            if (project.getReminders().isEmpty() == false)
            {
                elements.add(Line.text(GuiBase.TXT_GREEN + StringUtils.translate("shulkerplanner.hud.all_done")));
            }

            return;
        }

        elements.add(Line.text(GuiBase.TXT_YELLOW + StringUtils.translate("shulkerplanner.hud.reminders", open.size())));

        for (int i = 0; i < open.size() && i < max; ++i)
        {
            Reminder reminder = open.get(i);
            String text = GuiBase.TXT_WHITE + " ○ " + Format.trim(reminder.getText(), MAX_TEXT_WIDTH - 12);

            if (reminder.getItemId() != null)
            {
                elements.add(Line.item(new MaterialRow(reminder.getItemId(), 0, 0).getStack(), text));
            }
            else
            {
                elements.add(Line.text(text));
            }
        }

        if (open.size() > max)
        {
            elements.add(Line.text(GuiBase.TXT_GRAY + "   " + StringUtils.translate("shulkerplanner.hud.more", open.size() - max)));
        }
    }

    public static int getStateColor(BoxProgress.State state)
    {
        return switch (state)
        {
            case IN_BOX -> COLOR_IN_BOX;
            case IN_INVENTORY -> COLOR_IN_INVENTORY;
            case PARTIAL -> COLOR_PARTIAL;
            case MISSING -> COLOR_MISSING;
        };
    }

    private interface Element
    {
        int width();

        int height();

        void draw(GuiContext ctx, int x, int y);
    }

    private record Line(@Nullable ItemStack icon, String text) implements Element
    {
        static Line text(String text)
        {
            return new Line(null, text);
        }

        static Line item(ItemStack icon, String text)
        {
            return new Line(icon, text);
        }

        @Override
        public int width()
        {
            return StringUtils.getStringWidth(this.text) + (this.icon != null ? 18 : 0);
        }

        @Override
        public int height()
        {
            return this.icon != null ? ITEM_LINE : TEXT_LINE;
        }

        @Override
        public void draw(GuiContext ctx, int x, int y)
        {
            if (this.icon != null)
            {
                ctx.renderItem(this.icon, x, y);
                ctx.drawString(ctx.fontRenderer(), this.text, x + 18, y + 4, 0xFFFFFFFF, true);
            }
            else
            {
                ctx.drawString(ctx.fontRenderer(), this.text, x, y, 0xFFFFFFFF, true);
            }
        }
    }

    /**
     * The box drawn like the vanilla shulker box screen, every slot tinted by its state
     */
    private record Grid(BoxProgress progress) implements Element
    {
        @Override
        public int width()
        {
            return GRID_WIDTH;
        }

        @Override
        public int height()
        {
            return GRID_HEIGHT + 3;
        }

        @Override
        public void draw(GuiContext ctx, int x, int y)
        {
            // Vanilla style panel
            RenderUtils.drawRect(ctx, x, y + 1, GRID_WIDTH, GRID_HEIGHT, 0xFFC6C6C6);
            RenderUtils.drawRect(ctx, x, y + 1, GRID_WIDTH, 1, 0xFFFFFFFF);
            RenderUtils.drawRect(ctx, x, y + 1, 1, GRID_HEIGHT, 0xFFFFFFFF);
            RenderUtils.drawRect(ctx, x, y + GRID_HEIGHT, GRID_WIDTH, 1, 0xFF555555);
            RenderUtils.drawRect(ctx, x + GRID_WIDTH - 1, y + 1, 1, GRID_HEIGHT, 0xFF555555);

            List<BoxProgress.Slot> slots = this.progress.getSlots();

            for (int i = 0; i < PlannedBox.SLOTS; ++i)
            {
                int sx = x + 4 + (i % 9) * SLOT;
                int sy = y + 5 + (i / 9) * SLOT;

                // Slot frame: dark top/left, white bottom/right, like the vanilla slots
                RenderUtils.drawRect(ctx, sx, sy, SLOT, SLOT, 0xFF8B8B8B);
                RenderUtils.drawRect(ctx, sx, sy, SLOT - 1, 1, 0xFF373737);
                RenderUtils.drawRect(ctx, sx, sy, 1, SLOT - 1, 0xFF373737);
                RenderUtils.drawRect(ctx, sx + 1, sy + SLOT - 1, SLOT - 1, 1, 0xFFFFFFFF);
                RenderUtils.drawRect(ctx, sx + SLOT - 1, sy + 1, 1, SLOT - 1, 0xFFFFFFFF);

                if (i < slots.size())
                {
                    BoxProgress.Slot slot = slots.get(i);
                    int color = getStateColor(slot.state());
                    RenderUtils.drawRect(ctx, sx + 1, sy + 1, SLOT - 2, SLOT - 2, (color & 0x00FFFFFF) | 0xC0000000);

                    ItemStack stack = slot.stack().createStack();
                    ctx.renderItem(stack, sx + 1, sy + 1);
                    ctx.renderItemDecorations(ctx.fontRenderer(), stack, sx + 1, sy + 1);
                }
            }
        }
    }
}
