# Changelog

## 1.1.1 - Minecraft 26.3

- Fixed the planner always opening the same schematic. The `M + B` hotkey and the Litematica main menu button now use the placement selected in Litematica. Before, they used the first material list Litematica had remembered.
- The HUD only shows schematics that are loaded in the game (placed or loaded in Litematica). Unloading a schematic hides it.
- Selecting a placement switches the HUD right away. A selected placement without a plan shows a hint to press `M + B`, instead of the previous schematic.

## 1.1.0 - Minecraft 26.3

- The HUD shows the box you're filling as a real shulker box interface (9x3), each slot colored by state:
  inside, in your inventory ready to put in, partially there, missing.
- Three HUD views: Shulker box, Compact (only what's missing) and both. Amounts can be shown as stacks or as plain numbers.
- The HUD tracks the contents of the box live while it's open, keeps them after closing it,
  and also recognizes a partially filled box carried in the inventory.
- Inventory changes are reflected on the HUD every tick.
- The HUD can be dragged anywhere with the mouse (Move HUD button or `M + J`). The view and counts can be switched from the same screen.
- The HUD title is the name of the schematic.
- The HUD follows the placement selected in Litematica when it already has a saved project.
- New hotkeys: move the HUD, cycle the HUD view.

## 1.0.0 - Minecraft 26.3

First release.

- Shulker box plan from any Litematica material list (schematic, placement or area).
- Three packing modes: Smart, Sequential and One item per box.
- Options: full stacks, filling empty slots with spares, and box colors.
- Materials page: include/exclude items, gathered flags, count modes, multiplier and search.
- Reminders page with notes for materials and boxes.
- Packing assistant on the shulker box screen, with slot highlights and automatic completion.
- In-game HUD: progress, the current box with the missing items, and open reminders.
- Detection of filled boxes in the inventory.
- Creative mode box creation.
- Text export.
- English and Italian translations.
