package com.swingme.util;

import net.azureaaron.hmapi.events.HypixelPacketEvents;
import net.azureaaron.hmapi.network.packet.v1.s2c.LocationUpdateS2CPacket;

public final class HypixelLocationState {

    private static boolean onHypixel = false;
    private static boolean onSkyblock = false;
    private static boolean inDungeon  = false;
    private static boolean onOwnIsland = false;
    /** The Hypixel instance, e.g. {@code mini123A}; null until the first location update. */
    private static String serverName = null;

    private HypixelLocationState() {}

    public static void register() {
        HypixelPacketEvents.HELLO.register(packet -> onHypixel = true);

        HypixelPacketEvents.LOCATION_UPDATE.register(packet -> {
            if (!(packet instanceof LocationUpdateS2CPacket location)) return;

            serverName = location.serverName();
            onOwnIsland = location.map()
                    .map(map -> map.equals("Private Island") || map.equals("Garden"))
                    .orElse(false);

            onSkyblock = location.serverType()
                    .map("SKYBLOCK"::equals)
                    .orElse(false);

            // Dungeon is a sub-mode of SkyBlock, so short-circuit when not on SkyBlock
            inDungeon = onSkyblock && location.map()
                    .map("Dungeon"::equals)
                    .orElse(false);
        });
    }

    public static  boolean isOnHypixel() { return  onHypixel; }

    public static boolean isOnSkyblock() { return onSkyblock; }

    public static boolean isInDungeon()  { return inDungeon;  }

    /** Your Private Island or Garden, which other players only reach as visitors. */
    public static boolean isOnOwnIsland() { return onOwnIsland; }

    public static String serverName() { return serverName; }

    public static void reset() {
        onHypixel  = false;
        onSkyblock = false;
        inDungeon  = false;
        onOwnIsland = false;
        serverName = null;
    }
}