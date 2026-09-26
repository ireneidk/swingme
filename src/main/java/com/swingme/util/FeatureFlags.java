package com.swingme.util;

import com.swingme.config.SwingMeConfig;

/**
 * Centralised feature bitmask updated once per tick, for the settings that stay global:
 * the scale and view groups.
 * <p>
 * Hot render paths read a single {@code int} instead of multiple config booleans,
 * giving the JIT a trivial branch-predictor target and eliminating redundant
 * field accesses.
 * <p>
 * Some bits are <em>derived</em> rather than mapped 1:1 to a toggle — a scale slider
 * sitting at 1.0 is off, so the flag stays clear and the mixin early-outs without
 * ever comparing floats. Mixins should therefore test one bit, not a combination.
 * <p>
 * The hand and swing bits below are declared here but set by {@link ActiveSettings}, which
 * resolves them per item rather than from the global config, and read through
 * {@link ActiveSettings#isEnabled(int)}. {@link #update()} deliberately leaves them clear:
 * computing them here as well would produce a second, per-item-blind answer that no caller
 * wants.
 */
public final class FeatureFlags {

    // Per-item bits: owned by ActiveSettings, never set by update().
    public static final int ARM_POSITION             = 1 << 0;
    public static final int ITEM_TRANSFORM           = 1 << 1;
    public static final int SEPARATE_OFFHAND         = 1 << 2;
    public static final int DISABLE_SWING_BOB        = 1 << 3;
    public static final int IGNORE_SWING_SPEED       = 1 << 4;
    public static final int SWING_DURATION           = 1 << 5;
    public static final int SWING_OVERRIDE           = 1 << 6;
    public static final int DISABLE_SWING_ANIM       = 1 << 7;
    public static final int SWORD_BLOCK              = 1 << 8;
    public static final int SUPPRESS_REPEAT_SWING    = 1 << 18;
    public static final int HOLD_REPEAT_SWING_BOTTOM = 1 << 19;

    // Global bits: set by update() below.
    public static final int SCALE_NAME_TAGS          = 1 << 9;
    public static final int SCALE_ANY                = 1 << 10;
    public static final int CROSSHAIR_3RD            = 1 << 11;
    public static final int CROSSHAIR_3RD_FRONT      = 1 << 12;
    public static final int DISABLE_SELFIE           = 1 << 13;
    public static final int SHOW_OWN_NAMETAG         = 1 << 14;
    public static final int HIDE_PLAYERS             = 1 << 15;
    public static final int HIDE_PLAYERS_SB_ONLY     = 1 << 16;
    public static final int GROUND_ITEM_SCALE        = 1 << 17;
    public static final int DISABLE_CAMERA_SWAY      = 1 << 22;

    private static int flags = 0;

    private FeatureFlags() {}

    /** Recompute the global half of the bitmask from current config values. Call once per client tick. */
    public static void update() {
        int f = 0;

        if (SwingMeConfig.scaleNameTags)                  f |= SCALE_NAME_TAGS;
        if (SwingMeConfig.playerScale != 1f
                || SwingMeConfig.otherPlayersScale != 1f
                || SwingMeConfig.hypixelNpcScale != 1f)   f |= SCALE_ANY;

        if (SwingMeConfig.groundItemScale != 1f)          f |= GROUND_ITEM_SCALE;

        if (SwingMeConfig.enableCrosshairInThirdPerson)   f |= CROSSHAIR_3RD;
        if (SwingMeConfig.enableCrosshairInThirdPersonFront) f |= CROSSHAIR_3RD_FRONT;
        if (SwingMeConfig.disableSelfieCam)               f |= DISABLE_SELFIE;
        if (SwingMeConfig.showOwnNametagInThirdPerson)    f |= SHOW_OWN_NAMETAG;
        if (SwingMeConfig.hidePlayers)                    f |= HIDE_PLAYERS;
        if (SwingMeConfig.hidePlayersOnlyOnSkyblock)      f |= HIDE_PLAYERS_SB_ONLY;

        if (SwingMeConfig.disableCameraSway)              f |= DISABLE_CAMERA_SWAY;

        flags = f;
    }

    /** True when every bit in {@code mask} is set. */
    public static boolean isEnabled(int mask) {
        return (flags & mask) == mask;
    }
}
