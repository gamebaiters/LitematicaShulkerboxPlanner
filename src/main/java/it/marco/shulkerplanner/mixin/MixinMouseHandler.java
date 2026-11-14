package it.marco.shulkerplanner.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonInfo;

import it.marco.shulkerplanner.render.PackingAssistant;

/**
 * Gives the mouse buttons and the wheel to the packing assistant panel before anything else sees them.
 * malilib (and the mods using it, like Item Scroller and Tweakeroo), Mouse Tweaks and other inventory mods
 * handle the mouse in MouseHandler or through Fabric's screen events, before AbstractContainerScreen#mouseClicked,
 * and can use a click that was meant for the panel: then the panel buttons, moving and resizing did nothing.
 */
@Mixin(value = MouseHandler.class, priority = 500)
public abstract class MixinMouseHandler
{
    @Shadow @Final private Minecraft minecraft;

    @Shadow public abstract double getScaledXPos(Window window);
    @Shadow public abstract double getScaledYPos(Window window);

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void shulkerplanner_onButton(long handle, MouseButtonInfo info, int action, CallbackInfo ci)
    {
        Window window = this.minecraft.getWindow();

        if (handle != window.handle() || this.minecraft.gui.overlay() != null ||
            !(this.minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen))
        {
            return;
        }

        double x = this.getScaledXPos(window);
        double y = this.getScaledYPos(window);

        if (action == InputConstants.PRESS)
        {
            if (PackingAssistant.onMouseClicked(screen, x, y, info.button()))
            {
                ci.cancel();
            }
        }
        else if (action == InputConstants.RELEASE)
        {
            if (PackingAssistant.onMouseReleased(screen))
            {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void shulkerplanner_onScroll(long handle, double xOffset, double yOffset, CallbackInfo ci)
    {
        Window window = this.minecraft.getWindow();

        if (handle != window.handle() || this.minecraft.gui.overlay() != null ||
            !(this.minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen))
        {
            return;
        }

        // Like vanilla: discrete scrolling and the wheel sensitivity
        double amount = this.minecraft.options.discreteMouseScroll().get() ? Math.signum(yOffset) : yOffset;
        amount *= this.minecraft.options.mouseWheelSensitivity().get();

        if (PackingAssistant.onMouseScrolled(screen, this.getScaledXPos(window), this.getScaledYPos(window), amount))
        {
            ci.cancel();
        }
    }
}
