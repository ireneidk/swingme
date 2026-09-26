package com.swingme.util;

import com.swingme.config.ItemOverride;
import com.swingme.config.ItemOverrideStore;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/**
 * Resolves the held stacks to their overrides once per client tick so the render path
 * never touches NBT. Follows the same per-tick caching pattern as {@link PerTickCache}.
 * <p>
 * Both cached fields are null when the held item has no override, which is the signal
 * to fall back to the global {@code SwingMeConfig} values.
 */
public final class ItemOverrideResolver {

    private static ItemOverride mainHand = null;
    private static ItemOverride offHand = null;

    private ItemOverrideResolver() {}

    public static void update(LocalPlayer player) {
        // Nothing configured, or not on SkyBlock: cost nothing and fall back to global.
        if (player == null || ItemOverrideStore.isEmpty() || !HypixelLocationState.isOnSkyblock()) {
            clear();
            return;
        }
        mainHand = ItemOverrideStore.get(ItemIdentity.uuidOf(player.getMainHandItem()));
        offHand = ItemOverrideStore.get(ItemIdentity.uuidOf(player.getOffhandItem()));
    }

    public static void clear() {
        mainHand = null;
        offHand = null;
    }

    /** @return the override for the item in this hand, or null when it has none. */
    public static ItemOverride forHand(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? offHand : mainHand;
    }

    /** @return the override for the main-hand item, or null when it has none. */
    public static ItemOverride mainHand() {
        return mainHand;
    }
}
