package com.swingme.gui;

import com.swingme.config.ItemOverride;
import com.swingme.config.ItemOverrideStore;
import com.swingme.util.FreeCamera;
import com.swingme.util.ShareCode;
import com.swingme.util.SwingTester;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The mod's only settings UI: a draggable, resizable, background-free overlay.
 * <p>
 * Draws its own widgets rather than extending
 * {@link net.minecraft.client.gui.components.AbstractWidget}, whose draw method was renamed in 26.1.
 * <p>
 * Widths are fixed rather than proportional. The window resizes, but tabs and buttons keep a
 * constant size and simply reflow — a button that grows with the window ends up enormous and
 * hard to aim at, and the row area is capped for the same reason.
 */
public class ItemTuneScreen extends Screen {

    public static final int TITLE_BAR_H = 16;
    private static final int TAB_BAR_H = 14;
    private static final int FOOTER_H = 24;

    /** Fixed control metrics. Nothing here scales with the window. */
    private static final int TAB_W = 54;
    private static final int BTN_W = 64;
    private static final int BTN_H = 16;
    private static final int BTN_PAD = 6;
    /** Rows stop widening past this, so sliders stay aimable on a wide window. */
    private static final int CONTENT_MAX_W = 320;

    private static final int MIN_W = 220;
    private static final int MIN_H = 170;
    /** Thickness of the corner grab zones, in pixels. */
    private static final int RESIZE_GRIP = 6;

    private static final int CONTENT_PAD = 6;
    private static final int SCROLL_STEP = 12;

    private static final int SAVED_ROW_H = 20;
    private static final int SMALL_BTN_W = 44;
    private static final int SMALL_BTN_H = 12;

    /** The expanding side drawer holding the actions that are not needed every second. */
    private static final int PANEL_W = 84;
    private static final int PANEL_GAP = 2;
    private static final int PANEL_BTN_W = PANEL_W - 8;
    private static final int PANEL_ROWS = 5;
    private static final int PANEL_H = PANEL_ROWS * (BTN_H + 4) + 4;
    /** Width of the square arrow button in the title bar that opens the drawer. */
    private static final int ARROW_W = 12;

    private static final int TIP_MAX_W = 220;

    /** How long a copy/paste result stays on screen. */
    private static final long STATUS_MS = 3000L;

    /** Typed into the drawer's unlabelled button to reveal the camera settings. */
    private static final String SECRET = "FREE";

    /** Persisted between openings; written to disk by {@link ItemOverrideStore}. */
    public static int windowX = 40;
    public static int windowY = 40;
    public static int windowW = 236;
    public static int windowH = 250;

    /** Whether the side drawer is expanded. Persisted. */
    private static boolean panelOpen = false;

    /** The top tab row. Only {@link Group#ANIMATIONS} has a second row beneath it. */
    public enum Group {
        ANIMATIONS("swingme.tune.tab.animations"),
        MORE("swingme.tune.tab.scale"),
        PRESETS("swingme.tune.tab.presets");

        public final String key;

        Group(String key) {
            this.key = key;
        }
    }

    /** The second row, shown only while {@link Group#ANIMATIONS} is selected. */
    public enum Tab {
        HAND("swingme.tune.tab.position"),
        ITEM("swingme.tune.tab.item"),
        SWING("swingme.tune.tab.swing"),
        SAVED("swingme.tune.tab.saved");

        public final String key;

        Tab(String key) {
            this.key = key;
        }
    }

    /**
     * The only place the free-camera key can be bound, since it is kept out of the vanilla
     * controls list. Shown whether or not the camera settings themselves are unlocked.
     */
    private static final Row.KeyBind FREE_CAMERA_BIND =
            new Row.KeyBind("freeCameraKey", FreeCamera.KEY, FreeCamera::bind);

    /**
     * The Sizes and Camera rows share one tab, so this is the pair read back to back, with the
     * bind row between them where the camera settings appear once unlocked.
     * <p>
     * A view over the two tables, deliberately not a table of its own: {@code ShareCode}'s
     * registry is built from {@code Rows.SCALE} and {@code Rows.VIEW} in that order, and every
     * code in circulation is indexed by it. Merging the tabs is presentation only, which is also
     * what lets the bind row sit here without entering that registry.
     */
    private static final List<Row> MORE_ROWS = Stream.of(
            Rows.SCALE.stream(), Stream.of((Row) FREE_CAMERA_BIND), Rows.VIEW.stream())
            .flatMap(stream -> stream)
            .toList();

    /**
     * Stand-in target for the More rows, which read {@code SwingMeConfig} and ignore this
     * argument. The tab is reachable in held-item scope now, where the real target is null
     * whenever the held item carries no SkyBlock UUID.
     */
    private static final ItemOverride GLOBAL_ONLY_TARGET = new ItemOverride();

    /** Both remembered across openings so you land back where you were. */
    private static Group activeGroup = Group.ANIMATIONS;
    private static Tab activeTab = Tab.HAND;

    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private boolean resizing = false;
    /** Which edge each axis is anchored to while resizing: -1 = min side, +1 = max side. */
    private int resizeEdgeX = 0;
    private int resizeEdgeY = 0;
    private int resizeStartMouseX, resizeStartMouseY;
    private int resizeStartX, resizeStartY, resizeStartW, resizeStartH;

    private int scroll = 0;
    /** The row currently being dragged, or null. */
    private Row draggedRow = null;
    /** The row whose value is being typed, or null. */
    private Row.Slider editingRow = null;
    /** The key-bind row waiting for a keypress, or null. */
    private Row.KeyBind listeningRow = null;

    /** Non-null while the drawer's code is being typed; holds what has been typed so far. */
    private String secretBuffer = null;

    /** Rebuilt every frame while drawing; drawn last so it sits above the window. */
    private String hoverTooltip = "";

    /** Values captured by the last Reset, keyed by field name; empty when there is nothing to undo. */
    private final Map<String, Object> resetSnapshot = new HashMap<>();
    private Group resetGroup = null;
    private Tab resetTab = null;
    /** What the snapshot belongs to, so an undo cannot land on a different item. */
    private String resetKey = "";

    private String status = "";
    private long statusUntil = 0L;

    /** Screen to return to on close, or null to drop straight back into the game. */
    private final Screen parent;

    protected ItemTuneScreen(Screen parent) {
        super(Component.literal("SwingMe"));
        this.parent = parent;
    }

    /** Opens over the world, as the keybind and the command do. */
    public static void open() {
        open(null);
    }

    /** @param parent the screen to restore on close, for the mod list entry. */
    public static void open(Screen parent) {
        Minecraft client = Minecraft.getInstance();
        EditScope.reseedGlobal();
        client.setScreen(new ItemTuneScreen(parent));
    }

    /** Restores the drawer state saved on disk. Called once at startup. */
    public static void setPanelOpen(boolean open) {
        panelOpen = open;
    }

    /** Keeps the world ticking so the live animation preview keeps running in singleplayer. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Also runs on window resize, so a saved size larger than the screen gets pulled back in. */
    @Override
    protected void init() {
        windowW = Math.max(MIN_W, Math.min(windowW, this.width));
        windowH = Math.max(MIN_H, Math.min(windowH, this.height));
        windowX = clamp(windowX, 0, this.width - windowW);
        windowY = clamp(windowY, 0, this.height - windowH);
    }

    @Override
    public void onClose() {
        commitEdit();
        cancelCapture();
        ItemOverrideStore.setWindowPos(windowX, windowY);
        ItemOverrideStore.setWindowSize(windowW, windowH);
        ItemOverrideStore.setPanelOpen(panelOpen);
        ItemOverrideStore.setTheme(Theme.current().name());
        EditScope.writeNow();
        if (parent == null) {
            super.onClose();
            return;
        }
        Minecraft client = Minecraft.getInstance();
        client.setScreen(parent);
    }

    // -- Background ---------------------------------------------------------
    // Deliberately empty: the game must stay fully visible behind the window.

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {}

    // -- Drawing ------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        hoverTooltip = "";
        drawWindow(g, mouseX, mouseY);
        if (panelOpen) drawPanel(g, mouseX, mouseY);
        if (!hoverTooltip.isEmpty()) {
            drawTooltip(g, hoverTooltip, mouseX, mouseY);
        }
    }

    private void drawWindow(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Theme t = Theme.current();
        int x = windowX;
        int y = windowY;

        g.fill(x - 1, y - 1, x + windowW + 1, y + windowH + 1, t.border());
        g.fill(x, y, x + windowW, y + windowH, t.panel());
        g.fill(x, y, x + windowW, y + TITLE_BAR_H, t.titleBar());

        String title = EditScope.title();
        Draw.text(g, title.isEmpty() ? I18n.get("swingme.tune.title") : title, x + 5, y + 4, t.text());

        drawArrow(g, mouseX, mouseY);
        drawTabs(g);

        if (showingSaved()) {
            drawSaved(g, mouseX, mouseY);
        } else if (showingPresets()) {
            drawCentredNotice(g, I18n.get("swingme.tune.noPresets"));
        } else {
            drawContent(g, mouseX, mouseY);
        }

        drawFooter(g, mouseX, mouseY);
        drawResizeGrips(g);
    }

    private void drawArrow(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Theme t = Theme.current();
        int ax = arrowX();
        int ay = windowY + 2;
        boolean hover = inRect(mouseX, mouseY, ax, ay, ARROW_W, ARROW_W);
        g.fill(ax, ay, ax + ARROW_W, ay + ARROW_W, hover ? t.buttonHover() : t.button());
        Draw.centered(g, panelOpen ? "<" : ">", ax + ARROW_W / 2, ay + 2, t.text());
        if (hover) hoverTooltip = I18n.get("swingme.tune.panel.tooltip");
    }

    private void drawTabs(GuiGraphicsExtractor g) {
        List<String> groupKeys = new ArrayList<>();
        for (Group group : Group.values()) groupKeys.add(group.key);
        drawTabBand(g, groupKeys, activeGroup.ordinal(), tabBarTop());

        if (!showsTabBand()) return;

        List<String> tabKeys = new ArrayList<>();
        for (Tab tab : Tab.values()) tabKeys.add(tab.key);
        drawTabBand(g, tabKeys, activeTab.ordinal(), tabBandTop());
    }

    /** @param activeIndex the entry to highlight, or -1 for none. */
    private void drawTabBand(GuiGraphicsExtractor g, List<String> keys, int activeIndex, int top) {
        Theme t = Theme.current();
        int per = perRow(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            int tx = windowX + (i % per) * TAB_W;
            int ty = top + (i / per) * TAB_BAR_H;
            boolean active = i == activeIndex;
            g.fill(tx, ty, tx + TAB_W, ty + TAB_BAR_H, active ? t.tabActive() : t.tabInactive());
            Draw.centered(g, I18n.get(keys.get(i)), tx + TAB_W / 2, ty + 3, t.text());
        }
    }

    private void drawContent(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        ItemOverride o = rowTarget();
        if (o == null) {
            drawCentredNotice(g, I18n.get("swingme.tune.noHeldItem"));
            return;
        }

        g.enableScissor(windowX, contentTop(), windowX + windowW, contentBottom());
        int y = contentTop() - scroll;
        for (Row row : rows()) {
            if (!row.visible(o)) continue;
            if (y + Row.ROW_HEIGHT >= contentTop() && y <= contentBottom()) {
                row.draw(g, rowX(), y, rowW(), o);
                if (isOverContent(mouseX, mouseY)
                        && mouseY >= y && mouseY < y + Row.ROW_HEIGHT
                        && mouseX >= rowX() && mouseX < rowX() + rowW()) {
                    hoverTooltip = row.tooltip();
                }
            }
            y += Row.ROW_HEIGHT;
        }
        g.disableScissor();
    }

    /** Wrapped message centred in the content area, for the "nothing to edit" states. */
    private void drawCentredNotice(GuiGraphicsExtractor g, String message) {
        List<String> lines = wrap(message, rowW());
        int lineHeight = Draw.font().lineHeight + 2;
        int y = (contentTop() + contentBottom()) / 2 - lines.size() * lineHeight / 2;
        for (String line : lines) {
            Draw.centered(g, line, windowX + windowW / 2, y, Theme.current().mutedText());
            y += lineHeight;
        }
    }

    private void drawSaved(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Theme t = Theme.current();
        List<Map.Entry<String, ItemOverride>> entries = savedEntries();
        if (entries.isEmpty()) {
            drawCentredNotice(g, I18n.get("swingme.tune.noSaved"));
            return;
        }

        String heldUuid = EditScope.heldUuid();
        g.enableScissor(windowX, contentTop(), windowX + windowW, contentBottom());
        int y = contentTop() - scroll;
        for (Map.Entry<String, ItemOverride> entry : entries) {
            if (y + SAVED_ROW_H >= contentTop() && y <= contentBottom()) {
                boolean current = entry.getKey().equals(heldUuid);
                String name = entry.getValue().displayName;
                Draw.text(g, current ? "> " + name : name, rowX(), y + 5, t.text());

                int bx2 = rowX() + rowW() - SMALL_BTN_W;
                int bx1 = bx2 - SMALL_BTN_W - 3;
                g.fill(bx1, y + 3, bx1 + SMALL_BTN_W, y + 3 + SMALL_BTN_H, t.button());
                Draw.centered(g, I18n.get("swingme.tune.reset"), bx1 + SMALL_BTN_W / 2, y + 5, t.text());
                g.fill(bx2, y + 3, bx2 + SMALL_BTN_W, y + 3 + SMALL_BTN_H, t.remove());
                Draw.centered(g, I18n.get("swingme.tune.remove"), bx2 + SMALL_BTN_W / 2, y + 5, t.text());

                if (isOverContent(mouseX, mouseY) && mouseY >= y && mouseY < y + SAVED_ROW_H) {
                    if (mouseX >= bx1 && mouseX < bx1 + SMALL_BTN_W) {
                        hoverTooltip = I18n.get("swingme.tune.reset.tooltip");
                    } else if (mouseX >= bx2 && mouseX < bx2 + SMALL_BTN_W) {
                        hoverTooltip = I18n.get("swingme.tune.remove.tooltip");
                    }
                }
            }
            y += SAVED_ROW_H;
        }
        g.disableScissor();
    }

    /** Swing, Repeat and the scope switch — the three things you reach for constantly. */
    private void drawFooter(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Theme t = Theme.current();
        int by = footerTop() + 4;

        int sx = footerButtonX(0);
        boolean sHover = inRect(mouseX, mouseY, sx, by, BTN_W, BTN_H);
        g.fill(sx, by, sx + BTN_W, by + BTN_H, sHover ? t.buttonHover() : t.button());
        Draw.centered(g, I18n.get("swingme.tune.swing"), sx + BTN_W / 2, by + 4, t.text());

        int rx = footerButtonX(1);
        boolean repeating = SwingTester.isRepeating();
        boolean rHover = inRect(mouseX, mouseY, rx, by, BTN_W, BTN_H);
        int rColor = repeating ? t.buttonOn() : (rHover ? t.buttonHover() : t.button());
        g.fill(rx, by, rx + BTN_W, by + BTN_H, rColor);
        Draw.centered(g, I18n.get(repeating ? "swingme.tune.pause" : "swingme.tune.repeat"),
                rx + BTN_W / 2, by + 4, t.text());

        // Scope reads as an on/off switch: lit means "this item only".
        int cx = footerButtonX(2);
        boolean held = EditScope.mode() == EditScope.Mode.HELD_ITEM;
        boolean cHover = inRect(mouseX, mouseY, cx, by, BTN_W, BTN_H);
        int cColor = held ? t.scopeActive() : (cHover ? t.buttonHover() : t.button());
        g.fill(cx, by, cx + BTN_W, by + BTN_H, cColor);
        Draw.centered(g, I18n.get(held ? "swingme.tune.scope.held" : "swingme.tune.scope.all"),
                cx + BTN_W / 2, by + 4, t.text());

        if (sHover) hoverTooltip = I18n.get("swingme.tune.swing.tooltip");
        else if (rHover) hoverTooltip = I18n.get("swingme.tune.repeat.tooltip");
        else if (cHover) hoverTooltip = I18n.get(held ? "swingme.tune.scope.held.tooltip"
                                                     : "swingme.tune.scope.all.tooltip");

        if (!status.isEmpty() && Util.getMillis() < statusUntil) {
            Draw.centered(g, status, windowX + windowW / 2, footerTop() - 11, t.mutedText());
        }
    }

    /** The side drawer: reset, share codes and the theme switch. */
    private void drawPanel(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Theme t = Theme.current();
        int px = panelX();
        int py = panelY();

        g.fill(px - 1, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, t.border());
        g.fill(px, py, px + PANEL_W, py + PANEL_H, t.panel());

        boolean canUndo = canUndo();
        boolean typing = secretBuffer != null;
        String[] labels = {
                I18n.get(canUndo ? "swingme.tune.undo" : "swingme.tune.resetTab"),
                I18n.get("swingme.tune.copyCode"),
                I18n.get("swingme.tune.pasteCode"),
                I18n.get(Theme.current().key()),
                typing ? secretBuffer + "_" : I18n.get("swingme.tune.secret")
        };
        String[] tips = {
                canUndo ? "swingme.tune.undo.tooltip" : "swingme.tune.resetTab.tooltip",
                "swingme.tune.copyCode.tooltip",
                "swingme.tune.pasteCode.tooltip",
                "swingme.tune.theme.tooltip",
                "swingme.tune.secret.tooltip"
        };

        for (int i = 0; i < PANEL_ROWS; i++) {
            int bx = px + 4;
            int by = panelButtonY(i);
            boolean hover = inRect(mouseX, mouseY, bx, by, PANEL_BTN_W, BTN_H);
            int color = (i == 0 && canUndo) || (i == 4 && typing)
                    ? t.buttonUndo()
                    : (hover ? t.buttonHover() : t.button());
            g.fill(bx, by, bx + PANEL_BTN_W, by + BTN_H, color);
            Draw.centered(g, labels[i], bx + PANEL_BTN_W / 2, by + 4, t.text());
            if (hover) hoverTooltip = I18n.get(tips[i]);
        }
    }

    /** Small marks so the draggable corners are discoverable. */
    private void drawResizeGrips(GuiGraphicsExtractor g) {
        int c = Theme.current().grip();
        int x0 = windowX;
        int y0 = windowY;
        int x1 = windowX + windowW;
        int y1 = windowY + windowH;

        g.fill(x1 - RESIZE_GRIP, y1 - 2, x1, y1, c);
        g.fill(x1 - 2, y1 - RESIZE_GRIP, x1, y1, c);
        g.fill(x0, y1 - 2, x0 + RESIZE_GRIP, y1, c);
        g.fill(x0, y1 - RESIZE_GRIP, x0 + 2, y1, c);
        g.fill(x1 - RESIZE_GRIP, y0, x1, y0 + 2, c);
        g.fill(x1 - 2, y0, x1, y0 + RESIZE_GRIP, c);
        g.fill(x0, y0, x0 + RESIZE_GRIP, y0 + 2, c);
        g.fill(x0, y0, x0 + 2, y0 + RESIZE_GRIP, c);
    }

    private void drawTooltip(GuiGraphicsExtractor g, String text, int mouseX, int mouseY) {
        Theme t = Theme.current();
        List<String> lines = wrap(text, TIP_MAX_W);
        if (lines.isEmpty()) return;

        int w = 0;
        for (String line : lines) w = Math.max(w, Draw.width(line));
        int lineHeight = Draw.font().lineHeight + 1;
        int h = lines.size() * lineHeight;

        int x = mouseX + 10;
        int y = mouseY - 4;
        if (x + w + 4 > this.width) x = Math.max(4, mouseX - w - 12);
        if (y + h + 4 > this.height) y = this.height - h - 4;
        if (y < 4) y = 4;

        g.fill(x - 4, y - 4, x + w + 4, y + h + 3, t.tipBorder());
        g.fill(x - 3, y - 3, x + w + 3, y + h + 2, t.tipBody());

        int ty = y;
        for (String line : lines) {
            Draw.text(g, line, x, ty, t.tipText());
            ty += lineHeight;
        }
    }

    /** Splits on explicit newlines first, then wraps each paragraph on word boundaries. */
    private static List<String> wrap(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            if (paragraph.isEmpty()) {
                out.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && Draw.width(candidate) > maxWidth) {
                    out.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    // -- Geometry -----------------------------------------------------------

    private int arrowX() {
        return windowX + windowW - ARROW_W - 3;
    }

    private int tabBarTop() {
        return windowY + TITLE_BAR_H;
    }

    /** How many fixed-width tabs fit across the current window, at least one. */
    private int perRow(int count) {
        return Math.max(1, Math.min(count, windowW / TAB_W));
    }

    private int bandRows(int count) {
        int per = perRow(count);
        return (count + per - 1) / per;
    }

    private int groupBandRows() {
        return bandRows(Group.values().length);
    }

    /** The second row exists only under Animations, so it contributes nothing elsewhere. */
    private int tabBandRows() {
        return showsTabBand() ? bandRows(Tab.values().length) : 0;
    }

    private int tabBandTop() {
        return tabBarTop() + groupBandRows() * TAB_BAR_H;
    }

    private int tabBarHeight() {
        return (groupBandRows() + tabBandRows()) * TAB_BAR_H;
    }

    private int contentTop() {
        return tabBarTop() + tabBarHeight() + CONTENT_PAD;
    }

    private int footerTop() {
        return windowY + windowH - FOOTER_H;
    }

    private int contentBottom() {
        return footerTop() - 2;
    }

    private int rowX() {
        return windowX + CONTENT_PAD;
    }

    private int rowW() {
        return Math.min(windowW - CONTENT_PAD * 2, CONTENT_MAX_W);
    }

    private int footerButtonX(int index) {
        return windowX + BTN_PAD + index * (BTN_W + BTN_PAD);
    }

    /** Drawer sits to the right, or flips to the left when the screen edge is in the way. */
    private int panelX() {
        int right = windowX + windowW + PANEL_GAP;
        if (right + PANEL_W + 1 <= this.width) return right;
        return Math.max(1, windowX - PANEL_W - PANEL_GAP);
    }

    private int panelY() {
        return Math.min(windowY + TITLE_BAR_H, Math.max(1, this.height - PANEL_H - 1));
    }

    private int panelButtonY(int index) {
        return panelY() + 4 + index * (BTN_H + 4);
    }

    // -- Model --------------------------------------------------------------

    private boolean showsTabBand() {
        return activeGroup == Group.ANIMATIONS;
    }

    private boolean showingSaved() {
        return showsTabBand() && activeTab == Tab.SAVED;
    }

    private boolean showingPresets() {
        return activeGroup == Group.PRESETS;
    }

    /**
     * What the rows on the active tab should read and write.
     *
     * @return the edit scope's target, or the global stand-in on More, whose rows never look at
     *         it. Null only where a real item is genuinely required and none is held.
     */
    private ItemOverride rowTarget() {
        return activeGroup == Group.MORE ? GLOBAL_ONLY_TARGET : EditScope.target();
    }

    private List<Row> rows() {
        return switch (activeGroup) {
            case MORE -> MORE_ROWS;
            case PRESETS -> List.of();
            case ANIMATIONS -> switch (activeTab) {
                case HAND -> Rows.POSITION;
                case ITEM -> Rows.ITEM;
                case SWING -> Rows.SWING;
                case SAVED -> List.of();
            };
        };
    }

    /** Rebuilt each frame; the store is small and this keeps the list always current. */
    private List<Map.Entry<String, ItemOverride>> savedEntries() {
        return new ArrayList<>(ItemOverrideStore.all().entrySet());
    }

    // -- Reset / undo -------------------------------------------------------

    /** Identifies what is being edited: the global settings, or one specific item. */
    private static String targetKey() {
        return EditScope.mode() == EditScope.Mode.ALL_ITEMS ? "@global" : EditScope.heldUuid();
    }

    private boolean canUndo() {
        return resetGroup == activeGroup && resetTab == activeTab
                && resetKey.equals(targetKey()) && !resetSnapshot.isEmpty();
    }

    /**
     * Captures every row on this tab — hidden dependents included, so a reset really does clear
     * the tab — then writes the defaults.
     */
    private void resetActiveTab() {
        ItemOverride o = rowTarget();
        if (o == null) return;
        // Saved and Presets carry no rows, so there is nothing to reset and nothing to report.
        if (rows().isEmpty()) return;

        resetSnapshot.clear();
        for (Row row : rows()) {
            resetSnapshot.put(row.fieldName(), row.get(o));
            row.reset(o);
        }
        resetGroup = activeGroup;
        resetTab = activeTab;
        resetKey = targetKey();
        afterEdit();
        setStatus(I18n.get("swingme.tune.status.reset"));
    }

    private void undoReset() {
        ItemOverride o = rowTarget();
        if (o == null) return;

        for (Row row : rows()) {
            Object previous = resetSnapshot.get(row.fieldName());
            if (previous != null) row.set(o, previous);
        }
        clearUndo();
        afterEdit();
        setStatus(I18n.get("swingme.tune.status.undone"));
    }

    private void clearUndo() {
        resetSnapshot.clear();
        resetGroup = null;
        resetTab = null;
        resetKey = "";
    }

    // -- Share codes --------------------------------------------------------

    private void copyCode() {
        ItemOverride o = EditScope.target();
        if (o == null) {
            setStatus(I18n.get("swingme.tune.status.noTarget"));
            return;
        }
        String code = ShareCode.encode(o, EditScope.mode() == EditScope.Mode.HELD_ITEM);
        Minecraft.getInstance().keyboardHandler.setClipboard(code);
        setStatus(I18n.get("swingme.tune.status.copied", code.length()));
    }

    private void pasteCode() {
        ItemOverride o = EditScope.target();
        if (o == null) {
            setStatus(I18n.get("swingme.tune.status.noTarget"));
            return;
        }
        String code = Minecraft.getInstance().keyboardHandler.getClipboard();
        ShareCode.Result result =
                ShareCode.apply(code, o, EditScope.mode() == EditScope.Mode.HELD_ITEM);
        if (!result.ok) {
            setStatus(I18n.get("swingme.tune.status.badCode", result.error));
            return;
        }
        clearUndo();
        afterEdit();
        setStatus(I18n.get("swingme.tune.status.applied", result.applied));
    }

    /** Applies whatever is in the active value editor, if there is one. */
    private void commitEdit() {
        if (editingRow == null) return;
        Row.Slider slider = editingRow;
        editingRow = null;

        ItemOverride o = rowTarget();
        if (o != null && slider.commitEdit(o)) {
            afterEdit();
        } else {
            slider.cancelEdit();
        }
    }

    /**
     * Numbers are typed through key codes rather than {@code charTyped}, which the 26.1 screen API
     * no longer exposes. Only the characters a number is made of are accepted anyway.
     * <p>
     * The shortcut tests come from {@code InputWithModifiers}, so Ctrl and Cmd are handled the
     * same way vanilla text fields handle them.
     */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listeningRow != null) {
            Row.KeyBind bind = listeningRow;
            listeningRow = null;
            // Escape unbinds rather than cancelling, matching the vanilla controls screen.
            bind.bind(event.key() == GLFW.GLFW_KEY_ESCAPE
                    ? InputConstants.UNKNOWN
                    : InputConstants.getKey(event));
            return true;
        }

        if (secretBuffer != null) return typeSecret(event);

        if (event.isCycleFocus()) {
            cycleField(event.hasShiftDown());
            return true;
        }

        if (editingRow == null) return super.keyPressed(event);

        if (event.isSelectAll()) {
            editingRow.selectAll();
            return true;
        }
        if (event.isCopy()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(editingRow.editText());
            return true;
        }

        int key = event.key();
        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                commitEdit();
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                editingRow.cancelEdit();
                editingRow = null;
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                editingRow.backspace();
                return true;
            }
            default -> {
                char typed = charFor(key);
                if (typed != 0) {
                    editingRow.typeChar(typed);
                    return true;
                }
                return true;
            }
        }
    }

    /** @return the character this key contributes to a number, or 0 when it contributes none. */
    private static char charFor(int key) {
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return (char) ('0' + key - GLFW.GLFW_KEY_0);
        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) return (char) ('0' + key - GLFW.GLFW_KEY_KP_0);
        if (key == GLFW.GLFW_KEY_PERIOD || key == GLFW.GLFW_KEY_KP_DECIMAL) return '.';
        if (key == GLFW.GLFW_KEY_MINUS || key == GLFW.GLFW_KEY_KP_SUBTRACT) return '-';
        return 0;
    }

    /** Accumulates the drawer code. An exact match unlocks immediately, with no Enter needed. */
    private boolean typeSecret(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            cancelCapture();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!secretBuffer.isEmpty()) {
                secretBuffer = secretBuffer.substring(0, secretBuffer.length() - 1);
            }
            return true;
        }
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z
                && secretBuffer.length() < SECRET.length()) {
            secretBuffer += (char) ('A' + key - GLFW.GLFW_KEY_A);
            if (secretBuffer.equals(SECRET)) {
                secretBuffer = null;
                ItemOverrideStore.setSecretUnlocked(true);
                setStatus(I18n.get("swingme.tune.status.unlocked"));
            }
        }
        return true;
    }

    /** Reached only from a drawer click, which has already dropped any other pending edit. */
    private void beginSecret() {
        secretBuffer = "";
    }

    /** Drops a half-typed code and a pending bind, the way clicking away from a field does. */
    private void cancelCapture() {
        secretBuffer = null;
        if (listeningRow != null) {
            listeningRow.stopListening();
            listeningRow = null;
        }
    }

    // -- Field focus --------------------------------------------------------

    /** Moves the typing cursor to the next value field on this tab, wrapping at either end. */
    private void cycleField(boolean backwards) {
        List<Row.Slider> fields = editableFields();
        if (fields.isEmpty()) {
            commitEdit();
            return;
        }

        int current = editingRow == null ? -1 : fields.indexOf(editingRow);
        commitEdit();

        int next = backwards
                ? (current <= 0 ? fields.size() - 1 : current - 1)
                : (current + 1) % fields.size();

        ItemOverride o = rowTarget();
        if (o == null) return;
        Row.Slider slider = fields.get(next);
        slider.beginEdit(o);
        editingRow = slider;
        scrollIntoView(slider);
    }

    /** Only sliders carry a value field; toggles and the bind row have nothing to type into. */
    private List<Row.Slider> editableFields() {
        List<Row.Slider> out = new ArrayList<>();
        ItemOverride o = rowTarget();
        if (o == null) return out;
        for (Row row : rows()) {
            if (row instanceof Row.Slider slider && row.visible(o)) out.add(slider);
        }
        return out;
    }

    /** Scrolls {@code target} into the content area, so tabbing never focuses an unseen field. */
    private void scrollIntoView(Row target) {
        ItemOverride o = rowTarget();
        if (o == null) return;

        int offset = 0;
        for (Row row : rows()) {
            if (!row.visible(o)) continue;
            if (row == target) break;
            offset += Row.ROW_HEIGHT;
        }

        int viewHeight = contentBottom() - contentTop();
        if (offset < scroll) {
            scroll = offset;
        } else if (offset + Row.ROW_HEIGHT > scroll + viewHeight) {
            scroll = offset + Row.ROW_HEIGHT - viewHeight;
        }
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    private void setStatus(String message) {
        status = message;
        statusUntil = Util.getMillis() + STATUS_MS;
    }

    /**
     * Persists whatever the last interaction changed. More's rows write {@code SwingMeConfig}
     * themselves and are reachable in either scope, so they take the global path regardless of
     * what the scope switch says.
     */
    private void afterEdit() {
        if (activeGroup == Group.MORE) {
            EditScope.markGlobalDirty();
        } else {
            EditScope.markDirty();
        }
    }

    // -- Input --------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != 0) return super.mouseClicked(event, doubled);

        double mx = event.x();
        double my = event.y();

        // Clicking anywhere else accepts what was typed, the way a text field loses focus.
        commitEdit();
        cancelCapture();

        // The drawer floats outside the window, so it gets first refusal.
        if (panelOpen && clickPanel(mx, my)) return true;

        if (inRect(mx, my, arrowX(), windowY + 2, ARROW_W, ARROW_W)) {
            panelOpen = !panelOpen;
            ItemOverrideStore.setPanelOpen(panelOpen);
            return true;
        }

        // Corners win over the title bar, which overlaps the two top grips.
        int ex = resizeEdgeXAt(mx, my);
        int ey = resizeEdgeYAt(mx, my);
        if (ex != 0 && ey != 0) {
            resizing = true;
            resizeEdgeX = ex;
            resizeEdgeY = ey;
            resizeStartMouseX = (int) mx;
            resizeStartMouseY = (int) my;
            resizeStartX = windowX;
            resizeStartY = windowY;
            resizeStartW = windowW;
            resizeStartH = windowH;
            return true;
        }

        if (clickFooter(mx, my)) return true;

        if (isOverTitleBar(mx, my)) {
            dragging = true;
            dragOffsetX = (int) mx - windowX;
            dragOffsetY = (int) my - windowY;
            return true;
        }

        if (isOverTabBar(mx, my)) {
            clickTabBar(mx, my);
            return true;
        }

        if (isOverContent(mx, my)) {
            if (showingSaved()) {
                return clickSaved(mx, my);
            }
            ItemOverride o = rowTarget();
            if (o != null) {
                int y = contentTop() - scroll;
                for (Row row : rows()) {
                    if (!row.visible(o)) continue;
                    if (row instanceof Row.Slider slider
                            && slider.clickedValue(mx, my, rowX(), y, rowW())) {
                        slider.beginEdit(o);
                        editingRow = slider;
                        return true;
                    }
                    if (row instanceof Row.KeyBind bind
                            && bind.click(mx, my, rowX(), y, rowW(), o)) {
                        bind.listen();
                        listeningRow = bind;
                        return true;
                    }
                    if (row.click(mx, my, rowX(), y, rowW(), o)) {
                        draggedRow = row;
                        afterEdit();
                        return true;
                    }
                    y += Row.ROW_HEIGHT;
                }
            }
        }

        return super.mouseClicked(event, doubled);
    }

    /** Routes a tab-bar click to whichever band it landed in. */
    private void clickTabBar(double mx, double my) {
        boolean inGroupBand = my < tabBarTop() + groupBandRows() * TAB_BAR_H;

        if (inGroupBand) {
            int i = bandIndexAt(mx, my, tabBarTop(), Group.values().length);
            if (i < 0 || Group.values()[i] == activeGroup) return;
            activeGroup = Group.values()[i];
        } else {
            if (!showsTabBand()) return;
            int i = bandIndexAt(mx, my, tabBandTop(), Tab.values().length);
            if (i < 0 || Tab.values()[i] == activeTab) return;
            activeTab = Tab.values()[i];
        }
        clearUndo();
        scroll = 0;
    }

    /** @return the index within a band under the cursor, or -1 when the click missed. */
    private int bandIndexAt(double mx, double my, int bandTop, int count) {
        int per = perRow(count);
        int col = (int) ((mx - windowX) / TAB_W);
        int row = (int) ((my - bandTop) / TAB_BAR_H);
        int index = row * per + col;
        return (col >= 0 && col < per && index >= 0 && index < count) ? index : -1;
    }

    private boolean clickFooter(double mx, double my) {
        int by = footerTop() + 4;
        for (int i = 0; i < 3; i++) {
            if (!inRect(mx, my, footerButtonX(i), by, BTN_W, BTN_H)) continue;
            switch (i) {
                case 0 -> SwingTester.swing();
                case 1 -> SwingTester.toggleRepeat();
                default -> toggleScope();
            }
            return true;
        }
        return false;
    }

    private void toggleScope() {
        EditScope.setMode(EditScope.mode() == EditScope.Mode.ALL_ITEMS
                ? EditScope.Mode.HELD_ITEM
                : EditScope.Mode.ALL_ITEMS);
        clearUndo();
        scroll = 0;
    }

    private boolean clickPanel(double mx, double my) {
        int bx = panelX() + 4;
        for (int i = 0; i < PANEL_ROWS; i++) {
            if (!inRect(mx, my, bx, panelButtonY(i), PANEL_BTN_W, BTN_H)) continue;
            switch (i) {
                case 0 -> {
                    if (canUndo()) undoReset();
                    else resetActiveTab();
                }
                case 1 -> copyCode();
                case 2 -> pasteCode();
                case 3 -> {
                    Theme.set(Theme.next());
                    ItemOverrideStore.setTheme(Theme.current().name());
                }
                default -> beginSecret();
            }
            return true;
        }
        // Swallow clicks anywhere on the drawer so they never fall through to the world.
        return inRect(mx, my, panelX(), panelY(), PANEL_W, PANEL_H);
    }

    private boolean clickSaved(double mx, double my) {
        int y = contentTop() - scroll;
        int bx2 = rowX() + rowW() - SMALL_BTN_W;
        int bx1 = bx2 - SMALL_BTN_W - 3;

        for (Map.Entry<String, ItemOverride> entry : savedEntries()) {
            if (my >= y + 3 && my < y + 3 + SMALL_BTN_H) {
                if (mx >= bx1 && mx < bx1 + SMALL_BTN_W) {
                    // Reset: re-snapshot the current global values, keeping the item listed.
                    ItemOverrideStore.put(entry.getKey(),
                            ItemOverride.fromGlobal(entry.getValue().displayName));
                    return true;
                }
                if (mx >= bx2 && mx < bx2 + SMALL_BTN_W) {
                    ItemOverrideStore.remove(entry.getKey());
                    clearUndo();
                    return true;
                }
            }
            y += SAVED_ROW_H;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (resizing) {
            applyResize((int) event.x(), (int) event.y());
            return true;
        }

        if (dragging) {
            windowX = clamp((int) event.x() - dragOffsetX, 0, this.width - windowW);
            windowY = clamp((int) event.y() - dragOffsetY, 0, this.height - windowH);
            return true;
        }

        if (draggedRow != null) {
            ItemOverride o = rowTarget();
            if (o != null && draggedRow.drag(event.x(), rowX(), rowW(), o)) {
                afterEdit();
                return true;
            }
        }

        return super.mouseDragged(event, deltaX, deltaY);
    }

    private void applyResize(int mouseX, int mouseY) {
        int dx = mouseX - resizeStartMouseX;
        int dy = mouseY - resizeStartMouseY;

        if (resizeEdgeX > 0) {
            windowW = clamp(resizeStartW + dx, MIN_W, this.width - resizeStartX);
        } else {
            int newW = clamp(resizeStartW - dx, MIN_W, resizeStartX + resizeStartW);
            windowX = resizeStartX + resizeStartW - newW;
            windowW = newW;
        }

        if (resizeEdgeY > 0) {
            windowH = clamp(resizeStartH + dy, MIN_H, this.height - resizeStartY);
        } else {
            int newH = clamp(resizeStartH - dy, MIN_H, resizeStartY + resizeStartH);
            windowY = resizeStartY + resizeStartH - newH;
            windowH = newH;
        }

        scroll = Math.min(scroll, maxScroll());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean handled = false;
        if (resizing) {
            resizing = false;
            ItemOverrideStore.setWindowSize(windowW, windowH);
            ItemOverrideStore.setWindowPos(windowX, windowY);
            handled = true;
        }
        if (dragging && event.button() == 0) {
            dragging = false;
            handled = true;
        }
        if (draggedRow instanceof Row.Slider slider) {
            slider.release();
            handled = true;
        }
        draggedRow = null;
        if (handled) return true;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (isOverContent(mouseX, mouseY)) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (deltaY * SCROLL_STEP)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    // -- Hit tests ----------------------------------------------------------

    /** @return -1 on the left grip, +1 on the right grip, 0 elsewhere. */
    private int resizeEdgeXAt(double mx, double my) {
        if (my < windowY - 1 || my > windowY + windowH + 1) return 0;
        if (mx >= windowX - 1 && mx <= windowX + RESIZE_GRIP) return -1;
        if (mx >= windowX + windowW - RESIZE_GRIP && mx <= windowX + windowW + 1) return 1;
        return 0;
    }

    /** @return -1 on the top grip, +1 on the bottom grip, 0 elsewhere. */
    private int resizeEdgeYAt(double mx, double my) {
        if (mx < windowX - 1 || mx > windowX + windowW + 1) return 0;
        if (my >= windowY - 1 && my <= windowY + RESIZE_GRIP) return -1;
        if (my >= windowY + windowH - RESIZE_GRIP && my <= windowY + windowH + 1) return 1;
        return 0;
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean isOverTitleBar(double mx, double my) {
        return mx >= windowX && mx < windowX + windowW
                && my >= windowY && my < windowY + TITLE_BAR_H;
    }

    private boolean isOverTabBar(double mx, double my) {
        return mx >= windowX && mx < windowX + windowW
                && my >= tabBarTop() && my < tabBarTop() + tabBarHeight();
    }

    private boolean isOverContent(double mx, double my) {
        return mx >= windowX && mx < windowX + windowW
                && my >= contentTop() && my < contentBottom();
    }

    private int maxScroll() {
        int contentHeight;
        if (showingSaved()) {
            contentHeight = savedEntries().size() * SAVED_ROW_H;
        } else {
            ItemOverride o = rowTarget();
            if (o == null) return 0;
            int visible = 0;
            for (Row row : rows()) {
                if (row.visible(o)) visible++;
            }
            contentHeight = visible * Row.ROW_HEIGHT;
        }
        return Math.max(0, contentHeight - (contentBottom() - contentTop()));
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }
}
