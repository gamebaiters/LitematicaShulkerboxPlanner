package it.marco.shulkerplanner.data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.interfaces.ICompletionListener;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.mixin.IMixinMaterialListBase;
import it.marco.shulkerplanner.plan.CountMode;
import it.marco.shulkerplanner.plan.PlannedBox;
import it.marco.shulkerplanner.plan.ShulkerPacker;
import it.marco.shulkerplanner.plan.ShulkerPlan;
import it.marco.shulkerplanner.util.InventoryScanner;

/**
 * The currently active project, the Litematica material list it is linked to (if any)
 * and the shulker box plan calculated from it.
 */
public class PlannerSession implements IClientTickHandler
{
    public static final PlannerSession INSTANCE = new PlannerSession();

    @Nullable private PlannerProject project;
    @Nullable private MaterialListBase materialList;
    @Nullable private ShulkerPlan plan;
    @Nullable private Runnable updateListener;
    private Map<String, Integer> available = new HashMap<>();
    private Map<String, Integer> loose = new HashMap<>();
    /** The box (index in the plan) the player chose to follow on the HUD, or -1 for automatic */
    private int focusedBox = -1;
    private Map<String, Integer> inventoryBoxes = new HashMap<>();
    private boolean waitingForMaterialList;
    private int tickCounter;

    private PlannerSession()
    {
    }

    @Nullable
    public PlannerProject getProject()
    {
        return this.project;
    }

    @Nullable
    public MaterialListBase getMaterialList()
    {
        return this.materialList;
    }

    public boolean isWaitingForMaterialList()
    {
        return this.waitingForMaterialList;
    }

    /**
     * Sets a callback that gets called when the plan changes outside of the GUI
     * (material list task finished, inventory contents changed...)
     */
    public void setUpdateListener(@Nullable Runnable listener)
    {
        this.updateListener = listener;
    }

    /**
     * Opens (or switches to) the project of the given Litematica material list,
     * and imports the current material list contents into it.
     */
    public void openForMaterialList(MaterialListBase list)
    {
        this.switchProject(list.getName());
        this.linkMaterialList(list);

        // An empty list is most likely a placement list whose counting task hasn't finished yet
        this.waitingForMaterialList = list.getMaterialsAll().isEmpty();

        if (this.waitingForMaterialList == false)
        {
            this.importMaterials();
        }

        this.refreshInventory(false);
        this.invalidate();
    }

    /**
     * Opens a saved project without a linked material list
     */
    public void openProject(String name)
    {
        this.switchProject(name);
        this.materialList = null;
        this.waitingForMaterialList = false;
        this.refreshInventory(false);
        this.invalidate();
    }

    private void switchProject(String name)
    {
        if (this.project == null || this.project.getName().equals(name) == false)
        {
            this.save();
            this.project = ProjectStorage.load(name);
            this.materialList = null;
            this.focusedBox = -1;
        }

        Configs.Generic.LAST_PROJECT.setValueFromString(name);
    }

    private void linkMaterialList(MaterialListBase list)
    {
        this.materialList = list;

        ICompletionListener old = ((IMixinMaterialListBase) list).shulkerplanner_getCompletionListener();

        if ((old instanceof ChainedListener) == false)
        {
            list.setCompletionListener(new ChainedListener(list, old));
        }
    }

    /**
     * Re-creates the linked Litematica material list. For placements that runs as a task,
     * and the result gets imported once the task finishes.
     */
    public boolean requestMaterialListRefresh()
    {
        if (this.materialList == null)
        {
            return false;
        }

        List<MaterialListEntry> before = this.materialList.getMaterialsAll();
        this.waitingForMaterialList = true;
        this.materialList.reCreateMaterialList();

        // Schematic based lists are re-created immediately, placement based lists run a task
        if (this.materialList.getMaterialsAll() != before)
        {
            this.onMaterialListCompleted(this.materialList);
        }

        return true;
    }

    private void onMaterialListCompleted(MaterialListBase list)
    {
        if (list == this.materialList && this.project != null)
        {
            this.waitingForMaterialList = false;
            this.importMaterials();
            this.refreshInventory(false);
            this.invalidate();
            this.notifyUpdate();
        }
    }

    private void importMaterials()
    {
        if (this.project == null || this.materialList == null)
        {
            return;
        }

        Map<String, int[]> counts = new LinkedHashMap<>();

        for (MaterialListEntry entry : this.materialList.getMaterialsAll())
        {
            ItemStack stack = entry.getStack();

            if (stack.isEmpty())
            {
                continue;
            }

            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            int[] arr = counts.computeIfAbsent(id, k -> new int[2]);
            arr[0] += entry.getCountTotal();
            arr[1] += entry.getCountMissing();
        }

        List<MaterialRow> rows = counts.entrySet().stream()
                .map(e -> new MaterialRow(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        this.project.setMaterials(rows, this.materialList.getTitle());

        if (this.materialList.getMultiplier() > 1 && this.project.getMultiplier() == 1)
        {
            this.project.setMultiplier(this.materialList.getMultiplier());
        }

        this.applyAvailableCounts();
        this.save();
    }

    public void invalidate()
    {
        this.plan = null;
    }

    /**
     * Call after changing anything in the project: re-calculates the plan and saves the project.
     */
    public void onProjectChanged()
    {
        this.invalidate();
        this.save();
    }

    public ShulkerPlan getPlan()
    {
        if (this.plan == null)
        {
            if (this.project == null)
            {
                return ShulkerPlan.EMPTY;
            }

            this.plan = ShulkerPacker.pack(this.project.buildRequirements(), this.project.getPackMode(),
                                           this.project.getRoundUpStacks(), this.project.getFillSpareSlots());
        }

        return this.plan;
    }

    public void save()
    {
        if (this.project != null)
        {
            ProjectStorage.save(this.project);
        }
    }

    // ---------------------------------------------------------------- inventory

    public int getAvailable(String id)
    {
        return this.available.getOrDefault(id, 0);
    }

    /**
     * @return the amount of the item that is loose in the player's inventory (not inside shulker boxes or bundles)
     */
    public int getLoose(String id)
    {
        return this.loose.getOrDefault(id, 0);
    }

    public void setFocusedBox(int index)
    {
        this.focusedBox = index;
    }

    public boolean isFocused(PlannedBox box)
    {
        return this.focusedBox == box.getIndex() - 1;
    }

    /**
     * @return the box being worked on: the one chosen by the player (or by the packing assistant)
     * while it's not ready yet, otherwise the first box that is not ready yet. Null when everything is ready.
     */
    @Nullable
    public PlannedBox getCurrentBox()
    {
        List<PlannedBox> boxes = this.getPlan().getBoxes();

        if (this.focusedBox >= 0 && this.focusedBox < boxes.size() && this.isBoxReady(boxes.get(this.focusedBox)) == false)
        {
            return boxes.get(this.focusedBox);
        }

        for (PlannedBox box : boxes)
        {
            if (this.isBoxReady(box) == false)
            {
                return box;
            }
        }

        return null;
    }

    public boolean isBoxInInventory(PlannedBox box)
    {
        return box.getDuplicateOrdinal() < this.inventoryBoxes.getOrDefault(box.getSignature(), 0);
    }

    public boolean isBoxReady(PlannedBox box)
    {
        return (this.project != null && this.project.isBoxDone(box)) || this.isBoxInInventory(box);
    }

    public int getReadyBoxCount()
    {
        int count = 0;

        for (PlannedBox box : this.getPlan().getBoxes())
        {
            if (this.isBoxReady(box))
            {
                ++count;
            }
        }

        return count;
    }

    private void applyAvailableCounts()
    {
        if (this.project != null)
        {
            for (MaterialRow row : this.project.getMaterials())
            {
                row.setAvailable(this.getAvailable(row.getId()));
            }
        }
    }

    /**
     * Re-scans the player inventory.
     * @return true if something changed that affects the plan or its state
     */
    public boolean refreshInventory(boolean notify)
    {
        Minecraft mc = Minecraft.getInstance();

        if (this.project == null || mc.player == null)
        {
            return false;
        }

        Map<String, Integer> newAvailable = InventoryScanner.countAvailableItems(mc.player);
        Map<String, Integer> newBoxes = InventoryScanner.getShulkerSignatures(mc.player);
        this.loose = InventoryScanner.countLooseItems(mc.player);
        boolean availableChanged = newAvailable.equals(this.available) == false;
        boolean boxesChanged = newBoxes.equals(this.inventoryBoxes) == false;

        this.available = newAvailable;
        this.inventoryBoxes = newBoxes;
        this.applyAvailableCounts();

        if (availableChanged && this.project.getCountMode() == CountMode.TO_GATHER)
        {
            this.invalidate();
        }

        boolean marked = Configs.Generic.AUTO_MARK_BOXES.getBooleanValue() && this.autoMarkBoxes();

        if (marked)
        {
            this.save();
        }

        boolean changed = availableChanged || boxesChanged || marked;

        if (changed && notify)
        {
            this.notifyUpdate();
        }

        return changed;
    }

    /**
     * Marks boxes as done when an identically filled shulker box is in the player's inventory
     */
    private boolean autoMarkBoxes()
    {
        if (this.project == null || this.inventoryBoxes.isEmpty())
        {
            return false;
        }

        Map<String, Integer> boxCounts = new HashMap<>();

        for (PlannedBox box : this.getPlan().getBoxes())
        {
            boxCounts.merge(box.getSignature(), 1, Integer::sum);
        }

        boolean changed = false;

        for (Map.Entry<String, Integer> entry : boxCounts.entrySet())
        {
            String signature = entry.getKey();
            int inInventory = Math.min(entry.getValue(), this.inventoryBoxes.getOrDefault(signature, 0));

            if (inInventory > this.project.getDoneCount(signature))
            {
                this.project.setDoneCount(signature, inInventory);
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Marks the box as done, if it is not done yet. Used by the packing assistant.
     */
    public void markBoxDone(PlannedBox box)
    {
        if (this.project != null && this.project.isBoxDone(box) == false)
        {
            this.project.setBoxDone(box, true);
            this.save();
            this.notifyUpdate();
        }
    }

    private void notifyUpdate()
    {
        if (this.updateListener != null)
        {
            this.updateListener.run();
        }
    }

    @Override
    public void onClientTick(Minecraft mc)
    {
        if (this.project == null || mc.player == null)
        {
            return;
        }

        if (++this.tickCounter >= 20)
        {
            this.tickCounter = 0;
            this.refreshInventory(true);
        }
        else if (this.tickCounter % 5 == 0)
        {
            // The HUD shows the loose items of the current box, keep those responsive
            this.loose = InventoryScanner.countLooseItems(mc.player);
        }
    }

    public void onWorldChanged(boolean joined)
    {
        this.save();

        // Litematica clears its material lists when leaving a world
        this.materialList = null;
        this.waitingForMaterialList = false;
        this.available = new HashMap<>();
        this.loose = new HashMap<>();
        this.inventoryBoxes = new HashMap<>();
        this.invalidate();

        if (joined && this.project == null)
        {
            String last = Configs.Generic.LAST_PROJECT.getStringValue();

            if (last.isEmpty() == false && ProjectStorage.exists(last))
            {
                this.project = ProjectStorage.load(last);
            }
        }
    }

    /**
     * Forwards the completion event to the original listener (usually the Litematica material list GUI)
     */
    private class ChainedListener implements ICompletionListener
    {
        private final MaterialListBase list;
        @Nullable private final ICompletionListener original;

        private ChainedListener(MaterialListBase list, @Nullable ICompletionListener original)
        {
            this.list = list;
            this.original = original;
        }

        @Override
        public void onTaskCompleted()
        {
            if (this.original != null)
            {
                this.original.onTaskCompleted();
            }

            PlannerSession.this.onMaterialListCompleted(this.list);
        }

        @Override
        public void onTaskAborted()
        {
            if (this.original != null)
            {
                this.original.onTaskAborted();
            }

            PlannerSession.this.waitingForMaterialList = false;
        }
    }
}
