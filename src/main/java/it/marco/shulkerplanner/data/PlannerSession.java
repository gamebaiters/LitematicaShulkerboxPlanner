package it.marco.shulkerplanner.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListPlacement;
import fi.dy.masa.litematica.materials.MaterialListSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.util.InfoUtils;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.hud.HudStyle;
import it.marco.shulkerplanner.hud.HudStyles;
import it.marco.shulkerplanner.mixin.IMixinMaterialListBase;
import it.marco.shulkerplanner.mixin.IMixinMaterialListPlacement;
import it.marco.shulkerplanner.mixin.IMixinMaterialListSchematic;
import it.marco.shulkerplanner.plan.BoxProgress;
import it.marco.shulkerplanner.plan.BoxState;
import it.marco.shulkerplanner.plan.ItemRequirement;
import it.marco.shulkerplanner.plan.PlanRebaser;
import it.marco.shulkerplanner.plan.PlannedBox;
import it.marco.shulkerplanner.plan.ShulkerPacker;
import it.marco.shulkerplanner.plan.ShulkerPlan;
import it.marco.shulkerplanner.recipe.RawMaterialCalculator;
import it.marco.shulkerplanner.recipe.RecipeService;
import it.marco.shulkerplanner.util.InventoryScanner;

/**
 * The currently active project, the Litematica material list it is linked to (if any)
 * and the shulker box plan calculated from it.
 * <p>
 * The plan is a draft that follows the material list and the options until the player starts working
 * on it (marks a box, links a shulker box) or locks it. A locked plan never changes on its own:
 * the boxes keep their number and contents, and updating it keeps every started box as it is.
 */
public class PlannerSession implements IClientTickHandler
{
    public static final PlannerSession INSTANCE = new PlannerSession();

    @Nullable private PlannerProject project;
    @Nullable private MaterialListBase materialList;
    @Nullable private ShulkerPlan plan;
    @Nullable private RawMaterialCalculator.Result rawResult;
    @Nullable private RawMaterialCalculator rawCalculator;
    @Nullable private Map<String, Long> listedCounts;
    @Nullable private Runnable updateListener;
    private Map<String, Integer> available = new HashMap<>();
    private Map<String, Integer> loose = new HashMap<>();
    private Map<String, Integer> bundles = new HashMap<>();
    private List<InventoryScanner.ShulkerItem> inventoryShulkers = new ArrayList<>();
    private Map<String, Integer> inventoryBoxes = new HashMap<>();
    /** The bound box whose shulker box screen is open right now, -1 if none */
    private int openBoxId = -1;
    private boolean waitingForMaterialList;
    private int tickCounter;
    @Nullable private Object lastSelectedPlacement;
    @Nullable private String unplannedSelection;
    private boolean loadedInGame;
    private Set<String> litematicaIgnored = new HashSet<>();

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

        if (this.project != null)
        {
            this.project.setSchematicName(getSchematicName(list));
        }

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
            this.openBoxId = -1;
            this.litematicaIgnored = new HashSet<>();
            this.invalidate();
        }

        Configs.Generic.LAST_PROJECT.setValueFromString(name);
        this.unplannedSelection = null;
        // A project opened on purpose stays active until the selection in Litematica changes again
        this.lastSelectedPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        this.loadedInGame = this.computeLoadedInGame();
    }

    /**
     * @return the name of the schematic behind a Litematica material list
     */
    public static String getSchematicName(MaterialListBase list)
    {
        try
        {
            if (list instanceof MaterialListPlacement)
            {
                SchematicPlacement placement = ((IMixinMaterialListPlacement) list).shulkerplanner_getPlacement();
                LitematicaSchematic schematic = placement != null ? placement.getSchematic() : null;

                if (schematic != null && schematic.getMetadata().getName() != null && schematic.getMetadata().getName().isEmpty() == false)
                {
                    return schematic.getMetadata().getName();
                }

                return placement != null ? placement.getName() : list.getName();
            }
            else if (list instanceof MaterialListSchematic)
            {
                LitematicaSchematic schematic = ((IMixinMaterialListSchematic) list).shulkerplanner_getSchematic();

                if (schematic != null && schematic.getMetadata().getName() != null && schematic.getMetadata().getName().isEmpty() == false)
                {
                    return schematic.getMetadata().getName();
                }
            }
        }
        catch (Exception ignore)
        {
        }

        return list.getName();
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
        Set<String> ignored = new HashSet<>();
        Set<MaterialListEntry> ignoredEntries = null;

        try
        {
            ignoredEntries = ((IMixinMaterialListBase) this.materialList).shulkerplanner_getIgnored();
        }
        catch (Exception ignore) {}

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

            if (ignoredEntries != null && ignoredEntries.contains(entry))
            {
                ignored.add(id);
            }
        }

        List<MaterialRow> rows = counts.entrySet().stream()
                .map(e -> new MaterialRow(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        this.project.setMaterials(rows, this.materialList.getTitle());
        this.litematicaIgnored = ignored;

        if (this.materialList.getMultiplier() > 1 && this.project.getMultiplier() == 1)
        {
            this.project.setMultiplier(this.materialList.getMultiplier());
        }

        this.applyAvailableCounts();
        this.save();
    }

    /**
     * @return true if the entry is hidden ("ignored") in Litematica's material list, and that is respected
     */
    public boolean isIgnoredInLitematica(String id)
    {
        return Configs.Generic.RESPECT_LITEMATICA_IGNORED.getBooleanValue() && this.litematicaIgnored.contains(id);
    }

    public void invalidate()
    {
        this.plan = null;
        this.rawResult = null;
        this.rawCalculator = null;
        this.listedCounts = null;
    }

    /**
     * Call after changing anything in the project that doesn't affect the plan's boxes
     */
    public void onProjectChanged()
    {
        this.invalidate();
        this.save();
    }

    /**
     * Call after changing an option that changes how the boxes are packed.
     * A locked plan gets updated right away, keeping every started box as it is.
     */
    public void onPackingOptionsChanged()
    {
        this.invalidate();

        if (this.project != null && this.project.isLocked())
        {
            this.updateLockedPlan();
        }

        this.save();
    }

    // ---------------------------------------------------------------- requirements

    /**
     * @return what has to be carried: the listed materials, or their raw materials
     */
    public Map<String, Long> getRequiredCounts()
    {
        if (this.project == null)
        {
            return Map.of();
        }

        if (this.project.getPackRawMaterials())
        {
            RawMaterialCalculator.Result raw = this.getRawResult();
            return raw != null ? raw.raw() : Map.of();
        }

        return this.getListedCounts();
    }

    /**
     * @return the counts of the included listed materials (count mode and multiplier applied)
     */
    public Map<String, Long> getListedCounts()
    {
        if (this.project == null)
        {
            return Map.of();
        }

        if (this.listedCounts == null)
        {
            Map<String, Long> counts = new LinkedHashMap<>(this.project.buildRequiredCounts());
            counts.keySet().removeIf(this::isIgnoredInLitematica);
            this.listedCounts = counts;
        }

        return this.listedCounts;
    }

    public List<ItemRequirement> getRequirements()
    {
        return PlannerProject.createRequirements(this.getRequiredCounts());
    }

    /**
     * @return the raw materials of the listed materials, null without a project
     */
    @Nullable
    public RawMaterialCalculator.Result getRawResult()
    {
        if (this.project == null)
        {
            return null;
        }

        if (this.rawResult == null)
        {
            this.rawResult = this.getRawCalculator().calculate(this.getListedCounts());
        }

        return this.rawResult;
    }

    public RawMaterialCalculator getRawCalculator()
    {
        if (this.rawCalculator == null)
        {
            PlannerProject p = this.project;
            RawMaterialCalculator.Options options = p == null ? RawMaterialCalculator.Options.defaults() :
                    new RawMaterialCalculator.Options(Set.copyOf(p.getRawBaseItems()), Map.copyOf(p.getRecipeChoice()),
                                                      p.getAllowSmelting(), p.getAllowStonecutter());
            this.rawCalculator = new RawMaterialCalculator(RecipeService.getIndex(), options);
        }

        return this.rawCalculator;
    }

    // ---------------------------------------------------------------- plan

    public ShulkerPlan getPlan()
    {
        if (this.plan == null)
        {
            if (this.project == null)
            {
                return ShulkerPlan.EMPTY;
            }

            PlannerProject p = this.project;
            List<ItemRequirement> requirements = this.getRequirements();
            ShulkerPlan draft = ShulkerPacker.pack(requirements, p.getPackMode(), p.getRoundUpStacks(), p.getFillSpareSlots());

            if (p.isLocked() == false && p.hasLegacyDoneBoxes() && draft.getBoxes().isEmpty() == false)
            {
                // 1.x project with boxes marked as done: lock the plan they were marked on
                p.lock(draft.getBoxes(), this.getRequiredCounts());
                this.save();
            }

            if (p.isLocked())
            {
                this.plan = new ShulkerPlan(p.getLockedBoxes(), draft.getLooseItems(), draft.getRequirements());
            }
            else
            {
                this.plan = draft;
            }
        }

        return this.plan;
    }

    public boolean isPlanLocked()
    {
        return this.project != null && this.project.isLocked();
    }

    /**
     * Locks the current plan, if not locked yet. Done automatically when the player starts working on a box.
     */
    public void lockPlan()
    {
        if (this.project != null && this.project.isLocked() == false)
        {
            ShulkerPlan current = this.getPlan();
            this.project.lock(current.getBoxes(), this.getRequiredCounts());
            this.invalidate();
            this.save();
        }
    }

    /**
     * @return true if the locked plan was made for other amounts than the current ones
     */
    public boolean isPlanOutdated()
    {
        if (this.project == null || this.project.isLocked() == false)
        {
            return false;
        }

        return this.project.getLockedRequirements().equals(this.getRequiredCounts()) == false;
    }

    /**
     * @return the difference between the current requirements and the ones the plan was locked with:
     * item id to (current - locked)
     */
    public Map<String, Long> getPlanChanges()
    {
        Map<String, Long> diff = new LinkedHashMap<>();

        if (this.project == null || this.project.isLocked() == false)
        {
            return diff;
        }

        Map<String, Long> now = this.getRequiredCounts();
        Map<String, Long> then = this.project.getLockedRequirements();

        for (Map.Entry<String, Long> e : now.entrySet())
        {
            long d = e.getValue() - then.getOrDefault(e.getKey(), 0L);

            if (d != 0)
            {
                diff.put(e.getKey(), d);
            }
        }

        for (Map.Entry<String, Long> e : then.entrySet())
        {
            if (now.containsKey(e.getKey()) == false)
            {
                diff.put(e.getKey(), -e.getValue());
            }
        }

        return diff;
    }

    /**
     * Repacks the locked plan for the current requirements and options. Started boxes stay exactly as they are.
     */
    public void updateLockedPlan()
    {
        PlannerProject p = this.project;

        if (p == null || p.isLocked())
        {
            if (p == null)
            {
                return;
            }

            List<PlannedBox> kept = new ArrayList<>();

            for (PlannedBox box : p.getLockedBoxes())
            {
                if (p.isBoxStarted(box))
                {
                    kept.add(box);
                }
            }

            this.listedCounts = null;
            this.rawResult = null;
            List<PlannedBox> boxes = PlanRebaser.rebase(kept, this.getRequirements(), p.getPackMode(),
                                                        p.getRoundUpStacks(), p.getFillSpareSlots(), p.getNextBoxId());
            p.lock(boxes, this.getRequiredCounts());
            p.pruneBoxes(boxes);
            this.plan = null;
            this.save();
            this.notifyUpdate();
        }
    }

    /**
     * Throws the locked plan away: back to a draft that follows the material list. Box states and links are removed.
     */
    public void resetPlan()
    {
        if (this.project != null)
        {
            this.project.unlock();
            this.openBoxId = -1;
            this.invalidate();
            this.save();
        }
    }

    public void save()
    {
        if (this.project != null)
        {
            ProjectStorage.save(this.project);
        }
    }

    // ---------------------------------------------------------------- box states

    public BoxState getBoxState(PlannedBox box)
    {
        return this.project != null ? this.project.getBoxState(box.getId()) : BoxState.TODO;
    }

    /**
     * Sets the state of a box by hand. Locks the plan.
     */
    public void setBoxState(PlannedBox box, BoxState state)
    {
        if (this.project == null)
        {
            return;
        }

        this.lockPlan();
        this.project.setBoxState(box.getId(), state);
        this.plan = null;
        this.save();
        this.notifyUpdate();
    }

    public boolean isBoxDone(PlannedBox box)
    {
        return this.getBoxState(box) == BoxState.DONE;
    }

    public boolean isBoxInInventory(PlannedBox box)
    {
        return box.getDuplicateOrdinal() < this.inventoryBoxes.getOrDefault(box.getSignature(), 0);
    }

    /**
     * @return true if the box is done, or an identically filled shulker box is in the inventory
     */
    public boolean isBoxReady(PlannedBox box)
    {
        return this.isBoxDone(box) || this.isBoxInInventory(box);
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

    // ---------------------------------------------------------------- linked shulker boxes

    @Nullable
    public ShulkerBinding getBinding(PlannedBox box)
    {
        return this.project != null ? this.project.getBinding(box.getId()) : null;
    }

    /**
     * Links a real shulker box to a box of the plan. Locks the plan.
     */
    public void link(PlannedBox box, ShulkerBinding binding)
    {
        if (this.project == null)
        {
            return;
        }

        this.lockPlan();
        this.project.addBinding(binding);
        this.plan = null;
        this.save();
        this.notifyUpdate();
    }

    public void unlink(PlannedBox box)
    {
        if (this.project != null && this.project.getBinding(box.getId()) != null)
        {
            this.project.removeBinding(box.getId());

            if (this.project.getBoxState(box.getId()) == BoxState.FILLING)
            {
                this.project.setBoxState(box.getId(), BoxState.TODO);
            }

            if (this.openBoxId == box.getId())
            {
                this.openBoxId = -1;
            }

            this.plan = null;
            this.save();
            this.notifyUpdate();
        }
    }

    /**
     * Called by the packing assistant while the screen of a linked shulker box is open
     */
    public void setOpenBox(int boxId)
    {
        this.openBoxId = boxId;
    }

    public int getOpenBoxId()
    {
        return this.openBoxId;
    }

    /**
     * @return the contents of the box as last seen (its linked shulker box), or null if unknown
     */
    @Nullable
    public Map<String, Integer> getKnownContents(PlannedBox box)
    {
        ShulkerBinding binding = this.getBinding(box);
        return binding != null ? binding.getContents() : null;
    }

    public BoxProgress getProgress(PlannedBox box)
    {
        Map<String, Integer> contents = this.getKnownContents(box);

        if (contents == null && this.isBoxDone(box))
        {
            return BoxProgress.compute(box, box.getTotals(), this.loose, true);
        }

        return BoxProgress.compute(box, contents != null ? contents : Map.of(), this.loose, contents != null);
    }

    // ---------------------------------------------------------------- followed box

    public void setFollowedBox(@Nullable PlannedBox box)
    {
        if (this.project != null)
        {
            this.project.setFollowedBoxId(box != null ? box.getId() : -1);
            this.save();
        }
    }

    public boolean isFollowed(PlannedBox box)
    {
        return this.project != null && this.project.getFollowedBoxId() == box.getId();
    }

    /**
     * @return the box being worked on: the one the player follows, the linked box that is open right now,
     * the first started box, the first box still to do. Null when everything is done.
     */
    @Nullable
    public PlannedBox getCurrentBox()
    {
        if (this.project == null)
        {
            return null;
        }

        ShulkerPlan plan = this.getPlan();
        PlannedBox followed = plan.getBoxById(this.project.getFollowedBoxId());

        if (followed != null && this.isBoxReady(followed) == false)
        {
            return followed;
        }

        if (Configs.Generic.HUD_FOLLOW_OPENED_BOX.getBooleanValue())
        {
            PlannedBox open = plan.getBoxById(this.openBoxId);

            if (open != null && this.isBoxDone(open) == false)
            {
                return open;
            }
        }

        for (PlannedBox box : plan.getBoxes())
        {
            if (this.getBoxState(box) == BoxState.FILLING && this.isBoxReady(box) == false)
            {
                return box;
            }
        }

        for (PlannedBox box : plan.getBoxes())
        {
            if (this.isBoxReady(box) == false)
            {
                return box;
            }
        }

        return null;
    }

    @Nullable
    public BoxProgress getCurrentProgress()
    {
        PlannedBox box = this.getCurrentBox();
        return box != null ? this.getProgress(box) : null;
    }

    /**
     * @return the HUD style of the active project, or the default style without a project
     */
    public HudStyle getHudStyle()
    {
        return this.project != null ? this.project.getHudStyle() : HudStyles.getDefault();
    }

    /**
     * @return the name shown on the HUD: the custom title, or the schematic name of the active project
     */
    public String getDisplayName()
    {
        return this.project != null ? this.getHudStyle().resolveTitle(this.project.getSchematicName()) : "";
    }

    // ---------------------------------------------------------------- inventory and material progress

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

    public Map<String, Integer> getLooseItems()
    {
        return this.loose;
    }

    public List<InventoryScanner.ShulkerItem> getInventoryShulkers()
    {
        return this.inventoryShulkers;
    }

    /**
     * @return how many of the item are already packed: in the boxes marked as done (as planned),
     * and in the linked boxes still being filled (as last seen)
     */
    public Map<String, Long> getPackedCounts()
    {
        Map<String, Long> packed = new HashMap<>();

        for (PlannedBox box : this.getPlan().getBoxes())
        {
            Map<String, Integer> contents;

            if (this.isBoxDone(box))
            {
                Map<String, Integer> known = this.getKnownContents(box);
                contents = known != null && known.isEmpty() == false ? known : box.getTotals();
            }
            else if (this.isBoxInInventory(box))
            {
                contents = box.getTotals();
            }
            else
            {
                contents = this.getKnownContents(box);
            }

            if (contents != null)
            {
                contents.forEach((id, count) -> packed.merge(id, (long) count, Long::sum));
            }
        }

        return packed;
    }

    /**
     * @return what the player carries that isn't packed yet: loose items, bundles,
     * and shulker boxes that are not linked or ready boxes of the plan
     */
    public Map<String, Long> getCarryingCounts()
    {
        Map<String, Long> carrying = new HashMap<>();
        this.loose.forEach((id, c) -> carrying.merge(id, (long) c, Long::sum));
        this.bundles.forEach((id, c) -> carrying.merge(id, (long) c, Long::sum));

        Map<String, Integer> readyLeft = new HashMap<>();

        for (PlannedBox box : this.getPlan().getBoxes())
        {
            if (this.isBoxDone(box) || this.isBoxInInventory(box))
            {
                readyLeft.merge(box.getSignature(), 1, Integer::sum);
            }
        }

        List<ShulkerBinding> bindings = this.project != null ? this.project.getBindings() : List.of();

        for (InventoryScanner.ShulkerItem item : this.inventoryShulkers)
        {
            boolean known = false;

            for (ShulkerBinding binding : bindings)
            {
                if (binding.isPlaced() == false && binding.matchesItem(item.itemId(), item.customName(), item.contents()))
                {
                    known = true;
                    break;
                }
            }

            if (known == false && readyLeft.getOrDefault(item.signature(), 0) > 0)
            {
                readyLeft.merge(item.signature(), -1, Integer::sum);
                known = true;
            }

            if (known == false)
            {
                item.contents().forEach((id, c) -> carrying.merge(id, (long) c, Long::sum));
            }
        }

        // The shulker box items themselves are not "carried materials"
        return carrying;
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
        this.inventoryShulkers = InventoryScanner.getShulkerItems(mc.player);
        this.loose = InventoryScanner.countLooseItems(mc.player);
        this.bundles = InventoryScanner.countBundleItems(mc.player);
        boolean availableChanged = newAvailable.equals(this.available) == false;
        boolean boxesChanged = newBoxes.equals(this.inventoryBoxes) == false;

        this.available = newAvailable;
        this.inventoryBoxes = newBoxes;
        this.applyAvailableCounts();

        boolean marked = Configs.Generic.AUTO_DETECT_INVENTORY_BOXES.getBooleanValue() && boxesChanged && this.autoMarkBoxes();
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

        boolean changed = false;

        for (PlannedBox box : this.getPlan().getBoxes())
        {
            if (this.isBoxInInventory(box) && this.isBoxDone(box) == false)
            {
                this.lockPlan();
                this.project.setBoxState(box.getId(), BoxState.DONE);
                changed = true;
            }
        }

        if (changed)
        {
            this.plan = null;
            this.save();
        }

        return changed;
    }

    /**
     * Marks the box as done, if it is not done yet. Used by the packing assistant.
     */
    public void markBoxDone(PlannedBox box)
    {
        if (this.project != null && this.isBoxDone(box) == false)
        {
            this.setBoxState(box, BoxState.DONE);

            if (Configs.Warnings.NOTIFY_BOX_COMPLETE.getBooleanValue())
            {
                InfoUtils.printActionbarMessage("shulkerplanner.message.box_complete", box.getIndex());
            }
        }
    }

    public void notifyUpdate()
    {
        if (this.updateListener != null)
        {
            this.updateListener.run();
        }
    }

    @Override
    public void onClientTick(Minecraft mc)
    {
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        // Follow the placement selection right away, also when no project is open yet
        this.followSelectedPlacement();

        if (++this.tickCounter % 10 == 0)
        {
            this.loadedInGame = this.computeLoadedInGame();
        }

        if (this.project == null)
        {
            return;
        }

        // The HUD shows what the player carries for the current box: keep it updated every tick
        this.loose = InventoryScanner.countLooseItems(mc.player);
        ShulkerTracker.INSTANCE.tick(mc, this.project);

        if (this.tickCounter >= 20)
        {
            this.tickCounter = 0;
            this.refreshInventory(true);
        }
    }

    /**
     * @return true if the schematic of the active project is currently loaded in Litematica
     * (as a placement, or as a loaded schematic). The HUD is only shown for loaded schematics.
     */
    public boolean isLoadedInGame()
    {
        return this.project != null && this.loadedInGame && this.unplannedSelection == null;
    }

    /**
     * @return the name of the selected placement when it has no plan yet, otherwise null
     */
    @Nullable
    public String getUnplannedSelection()
    {
        return this.unplannedSelection;
    }

    private boolean computeLoadedInGame()
    {
        if (this.project == null)
        {
            return false;
        }

        String name = this.project.getName();

        try
        {
            for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements())
            {
                if (name.equals(placement.getName()))
                {
                    return true;
                }
            }

            for (LitematicaSchematic schematic : SchematicHolder.getInstance().getAllSchematics())
            {
                if (name.equals(schematic.getMetadata().getName()))
                {
                    return true;
                }
            }
        }
        catch (Exception ignore)
        {
        }

        // Lists that don't come from a schematic (area analysis, custom lists) are loaded as long as they're linked
        return this.materialList != null &&
               (this.materialList instanceof MaterialListPlacement) == false &&
               (this.materialList instanceof MaterialListSchematic) == false;
    }

    /**
     * Makes the active project follow the placement selected in Litematica.
     * A selected placement without a plan hides the HUD of the previous project and shows a hint instead.
     */
    private void followSelectedPlacement()
    {
        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement == this.lastSelectedPlacement)
        {
            return;
        }

        this.lastSelectedPlacement = placement;
        this.unplannedSelection = null;

        if (placement == null || Configs.Generic.HUD_FOLLOW_PLACEMENT.getBooleanValue() == false)
        {
            this.loadedInGame = this.computeLoadedInGame();
            return;
        }

        String name = placement.getName();

        if (this.project == null || this.project.getName().equals(name) == false)
        {
            if (ProjectStorage.exists(name))
            {
                MaterialListBase list = placement.getMaterialList();

                if (list != null && list.getMaterialsAll().isEmpty() == false)
                {
                    this.openForMaterialList(list);
                }
                else
                {
                    this.openProject(name);
                }

                this.notifyUpdate();
            }
            else
            {
                this.unplannedSelection = name;
            }
        }

        this.loadedInGame = this.computeLoadedInGame();
    }

    public void onWorldChanged(boolean joined)
    {
        this.save();

        // Litematica clears its material lists when leaving a world
        this.materialList = null;
        this.waitingForMaterialList = false;
        this.available = new HashMap<>();
        this.loose = new HashMap<>();
        this.bundles = new HashMap<>();
        this.inventoryBoxes = new HashMap<>();
        this.inventoryShulkers = new ArrayList<>();
        this.openBoxId = -1;
        this.lastSelectedPlacement = null;
        this.unplannedSelection = null;
        this.loadedInGame = false;
        RecipeService.invalidate();
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
