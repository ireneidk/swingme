package com.swingme.util;

import com.swingme.SwingMe;
import com.swingme.config.SwingMeConfig;
import com.swingme.gui.EditScope;
import com.swingme.gui.Row;
import com.swingme.gui.Rows;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonWriter;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Export / import manager for config presets.
 * <p>
 * Presets are exchanged as compact JSON text via the clipboard — no files.
 * Category-scoped exports let users share just item animations, just scales, etc.
 */
public final class PresetManager {

    // v2 dropped the master toggles (enableHandItemTransform, enableAnimOverrides,
    // enableGroundItemScale); their sub-toggles and sliders now stand on their own.
    private static final int SCHEMA_VERSION = 2;
    private static final Gson GSON = new Gson();

    /** Category → ordered map of field-name → default-value, taken from the overlay's row tables. */
    private static final Map<String, Map<String, Object>> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(SwingMeConfig.HAND, defaultsOf(Rows.POSITION, Rows.ITEM));
        DEFAULTS.put(SwingMeConfig.ANIM, defaultsOf(Rows.SWING));
        DEFAULTS.put(SwingMeConfig.SCALE, defaultsOf(Rows.SCALE));
        DEFAULTS.put(SwingMeConfig.VIEW, defaultsOf(Rows.VIEW));
    }

    @SafeVarargs
    private static Map<String, Object> defaultsOf(List<Row>... tables) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (List<Row> table : tables) {
            for (Row row : table) out.put(row.fieldName(), row.defaultValue());
        }
        return out;
    }

    private PresetManager() {}

    /**
     * Exports the current config as a clickable chat component.
     *
     * @param category null for full export, or one of HAND/ANIM/SCALE/VIEW/ITEM
     * @return chat message containing the JSON and a copy button
     */
    public static Component exportToChat(String category) {
        String json = buildExportJson(category);
        String preview = buildPreview(category);

        MutableComponent msg = Component.literal("§a[SwingMe] §fPreset exported: " + preview + " ");

        MutableComponent copyBtn = Component.literal("§2[Copy JSON]")
                .withStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent.CopyToClipboard(json))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy preset JSON"))));

        MutableComponent instruct = Component.literal("\n§7Use §f/swingme import <json> §7to apply this preset.");

        return msg.append(copyBtn).append(instruct);
    }

    /**
     * Imports a preset from a JSON string.
     *
     * @param json raw JSON string
     * @return human-readable result message
     */
    public static String importFromJson(String json) {
        JsonObject root;
        try {
            root = GSON.fromJson(json, JsonObject.class);
        } catch (JsonParseException e) {
            SwingMe.LOGGER.warn("Preset import failed: invalid JSON — {}", e.getMessage());
            return "§cInvalid JSON. Make sure you copied the whole string.";
        }

        if (root == null || !root.has("v") || !root.has("d")) {
            return "§cInvalid preset format. Expected 'v' and 'd' fields.";
        }

        int version = root.get("v").getAsInt();
        if (version != SCHEMA_VERSION) {
            return "§cUnsupported preset version (v" + version + "). This mod expects v" + SCHEMA_VERSION + ".";
        }

        JsonObject data = root.getAsJsonObject("d");
        if (data == null || data.isEmpty()) {
            return "§cPreset contains no data.";
        }

        int applied = 0;
        int skipped = 0;

        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            String fieldName = entry.getKey();
            JsonElement value = entry.getValue();

            Field field;
            try {
                field = SwingMeConfig.class.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                SwingMe.LOGGER.debug("Preset skipped unknown field: {}", fieldName);
                skipped++;
                continue;
            }

            try {
                applyField(field, value);
                applied++;
            } catch (IllegalAccessException | IllegalArgumentException e) {
                SwingMe.LOGGER.warn("Preset failed to apply field '{}': {}", fieldName, e.getMessage());
                skipped++;
            }
        }

        FeatureFlags.update();
        // The overlay edits a buffer, not the config. Without this it still holds the values
        // from before the import, and the next copied share code would carry those instead.
        EditScope.reseedGlobal();
        eu.midnightdust.lib.config.MidnightConfig.write(SwingMe.MOD_ID);

        return "§aApplied " + applied + " setting" + (applied == 1 ? "" : "s")
                + (skipped > 0 ? " §7(" + skipped + " skipped)" : "") + ".";
    }

    /** Builds a compact JSON string for the given category (or all categories if null). */
    private static String buildExportJson(String category) {
        JsonObject root = new JsonObject();
        root.addProperty("v", SCHEMA_VERSION);
        if (category != null) {
            root.addProperty("cat", category);
        }

        JsonObject data = new JsonObject();
        if (category == null) {
            for (Map<String, Object> catMap : DEFAULTS.values()) {
                addDifferences(data, catMap);
            }
        } else {
            Map<String, Object> catMap = DEFAULTS.get(category);
            if (catMap != null) {
                addDifferences(data, catMap);
            }
        }
        root.add("d", data);

        return compactJson(root);
    }

    /** Compares current config values against defaults and adds only non-defaults to the JSON. */
    private static void addDifferences(JsonObject out, Map<String, Object> defaults) {
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String name = entry.getKey();
            Object def = entry.getValue();
            Object current = getFieldValue(name);
            if (current == null) continue;
            if (!valuesEqual(current, def)) {
                switch (current) {
                    case Boolean b -> out.addProperty(name, b);
                    case Float v -> out.addProperty(name, v);
                    case Number number -> out.addProperty(name, number.floatValue());
                    default -> {
                    }
                }
            }
        }
    }

    /** Returns a short human preview like "Item Animation" or "All settings". */
    private static String buildPreview(String category) {
        if (category == null) return "§7All settings";
        return switch (category) {
            case SwingMeConfig.HAND -> "§7Held Item";
            case SwingMeConfig.ANIM -> "§7Swing Animation";
            case SwingMeConfig.SCALE -> "§7More";
            case SwingMeConfig.VIEW -> "§7Camera & Crosshair";
            default -> "§7Custom";
        };
    }

    /** Reads a static field from {@link SwingMeConfig} via reflection. */
    private static Object getFieldValue(String name) {
        try {
            Field field = SwingMeConfig.class.getDeclaredField(name);
            return field.get(null);
        } catch (ReflectiveOperationException e) {
            SwingMe.LOGGER.error("Failed to read config field '{}': {}", name, e.getMessage());
            return null;
        }
    }

    /** Writes a JSON value into a static {@link SwingMeConfig} field. */
    private static void applyField(Field field, JsonElement value) throws IllegalAccessException {
        Class<?> type = field.getType();
        if (type == boolean.class || type == Boolean.class) {
            field.setBoolean(null, value.getAsBoolean());
        } else if (type == float.class || type == Float.class) {
            field.setFloat(null, value.getAsFloat());
        } else if (type == int.class || type == Integer.class) {
            field.setInt(null, value.getAsInt());
        } else if (type == double.class || type == Double.class) {
            field.setDouble(null, value.getAsDouble());
        } else {
            throw new IllegalArgumentException("Unsupported field type: " + type.getName());
        }
    }

    /** Loose equality for Float vs the Integer/Float defaults stored in the map. */
    private static boolean valuesEqual(Object a, Object b) {
        if (a == null || b == null) return a == b;
        if (a instanceof Number na && b instanceof Number nb) {
            return Float.compare(na.floatValue(), nb.floatValue()) == 0;
        }
        return a.equals(b);
    }

    /** Emits JSON without any unnecessary whitespace. */
    private static String compactJson(JsonObject obj) {
        StringWriter sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.setIndent("");
            jw.setLenient(false);
            GSON.toJson(obj, jw);
        } catch (IOException e) {
            // StringWriter never throws IOException
            throw new AssertionError(e);
        }
        return sw.toString();
    }
}
