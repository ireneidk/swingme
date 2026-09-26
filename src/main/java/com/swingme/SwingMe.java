package com.swingme;

import com.swingme.command.Commands;
import com.swingme.config.ItemOverrideStore;
import com.swingme.config.SwingMeConfig;
import com.swingme.gui.EditScope;
import com.swingme.gui.ItemTuneScreen;
import com.swingme.gui.Theme;
import com.swingme.util.ActiveSettings;
import com.swingme.util.BlockingState;
import com.swingme.util.FeatureFlags;
import com.swingme.util.HypixelLocationState;
import com.swingme.util.ItemOverrideResolver;
import com.swingme.util.SwingHoldState;
import com.swingme.util.SwingTester;
import eu.midnightdust.lib.config.MidnightConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.azureaaron.hmapi.network.HypixelNetworking;
import net.azureaaron.hmapi.network.packet.v1.s2c.LocationUpdateS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.tags.ItemTags;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwingMe implements ClientModInitializer {

    public static final String MOD_ID = "swingme";
    public static final Logger LOGGER = LoggerFactory.getLogger("SwingMe");

    private static final String CREATOR = "Eiryna";
    /** The Hypixel instance the creator was last announced in, so each lobby says it once. */
    private static String greetedServer = null;

    @Override
    public void onInitializeClient() {
        MidnightConfig.init(MOD_ID, SwingMeConfig.class);

        // Seed the overlay's global buffer up front so /swingme code works before the
        // overlay has ever been opened.
        EditScope.reseedGlobal();

        ItemOverrideStore.load();
        ItemTuneScreen.windowX = ItemOverrideStore.windowX();
        ItemTuneScreen.windowY = ItemOverrideStore.windowY();
        ItemTuneScreen.windowW = ItemOverrideStore.windowW();
        ItemTuneScreen.windowH = ItemOverrideStore.windowH();
        ItemTuneScreen.setPanelOpen(ItemOverrideStore.panelOpen());
        Theme.set(Theme.byName(ItemOverrideStore.theme()));

        HypixelNetworking.registerToEvents(
                Util.make(new Object2IntOpenHashMap<>(), map -> map.put(LocationUpdateS2CPacket.ID, 1))
        );

        HypixelLocationState.register();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            HypixelLocationState.reset();
            SwingHoldState.reset();
            ItemOverrideResolver.clear();
            SwingTester.stopRepeat();
        });

        // Registered before the sword-block key so it sits first among this mod's
        // entries in the Miscellaneous section of the controls screen.
        KeyMapping tuneKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.swingme.tune_held_item",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                KeyMapping.Category.MISC
        ));

        KeyMapping blockKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.swingme.sword_block",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                KeyMapping.Category.GAMEPLAY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FeatureFlags.update();

            if (client.player == null) {
                SwingHoldState.reset();
                ItemOverrideResolver.clear();
                SwingTester.stopRepeat();
                return;
            }

            ItemOverrideResolver.update(client.player);
            greetCreator(client);
            ActiveSettings.selectMainHand();

            SwingHoldState.update(ActiveSettings.isEnabled(FeatureFlags.SUPPRESS_REPEAT_SWING)
                    && client.options.keyAttack.isDown());

            BlockingState.isBlocking = ActiveSettings.isEnabled(FeatureFlags.SWORD_BLOCK)
                    && blockKey.isDown()
                    && client.player.getMainHandItem().is(ItemTags.SWORDS);

            // The overlay opens regardless of what is held: it starts in the global scope,
            // and the held-item scope reports for itself when the item cannot be tuned.
            while (tuneKey.consumeClick()) {
                ItemTuneScreen.open();
            }

            SwingTester.tick();
            EditScope.flush();
            ItemOverrideStore.saveIfDirty();
        });

        Commands.register();
    }

    private static void greetCreator(Minecraft client) {
        String server = HypixelLocationState.serverName();
        if (server == null || server.equals(greetedServer) || HypixelLocationState.isOnOwnIsland()) return;
        if (client.player.getPlainTextName().equalsIgnoreCase(CREATOR)) return;
        for (AbstractClientPlayer player : client.level.players()) {
            if (player.getPlainTextName().equalsIgnoreCase(CREATOR)) {
                greetedServer = server;
                client.player.sendSystemMessage(Component.literal(
                        "Eiryna is here! They made the swing thing ur using :o Say hi!")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                return;
            }
        }
    }
}
