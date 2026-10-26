package it.marco.shulkerplanner.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;

import it.marco.shulkerplanner.render.PackingAssistant;

/**
 * Hooks the packing assistant panel into the shulker box screen
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen
{
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void shulkerplanner_renderPackingAssistant(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci)
    {
        PackingAssistant.render((AbstractContainerScreen<?>) (Object) this, graphics, mouseX, mouseY,
                                this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void shulkerplanner_onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir)
    {
        if (PackingAssistant.onMouseClicked((AbstractContainerScreen<?>) (Object) this, event.x(), event.y()))
        {
            cir.setReturnValue(true);
        }
    }
}
