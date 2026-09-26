package com.swingme.gui;

import java.util.Locale;

/**
 * The overlay's colour palettes, one per constant, as packed ARGB ints ready for {@code fill()}.
 * <p>
 * Every background here stays semi-transparent because the window is drawn over live gameplay and
 * the game showing through is the whole point of it. That makes legibility backdrop-dependent — a
 * light panel loses contrast over a cave mouth, a dark one loses it over snow — so each palette was
 * checked by compositing its translucent backgrounds over both a near-black and a near-white
 * backdrop and measuring WCAG relative-luminance contrast for text, label, muted text and tooltip
 * text against whatever sits behind them. Every pair clears 4.5:1 over both backdrops except in
 * {@link #DARK}, which is the historical palette reproduced value for value so the default look
 * does not change: its muted text (about 4.4:1 over bright scenery) and the white ON label on its
 * green fills (about 3.3:1) are left exactly as they shipped.
 * <p>
 * The three light palettes carry dark text and draw it without a drop shadow — see
 * {@link #shadow()}. They darken rather than lighten their muted text, because a pale grey that
 * reads as "secondary" on an opaque light panel disappears once the panel is translucent, and they
 * sit at a higher alpha than the dark palettes since a pale panel loses more of itself to bright
 * scenery showing through. Their tooltips deliberately break the theme with a near-black body: a
 * tooltip draws on top of the window, and a pale tooltip over a pale panel reads as one shape.
 * <p>
 * {@link #toggleOn()}, {@link #toggleOff()} and {@link #buttonOn()} are the only signal that a
 * setting is on, so they stay green and red in every palette instead of following the theme hue.
 * The dark palettes use a deeper green than DARK's for no reason other than letting the white ON
 * label sitting on top of it clear the floor.
 */
public enum Theme {

    // Constructor order, repeated as the line grouping below:
    //   panel, titleBar, border, grip
    //   text, mutedText, label
    //   tabActive, tabInactive, scopeActive
    //   button, buttonHover, buttonOn, buttonUndo, remove
    //   sliderTrack, sliderFill, sliderKnob
    //   toggleOn, toggleOff
    //   tipBorder, tipBody, tipText

    DARK(
            0xE01A1A1F, 0xFF2B2B33, 0xFF4A4A57, 0xFF6E6E82,
            0xFFFFFFFF, 0xFF9A9AAB, 0xFFCFCFDA,
            0xFF3D4C6E, 0xFF23232B, 0xFF4C6E4C,
            0xE02E2E38, 0xE04A4A5C, 0xFF4C9E5A, 0xFF6E5A2E, 0xFF8A3A3A,
            0xFF3A3A45, 0xFF5A7FCF, 0xFFE8E8F0,
            0xFF4C9E5A, 0xFF5A3A3A,
            0xFF2B2B4A, 0xF0100014, 0xFFE0E0F0),

    MIDNIGHT(
            0xE00B0F1E, 0xFF141B33, 0xFF2E3A63, 0xFF4A5A8C,
            0xFFFFFFFF, 0xFF98A8CE, 0xFFC2CEEA,
            0xFF2C3E76, 0xFF121729, 0xFF2A6060,
            0xE0181F38, 0xE02A3557, 0xFF20805F, 0xFF7A6428, 0xFF8C3050,
            0xFF1E263F, 0xFF4A7BE0, 0xFFDCE6FF,
            0xFF20805F, 0xFF5A2E44,
            0xFF44559A, 0xF0243060, 0xFFDCE6FF),

    SLATE(
            0xE01E1E1E, 0xFF2E2E2E, 0xFF4C4C4C, 0xFF787878,
            0xFFFFFFFF, 0xFFA8A8A8, 0xFFD6D6D6,
            0xFF585858, 0xFF262626, 0xFF6E6E6E,
            0xE0333333, 0xE04A4A4A, 0xFF3F7C4C, 0xFF7A6A3A, 0xFF8A4040,
            0xFF3A3A3A, 0xFF8C8C8C, 0xFFEDEDED,
            0xFF3F7C4C, 0xFF6A3838,
            0xFF5A5A5A, 0xF00A0A0A, 0xFFE6E6E6),

    LIGHT(
            0xF0F2F2F6, 0xFFE2E2EA, 0xFF8E8E9C, 0xFF9A9AA8,
            0xFF1A1A22, 0xFF5A5A66, 0xFF33333F,
            0xFFAFC2E8, 0xFFDCDCE4, 0xFFA8D8AE,
            0xF0E4E4EC, 0xF0CFCFDC, 0xFF6FC383, 0xFFE9C77A, 0xFFE08A8A,
            0xFFC8C8D4, 0xFF4E79D0, 0xFF2A2A36,
            0xFF5CB073, 0xFFE4A0A0,
            0xFF6A6A7A, 0xF01E1E28, 0xFFF2F2F8),

    PASTEL_PINK(
            0xF0FBEDF4, 0xFFF3D8E6, 0xFFD9A8C2, 0xFFC28FA8,
            0xFF32202B, 0xFF7A5468, 0xFF4C3242,
            0xFFF0BFD6, 0xFFEBDCE4, 0xFFCDE7C8,
            0xF0F5E2EC, 0xF0EBCBDD, 0xFF74C486, 0xFFEDC97F, 0xFFE58A9C,
            0xFFE6CEDA, 0xFFD8618F, 0xFF5E3850,
            0xFF5CAE74, 0xFFEDA5AE,
            0xFF8A4463, 0xF0361F2A, 0xFFFDEFF5),

    PASTEL_BLUE(
            0xF0EAF3FC, 0xFFD5E5F5, 0xFFA2BFDD, 0xFF89A6C4,
            0xFF16212C, 0xFF4E6272, 0xFF2C3E50,
            0xFFB3D2F0, 0xFFDAE6F2, 0xFFC2E5CC,
            0xF0E1EDF8, 0xF0C8DCEE, 0xFF62BC7E, 0xFFE9C77F, 0xFFE0868F,
            0xFFCADCEC, 0xFF3F82C8, 0xFF27394C,
            0xFF52A96C, 0xFFE89BA3,
            0xFF35597E, 0xF0182634, 0xFFF0F7FF);

    /** Which palette the overlay draws with. Not persisted here; the config layer owns that. */
    private static Theme current = DARK;

    private final int panel;
    private final int titleBar;
    private final int border;
    private final int grip;
    private final int text;
    private final int mutedText;
    private final int label;
    private final int tabActive;
    private final int tabInactive;
    private final int scopeActive;
    private final int button;
    private final int buttonHover;
    private final int buttonOn;
    private final int buttonUndo;
    private final int remove;
    private final int sliderTrack;
    private final int sliderFill;
    private final int sliderKnob;
    private final int toggleOn;
    private final int toggleOff;
    private final int tipBorder;
    private final int tipBody;
    private final int tipText;

    /**
     * Whether text in this palette is drawn with a drop shadow.
     * <p>
     * Minecraft's shadow is a darkened copy of the glyph offset by a pixel. Under light text on
     * a dark panel that reads as depth; under dark text on a light panel it is a dark smear
     * behind dark glyphs, which looks like a rendering fault rather than a style. Derived from
     * the text colour instead of declared per palette so a new theme cannot forget it.
     */
    public boolean shadow() {
        return luminance(text) > 0.5f;
    }

    /** Perceptual luminance of a packed colour, ignoring alpha. 0 is black, 1 is white. */
    private static float luminance(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        return 0.2126f * r + 0.7152f * g + 0.0722f * b;
    }

    /** Derived from {@link #name()} so a new palette cannot forget to declare its key. */
    private final String key = "swingme.tune.theme." + name().toLowerCase(Locale.ROOT);

    Theme(int panel, int titleBar, int border, int grip,
          int text, int mutedText, int label,
          int tabActive, int tabInactive, int scopeActive,
          int button, int buttonHover, int buttonOn, int buttonUndo, int remove,
          int sliderTrack, int sliderFill, int sliderKnob,
          int toggleOn, int toggleOff,
          int tipBorder, int tipBody, int tipText) {
        this.panel = panel;
        this.titleBar = titleBar;
        this.border = border;
        this.grip = grip;
        this.text = text;
        this.mutedText = mutedText;
        this.label = label;
        this.tabActive = tabActive;
        this.tabInactive = tabInactive;
        this.scopeActive = scopeActive;
        this.button = button;
        this.buttonHover = buttonHover;
        this.buttonOn = buttonOn;
        this.buttonUndo = buttonUndo;
        this.remove = remove;
        this.sliderTrack = sliderTrack;
        this.sliderFill = sliderFill;
        this.sliderKnob = sliderKnob;
        this.toggleOn = toggleOn;
        this.toggleOff = toggleOff;
        this.tipBorder = tipBorder;
        this.tipBody = tipBody;
        this.tipText = tipText;
    }

    // -- Chrome -------------------------------------------------------------

    /** Translucent in every palette: the world has to stay readable behind the window. */
    public int panel() {
        return panel;
    }

    public int titleBar() {
        return titleBar;
    }

    public int border() {
        return border;
    }

    /** Louder than the border on purpose — the corner marks are the only resize affordance. */
    public int grip() {
        return grip;
    }

    // -- Type ---------------------------------------------------------------

    public int text() {
        return text;
    }

    public int mutedText() {
        return mutedText;
    }

    public int label() {
        return label;
    }

    // -- Bars ---------------------------------------------------------------

    public int tabActive() {
        return tabActive;
    }

    public int tabInactive() {
        return tabInactive;
    }

    /** Green-leaning everywhere so the edit scope never reads as just another selected tab. */
    public int scopeActive() {
        return scopeActive;
    }

    // -- Buttons ------------------------------------------------------------

    public int button() {
        return button;
    }

    public int buttonHover() {
        return buttonHover;
    }

    public int buttonOn() {
        return buttonOn;
    }

    /** Warm, so an available undo is noticeable without reading the button's label. */
    public int buttonUndo() {
        return buttonUndo;
    }

    public int remove() {
        return remove;
    }

    // -- Slider -------------------------------------------------------------

    public int sliderTrack() {
        return sliderTrack;
    }

    public int sliderFill() {
        return sliderFill;
    }

    /** Separates from both track and fill, since it is dragged across the whole width. */
    public int sliderKnob() {
        return sliderKnob;
    }

    // -- Toggles ------------------------------------------------------------

    public int toggleOn() {
        return toggleOn;
    }

    public int toggleOff() {
        return toggleOff;
    }

    // -- Tooltip ------------------------------------------------------------

    public int tipBorder() {
        return tipBorder;
    }

    /** Separates from the panel it overlaps rather than matching it, so tooltips read as on top. */
    public int tipBody() {
        return tipBody;
    }

    public int tipText() {
        return tipText;
    }

    // -- Selection ----------------------------------------------------------

    /** @return this palette's lang key, e.g. {@code swingme.tune.theme.dark}. */
    public String key() {
        return key;
    }

    public static Theme current() {
        return current;
    }

    public static void set(Theme theme) {
        if (theme != null) current = theme;
    }

    /**
     * Advances to the next palette, wrapping past the last one.
     *
     * @return the palette now in use.
     */
    public static Theme next() {
        Theme[] all = values();
        current = all[(current.ordinal() + 1) % all.length];
        return current;
    }

    /**
     * Resolves a saved palette name, ignoring case.
     * <p>
     * Falls back to {@link #DARK} for an unknown, empty or null name rather than throwing: this
     * reads a config file a user can hand-edit, and a typo there should cost the theme, not the
     * screen.
     */
    public static Theme byName(String name) {
        if (name == null) return DARK;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return DARK;
        for (Theme theme : values()) {
            if (theme.name().equalsIgnoreCase(trimmed)) return theme;
        }
        return DARK;
    }
}
