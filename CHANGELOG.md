# Changelog

## 1.0.0

First release of SwingMe, forked from ScaleMe 3.3.1 by Kd_Gaming1 with permission. Version
numbering restarts here and does not continue ScaleMe's history.

### New

- **Per-item settings.** Any SkyBlock item can carry its own hand position and swing animation.
  An item's settings completely replace your global ones while you are holding it.
- **In-game overlay.** A draggable, resizable settings window that draws over live gameplay
  without dimming it, so you can see your changes on the real model while you make them. Opened
  with a keybind (Controls → Miscellaneous, unbound by default) or `/swingme`.
- **All Items / Held Item switch** for choosing what you are editing.
- **Share codes.** Copy a short code for your settings and paste it to a friend, from the overlay
  or with `/swingme code`.
- **Six themes**, including three light ones.
- **Presets.** Four ready-made hand and swing setups on the Presets tab: Diggin me, base-swing,
  Far-chop and Cropper. Click one with Held Item selected to apply it to the item in your hand.
- **Reset and Undo**, per tab.
- **Swing tester** with a repeat mode, so you can preview animation settings without attacking.
- **Nametags scale with your character**, and follow size applied by other mods such as Odin.
- **Typed values.** Click the number on any slider to enter an exact value, including past the
  slider's range — swing speed drags down to 0.1 but accepts a typed 0.05.
- **Stop Item Swaying With the Camera.** Holds the held item still while you turn, instead of
  letting it lag behind the view. Under More; it applies to every item rather than per item.
- **Keyboard editing in number fields.** Ctrl+A selects the typed value, Ctrl+C copies it, and
  Tab moves to the next slider on the tab (Shift+Tab goes back), scrolling it into view.
- **Scaled heads aim where you aim.** Shrinking yourself used to leave your head pointing along a
  line that passes above whatever you were actually aiming at, because the model scales about your
  feet while the aim ray still leaves your full-height eye. Your head now faces the aim point.
  Applies to your own model in third person only, and does nothing at normal size.

### Changed

- The old config menu is gone. Everything is in the overlay now, including the size and camera
  settings.
- The **Sizes** tab is now **More**, since it also holds the camera options.
- **Tabs are now two rows.** The top row is Animation, More and Presets; picking Animation opens
  Hand, Item, Swing and Saved beneath it.
- **View has been folded into More**, so the crosshair, selfie cam, own-nametag and hide-players
  settings live alongside the sizes. Resetting More now clears all of them at once; Undo still
  takes one press.
- Nametag resizing previously only moved the tag up and down without changing its size. It now
  scales properly.
- **More is always available.** It used to disappear while the scope switch was on Held Item,
  even though its settings were never per-item. It now stays put in both scopes.

### Removed

- **Villager scaling.** It was the one size setting that never went through the shared scale
  resolver, and it had no purpose on SkyBlock. Share codes made before this change are refused
  rather than misread: removing the setting shifted how later ones are numbered, so the code
  format moved to v2. Per-item codes are unaffected.
- The original mod's Modrinth and CurseForge project ids, so this fork cannot publish over them.
- Support for Minecraft 1.21.10 and 1.21.11. SwingMe targets current SkyBlock only; supported
  versions are 26.1 (26.1.2) and 26.2.
