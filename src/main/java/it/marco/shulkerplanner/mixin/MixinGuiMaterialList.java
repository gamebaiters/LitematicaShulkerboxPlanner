package it.marco.shulkerplanner.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.gui.PlannerGuis;

/**
 * Adds the "Shulker Planner" button to Litematica's material list GUI, in the top row of buttons when there is room
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
        int width = gui.getStringWidth(label) + 10;
        int x;
        int y;

        // In the top row after Litematica's buttons, before the multiplier field, if there is room:
        // the bottom row has Litematica's summary text, which a button there would cover on small screens
        IMixinGuiBase base = (IMixinGuiBase) gui;
        int topY = Integer.MAX_VALUE;

        for (ButtonBase b : base.shulkerplanner_getButtons())
        {
            topY = Math.min(topY, b.getY());
        }

        int rowRight = 0;

        for (ButtonBase b : base.shulkerplanner_getButtons())
        {
            if (b.getY() == topY)
            {
                rowRight = Math.max(rowRight, b.getX() + b.getWidth());
            }
        }

        int limit = gui.getScreenWidth() - 10;

        for (var wrapper : base.shulkerplanner_getTextFields())
        {
            var field = wrapper.textField();

            if (Math.abs(field.getY() - topY) < 20)
            {
                limit = Math.min(limit, field.getX() - gui.getStringWidth(StringUtils.translate("litematica.gui.label.material_list.multiplier")) - 8);
            }
        }

        if (topY != Integer.MAX_VALUE && rowRight + 4 + width <= limit)
        {
            x = rowRight + 4;
            y = topY;
        }
        else
        {
            String mainMenu = StringUtils.translate(GuiMainMenu.ButtonListenerChangeMenu.ButtonType.MAIN_MENU.getLabelKey());
            int mainMenuWidth = gui.getStringWidth(mainMenu) + 20;
            x = gui.getScreenWidth() - mainMenuWidth - 10 - width - 4;
            y = gui.getScreenHeight() - 36;
        }

        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label, StringUtils.translate("shulkerplanner.gui.button.open_planner.hover"));
        gui.addButton(button, (btn, mouseButton) -> PlannerGuis.openForMaterialList(gui.getMaterialList(), gui));
    }
}
