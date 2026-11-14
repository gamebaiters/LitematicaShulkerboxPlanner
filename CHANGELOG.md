# Changelog

## 2.1.4 - Minecraft 26.3

**The shulker boxes follow the material list, every time**
- Ignoring an item in Litematica's material list, clearing the ignored items, or refreshing the list (Litematica's *Refresh* button, the planner's *Refresh materials* button, the "scheduled task" of a placement list) now updates the shulker boxes right away, with a message telling how many items changed. Before, as soon as Litematica's material list screen had been opened, the planner stopped hearing about the refresh (the screen takes the list's completion listener) and ignoring an item was never noticed: "scheduled task added", then nothing changed.
- A locked plan is updated at once: boxes marked done stay exactly as they are; boxes being filled (or linked) lose the items that aren't needed at all anymore (removed or ignored) and take in the new ones in their free slots; if less of an item is needed but it's still needed (count mode *Missing* while building), what's already packed stays. A started box left with nothing in it is dropped.

## 2.1.3 - Minecraft 26.3

- Boxes being filled take in the items added to the material list (partial stacks and free slots) before new boxes are made.

## 2.1.2 - Minecraft 26.3

- The panel next to a shulker box no longer shows the state of a box linked to another shulker box.
- The HUD outside containers stays on the last linked box opened.
- Optional JEI integration: click an item of the panel for its recipes (right click: uses), R / U and bookmarks work on it.

## 2.1.1 - Minecraft 26.3

**Fixes**
- The buttons of the packing assistant panel did nothing, and the panel could not be moved or resized: Minecraft 26.3 numbers the left mouse button differently, and the panel ignored it. Only the mouse wheel worked.
- The panel now gets the mouse before other mods (malilib, Item Scroller, Tweakeroo, Mouse Tweaks…), so they can't take a click meant for it; moving and resizing follow the mouse every frame and end when the button is released.
- The recipe button of the Raw materials tab went to the previous recipe on left click: left click is now the next recipe, right click the previous one.
- The game tests now also drive the panel with the real mouse (through Minecraft's input handling), with and without Item Scroller, Tweakeroo, Mouse Tweaks and MiniHUD installed.

## 2.1.0 - Minecraft 26.3

**Packing assistant panel**
- Anchored to the shulker box screen: by default right next to it, top aligned and as tall as it, whatever the GUI scale.
- Drag it by its title bar and resize it from the bottom right corner: the place is saved relative to the shulker box screen, so it's the same every time you open a box (and it follows the screen at other GUI scales). The ↺ button in the title bar puts it back.
- The list of items scrolls with the mouse wheel or by dragging the scroll bar, so even a box with many item types fits on small screens. Hover an item for the details.
- See-through background, with the opacity in *Settings* (*Assistant opacity*, 10-100%).
- A panel moved over the shulker box screen takes the clicks, so nothing behind it is picked up by mistake, and no slot tooltip shows through it.
- *Follow on HUD* is now the ◉ button in the title bar; the state (Preview / Linked / Done) is next to the progress bar.

**3D buttons, nothing overlapping**
- Every button of the planner, of the panel and of the HUD editor is now a 3D button: lit top, thick bottom edge, pressed in when it's the current page or a switch that is on. The selected page of the HUD editor no longer looks disabled.
- Fixed overlapping elements: the progress in the planner header could cover the "Shulker Planner" title (top left) with short schematic names, the *Lock plan* / *Update plan* button was drawn under its banner, the hotbar showed through the summary at the bottom, and in Litematica's material list the *Shulker Planner* button covered the summary text on small screens (it's now in the top row of buttons when there is room).
- On short screens (GUI scale 4 on 1080p) the informational banner is hidden to leave room for the list; warnings and banners with a button are always shown.
- Every screen is checked automatically at GUI scales 2, 3 and 4 for overlapping or cut off texts, icons and buttons.

## 2.0.0 - Minecraft 26.3

**Correct numbers**
- The plan no longer changes on its own. As soon as you start working on it (mark a box, link a shulker box) or press *Lock plan*, the boxes keep their number and contents. When the material list changes, a banner tells you what changed and *Update plan* repacks only the boxes you haven't started.
- Fixed the counts shrinking while packing: the old *To gather* mode subtracted the items already packed in your boxes from the plan itself, so boxes were renumbered and their state was lost. The plan is now made from *Total* or *Missing* only.
- The Materials tab shows, for every material: **Needed**, **Packed** (in boxes done or being filled), **Carrying** (inventory, bundles and shulker boxes not linked to the plan) and **To get**, with a progress bar. Nothing is counted twice.
- With a multiplier, the copy you're building counts what's missing and the other copies count in full (before, the missing counts were ignored).
- Entries hidden ("ignored") in Litematica's material list are left out (can be turned off).

**Shulker boxes linked by hand**
- Opening a shulker box doesn't change anything anymore: no box is picked for you, the HUD doesn't jump, nothing gets reset.
- The packing assistant shows a preview of the suggested box; choose the box with ◀ ▶ and press *Link*. From then on that shulker box *is* that box.
- The link stays when you break the shulker box, carry it around and place it somewhere else: it's recognised by color, name and contents, and the new position is taken over.
- A linked box being filled is shown on the HUD with what's already inside, and marked done when it matches the plan. *Unlink*, *Mark done* and *Follow on HUD* are on the panel and on every box card.
- The old automatic behaviour is still available with *Auto-link opened boxes* (off by default).

**Raw materials**
- New *Raw materials* page: everything on the material list broken down into what you have to gather (cobblestone, logs, sand, raw iron...), with every crafting step in order: how many crafts, at which station, which ingredients, leftovers.
- Recipes come from the world in singleplayer (data packs and mods included) and from the game on servers.
- Turn furnace and stonecutter recipes on or off, pick another recipe for an item, or keep an item as it is instead of breaking it down.
- *Pack raw items* plans the shulker boxes with the raw materials instead of the finished blocks.

**Warnings you can turn off**
- New *Settings* page with every warning and highlight on its own switch: items to remove, red/orange/green slot highlights, highlights on unlinked boxes, HUD wrong items, the unlinked box hint, the "box complete" message, the outdated plan banner. Plus *Enable all* / *Disable all*. They are also in the configuration screen, in the new *Warnings* category.

**New look**
- New planner layout: header with the overall progress and the plan state (Draft / Locked / Raw), side bar with the pages, toolbars with the main options first and *More options* for the rest, flat buttons and switches, cards for the boxes with a colored state stripe and a state label, progress bars everywhere.
- Redesigned packing assistant panel. Every state uses a color *and* a label or icon.

## 1.2.0 - Minecraft 26.3

- Every schematic now has its own HUD style, saved with the project.
- New HUD editor (`M + J` or *Customize HUD* in the planner), with three pages:
  - **Look:** custom title (empty uses the schematic name), 7 themes, every color editable (hex or click the swatch to cycle a palette), background opacity.
  - **Elements:** turn every part of the HUD on or off: title, progress, current box, box totals, 9x3 box view, color legend, missing items list, items to remove, status line, reminders. Quick views: Full, Shulker box, List only, Minimal, Reminders only.
  - **Layout:** size from 50% to 250%, compact mode, padding, text shadow, side bar, border, how many items and reminders to show, stacks or plain numbers.
- Resize the HUD by dragging its bottom right corner or with the mouse wheel over it.
- *Use as style for new projects* saves the current style as the default for new schematics.
- The old global HUD options are carried over to the default style on the first start.

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
