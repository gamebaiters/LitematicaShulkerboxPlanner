package it.marco.shulkerplanner.gui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.DyeColor;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.input.ScanCodes;

import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.ProjectStorage;
import it.marco.shulkerplanner.gui.widgets.WidgetListPlannerBoxes;
import it.marco.shulkerplanner.gui.widgets.WidgetPlannerBoxEntry;
import it.marco.shulkerplanner.plan.ItemRequirement;
import it.marco.shulkerplanner.plan.PlannedBox;
import it.marco.shulkerplanner.plan.ShulkerPlan;
import it.marco.shulkerplanner.util.BoxItemFactory;
import it.marco.shulkerplanner.util.Format;

public class GuiPlannerBoxes extends GuiPlannerBase<PlannedBox, WidgetPlannerBoxEntry, WidgetListPlannerBoxes>
{
    private static final long EXPORT_COOLDOWN_MS = 2000;
    private static boolean hideReady;
    private static long lastExport;

    public GuiPlannerBoxes()
    {
        super(PlannerTab.BOXES);
    }

    @Override
    protected WidgetListPlannerBoxes createListWidget(int listX, int listY)
    {
        return new WidgetListPlannerBoxes(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    public int getPlanBoxCount()
    {
        return this.session.getPlan().getBoxCount();
    }

    public List<PlannedBox> getVisibleBoxes()
    {
        List<PlannedBox> boxes = this.session.getPlan().getBoxes();

        if (hideReady == false)
        {
            return boxes;
        }

        List<PlannedBox> list = new ArrayList<>();

        for (PlannedBox box : boxes)
        {
            if (this.session.isBoxReady(box) == false)
            {
                list.add(box);
            }
        }

        return list;
    }

    public static String getColorName(int color)
    {
        if (color == PlannerProject.COLOR_RAINBOW)
        {
            return StringUtils.translate("shulkerplanner.color.rainbow");
        }
        else if (color <= PlannerProject.COLOR_UNDYED || color > 16)
        {
            return StringUtils.translate("shulkerplanner.color.undyed");
        }

        return StringUtils.translate("color.minecraft." + DyeColor.byId(color - 1).getName());
    }

    @Override
    protected void createOptionButtons()
    {
        PlannerProject project = this.getProject();

        if (project == null)
        {
            return;
        }

        ButtonGeneric button = this.createValueButton("shulkerplanner.gui.button.pack_mode", project.getPackMode().getDisplayName(), project.getPackMode().getHoverText());
        this.addFlowButton(button, (btn, mb) -> {
            project.setPackMode(project.getPackMode().cycle(mb == ScanCodes.OFFSET_MOUSE_LEFT));
            this.onProjectChanged();
        });

        button = this.createValueButton("shulkerplanner.gui.button.count_mode", project.getCountMode().getDisplayName(), project.getCountMode().getHoverText());
        this.addFlowButton(button, (btn, mb) -> {
            project.setCountMode(project.getCountMode().cycle(mb == ScanCodes.OFFSET_MOUSE_LEFT));
            this.onProjectChanged();
        });

        button = this.createOnOffButton("shulkerplanner.gui.button.round_up", project.getRoundUpStacks(),
                                        StringUtils.translate("shulkerplanner.gui.button.round_up.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            project.setRoundUpStacks(! project.getRoundUpStacks());
            this.onProjectChanged();
        });

        button = this.createOnOffButton("shulkerplanner.gui.button.fill_spare", project.getFillSpareSlots(),
                                        StringUtils.translate("shulkerplanner.gui.button.fill_spare.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            project.setFillSpareSlots(! project.getFillSpareSlots());
            this.onProjectChanged();
        });

        button = this.createValueButton("shulkerplanner.gui.button.color", getColorName(project.getBoxColor()),
                                        StringUtils.translate("shulkerplanner.gui.button.color.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            project.setBoxColor(project.getBoxColor() + (mb == ScanCodes.OFFSET_MOUSE_LEFT ? 1 : -1));
            this.onProjectChanged();
        });

        button = this.createOnOffButton("shulkerplanner.gui.button.hide_ready", hideReady, null);
        this.addFlowButton(button, (btn, mb) -> {
            hideReady = ! hideReady;
            this.initGui();
        });

        button = new ButtonGeneric(0, 0, -1, 20, StringUtils.translate("shulkerplanner.gui.button.give_all"),
                                   StringUtils.translate("shulkerplanner.gui.button.give_all.hover"));
        button.setEnabled(BoxItemFactory.canGiveItems());
        this.addFlowButton(button, (btn, mb) -> {
            int given = BoxItemFactory.giveAllMissing(this.session);
            int remaining = 0;

            for (PlannedBox box : this.session.getPlan().getBoxes())
            {
                if (this.session.isBoxReady(box) == false)
                {
                    ++remaining;
                }
            }

            if (remaining > 0)
            {
                this.addMessage(MessageType.WARNING, 4000, "shulkerplanner.message.boxes_given_partial", given, remaining);
            }
            else
            {
                this.addMessage(MessageType.SUCCESS, 3000, "shulkerplanner.message.boxes_given", given);
            }

            this.initGui();
        });

        button = new ButtonGeneric(0, 0, -1, 20, StringUtils.translate("shulkerplanner.gui.button.export"),
                                   StringUtils.translate("shulkerplanner.gui.button.export.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            long now = System.currentTimeMillis();

            if (now - lastExport < EXPORT_COOLDOWN_MS)
            {
                this.addMessage(MessageType.WARNING, 1500, "shulkerplanner.message.export_cooldown");
                return;
            }

            lastExport = now;
            String text = BoxItemFactory.createExportText(project, this.session.getPlan());

            if (GuiBase.isShiftDown())
            {
                this.mc.keyboardHandler.setClipboard(text);
                this.addMessage(MessageType.SUCCESS, 3000, "shulkerplanner.message.copied_to_clipboard");
            }
            else
            {
                Path file = ProjectStorage.writeExport(project.getName(), text);

                if (file != null)
                {
                    this.addMessage(MessageType.SUCCESS, 5000, "shulkerplanner.message.exported", file.getFileName().toString());
                }
                else
                {
                    this.addMessage(MessageType.ERROR, 4000, "shulkerplanner.message.export_failed");
                }
            }
        });

        button = new ButtonGeneric(0, 0, -1, 20, StringUtils.translate("shulkerplanner.gui.button.reset_boxes"),
                                   StringUtils.translate("shulkerplanner.gui.button.reset_boxes.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            if (GuiBase.isShiftDown())
            {
                project.clearDoneBoxes();
                this.onProjectChanged();
            }
            else
            {
                this.addMessage(MessageType.WARNING, 3000, "shulkerplanner.message.hold_shift_to_reset");
            }
        });
    }

    @Override
    protected List<String> getSummaryLines()
    {
        List<String> lines = new ArrayList<>();
        ShulkerPlan plan = this.session.getPlan();
        PlannerProject project = this.getProject();

        if (project == null)
        {
            return lines;
        }

        if (plan.isEmpty())
        {
            lines.add(GuiBase.TXT_GOLD + StringUtils.translate(project.getMaterials().isEmpty() ? "shulkerplanner.gui.label.boxes.no_materials"
                                                                                                : "shulkerplanner.gui.label.boxes.nothing_to_pack"));
            return lines;
        }

        int count = plan.getBoxCount();
        int ready = this.session.getReadyBoxCount();
        List<PlannedBox> boxes = plan.getBoxes();
        String boxInfo;

        if (count == plan.getFullBoxCount())
        {
            boxInfo = StringUtils.translate("shulkerplanner.gui.label.boxes.summary_all_full", count);
        }
        else
        {
            int lastUsed = boxes.isEmpty() ? 0 : boxes.getLast().getUsedSlots();
            boxInfo = StringUtils.translate("shulkerplanner.gui.label.boxes.summary_partial", count, plan.getFullBoxCount(), plan.getFreeSlots(), lastUsed);
        }

        String readyColor = ready >= count ? GuiBase.TXT_GREEN : GuiBase.TXT_YELLOW;
        lines.add(boxInfo + GuiBase.TXT_RST + "  " + readyColor + StringUtils.translate("shulkerplanner.gui.label.boxes.ready", ready, count));

        String line2 = GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.gui.label.boxes.summary_items", Format.number(plan.getTotalItems()), plan.getTotalStacks());

        if (plan.getLooseItems().isEmpty() == false)
        {
            List<String> loose = new ArrayList<>();

            for (ItemRequirement req : plan.getLooseItems())
            {
                loose.add(req.count() + "x " + req.proto().getHoverName().getString());
            }

            line2 += GuiBase.TXT_GOLD + "  " + StringUtils.translate("shulkerplanner.gui.label.boxes.loose", String.join(", ", loose));
        }

        lines.add(Format.trim(line2, this.getScreenWidth() - MARGIN * 2 - 4));

        return lines;
    }
}
