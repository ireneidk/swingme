# NOTICE

SwingMe is a derivative work of **ScaleMe** by **Kd_Gaming1**.

- Original project: <https://github.com/KdGaming0/ScaleMe>
- Original on Modrinth: <https://modrinth.com/mod/scaleme>
- Original license: GNU General Public License v3.0

SwingMe is published with the original author's permission and remains licensed under the GNU
General Public License v3.0. The original copyright is not superseded by this fork.

## Relationship to the original

SwingMe is a separate mod, not a drop-in replacement:

- The mod id changed from `scaleme` to `swingme`, so both can be installed side by side, no reason to.
  They hook the same rendering and their effects stack.
- Config files are separate. SwingMe does not read or migrate ScaleMe's config.
- Version numbering restarts at 1.0.0 and does not continue ScaleMe's release history.
- The Modrinth and CurseForge project ids of the original have been removed from this repository,
  so a publish run cannot upload over the original author's listings.

## What this fork changed

Substantially rewritten:

- Per-item settings keyed by SkyBlock item UUID, replacing the single global set.
- The MidnightLib config screen was removed and replaced with an in-game overlay.
- Share codes, themes, per-tab reset/undo, and a local swing tester were added.
- Nametags scale with the rendered character size, including scale applied by other mods.

## Third-party

Required at runtime or bundled, each under its own license:

- [Fabric API](https://github.com/FabricMC/fabric) and Fabric Loader — Apache-2.0
- [MidnightLib](https://github.com/TeamMidnightDust/MidnightLib) — MIT
- [hm-api](https://github.com/AzureAaron/hm-api) (Hypixel Mod API) — see project
- [MixinExtras](https://github.com/LlamaLad7/MixinExtras) — MIT
