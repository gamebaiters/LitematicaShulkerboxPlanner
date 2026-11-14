package it.marco.shulkerplanner.gui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.world.item.DyeColor;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.input.ScanCodes;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.ProjectStorage;
import it.marco.shulkerplanner.gui.ui.FlatButton;
import it.marco.shulkerplanner.gui.ui.Theme;
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
    private static boolean showMoreOptions;
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

        FlatButton button = this.createValueButton("shulkerplanner.gui.button.pack_mode", project.getPackMode().getDisplayName(), project.getPackMode().getHoverText());
        this.addFlowButton(button, (btn, mb) -> {
            project.setPackMode(project.getPackMode().cycle(mb == ScanCodes.OFFSET_MOUSE_LEFT));
            this.onPackingOptionsChanged();
        });

        button = this.createValueButton("shulkerplanner.gui.button.count_mode", project.getCountMode().getDisplayName(), project.getCountMode().getHoverText());
        this.addFlowButton(button, (btn, mb) -> {
            project.setCountMode(project.getCountMode().cycle(mb == ScanCodes.OFFSET_MOUSE_LEFT));
            this.onPackingOptionsChanged();
        });

        button = this.createToggle("shulkerplanner.gui.button.pack_raw", project.getPackRawMaterials(),
                                   StringUtils.translate("shulkerplanner.gui.button.pack_raw.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            project.setPackRawMaterials(! project.getPackRawMaterials());
            this.onPackingOptionsChanged();
        });

        button = this.createToggle("shulkerplanner.gui.button.hide_ready", hideReady, StringUtils.translate("shulkerplanner.gui.button.hide_ready.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            hideReady = ! hideReady;
            this.initGui();
        });

        button = new FlatButton(0, 0, -1, (showMoreOptions ? "▴ " : "▾ ") + StringUtils.translate("shulkerplanner.gui.button.more_options"), FlatButton.Style.GHOST);
        this.addFlowButton(button, (btn, mb) -> {
            showMoreOptions = ! showMoreOptions;
            this.initGui();
        });

        if (showMoreOptions == false)
        {
            return;
        }

        this.newFlowRow();

        button = this.createToggle("shulkerplanner.gui.button.round_up", project.getRoundUpStacks(),
                                   StringUtils.translate("shulkerplanner.gui.button.round_up.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            project.setRoundUpStacks(! project.getRoundUpStacks());
            this.onPackingOptionsChanged();
        });

        button = this.createToggle("shulkerplanner.gui.button.fill_spare", project.getFillSpareSlots(),
                                   StringUtils.translate("shulkerplanner.gui.button.fill_spare.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            project.setFillSpareSlots(! project.getFillSpareSlots());
            this.onPackingOptionsChanged();
        });

        button = this.createValueButton("shulkerplanner.gui.button.color", getColorName(project.getBoxColor()),
                                        StringUtils.translate("shulkerplanner.gui.button.color.hover"));
        this.addFlowButton(button, (btn, mb) -> {
            project.setBoxColor(project.getBoxColor() + (mb == ScanCodes.OFFSET_MOUSE_LEFT ? 1 : -1));
            this.onProjectChanged();
        });

        if (BoxItemFactory.canGiveItems())
        {
            button = this.createAction("shulkerplanner.gui.button.give_all", StringUtils.translate("shulkerplanner.gui.button.give_all.hover"));
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
        }

        button = this.createAction("shulkerplanner.gui.button.export", StringUtils.translate("shulkerplanner.gui.button.export.hover"));
        this.addFlowButton(button, (btn, mb) -> this.export(project));

        if (this.session.isPlanLocked())
        {
            button = this.createAction("shulkerplanner.gui.button.reset_plan", StringUtils.translate("shulkerplanner.gui.button.reset_plan.hover"));
            button.setStyle(FlatButton.Style.DANGER);
            this.addFlowButton(button, (btn, mb) -> {
                if (GuiBase.isShiftDown())
                {
                    this.session.resetPlan();
                    this.addMessage(MessageType.SUCCESS, 2500, "shulkerplanner.message.plan_reset");
                    this.initGui();
                }
                else
                {
                    this.addMessage(MessageType.WARNING, 3000, "shulkerplanner.message.hold_shift_to_reset");
                }
            });
        }
    }

    private void export(PlannerProject project)
    {
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
    }

    @Override
    @Nullable
    protected Banner createBanner()
    {
        PlannerProject project = this.getProject();

        if (project == null || this.session.getPlan().isEmpty())
        {
            return null;
        }

        if (this.session.isPlanLocked())
        {
            if (this.session.isPlanOutdated() && Configs.Warnings.WARN_PLAN_OUTDATED.getBooleanValue())
            {
                Map<String, Long> changes = this.session.getPlanChanges();
                long more = 0;
                long less = 0;

                for (long d : changes.values())
                {
                    if (d > 0) more += d; else less -= d;
                }

                String text = StringUtils.translate("shulkerplanner.gui.banner.outdated", changes.size(), Format.number(more), Format.number(less));
                return new Banner(text, Theme.WARN, Theme.WARN_BG, StringUtils.translate("shulkerplanner.gui.button.update_plan"), () -> {
                    this.session.updateLockedPlan();
                    this.addMessage(MessageType.SUCCESS, 2500, "shulkerplanner.message.plan_updated");
                    this.initGui();
                });
            }

            return new Banner(StringUtils.translate("shulkerplanner.gui.banner.locked"), Theme.ACCENT_HOVER, Theme.ACCENT_BG, null, null);
        }

        return new Banner(StringUtils.translate("shulkerplanner.gui.banner.draft"), Theme.TEXT_DIM, Theme.SURFACE_2,
                          StringUtils.translate("shulkerplanner.gui.button.lock_plan"), () -> {
                              this.session.lockPlan();
                              this.addMessage(MessageType.SUCCESS, 2500, "shulkerplanner.message.plan_locked");
                              this.initGui();
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
        lines.add(boxInfo + GuiBase.TXT_RST + "   " + readyColor + StringUtils.translate("shulkerplanner.gui.label.boxes.ready", ready, count));

        String line2 = GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.gui.label.boxes.summary_items", Format.number(plan.getTotalItems()), plan.getTotalStacks());

        if (plan.getLooseItems().isEmpty() == false)
        {
            List<String> loose = new ArrayList<>();

            for (ItemRequirement req : plan.getLooseItems())
            {
                loose.add(req.count() + "x " + new MaterialRow(req.id(), 0, 0).getDisplayName());
            }

            line2 += GuiBase.TXT_GOLD + "   " + StringUtils.translate("shulkerplanner.gui.label.boxes.loose", String.join(", ", loose));
        }

        lines.add(line2);

        return lines;
    }
}
