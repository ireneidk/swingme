package com.swingme.mixin;

import com.swingme.util.CharacterScale;
import com.swingme.util.FeatureFlags;
import com.swingme.util.PerTickCache;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {

    /**
     * Handoff between the HEAD and TAIL hooks on {@code scale}. Plain statics are safe here:
     * {@code scale()} is a leaf on the render thread — it cannot be re-entered between the two
     * hooks — and the id is carried alongside so a mismatch degrades to "no external scale"
     * rather than to a wrong number.
     */
    @Unique private static int swingme$measuredId = -1;
    @Unique private static float swingme$m11AtHead = 1f;

    @Inject(
            method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void swingme$showOwnNametagInThirdPerson(Avatar entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (!FeatureFlags.isEnabled(FeatureFlags.SHOW_OWN_NAMETAG)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Only your own nametag
        if (entity != mc.player) return;

        // Only in third person
        CameraType cam = mc.options.getCameraType();
        if (cam.isFirstPerson()) return;

        cir.setReturnValue(true);
    }

    /**
     * Opens the measurement window. Anything another mod does to the pose from here until our
     * TAIL hook — including its own HEAD injection on this same method — shows up in the ratio
     * {@code m11(tail) / m11(head)}.
     */
    @Inject(method = "scale*", at = @At("HEAD"))
    private void swingme$measureExternalScale(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        if (!FeatureFlags.isEnabled(FeatureFlags.SCALE_ANY)) return;

        swingme$measuredId = state.id;
        swingme$m11AtHead = poseStack.last().pose().m11();
    }

    @Inject(method = "scale*", at = @At("TAIL"))
    private void swingme$applyScale(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        if (!FeatureFlags.isEnabled(FeatureFlags.SCALE_ANY)) return;

        float scale = PerTickCache.getScale(state.id);

        // Close the measurement window *before* adding our own factor, so `external` is
        // purely what other mods contributed. The ratio is sign-safe: the avatar pose is
        // already Y-flipped at this point, and head/tail share that flip.
        float external = 1f;
        if (swingme$measuredId == state.id) {
            float head = swingme$m11AtHead;
            if (head != 0f) {
                float ratio = poseStack.last().pose().m11() / head;
                if (ratio != 0f && Float.isFinite(ratio)) external = ratio;
            }
        }
        swingme$measuredId = -1;

        CharacterScale.put(state.id, external * scale);

        if (scale == 1f) return;

        poseStack.scale(scale, scale, scale);
    }

}
