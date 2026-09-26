package com.swingme.mixin;

import com.swingme.util.FeatureFlags;
import com.swingme.util.FreeCamera;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides what pressing F5 does, in two unrelated ways that both hook the same method:
 * <ul>
 *   <li>Skips THIRD_PERSON_FRONT when selfie cam is disabled — normally
 *       FIRST_PERSON &rarr; THIRD_PERSON_BACK &rarr; THIRD_PERSON_FRONT &rarr; FIRST_PERSON,
 *       this makes it FIRST_PERSON &rarr; THIRD_PERSON_BACK &rarr; FIRST_PERSON.</li>
 *   <li>While the free camera is engaged, F5 drives {@link FreeCamera}'s own two-step exit
 *       instead of vanilla's cycle, and takes priority over the selfie-cam skip above.</li>
 * </ul>
 * Both live in one injector rather than two competing ones, since two {@code cancellable} HEAD
 * injections on the same method would otherwise race for which return value wins.
 */
@Mixin(CameraType.class)
public class CameraTypeMixin {

    @Inject(method = "cycle", at = @At("HEAD"), cancellable = true)
    private void swingme$cycleOverride(CallbackInfoReturnable<CameraType> cir) {
        if (FreeCamera.isActive()) {
            FreeCamera.unlockKeepingDistance();
            cir.setReturnValue(CameraType.THIRD_PERSON_BACK);
            return;
        }
        if (FreeCamera.isUnlocked()) {
            FreeCamera.clearUnlocked();
            cir.setReturnValue(CameraType.FIRST_PERSON);
            return;
        }

        if (!FeatureFlags.isEnabled(FeatureFlags.DISABLE_SELFIE)) return;

        CameraType current = (CameraType) (Object) this;
        if (current == CameraType.THIRD_PERSON_BACK) {
            cir.setReturnValue(CameraType.FIRST_PERSON);
        }
    }
}