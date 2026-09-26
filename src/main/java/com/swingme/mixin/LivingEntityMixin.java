package com.swingme.mixin;

import com.swingme.util.ActiveSettings;
import com.swingme.util.FeatureFlags;
import com.swingme.util.SwingHoldState;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mixins into {@link LivingEntity} to support swing animation overrides. */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    /** Vanilla default swing duration in ticks. */
    @Unique
    private static final int swingme$DEFAULT_SWING_DURATION = 6;

    /** Progress where vanilla's downward chop reaches its maximum. */
    @Unique
    private static final float swingme$SWING_BOTTOM_PROGRESS = 0.25f;

    /**
     * Modifies the swing duration based on config speed multiplier,
     * optionally ignoring potion/enchantment effects.
     */
    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void swingme$modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (!ActiveSettings.isEnabled(FeatureFlags.SWING_DURATION)) return;

        boolean ignore = ActiveSettings.isEnabled(FeatureFlags.IGNORE_SWING_SPEED);
        float speed = ActiveSettings.swingAnimationSpeed;

        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null || self != mc.player) return;

        int duration = ignore ? swingme$DEFAULT_SWING_DURATION : cir.getReturnValue();
        if (speed != 1f) duration = Math.max(1, Math.round(duration / speed));

        cir.setReturnValue(duration);
    }

    /**
     * Leaves the first held-attack swing intact while preventing later calls from restarting it.
     * The outer {@code LocalPlayer.swing} call still sends its packet, so this is visual only.
     */
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void swingme$suppressRepeatedSwing(InteractionHand hand, boolean fromServer, CallbackInfo ci) {
        if (!SwingHoldState.isSuppressing()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) return;

        if (self == Minecraft.getInstance().player) ci.cancel();
    }

    /** Suppresses or holds the attack animation for the local player when configured. */
    @Inject(method = "getAttackAnim", at = @At("RETURN"), cancellable = true)
    private void swingme$suppressAttackAnim(float partialTick, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) return;

        var mc = Minecraft.getInstance();
        if (self != mc.player) return;

        if (ActiveSettings.isEnabled(FeatureFlags.DISABLE_SWING_ANIM)) {
            cir.setReturnValue(0f);
        } else if (ActiveSettings.isEnabled(FeatureFlags.HOLD_REPEAT_SWING_BOTTOM)
                && SwingHoldState.isSuppressing()) {
            // Let the first swing travel down once, then keep it at the downward peak until release.
            float swingProgress = cir.getReturnValue();
            if (SwingHoldState.latchBottomIfReached(swingProgress, swingme$SWING_BOTTOM_PROGRESS)) {
                cir.setReturnValue(swingme$SWING_BOTTOM_PROGRESS);
            }
        }
    }
}
