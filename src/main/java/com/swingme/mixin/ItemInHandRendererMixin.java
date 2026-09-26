package com.swingme.mixin;

import com.swingme.util.ActiveSettings;
import com.swingme.util.BlockingState;
import com.swingme.util.FeatureFlags;
import com.swingme.util.HandContext;
import com.swingme.util.SwordBlockPose;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import org.joml.Quaternionfc;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks into {@link ItemInHandRenderer} for swing and hand-context overrides. */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Shadow private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight) {}
    @Shadow private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float attackProgress) {}

    // ── Swing Bobbing ───────────────────────────────────────────────────────

    /**
     * Forces attack-strength scale to 1.0 so the item never dips on attack.
     * Uses {@link WrapOperation} for clean parameter access.
     */
    @WrapOperation(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"
            )
    )
    private float swingme$suppressSwingBobbing(LocalPlayer player, float partialTick, Operation<Float> original) {
        if (!ActiveSettings.isEnabled(FeatureFlags.DISABLE_SWING_BOB)) {
            return original.call(player, partialTick);
        }
        return 1.0f;
    }

    // ── Camera Sway ─────────────────────────────────────────────────────────

    /**
     * Drops the two rotations that tilt the hand against the direction the camera is turning.
     * <p>
     * They are the only {@code mulPose} calls in this method on either target, so wrapping the
     * call site needs no ordinal and stays correct if their order ever swaps.
     */
    @WrapOperation(
            method = "renderHandsWithItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"
            )
    )
    private void swingme$suppressCameraSway(PoseStack poseStack, Quaternionfc rotation, Operation<Void> original) {
        if (FeatureFlags.isEnabled(FeatureFlags.DISABLE_CAMERA_SWAY)) return;
        original.call(poseStack, rotation);
    }

    // ── Hand Context ────────────────────────────────────────────────────────

    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void swingme$captureHand(
            AbstractClientPlayer player,
            float tickDelta, float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack heldItem,
            float equipProgress,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            CallbackInfo ci) {

        HandContext.renderDepth++;
        ActiveSettings.selectHand(hand);


        if (ActiveSettings.isEnabled(FeatureFlags.ITEM_TRANSFORM)) {
            HandContext.update(hand);
        } else {
            HandContext.currentHand = hand;
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void swingme$releaseHand(
            AbstractClientPlayer player,
            float tickDelta, float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack heldItem,
            float equipProgress,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            CallbackInfo ci) {

        swingme$leaveHand();
    }

    /**
     * Closes one {@code renderArmWithItem} entry: decrements the render depth and, once the
     * outermost call has returned, restores {@link ActiveSettings} to the main-hand item.
     * <p>
     * Shared by the normal RETURN injector and the sword-block injector below, which cancels
     * at HEAD and so never reaches RETURN — without this in one place, the two paths could
     * drift apart and leave {@link HandContext} pointed at the wrong hand.
     */
    @Unique
    private void swingme$leaveHand() {
        HandContext.renderDepth--;
        if (HandContext.renderDepth <= 0) {
            HandContext.renderDepth = 0;
            HandContext.currentHand = null;
            // Put the settings back on the main-hand item. Without this the off-hand's
            // selection leaks out of the render path, and getAttackAnim — which
            // renderHandsWithItems calls before each renderArmWithItem — would read the
            // wrong item's swing settings.
            ActiveSettings.selectMainHand();
        }
    }

    // ── Swing Drift Override ────────────────────────────────────────────────

    /**
     * Replaces vanilla's fixed swing-drift translation with per-axis configurable amounts.
     *
     * <p>Vanilla defaults: X = -0.4, Y = 0.2, Z = -0.2
     */
    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void swingme$overrideSwingDrift(float attackProgress, PoseStack poseStack, int handSide, HumanoidArm arm, CallbackInfo ci) {
        if (!ActiveSettings.isEnabled(FeatureFlags.SWING_OVERRIDE)) return;
        ci.cancel();

        float sqrtAttack = Mth.sqrt(attackProgress);

        float driftX = ActiveSettings.swingArmXScale * Mth.sin(sqrtAttack * Mth.PI);
        float driftY = ActiveSettings.swingArmYScale * Mth.sin(sqrtAttack * (Mth.PI * 2));
        float driftZ = ActiveSettings.swingArmZScale * Mth.sin(attackProgress * Mth.PI);

        poseStack.translate(ActiveSettings.swingArmXMultiplyBySide ? handSide * driftX : driftX, driftY, driftZ);

        applyItemArmAttackTransform(poseStack, arm, attackProgress);
    }

    // ── Swing Arc Override ──────────────────────────────────────────────────

    /** Replaces vanilla's fixed swing-arc rotations with per-axis configurable amounts.
     *  Vanilla defaults: preRotation Y = 45°, arc Y = -20°, Z = -20°, X chop = -80° */
    @Inject(method = "applyItemArmAttackTransform", at = @At("HEAD"), cancellable = true)
    private void swingme$overrideSwingArc(PoseStack poseStack, HumanoidArm arm, float attackProgress, CallbackInfo ci) {
        if (!ActiveSettings.isEnabled(FeatureFlags.SWING_OVERRIDE)) return;
        ci.cancel();

        int armSideSign = (arm == HumanoidArm.RIGHT) ? 1 : -1;

        float lateSwingCurve = Mth.sin(attackProgress * attackProgress * Mth.PI);  // peaks late, drives Y arc
        float midSwingCurve  = Mth.sin(Mth.sqrt(attackProgress) * Mth.PI);         // peaks mid, drives Z tilt + X chop

        poseStack.mulPose(Axis.YP.rotationDegrees(armSideSign * (ActiveSettings.swingPreRotationY + lateSwingCurve * ActiveSettings.swingArcYAmount)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(armSideSign * midSwingCurve * ActiveSettings.swingArcZAmount));
        poseStack.mulPose(Axis.XP.rotationDegrees(midSwingCurve * ActiveSettings.swingArcXAmount));

        // Undoes the Y pre-rotation so the item swings back to center (disable for 1.8-style swing)
        if (ActiveSettings.swingCounterRotation) {
            poseStack.mulPose(Axis.YP.rotationDegrees(armSideSign * -ActiveSettings.swingPreRotationY));
        }
    }

    /** Replaces the vanilla arm anchor position and height-bob scale with configurable values.
     *  Vanilla: translate(invert * 0.56, -0.52 + inverseArmHeight * -0.6, -0.72) */
    @Inject(method = "applyItemArmTransform", at = @At("HEAD"), cancellable = true)
    private void swingme$overrideArmBasePosition(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight, CallbackInfo ci) {
        if (!ActiveSettings.isEnabled(FeatureFlags.ARM_POSITION)) return;
        ci.cancel();

        int invert = (arm == HumanoidArm.RIGHT) ? 1 : -1;

        boolean useOffhand = (HandContext.currentHand == InteractionHand.OFF_HAND)
                && ActiveSettings.isEnabled(FeatureFlags.SEPARATE_OFFHAND);

        float baseX = useOffhand ? ActiveSettings.armBaseXOffhand : ActiveSettings.armBaseX;
        float baseY = useOffhand ? ActiveSettings.armBaseYOffhand : ActiveSettings.armBaseY;
        float baseZ = useOffhand ? ActiveSettings.armBaseZOffhand : ActiveSettings.armBaseZ;
        float heightScale = useOffhand ? ActiveSettings.armHeightScaleOffhand : ActiveSettings.armHeightScale;

        poseStack.translate(invert * baseX, baseY + inverseArmHeight * heightScale, baseZ);
    }

    /** Applies a blocking pose when holding a sword and right-clicking. */
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void swingme$applySwordBlockPose(
            AbstractClientPlayer player, float tickDelta, float pitch,
            InteractionHand hand, float swingProgress, ItemStack heldItem,
            float equipProgress, PoseStack poseStack,
            SubmitNodeCollector collector, int packedLight,
            CallbackInfo ci) {

        if (!BlockingState.isBlocking
                || hand != InteractionHand.MAIN_HAND
                || !heldItem.is(ItemTags.SWORDS)) return;

        ci.cancel();

        poseStack.pushPose();
        HumanoidArm arm = player.getMainArm();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;

        applyItemArmTransform(poseStack, arm, equipProgress);
        poseStack.translate(side * SwordBlockPose.TRANSLATE_X, SwordBlockPose.TRANSLATE_Y, SwordBlockPose.TRANSLATE_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(SwordBlockPose.ROTATE_X));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * SwordBlockPose.ROTATE_Y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * SwordBlockPose.ROTATE_Z));

        ((ItemInHandRenderer) (Object) this).renderItem(
                player,
                heldItem,
                arm == HumanoidArm.RIGHT
                        ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                poseStack,
                collector,
                packedLight
        );
        poseStack.popPose();

        // Cancelling at HEAD means the RETURN injector never runs, so the same restore
        // swingme$releaseHand normally performs has to happen here too.
        swingme$leaveHand();
    }
}
