package it.marco.shulkerplanner.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.gui.PlannerGuis;

/**
 * Adds the "Shulker Planner" button to the bottom row of Litematica's material list GUI
 */
@Mixin(value = GuiMaterialList.class, remap = false)
public abstract class MixinGuiMaterialList
{
    @Inject(method = "initGui", at = @At("TAIL"))
    private void shulkerplanner_addPlannerButton(CallbackInfo ci)
    {
        if (Configs.Generic.ADD_BUTTON_MATERIAL_LIST.getBooleanValue() == false)
        {
            return;
        }

        GuiMaterialList gui = (GuiMaterialList) (Object) this;
        String label = StringUtils.translate("shulkerplanner.gui.button.open_planner_short");
        String mainMenu = StringUtils.translate(GuiMainMenu.ButtonListenerChangeMenu.ButtonType.MAIN_MENU.getLabelKey());
        int mainMenuWidth = gui.getStringWidth(mainMenu) + 20;
        int width = gui.getStringWidth(label) + 10;
        int x = gui.getScreenWidth() - mainMenuWidth - 10 - width - 4;
        int y = gui.getScreenHeight() - 36;

        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label, StringUtils.translate("shulkerplanner.gui.button.open_planner.hover"));
        gui.addButton(button, (btn, mouseButton) -> PlannerGuis.openForMaterialList(gui.getMaterialList(), gui));
    }
}
