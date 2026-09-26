package com.swingme.util;

import com.swingme.SwingMe;
import com.swingme.config.ItemOverrideStore;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Detached orbit camera: the player's facing is frozen while the view swings freely around their
 * head. Toggled by its own keybind, and always passes through blocks.
 * <p>
 * Pressing F5 while active hands rotation back to the player instead of exiting outright: the
 * view becomes normal, unlocked third person, but keeps whatever distance the orbit had reached,
 * as if that had always been a regular third-person zoom. That is the {@link #unlocked} state.
 * From there, either camera key drops straight to first person.
 */
public final class FreeCamera {

    /**
     * Deliberately not passed to {@code KeyMappingHelper.registerKeyMapping}: that is what puts a
     * mapping in the vanilla controls list and in {@code options.txt}, and this one is bound from
     * the mod's own menu instead. The constructor alone is enough for the key to fire, since it
     * registers itself with {@link KeyMapping}'s static tables, which is what the keyboard handler
     * drives. Persistence is {@link ItemOverrideStore}'s job.
     */
    public static final KeyMapping KEY = new KeyMapping(
            "key.swingme.free_camera",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KeyMapping.Category.MISC
    );

    private static final float DEFAULT_DISTANCE = 4f;
    private static final float MIN_DISTANCE = 0.5f;

    /** Matches {@code Entity.turn}, so looking around feels the same as it does normally. */
    private static final float LOOK_SPEED = 0.15f;

    private static boolean active;
    private static boolean unlocked;
    private static float yaw;
    private static float pitch;
    private static float distance = DEFAULT_DISTANCE;
    private static CameraType restoreType;

    private FreeCamera() {}

    /** Points {@link #KEY} at {@code key} and records it. */
    public static void bind(InputConstants.Key key) {
        KEY.setKey(key);
        // setKey only writes the field; the lookup table the keyboard handler reads is stale
        // until this rebuild.
        KeyMapping.resetMapping();
        ItemOverrideStore.setFreeCameraKey(KEY.saveString());
    }

    /** Restores the saved binding at startup. Call once, after the store has loaded. */
    public static void loadSavedKey() {
        String saved = ItemOverrideStore.freeCameraKey();
        if (saved.isEmpty()) return;
        try {
            KEY.setKey(InputConstants.getKey(saved));
            KeyMapping.resetMapping();
        } catch (IllegalArgumentException e) {
            SwingMe.LOGGER.warn("Ignoring unrecognised free-camera key '{}'; rebind it in the "
                    + "SwingMe menu.", saved);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isUnlocked() {
        return unlocked;
    }

    /** True while either the orbit or the post-F5 unlocked view is holding the camera. */
    public static boolean isEngaged() {
        return active || unlocked;
    }

    public static float yaw() {
        return yaw;
    }

    public static float pitch() {
        return pitch;
    }

    public static float distance() {
        return distance;
    }

    public static void toggle() {
        if (active) {
            stop();
        } else if (unlocked) {
            unlocked = false;
            restoreType = null;
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        } else {
            start();
        }
    }

    private static void start() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        yaw = player.getYRot();
        pitch = player.getXRot();
        distance = DEFAULT_DISTANCE;
        restoreType = client.options.getCameraType();
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        active = true;
    }

    public static void stop() {
        if (!active) return;
        active = false;
        Minecraft.getInstance().options.setCameraType(
                restoreType == null ? CameraType.FIRST_PERSON : restoreType);
        restoreType = null;
    }

    /**
     * F5 pressed while active: stops orbiting and gives rotation back to the player, but keeps the
     * current distance rather than snapping to a default. Called from the {@code CameraType.cycle()}
     * hook, which supplies the resulting {@code THIRD_PERSON_BACK} itself — this only updates state.
     */
    public static void unlockKeepingDistance() {
        active = false;
        unlocked = true;
    }

    /**
     * F5 pressed while already unlocked: drops to first person. Called from the same hook, which
     * supplies {@code FIRST_PERSON} itself.
     */
    public static void clearUnlocked() {
        unlocked = false;
        restoreType = null;
    }

    /**
     * Holds the view in third person for as long as the camera is detached. Without this, F5 would
     * drop you into first person still looking through the free camera's rotation, with a player
     * who no longer turns.
     */
    public static void tick() {
        if (!active) return;

        Minecraft client = Minecraft.getInstance();
        if (client.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    /** Steers the camera with mouse movement that would otherwise have turned the player. */
    public static void look(double deltaYaw, double deltaPitch) {
        yaw += (float) deltaYaw * LOOK_SPEED;
        pitch = Mth.clamp(pitch + (float) deltaPitch * LOOK_SPEED, -90f, 90f);
    }

    /**
     * Scroll up pulls the camera in, scroll down pushes it out. The step grows with the current
     * distance so pulling far back does not take dozens of clicks; there is no upper limit.
     */
    public static void zoom(double scroll) {
        float step = Math.max(1f, distance * 0.15f);
        distance = Math.max(MIN_DISTANCE, distance - (float) scroll * step);
    }

    /** Drops the camera without touching options, for when the player or level is already gone. */
    public static void reset() {
        active = false;
        unlocked = false;
        restoreType = null;
        distance = DEFAULT_DISTANCE;
    }

    /**
     * Drops the orbit when the Free option is turned off mid-session. The unlocked state is left
     * as-is beyond clearing its flag: it is already a normal, valid third-person view, so there is
     * nothing to restore.
     */
    public static void forceExit() {
        if (active) stop();
        unlocked = false;
    }
}
