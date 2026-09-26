package com.swingme.mixin;

import com.swingme.util.FeatureFlags;
import com.swingme.util.HypixelLocationState;
import com.swingme.util.NpcCache;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(
            method = "submit",
            at = @At("HEAD"),
            cancellable = true
    )
    private <S extends EntityRenderState> void swingme$hidePlayers(
            S renderState,
            CameraRenderState camera,
            double x,
            double y,
            double z,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CallbackInfo ci
    ) {
        if (!FeatureFlags.isEnabled(FeatureFlags.HIDE_PLAYERS)) return;
        if (FeatureFlags.isEnabled(FeatureFlags.HIDE_PLAYERS_SB_ONLY) && !HypixelLocationState.isOnSkyblock()) return;
        if (!(renderState instanceof AvatarRenderState avatarState)) return;
        if (avatarState.entityType != EntityType.PLAYER) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && avatarState.id == mc.player.getId()) return;

        // Don't hide Hypixel NPCs — only real players
        if (NpcCache.isHypixelNpc(avatarState.id)) return;

        ci.cancel();
    }
}