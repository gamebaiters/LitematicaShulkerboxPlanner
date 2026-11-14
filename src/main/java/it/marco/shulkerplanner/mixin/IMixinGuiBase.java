package it.marco.shulkerplanner.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;

/**
 * The buttons and text fields of a malilib screen, to place the planner button where it doesn't cover anything
 */
@Mixin(value = GuiBase.class, remap = false)
public interface IMixinGuiBase
{
    @Accessor("buttons")
    List<ButtonBase> shulkerplanner_getButtons();

    @Accessor("textFields")
    List<TextFieldWrapper<? extends GuiTextFieldGeneric>> shulkerplanner_getTextFields();
}
