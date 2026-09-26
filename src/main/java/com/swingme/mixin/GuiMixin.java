package com.swingme.mixin;

import com.swingme.util.FeatureFlags;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public class GuiMixin {

    @WrapOperation(
            method = "extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z")
    )
    private boolean swingme$shouldRenderCrosshair(CameraType cameraType, Operation<Boolean> original) {
        boolean isFirstPerson = original.call(cameraType);

        if (isFirstPerson) return true;

        Minecraft mc = Minecraft.getInstance();
        CameraType currentCameraType = mc.options.getCameraType();

        if (currentCameraType == CameraType.THIRD_PERSON_FRONT) {
            return FeatureFlags.isEnabled(FeatureFlags.CROSSHAIR_3RD_FRONT);
        }
        return FeatureFlags.isEnabled(FeatureFlags.CROSSHAIR_3RD);
    }
}