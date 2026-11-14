package it.marco.shulkerplanner.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;

import it.marco.shulkerplanner.ShulkerPlanner;
import it.marco.shulkerplanner.data.PlannerSession;

/**
 * Tells the planner about every change of a Litematica material list: new counts (refresh, also the
 * "scheduled task" of placement lists), an entry ignored, the ignored entries cleared.
 * The list's completion listener can't be used for this: Litematica's material list screen replaces it
 * with itself every time it is opened, and ignoring an entry doesn't call it at all.
 */
@Mixin(value = MaterialListBase.class, remap = false)
public abstract class MixinMaterialListBase
{
    @Inject(method = "setMaterialListEntries", at = @At("TAIL"))
    private void shulkerplanner_onEntriesSet(List<MaterialListEntry> list, CallbackInfo ci)
    {
        this.shulkerplanner_notify(true);
    }

    @Inject(method = "ignoreEntry", at = @At("TAIL"))
    private void shulkerplanner_onEntryIgnored(MaterialListEntry entry, CallbackInfo ci)
    {
        this.shulkerplanner_notify(false);
    }

    @Inject(method = "clearIgnored", at = @At("TAIL"))
    private void shulkerplanner_onIgnoredCleared(CallbackInfo ci)
    {
        this.shulkerplanner_notify(false);
    }

    private void shulkerplanner_notify(boolean newCounts)
    {
        try
        {
            PlannerSession.INSTANCE.onLitematicaListChanged((MaterialListBase) (Object) this, newCounts);
        }
        catch (Exception e)
        {
            ShulkerPlanner.LOGGER.warn("Shulker Planner: failed to follow a material list change", e);
        }
    }
}
