package it.marco.shulkerplanner.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.plan.ItemRequirement;
import it.marco.shulkerplanner.plan.PlannedBox;
import it.marco.shulkerplanner.plan.ShulkerPlan;
import it.marco.shulkerplanner.util.Format;
import it.marco.shulkerplanner.util.InventoryScanner;

/**
 * Shows the planned contents of a shulker box next to an opened shulker box screen,
 * compares them to what is actually inside, and highlights the slots to fix / items to move.
 */
public class PackingAssistant
{
    private static final int PANEL_MAX_WIDTH = 160;
    private static final int PANEL_MIN_WIDTH = 110;
    private static int panelWidth = PANEL_MAX_WIDTH;
    private static final int LINE_HEIGHT = 18;
    private static final int MAX_ITEM_LINES = 9;

    @Nullable private static Object lastScreen;
    private static int manualIndex = -1;
    /** The box (index in the plan) that was completed and auto-marked while this screen was open */
    private static int completedIndex = -1;
    /** The contents of the box when the screen was opened. Already complete boxes don't get auto-marked again. */
    @Nullable private static String initialSignature;
    private static int prevX, prevY, nextX, nextY, autoX, autoY, autoW;
    private static boolean buttonsValid;

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

    private static PlannedBox pickTarget(List<PlannedBox> boxes, Map<String, Integer> contents)
    {
        PlannerSession session = PlannerSession.INSTANCE;
        PlannerProject project = session.getProject();

        if (manualIndex >= 0 && manualIndex < boxes.size())
        {
            return boxes.get(manualIndex);
        }

        manualIndex = -1;

        // Keep showing the box that was just completed, instead of jumping to an identical box that isn't done yet
        if (completedIndex >= 0 && completedIndex < boxes.size() &&
            boxes.get(completedIndex).getSignature().equals(PlannedBox.createSignature(contents)))
        {
            return boxes.get(completedIndex);
        }

        completedIndex = -1;

        if (contents.isEmpty())
        {
            // An empty box: suggest the first box that hasn't been filled yet
            for (PlannedBox box : boxes)
            {
                if (session.isBoxReady(box) == false)
                {
                    return box;
                }
            }

            return boxes.getFirst();
        }

        String signature = PlannedBox.createSignature(contents);
        PlannedBox exact = null;

        for (PlannedBox box : boxes)
        {
            if (box.getSignature().equals(signature))
            {
                if (project != null && project.isBoxDone(box) == false)
                {
                    return box;
                }

                if (exact == null)
                {
                    exact = box;
                }
            }
        }

        if (exact != null)
        {
            return exact;
        }

        // The box with the most matching items, preferring boxes that are not done yet
        PlannedBox best = null;
        long bestScore = Long.MIN_VALUE;

        for (PlannedBox box : boxes)
        {
            long score = 0;

            for (Map.Entry<String, Integer> e : contents.entrySet())
            {
                int planned = box.getTotals().getOrDefault(e.getKey(), 0);
                score += planned > 0 ? Math.min(planned, e.getValue()) * 4L : -e.getValue();
            }

            if (project != null && project.isBoxDone(box))
            {
                score -= 1_000_000L;
            }

            if (score > bestScore)
            {
                bestScore = score;
                best = box;
            }
        }

        return best != null ? best : boxes.getFirst();
    }

    /**
     * Called at the end of AbstractContainerScreen#extractContents, after the slots have been drawn
     */
    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                              int leftPos, int topPos, int imageWidth, int imageHeight)
    {
        if (screen != lastScreen)
        {
            lastScreen = screen;
            manualIndex = -1;
            completedIndex = -1;
            initialSignature = null;
        }

        buttonsValid = false;

        if (isActive(screen) == false)
        {
            return;
        }

        PlannerSession session = PlannerSession.INSTANCE;
        PlannerProject project = session.getProject();
        ShulkerPlan plan = session.getPlan();
        List<PlannedBox> boxes = plan.getBoxes();
        AbstractContainerMenu menu = screen.getMenu();
        Map<String, Integer> contents = getContainerContents(menu);
        String signature = PlannedBox.createSignature(contents);

        // The container contents arrive from the server a moment after the screen opens
        if (initialSignature == null && menu.getStateId() > 0)
        {
            initialSignature = signature;
        }

        PlannedBox target = pickTarget(boxes, contents);

        // The HUD follows the box that is being filled
        session.setFocusedBox(target.getIndex() - 1);
        session.setLiveBox(target.getIndex() - 1, contents);
        Map<String, Integer> planned = target.getTotals();
        boolean complete = contents.equals(new HashMap<>(planned));
        Font font = Minecraft.getInstance().font;

        if (complete && completedIndex < 0 && manualIndex < 0 && project != null &&
            initialSignature != null && signature.equals(initialSignature) == false &&
            Configs.Generic.AUTO_MARK_BOXES.getBooleanValue())
        {
            completedIndex = target.getIndex() - 1;
            session.markBoxDone(target);
        }

        if (Configs.Generic.PACKING_HIGHLIGHT_SLOTS.getBooleanValue())
        {
            renderSlotHighlights(menu, graphics, leftPos, topPos, contents, planned);
        }

        // Lines: items of the plan, then the wrong items
        List<String> wrong = new ArrayList<>();

        for (String id : contents.keySet())
        {
            if (planned.containsKey(id) == false)
            {
                wrong.add(id);
            }
        }

        int itemLines = Math.min(planned.size(), MAX_ITEM_LINES);
        int wrongLines = Math.min(wrong.size(), 4);
        int height = 8 + 12 + 14 + itemLines * LINE_HEIGHT + (planned.size() > itemLines ? 10 : 0)
                     + (wrongLines > 0 ? 12 + wrongLines * LINE_HEIGHT : 0) + 14;

        // Use the side with more free space, shrinking the panel on small screens
        int spaceRight = screen.width - (leftPos + imageWidth) - 6;
        int spaceLeft = leftPos - 6;
        boolean right = spaceRight >= PANEL_MAX_WIDTH || spaceRight >= spaceLeft;
        panelWidth = Math.max(PANEL_MIN_WIDTH, Math.min(PANEL_MAX_WIDTH, right ? spaceRight : spaceLeft));
        int x = right ? leftPos + imageWidth + 4 : leftPos - panelWidth - 4;
        x = Math.max(2, Math.min(x, screen.width - panelWidth - 2));

        int y = Math.max(2, topPos);

        graphics.fill(x, y, x + panelWidth, y + height, 0xE0101018);
        graphics.outline(x, y, panelWidth, height, complete ? 0xFF40D040 : 0xFFA060E0);

        int tx = x + 5;
        int ty = y + 5;

        graphics.text(font, ChatFormatting.LIGHT_PURPLE + "Shulker Planner", tx, ty, 0xFFFFFFFF, true);
        ty += 12;

        // Box selector: < #3 / 12 >
        prevX = tx;
        prevY = ty;
        drawButton(graphics, font, prevX, prevY, "<", mouseX, mouseY);

        String boxLabel = "#" + target.getIndex() + " / " + boxes.size();
        int labelX = tx + 14;
        graphics.text(font, ChatFormatting.BOLD + boxLabel, labelX, ty + 2, 0xFFFFFFFF, false);

        nextX = labelX + font.width(ChatFormatting.BOLD + boxLabel) + 4;
        nextY = ty;
        drawButton(graphics, font, nextX, nextY, ">", mouseX, mouseY);

        String mode = manualIndex >= 0 ? StringUtils.translate("shulkerplanner.assistant.manual")
                                       : StringUtils.translate("shulkerplanner.assistant.auto");
        autoX = nextX + 16;
        autoY = ty + 2;
        autoW = font.width(mode);
        graphics.text(font, (manualIndex >= 0 ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY) + mode, autoX, autoY, 0xFFFFFFFF, false);
        buttonsValid = true;
        ty += 14;

        int missingItems = 0;
        int line = 0;

        for (Map.Entry<String, Integer> e : planned.entrySet())
        {
            int want = e.getValue();
            int have = contents.getOrDefault(e.getKey(), 0);

            if (have < want)
            {
                missingItems += want - have;
            }

            if (line++ >= MAX_ITEM_LINES)
            {
                continue;
            }

            ItemRequirement req = target.getItems().get(e.getKey());
            graphics.item(req.proto(), tx, ty);

            ChatFormatting color = have == want ? ChatFormatting.GREEN : (have > want ? ChatFormatting.GOLD : (have > 0 ? ChatFormatting.YELLOW : ChatFormatting.RED));
            String count = color + Format.number(have) + ChatFormatting.GRAY + "/" + Format.number(want);
            graphics.text(font, count, tx + 19, ty + 1, 0xFFFFFFFF, false);

            int countW = font.width(count);
            String name = Format.trim(req.proto().getHoverName().getString(), panelWidth - 32 - countW);
            graphics.text(font, ChatFormatting.GRAY + name, tx + 23 + countW, ty + 1, 0xFFFFFFFF, false);

            if (have < want)
            {
                int needed = want - have;
                String inv = StringUtils.translate("shulkerplanner.assistant.in_inventory", Format.number(countInPlayerSlots(menu, e.getKey())));
                graphics.text(font, ChatFormatting.DARK_GRAY + Format.stacks(needed, req.maxStack()) + "  " + inv, tx + 19, ty + 10, 0xFFFFFFFF, false);
            }

            ty += LINE_HEIGHT;
        }

        if (planned.size() > itemLines)
        {
            graphics.text(font, ChatFormatting.GRAY + StringUtils.translate("shulkerplanner.gui.label.box.more", planned.size() - itemLines), tx, ty, 0xFFFFFFFF, false);
            ty += 10;
        }

        if (wrongLines > 0)
        {
            graphics.text(font, ChatFormatting.RED + StringUtils.translate("shulkerplanner.assistant.remove"), tx, ty + 2, 0xFFFFFFFF, false);
            ty += 12;

            for (int i = 0; i < wrongLines; ++i)
            {
                String id = wrong.get(i);
                ItemStack stack = new MaterialRow(id, 0, 0).getStack();
                graphics.item(stack, tx, ty);
                graphics.text(font, ChatFormatting.RED + Format.number(contents.get(id)) + " " + ChatFormatting.GRAY
                                    + Format.trim(stack.getHoverName().getString(), panelWidth - 60), tx + 19, ty + 4, 0xFFFFFFFF, false);
                ty += LINE_HEIGHT;
            }
        }

        String status;

        if (complete)
        {
            status = ChatFormatting.GREEN + StringUtils.translate("shulkerplanner.assistant.complete");
        }
        else if (wrong.isEmpty() == false)
        {
            status = ChatFormatting.RED + StringUtils.translate("shulkerplanner.assistant.wrong_items");
        }
        else
        {
            status = ChatFormatting.GOLD + StringUtils.translate("shulkerplanner.assistant.missing", Format.number(missingItems));
        }

        graphics.text(font, status, tx, ty + 3, 0xFFFFFFFF, false);
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

    private static void renderSlotHighlights(AbstractContainerMenu menu, GuiGraphicsExtractor graphics, int leftPos, int topPos,
                                             Map<String, Integer> contents, Map<String, Integer> planned)
    {
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
                // Inside the shulker box: wrong item, or too many of it
                if (want == 0)
                {
                    color = 0x70FF2020;
                }
                else if (have > want)
                {
                    color = 0x70FFA000;
                }
            }
            else if (want > have)
            {
                // Player inventory: an item that should still go into the box
                color = 0x6020E020;
            }

            if (color != 0)
            {
                int sx = leftPos + slot.x;
                int sy = topPos + slot.y;
                graphics.fill(sx, sy, sx + 16, sy + 16, color);
            }
        }
    }

    private static void drawButton(GuiGraphicsExtractor graphics, Font font, int x, int y, String label, int mouseX, int mouseY)
    {
        boolean hover = mouseX >= x && mouseX < x + 12 && mouseY >= y && mouseY < y + 12;
        graphics.fill(x, y, x + 12, y + 12, hover ? 0xFF606080 : 0xFF303040);
        graphics.outline(x, y, 12, 12, 0xFF8080A0);
        graphics.text(font, label, x + 6 - font.width(label) / 2, y + 2, 0xFFFFFFFF, false);
    }

    /**
     * @return true if the click was handled by the assistant panel
     */
    public static boolean onMouseClicked(AbstractContainerScreen<?> screen, double mouseX, double mouseY)
    {
        if (buttonsValid == false || screen != lastScreen || isActive(screen) == false)
        {
            return false;
        }

        List<PlannedBox> boxes = PlannerSession.INSTANCE.getPlan().getBoxes();
        int count = boxes.size();

        if (count == 0)
        {
            return false;
        }

        int current = manualIndex;

        if (current < 0)
        {
            PlannedBox target = pickTarget(boxes, getContainerContents(screen.getMenu()));
            current = target.getIndex() - 1;
        }

        if (isInside(mouseX, mouseY, prevX, prevY, 12, 12))
        {
            manualIndex = Math.floorMod(current - 1, count);
            return true;
        }
        else if (isInside(mouseX, mouseY, nextX, nextY, 12, 12))
        {
            manualIndex = Math.floorMod(current + 1, count);
            return true;
        }
        else if (isInside(mouseX, mouseY, autoX, autoY - 2, autoW, 12))
        {
            manualIndex = -1;
            return true;
        }

        return false;
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h)
    {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
