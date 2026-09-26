package com.swingme.config;

import com.swingme.SwingMe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistence for per-item overrides, in its own file rather than in MidnightLib's config,
 * which only handles its own {@code @Entry} fields.
 */
public final class ItemOverrideStore {

    private static final int SCHEMA_VERSION = 1;
    private static final String FILE_NAME = "swingme_item_overrides.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Serialized shape of the file. */
    private static class Data {
        int version = SCHEMA_VERSION;
        int windowX = 40;
        int windowY = 40;
        int windowW = 236;
        int windowH = 250;
        String theme = "DARK";
        boolean panelOpen = false;
        boolean secretUnlocked = false;
        /** The free-camera key as {@code KeyMapping.saveString()}; empty means never bound here. */
        String freeCameraKey = "";
        Map<String, ItemOverride> overrides = new LinkedHashMap<>();
    }

    private static Data data = new Data();
    private static boolean dirty = false;

    private ItemOverrideStore() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void load() {
        Path file = path();
        if (!Files.exists(file)) {
            data = new Data();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Data parsed = GSON.fromJson(reader, Data.class);
            if (parsed == null) {
                data = new Data();
            } else {
                if (parsed.overrides == null) parsed.overrides = new LinkedHashMap<>();
                data = parsed;
            }
        } catch (IOException | JsonParseException e) {
            // The next write replaces the unreadable file, so say so loudly rather than
            // implying the old contents survive somewhere.
            SwingMe.LOGGER.warn("Could not read {}; starting empty, and the next save will "
                    + "overwrite it. Back it up now if you want to recover it by hand.", FILE_NAME, e);
            data = new Data();
        }
    }

    /** Queues a write for the end of the tick. */
    public static void markDirty() {
        dirty = true;
    }

    /** Writes the file if anything changed. Call once per client tick. */
    public static void saveIfDirty() {
        if (!dirty) return;
        dirty = false;
        data.version = SCHEMA_VERSION;
        try (Writer writer = Files.newBufferedWriter(path())) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            SwingMe.LOGGER.error("Could not write {}", FILE_NAME, e);
        }
    }

    /** @return the override for this UUID, or null when there is none. */
    public static ItemOverride get(String uuid) {
        if (uuid == null || uuid.isEmpty()) return null;
        return data.overrides.get(uuid);
    }

    public static boolean isEmpty() {
        return data.overrides.isEmpty();
    }

    public static void put(String uuid, ItemOverride override) {
        data.overrides.put(uuid, override);
        markDirty();
    }

    public static void remove(String uuid) {
        if (data.overrides.remove(uuid) != null) markDirty();
    }

    public static Map<String, ItemOverride> all() {
        return Collections.unmodifiableMap(data.overrides);
    }

    public static int windowX() { return data.windowX; }
    public static int windowY() { return data.windowY; }
    public static int windowW() { return data.windowW; }
    public static int windowH() { return data.windowH; }

    public static void setWindowPos(int x, int y) {
        if (data.windowX == x && data.windowY == y) return;
        data.windowX = x;
        data.windowY = y;
        markDirty();
    }

    public static void setWindowSize(int w, int h) {
        if (data.windowW == w && data.windowH == h) return;
        data.windowW = w;
        data.windowH = h;
        markDirty();
    }

    /** @return the saved theme name; {@code Theme.byName} decides what an unknown value means. */
    public static String theme() { return data.theme == null ? "" : data.theme; }

    public static void setTheme(String name) {
        if (name.equals(data.theme)) return;
        data.theme = name;
        markDirty();
    }

    public static boolean panelOpen() { return data.panelOpen; }

    public static void setPanelOpen(boolean open) {
        if (data.panelOpen == open) return;
        data.panelOpen = open;
        markDirty();
    }

    public static boolean secretUnlocked() { return data.secretUnlocked; }

    public static void setSecretUnlocked(boolean unlocked) {
        if (data.secretUnlocked == unlocked) return;
        data.secretUnlocked = unlocked;
        markDirty();
    }

    /**
     * The free-camera keybind is not registered with the vanilla controls list, so
     * {@code options.txt} never sees it and this file is what makes it survive a restart.
     */
    public static String freeCameraKey() { return data.freeCameraKey == null ? "" : data.freeCameraKey; }

    public static void setFreeCameraKey(String saveString) {
        if (saveString.equals(data.freeCameraKey)) return;
        data.freeCameraKey = saveString;
        markDirty();
    }
}
