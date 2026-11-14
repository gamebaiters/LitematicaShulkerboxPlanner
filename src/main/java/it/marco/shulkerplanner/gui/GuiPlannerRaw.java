package it.marco.shulkerplanner.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.gui.ui.PlannerButton;
import it.marco.shulkerplanner.gui.ui.Theme;
import it.marco.shulkerplanner.gui.widgets.WidgetListPlannerRaw;
import it.marco.shulkerplanner.gui.widgets.WidgetPlannerRawEntry;
import it.marco.shulkerplanner.recipe.RawMaterialCalculator;
import it.marco.shulkerplanner.recipe.RecipeService;
import it.marco.shulkerplanner.util.Format;

/**
 * The raw materials needed to craft everything on the material list, and the crafting steps to get there
 */
public class GuiPlannerRaw extends GuiPlannerBase<RawRow, WidgetPlannerRawEntry, WidgetListPlannerRaw>
{
    public GuiPlannerRaw()
    {
        super(PlannerTab.RAW);
    }

    @Override
    protected WidgetListPlannerRaw createListWidget(int listX, int listY)
    {
        return new WidgetListPlannerRaw(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    public List<RawRow> getRows()
    {
        List<RawRow> rows = new ArrayList<>();
        RawMaterialCalculator.Result result = this.session.getRawResult();

        if (result == null)
        {
            return rows;
        }

        long total = 0;

        for (long v : result.raw().values())
        {
            total += v;
        }

        rows.add(RawRow.header(RawRow.Kind.HEADER_RAW, total));

        for (Map.Entry<String, Long> e : result.raw().entrySet())
        {
            rows.add(new RawRow(RawRow.Kind.RAW, e.getKey(), e.getValue(), null));
        }

        if (result.steps().isEmpty() == false)
        {
            rows.add(RawRow.header(RawRow.Kind.HEADER_STEPS, result.steps().size()));

            for (RawMaterialCalculator.Step step : result.steps())
            {
                rows.add(new RawRow(RawRow.Kind.STEP, step.item(), step.needed(), step));
            }
        }

        return rows;
    }

    @Override
    protected void createOptionButtons()
    {
        PlannerProject project = this.getProject();

        if (project == null)
        {
            return;
        }

        PlannerButton button = this.createToggle("shulkerplanner.gui.button.pack_raw", project.getPackRawMaterials(),
                                              StringUtils.translate("shulkerplanner.gui.button.pack_raw.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            project.setPackRawMaterials(! project.getPackRawMaterials());
            this.onPackingOptionsChanged();
        });

        this.addFlowGap();

        button = this.createToggle("shulkerplanner.gui.button.allow_smelting", project.getAllowSmelting(),
                                   StringUtils.translate("shulkerplanner.gui.button.allow_smelting.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            project.setAllowSmelting(! project.getAllowSmelting());
            this.onRecipeOptionsChanged();
        });

        button = this.createToggle("shulkerplanner.gui.button.allow_stonecutter", project.getAllowStonecutter(),
                                   StringUtils.translate("shulkerplanner.gui.button.allow_stonecutter.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            project.setAllowStonecutter(! project.getAllowStonecutter());
            this.onRecipeOptionsChanged();
        });

        if (project.getRawBaseItems().isEmpty() == false || project.getRecipeChoice().isEmpty() == false)
        {
            button = this.createAction("shulkerplanner.gui.button.reset_recipes", StringUtils.translate("shulkerplanner.gui.button.reset_recipes.hover"));
            this.addFlowButton(button, (btn, mb) -> {
                project.getRawBaseItems().clear();
                project.getRecipeChoice().clear();
                this.onRecipeOptionsChanged();
            });
        }
    }

    /**
     * Recipe options only change the plan when raw materials are packed
     */
    public void onRecipeOptionsChanged()
    {
        PlannerProject project = this.getProject();

        if (project != null && project.getPackRawMaterials())
        {
            this.onPackingOptionsChanged();
        }
        else
        {
            this.onProjectChanged();
        }
    }

    @Override
    @Nullable
    protected Banner createBanner()
    {
        String source = StringUtils.translate(RecipeService.isFromWorld() ? "shulkerplanner.gui.banner.recipes_world"
                                                                          : "shulkerplanner.gui.banner.recipes_vanilla", RecipeService.getIndex().size());
        return new Banner(source, Theme.INFO, Theme.INFO_BG, null, null);
    }

    @Override
    protected List<String> getSummaryLines()
    {
        List<String> lines = new ArrayList<>();
        RawMaterialCalculator.Result result = this.session.getRawResult();
        PlannerProject project = this.getProject();

        if (result == null || project == null)
        {
            return lines;
        }

        long raw = 0;
        long stacks = 0;

        for (Map.Entry<String, Long> e : result.raw().entrySet())
        {
            raw += e.getValue();
            stacks += Math.ceilDiv(e.getValue(), 64);
        }

        long listed = 0;

        for (long v : this.session.getListedCounts().values())
        {
            listed += v;
        }

        lines.add(StringUtils.translate("shulkerplanner.gui.label.raw.summary1", result.raw().size(), Format.number(raw), result.steps().size()));
        lines.add(GuiBase.TXT_GRAY + StringUtils.translate(project.getPackRawMaterials() ? "shulkerplanner.gui.label.raw.summary_packing_raw"
                                                                                        : "shulkerplanner.gui.label.raw.summary_packing_listed",
                                                           Format.number(listed)));
        return lines;
    }

    public void showMessage(MessageType type, String key, Object... args)
    {
        this.addMessage(type, 2500, key, args);
    }
}
