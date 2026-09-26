package com.swingme.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/**
 * Plays the main-hand swing animation locally, for previewing animation settings.
 * <p>
 * Deliberately does not call {@code LocalPlayer.swing}, which would send a
 * {@code ServerboundSwingPacket} and make the swing visible to other players. Setting the
 * three swing fields is what vanilla's {@code LivingEntity.swing} does internally, so the
 * normal {@code updateSwingTime} tick advances and ends the animation as usual.
 * <p>
 * Because it bypasses {@code swing(hand, boolean)} entirely it is also unaffected by the
 * mod's own repeated-swing suppression, which is what a test button should do.
 */
public final class SwingTester {

    private static boolean repeating = false;

    private SwingTester() {}

    /** Plays one swing. */
    public static void swing() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        player.swinging = true;
        player.swingingArm = InteractionHand.MAIN_HAND;
        player.swingTime = -1;
    }

    public static boolean isRepeating() {
        return repeating;
    }

    /** Starts or stops the continuous swing loop. */
    public static void toggleRepeat() {
        repeating = !repeating;
        if (repeating) swing();
    }

    public static void stopRepeat() {
        repeating = false;
    }

    /**
     * Restarts the swing as soon as the previous one finishes, which is what vanilla looks
     * like while the attack key is held on a block. Call once per client tick.
     */
    public static void tick() {
        if (!repeating) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            repeating = false;
            return;
        }
        if (!player.swinging) swing();
    }
}
