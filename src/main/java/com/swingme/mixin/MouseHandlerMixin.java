package com.swingme.mixin;

import com.swingme.util.FreeCamera;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    /** While the free camera is up the wheel drives its distance, so the hotbar must not move. */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void swingme$scrollAdjustsDistance(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (!FreeCamera.isActive()) return;

        // An open screen still gets its scroll: only the in-world wheel is taken over.
        Minecraft client = Minecraft.getInstance();
        if (client.screen != null) return;

        FreeCamera.zoom(yOffset);
        ci.cancel();
    }

    /**
     * Wrapped rather than cancelling {@code turnPlayer}, so the deltas arriving here have already
     * had sensitivity and mouse inversion applied and the camera keeps the feel of normal looking.
     * Skipping the call is what freezes the player's facing.
     */
    @WrapOperation(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void swingme$lookMovesCamera(LocalPlayer player, double yawDelta, double pitchDelta,
                                         Operation<Void> original) {
        if (FreeCamera.isActive()) {
            FreeCamera.look(yawDelta, pitchDelta);
        } else {
            original.call(player, yawDelta, pitchDelta);
        }
    }
}
