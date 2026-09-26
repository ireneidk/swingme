package com.swingme.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * A complete snapshot of every held-item and swing setting for one specific item.
 * <p>
 * Snapshots are full, never partial: when an override exists, every one of these values
 * is used and none are inherited from {@link SwingMeConfig}. That removes any ambiguity
 * about which fields count as "set".
 * <p>
 * Every field except {@link #displayName} must have a {@link SwingMeConfig} field of the same
 * name and type. That pairing is what lets the overlay reuse the existing
 * {@code swingme.midnightconfig.<field>} language keys for its labels, and it is what the
 * conversions below walk instead of restating all 42 settings twice; it is checked at class
 * load, so adding a setting to one side and not the other fails at startup rather than
 * silently dropping the value.
 */
public class ItemOverride {

    /** Captured when the item is registered, so the list can label an item you are not holding. */
    public String displayName = "";

    // -- Hand: arm position -------------------------------------------------
    public boolean enableArmPositionOverride = false;
    public float armBaseX = 0.56f;
    public float armBaseY = -0.52f;
    public float armBaseZ = -0.72f;
    public float armHeightScale = -0.6f;

    // -- Hand: item transform -----------------------------------------------
    public boolean enableItemTransformOverride = false;
    public float itemScale = 1f;
    public float itemTranslationX = 0f;
    public float itemTranslationY = 0f;
    public float itemTranslationZ = 0f;
    public float itemRotationX = 0f;
    public float itemRotationY = 0f;
    public float itemRotationZ = 0f;

    // -- Hand: off-hand -----------------------------------------------------
    public boolean enableSeparateHandTransforms = false;
    public float armBaseXOffhand = 0.56f;
    public float armBaseYOffhand = -0.52f;
    public float armBaseZOffhand = -0.72f;
    public float armHeightScaleOffhand = -0.6f;
    public float itemScaleOffhand = 1f;
    public float itemTranslationXOffhand = 0f;
    public float itemTranslationYOffhand = 0f;
    public float itemTranslationZOffhand = 0f;
    public float itemRotationXOffhand = 0f;
    public float itemRotationYOffhand = 0f;
    public float itemRotationZOffhand = 0f;

    // -- Swing: behaviour ---------------------------------------------------
    public boolean disableSwingAnimation = false;
    public boolean suppressRepeatedSwingAnimation = false;
    public boolean holdRepeatedSwingsAtBottom = false;
    public float swingAnimationSpeed = 1f;
    public boolean ignoreSwingSpeedEffects = false;
    public boolean disableSwingBobbing = false;

    // -- Swing: shape -------------------------------------------------------
    public boolean enableSwingOverride = false;
    public float swingArcXAmount = -80f;
    public float swingArcYAmount = -20f;
    public float swingArcZAmount = -20f;
    public float swingPreRotationY = 45f;
    public boolean swingCounterRotation = true;
    public float swingArmXScale = -0.4f;
    public float swingArmYScale = 0.2f;
    public float swingArmZScale = -0.2f;
    public boolean swingArmXMultiplyBySide = true;

    // -- Sword block --------------------------------------------------------
    public boolean enableSwordBlock = false;

    /** Settings fields, paired index-for-index with their {@link SwingMeConfig} twins. */
    private static final Field[] SNAPSHOT;
    private static final Field[] GLOBAL;

    static {
        List<Field> snapshot = new ArrayList<>();
        List<Field> global = new ArrayList<>();
        for (Field field : ItemOverride.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getName().equals("displayName")) continue;

            Class<?> type = field.getType();
            if (type != float.class && type != boolean.class) {
                throw new IllegalStateException(
                        "ItemOverride." + field.getName() + " must be a float or boolean setting, or be "
                                + "excluded here the way displayName is");
            }
            try {
                Field twin = SwingMeConfig.class.getDeclaredField(field.getName());
                if (twin.getType() != type) throw new NoSuchFieldException();
                snapshot.add(field);
                global.add(twin);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(
                        "ItemOverride." + field.getName() + " has no SwingMeConfig field of the same name "
                                + "and type", e);
            }
        }
        SNAPSHOT = snapshot.toArray(new Field[0]);
        GLOBAL = global.toArray(new Field[0]);
    }

    /** A snapshot of the current global settings, used as the starting point for a new item. */
    public static ItemOverride fromGlobal(String displayName) {
        ItemOverride o = new ItemOverride();
        o.displayName = displayName;
        o.loadFromGlobal();
        return o;
    }

    /** Replaces every setting in this snapshot with the current global value. */
    public void loadFromGlobal() {
        for (int i = 0; i < SNAPSHOT.length; i++) {
            copy(GLOBAL[i], null, SNAPSHOT[i], this);
        }
    }

    /**
     * The inverse of {@link #loadFromGlobal}, used by the overlay's "All Items" scope.
     * {@code displayName} is not a config field and is left alone.
     */
    public void applyToGlobal() {
        for (int i = 0; i < SNAPSHOT.length; i++) {
            copy(SNAPSHOT[i], this, GLOBAL[i], null);
        }
    }

    private static void copy(Field from, Object fromOwner, Field to, Object toOwner) {
        try {
            if (to.getType() == float.class) {
                to.setFloat(toOwner, from.getFloat(fromOwner));
            } else {
                to.setBoolean(toOwner, from.getBoolean(fromOwner));
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
