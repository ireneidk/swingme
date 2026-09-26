package com.swingme.gui;

import com.swingme.config.ItemOverride;
import com.swingme.config.SwingMeConfig;

import java.util.List;
import java.util.function.Predicate;

/**
 * The row tables for each tab. Ranges match the {@code @Entry} annotations in
 * {@code SwingMeConfig} and the visibility predicates mirror its {@code @Condition}s,
 * so the overlay behaves the same way the main config menu does.
 */
public final class Rows {

    private Rows() {}

    private static final Predicate<ItemOverride> ARM_ON = o -> o.enableArmPositionOverride;
    private static final Predicate<ItemOverride> TRANSFORM_ON = o -> o.enableItemTransformOverride;
    private static final Predicate<ItemOverride> ARM_AND_OFFHAND =
            o -> o.enableArmPositionOverride && o.enableSeparateHandTransforms;
    private static final Predicate<ItemOverride> TRANSFORM_AND_OFFHAND =
            o -> o.enableItemTransformOverride && o.enableSeparateHandTransforms;
    private static final Predicate<ItemOverride> SWING_ON = o -> !o.disableSwingAnimation;
    private static final Predicate<ItemOverride> SWING_SHAPE_ON =
            o -> !o.disableSwingAnimation && o.enableSwingOverride;

    public static final List<Row> POSITION = List.of(
            new Row.Toggle("enableArmPositionOverride",
                    o -> o.enableArmPositionOverride, (o, v) -> o.enableArmPositionOverride = v, null),
            new Row.Slider("armBaseX", -2f, 2f,
                    o -> o.armBaseX, (o, v) -> o.armBaseX = v, ARM_ON),
            new Row.Slider("armBaseY", -2f, 2f,
                    o -> o.armBaseY, (o, v) -> o.armBaseY = v, ARM_ON),
            new Row.Slider("armBaseZ", -2f, 2f,
                    o -> o.armBaseZ, (o, v) -> o.armBaseZ = v, ARM_ON),
            new Row.Slider("armHeightScale", -2f, 0f,
                    o -> o.armHeightScale, (o, v) -> o.armHeightScale = v, ARM_ON),

            new Row.Toggle("enableSeparateHandTransforms",
                    o -> o.enableSeparateHandTransforms, (o, v) -> o.enableSeparateHandTransforms = v, null),
            new Row.Slider("armBaseXOffhand", -2f, 2f,
                    o -> o.armBaseXOffhand, (o, v) -> o.armBaseXOffhand = v, ARM_AND_OFFHAND),
            new Row.Slider("armBaseYOffhand", -2f, 2f,
                    o -> o.armBaseYOffhand, (o, v) -> o.armBaseYOffhand = v, ARM_AND_OFFHAND),
            new Row.Slider("armBaseZOffhand", -2f, 2f,
                    o -> o.armBaseZOffhand, (o, v) -> o.armBaseZOffhand = v, ARM_AND_OFFHAND),
            new Row.Slider("armHeightScaleOffhand", -2f, 0f,
                    o -> o.armHeightScaleOffhand, (o, v) -> o.armHeightScaleOffhand = v, ARM_AND_OFFHAND)
    );

    public static final List<Row> ITEM = List.of(
            new Row.Toggle("enableItemTransformOverride",
                    o -> o.enableItemTransformOverride, (o, v) -> o.enableItemTransformOverride = v, null),
            new Row.Slider("itemScale", 0.1f, 3f,
                    o -> o.itemScale, (o, v) -> o.itemScale = v, TRANSFORM_ON),
            new Row.Slider("itemTranslationX", -1f, 1f,
                    o -> o.itemTranslationX, (o, v) -> o.itemTranslationX = v, TRANSFORM_ON),
            new Row.Slider("itemTranslationY", -1f, 1f,
                    o -> o.itemTranslationY, (o, v) -> o.itemTranslationY = v, TRANSFORM_ON),
            new Row.Slider("itemTranslationZ", -1f, 1f,
                    o -> o.itemTranslationZ, (o, v) -> o.itemTranslationZ = v, TRANSFORM_ON),
            new Row.Slider("itemRotationX", -180f, 180f,
                    o -> o.itemRotationX, (o, v) -> o.itemRotationX = v, TRANSFORM_ON),
            new Row.Slider("itemRotationY", -180f, 180f,
                    o -> o.itemRotationY, (o, v) -> o.itemRotationY = v, TRANSFORM_ON),
            new Row.Slider("itemRotationZ", -180f, 180f,
                    o -> o.itemRotationZ, (o, v) -> o.itemRotationZ = v, TRANSFORM_ON),

            new Row.Slider("itemScaleOffhand", 0.1f, 3f,
                    o -> o.itemScaleOffhand, (o, v) -> o.itemScaleOffhand = v, TRANSFORM_AND_OFFHAND),
            new Row.Slider("itemTranslationXOffhand", -1f, 1f,
                    o -> o.itemTranslationXOffhand, (o, v) -> o.itemTranslationXOffhand = v, TRANSFORM_AND_OFFHAND),
            new Row.Slider("itemTranslationYOffhand", -1f, 1f,
                    o -> o.itemTranslationYOffhand, (o, v) -> o.itemTranslationYOffhand = v, TRANSFORM_AND_OFFHAND),
            new Row.Slider("itemTranslationZOffhand", -1f, 1f,
                    o -> o.itemTranslationZOffhand, (o, v) -> o.itemTranslationZOffhand = v, TRANSFORM_AND_OFFHAND),
            new Row.Slider("itemRotationXOffhand", -180f, 180f,
                    o -> o.itemRotationXOffhand, (o, v) -> o.itemRotationXOffhand = v, TRANSFORM_AND_OFFHAND),
            new Row.Slider("itemRotationYOffhand", -180f, 180f,
                    o -> o.itemRotationYOffhand, (o, v) -> o.itemRotationYOffhand = v, TRANSFORM_AND_OFFHAND),
            new Row.Slider("itemRotationZOffhand", -180f, 180f,
                    o -> o.itemRotationZOffhand, (o, v) -> o.itemRotationZOffhand = v, TRANSFORM_AND_OFFHAND)
    );

    public static final List<Row> SWING = List.of(
            new Row.Toggle("disableSwingAnimation",
                    o -> o.disableSwingAnimation, (o, v) -> o.disableSwingAnimation = v, null),
            new Row.Toggle("suppressRepeatedSwingAnimation",
                    o -> o.suppressRepeatedSwingAnimation, (o, v) -> o.suppressRepeatedSwingAnimation = v, SWING_ON),
            new Row.Toggle("holdRepeatedSwingsAtBottom",
                    o -> o.holdRepeatedSwingsAtBottom, (o, v) -> o.holdRepeatedSwingsAtBottom = v,
                    o -> !o.disableSwingAnimation && o.suppressRepeatedSwingAnimation),
            new Row.Slider("swingAnimationSpeed", 0.1f, 2f,
                    o -> o.swingAnimationSpeed, (o, v) -> o.swingAnimationSpeed = v, SWING_ON),
            new Row.Toggle("ignoreSwingSpeedEffects",
                    o -> o.ignoreSwingSpeedEffects, (o, v) -> o.ignoreSwingSpeedEffects = v, SWING_ON),
            new Row.Toggle("disableSwingBobbing",
                    o -> o.disableSwingBobbing, (o, v) -> o.disableSwingBobbing = v, SWING_ON),

            new Row.Toggle("enableSwingOverride",
                    o -> o.enableSwingOverride, (o, v) -> o.enableSwingOverride = v, SWING_ON),
            new Row.Slider("swingArcXAmount", -180f, 180f,
                    o -> o.swingArcXAmount, (o, v) -> o.swingArcXAmount = v, SWING_SHAPE_ON),
            new Row.Slider("swingArcYAmount", -180f, 180f,
                    o -> o.swingArcYAmount, (o, v) -> o.swingArcYAmount = v, SWING_SHAPE_ON),
            new Row.Slider("swingArcZAmount", -180f, 180f,
                    o -> o.swingArcZAmount, (o, v) -> o.swingArcZAmount = v, SWING_SHAPE_ON),
            new Row.Slider("swingPreRotationY", 0f, 90f,
                    o -> o.swingPreRotationY, (o, v) -> o.swingPreRotationY = v, SWING_SHAPE_ON),
            new Row.Toggle("swingCounterRotation",
                    o -> o.swingCounterRotation, (o, v) -> o.swingCounterRotation = v, SWING_SHAPE_ON),
            new Row.Slider("swingArmXScale", -2f, 2f,
                    o -> o.swingArmXScale, (o, v) -> o.swingArmXScale = v, SWING_SHAPE_ON),
            new Row.Slider("swingArmYScale", -2f, 2f,
                    o -> o.swingArmYScale, (o, v) -> o.swingArmYScale = v, SWING_SHAPE_ON),
            new Row.Slider("swingArmZScale", -2f, 2f,
                    o -> o.swingArmZScale, (o, v) -> o.swingArmZScale = v, SWING_SHAPE_ON),
            new Row.Toggle("swingArmXMultiplyBySide",
                    o -> o.swingArmXMultiplyBySide, (o, v) -> o.swingArmXMultiplyBySide = v, SWING_SHAPE_ON),

            new Row.Toggle("enableSwordBlock",
                    o -> o.enableSwordBlock, (o, v) -> o.enableSwordBlock = v, null)
    );

    // -- Global-only tables --------------------------------------------------
    // Sizes and camera settings have no per-item meaning, so these rows read and write
    // SwingMeConfig directly and ignore the ItemOverride argument. That also means their
    // getters cannot supply a default, hence the explicit default on every row.

    public static final List<Row> SCALE = List.of(
            new Row.Slider("playerScale", 0.1f, 4f,
                    o -> SwingMeConfig.playerScale, (o, v) -> SwingMeConfig.playerScale = v, null, 1f),
            new Row.Slider("otherPlayersScale", 0.1f, 4f,
                    o -> SwingMeConfig.otherPlayersScale, (o, v) -> SwingMeConfig.otherPlayersScale = v, null, 1f),
            new Row.Slider("hypixelNpcScale", 0.1f, 4f,
                    o -> SwingMeConfig.hypixelNpcScale, (o, v) -> SwingMeConfig.hypixelNpcScale = v, null, 1f),
            new Row.Toggle("scaleNameTags",
                    o -> SwingMeConfig.scaleNameTags, (o, v) -> SwingMeConfig.scaleNameTags = v, null, false),
            new Row.Slider("groundItemScale", 0.1f, 4f,
                    o -> SwingMeConfig.groundItemScale, (o, v) -> SwingMeConfig.groundItemScale = v, null, 1f)
    );

    public static final List<Row> VIEW = List.of(
            new Row.Toggle("enableCrosshairInThirdPerson",
                    o -> SwingMeConfig.enableCrosshairInThirdPerson,
                    (o, v) -> SwingMeConfig.enableCrosshairInThirdPerson = v, null, false),
            new Row.Toggle("enableCrosshairInThirdPersonFront",
                    o -> SwingMeConfig.enableCrosshairInThirdPersonFront,
                    (o, v) -> SwingMeConfig.enableCrosshairInThirdPersonFront = v, null, false),
            new Row.Toggle("disableSelfieCam",
                    o -> SwingMeConfig.disableSelfieCam, (o, v) -> SwingMeConfig.disableSelfieCam = v, null, false),
            new Row.Toggle("showOwnNametagInThirdPerson",
                    o -> SwingMeConfig.showOwnNametagInThirdPerson,
                    (o, v) -> SwingMeConfig.showOwnNametagInThirdPerson = v, null, false),
            new Row.Toggle("hidePlayers",
                    o -> SwingMeConfig.hidePlayers, (o, v) -> SwingMeConfig.hidePlayers = v, null, false),
            new Row.Toggle("hidePlayersOnlyOnSkyblock",
                    o -> SwingMeConfig.hidePlayersOnlyOnSkyblock,
                    (o, v) -> SwingMeConfig.hidePlayersOnlyOnSkyblock = v,
                    o -> SwingMeConfig.hidePlayers, false),
            new Row.Toggle("disableCameraSway",
                    o -> SwingMeConfig.disableCameraSway,
                    (o, v) -> SwingMeConfig.disableCameraSway = v, null, false)
    );
}
