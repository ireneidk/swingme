package com.swingme.mixin;

import com.swingme.util.ActiveSettings;
import com.swingme.util.FeatureFlags;
import com.swingme.util.SwingHoldState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks repeat swings started while the attack key remains held. */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Inject(method = "swing", at = @At("HEAD"))
    private void swingme$trackHeldAttackSwing(InteractionHand hand, CallbackInfo ci) {
        boolean holdingAttack = ActiveSettings.isEnabled(FeatureFlags.SUPPRESS_REPEAT_SWING)
                && Minecraft.getInstance().options.keyAttack.isDown();
        SwingHoldState.onSwing(holdingAttack);
    }
}
