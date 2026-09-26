package com.swingme.mixin;

import com.swingme.util.FeatureFlags;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Skips THIRD_PERSON_FRONT when selfie cam is disabled — normally
 * FIRST_PERSON &rarr; THIRD_PERSON_BACK &rarr; THIRD_PERSON_FRONT &rarr; FIRST_PERSON,
 * this makes it FIRST_PERSON &rarr; THIRD_PERSON_BACK &rarr; FIRST_PERSON.
 */
@Mixin(CameraType.class)
public class CameraTypeMixin {

    @Inject(method = "cycle", at = @At("HEAD"), cancellable = true)
    private void swingme$cycleOverride(CallbackInfoReturnable<CameraType> cir) {
        if (!FeatureFlags.isEnabled(FeatureFlags.DISABLE_SELFIE)) return;

        CameraType current = (CameraType) (Object) this;
        if (current == CameraType.THIRD_PERSON_BACK) {
            cir.setReturnValue(CameraType.FIRST_PERSON);
        }
    }
}