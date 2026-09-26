package com.swingme.gui;

import com.swingme.SwingMe;
import com.swingme.config.ItemOverride;
import com.swingme.config.ItemOverrideStore;
import com.swingme.util.ItemIdentity;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

/**
 * What the overlay is currently editing: the global settings shared by every item, or the
 * settings of the item in the player's main hand.
 * <p>
 * Both scopes present the same {@link ItemOverride} shape so the row tables work unchanged.
 * In {@link Mode#ALL_ITEMS} the rows edit a buffer that is pushed back onto
 * {@code SwingMeConfig}; in {@link Mode#HELD_ITEM} they edit the stored override for the held
 * item, or a scratch copy that is only persisted once the user actually changes something.
 * <p>
 * The global-only rows ({@code Rows.SCALE}, {@code Rows.VIEW}) bypass all of this and write
 * {@code SwingMeConfig} directly; they still need {@link #markGlobalDirty()} so the file gets
 * written.
 */
public final class EditScope {

    public enum Mode {
        ALL_ITEMS("swingme.tune.scope.all"),
        HELD_ITEM("swingme.tune.scope.held");

        public final String key;

        Mode(String key) {
            this.key = key;
        }
    }

    /** Remembered across openings. */
    private static Mode mode = Mode.ALL_ITEMS;

    /** Edited by the rows in ALL_ITEMS scope, then pushed onto the config. */
    private static final ItemOverride GLOBAL_BUFFER = new ItemOverride();

    /** Scratch override for a held item that has no saved settings yet. */
    private static ItemOverride heldScratch = null;
    private static String heldScratchUuid = "";

    private static boolean globalDirty = false;
    private static boolean writePending = false;
    private static long lastChange = 0L;

    /**
     * How long to wait after the last edit before writing the config file. Dragging a slider
     * changes a value many times a second, and each write re-serializes the whole config.
     */
    private static final long WRITE_DELAY_MS = 750L;

    private EditScope() {}

    public static Mode mode() {
        return mode;
    }

    public static void setMode(Mode next) {
        if (mode == next) return;
        mode = next;
        if (next == Mode.ALL_ITEMS) reseedGlobal();
    }

    /** Copies the live config into the buffer. Call whenever the overlay opens. */
    public static void reseedGlobal() {
        GLOBAL_BUFFER.loadFromGlobal();
    }

    /**
     * The object the rows should read and write.
     *
     * @return the current target, or null in HELD_ITEM scope when the held item has no
     *         SkyBlock UUID and therefore cannot carry its own settings.
     */
    public static ItemOverride target() {
        if (mode == Mode.ALL_ITEMS) return GLOBAL_BUFFER;

        String uuid = heldUuid();
        if (uuid.isEmpty()) return null;

        ItemOverride saved = ItemOverrideStore.get(uuid);
        if (saved != null) {
            heldScratch = null;
            heldScratchUuid = "";
            return saved;
        }

        // Nothing saved for this item yet. Show it seeded from the global settings and only
        // commit to the store once an edit actually happens, so merely holding an item never
        // fills the file with entries.
        if (heldScratch == null || !heldScratchUuid.equals(uuid)) {
            heldScratch = ItemOverride.fromGlobal(heldName());
            heldScratchUuid = uuid;
        }
        return heldScratch;
    }

    /** The main-hand item's SkyBlock UUID, or an empty string. */
    public static String heldUuid() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return "";
        return ItemIdentity.uuidOf(client.player.getMainHandItem());
    }

    public static String heldName() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return "";
        ItemStack stack = client.player.getMainHandItem();
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    /** The name to show in the title bar for the current scope. */
    public static String title() {
        if (mode == Mode.ALL_ITEMS) return "";
        String name = heldName();
        return name.isEmpty() ? "" : name;
    }

    /**
     * Records that the target was just edited, persisting a scratch override on first change.
     * Call after every row edit, reset, undo and share-code import.
     */
    public static void markDirty() {
        if (mode == Mode.ALL_ITEMS) {
            globalDirty = true;
            return;
        }
        if (heldScratch != null && !heldScratchUuid.isEmpty()) {
            ItemOverrideStore.put(heldScratchUuid, heldScratch);
            heldScratch = null;
            heldScratchUuid = "";
        }
        ItemOverrideStore.markDirty();
    }

    /**
     * Records a change a global-only row made straight to {@code SwingMeConfig}: it needs the file
     * written, but has nothing in the buffer to push and must not touch the held item.
     * <p>
     * Separate from {@link #markDirty()} because those rows are reachable in either scope now, and
     * in HELD_ITEM scope {@code markDirty} would commit a scratch override for an item the user
     * never edited while leaving the config file unwritten.
     */
    public static void markGlobalDirty() {
        writePending = true;
        lastChange = Util.getMillis();
    }

    /**
     * Pushes buffered global edits onto the config, then writes the file once the edits stop.
     * Call once per client tick.
     * <p>
     * The push happens immediately so the change is visible in the world on the next frame;
     * only the file write waits, so a slider drag costs one write instead of one per tick.
     */
    public static void flush() {
        if (globalDirty) {
            globalDirty = false;
            GLOBAL_BUFFER.applyToGlobal();
            writePending = true;
            lastChange = Util.getMillis();
        }
        if (writePending && Util.getMillis() - lastChange >= WRITE_DELAY_MS) {
            writeNow();
        }
    }

    /** Writes the config file straight away, for the moments a delay could lose the edit. */
    public static void writeNow() {
        if (globalDirty) {
            globalDirty = false;
            GLOBAL_BUFFER.applyToGlobal();
        }
        if (!writePending) return;
        writePending = false;
        MidnightConfig.write(SwingMe.MOD_ID);
    }
}
