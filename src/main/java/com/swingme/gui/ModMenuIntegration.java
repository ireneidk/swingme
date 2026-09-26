package com.swingme.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Points ModMenu's config button at the overlay.
 * <p>
 * MidnightLib registers a config screen for every mod that calls
 * {@code MidnightConfig.init}, and this mod still calls it because MidnightLib owns the
 * settings file. Declaring our own {@code modmenu} entrypoint takes that button back, so the
 * overlay is the only settings UI.
 * <p>
 * ModMenu is an optional dependency: this class is only loaded when ModMenu itself queries
 * the entrypoint, so nothing here runs when it is absent.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // The overlay draws no background, so the mod list stays visible behind it, and
        // closing returns to the list rather than dumping the player into the world.
        return parent -> {
            EditScope.reseedGlobal();
            return new ItemTuneScreen(parent);
        };
    }
}
