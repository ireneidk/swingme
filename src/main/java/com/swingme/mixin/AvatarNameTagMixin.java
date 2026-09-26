package com.swingme.mixin;

import com.swingme.util.CharacterScale;
import com.swingme.util.FeatureFlags;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scales the nametag quad itself so it grows and shrinks with the body it belongs to,
 * using the <em>total</em> scale from {@link CharacterScale} — this mod's factor times
 * whatever another mod applied to the same pose.
 * <p>
 * <b>Why {@code AvatarRenderer} and not {@code EntityRenderer}:</b> on every supported
 * version {@code AvatarRenderer} overrides the nametag method and jumps straight to
 * {@code EntityRenderer}'s five-argument overload rather than its four-argument body. An
 * injection on {@code EntityRenderer}'s four-argument method would therefore never run for a
 * player. This override is the one place that is on the player path on both supported
 * versions, and it carries an identical signature on each.
 */
@Mixin(AvatarRenderer.class)
public class AvatarNameTagMixin {

    /**
     * Uses a MixinExtras {@code @Share} rather than a mixin field or a thread-local depth
     * counter. A shared ref is compiled into a local variable of the target method, so every
     * invocation gets its own copy: re-entrancy and cross-thread rendering are handled for
     * free, and — unlike a depth counter — an exception escaping between HEAD and RETURN
     * cannot leave a stale count behind that permanently disables the feature.
     */
    @Inject(method = "submitNameDisplay", at = @At("HEAD"))
    private void swingme$pushNameTagScale(
            AvatarRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci,
            @Share("swingme$nameTagPushed") LocalBooleanRef pushed
    ) {
        // The ref starts out false, so an early return here leaves nothing for RETURN to pop.
        if (!FeatureFlags.isEnabled(FeatureFlags.SCALE_ANY)) return;
        if (!FeatureFlags.isEnabled(FeatureFlags.SCALE_NAME_TAGS)) return;

        float scale = CharacterScale.get(state.id);
        if (scale == 1f) return;

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        pushed.set(true);
    }

    /**
     * {@code RETURN}, not {@code TAIL}: TAIL only matches the final return instruction, so an
     * early return inside the method would skip the pop and leave the pose stack unbalanced,
     * corrupting everything rendered afterwards.
     */
    @Inject(method = "submitNameDisplay", at = @At("RETURN"))
    private void swingme$popNameTagScale(
            AvatarRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo ci,
            @Share("swingme$nameTagPushed") LocalBooleanRef pushed
    ) {
        if (!pushed.get()) return;

        poseStack.popPose();
    }
}
