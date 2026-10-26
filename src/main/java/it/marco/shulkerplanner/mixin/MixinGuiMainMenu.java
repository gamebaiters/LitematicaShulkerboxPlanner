package it.marco.shulkerplanner.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.gui.PlannerGuis;

/**
 * Adds the "Shulker Planner" button to Litematica's main menu, in the free space below "Configuration"
 */
@Mixin(value = GuiMainMenu.class, remap = false)
public abstract class MixinGuiMainMenu
{
    @Shadow
    private int getButtonWidth() { return 0; }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void shulkerplanner_addPlannerButton(CallbackInfo ci)
    {
        if (Configs.Generic.ADD_BUTTON_MAIN_MENU.getBooleanValue() == false)
        {
            return;
        }

        GuiMainMenu gui = (GuiMainMenu) (Object) this;
        int width = this.getButtonWidth();
        int x = 12 + width + 20;
        int y = 30 + 22;

        String label = StringUtils.translate("shulkerplanner.gui.button.open_planner");
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label, StringUtils.translate("shulkerplanner.gui.button.open_planner.hover"));
        gui.addButton(button, (btn, mouseButton) -> PlannerGuis.openPlanner(gui, null));
    }
}
