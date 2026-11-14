package it.marco.shulkerplanner.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.compat.JeiCompat;
import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.data.ShulkerBinding;
import it.marco.shulkerplanner.data.ShulkerTracker;
import it.marco.shulkerplanner.gui.ui.LayoutProbe;
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
 * <p>
 * The panel is anchored to the container screen: by default it's next to it, top aligned and as tall as it.
 * It can be dragged by its title bar and resized from the bottom right corner, the position is saved
 * relative to the container screen. The list of items scrolls with the mouse wheel or the scroll bar.
 */
public class PackingAssistant
{
    private static final int DEFAULT_WIDTH = 176;
    private static final int MIN_WIDTH = 120;
    private static final int MIN_HEIGHT = 96;
    private static final int PAD = 6;
    private static final int TITLE_HEIGHT = 20;
    private static final int ROW_HEIGHT = 20;
    private static final int HEADER_ROW_HEIGHT = 12;
    private static final int BUTTON_HEIGHT = 16;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int GRIP = 9;
    private static final int SIDE_RIGHT = 1;
    private static final int SIDE_LEFT = 2;

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
    private static final List<Row> rows = new ArrayList<>();

    // The panel as drawn in the last frame, in screen coordinates
    private static int panelX;
    private static int panelY;
    private static int panelW;
    private static int panelH;
    private static boolean panelVisible;
    // The scrolled list inside the panel
    private static int listX;
    private static int listY;
    private static int listW;
    private static int listH;
    private static int scroll;
    private static int contentHeight;
    private static int shownBoxId = -1;
    // The container screen the panel is anchored to
    private static int anchorLeft;
    private static int anchorTop;
    private static int anchorWidth;

    private enum Drag
    {
        NONE,
        MOVE,
        RESIZE,
        SCROLLBAR
    }

    private static Drag drag = Drag.NONE;
    private static boolean pressedInPanel;
    private static double dragStartX;
    private static double dragStartY;
    private static int dragStartPanelX;
    private static int dragStartPanelY;
    private static int dragStartW;
    private static int dragStartH;
    private static int dragStartScroll;
    @Nullable private static HoveredItem hoveredItem;

    public record HoveredItem(ItemStack stack, int x, int y, int w, int h)
    {
    }

    private record Button(int x, int y, int w, int h, Runnable action)
    {
        boolean contains(double mx, double my)
        {
            return Ui.inside(mx, my, this.x, this.y, this.w, this.h);
        }
    }

    /**
     * One line of the scrolled list
     */
    private record Row(int height, @Nullable String id, int want, int have, boolean wrong, boolean header)
    {
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
        panelVisible = false;
        scroll = 0;
        shownBoxId = -1;
        drag = Drag.NONE;
        pressedInPanel = false;
        hoveredItem = null;
    }

    /**
     * Called when any screen is closed or replaced
     */
    public static void onScreenClosed()
    {
        if (lastScreen != null)
        {
            if (drag == Drag.MOVE || drag == Drag.RESIZE)
            {
                Configs.saveToFile();
            }

            PlannerSession.INSTANCE.refreshInventory(true);
            PlannerSession.INSTANCE.save();
            reset(null);
        }
    }

    /**
     * @return the box suggested for an unlinked shulker box: the followed box, the first box still to fill
     * that has no shulker box linked yet, or null if every box is already linked
     */
    @Nullable
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

        for (PlannedBox box : plan.getBoxes())
        {
            if (session.getBinding(box) == null)
            {
                return box;
            }
        }

        return null;
    }

    private static List<PlannedBox> unlinkedBoxes(ShulkerPlan plan, PlannerSession session)
    {
        List<PlannedBox> result = new ArrayList<>();

        for (PlannedBox box : plan.getBoxes())
        {
            if (session.getBinding(box) == null)
            {
                result.add(box);
            }
        }

        return result;
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
        hoveredItem = null;

        if (isActive(screen) == false)
        {
            panelVisible = false;
            return;
        }

        if (drag != Drag.NONE)
        {
            // Move, resize and the scroll bar follow the mouse every frame, so they work even when another mod
            // takes the mouseDragged events of the container screen (they only end with the button release)
            Minecraft mc = Minecraft.getInstance();
            onMouseDragged(screen, mc.mouseHandler.getScaledXPos(mc.getWindow()), mc.mouseHandler.getScaledYPos(mc.getWindow()));
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

                if (suggestion != null && session.getBinding(suggestion) == null)
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

            if (target == null || session.getBinding(target) != null)
            {
                target = suggestBox(plan, contents);
                previewId = target != null ? target.getId() : -1;
            }
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

        if (target == null)
        {
            anchorLeft = leftPos;
            anchorTop = topPos;
            anchorWidth = imageWidth;
            drawAllLinkedPanel(screen, ctx, mouseX, mouseY, plan);
            return;
        }

        Map<String, Integer> planned = target.getTotals();
        boolean complete = contents.equals(new HashMap<>(planned));
        BoxState state = linked ? session.getBoxState(target) : BoxState.TODO;

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

        if (target.getId() != shownBoxId)
        {
            shownBoxId = target.getId();
            scroll = 0;
        }

        anchorLeft = leftPos;
        anchorTop = topPos;
        anchorWidth = imageWidth;
        drawPanel(screen, ctx, mouseX, mouseY, imageHeight, plan, target, linked, state, contents, complete);
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

    // ---------------------------------------------------------------- geometry

    /**
     * Puts the panel back next to the container screen, with the automatic size
     */
    public static void resetGeometry()
    {
        Configs.Generic.ASSISTANT_SIDE.setIntegerValue(0);
        Configs.Generic.ASSISTANT_GAP.setIntegerValue(4);
        Configs.Generic.ASSISTANT_OFFSET_Y.setIntegerValue(0);
        Configs.Generic.ASSISTANT_WIDTH.setIntegerValue(0);
        Configs.Generic.ASSISTANT_HEIGHT.setIntegerValue(0);
        Configs.saveToFile();
    }

    /**
     * @return the panel as drawn in the last frame: x, y, width, height. Width 0 if it's not shown.
     */
    public static int[] getPanelBounds()
    {
        return panelVisible ? new int[] { panelX, panelY, panelW, panelH } : new int[] { 0, 0, 0, 0 };
    }

    /**
     * @return the scroll position of the list and the maximum scroll position
     */
    public static int[] getScroll()
    {
        return new int[] { scroll, getMaxScroll() };
    }

    private static int getMaxScroll()
    {
        return Math.max(0, contentHeight - listH);
    }

    public static boolean isMouseOverPanel(double mouseX, double mouseY)
    {
        return panelVisible && Ui.inside(mouseX, mouseY, panelX, panelY, panelW, panelH);
    }

    @Nullable
    public static HoveredItem getHoveredItem(double mouseX, double mouseY)
    {
        HoveredItem item = hoveredItem;

        if (item != null && panelVisible && Ui.inside(mouseX, mouseY, item.x(), item.y(), item.w(), item.h()))
        {
            return item;
        }

        return null;
    }

    /**
     * Saves where the panel is, relative to the container screen it's anchored to
     */
    private static void storeAnchor(int x, int y, int w)
    {
        int containerRight = anchorLeft + anchorWidth;
        boolean right = x + w / 2 >= anchorLeft + anchorWidth / 2;
        Configs.Generic.ASSISTANT_SIDE.setIntegerValue(right ? SIDE_RIGHT : SIDE_LEFT);
        Configs.Generic.ASSISTANT_GAP.setIntegerValue(right ? x - containerRight : anchorLeft - (x + w));
        Configs.Generic.ASSISTANT_OFFSET_Y.setIntegerValue(y - anchorTop);
    }

    // ---------------------------------------------------------------- drawing

    private static int stackLines(List<String> labels, int width)
    {
        int lines = 1;
        int x = 0;

        for (String label : labels)
        {
            int w = Ui.width(label) + 10;

            if (x > 0 && x + w > width)
            {
                ++lines;
                x = 0;
            }

            x += w + 3;
        }

        return lines;
    }

    private static void drawAllLinkedPanel(AbstractContainerScreen<?> screen, GuiContext ctx, int mouseX, int mouseY, ShulkerPlan plan)
    {
        int sw = screen.width;
        int sh = screen.height;
        int containerRight = anchorLeft + anchorWidth;
        int side = Configs.Generic.ASSISTANT_SIDE.getIntegerValue();
        int gap = Configs.Generic.ASSISTANT_GAP.getIntegerValue();

        if (side != SIDE_RIGHT && side != SIDE_LEFT)
        {
            int spaceRight = sw - containerRight - 6;
            int spaceLeft = anchorLeft - 6;
            side = spaceRight >= MIN_WIDTH || spaceRight >= spaceLeft ? SIDE_RIGHT : SIDE_LEFT;
            gap = 4;
        }

        int space = side == SIDE_RIGHT ? sw - containerRight - gap - 2 : anchorLeft - gap - 2;
        int w = Configs.Generic.ASSISTANT_WIDTH.getIntegerValue();

        if (w <= 0)
        {
            w = Math.max(MIN_WIDTH, Math.min(DEFAULT_WIDTH, space));
        }

        w = Math.max(MIN_WIDTH, Math.min(w, sw - 4));
        int h = TITLE_HEIGHT + 34;

        if (drag == Drag.RESIZE)
        {
            w = panelW;
            h = Math.max(h, panelH);
        }

        int x = drag == Drag.MOVE || drag == Drag.RESIZE ? panelX : (side == SIDE_RIGHT ? containerRight + gap : anchorLeft - gap - w);
        int y = drag == Drag.MOVE || drag == Drag.RESIZE ? panelY : anchorTop + Configs.Generic.ASSISTANT_OFFSET_Y.getIntegerValue();
        x = Math.max(2, Math.min(x, sw - w - 2));
        y = Math.max(2, Math.min(y, sh - h - 2));
        panelX = x;
        panelY = y;
        panelW = w;
        panelH = h;
        panelVisible = true;
        listX = 0;
        listY = 0;
        listW = 0;
        listH = 0;
        contentHeight = 0;

        int alpha = Math.round(255 * Configs.Generic.ASSISTANT_OPACITY.getIntegerValue() / 100f);
        Ui.framed(ctx, x, y, w, h, Theme.withAlpha(0x140F1C, alpha), Theme.withAlpha(Theme.BORDER_STRONG, Math.max(alpha, 0xC0)));
        Ui.gradient(ctx, x + 1, y + 1, w - 2, TITLE_HEIGHT - 1, Theme.withAlpha(Theme.BORDER_STRONG, Math.min(255, alpha / 2 + 0x40)),
                    Theme.withAlpha(0x2B2238, Math.min(255, alpha + 0x20)));
        Ui.hline(ctx, x + 1, y + TITLE_HEIGHT, w - 2, Theme.withAlpha(Theme.BORDER_STRONG, 0x90));

        int tx = x + PAD;
        int right = x + w - PAD;
        boolean overTitle = Ui.inside(mouseX, mouseY, x, y, w, TITLE_HEIGHT) && drag == Drag.NONE;
        Ui.dragHandle(ctx, tx, y + 6, overTitle || drag == Drag.MOVE ? Theme.TEXT : Theme.TEXT_MUTED);
        Ui.item(ctx, new ItemStack(Items.SHULKER_BOX), tx + 8, y + 4, 12, false);

        int small = 14;
        int resetX = right - small;
        boolean hoverReset = Ui.inside(mouseX, mouseY, resetX, y + 3, small, small) && drag == Drag.NONE;
        Ui.button3d(ctx, resetX, y + 3, small, small, hoverReset ? Ui.BEVEL_QUIET.hovered() : Ui.BEVEL_QUIET, false);
        Ui.resetIcon(ctx, resetX + 4, y + 5, hoverReset ? Theme.TEXT : Theme.TEXT_DIM);
        buttons.add(new Button(resetX, y + 3, small, small, () -> {
            resetGeometry();
            scroll = 0;
        }));

        String title = StringUtils.translate("shulkerplanner.gui.tab.boxes") + " (" + plan.getBoxCount() + ")";
        Ui.textShadow(ctx, Ui.trim(title, resetX - 4 - (tx + 24)), tx + 24, y + 7, Theme.TEXT);
        Ui.text(ctx, Ui.trim(StringUtils.translate("shulkerplanner.assistant.all_linked"), w - 2 * PAD), tx, y + TITLE_HEIGHT + 10, Theme.TEXT_DIM);

        if (drag == Drag.NONE && hoverReset)
        {
            Ui.tooltip(ctx, mouseX, mouseY, List.of(StringUtils.translate("shulkerplanner.assistant.hover.reset")));
        }
    }

    private static void drawPanel(AbstractContainerScreen<?> screen, GuiContext ctx, int mouseX, int mouseY, int imageHeight,
                                  ShulkerPlan plan, PlannedBox target, boolean linked, BoxState state,
                                  Map<String, Integer> contents, boolean complete)
    {
        PlannerSession session = PlannerSession.INSTANCE;
        PlannerProject project = session.getProject();
        Map<String, Integer> planned = target.getTotals();
        List<String> wrong = new ArrayList<>();

        for (String id : contents.keySet())
        {
            if (planned.containsKey(id) == false)
            {
                wrong.add(id);
            }
        }

        boolean showWrong = Configs.Warnings.PANEL_WRONG_ITEMS.getBooleanValue() && wrong.isEmpty() == false;
        boolean hint = linked == false && Configs.Warnings.HINT_UNLINKED_BOX.getBooleanValue();

        // The rows of the scrolled list
        rows.clear();

        for (Map.Entry<String, Integer> e : planned.entrySet())
        {
            rows.add(new Row(ROW_HEIGHT, e.getKey(), e.getValue(), contents.getOrDefault(e.getKey(), 0), false, false));
        }

        if (showWrong)
        {
            rows.add(new Row(HEADER_ROW_HEIGHT, null, 0, 0, true, true));

            for (String id : wrong)
            {
                rows.add(new Row(ROW_HEIGHT, id, 0, contents.get(id), true, false));
            }
        }

        contentHeight = 0;

        for (Row row : rows)
        {
            contentHeight += row.height();
        }

        // The action buttons, which wrap to more lines on a narrow panel
        List<String> labels = new ArrayList<>();
        String doneLabel = state == BoxState.DONE ? StringUtils.translate("shulkerplanner.assistant.undo_done")
                                                  : "✔ " + StringUtils.translate("shulkerplanner.assistant.mark_done");
        boolean followed = session.isFollowed(target);
        String linkLabel = "+ " + StringUtils.translate("shulkerplanner.assistant.link", target.getIndex());
        boolean canLink = session.getBinding(target) == null;

        if (linked)
        {
            labels.add("✕ " + StringUtils.translate("shulkerplanner.assistant.unlink"));
            labels.add(doneLabel);
        }
        else
        {
            labels.add("◀");
            labels.add("▶");
            labels.add(canLink ? linkLabel : StringUtils.translate("shulkerplanner.assistant.already_linked"));
        }

        // Geometry: anchored to the container screen, size and position from the config
        int sw = screen.width;
        int sh = screen.height;
        int containerRight = anchorLeft + anchorWidth;
        int side = Configs.Generic.ASSISTANT_SIDE.getIntegerValue();
        int gap = Configs.Generic.ASSISTANT_GAP.getIntegerValue();

        if (side != SIDE_RIGHT && side != SIDE_LEFT)
        {
            int spaceRight = sw - containerRight - 6;
            int spaceLeft = anchorLeft - 6;
            side = spaceRight >= MIN_WIDTH || spaceRight >= spaceLeft ? SIDE_RIGHT : SIDE_LEFT;
            gap = 4;
        }

        int space = side == SIDE_RIGHT ? sw - containerRight - gap - 2 : anchorLeft - gap - 2;
        int w = Configs.Generic.ASSISTANT_WIDTH.getIntegerValue();

        if (w <= 0)
        {
            w = Math.max(MIN_WIDTH, Math.min(DEFAULT_WIDTH, space));
        }

        w = Math.max(MIN_WIDTH, Math.min(w, sw - 4));
        int inner = w - 2 * PAD;
        int footerLines = stackLines(labels, inner - GRIP);
        int hintHeight = hint ? 24 : 0;
        int fixed = TITLE_HEIGHT + 19 + 4 + 13 + hintHeight + footerLines * (BUTTON_HEIGHT + 3) + 4;
        int h = Configs.Generic.ASSISTANT_HEIGHT.getIntegerValue();

        if (h <= 0)
        {
            // Automatic: as tall as the container screen, shorter if there's little to show
            h = Math.min(fixed + Math.max(contentHeight, ROW_HEIGHT), Math.max(imageHeight, MIN_HEIGHT));
        }

        h = Math.max(Math.min(MIN_HEIGHT, sh - 4), Math.min(h, sh - 4));
        int x = side == SIDE_RIGHT ? containerRight + gap : anchorLeft - gap - w;
        int y = anchorTop + Configs.Generic.ASSISTANT_OFFSET_Y.getIntegerValue();

        if (drag == Drag.MOVE || drag == Drag.RESIZE)
        {
            // While dragging, the panel follows the mouse exactly
            x = panelX;
            y = panelY;

            if (drag == Drag.RESIZE)
            {
                w = panelW;
                h = panelH;
                inner = w - 2 * PAD;
                footerLines = stackLines(labels, inner - GRIP);
                fixed = TITLE_HEIGHT + 19 + 4 + 13 + hintHeight + footerLines * (BUTTON_HEIGHT + 3) + 4;
            }
        }

        x = Math.max(2, Math.min(x, sw - w - 2));
        y = Math.max(2, Math.min(y, sh - h - 2));
        panelX = x;
        panelY = y;
        panelW = w;
        panelH = h;
        panelVisible = true;

        // Background: translucent, so the world (and anything below) stays visible
        int alpha = Math.round(255 * Configs.Generic.ASSISTANT_OPACITY.getIntegerValue() / 100f);
        int accent = state == BoxState.DONE ? Theme.SUCCESS : (linked ? Theme.ACCENT : Theme.BORDER_STRONG);
        Ui.framed(ctx, x, y, w, h, Theme.withAlpha(0x140F1C, alpha), Theme.withAlpha(accent, Math.max(alpha, 0xC0)));
        Ui.gradient(ctx, x + 1, y + 1, w - 2, TITLE_HEIGHT - 1, Theme.withAlpha(accent, Math.min(255, alpha / 2 + 0x40)),
                    Theme.withAlpha(0x2B2238, Math.min(255, alpha + 0x20)));
        Ui.hline(ctx, x + 1, y + TITLE_HEIGHT, w - 2, Theme.withAlpha(accent, 0x90));

        int tx = x + PAD;
        int right = x + w - PAD;

        // Title bar: drag handle, shulker icon, box number, state chip, reset button
        boolean overTitle = Ui.inside(mouseX, mouseY, x, y, w, TITLE_HEIGHT) && drag == Drag.NONE;
        Ui.dragHandle(ctx, tx, y + 6, overTitle || drag == Drag.MOVE ? Theme.TEXT : Theme.TEXT_MUTED);
        ItemStack icon = project != null ? new ItemStack(project.getShulkerItem(target.getIndex())) : new ItemStack(Items.SHULKER_BOX);
        Ui.item(ctx, icon, tx + 8, y + 4, 12, false);

        // Small square buttons on the right of the title bar: follow on the HUD, put the panel back
        int small = 14;
        int resetX = right - small;
        int hudX = resetX - 3 - small;
        boolean hoverReset = Ui.inside(mouseX, mouseY, resetX, y + 3, small, small) && drag == Drag.NONE;
        boolean hoverHud = Ui.inside(mouseX, mouseY, hudX, y + 3, small, small) && drag == Drag.NONE;
        LayoutProbe.add(LayoutProbe.Kind.BUTTON, "reset", resetX, y + 3, small, small);
        Ui.button3d(ctx, resetX, y + 3, small, small, hoverReset ? Ui.BEVEL_QUIET.hovered() : Ui.BEVEL_QUIET, false);
        Ui.resetIcon(ctx, resetX + 4, y + 5, hoverReset ? Theme.TEXT : Theme.TEXT_DIM);
        buttons.add(new Button(resetX, y + 3, small, small, () -> {
            resetGeometry();
            scroll = 0;
        }));

        Ui.Bevel hudBevel = followed ? Ui.BEVEL_ACTIVE : Ui.BEVEL_QUIET;
        LayoutProbe.add(LayoutProbe.Kind.BUTTON, "hud", hudX, y + 3, small, small);
        Ui.button3d(ctx, hudX, y + 3, small, small, hoverHud ? hudBevel.hovered() : hudBevel, followed);
        Ui.followIcon(ctx, hudX + 4, y + (followed ? 6 : 5), followed, followed ? Theme.ACCENT_HOVER : (hoverHud ? Theme.TEXT : Theme.TEXT_DIM));
        PlannedBox hudBox = target;
        buttons.add(new Button(hudX, y + 3, small, small, () -> session.setFollowedBox(followed ? null : hudBox)));

        int titleX = tx + 24;
        String title = StringUtils.translate("shulkerplanner.assistant.box", target.getIndex(), plan.getBoxCount());
        Ui.textShadow(ctx, Ui.trim(title, hudX - 4 - titleX), titleX, y + 7, Theme.TEXT);

        // State chip, progress bar and percentage on one row
        String chip;
        int chipFg;
        int chipBg;

        if (linked && state == BoxState.DONE)
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

        int py = y + TITLE_HEIGHT + 4;
        int chipW = linked && state != BoxState.DONE ? Ui.lockChip(ctx, chip, tx, py, chipFg, chipBg)
                                                     : Ui.chip(ctx, chip, tx, py, chipFg, chipBg);
        int ty = py + 1;

        // Progress bar
        long need = target.getItemCount();
        long inside = 0;

        for (Map.Entry<String, Integer> e : planned.entrySet())
        {
            inside += Math.min(e.getValue(), contents.getOrDefault(e.getKey(), 0));
        }

        String pct = need > 0 ? (int) (100 * inside / need) + "%" : "";
        int barX = tx + chipW + 5;
        Ui.progress(ctx, barX, ty + 2, right - Ui.width("100%") - 4 - barX, 5, need > 0 ? (double) inside / need : 0,
                    complete ? Theme.SUCCESS : Theme.ACCENT, Theme.withAlpha(Theme.SURFACE_3, 0xFF));
        Ui.textRight(ctx, pct, right, ty + 1, Theme.TEXT_DIM);
        ty += 14;

        // The scrolled list of items
        int bottomFixed = 13 + hintHeight + footerLines * (BUTTON_HEIGHT + 3) + 4;
        listX = tx;
        listY = ty;
        listH = Math.max(ROW_HEIGHT / 2, y + h - bottomFixed - ty - 2);
        boolean scrollable = contentHeight > listH;
        listW = inner - (scrollable ? SCROLLBAR_WIDTH + 3 : 0);
        scroll = Math.max(0, Math.min(scroll, getMaxScroll()));

        Ui.rect(ctx, listX - 2, listY - 1, inner + 4, listH + 2, Theme.withAlpha(0x000000, Math.min(0x60, alpha / 3)));
        Ui.pushClip(ctx, listX - 2, listY, inner + 4, listH);
        String hoveredId = null;
        int hoveredWant = 0;
        int hoveredHave = 0;
        int ry = listY - scroll;

        for (Row row : rows)
        {
            if (ry + row.height() > listY && ry < listY + listH)
            {
                boolean hoverRow = row.header() == false && Ui.inside(mouseX, mouseY, listX, Math.max(ry, listY), listW,
                                                                      Math.min(ry + row.height(), listY + listH) - Math.max(ry, listY));
                if (hoverRow && drag == Drag.NONE)
                {
                    Ui.rect(ctx, listX - 2, ry, listW + 2, row.height(), Theme.withAlpha(0xFFFFFF, 0x14));
                    hoveredId = row.id();
                    hoveredWant = row.want();
                    hoveredHave = row.have();

                    if (row.id() != null)
                    {
                        ItemRequirement req = target.getItems().get(row.id());
                        ItemStack stack = req != null ? req.proto() : new MaterialRow(row.id(), 0, 0).getStack();
                        int clipTop = Math.max(ry, listY);
                        int clipBottom = Math.min(ry + row.height(), listY + listH);
                        hoveredItem = new HoveredItem(stack, listX, clipTop, listW, Math.max(1, clipBottom - clipTop));
                    }
                }

                drawRow(ctx, screen.getMenu(), row, target, ry);
            }

            ry += row.height();
        }

        Ui.popClip(ctx);

        if (scrollable)
        {
            int sbx = listX + inner - SCROLLBAR_WIDTH;
            boolean hoverBar = Ui.inside(mouseX, mouseY, sbx, listY, SCROLLBAR_WIDTH, listH) || drag == Drag.SCROLLBAR;
            Ui.scrollbar(ctx, sbx, listY, SCROLLBAR_WIDTH, listH, scroll, getMaxScroll(), listH, hoverBar);
        }

        ty = listY + listH + 4;

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
        else if (showWrong)
        {
            status = StringUtils.translate("shulkerplanner.assistant.wrong_items");
            statusColor = Theme.DANGER;
        }
        else
        {
            status = StringUtils.translate("shulkerplanner.assistant.missing", Format.number(missing));
            statusColor = Theme.WARN;
        }

        Ui.textShadow(ctx, Ui.trim(status, inner), tx, ty, statusColor);
        ty += 13;

        if (hint)
        {
            Ui.panel(ctx, tx - 2, ty - 2, inner + 4, 22, Theme.withAlpha(Theme.INFO, 0x38));
            Ui.text(ctx, Ui.trim(StringUtils.translate("shulkerplanner.assistant.hint_unlinked1"), inner - 2), tx + 1, ty, Theme.INFO);
            Ui.text(ctx, Ui.trim(StringUtils.translate("shulkerplanner.assistant.hint_unlinked2"), inner - 2), tx + 1, ty + 10, Theme.TEXT);
            ty += hintHeight;
        }

        // Actions, wrapping to the next line when they don't fit, never under the resize grip
        int bx = tx;
        int by = ty;
        int flowRight = right - GRIP;
        List<Runnable> actions = new ArrayList<>();
        List<PlannedBox> unlinked = unlinkedBoxes(plan, session);
        int index = unlinked.indexOf(target);
        int count = unlinked.size();
        PlannedBox shown = target;
        BoxState shownState = state;

        if (linked)
        {
            actions.add(() -> { session.unlink(shown); binding = null; previewId = shown.getId(); });
            actions.add(() -> session.setBoxState(shown, shownState == BoxState.DONE ? BoxState.FILLING : BoxState.DONE));
        }
        else
        {
            actions.add(count > 1 ? () -> previewId = unlinked.get(Math.floorMod(index - 1, count)).getId() : null);
            actions.add(count > 1 ? () -> previewId = unlinked.get(Math.floorMod(index + 1, count)).getId() : null);
            actions.add(canLink ? () -> link(shown, contents) : null);
        }

        for (int i = 0; i < labels.size(); ++i)
        {
            String label = labels.get(i);
            Runnable action = actions.get(i);
            int bw = Ui.width(label) + 10;

            if (bx > tx && bx + bw > flowRight)
            {
                bx = tx;
                by += BUTTON_HEIGHT + 3;
            }

            bw = Math.min(bw, flowRight - bx);

            if (action == null)
            {
                Ui.text(ctx, Ui.trim(label, flowRight - bx), bx, by + 4, Theme.TEXT_MUTED);
                continue;
            }

            boolean primary = linked == false && i == 2;
            boolean hover = Ui.inside(mouseX, mouseY, bx, by, bw, BUTTON_HEIGHT) && drag == Drag.NONE;
            Ui.Bevel bevel = primary ? Ui.BEVEL_PRIMARY : Ui.BEVEL_SECONDARY;
            Ui.button3dLabel(ctx, bx, by, bw, BUTTON_HEIGHT, Ui.trim(label, bw - 6), hover ? bevel.hovered() : bevel, false,
                             primary ? 0xFFFFFFFF : Theme.TEXT);
            buttons.add(new Button(bx, by, bw, BUTTON_HEIGHT, action));
            bx += bw + 3;
        }

        // Resize grip in the bottom right corner
        boolean overGrip = Ui.inside(mouseX, mouseY, x + w - GRIP - 1, y + h - GRIP - 1, GRIP + 1, GRIP + 1) || drag == Drag.RESIZE;
        Ui.resizeGrip(ctx, x + w - 2, y + h - 2, overGrip ? Theme.TEXT : Theme.withAlpha(accent, 0xC0));
        LayoutProbe.add(LayoutProbe.Kind.BUTTON, "resize", x + w - GRIP - 1, y + h - GRIP - 1, GRIP, GRIP);

        // Tooltips last, above everything
        if (drag == Drag.NONE)
        {
            if (hoveredId != null)
            {
                drawRowTooltip(ctx, screen.getMenu(), mouseX, mouseY, target, hoveredId, hoveredWant, hoveredHave);
            }
            else if (hoverReset)
            {
                Ui.tooltip(ctx, mouseX, mouseY, List.of(StringUtils.translate("shulkerplanner.assistant.hover.reset")));
            }
            else if (hoverHud)
            {
                Ui.tooltip(ctx, mouseX, mouseY, List.of(StringUtils.translate(followed ? "shulkerplanner.assistant.hover.hud_on"
                                                                                       : "shulkerplanner.assistant.hover.hud_off")));
            }
            else if (overTitle)
            {
                Ui.tooltip(ctx, mouseX, mouseY, List.of(StringUtils.translate("shulkerplanner.assistant.hover.move")));
            }
            else if (overGrip)
            {
                Ui.tooltip(ctx, mouseX, mouseY, List.of(StringUtils.translate("shulkerplanner.assistant.hover.resize")));
            }
        }
    }

    private static void drawRow(GuiContext ctx, AbstractContainerMenu menu, Row row, PlannedBox target, int y)
    {
        int x = listX;
        int w = listW;

        if (row.header())
        {
            Ui.text(ctx, "⚠ " + StringUtils.translate("shulkerplanner.assistant.remove"), x, y + 2, Theme.DANGER);
            return;
        }

        if (row.wrong())
        {
            ItemStack stack = new MaterialRow(row.id(), 0, 0).getStack();
            Ui.item(ctx, stack, x, y + 2);
            String text = Format.number(row.have()) + "  " + stack.getHoverName().getString();
            Ui.text(ctx, Ui.trim(text, w - 20), x + 19, y + 6, Theme.DANGER);
            return;
        }

        int want = row.want();
        int have = row.have();
        ItemRequirement req = target.getItems().get(row.id());
        Ui.item(ctx, req.proto(), x, y + 2);

        int color = have == want ? Theme.SUCCESS : (have > want ? Theme.WARN : (have > 0 ? Theme.TEXT : Theme.TEXT_DIM));
        String mark = have == want ? "✔ " : (have > want ? "▲ " : "");
        String count = mark + Format.number(have) + "/" + Format.number(want);
        int cw = Ui.width(count);

        if (cw > w - 20)
        {
            count = Ui.trim(count, w - 20);
            cw = Ui.width(count);
        }

        Ui.text(ctx, count, x + 19, y + 2, color);
        String name = Ui.trim(req.proto().getHoverName().getString(), w - 23 - cw);

        if (name.isEmpty() == false)
        {
            Ui.text(ctx, name, x + 23 + cw, y + 2, Theme.TEXT_DIM);
        }

        String sub = null;
        int subColor = Theme.TEXT_MUTED;

        if (have < want)
        {
            int inInv = countInPlayerSlots(menu, row.id());
            sub = StringUtils.translate("shulkerplanner.assistant.to_put_in", Format.stacks(want - have, req.maxStack()))
                  + "  " + StringUtils.translate("shulkerplanner.assistant.in_inventory", Format.number(inInv));
            subColor = inInv >= want - have ? Theme.INFO : Theme.TEXT_MUTED;
        }
        else if (have > want)
        {
            sub = StringUtils.translate("shulkerplanner.assistant.too_many", Format.number(have - want));
            subColor = Theme.WARN;
        }

        if (sub != null)
        {
            Ui.text(ctx, Ui.trim(sub, w - 19), x + 19, y + 11, subColor);
        }
    }

    private static void drawRowTooltip(GuiContext ctx, AbstractContainerMenu menu, int mouseX, int mouseY,
                                       PlannedBox target, String id, int want, int have)
    {
        ItemRequirement req = target.getItems().get(id);
        ItemStack stack = req != null ? req.proto() : new MaterialRow(id, 0, 0).getStack();
        int maxStack = stack.getMaxStackSize();
        List<String> lines = new ArrayList<>();
        lines.add(GuiBase.TXT_BOLD + stack.getHoverName().getString());

        if (req == null)
        {
            lines.add(GuiBase.TXT_RED + StringUtils.translate("shulkerplanner.assistant.hover.wrong", Format.number(have)));
        }
        else
        {
            lines.add(StringUtils.translate("shulkerplanner.assistant.hover.in_box", Format.number(have), Format.number(want), Format.stacks(want, maxStack)));

            if (have < want)
            {
                lines.add(GuiBase.TXT_AQUA + StringUtils.translate("shulkerplanner.assistant.to_put_in", Format.stacks(want - have, maxStack)));
            }

            lines.add(GuiBase.TXT_GRAY + StringUtils.translate("shulkerplanner.assistant.in_inventory", Format.number(countInPlayerSlots(menu, id))));
        }

        if (JeiCompat.isAvailable())
        {
            lines.add(GuiBase.TXT_DARK_GRAY + StringUtils.translate("shulkerplanner.assistant.hover.jei"));
        }

        Ui.tooltip(ctx, mouseX, mouseY, lines);
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

    // ---------------------------------------------------------------- input

    private static boolean isOurs(AbstractContainerScreen<?> screen)
    {
        return screen == lastScreen && panelVisible && isActive(screen);
    }

    private static void playClick()
    {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    /**
     * @return true if the click was on the panel. Every click on the panel is used by it,
     * so nothing behind it (slots, or "clicked outside" dropping the carried item) gets it.
     */
    public static boolean onMouseClicked(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button)
    {
        if (drag != Drag.NONE)
        {
            // The release of the last drag was lost (outside the window): end it now
            onMouseReleased(screen);
        }

        pressedInPanel = false;

        if (isOurs(screen) == false || isMouseOverPanel(mouseX, mouseY) == false)
        {
            return false;
        }

        pressedInPanel = true;

        // Minecraft 26.3 numbers the mouse buttons like SDL: left is 1 (InputConstants.MOUSE_BUTTON_LEFT), not GLFW's 0
        if (button == InputConstants.MOUSE_BUTTON_RIGHT)
        {
            HoveredItem item = getHoveredItem(mouseX, mouseY);

            if (item != null && JeiCompat.isAvailable() && JeiCompat.showUses(item.stack()))
            {
                playClick();
            }

            return true;
        }

        if (button != InputConstants.MOUSE_BUTTON_LEFT)
        {
            return true;
        }

        for (Button b : List.copyOf(buttons))
        {
            if (b.contains(mouseX, mouseY))
            {
                b.action().run();
                playClick();
                return true;
            }
        }

        dragStartX = mouseX;
        dragStartY = mouseY;
        dragStartPanelX = panelX;
        dragStartPanelY = panelY;
        dragStartW = panelW;
        dragStartH = panelH;
        dragStartScroll = scroll;

        if (Ui.inside(mouseX, mouseY, panelX + panelW - GRIP - 1, panelY + panelH - GRIP - 1, GRIP + 1, GRIP + 1))
        {
            drag = Drag.RESIZE;
        }
        else if (Ui.inside(mouseX, mouseY, panelX, panelY, panelW, TITLE_HEIGHT))
        {
            drag = Drag.MOVE;
        }
        else if (getMaxScroll() > 0 && Ui.inside(mouseX, mouseY, listX + listW, listY, panelX + panelW - PAD - (listX + listW) + 1, listH))
        {
            // Click on the scroll bar track: jump there, then drag
            int thumb = Math.max(10, listH * listH / (contentHeight));
            double f = (mouseY - listY - thumb / 2.0) / Math.max(1, listH - thumb);
            scroll = (int) Math.round(Math.max(0, Math.min(1, f)) * getMaxScroll());
            dragStartScroll = scroll;
            drag = Drag.SCROLLBAR;
        }
        else
        {
            HoveredItem item = getHoveredItem(mouseX, mouseY);

            if (item != null && JeiCompat.isAvailable() && JeiCompat.showRecipes(item.stack()))
            {
                playClick();
            }
        }

        return true;
    }

    public static boolean onMouseDragged(AbstractContainerScreen<?> screen, double mouseX, double mouseY)
    {
        if (isOurs(screen) == false || drag == Drag.NONE)
        {
            return pressedInPanel && isOurs(screen);
        }

        int dx = (int) Math.round(mouseX - dragStartX);
        int dy = (int) Math.round(mouseY - dragStartY);

        switch (drag)
        {
            case MOVE -> {
                panelX = Math.max(2, Math.min(dragStartPanelX + dx, screen.width - panelW - 2));
                panelY = Math.max(2, Math.min(dragStartPanelY + dy, screen.height - panelH - 2));
                storeAnchor(panelX, panelY, panelW);
            }
            case RESIZE -> {
                panelW = Math.max(MIN_WIDTH, Math.min(dragStartW + dx, screen.width - panelX - 2));
                panelH = Math.max(Math.min(MIN_HEIGHT, screen.height - 4), Math.min(dragStartH + dy, screen.height - panelY - 2));
                Configs.Generic.ASSISTANT_WIDTH.setIntegerValue(panelW);
                Configs.Generic.ASSISTANT_HEIGHT.setIntegerValue(panelH);
                storeAnchor(panelX, panelY, panelW);
            }
            case SCROLLBAR -> {
                int thumb = Math.max(10, listH * listH / Math.max(1, contentHeight));
                int track = Math.max(1, listH - thumb);
                scroll = Math.max(0, Math.min(getMaxScroll(), dragStartScroll + (int) Math.round((mouseY - dragStartY) * getMaxScroll() / track)));
            }
            default -> {
            }
        }

        return true;
    }

    public static boolean onMouseReleased(AbstractContainerScreen<?> screen)
    {
        boolean used = pressedInPanel && isOurs(screen);

        if (drag == Drag.MOVE || drag == Drag.RESIZE)
        {
            Configs.saveToFile();
            used = true;
        }

        drag = Drag.NONE;
        pressedInPanel = false;
        return used;
    }

    public static boolean onMouseScrolled(AbstractContainerScreen<?> screen, double mouseX, double mouseY, double amount)
    {
        if (isOurs(screen) == false || isMouseOverPanel(mouseX, mouseY) == false)
        {
            return false;
        }

        if (amount != 0)
        {
            scroll = Math.max(0, Math.min(getMaxScroll(), scroll - (int) Math.round(amount * ROW_HEIGHT)));
        }

        return true;
    }
}
