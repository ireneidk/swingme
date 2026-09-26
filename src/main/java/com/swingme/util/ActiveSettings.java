package com.swingme.util;

import com.swingme.config.ItemOverride;
import com.swingme.config.SwingMeConfig;
import net.minecraft.world.InteractionHand;

/**
 * The held-item and swing settings actually in effect right now — the current item's
 * override when it has one, otherwise the global {@link SwingMeConfig} values.
 * <p>
 * Selected per hand in the render path and per tick elsewhere. Mixins read these fields
 * and {@link #isEnabled(int)} instead of the config, keeping the "mixins never read
 * config directly" rule intact. Bit constants are {@link FeatureFlags}'s, reused as-is.
 */
public final class ActiveSettings {

    public static boolean enableArmPositionOverride;
    public static float armBaseX, armBaseY, armBaseZ, armHeightScale;

    public static boolean enableItemTransformOverride;
    public static float itemScale, itemTranslationX, itemTranslationY, itemTranslationZ;
    public static float itemRotationX, itemRotationY, itemRotationZ;

    public static boolean enableSeparateHandTransforms;
    public static float armBaseXOffhand, armBaseYOffhand, armBaseZOffhand, armHeightScaleOffhand;
    public static float itemScaleOffhand;
    public static float itemTranslationXOffhand, itemTranslationYOffhand, itemTranslationZOffhand;
    public static float itemRotationXOffhand, itemRotationYOffhand, itemRotationZOffhand;

    public static boolean disableSwingAnimation, suppressRepeatedSwingAnimation, holdRepeatedSwingsAtBottom;
    public static float swingAnimationSpeed;
    public static boolean ignoreSwingSpeedEffects, disableSwingBobbing;

    public static boolean enableSwingOverride;
    public static float swingArcXAmount, swingArcYAmount, swingArcZAmount, swingPreRotationY;
    public static boolean swingCounterRotation;
    public static float swingArmXScale, swingArmYScale, swingArmZScale;
    public static boolean swingArmXMultiplyBySide;

    public static boolean enableSwordBlock;

    private static int flags = 0;

    private ActiveSettings() {}

    /** Loads the settings for the item in {@code hand}. */
    public static void selectHand(InteractionHand hand) {
        apply(ItemOverrideResolver.forHand(hand));
    }

    /** Loads the settings for the main-hand item. Used outside the render path. */
    public static void selectMainHand() {
        apply(ItemOverrideResolver.mainHand());
    }

    /** True when every bit in {@code mask} is set. */
    public static boolean isEnabled(int mask) {
        return (flags & mask) == mask;
    }

    /** @param o the current item's override, or null to fall back to the global config. */
    private static void apply(ItemOverride o) {
        if (o == null) {
            copyGlobal();
        } else {
            copyOverride(o);
        }
        recomputeFlags();
    }

    private static void copyGlobal() {
        enableArmPositionOverride = SwingMeConfig.enableArmPositionOverride;
        armBaseX = SwingMeConfig.armBaseX;
        armBaseY = SwingMeConfig.armBaseY;
        armBaseZ = SwingMeConfig.armBaseZ;
        armHeightScale = SwingMeConfig.armHeightScale;

        enableItemTransformOverride = SwingMeConfig.enableItemTransformOverride;
        itemScale = SwingMeConfig.itemScale;
        itemTranslationX = SwingMeConfig.itemTranslationX;
        itemTranslationY = SwingMeConfig.itemTranslationY;
        itemTranslationZ = SwingMeConfig.itemTranslationZ;
        itemRotationX = SwingMeConfig.itemRotationX;
        itemRotationY = SwingMeConfig.itemRotationY;
        itemRotationZ = SwingMeConfig.itemRotationZ;

        enableSeparateHandTransforms = SwingMeConfig.enableSeparateHandTransforms;
        armBaseXOffhand = SwingMeConfig.armBaseXOffhand;
        armBaseYOffhand = SwingMeConfig.armBaseYOffhand;
        armBaseZOffhand = SwingMeConfig.armBaseZOffhand;
        armHeightScaleOffhand = SwingMeConfig.armHeightScaleOffhand;
        itemScaleOffhand = SwingMeConfig.itemScaleOffhand;
        itemTranslationXOffhand = SwingMeConfig.itemTranslationXOffhand;
        itemTranslationYOffhand = SwingMeConfig.itemTranslationYOffhand;
        itemTranslationZOffhand = SwingMeConfig.itemTranslationZOffhand;
        itemRotationXOffhand = SwingMeConfig.itemRotationXOffhand;
        itemRotationYOffhand = SwingMeConfig.itemRotationYOffhand;
        itemRotationZOffhand = SwingMeConfig.itemRotationZOffhand;

        disableSwingAnimation = SwingMeConfig.disableSwingAnimation;
        suppressRepeatedSwingAnimation = SwingMeConfig.suppressRepeatedSwingAnimation;
        holdRepeatedSwingsAtBottom = SwingMeConfig.holdRepeatedSwingsAtBottom;
        swingAnimationSpeed = SwingMeConfig.swingAnimationSpeed;
        ignoreSwingSpeedEffects = SwingMeConfig.ignoreSwingSpeedEffects;
        disableSwingBobbing = SwingMeConfig.disableSwingBobbing;

        enableSwingOverride = SwingMeConfig.enableSwingOverride;
        swingArcXAmount = SwingMeConfig.swingArcXAmount;
        swingArcYAmount = SwingMeConfig.swingArcYAmount;
        swingArcZAmount = SwingMeConfig.swingArcZAmount;
        swingPreRotationY = SwingMeConfig.swingPreRotationY;
        swingCounterRotation = SwingMeConfig.swingCounterRotation;
        swingArmXScale = SwingMeConfig.swingArmXScale;
        swingArmYScale = SwingMeConfig.swingArmYScale;
        swingArmZScale = SwingMeConfig.swingArmZScale;
        swingArmXMultiplyBySide = SwingMeConfig.swingArmXMultiplyBySide;

        enableSwordBlock = SwingMeConfig.enableSwordBlock;
    }

    private static void copyOverride(ItemOverride o) {
        enableArmPositionOverride = o.enableArmPositionOverride;
        armBaseX = o.armBaseX;
        armBaseY = o.armBaseY;
        armBaseZ = o.armBaseZ;
        armHeightScale = o.armHeightScale;

        enableItemTransformOverride = o.enableItemTransformOverride;
        itemScale = o.itemScale;
        itemTranslationX = o.itemTranslationX;
        itemTranslationY = o.itemTranslationY;
        itemTranslationZ = o.itemTranslationZ;
        itemRotationX = o.itemRotationX;
        itemRotationY = o.itemRotationY;
        itemRotationZ = o.itemRotationZ;

        enableSeparateHandTransforms = o.enableSeparateHandTransforms;
        armBaseXOffhand = o.armBaseXOffhand;
        armBaseYOffhand = o.armBaseYOffhand;
        armBaseZOffhand = o.armBaseZOffhand;
        armHeightScaleOffhand = o.armHeightScaleOffhand;
        itemScaleOffhand = o.itemScaleOffhand;
        itemTranslationXOffhand = o.itemTranslationXOffhand;
        itemTranslationYOffhand = o.itemTranslationYOffhand;
        itemTranslationZOffhand = o.itemTranslationZOffhand;
        itemRotationXOffhand = o.itemRotationXOffhand;
        itemRotationYOffhand = o.itemRotationYOffhand;
        itemRotationZOffhand = o.itemRotationZOffhand;

        disableSwingAnimation = o.disableSwingAnimation;
        suppressRepeatedSwingAnimation = o.suppressRepeatedSwingAnimation;
        holdRepeatedSwingsAtBottom = o.holdRepeatedSwingsAtBottom;
        swingAnimationSpeed = o.swingAnimationSpeed;
        ignoreSwingSpeedEffects = o.ignoreSwingSpeedEffects;
        disableSwingBobbing = o.disableSwingBobbing;

        enableSwingOverride = o.enableSwingOverride;
        swingArcXAmount = o.swingArcXAmount;
        swingArcYAmount = o.swingArcYAmount;
        swingArcZAmount = o.swingArcZAmount;
        swingPreRotationY = o.swingPreRotationY;
        swingCounterRotation = o.swingCounterRotation;
        swingArmXScale = o.swingArmXScale;
        swingArmYScale = o.swingArmYScale;
        swingArmZScale = o.swingArmZScale;
        swingArmXMultiplyBySide = o.swingArmXMultiplyBySide;

        enableSwordBlock = o.enableSwordBlock;
    }

    /** Mirrors the held-item and swing half of {@link FeatureFlags#update()}. */
    private static void recomputeFlags() {
        int f = 0;
        if (enableArmPositionOverride)      f |= FeatureFlags.ARM_POSITION;
        if (enableItemTransformOverride)    f |= FeatureFlags.ITEM_TRANSFORM;
        if (enableSeparateHandTransforms)   f |= FeatureFlags.SEPARATE_OFFHAND;

        if (disableSwingBobbing)            f |= FeatureFlags.DISABLE_SWING_BOB;
        if (ignoreSwingSpeedEffects)        f |= FeatureFlags.IGNORE_SWING_SPEED;
        if (enableSwingOverride)            f |= FeatureFlags.SWING_OVERRIDE;
        if (disableSwingAnimation)          f |= FeatureFlags.DISABLE_SWING_ANIM;
        if (suppressRepeatedSwingAnimation) f |= FeatureFlags.SUPPRESS_REPEAT_SWING;
        if (holdRepeatedSwingsAtBottom)     f |= FeatureFlags.HOLD_REPEAT_SWING_BOTTOM;

        if (ignoreSwingSpeedEffects || swingAnimationSpeed != 1f) f |= FeatureFlags.SWING_DURATION;

        if (enableSwordBlock)               f |= FeatureFlags.SWORD_BLOCK;
        flags = f;
    }
}
