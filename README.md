# SwingMe

> **SwingMe is a modification to [ScaleMe](https://modrinth.com/mod/scaleme) by Kd_Gaming1**.It is a **separate mod** with its own id
> (`swingme`), its own config files, and its own settings UI. It is not affiliated with, endorsed
> by, or supported by the ScaleMe project. Please do not report SwingMe issues to ScaleMe.
>
> Original project: <https://github.com/KdGaming0/ScaleMe>

**Give every SkyBlock item its own held-item position and swing animation.**

A client-side Fabric mod for Hypixel SkyBlock. Where the original ScaleMe applied one set of hand
and animation settings to everything you held, SwingMe identifies individual SkyBlock items by
their internal UUID. So your **Terminator** can sit differently from your Hyperion and swing at a
different speed, and those settings follow that specific item rather than the item type. Have no
**fear** of switching things up: even if you **drop** an item into another slot, its settings
stay with it.

---

## What SwingMe adds over ScaleMe

- **Per-item settings.** Any SkyBlock item can carry its own copy of every hand and swing
  setting. An item's settings fully replace the global ones while it is in your hand — nothing is
  blended or inherited, so there is never a question of which value won.
- **One in-game overlay instead of a config menu.** A draggable, resizable window that draws over
  live gameplay with no dimming or blur, so you can watch your changes on the actual model as you
  drag a slider. The original config screen is gone; everything lives here.
- **Share codes.** A short text code for your settings that you can paste to a friend. Only
  values you actually changed are encoded.
- **Nametags that scale with your character**, including size applied by other mods.
- **A swing tester**, so you can preview animation settings without hitting anything.

Everything that was in ScaleMe, item and crosshair tweaks, sword
blocking, swing overrides is still here, moved into an overlay.

---

## Using it

Bind **Edit Held Item Animation** under *Options → Controls → Miscellaneous*. It has no default
key. Press it in game to open the overlay; `/swingme` opens the same window.

### The scope switch

The third button along the bottom toggles what you are editing:

| Scope | What it edits |
|---|---|
| **All Items** | The global settings, used by anything without its own. |
| **Held Item** | Only the SkyBlock item currently in your main hand. |

Held Item scope needs an item with a SkyBlock UUID, ordinary Minecraft items cannot be told apart
from one another, so the overlay says so rather than pretending. Nothing is saved for an item
until you actually change something, so browsing your inventory will not fill the file up.


### Tabs

The top row holds three groups. **Animation** opens a second row; the other two do not.

- **Animation**
  - **Hand** — where your arm sits (position, height, separate off-hand values)
  - **Item** — the held item's own scale, offset and rotation
  - **Swing** — animation speed, arc shape, bobbing, repeated-swing behaviour, sword block
  - **Saved** — every item with its own settings; reset one back to your globals, or remove it
- **More** — player, other players, NPC and dropped-item scale, nametag resizing, the
  third-person camera options below, and the crosshair, selfie cam, own nametag and
  hide-players settings
- **Presets** — settings that ship with the mod, ready to apply. Empty for now.

Hover any setting for the same explanation the old config menu gave. **Click the number on any
slider to type an exact value**, including values past the slider's own range — swing speed slides
down to 0.1 but accepts a typed 0.05.

### Camera (in More)

- **Perspective** — third person (F5) stops pulling in close when there's a wall behind you; the
  camera passes through it instead.
- **Free** — enables the **Free Camera** keybind (*Controls → Miscellaneous*, unbound by default).
  Press it and your player stops turning while the mouse swings the view a full 360° around your
  head; the scroll wheel changes camera distance instead of your held item, with no distance limit.
  Press again to return to how you were looking.

### The side drawer

The arrow in the title bar opens a drawer holding **Reset / Undo**, **Copy Code**, **Paste Code**
and the **theme** switch. Reset clears the visible tab only, and turns into Undo so a misclick is
recoverable.

### Share codes

**Copy Code** puts a code on your clipboard; **Paste Code** applies one. `/swingme code` and
`/swingme code <code>` do the same from chat.

Codes are scoped: an All Items code will not apply while you are in Held Item scope, and vice
versa — it is rejected rather than half-applied. Applying a code is a **replacement**, not a
merge: any setting the code does not mention returns to its default, so what you get is what the
sender had.

Codes usually run 15–30 characters. They carry no player or item identity, only setting values.

### Export and import (chat only)

Not to be confused with the **Presets** tab, which holds codes shipped with the mod. Share codes
cover one scope at a time; if you want to move your **global** settings between installs — or hand
someone just your swing animation without the rest — `/swingme export` puts them on your clipboard
as JSON, and `/swingme import <json>` applies them.

| Command | Exports |
|---|---|
| `/swingme export` | Everything |
| `/swingme export hand` | Held item: position, size and angle |
| `/swingme export anim` | Swing animation, including sword block |
| `/swingme export scale` | Sizes, nametag scaling and the camera options |
| `/swingme export view` | Third-person crosshair, selfie cam, hiding players |

Only values you have changed are written, so an export of untouched settings is nearly empty.
Import applies whatever the JSON names and reports how many settings it skipped. These are global
settings only — per-item overrides are not exported, use a Held Item share code for those.

### Swing testing

**Swing** plays one animation. **Repeat** keeps swinging as though you were holding attack on a
block, and carries on after you close the window so you can watch unobstructed. Both are purely
local — no attack packet is sent, so other players see nothing and nothing is hit.

---

## Config files

| File | Holds |
|---|---|
| `config/swingme.json` | Global settings (MidnightLib) |
| `config/swingme_item_overrides.json` | Per-item settings, window position, theme |

SwingMe does not read ScaleMe's config. Installing both at once is possible but pointless — they
hook the same rendering and their effects stack.

---

## Building

Multi-version via [Stonecutter](https://stonecutter.kikugie.dev/): one source tree, two targets.

```bash
./gradlew build                  # all versions: 26.1, 26.2
./gradlew :26.1:build            # one version
./gradlew :26.1:runClient        # dev client
./gradlew buildAndCollect        # jars into build/libs/<version>/
./gradlew "Reset active project" # run before committing
```

Jars land in `versions/<version>/build/libs/`. Requires JDK 25.

---

## License and attribution

GPL-3.0, inherited from ScaleMe and unchanged. The original copyright stands; see
[LICENSE](LICENSE) and [NOTICE.md](NOTICE.md).

If you fork this in turn, keep both attributions.
