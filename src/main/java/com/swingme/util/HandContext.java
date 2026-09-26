package com.swingme.util;


import net.minecraft.world.InteractionHand;

/** Holds the resolved transform for whichever hand is currently rendering. Only valid while renderDepth > 0. */
public final class HandContext {

    public static boolean activeTransform = false;
    public static InteractionHand currentHand = null;
    public static int renderDepth = 0;

    public static float translationX, translationY, translationZ;
    public static float rotationX, rotationY, rotationZ;
    public static float scale = 1f;

    private HandContext() {}

    /** Resolves the active values for the given hand. Off-hand uses its own sliders only when separately enabled. */
    public static void update(InteractionHand hand) {
        currentHand = hand;

        boolean useOffhandTransform = (hand == InteractionHand.OFF_HAND)
                && ActiveSettings.enableSeparateHandTransforms;

        if (useOffhandTransform) {
            translationX = ActiveSettings.itemTranslationXOffhand;
            translationY = ActiveSettings.itemTranslationYOffhand;
            translationZ = ActiveSettings.itemTranslationZOffhand;
            rotationX    = ActiveSettings.itemRotationXOffhand;
            rotationY    = ActiveSettings.itemRotationYOffhand;
            rotationZ    = ActiveSettings.itemRotationZOffhand;
            scale        = ActiveSettings.itemScaleOffhand;
        } else {
            translationX = ActiveSettings.itemTranslationX;
            translationY = ActiveSettings.itemTranslationY;
            translationZ = ActiveSettings.itemTranslationZ;
            rotationX    = ActiveSettings.itemRotationX;
            rotationY    = ActiveSettings.itemRotationY;
            rotationZ    = ActiveSettings.itemRotationZ;
            scale        = ActiveSettings.itemScale;
        }

        activeTransform = translationX != 0f || translationY != 0f || translationZ != 0f
                || rotationX != 0f || rotationY != 0f || rotationZ != 0f || scale != 1f;
    }
}