package it.marco.shulkerplanner.data;

import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import it.marco.shulkerplanner.util.InventoryScanner;

/**
 * Keeps the shulker boxes linked to the plan attached to their box while they move around:
 * <ul>
 *     <li>the position of a shulker box comes from the block the player right clicked to open it;</li>
 *     <li>when a linked box is broken it becomes "carried", and the shulker box item with the same color,
 *     name and contents in the inventory is that box;</li>
 *     <li>when the player places a carried box (or opens it somewhere else), the new position is taken over.</li>
 * </ul>
 * Nothing here ever links a box on its own: links are only made by the player.
 */
public class ShulkerTracker
{
    public static final ShulkerTracker INSTANCE = new ShulkerTracker();
    private static final long OPEN_WINDOW_MS = 3000;
    private static final int PLACE_WINDOW_TICKS = 60;

    @Nullable private BlockPos lastUsePos;
    @Nullable private String lastUseDim;
    private long lastUseTime;
    @Nullable private PendingPlace pending;
    private int tickCounter;

    private record PendingPlace(String dim, BlockPos pos, String itemId, @Nullable String name, Map<String, Integer> contents, int tick)
    {
    }

    /**
     * What is known about an opened shulker box screen
     *
     * @param dim        the dimension id
     * @param pos        the position of the block, null if unknown (opened by a mod from an item...)
     * @param itemId     the shulker box item id (color), or null if unknown
     * @param customName the name given in an anvil, or null
     */
    public record Opened(String dim, @Nullable BlockPos pos, @Nullable String itemId, @Nullable String customName)
    {
    }

    private ShulkerTracker()
    {
    }

    /**
     * Called when the player right clicks a block (before the server answers)
     */
    public void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null)
        {
            return;
        }

        String dim = mc.level.dimension().identifier().toString();
        this.lastUsePos = hit.getBlockPos().immutable();
        this.lastUseDim = dim;
        this.lastUseTime = System.currentTimeMillis();

        ItemStack held = player.getItemInHand(hand);
        Map<String, Integer> contents = InventoryScanner.getShulkerContents(held);

        // Right clicking an openable shulker box opens it instead of placing (unless sneaking)
        BlockState clicked = mc.level.getBlockState(hit.getBlockPos());

        if (contents != null && (clicked.getBlock() instanceof ShulkerBoxBlock == false || player.isShiftKeyDown()))
        {
            BlockPos target = clicked.canBeReplaced() ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
            this.pending = new PendingPlace(dim, target.immutable(), InventoryScanner.getId(held),
                                            InventoryScanner.getCustomName(held), contents, this.tickCounter);
        }
    }

    public void tick(Minecraft mc, PlannerProject project)
    {
        ++this.tickCounter;

        if (mc.level == null || this.tickCounter % 5 != 0)
        {
            return;
        }

        String dim = mc.level.dimension().identifier().toString();
        boolean changed = false;

        if (this.tickCounter % 20 == 0)
        {
            for (ShulkerBinding binding : project.getBindings())
            {
                BlockPos pos = binding.getPos();

                if (pos != null && dim.equals(binding.getDimension()) && mc.level.isLoaded(pos) &&
                    mc.level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock == false &&
                    mc.player != null && mc.player.blockPosition().distSqr(pos) < 64 * 64)
                {
                    binding.setCarried();
                    changed = true;
                }
            }
        }

        PendingPlace p = this.pending;

        if (p != null)
        {
            if (this.tickCounter - p.tick > PLACE_WINDOW_TICKS)
            {
                this.pending = null;
            }
            else if (dim.equals(p.dim) && mc.level.getBlockState(p.pos).getBlock() instanceof ShulkerBoxBlock)
            {
                this.pending = null;
                ShulkerBinding binding = this.findCarried(project, p.itemId, p.name, p.contents);

                if (binding != null)
                {
                    binding.setPlaced(dim, p.pos);
                    changed = true;
                }
            }
        }

        if (changed)
        {
            PlannerSession.INSTANCE.save();
            PlannerSession.INSTANCE.notifyUpdate();
        }
    }

    @Nullable
    private ShulkerBinding findCarried(PlannerProject project, String itemId, @Nullable String name, Map<String, Integer> contents)
    {
        ShulkerBinding best = null;

        for (ShulkerBinding binding : project.getBindings())
        {
            if (binding.isPlaced() == false && binding.matchesItem(itemId, name, contents))
            {
                // The most recently picked up one, when several identical boxes are carried
                if (best == null || binding.getLastSeen() > best.getLastSeen())
                {
                    best = binding;
                }
            }
        }

        return best;
    }

    /**
     * @return what is known about the shulker box whose screen was just opened
     */
    public Opened identifyOpened(Component title)
    {
        Minecraft mc = Minecraft.getInstance();
        String dim = mc.level != null ? mc.level.dimension().identifier().toString() : "";
        BlockPos pos = null;
        String itemId = null;

        if (mc.level != null && this.lastUsePos != null && dim.equals(this.lastUseDim) &&
            System.currentTimeMillis() - this.lastUseTime < OPEN_WINDOW_MS)
        {
            BlockState state = mc.level.getBlockState(this.lastUsePos);

            if (state.getBlock() instanceof ShulkerBoxBlock)
            {
                pos = this.lastUsePos;
                itemId = BuiltInRegistries.ITEM.getKey(state.getBlock().asItem()).toString();
            }
        }

        String name = title.getString();
        String defaultName = Component.translatable("container.shulkerBox").getString();
        String customName = name.isEmpty() || name.equals(defaultName) ? null : name;

        return new Opened(dim, pos, itemId, customName);
    }

    /**
     * Finds the linked box of an opened shulker box: by position, by its name, or as a carried box
     * that was placed again (same color and contents). Updates the position of the link.
     */
    @Nullable
    public ShulkerBinding findBinding(PlannerProject project, Opened opened, Map<String, Integer> contents)
    {
        if (opened.pos() != null)
        {
            for (ShulkerBinding binding : project.getBindings())
            {
                if (binding.isAt(opened.dim(), opened.pos()))
                {
                    return binding;
                }
            }
        }

        if (opened.customName() != null)
        {
            for (ShulkerBinding binding : project.getBindings())
            {
                if (opened.customName().equals(binding.getCustomName()) &&
                    (opened.itemId() == null || opened.itemId().equals(binding.getItemId())))
                {
                    if (opened.pos() != null)
                    {
                        binding.setPlaced(opened.dim(), opened.pos());
                    }

                    return binding;
                }
            }
        }

        if (opened.pos() != null && opened.itemId() != null)
        {
            ShulkerBinding carried = this.findCarried(project, opened.itemId(), opened.customName(), contents);

            if (carried != null)
            {
                carried.setPlaced(opened.dim(), opened.pos());
                return carried;
            }
        }

        return null;
    }

    /**
     * Creates the link for the opened shulker box
     */
    public ShulkerBinding createBinding(int boxId, Opened opened, Map<String, Integer> contents)
    {
        String itemId = opened.itemId() != null ? opened.itemId() : "minecraft:shulker_box";
        ShulkerBinding binding = new ShulkerBinding(boxId, itemId, opened.customName());

        if (opened.pos() != null)
        {
            binding.setPlaced(opened.dim(), opened.pos());
        }

        binding.setContents(contents);
        return binding;
    }

    /**
     * @return a text telling where the linked box is
     */
    public static String describeLocation(ShulkerBinding binding)
    {
        BlockPos pos = binding.getPos();

        if (pos == null)
        {
            return fi.dy.masa.malilib.util.StringUtils.translate("shulkerplanner.link.carried");
        }

        return fi.dy.masa.malilib.util.StringUtils.translate("shulkerplanner.link.placed", pos.getX(), pos.getY(), pos.getZ());
    }
}
