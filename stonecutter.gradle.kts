plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "2.1.+" apply false
}

stonecutter active "26.1"

// See https://stonecutter.kikugie.dev/wiki/config/params
//
// Version differences are handled entirely by the textual replacements below; `src/` carries no
// `//?` conditional comments, so swaps, constants and dependency predicates have nothing to bind
// to and are deliberately not declared.
stonecutter parameters {
    replacements {
        string(current.parsed >= "26.2") {
            replace("client.setScreen(", "client.gui.setScreen(")
            replace(
                "import net.minecraft.world.entity.EntityType;",
                "import net.minecraft.world.entity.EntityType;\nimport net.minecraft.world.entity.EntityTypes;"
            )
            // 26.2 renamed the first-person render path render* -> submit* (signatures unchanged)
            replace("\"renderArmWithItem\"", "\"submitArmWithItem\"")
            replace("\"renderHandsWithItems\"", "\"submitHandsWithItems\"")
            // 26.2 split Gui: HUD rendering moved to the new Hud class, Gui now manages screens
            replace("import net.minecraft.client.gui.Gui;", "import net.minecraft.client.gui.Hud;")
            replace("@Mixin(Gui.class)", "@Mixin(Hud.class)")
        }
        regex(current.parsed >= "26.2") {
            replace(
                "(?<!\\.)\\bclient\\.screen\\b" to "client.gui.screen()",
                "\\bclient\\.gui\\.screen\\(\\)" to "client.screen"
            )
            replace(
                "\\bEntityType\\.([A-Z][A-Z0-9_]*)\\b" to "EntityTypes.$1",
                "\\bEntityTypes\\.([A-Z][A-Z0-9_]*)\\b" to "EntityType.$1"
            )
        }
        regex(current.parsed >= "26.1") {
            // GuiGraphics was renamed GuiGraphicsExtractor in 26.1. The lookahead keeps a
            // second forward pass from producing GuiGraphicsExtractorExtractor.
            replace(
                "\\bGuiGraphics\\b(?!Extractor)" to "GuiGraphicsExtractor",
                "\\bGuiGraphicsExtractor\\b" to "GuiGraphics"
            )
        }
    }
}

val releaseVersions = listOf("26.1", "26.2")

stonecutter tasks {
    order("publishMods")
}

tasks.register("publishToAllPlatforms") {
    group       = "publishing"
    description = "Publish all release groups to Modrinth and CurseForge sequentially."
    dependsOn(releaseVersions.map { ":$it:publishMods" })
}

gradle.projectsEvaluated {
    releaseVersions.zipWithNext().forEach { (prev, next) ->
        project(":$next").tasks.matching { it.name == "publishMods" }.configureEach {
            mustRunAfter(project(":$prev").tasks.matching { it.name == "publishMods" })
        }
    }
}
