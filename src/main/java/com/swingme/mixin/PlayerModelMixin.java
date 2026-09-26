package com.swingme.mixin;

import com.swingme.util.PerTickCache;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Points your own head at what you are actually aiming at once the model is scaled down.
 * <p>
 * Player scaling is render-only, so the aim ray still leaves your real eye at full height while
 * the model shrinks about its feet. A half-size player's head renders roughly 0.8 blocks below
 * the ray it is aiming along, and pointing the head straight down that ray then reads as looking
 * past the target rather than at it. Pitching the head to face the aim point instead closes that
 * gap, and costs nothing at full size because the correction collapses to zero.
 * <p>
 * Local player only, and only in third person: nobody else sees this, since it is applied after
 * the rotation the server knows about.
 */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {

    /**
     * How far along the aim ray the head is treated as looking. Fixed rather than read from the
     * real hit result, whose distance jumps the instant the ray slips off a block edge and would
     * snap the head with it.
     */
    @Unique
    private static final float swingme$FOCUS_DISTANCE = 4.5f;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("TAIL")
    )
    private void swingme$aimHeadAtLookTarget(AvatarRenderState state, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || state.id != player.getId()) return;
        if (client.options.getCameraType().isFirstPerson()) return;

        float scale = PerTickCache.getScale(state.id);
        if (scale == 1f) return;

        float drop = player.getEyeHeight() * (1f - scale);

        ModelPart head = ((HumanoidModel<?>) (Object) this).head;
        float pitch = head.xRot;
        head.xRot = (float) Math.atan2(
                Mth.sin(pitch) * swingme$FOCUS_DISTANCE - drop,
                Mth.cos(pitch) * swingme$FOCUS_DISTANCE);
    }
}
