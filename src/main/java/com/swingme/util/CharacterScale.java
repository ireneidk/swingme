package com.swingme.util;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.client.Minecraft;

/**
 * Per-entity <em>total</em> render scale: this mod's own factor multiplied by whatever
 * other mods applied to the same pose.
 * <p>
 * The value is <em>measured</em>, never read from another mod's fields, so nothing here
 * references a foreign class. With no other scaling mod installed the measurement collapses
 * to {@code 1.0} and this becomes an exact mirror of {@link PerTickCache#getScale(int)}.
 *
 * <h2>How the measurement works</h2>
 * {@code AvatarRendererMixin} brackets {@code AvatarRenderer.scale(...)} with a HEAD and a
 * TAIL injection and compares {@code poseStack.last().pose().m11()} (the Y scale) across the
 * two. Anything that changed the pose between them shows up in the ratio.
 *
 * <h2>Known limits (accepted, not bugs)</h2>
 * <ul>
 *   <li>Another mod injecting at {@code HEAD} of the same method lands <em>inside</em> our
 *       window — its scale is captured. This is the common case (Odin does exactly this).</li>
 *   <li>A mod injecting at {@code TAIL} <em>after</em> us runs outside the window and is
 *       <em>not</em> captured. Mixin gives no ordering guarantee we could rely on to fix
 *       this, so it is an accepted limitation.</li>
 *   <li>Only the Y axis is sampled. A caller applying a non-uniform scale, or a rotation that
 *       tilts the Y axis, would be mis-measured. Neither happens on the vanilla avatar path,
 *       where {@code scale()} is reached after a pure Y-axis rotation.</li>
 * </ul>
 *
 * <h2>Why the map cannot grow unbounded</h2>
 * Entities load and unload constantly, and nothing here observes entity removal. The map is
 * therefore cleared on a cadence — on every client tick / level change, exactly like
 * {@link PerTickCache} — which bounds it to "avatars that were actually rendered during one
 * tick". The alternative (keeping only what the current frame wrote) would need a frame
 * boundary signal this class does not have, and would buy nothing: entries are rewritten
 * every frame anyway, and a reader always runs in the same frame as its writer because the
 * nametag is submitted from inside the same {@code EntityRenderer.submit} call that scaled
 * the model. Entries that resolve to {@code 1.0} are not stored at all.
 */
public final class CharacterScale {

    private CharacterScale() {}

    private static final Int2FloatOpenHashMap SCALES = new Int2FloatOpenHashMap();
    private static long lastTick = Long.MIN_VALUE;
    private static Object lastLevel = null;

    static {
        // Absent entity -> unscaled. Keeps every reader branch-free.
        SCALES.defaultReturnValue(1f);
    }

    /**
     * Records the total render scale for {@code entityId}. Called once per avatar per frame
     * from the render path; a value of exactly {@code 1.0} clears the entry instead of
     * storing it.
     */
    public static void put(int entityId, float totalScale) {
        rollOver();

        if (totalScale == 1f || !Float.isFinite(totalScale)) {
            SCALES.remove(entityId);
            return;
        }
        SCALES.put(entityId, totalScale);
    }

    /** Total render scale last measured for {@code entityId}, or {@code 1.0} if unknown. */
    public static float get(int entityId) {
        return SCALES.get(entityId);
    }

    /**
     * Drops everything once per client tick (or on a level change). Only called from
     * {@link #put}: a reader always follows its own writer within the same frame, so it can
     * never observe an entry this would have dropped.
     */
    private static void rollOver() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (lastLevel != null) {
                SCALES.clear();
                lastLevel = null;
            }
            return;
        }

        Object level = mc.level;
        long tick = mc.level.getGameTime();

        if (level != lastLevel || tick != lastTick) {
            SCALES.clear();
            lastLevel = level;
            lastTick = tick;
        }
    }
}
