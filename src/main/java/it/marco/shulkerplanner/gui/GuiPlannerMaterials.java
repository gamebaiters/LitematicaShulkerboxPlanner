package it.marco.shulkerplanner.gui;

import java.util.ArrayList;
import java.util.List;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldInteger;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.input.ScanCodes;

import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.Reminder;
import it.marco.shulkerplanner.gui.widgets.WidgetListPlannerMaterials;
import it.marco.shulkerplanner.gui.widgets.WidgetPlannerMaterialEntry;
import it.marco.shulkerplanner.plan.ShulkerPlan;
import it.marco.shulkerplanner.util.Format;

public class GuiPlannerMaterials extends GuiPlannerBase<MaterialRow, WidgetPlannerMaterialEntry, WidgetListPlannerMaterials>
{
    public GuiPlannerMaterials()
    {
        super(PlannerTab.MATERIALS);
    }

    @Override
    protected WidgetListPlannerMaterials createListWidget(int listX, int listY)
    {
        return new WidgetListPlannerMaterials(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    @Override
    protected void createOptionButtons()
    {
        PlannerProject project = this.getProject();

        if (project == null)
        {
            return;
        }

        ButtonGeneric button = this.createValueButton("shulkerplanner.gui.button.count_mode", project.getCountMode().getDisplayName(), project.getCountMode().getHoverText());
        this.addFlowButton(button, (btn, mb) -> {
            project.setCountMode(project.getCountMode().cycle(mb == ScanCodes.OFFSET_MOUSE_LEFT));
            this.onProjectChanged();
        });

        button = this.createValueButton("shulkerplanner.gui.button.sort", project.getMaterialSort().getDisplayName(), null);
        this.addFlowButton(button, (btn, mb) -> {
            project.setMaterialSort(project.getMaterialSort().cycle(mb == ScanCodes.OFFSET_MOUSE_LEFT));
            this.onProjectChanged();
        });

        button = this.createOnOffButton("shulkerplanner.gui.button.hide_excluded", project.getHideExcluded(), null);
        this.addFlowButton(button, (btn, mb) -> {
            project.setHideExcluded(! project.getHideExcluded());
            this.onProjectChanged();
        });

        this.addFlowLabel(StringUtils.translate("shulkerplanner.gui.label.multiplier"));
        GuiTextFieldInteger tf = this.addFlowTextField((x, y, w, h) -> new GuiTextFieldInteger(x, y, w, h, this.font), 36, textField -> {
            try
            {
                int value = Integer.parseInt(textField.getValueWrapper());

                if (value != project.getMultiplier() && value > 0)
                {
                    project.setMultiplier(value);
                    this.session.onProjectChanged();
                    if (this.getListWidget() != null)
                    {
                        this.getListWidget().refreshEntries();
                    }
                }
            }
            catch (NumberFormatException ignore) {}

            return true;
        });
        tf.setValueWrapper(String.valueOf(project.getMultiplier()));

        button = new ButtonGeneric(0, 0, -1, 20, StringUtils.translate("shulkerplanner.gui.button.include_all"));
        this.addFlowButton(button, (btn, mb) -> {
            project.clearExcluded();
            this.onProjectChanged();
        });

        button = new ButtonGeneric(0, 0, -1, 20, StringUtils.translate("shulkerplanner.gui.button.exclude_all"));
        this.addFlowButton(button, (btn, mb) -> {
            for (MaterialRow row : project.getMaterials())
            {
                project.setExcluded(row.getId(), true);
            }
            this.onProjectChanged();
        });

        button = new ButtonGeneric(0, 0, -1, 20, StringUtils.translate("shulkerplanner.gui.button.exclude_gathered"),
                                   StringUtils.translate("shulkerplanner.gui.button.exclude_gathered.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            for (MaterialRow row : project.getMaterials())
            {
                if (project.isGathered(row.getId()))
                {
                    project.setExcluded(row.getId(), true);
                }
            }
            this.onProjectChanged();
        });

        button = new ButtonGeneric(0, 0, -1, 20, StringUtils.translate("shulkerplanner.gui.button.notes_for_missing"),
                                   StringUtils.translate("shulkerplanner.gui.button.notes_for_missing.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            int added = this.addRemindersForMissing(project);
            this.addMessage(MessageType.SUCCESS, 3000, "shulkerplanner.message.reminders_added", added);
            this.onProjectChanged();
        });
    }

    private int addRemindersForMissing(PlannerProject project)
    {
        int added = 0;

        for (MaterialRow row : project.getMaterials())
        {
            String id = row.getId();
            int count = project.getRequiredCount(row);

            if (project.isExcluded(id) || project.isGathered(id) || count <= 0 || project.hasReminderFor(id))
            {
                continue;
            }

            String amount = Format.number(count) + " (" + Format.stacks(count, row.getStack().getMaxStackSize()) + ")";
            project.addReminder(new Reminder(StringUtils.translate("shulkerplanner.reminder.gather_item", amount, row.getDisplayName()), id));
            ++added;
        }

        return added;
    }

    @Override
    protected List<String> getSummaryLines()
    {
        PlannerProject project = this.getProject();
        List<String> lines = new ArrayList<>();

        if (project == null)
        {
            return lines;
        }

        int total = project.getMaterials().size();
        int excluded = project.getExcludedCount();
        int gathered = 0;

        for (MaterialRow row : project.getMaterials())
        {
            if (project.isGathered(row.getId()) && project.isExcluded(row.getId()) == false)
            {
                ++gathered;
            }
        }

        ShulkerPlan plan = this.session.getPlan();
        lines.add(StringUtils.translate("shulkerplanner.gui.label.materials.summary1", total - excluded, total, gathered, total - excluded));
        lines.add(GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.gui.label.materials.summary2",
                                                           Format.number(plan.getTotalItems()), plan.getTotalStacks(), plan.getBoxCount()));

        return lines;
    }
}
