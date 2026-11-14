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
import net.minecraft.world.inventory.Slot;

import it.marco.shulkerplanner.render.PackingAssistant;

/**
 * Hooks the packing assistant panel into the shulker box screen: drawing, clicks, dragging (move, resize,
 * scroll bar), the mouse wheel, and no slot hover or tooltip under the panel
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

    @Inject(method = "removed", at = @At("HEAD"))
    private void shulkerplanner_onRemoved(CallbackInfo ci)
    {
        PackingAssistant.onScreenClosed();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void shulkerplanner_onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir)
    {
        if (PackingAssistant.onMouseClicked((AbstractContainerScreen<?>) (Object) this, event.x(), event.y(), event.button()))
        {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void shulkerplanner_onMouseDragged(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir)
    {
        if (PackingAssistant.onMouseDragged((AbstractContainerScreen<?>) (Object) this, event.x(), event.y()))
        {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void shulkerplanner_onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir)
    {
        if (PackingAssistant.onMouseReleased((AbstractContainerScreen<?>) (Object) this))
        {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void shulkerplanner_onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir)
    {
        if (PackingAssistant.onMouseScrolled((AbstractContainerScreen<?>) (Object) this, mouseX, mouseY, scrollY))
        {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getHoveredSlot", at = @At("HEAD"), cancellable = true)
    private void shulkerplanner_noSlotUnderPanel(double mouseX, double mouseY, CallbackInfoReturnable<Slot> cir)
    {
        if (PackingAssistant.isMouseOverPanel(mouseX, mouseY))
        {
            cir.setReturnValue(null);
        }
    }
}
