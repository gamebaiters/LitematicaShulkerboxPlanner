package it.marco.shulkerplanner.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.data.ShulkerBinding;
import it.marco.shulkerplanner.data.ShulkerTracker;
import it.marco.shulkerplanner.gui.ui.Theme;
import it.marco.shulkerplanner.gui.ui.Ui;
import it.marco.shulkerplanner.plan.BoxState;
import it.marco.shulkerplanner.plan.ItemRequirement;
import it.marco.shulkerplanner.plan.PlannedBox;
import it.marco.shulkerplanner.plan.ShulkerPlan;
import it.marco.shulkerplanner.util.Format;
import it.marco.shulkerplanner.util.InventoryScanner;

/**
 * The panel next to an opened shulker box screen.
 * <p>
 * A shulker box is only tied to a box of the plan when the player links it: an unlinked shulker box shows
 * a preview of the suggested box and a "Link" button, and nothing changes until it's pressed.
 * A linked shulker box stays linked when it's picked up and placed again, see {@link ShulkerTracker}.
 */
public class PackingAssistant
{
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_MIN_WIDTH = 132;
    private static final int LINE_HEIGHT = 18;
    private static final int MAX_ITEM_LINES = 8;

    @Nullable private static Object lastScreen;
    @Nullable private static ShulkerTracker.Opened opened;
    private static boolean identified;
    @Nullable private static ShulkerBinding binding;
    /** The box shown while the shulker box is not linked */
    private static int previewId = -1;
    /** The contents of the box when the screen was opened. Already complete boxes don't get auto-marked again. */
    @Nullable private static String initialSignature;
    @Nullable private static String lastSavedSignature;
    private static final List<Button> buttons = new ArrayList<>();

    private record Button(int x, int y, int w, int h, Runnable action)
    {
        boolean contains(double mx, double my)
        {
            return Ui.inside(mx, my, this.x, this.y, this.w, this.h);
        }
    }

    private static boolean isActive(AbstractContainerScreen<?> screen)
    {
        PlannerSession session = PlannerSession.INSTANCE;

        return Configs.Generic.PACKING_ASSISTANT.getBooleanValue() &&
               screen.getMenu() instanceof ShulkerBoxMenu &&
               session.getProject() != null &&
               session.getPlan().getBoxes().isEmpty() == false;
    }

    private static Map<String, Integer> getContainerContents(AbstractContainerMenu menu)
    {
        Map<String, Integer> contents = new LinkedHashMap<>();

        for (int i = 0; i < PlannedBox.SLOTS && i < menu.slots.size(); ++i)
        {
            ItemStack stack = menu.slots.get(i).getItem();

            if (stack.isEmpty() == false)
            {
                contents.merge(InventoryScanner.getId(stack), stack.getCount(), Integer::sum);
            }
        }

        return contents;
    }

    private static void reset(@Nullable Object screen)
    {
        lastScreen = screen;
        opened = null;
        identified = false;
        binding = null;
        previewId = -1;
        initialSignature = null;
        lastSavedSignature = null;
        PlannerSession.INSTANCE.setOpenBox(-1);
    }

    /**
     * Called when any screen is closed or replaced
     */
    public static void onScreenClosed()
    {
        if (lastScreen != null)
        {
            PlannerSession.INSTANCE.save();
            reset(null);
        }
    }

    /**
     * @return the box suggested for an unlinked shulker box: the followed box, the first box still to fill
     * that has no shulker box linked yet
     */
    private static PlannedBox suggestBox(ShulkerPlan plan, Map<String, Integer> contents)
    {
        PlannerSession session = PlannerSession.INSTANCE;
        PlannerProject project = session.getProject();
        PlannedBox followed = project != null ? plan.getBoxById(project.getFollowedBoxId()) : null;

        if (followed != null && session.getBinding(followed) == null && session.isBoxDone(followed) == false)
        {
            return followed;
        }

        // A filled shulker box that matches a box exactly
        if (contents.isEmpty() == false)
        {
            String signature = PlannedBox.createSignature(contents);

            for (PlannedBox box : plan.getBoxes())
            {
                if (box.getSignature().equals(signature) && session.getBinding(box) == null)
                {
                    return box;
                }
            }
        }

        for (PlannedBox box : plan.getBoxes())
        {
            if (session.getBinding(box) == null && session.isBoxReady(box) == false)
            {
                return box;
            }
        }

        return plan.getBoxes().getFirst();
    }

    /**
     * Called at the end of AbstractContainerScreen#extractContents, after the slots have been drawn
     */
    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                              int leftPos, int topPos, int imageWidth, int imageHeight)
    {
        if (screen != lastScreen)
        {
            if (lastScreen != null)
            {
                PlannerSession.INSTANCE.save();
            }

            reset(screen);
        }

        buttons.clear();

        if (isActive(screen) == false)
        {
            return;
        }

        PlannerSession session = PlannerSession.INSTANCE;
        PlannerProject project = session.getProject();
        ShulkerPlan plan = session.getPlan();
        AbstractContainerMenu menu = screen.getMenu();
        Map<String, Integer> contents = getContainerContents(menu);
        String signature = PlannedBox.createSignature(contents);
        GuiContext ctx = GuiContext.fromGuiGraphics(graphics);

        // The container contents arrive from the server a moment after the screen opens
        boolean synced = menu.getStateId() > 0;

        if (synced && identified == false)
        {
            identified = true;
            initialSignature = signature;
            opened = ShulkerTracker.INSTANCE.identifyOpened(screen.getTitle());
            binding = ShulkerTracker.INSTANCE.findBinding(project, opened, contents);

            if (binding == null && Configs.Generic.AUTO_LINK_OPENED_BOXES.getBooleanValue())
            {
                PlannedBox suggestion = suggestBox(plan, contents);

                if (session.getBinding(suggestion) == null)
                {
                    link(suggestion, contents);
                }
            }
        }

        // The link may have been removed from the planner meanwhile, or added externally
        if (binding != null && project.getBindings().contains(binding) == false)
        {
            binding = null;
        }
        else if (binding == null && identified && opened != null)
        {
            binding = ShulkerTracker.INSTANCE.findBinding(project, opened, contents);
        }

        PlannedBox target = binding != null ? plan.getBoxById(binding.getBoxId()) : null;

        if (binding != null && target == null)
        {
            binding = null;
        }

        boolean linked = target != null;

        if (linked == false)
        {
            target = plan.getBoxById(previewId);

            if (target == null)
            {
                target = suggestBox(plan, contents);
                previewId = target.getId();
            }

            session.setOpenBox(-1);
        }
        else
        {
            session.setOpenBox(target.getId());

            if (synced && signature.equals(lastSavedSignature) == false)
            {
                binding.setContents(contents);
                lastSavedSignature = signature;
                session.notifyUpdate();
            }
        }

        Map<String, Integer> planned = target.getTotals();
        boolean complete = contents.equals(new HashMap<>(planned));
        BoxState state = session.getBoxState(target);

        if (linked && complete && state != BoxState.DONE && initialSignature != null &&
            signature.equals(initialSignature) == false && Configs.Generic.AUTO_MARK_BOXES.getBooleanValue())
        {
            session.markBoxDone(target);
            state = BoxState.DONE;
        }

        if (linked || Configs.Warnings.HIGHLIGHT_UNLINKED.getBooleanValue())
        {
            renderSlotHighlights(menu, ctx, leftPos, topPos, contents, planned);
        }

        drawPanel(screen, ctx, mouseX, mouseY, leftPos, topPos, imageWidth, plan, target, linked, state, contents, complete);
    }

    /**
     * Links the currently open shulker box screen to the given planned box (same as clicking "+ Link")
     */
    public static void linkOpenBox(PlannedBox box)
    {
        if (lastScreen instanceof AbstractContainerScreen<?> screen && isActive(screen))
        {
            if (opened == null)
            {
                opened = ShulkerTracker.INSTANCE.identifyOpened(screen.getTitle());
            }

            link(box, getContainerContents(screen.getMenu()));
        }
    }

    private static void link(PlannedBox box, Map<String, Integer> contents)
    {
        PlannerSession session = PlannerSession.INSTANCE;
        PlannerProject project = session.getProject();

        if (project == null || opened == null)
        {
            return;
        }

        ShulkerBinding created = ShulkerTracker.INSTANCE.createBinding(box.getId(), opened, contents);
        session.link(box, created);
        binding = project.getBinding(box.getId());
        lastSavedSignature = PlannedBox.createSignature(contents);
    }

    private static void drawPanel(AbstractContainerScreen<?> screen, GuiContext ctx, int mouseX, int mouseY, int leftPos, int topPos, int imageWidth,
                                  ShulkerPlan plan, PlannedBox target, boolean linked, BoxState state,
                                  Map<String, Integer> contents, boolean complete)
    {
        PlannerSession session = PlannerSession.INSTANCE;
        Map<String, Integer> planned = target.getTotals();
        List<String> wrong = new ArrayList<>();

        for (String id : contents.keySet())
        {
            if (planned.containsKey(id) == false)
            {
                wrong.add(id);
            }
        }

        boolean showWrong = Configs.Warnings.PANEL_WRONG_ITEMS.getBooleanValue() && (linked || wrong.isEmpty() == false);
        int itemLines = Math.min(planned.size(), MAX_ITEM_LINES);
        int wrongLines = showWrong ? Math.min(wrong.size(), 3) : 0;
        boolean hint = linked == false && Configs.Warnings.HINT_UNLINKED_BOX.getBooleanValue();

        int height = 8 + 18 + 10 + 6 + itemLines * LINE_HEIGHT + (planned.size() > itemLines ? 10 : 0)
                     + (wrongLines > 0 ? 12 + wrongLines * LINE_HEIGHT : 0) + 14 + (hint ? 22 : 0) + 22 + 6;

        int spaceRight = screen.width - (leftPos + imageWidth) - 8;
        int spaceLeft = leftPos - 8;
        boolean right = spaceRight >= PANEL_WIDTH || spaceRight >= spaceLeft;
        int width = Math.max(PANEL_MIN_WIDTH, Math.min(PANEL_WIDTH, right ? spaceRight : spaceLeft));
        int x = right ? leftPos + imageWidth + 4 : leftPos - width - 4;
        x = Math.max(2, Math.min(x, screen.width - width - 2));
        int y = Math.max(2, Math.min(topPos, screen.height - height - 2));

        int accent = state == BoxState.DONE ? Theme.SUCCESS : (linked ? Theme.ACCENT : Theme.BORDER_STRONG);
        Ui.panel(ctx, x, y, width, height, 0xF2140F1C, accent);
        Ui.rect(ctx, x + 1, y + 1, width - 2, 2, accent);

        int tx = x + 7;
        int ty = y + 7;
        int inner = width - 14;

        // Header: shulker icon, box number, link state
        PlannerProject project = session.getProject();
        ItemStack icon = project != null ? new ItemStack(project.getShulkerItem(target.getIndex())) : new ItemStack(Items.SHULKER_BOX);
        Ui.item(ctx, icon, tx, ty);
        String title = StringUtils.translate("shulkerplanner.assistant.box", target.getIndex(), plan.getBoxCount());
        Ui.textShadow(ctx, title, tx + 20, ty + 1, Theme.TEXT);

        String chip;
        int chipFg;
        int chipBg;

        if (state == BoxState.DONE)
        {
            chip = "✔ " + StringUtils.translate("shulkerplanner.assistant.state.done");
            chipFg = Theme.SUCCESS;
            chipBg = Theme.SUCCESS_BG;
        }
        else if (linked)
        {
            chip = StringUtils.translate("shulkerplanner.assistant.state.linked");
            chipFg = Theme.ACCENT_HOVER;
            chipBg = Theme.ACCENT_BG;
        }
        else
        {
            chip = StringUtils.translate("shulkerplanner.assistant.state.preview");
            chipFg = Theme.TEXT_DIM;
            chipBg = Theme.SURFACE_3;
        }

        if (linked && state != BoxState.DONE)
        {
            Ui.lockChip(ctx, chip, tx + 20, ty + 10, chipFg, chipBg);
        }
        else
        {
            Ui.chip(ctx, chip, tx + 20, ty + 10, chipFg, chipBg);
        }
        ty += 22;

        // Progress bar
        long need = target.getItemCount();
        long inside = 0;

        for (Map.Entry<String, Integer> e : planned.entrySet())
        {
            inside += Math.min(e.getValue(), contents.getOrDefault(e.getKey(), 0));
        }

        String pct = need > 0 ? (int) (100 * inside / need) + "%" : "";
        Ui.progress(ctx, tx, ty + 1, inner - Ui.width(pct) - 4, 5, need > 0 ? (double) inside / need : 0,
                    complete ? Theme.SUCCESS : Theme.ACCENT, Theme.SURFACE_3);
        Ui.textRight(ctx, pct, tx + inner, ty, Theme.TEXT_DIM);
        ty += 12;

        // Items
        int line = 0;

        for (Map.Entry<String, Integer> e : planned.entrySet())
        {
            if (line++ >= MAX_ITEM_LINES)
            {
                break;
            }

            int want = e.getValue();
            int have = contents.getOrDefault(e.getKey(), 0);
            ItemRequirement req = target.getItems().get(e.getKey());
            Ui.item(ctx, req.proto(), tx, ty);

            int color = have == want ? Theme.SUCCESS : (have > want ? Theme.WARN : (have > 0 ? Theme.TEXT : Theme.TEXT_DIM));
            String mark = have == want ? "✔ " : (have > want ? "▲ " : "");
            String count = mark + Format.number(have) + "/" + Format.number(want);
            Ui.text(ctx, count, tx + 19, ty, color);
            int cw = Ui.width(count);
            Ui.text(ctx, Ui.trim(req.proto().getHoverName().getString(), inner - 23 - cw), tx + 23 + cw, ty, Theme.TEXT_DIM);

            if (have < want)
            {
                int inInv = countInPlayerSlots(screen.getMenu(), e.getKey());
                String sub = StringUtils.translate("shulkerplanner.assistant.to_put_in", Format.stacks(want - have, req.maxStack()))
                             + "  " + StringUtils.translate("shulkerplanner.assistant.in_inventory", Format.number(inInv));
                Ui.text(ctx, Ui.trim(sub, inner - 19), tx + 19, ty + 9, inInv >= want - have ? Theme.INFO : Theme.TEXT_MUTED);
            }
            else if (have > want)
            {
                Ui.text(ctx, StringUtils.translate("shulkerplanner.assistant.too_many", Format.number(have - want)), tx + 19, ty + 9, Theme.WARN);
            }

            ty += LINE_HEIGHT;
        }

        if (planned.size() > itemLines)
        {
            Ui.text(ctx, StringUtils.translate("shulkerplanner.gui.label.box.more", planned.size() - itemLines), tx, ty, Theme.TEXT_MUTED);
            ty += 10;
        }

        if (wrongLines > 0)
        {
            Ui.text(ctx, "⚠ " + StringUtils.translate("shulkerplanner.assistant.remove"), tx, ty + 2, Theme.DANGER);
            ty += 12;

            for (int i = 0; i < wrongLines; ++i)
            {
                String id = wrong.get(i);
                ItemStack stack = new MaterialRow(id, 0, 0).getStack();
                Ui.item(ctx, stack, tx, ty);
                Ui.text(ctx, Ui.trim(Format.number(contents.get(id)) + "  " + stack.getHoverName().getString(), inner - 20), tx + 19, ty + 4, Theme.DANGER);
                ty += LINE_HEIGHT;
            }
        }

        // Status
        String status;
        int statusColor;
        int missing = 0;

        for (Map.Entry<String, Integer> e : planned.entrySet())
        {
            missing += Math.max(0, e.getValue() - contents.getOrDefault(e.getKey(), 0));
        }

        if (complete)
        {
            status = StringUtils.translate("shulkerplanner.assistant.complete");
            statusColor = Theme.SUCCESS;
        }
        else if (wrong.isEmpty() == false && showWrong)
        {
            status = StringUtils.translate("shulkerplanner.assistant.wrong_items");
            statusColor = Theme.DANGER;
        }
        else
        {
            status = StringUtils.translate("shulkerplanner.assistant.missing", Format.number(missing));
            statusColor = Theme.WARN;
        }

        Ui.text(ctx, Ui.trim(status, inner), tx, ty + 3, statusColor);
        ty += 14;

        if (hint)
        {
            Ui.panel(ctx, tx - 2, ty, inner + 4, 20, Theme.INFO_BG);
            Ui.text(ctx, Ui.trim(StringUtils.translate("shulkerplanner.assistant.hint_unlinked1"), inner), tx + 1, ty + 2, Theme.INFO);
            Ui.text(ctx, Ui.trim(StringUtils.translate("shulkerplanner.assistant.hint_unlinked2"), inner), tx + 1, ty + 11, Theme.TEXT_DIM);
            ty += 22;
        }

        // Actions
        int by = ty + 2;

        if (linked)
        {
            int bx = tx;
            bx += button(ctx, mouseX, mouseY, bx, by, "✕ " + StringUtils.translate("shulkerplanner.assistant.unlink"), false,
                         () -> { session.unlink(target); binding = null; previewId = target.getId(); }) + 3;

            String doneLabel = state == BoxState.DONE ? StringUtils.translate("shulkerplanner.assistant.undo_done")
                                                      : "✔ " + StringUtils.translate("shulkerplanner.assistant.mark_done");
            bx += button(ctx, mouseX, mouseY, bx, by, doneLabel, false,
                         () -> session.setBoxState(target, state == BoxState.DONE ? BoxState.FILLING : BoxState.DONE)) + 3;

            boolean followed = session.isFollowed(target);
            button(ctx, mouseX, mouseY, bx, by, followed ? "◉ HUD" : "○ HUD", followed,
                   () -> session.setFollowedBox(followed ? null : target));
        }
        else
        {
            int count = plan.getBoxCount();
            List<PlannedBox> boxes = plan.getBoxes();
            int index = boxes.indexOf(target);
            int bx = tx;
            bx += button(ctx, mouseX, mouseY, bx, by, "◀", false, () -> previewId = boxes.get(Math.floorMod(index - 1, count)).getId()) + 2;
            bx += button(ctx, mouseX, mouseY, bx, by, "▶", false, () -> previewId = boxes.get(Math.floorMod(index + 1, count)).getId()) + 4;

            ShulkerBinding other = session.getBinding(target);
            String label = "+ " + StringUtils.translate("shulkerplanner.assistant.link", target.getIndex());
            boolean canLink = other == null;

            if (canLink)
            {
                button(ctx, mouseX, mouseY, bx, by, label, true, () -> link(target, contents));
            }
            else
            {
                Ui.text(ctx, Ui.trim(StringUtils.translate("shulkerplanner.assistant.already_linked"), tx + inner - bx), bx, by + 5, Theme.TEXT_MUTED);
            }
        }
    }

    /**
     * Draws a small flat button and registers its click area
     * @return the width
     */
    private static int button(GuiContext ctx, int mouseX, int mouseY, int x, int y, String label, boolean primary, Runnable action)
    {
        int w = Ui.width(label) + 10;
        int h = 16;
        boolean hover = Ui.inside(mouseX, mouseY, x, y, w, h);
        int bg = primary ? (hover ? Theme.ACCENT_HOVER : Theme.ACCENT) : (hover ? Theme.SURFACE_HOVER : Theme.SURFACE_3);
        Ui.panel(ctx, x, y, w, h, bg, primary ? bg : (hover ? Theme.BORDER_STRONG : Theme.BORDER));
        Ui.text(ctx, label, x + 5, y + 4, primary ? 0xFFFFFFFF : Theme.TEXT);
        buttons.add(new Button(x, y, w, h, action));
        return w;
    }

    private static int countInPlayerSlots(AbstractContainerMenu menu, String id)
    {
        int count = 0;

        for (int i = PlannedBox.SLOTS; i < menu.slots.size(); ++i)
        {
            ItemStack stack = menu.slots.get(i).getItem();

            if (stack.isEmpty() == false && InventoryScanner.getId(stack).equals(id))
            {
                count += stack.getCount();
            }
        }

        return count;
    }

    private static void renderSlotHighlights(AbstractContainerMenu menu, GuiContext ctx, int leftPos, int topPos,
                                             Map<String, Integer> contents, Map<String, Integer> planned)
    {
        boolean wrongSlots = Configs.Warnings.HIGHLIGHT_WRONG_SLOTS.getBooleanValue();
        boolean overfilled = Configs.Warnings.HIGHLIGHT_OVERFILLED.getBooleanValue();
        boolean toMove = Configs.Warnings.HIGHLIGHT_ITEMS_TO_MOVE.getBooleanValue();

        for (int i = 0; i < menu.slots.size(); ++i)
        {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();

            if (stack.isEmpty() || slot.isActive() == false)
            {
                continue;
            }

            String id = InventoryScanner.getId(stack);
            int want = planned.getOrDefault(id, 0);
            int have = contents.getOrDefault(id, 0);
            int color = 0;

            if (i < PlannedBox.SLOTS)
            {
                if (want == 0 && wrongSlots)
                {
                    color = 0x70FF2020;
                }
                else if (want > 0 && have > want && overfilled)
                {
                    color = 0x70FFA000;
                }
            }
            else if (want > have && toMove)
            {
                color = 0x6020E020;
            }

            if (color != 0)
            {
                int sx = leftPos + slot.x;
                int sy = topPos + slot.y;
                Ui.rect(ctx, sx, sy, 16, 16, color);
            }
        }
    }

    /**
     * @return true if the click was handled by the assistant panel
     */
    public static boolean onMouseClicked(AbstractContainerScreen<?> screen, double mouseX, double mouseY)
    {
        if (screen != lastScreen || isActive(screen) == false)
        {
            return false;
        }

        for (Button button : List.copyOf(buttons))
        {
            if (button.contains(mouseX, mouseY))
            {
                button.action().run();
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
        }

        return false;
    }
}
