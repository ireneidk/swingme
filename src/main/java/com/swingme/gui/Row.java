package com.swingme.gui;

import com.swingme.config.ItemOverride;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** One editable line in the overlay. */
public abstract class Row {

    public static final int ROW_HEIGHT = 15;

    /**
     * The field defaults, read through each row's own getter. Never mutated.
     * <p>
     * Rows backed by {@code SwingMeConfig} statics cannot use this — their getters ignore the
     * argument entirely — so those rows carry an explicit default instead.
     */
    private static final ItemOverride DEFAULTS = new ItemOverride();

    /** MidnightLib language key for this setting, reused so labels stay identical to the main menu. */
    private final String fieldName;
    private final Predicate<ItemOverride> visibleWhen;

    protected Row(String fieldName, Predicate<ItemOverride> visibleWhen) {
        this.fieldName = fieldName;
        this.visibleWhen = visibleWhen;
    }

    /** Stable identifier, also the share-code registry key. */
    public String fieldName() {
        return fieldName;
    }

    public String label() {
        return I18n.get("swingme.midnightconfig." + fieldName);
    }

    /**
     * The same explanatory text the main config menu shows on hover.
     *
     * @return the tooltip, or an empty string when this setting has no {@code .tooltip} key.
     */
    public String tooltip() {
        // I18n.exists was removed in 26.2, and a missing key resolves to the key itself
        // on every target, so compare against it rather than probing.
        String key = "swingme.midnightconfig." + fieldName + ".tooltip";
        String value = I18n.get(key);
        return value.equals(key) ? "" : value;
    }

    public boolean visible(ItemOverride o) {
        return visibleWhen == null || visibleWhen.test(o);
    }

    public abstract void draw(GuiGraphicsExtractor g, int x, int y, int w, ItemOverride o);

    /** @return true when this row consumed the click. */
    public abstract boolean click(double mx, double my, int x, int y, int w, ItemOverride o);

    /** @return true when this row consumed the drag. */
    public boolean drag(double mx, int x, int w, ItemOverride o) {
        return false;
    }

    // -- Value access -------------------------------------------------------
    // Boxed on purpose: these are only touched on click, reset and share-code
    // encoding, never per frame, and one shape keeps the share-code registry simple.

    /** @return a {@link Float} for sliders, a {@link Boolean} for toggles. */
    public abstract Object get(ItemOverride o);

    /** @param value a {@link Float} for sliders, a {@link Boolean} for toggles. */
    public abstract void set(ItemOverride o, Object value);

    /** @return this setting's default, same boxed type as {@link #get}. */
    public abstract Object defaultValue();

    public abstract boolean isFloat();

    /** Slider bounds. Meaningless for toggles, which return 0. */
    public float min() {
        return 0f;
    }

    public float max() {
        return 0f;
    }

    public void reset(ItemOverride o) {
        set(o, defaultValue());
    }

    // -- Slider -------------------------------------------------------------

    public static class Slider extends Row {
        /** Click target for the value text, wide enough to aim at even when the number is short. */
        private static final int EDIT_W = 46;

        /**
         * How far a typed value may exceed the drag range. The slider covers the useful span; this
         * only has to stop a typo turning into an unrecoverable value.
         */
        private static final float TYPED_RANGE_FACTOR = 9f;

        private final Function<ItemOverride, Float> getter;
        private final Setter setter;
        private final float min;
        private final float max;
        /** Explicit default for rows not backed by {@link ItemOverride}; null to read {@link #DEFAULTS}. */
        private final Float explicitDefault;
        private boolean grabbed = false;

        /** Non-null while the value is being typed rather than dragged. */
        private String editBuffer = null;
        /** True once Ctrl+A has selected the whole buffer, so the next edit replaces it. */
        private boolean selectedAll = false;

        public interface Setter {
            void set(ItemOverride o, float value);
        }

        public Slider(String fieldName, float min, float max,
                      Function<ItemOverride, Float> getter, Setter setter,
                      Predicate<ItemOverride> visibleWhen) {
            this(fieldName, min, max, getter, setter, visibleWhen, null);
        }

        /** @param explicitDefault the default for a row whose getter ignores its argument. */
        public Slider(String fieldName, float min, float max,
                      Function<ItemOverride, Float> getter, Setter setter,
                      Predicate<ItemOverride> visibleWhen, Float explicitDefault) {
            super(fieldName, visibleWhen);
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
            this.explicitDefault = explicitDefault;
        }

        @Override
        public void draw(GuiGraphicsExtractor g, int x, int y, int w, ItemOverride o) {
            float value = getter.apply(o);
            // A typed value can sit outside the drag range, so the fill has to be clamped or it
            // would run past the track.
            float t = Math.max(0f, Math.min(1f, (value - min) / (max - min)));
            int trackY = y + 9;
            int fillW = Math.round(t * w);

            Draw.text(g, label(), x, y, Theme.current().label());

            if (editBuffer != null) {
                int bx = x + w - EDIT_W;
                g.fill(bx, y - 1, x + w, y + 9, Theme.current().button());
                if (selectedAll && !editBuffer.isEmpty()) {
                    g.fill(bx + 1, y - 1, bx + 3 + Draw.width(editBuffer), y + 9,
                            Theme.current().sliderFill());
                }
                Draw.text(g, selectedAll ? editBuffer : editBuffer + "_", bx + 2, y,
                        Theme.current().text());
            } else {
                String shown = String.format("%.3f", value);
                Draw.text(g, shown, x + w - Draw.width(shown), y, Theme.current().text());
            }

            g.fill(x, trackY, x + w, trackY + 4, Theme.current().sliderTrack());
            g.fill(x, trackY, x + fillW, trackY + 4, Theme.current().sliderFill());
            int knobX = Math.max(x, Math.min(x + w - 2, x + fillW - 1));
            g.fill(knobX, trackY - 2, knobX + 2, trackY + 6, Theme.current().sliderKnob());
        }

        @Override
        public boolean click(double mx, double my, int x, int y, int w, ItemOverride o) {
            if (my < y || my >= y + ROW_HEIGHT || mx < x || mx >= x + w) return false;
            grabbed = true;
            applyFromMouse(mx, x, w, o);
            return true;
        }

        /** @return true when the click landed on the value text, which starts typing instead of dragging. */
        public boolean clickedValue(double mx, double my, int x, int y, int w) {
            return my >= y - 1 && my < y + 9 && mx >= x + w - EDIT_W && mx < x + w;
        }

        public boolean isEditing() {
            return editBuffer != null;
        }

        public void beginEdit(ItemOverride o) {
            editBuffer = String.format("%.3f", getter.apply(o));
            selectedAll = false;
        }

        public void cancelEdit() {
            editBuffer = null;
            selectedAll = false;
        }

        /** The text currently being typed, for Ctrl+C. Empty when nothing is being edited. */
        public String editText() {
            return editBuffer == null ? "" : editBuffer;
        }

        public void selectAll() {
            if (editBuffer != null) selectedAll = true;
        }

        /** Accepts the characters a number can be made of; anything else is ignored. */
        public void typeChar(char c) {
            if (editBuffer == null) return;
            if (selectedAll) {
                editBuffer = "";
                selectedAll = false;
            }
            if (editBuffer.length() >= 12) return;
            if (c == '-' && !editBuffer.isEmpty()) return;
            if (c == '.' && editBuffer.contains(".")) return;
            if (c != '-' && c != '.' && (c < '0' || c > '9')) return;
            editBuffer += c;
        }

        public void backspace() {
            if (editBuffer == null || editBuffer.isEmpty()) return;
            if (selectedAll) {
                editBuffer = "";
                selectedAll = false;
                return;
            }
            editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
        }

        /**
         * Applies the typed number, or leaves the value alone when it does not parse.
         *
         * @return true when the value actually changed.
         */
        public boolean commitEdit(ItemOverride o) {
            String typed = editBuffer;
            editBuffer = null;
            selectedAll = false;
            if (typed == null || typed.isEmpty() || typed.equals("-") || typed.equals(".")) return false;

            float parsed;
            try {
                parsed = Float.parseFloat(typed);
            } catch (NumberFormatException e) {
                return false;
            }
            if (!Float.isFinite(parsed)) return false;

            float clamped = Math.max(typedMin(), Math.min(typedMax(), parsed));
            if (clamped == getter.apply(o)) return false;
            setter.set(o, clamped);
            return true;
        }

        /**
         * Typed values may leave the drag range — swing speed below its 0.1 minimum is the reason
         * this exists — but a range that starts above zero stays positive, since those are scales
         * and speeds that divide.
         */
        private float typedMin() {
            float span = max - min;
            return min > 0f ? 0.001f : min - span * TYPED_RANGE_FACTOR;
        }

        private float typedMax() {
            return max + (max - min) * TYPED_RANGE_FACTOR;
        }

        @Override
        public boolean drag(double mx, int x, int w, ItemOverride o) {
            if (!grabbed) return false;
            applyFromMouse(mx, x, w, o);
            return true;
        }

        public void release() {
            grabbed = false;
        }

        private void applyFromMouse(double mx, int x, int w, ItemOverride o) {
            float t = (float) ((mx - x) / w);
            t = Math.max(0f, Math.min(1f, t));
            setter.set(o, min + t * (max - min));
        }

        @Override
        public Object get(ItemOverride o) {
            return getter.apply(o);
        }

        @Override
        public void set(ItemOverride o, Object value) {
            setter.set(o, (Float) value);
        }

        @Override
        public Object defaultValue() {
            return explicitDefault != null ? explicitDefault : getter.apply(DEFAULTS);
        }

        @Override
        public boolean isFloat() {
            return true;
        }

        @Override
        public float min() {
            return min;
        }

        @Override
        public float max() {
            return max;
        }
    }

    // -- Key bind -----------------------------------------------------------

    /**
     * A row that binds a {@link KeyMapping}.
     * <p>
     * Presentation only: it is never put in a {@code Rows} table, because the share-code registry
     * is built from those and a key is not a settings value anyone would want in a code.
     */
    public static class KeyBind extends Row {
        private static final int BTN_W = 60;
        private static final int BTN_H = 12;

        private final KeyMapping mapping;
        private final Consumer<InputConstants.Key> onBind;
        private boolean listening = false;

        /** @param onBind applies and persists the new key; see {@code FreeCamera.bind}. */
        public KeyBind(String fieldName, KeyMapping mapping, Consumer<InputConstants.Key> onBind) {
            super(fieldName, null);
            this.mapping = mapping;
            this.onBind = onBind;
        }

        public boolean isListening() {
            return listening;
        }

        public void listen() {
            listening = true;
        }

        public void stopListening() {
            listening = false;
        }

        /** Binds {@code key} and stops listening. Escape arrives here as {@code UNKNOWN}, unbinding. */
        public void bind(InputConstants.Key key) {
            listening = false;
            onBind.accept(key);
        }

        @Override
        public void draw(GuiGraphicsExtractor g, int x, int y, int w, ItemOverride o) {
            Draw.text(g, label(), x, y + 2, Theme.current().label());
            int bx = x + w - BTN_W;
            g.fill(bx, y, bx + BTN_W, y + BTN_H,
                    listening ? Theme.current().buttonOn() : Theme.current().button());
            String shown = listening
                    ? "> ... <"
                    : mapping.getTranslatedKeyMessage().getString();
            Draw.centered(g, shown, bx + BTN_W / 2, y + 2, Theme.current().text());
        }

        @Override
        public boolean click(double mx, double my, int x, int y, int w, ItemOverride o) {
            return my >= y && my < y + ROW_HEIGHT && mx >= x + w - BTN_W && mx < x + w;
        }

        @Override
        public Object get(ItemOverride o) {
            return mapping.saveString();
        }

        @Override
        public void set(ItemOverride o, Object value) {
            try {
                bind(InputConstants.getKey((String) value));
            } catch (IllegalArgumentException e) {
                bind(InputConstants.UNKNOWN);
            }
        }

        @Override
        public Object defaultValue() {
            return InputConstants.UNKNOWN.getName();
        }

        @Override
        public boolean isFloat() {
            return false;
        }
    }

    // -- Toggle -------------------------------------------------------------

    public static class Toggle extends Row {
        private final Function<ItemOverride, Boolean> getter;
        private final Setter setter;
        /** Explicit default for rows not backed by {@link ItemOverride}; null to read {@link #DEFAULTS}. */
        private final Boolean explicitDefault;

        public interface Setter {
            void set(ItemOverride o, boolean value);
        }

        public Toggle(String fieldName,
                      Function<ItemOverride, Boolean> getter, Setter setter,
                      Predicate<ItemOverride> visibleWhen) {
            this(fieldName, getter, setter, visibleWhen, null);
        }

        /** @param explicitDefault the default for a row whose getter ignores its argument. */
        public Toggle(String fieldName,
                      Function<ItemOverride, Boolean> getter, Setter setter,
                      Predicate<ItemOverride> visibleWhen, Boolean explicitDefault) {
            super(fieldName, visibleWhen);
            this.getter = getter;
            this.setter = setter;
            this.explicitDefault = explicitDefault;
        }

        @Override
        public void draw(GuiGraphicsExtractor g, int x, int y, int w, ItemOverride o) {
            boolean on = getter.apply(o);
            Draw.text(g, label(), x, y + 2, Theme.current().label());
            int bw = 26;
            int bx = x + w - bw;
            g.fill(bx, y, bx + bw, y + 12, on ? Theme.current().toggleOn() : Theme.current().toggleOff());
            Draw.centered(g, on ? "ON" : "OFF", bx + bw / 2, y + 2, Theme.current().text());
        }

        @Override
        public boolean click(double mx, double my, int x, int y, int w, ItemOverride o) {
            if (my < y || my >= y + ROW_HEIGHT || mx < x || mx >= x + w) return false;
            setter.set(o, !getter.apply(o));
            return true;
        }

        @Override
        public Object get(ItemOverride o) {
            return getter.apply(o);
        }

        @Override
        public void set(ItemOverride o, Object value) {
            setter.set(o, (Boolean) value);
        }

        @Override
        public Object defaultValue() {
            return explicitDefault != null ? explicitDefault : getter.apply(DEFAULTS);
        }

        @Override
        public boolean isFloat() {
            return false;
        }
    }
}
