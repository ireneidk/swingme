package com.swingme.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The vanilla graphics class was renamed in 26.1; a Stonecutter regex swap keeps the type
 * below correct on every target, so no manual branch is needed here.
 */
public final class Draw {

    private Draw() {}

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    /**
     * Left-aligned text. The drop shadow follows the active theme: see
     * {@link Theme#shadow()} for why a light palette must not have one.
     */
    public static void text(GuiGraphicsExtractor g, String s, int x, int y, int argb) {
        boolean shadow = Theme.current().shadow();
        g.text(font(), s, x, y, argb, shadow);
    }

    /**
     * Text horizontally centred on {@code cx}.
     * <p>
     * Centres by hand rather than calling the vanilla centred helper, whose convenience
     * overload always draws a shadow and gives no way to turn it off.
     */
    public static void centered(GuiGraphicsExtractor g, String s, int cx, int y, int argb) {
        text(g, s, cx - width(s) / 2, y, argb);
    }

    /** Width of {@code s} in pixels. */
    public static int width(String s) {
        return font().width(s);
    }
}
