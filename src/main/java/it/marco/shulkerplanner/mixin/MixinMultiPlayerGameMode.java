package it.marco.shulkerplanner.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

import it.marco.shulkerplanner.data.ShulkerTracker;

/**
 * Remembers which block the player right clicked, so an opened shulker box screen can be tied to its position,
 * and a placed shulker box to its new position
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode
{
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void shulkerplanner_onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir)
    {
        ShulkerTracker.INSTANCE.onUseItemOn(player, hand, hit);
    }
}
