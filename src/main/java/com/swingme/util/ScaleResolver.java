package com.swingme.util;

import com.swingme.config.SwingMeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

public final class ScaleResolver {

    private ScaleResolver() {}

    /** Returns the configured scale for the given entity id, or 1.0f if no scaling applies. */
    public static float resolveScale(Minecraft mc, int entityId) {
        if (mc.player == null || mc.level == null) return 1f;

        boolean onHypixel = HypixelLocationState.isOnHypixel();
        boolean scalingAllowed = !onHypixel || HypixelLocationState.isOnSkyblock();

        if (entityId == mc.player.getId()) {
            return SwingMeConfig.playerScale;
        }

        if (onHypixel) {
            var entity = mc.level.getEntity(entityId);
            if (entity instanceof AbstractClientPlayer player && HypixelNpcUtil.isHypixelNpc(player)) {
                return HypixelLocationState.isInDungeon() ? 1f : SwingMeConfig.hypixelNpcScale;
            }
        }

        return scalingAllowed ? SwingMeConfig.otherPlayersScale : 1f;
    }
}
