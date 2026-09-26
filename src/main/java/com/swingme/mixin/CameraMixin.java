package com.swingme.mixin;

import com.swingme.util.FeatureFlags;
import com.swingme.util.FreeCamera;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Camera;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {

    /**
     * Vanilla shortens the third-person distance by raycasting against blocks, which is what makes
     * the camera snap to your face indoors. Returning the requested distance unmodified skips that
     * raycast entirely, and is also where the free camera's own distance is substituted — the
     * result feeds straight into the {@code move} call that places the camera.
     * <p>
     * The unlocked state (F5 pressed while the orbit was active, see {@link FreeCamera}) keeps
     * fixing the distance too, unconditionally: that is what "keeps the distance" means once
     * rotation has been handed back to the player.
     */
    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void swingme$cameraDistance(float requested, CallbackInfoReturnable<Float> cir) {
        if (FreeCamera.isActive() || FreeCamera.isUnlocked()) {
            cir.setReturnValue(FreeCamera.distance());
        } else if (FeatureFlags.isEnabled(FeatureFlags.PERSPECTIVE)) {
            cir.setReturnValue(requested);
        }
    }

    /**
     * The camera is placed by rotating first and then moving backwards along that rotation, so
     * replacing the rotation is enough to orbit the head instead of sitting behind the player.
     */
    @WrapOperation(
            method = "alignWithEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V")
    )
    private void swingme$freeCameraRotation(Camera camera, float yaw, float pitch, Operation<Void> original) {
        if (FreeCamera.isActive()) {
            original.call(camera, FreeCamera.yaw(), FreeCamera.pitch());
        } else {
            original.call(camera, yaw, pitch);
        }
    }
}
