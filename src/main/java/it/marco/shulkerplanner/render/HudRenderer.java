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
import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.data.Reminder;
import it.marco.shulkerplanner.plan.ItemRequirement;
import it.marco.shulkerplanner.plan.PlannedBox;
import it.marco.shulkerplanner.plan.ShulkerPlan;
import it.marco.shulkerplanner.util.Format;

/**
 * The in-game HUD: active project, progress, the shulker box being filled with what's
 * still missing for it, and the open reminders.
 */
public class HudRenderer implements IRenderer
{
    public static final HudRenderer INSTANCE = new HudRenderer();

    private static final int TEXT_LINE = 10;
    private static final int ITEM_LINE = 17;
    private static final int PADDING = 4;
    private static final int MAX_TEXT_WIDTH = 240;

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

        PlannerProject project = PlannerSession.INSTANCE.getProject();

        if (project != null)
        {
            this.render(ctx, project, PlannerSession.INSTANCE);
        }
    }

    private void render(GuiContext ctx, PlannerProject project, PlannerSession session)
    {
        List<Line> lines = new ArrayList<>();
        ShulkerPlan plan = session.getPlan();

        lines.add(Line.text(GuiBase.TXT_LIGHT_PURPLE + GuiBase.TXT_BOLD + Format.trim(project.getName(), MAX_TEXT_WIDTH)));

        if (Configs.Generic.HUD_SHOW_PROGRESS.getBooleanValue())
        {
            this.addProgress(lines, project, session, plan);
        }

        if (Configs.Generic.HUD_SHOW_CURRENT_BOX.getBooleanValue())
        {
            this.addCurrentBox(lines, session, plan);
        }

        if (Configs.Generic.HUD_MAX_LINES.getIntegerValue() > 0)
        {
            this.addReminders(lines, project);
        }

        int width = 0;
        int height = 0;

        for (Line line : lines)
        {
            width = Math.max(width, line.width());
            height += line.height();
        }

        int x = Configs.Generic.HUD_X.getIntegerValue();
        int y = Configs.Generic.HUD_Y.getIntegerValue();

        // Keep the HUD on the screen when the window gets smaller
        x = Math.max(0, Math.min(x, ctx.guiWidth() - width - PADDING * 2));
        y = Math.max(0, Math.min(y, ctx.guiHeight() - height - PADDING * 2));

        RenderUtils.drawRect(ctx, x, y, width + PADDING * 2, height + PADDING * 2 - 2, 0xA0000000);
        RenderUtils.drawRect(ctx, x, y, 2, height + PADDING * 2 - 2, 0xFFA060E0);

        int ly = y + PADDING;

        for (Line line : lines)
        {
            line.draw(ctx, x + PADDING + 1, ly);
            ly += line.height();
        }
    }

    private void addProgress(List<Line> lines, PlannerProject project, PlannerSession session, ShulkerPlan plan)
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
        lines.add(Line.text(GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.hud.progress",
                                                                     c1 + ready + "/" + count + GuiBase.TXT_GRAY,
                                                                     c2 + gathered + "/" + included + GuiBase.TXT_GRAY)));
    }

    private void addCurrentBox(List<Line> lines, PlannerSession session, ShulkerPlan plan)
    {
        if (plan.getBoxes().isEmpty())
        {
            return;
        }

        PlannedBox box = session.getCurrentBox();

        if (box == null)
        {
            lines.add(Line.text(GuiBase.TXT_GREEN + StringUtils.translate("shulkerplanner.hud.all_boxes_ready")));
            return;
        }

        String type = box.isPure() ? StringUtils.translate("shulkerplanner.gui.label.box.pure")
                                   : StringUtils.translate("shulkerplanner.gui.label.box.mixed", box.getTotals().size());
        lines.add(Line.text(GuiBase.TXT_AQUA + StringUtils.translate("shulkerplanner.hud.current_box", box.getIndex(), plan.getBoxCount())
                            + GuiBase.TXT_GRAY + "  " + type));

        int maxLines = Configs.Generic.HUD_MAX_BOX_LINES.getIntegerValue();
        int shown = 0;
        int missingTypes = 0;
        int missingItems = 0;

        // Items still missing first, then the ones that are already there
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(box.getTotals().entrySet());
        entries.sort((a, b) -> Boolean.compare(session.getLoose(a.getKey()) >= a.getValue(), session.getLoose(b.getKey()) >= b.getValue()));

        for (Map.Entry<String, Integer> e : entries)
        {
            int need = e.getValue();
            int have = Math.min(need, session.getLoose(e.getKey()));

            if (have < need)
            {
                ++missingTypes;
                missingItems += need - have;
            }

            if (shown >= maxLines)
            {
                continue;
            }

            ItemRequirement req = box.getItems().get(e.getKey());
            String color = have >= need ? GuiBase.TXT_GREEN : (have > 0 ? GuiBase.TXT_YELLOW : GuiBase.TXT_RED);
            String text = color + Format.number(have) + GuiBase.TXT_GRAY + "/" + Format.number(need) + " " + GuiBase.TXT_WHITE
                          + Format.trim(req.proto().getHoverName().getString(), 120);

            if (have < need)
            {
                text += GuiBase.TXT_GRAY + "  -" + Format.stacks(need - have, req.maxStack());
            }

            lines.add(Line.item(req.proto(), text));
            ++shown;
        }

        if (entries.size() > shown)
        {
            lines.add(Line.text(GuiBase.TXT_GRAY + "  " + StringUtils.translate("shulkerplanner.gui.label.box.more", entries.size() - shown)));
        }

        if (missingTypes == 0)
        {
            lines.add(Line.text(GuiBase.TXT_GREEN + StringUtils.translate("shulkerplanner.hud.box_items_ready")));
        }
        else
        {
            lines.add(Line.text(GuiBase.TXT_GOLD + StringUtils.translate("shulkerplanner.hud.box_items_missing", Format.number(missingItems), missingTypes)));
        }
    }

    private void addReminders(List<Line> lines, PlannerProject project)
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
                lines.add(Line.text(GuiBase.TXT_GREEN + StringUtils.translate("shulkerplanner.hud.all_done")));
            }

            return;
        }

        lines.add(Line.text(GuiBase.TXT_YELLOW + StringUtils.translate("shulkerplanner.hud.reminders", open.size())));

        for (int i = 0; i < open.size() && i < max; ++i)
        {
            lines.add(Line.text(GuiBase.TXT_WHITE + " ○ " + Format.trim(open.get(i).getText(), MAX_TEXT_WIDTH - 12)));
        }

        if (open.size() > max)
        {
            lines.add(Line.text(GuiBase.TXT_GRAY + "   " + StringUtils.translate("shulkerplanner.hud.more", open.size() - max)));
        }
    }

    private record Line(@Nullable ItemStack icon, String text)
    {
        static Line text(String text)
        {
            return new Line(null, text);
        }

        static Line item(ItemStack icon, String text)
        {
            return new Line(icon, text);
        }

        int width()
        {
            return StringUtils.getStringWidth(this.text) + (this.icon != null ? 18 : 0);
        }

        int height()
        {
            return this.icon != null ? ITEM_LINE : TEXT_LINE;
        }

        void draw(GuiContext ctx, int x, int y)
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
}
