package it.marco.shulkerplanner.mixin;

import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.malilib.interfaces.ICompletionListener;

@Mixin(value = MaterialListBase.class, remap = false)
public interface IMixinMaterialListBase
{
    @Nullable
    @Accessor("completionListener")
    ICompletionListener shulkerplanner_getCompletionListener();

    @Accessor("ignored")
    java.util.Set<fi.dy.masa.litematica.materials.MaterialListEntry> shulkerplanner_getIgnored();
}
