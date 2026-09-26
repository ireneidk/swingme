package com.swingme.command;

import com.swingme.config.ItemOverride;
import com.swingme.config.SwingMeConfig;
import com.swingme.gui.EditScope;
import com.swingme.gui.ItemTuneScreen;
import com.swingme.util.PresetManager;
import com.swingme.util.ShareCode;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;

public class Commands {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(literal("swingme")
                .executes(Commands::executeOpenConfig)
                .then(literal("config")
                        .executes(Commands::executeOpenConfig))
                .then(literal("export")
                        .executes(ctx -> executeExport(ctx, null))
                        .then(argument("category", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("hand");
                                    builder.suggest("anim");
                                    builder.suggest("scale");
                                    builder.suggest("view");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String cat = StringArgumentType.getString(ctx, "category");
                                    String resolved = resolveCategory(cat);
                                    if (resolved == null) {
                                        sendError(ctx, "Unknown category '" + cat
                                                + "'. Use hand, anim, scale or view — or no category to export everything.");
                                        return 0;
                                    }
                                    return executeExport(ctx, resolved);
                                })))
                .then(literal("import")
                        .then(argument("json", StringArgumentType.greedyString())
                                .executes(Commands::executeImport)))
                .then(literal("code")
                        .executes(Commands::executeCopyCode)
                        .then(argument("code", StringArgumentType.greedyString())
                                .executes(Commands::executeApplyCode)))
        ));
    }

    /**
     * Opens the settings overlay.
     * Uses client.schedule() to delay opening until after the chat closes.
     */
    private static int executeOpenConfig(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            sendError(ctx, "You must be in-game to open the settings.");
            return 0;
        }

        client.schedule(ItemTuneScreen::open);

        ctx.getSource().sendFeedback(Component.literal("§a[SwingMe] Opening settings..."));
        return 1;
    }

    /** Copies a share code for whatever the overlay is currently scoped to. */
    private static int executeCopyCode(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft client = Minecraft.getInstance();
        ItemOverride target = EditScope.target();
        if (target == null) {
            sendError(ctx, "Hold a SkyBlock item, or switch the overlay to All Items.");
            return 0;
        }

        String code = ShareCode.encode(target, EditScope.mode() == EditScope.Mode.HELD_ITEM);
        client.keyboardHandler.setClipboard(code);
        ctx.getSource().sendFeedback(Component.literal(
                "§a[SwingMe] §fCopied to clipboard: §e" + code));
        return 1;
    }

    private static int executeApplyCode(CommandContext<FabricClientCommandSource> ctx) {
        ItemOverride target = EditScope.target();
        if (target == null) {
            sendError(ctx, "Hold a SkyBlock item, or switch the overlay to All Items.");
            return 0;
        }

        ShareCode.Result result = ShareCode.apply(StringArgumentType.getString(ctx, "code"), target,
                EditScope.mode() == EditScope.Mode.HELD_ITEM);
        if (!result.ok) {
            sendError(ctx, "That code could not be read: " + result.error);
            return 0;
        }

        EditScope.markDirty();
        ctx.getSource().sendFeedback(Component.literal(
                "§a[SwingMe] §fApplied §e" + result.applied + "§f settings."));
        return 1;
    }

    private static int executeExport(CommandContext<FabricClientCommandSource> ctx, String category) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            sendError(ctx, "You must be in-game to export presets.");
            return 0;
        }

        ctx.getSource().sendFeedback(PresetManager.exportToChat(category));
        return 1;
    }

    private static int executeImport(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            sendError(ctx, "You must be in-game to import presets.");
            return 0;
        }

        String json = StringArgumentType.getString(ctx, "json");
        String result = PresetManager.importFromJson(json);
        ctx.getSource().sendFeedback(Component.literal(result));
        return 1;
    }

    /**
     * Maps a user-facing category name to the internal MidnightConfig category constant.
     *
     * @return null when the name is not recognised; callers must reject rather than fall through
     *         to a full export, which is what {@code null} means to {@link #executeExport}.
     */
    private static String resolveCategory(String input) {
        return switch (input.toLowerCase()) {
            case "hand" -> SwingMeConfig.HAND;
            case "anim", "animation", "swing" -> SwingMeConfig.ANIM;
            // "item"/"ground" kept as aliases — dropped items moved into the Sizes category
            case "scale", "scaling", "size", "sizes", "item", "ground" -> SwingMeConfig.SCALE;
            case "view", "camera" -> SwingMeConfig.VIEW;
            default -> null;
        };
    }

    private static void sendError(CommandContext<FabricClientCommandSource> ctx, String message) {
        ctx.getSource().sendError(Component.literal("§c[SwingMe] " + message));
    }
}