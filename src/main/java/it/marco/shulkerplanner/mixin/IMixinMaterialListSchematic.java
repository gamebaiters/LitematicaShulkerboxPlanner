package it.marco.shulkerplanner.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import fi.dy.masa.litematica.materials.MaterialListSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;

@Mixin(value = MaterialListSchematic.class, remap = false)
public interface IMixinMaterialListSchematic
{
    @Accessor("schematic")
    LitematicaSchematic shulkerplanner_getSchematic();
}
