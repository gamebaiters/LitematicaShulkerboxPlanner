# Generates the language files. Run from the project root: python tools/gen_lang.py
import json, os

P = "shulkerplanner."
it = {}
en = {}

def t(key, i, e):
    it[P + key] = i
    en[P + key] = e

# Enums
t("count_mode.total", "Totale", "Total")
t("count_mode.total.hover", "Tutti gli item della schematica (x moltiplicatore)", "All the items of the schematic (x multiplier)")
t("count_mode.missing", "Mancanti", "Missing")
t("count_mode.missing.hover", "Solo i blocchi ancora da piazzare (lista per placement).\nCon moltiplicatore > 1 conta il totale.", "Only the blocks still to be placed (placement material list).\nWith a multiplier > 1 the total is used.")
t("count_mode.to_gather", "Da procurare", "To gather")
t("count_mode.to_gather.hover", "Mancanti meno quelli che hai già nell'inventario\n(anche dentro shulker box e bundle)", "Missing minus what you already have in your inventory\n(including inside shulker boxes and bundles)")
t("pack_mode.smart", "Intelligente", "Smart")
t("pack_mode.smart.hover", "Shulker piene di un solo item per le grandi quantità,\npoi i resti raggruppati spezzando meno item possibile.\nSempre il numero minimo di shulker.", "Full single item boxes for big amounts, then the\nleftovers grouped while splitting as few items as possible.\nAlways the minimum amount of boxes.")
t("pack_mode.sequential", "In ordine", "Sequential")
t("pack_mode.sequential.hover", "Tutti gli stack in ordine di categoria,\nuna shulker dopo l'altra. Numero minimo di shulker.", "All stacks in category order, one box after another.\nMinimum amount of boxes.")
t("pack_mode.one_type", "Un item per shulker", "One item per box")
t("pack_mode.one_type.hover", "Ogni tipo di item ha le sue shulker, mai mischiato.\nUsa più shulker.", "Every item type gets its own boxes, never mixed.\nUses more boxes.")
t("material_sort.count", "Quantità", "Count")
t("material_sort.name", "Nome", "Name")
t("material_sort.category", "Categoria", "Category")
t("material_sort.status", "Stato", "Status")
t("color.rainbow", "Arcobaleno", "Rainbow")
t("color.undyed", "Base", "Undyed")

# Tabs and titles
t("gui.tab.materials", "Materiali", "Materials")
t("gui.tab.boxes", "Shulker", "Shulker boxes")
t("gui.tab.reminders", "Promemoria", "Reminders")
t("gui.title", "Shulker Planner - %s", "Shulker Planner - %s")
t("gui.title.configs", "Shulker Planner - Configurazione", "Shulker Planner - Configuration")

# Buttons
t("gui.button.close", "Chiudi", "Close")
t("gui.button.open_material_list", "Lista materiali", "Material list")
t("gui.button.refresh_materials", "Aggiorna", "Refresh")
t("gui.button.refresh_materials.hover", "Ricalcola la lista materiali di Litematica\ne importa i nuovi conteggi", "Re-creates the Litematica material list\nand imports the new counts")
t("gui.button.open_planner", "Shulker Planner", "Shulker Planner")
t("gui.button.open_planner_short", "Shulker Planner", "Shulker Planner")
t("gui.button.open_planner.hover", "Pianifica le shulker box da riempire\nper questa lista materiali", "Plan the shulker boxes to fill\nfor this material list")
t("gui.button.count_mode", "Conteggio: %s", "Count: %s")
t("gui.button.sort", "Ordina: %s", "Sort: %s")
t("gui.button.hide_excluded", "Nascondi esclusi", "Hide excluded")
t("gui.button.include_all", "Includi tutti", "Include all")
t("gui.button.exclude_all", "Escludi tutti", "Exclude all")
t("gui.button.exclude_gathered", "Escludi procurati", "Exclude gathered")
t("gui.button.exclude_gathered.hover", "Esclude dal piano tutti gli item\nsegnati come procurati", "Excludes all the items marked\nas gathered from the plan")
t("gui.button.notes_for_missing", "Promemoria per i mancanti", "Reminders for missing")
t("gui.button.notes_for_missing.hover", "Crea un promemoria per ogni materiale incluso\nnon ancora procurato", "Creates a reminder for every included\nmaterial that is not gathered yet")
t("gui.button.included", "✔ Incluso", "✔ Included")
t("gui.button.excluded", "✘ Escluso", "✘ Excluded")
t("gui.button.include.hover", "Includi / rimuovi questo item dalle shulker", "Include / remove this item from the boxes")
t("gui.button.gathered", "✔ Procurato", "✔ Gathered")
t("gui.button.not_gathered", "○ Procurato", "○ Gathered")
t("gui.button.gathered.hover", "Segna l'item come procurato (raccolto / craftato)", "Mark the item as gathered (collected / crafted)")
t("gui.button.add_note_short", "+ Nota", "+ Note")
t("gui.button.add_note.hover", "Aggiungi un promemoria per questo item", "Add a reminder for this item")
t("gui.button.add_note_box.hover", "Aggiungi un promemoria per riempire questa shulker", "Add a reminder to fill this box")
t("gui.button.pack_mode", "Modalità: %s", "Mode: %s")
t("gui.button.round_up", "Stack completi", "Full stacks")
t("gui.button.round_up.hover", "Arrotonda ogni item a stack completi (es. 70 -> 128),\ncosì ogni slot è pieno al massimo", "Rounds every item up to full stacks (eg. 70 -> 128),\nso every slot is completely full")
t("gui.button.fill_spare", "Riempi slot vuoti", "Fill empty slots")
t("gui.button.fill_spare.hover", "Riempie gli slot liberi di ogni shulker con stack\nextra dell'item principale di quella shulker (scorta)", "Fills the free slots of every box with extra\nstacks of the main item of that box (spares)")
t("gui.button.color", "Colore: %s", "Color: %s")
t("gui.button.color.hover", "Colore delle shulker create in creativa.\nClick sinistro / destro per cambiare", "Color of the boxes created in creative mode.\nLeft / right click to change")
t("gui.button.hide_ready", "Nascondi pronte", "Hide ready")
t("gui.button.give_all", "Crea tutte (creativa)", "Create all (creative)")
t("gui.button.give_all.hover", "Mette nell'inventario tutte le shulker non ancora pronte,\ngià riempite. Solo in modalità creativa.", "Puts all the boxes that are not ready yet into your\ninventory, already filled. Creative mode only.")
t("gui.button.give", "Crea", "Create")
t("gui.button.give.hover", "Crea questa shulker già riempita\nnell'inventario (solo creativa)", "Creates this box, already filled,\nin your inventory (creative only)")
t("gui.button.export", "Esporta", "Export")
t("gui.button.export.hover", "Salva l'elenco delle shulker in un file di testo\nShift + click: copia negli appunti", "Saves the list of boxes to a text file\nShift + click: copy to the clipboard")
t("gui.button.reset_boxes", "Azzera stato", "Reset status")
t("gui.button.reset_boxes.hover", "Shift + click: segna tutte le shulker come da riempire", "Shift + click: mark all the boxes as not filled")
t("gui.button.box_done", "✔ Pronta", "✔ Ready")
t("gui.button.box_not_done", "○ Pronta", "○ Ready")
t("gui.button.box_done.hover", "Segna la shulker come riempita / pronta", "Mark the box as filled / ready")
t("gui.button.add_reminder", "Aggiungi", "Add")
t("gui.button.add_reminder.hover", "Aggiungi il promemoria (oppure premi Invio)", "Add the reminder (or press Enter)")
t("gui.button.save_reminder", "Salva", "Save")
t("gui.button.cancel", "Annulla", "Cancel")
t("gui.button.remove_completed", "Rimuovi completati", "Remove completed")
t("gui.button.show_completed", "Mostra completati", "Show completed")
t("gui.button.hud", "HUD: %s", "HUD: %s")
t("gui.button.hud.hover", "HUD in gioco: progresso, shulker in corso con gli item\nche ti mancano e promemoria aperti (hotkey M+H)", "In-game HUD: progress, the current box with the items\nyou still need and the open reminders (hotkey M+H)")
t("gui.button.reminder_toggle.hover", "Segna come fatto / da fare", "Mark as done / to do")
t("gui.button.reminder_delete.hover", "Elimina", "Delete")
t("gui.button.reminder_edit.hover", "Modifica", "Edit")

# Labels
t("gui.label.multiplier", "Moltiplicatore:", "Multiplier:")
t("gui.label.materials.header.item", "Item", "Item")
t("gui.label.materials.header.required", "Da imballare", "To pack")
t("gui.label.materials.header.available", "Hai", "Have")
t("gui.label.materials.header.boxes", "Shulker", "Boxes")
t("gui.label.materials.header.stacks", "Stack", "Stacks")
t("gui.label.materials.summary1", "Inclusi: %d/%d  -  Procurati: %d/%d", "Included: %d/%d  -  Gathered: %d/%d")
t("gui.label.materials.summary2", "Da imballare: %s item, %d stack, %d shulker", "To pack: %s items, %d stacks, %d boxes")
t("gui.label.waiting_for_material_list", "Calcolo della lista materiali in corso...", "Counting the material list...")
t("gui.label.type_to_search", "Scrivi per cercare...", "Type to search...")
t("gui.label.box.pure", "Item singolo", "Single item")
t("gui.label.box.mixed", "Mista (%d tipi)", "Mixed (%d types)")
t("gui.label.box.slots", "%d/27 slot", "%d/27 slots")
t("gui.label.box.status_done", "✔ Pronta", "✔ Ready")
t("gui.label.box.status_inventory", "▣ In inventario", "▣ In inventory")
t("gui.label.box.status_todo", "○ Da riempire", "○ To fill")
t("gui.label.box.followed", "◉ Nell'HUD", "◉ On the HUD")
t("gui.label.box.click_to_follow", "Clic: mostra nell'HUD", "Click: show on HUD")
t("gui.label.box.more", "+%d altri...", "+%d more...")
t("gui.label.boxes.summary_all_full", "§bShulker necessarie: %d§r (tutte piene)", "§bBoxes needed: %d§r (all full)")
t("gui.label.boxes.summary_partial", "§bShulker necessarie: %d§r (%d piene, %d slot liberi, ultima %d/27)", "§bBoxes needed: %d§r (%d full, %d free slots, last %d/27)")
t("gui.label.boxes.ready", "Pronte: %d/%d", "Ready: %d/%d")
t("gui.label.boxes.summary_items", "Item totali: %s in %d stack", "Total items: %s in %d stacks")
t("gui.label.boxes.loose", "A parte (non entrano in shulker): %s", "Separately (can't go in a box): %s")
t("gui.label.boxes.no_materials", "Nessun materiale: apri la lista materiali di Litematica e premi Aggiorna", "No materials: open the Litematica material list and press Refresh")
t("gui.label.boxes.nothing_to_pack", "Niente da imballare con le opzioni attuali (controlla conteggio ed esclusi)", "Nothing to pack with the current options (check the count mode and the excluded items)")
t("gui.label.reminders.empty", "Nessun promemoria. Scrivi sopra e premi Invio, o usa \"+ Nota\" su materiali e shulker.", "No reminders. Type above and press Enter, or use \"+ Note\" on materials and boxes.")
t("gui.label.reminders.summary", "Completati: %d/%d  -  Aperti: %d", "Completed: %d/%d  -  Open: %d")

# Hover texts
t("gui.hover.material.total", "Totale schematica: %s (%s)", "Schematic total: %s (%s)")
t("gui.hover.material.missing", "Mancanti nel mondo: %s (%s)", "Missing in the world: %s (%s)")
t("gui.hover.material.available", "In inventario: %s", "In inventory: %s")
t("gui.hover.material.required", "Da imballare [%s]: %s (%s) = %s shulker", "To pack [%s]: %s (%s) = %s boxes")
t("gui.hover.material.stack_size", "Stack massimo: %d", "Max stack size: %d")
t("gui.hover.material.not_boxable", "Non entra in una shulker box: va portato a parte", "Can't be put in a shulker box: carry it separately")
t("gui.hover.material.excluded", "Escluso dal piano", "Excluded from the plan")
t("gui.hover.material.gathered", "Procurato", "Gathered")
t("gui.hover.slot.count", "%d / %d nello slot", "%d / %d in the slot")
t("gui.hover.slot.in_box", "Totale in questa shulker: %s (%s)", "Total in this box: %s (%s)")
t("gui.hover.slot.spare", "Scorta extra (riempimento slot vuoti)", "Extra spare (filling empty slots)")
t("gui.hover.box.items", "%s item in %d slot", "%s items in %d slots")

# HUD
t("hud.current_box", "▶ Shulker #%d/%d", "▶ Box #%d/%d")
t("hud.all_boxes_ready", "✔ Tutte le shulker sono pronte!", "✔ All the boxes are ready!")
t("hud.box_items_ready", "✔ Hai tutto: riempi la shulker!", "✔ You have everything: fill the box!")
t("hud.box_items_missing", "Ti mancano %s item (%d tipi)", "You still need %s items (%d types)")
t("hud.reminders", "Promemoria (%d):", "Reminders (%d):")
t("hud.more", "...e altri %d promemoria", "...and %d more reminders")
t("hud.all_done", "Tutti i promemoria completati!", "All reminders done!")

# Messages
t("message.box_given", "Shulker #%d creata nell'inventario", "Box #%d created in the inventory")
t("message.boxes_given", "%d shulker create nell'inventario", "%d boxes created in the inventory")
t("message.boxes_given_partial", "%d shulker create, ne mancano %d: inventario pieno", "%d boxes created, %d left: inventory full")
t("message.copied_to_clipboard", "Elenco copiato negli appunti", "List copied to the clipboard")
t("message.export_failed", "Esportazione fallita", "Export failed")
t("message.exported", "Elenco salvato in config/shulkerplanner/exports/%s", "List saved to config/shulkerplanner/exports/%s")
t("message.export_cooldown", "Attendi un attimo prima di esportare di nuovo", "Wait a moment before exporting again")
t("message.reminder_exists", "Questo promemoria c'è già", "This reminder already exists")
t("message.hold_shift_to_reset", "Tieni premuto Shift e clicca per azzerare", "Hold Shift and click to reset")
t("message.hud_enabled", "HUD promemoria attivo", "Reminder HUD enabled")
t("message.hud_disabled", "HUD promemoria disattivato", "Reminder HUD disabled")
t("message.inventory_full", "Inventario pieno (oppure non sei in creativa)", "Inventory full (or not in creative mode)")
t("message.no_schematic", "Shulker Planner: seleziona un placement di Litematica (o apri la sua lista materiali)", "Shulker Planner: select a Litematica placement (or open its material list)")
t("message.refreshing_materials", "Aggiornamento lista materiali...", "Refreshing the material list...")
t("message.reminder_added", "Promemoria aggiunto: %s", "Reminder added: %s")
t("message.reminders_added", "Aggiunti %d promemoria", "Added %d reminders")
t("reminder.gather_item", "Procurare %s %s", "Gather %s %s")
t("reminder.fill_box", "Riempire shulker #%d/%d: %s", "Fill box #%d/%d: %s")

# Packing assistant
t("assistant.auto", "auto", "auto")
t("assistant.manual", "manuale", "manual")
t("assistant.complete", "✔ Shulker completa!", "✔ Box complete!")
t("assistant.missing", "Mancano %s item", "%s items missing")
t("assistant.wrong_items", "Togli gli item in rosso", "Remove the red items")
t("assistant.remove", "Da togliere:", "Remove:")
t("assistant.in_inventory", "inv: %s", "inv: %s")

# Export
t("export.header", "Shulker: %d (%d piene) - Item: %s in %d stack", "Boxes: %d (%d full) - Items: %s in %d stacks")
t("export.options", "Conteggio: %s - Modalità: %s - Moltiplicatore: %d", "Count: %s - Mode: %s - Multiplier: %d")
t("export.box", "Shulker #%d/%d (%d/27 slot)", "Box #%d/%d (%d/27 slots)")
t("export.loose", "Da portare a parte:", "Carry separately:")


# HUD views and editor
t("hud.no_plan", "Nessun piano: premi M+B per crearlo", "No plan yet: press M+B to create it")
t("hud.live", "contenuto rilevato", "contents tracked")
t("hud.legend.in_box", " dentro", " inside")
t("hud.legend.in_inventory", " da mettere", " to put in")
t("hud.legend.partial", " in parte", " partial")
t("hud.legend.missing", " manca", " missing")
t("hud.remove", "Da togliere: %s %s", "Remove: %s %s")
t("hud_count.stacks", "Stack", "Stacks")
t("hud_count.items", "Numero", "Items")
t("message.hud_view", "Vista HUD: %s", "HUD view: %s")


# Customizable HUD
t("hud.progress_boxes", "Shulker pronte", "Boxes ready")
t("hud.progress_materials", "Materiali", "Materials")
t("hud.totals_inside", "Dentro", "Inside")
t("hud.totals_inventory", "In inventario", "In inventory")
t("hud.totals_missing", "Mancano", "Missing")
t("hud.put_in_label", "metti", "put in")
t("hud.missing_label", "mancano", "missing")
t("hud.have_label", "hai", "have")
t("hud_element.title", "Titolo", "Title")
t("hud_element.progress", "Progresso", "Progress")
t("hud_element.box_header", "Shulker in corso", "Current box")
t("hud_element.box_totals", "Totali shulker", "Box totals")
t("hud_element.box_grid", "Interfaccia shulker 9x3", "Shulker box 9x3 view")
t("hud_element.grid_legend", "Legenda colori", "Color legend")
t("hud_element.box_items", "Lista item mancanti", "Missing items list")
t("hud_element.wrong_items", "Item da togliere", "Items to remove")
t("hud_element.box_status", "Riga di stato", "Status line")
t("hud_element.reminders", "Promemoria", "Reminders")
t("hud_color.background", "Sfondo", "Background")
t("hud_color.accent", "Barra laterale", "Side bar")
t("hud_color.border", "Bordo", "Border")
t("hud_color.title", "Titolo", "Title")
t("hud_color.text", "Testo", "Text")
t("hud_color.label", "Etichette", "Labels")
t("hud_color.good", "OK / dentro", "OK / inside")
t("hud_color.info", "Info / da mettere", "Info / to put in")
t("hud_color.warn", "Attenzione", "Warning")
t("hud_color.bad", "Manca / errore", "Missing / error")
t("hud_theme.purple", "Viola", "Purple")
t("hud_theme.dark", "Scuro", "Dark")
t("hud_theme.light", "Chiaro", "Light")
t("hud_theme.ocean", "Oceano", "Ocean")
t("hud_theme.emerald", "Smeraldo", "Emerald")
t("hud_theme.sunset", "Tramonto", "Sunset")
t("hud_theme.glass", "Vetro", "Glass")
t("hud_theme.custom", "Personalizzato", "Custom")
t("hud_preset.full", "Completa", "Full")
t("hud_preset.shulker", "Solo shulker", "Shulker box")
t("hud_preset.list", "Solo lista", "List only")
t("hud_preset.minimal", "Minima", "Minimal")
t("hud_preset.reminders", "Solo promemoria", "Reminders only")
t("hud_editor.page.look", "Aspetto", "Look")
t("hud_editor.page.elements", "Elementi", "Elements")
t("hud_editor.page.layout", "Layout", "Layout")
t("hud_editor.title", "Titolo", "Title")
t("hud_editor.theme", "Tema", "Theme")
t("hud_editor.opacity", "Opacità sfondo: %d%%", "Background opacity: %d%%")
t("hud_editor.preset", "Vista: %s", "View: %s")
t("hud_editor.preset_custom", "Personalizzata", "Custom")
t("hud_editor.scale", "Dimensione: %d%%", "Size: %d%%")
t("hud_editor.compact", "Compatto: %s", "Compact: %s")
t("hud_editor.padding", "Margini: %d", "Padding: %d")
t("hud_editor.shadow", "Ombra testo: %s", "Text shadow: %s")
t("hud_editor.accent_bar", "Barra laterale: %s", "Side bar: %s")
t("hud_editor.border", "Bordo: %s", "Border: %s")
t("hud_editor.max_box_items", "Item shulker mostrati: %d", "Box items shown: %d")
t("hud_editor.max_reminders", "Promemoria mostrati: %d", "Reminders shown: %d")
t("hud_editor.counts", "Quantità: %s", "Amounts: %s")
t("hud_editor.reset_position", "Posizione iniziale", "Reset position")
t("hud_editor.save_default", "Usa come stile per i nuovi progetti", "Use as style for new projects")
t("hud_editor.load_default", "Applica lo stile predefinito", "Apply the default style")
t("hud_editor.done", "Fatto", "Done")
t("hud_editor.no_project", "Nessuna schematica attiva: apri prima lo Shulker Planner", "No active schematic: open the Shulker Planner first")
t("hud_editor.scroll", "Rotellina per scorrere", "Scroll for more")
t("hud_editor.hint", "Trascina per spostare - angolo o rotellina per ridimensionare", "Drag to move - corner or mouse wheel to resize")
t("gui.button.edit_hud", "Personalizza HUD", "Customize HUD")
t("gui.button.edit_hud.hover", "Posizione, dimensione, titolo, tema, colori ed elementi\ndell'HUD di questa schematica. Hotkey M+J", "Position, size, title, theme, colors and elements\nof the HUD of this schematic. Hotkey M+J")

# 2.0: Tabs, Shell, Banners, Linking, Raw Materials, Assistant, Settings
t("gui.tab.raw", "Materie prime", "Raw materials")
t("gui.tab.raw.hover", "Calcola le risorse sorgente e i passi di crafting", "Calculates raw source resources and crafting steps")
t("gui.tab.settings", "Impostazioni", "Settings")
t("gui.tab.settings.hover", "Attiva o disattiva avvisi, evidenziazioni e opzioni", "Toggle warnings, highlights and options")
t("gui.tab.boxes.hover", "Fasi del piano shulker, stato e collegamenti", "Planned shulker phases, states and links")
t("gui.tab.materials.hover", "Lista completa dei materiali da procurare e impacchettare", "Full list of materials to gather and pack")
t("gui.tab.reminders.hover", "Note e promemoria per questo progetto", "Notes and reminders for this project")

t("gui.brand", "SHULKER PLANNER", "SHULKER PLANNER")
t("gui.header.progress", "%s/%s shulker", "%s/%s boxes")
t("gui.chip.locked", "BLOCCATO", "LOCKED")
t("gui.chip.draft", "BOZZA", "DRAFT")
t("gui.chip.raw", "SORGENTI", "RAW")
t("gui.label.source", "Litematica", "Litematica")
t("gui.hover.cycle", "Clic: successivo · Destro: precedente", "Click: next · Right-click: previous")
t("gui.button.hud_short", "HUD", "HUD")
t("gui.button.more_options", "Altre opzioni", "More options")

t("box_state.todo", "Da fare", "To do")
t("box_state.filling", "In corso", "Filling")
t("box_state.done", "Pronta", "Done")

t("recipe_type.stonecutting", "Tagliapietre", "Stonecutter")
t("recipe_type.crafting", "Banco da lavoro", "Crafting table")
t("recipe_type.smelting", "Fornace", "Furnace")
t("recipe_type.smithing", "Banco da forgiatura", "Smithing table")

t("gui.banner.draft", "Piano in bozza: si aggiorna da solo con Litematica. Bloccalo per iniziare a riempire le shulker.", "Draft plan: follows Litematica automatically. Lock it when you start packing boxes.")
t("gui.banner.locked", "Piano bloccato: fasi, stati e shulker collegate restano fissi.", "Locked plan: phases, states and linked shulker boxes stay fixed.")
t("gui.banner.outdated", "La lista materiali è cambiata (%s differenze: +%s / -%s).", "The material list changed (%s differences: +%s / -%s).")
t("gui.banner.recipes_world", "Ricette caricate dal mondo corrente (%s ricette, datapack/mod inclusi).", "Recipes loaded from the current world (%s recipes, data packs/mods included).")
t("gui.banner.recipes_vanilla", "Ricette vanilla dal client (%s ricette).", "Vanilla recipes loaded from client (%s recipes).")

t("gui.button.lock_plan", "Blocca piano", "Lock plan")
t("gui.button.update_plan", "Aggiorna piano", "Update plan")
t("gui.button.reset_plan", "Sblocca / Azzera", "Unlock / Reset")
t("gui.button.reset_plan.hover", "Tieni Shift e clicca per sbloccare il piano\ne azzerare stati e shulker collegate", "Hold Shift and click to unlock the plan\nand clear box states and linked shulker boxes")
t("gui.button.hide_ready.hover", "Nasconde le shulker già segnate come pronte", "Hides boxes already marked as ready")
t("gui.button.box_undo", "Riapri", "Reopen")
t("gui.button.follow", "Segui", "Follow")
t("gui.button.follow.hover", "Mostra questa fase sull'HUD e nell'assistente", "Follow this phase on the HUD and packing assistant")
t("gui.button.unlink", "Scollega", "Unlink")
t("gui.button.unlink.hover", "Tieni Shift e clicca per scollegare questa shulker box", "Hold Shift and click to unlink this shulker box")
t("gui.button.include", "Includi", "Include")
t("gui.button.gathered_short", "Procurato", "Gathered")

t("gui.button.pack_raw", "Impacchetta sorgenti", "Pack raw items")
t("gui.button.pack_raw.hover", "Pianifica le shulker con le materie prime sorgente\nanziché con i blocchi finali", "Plans the shulker boxes using raw source materials\ninstead of the final crafted blocks")
t("gui.button.allow_smelting", "Fornace", "Smelting")
t("gui.button.allow_smelting.hover", "Scompone anche i blocchi cotti in fornace (es. pietra, vetro)", "Also breaks down smelted items (e.g. stone, glass)")
t("gui.button.allow_stonecutter", "Tagliapietre", "Stonecutter")
t("gui.button.allow_stonecutter.hover", "Usa il tagliapietre per evitare sprechi di blocchi in pietra", "Uses the stonecutter to avoid wasting stone blocks")
t("gui.button.reset_recipes", "Ripristina ricette", "Reset recipes")
t("gui.button.reset_recipes.hover", "Ripristina le scelte predefinite delle ricette\ne gli item tenuti come base", "Resets custom recipe choices and kept base items")
t("gui.button.break_down", "Scomponi", "Break down")
t("gui.button.break_down.hover", "Torna a scomporre questo item nei suoi ingredienti", "Break this item down into its ingredients again")
t("gui.button.keep_item", "Non scomporre", "Keep as is")
t("gui.button.keep_item.hover", "Considera questo item come risorsa di partenza\nsenza scomporlo ulteriormente", "Treat this item as a raw resource without breaking it down")
t("gui.button.recipe", "Ricetta %s/%s", "Recipe %s/%s")
t("gui.button.recipe.hover", "Clic per cambiare ricetta di crafting per questo item", "Click to cycle the crafting recipe for this item")

t("gui.button.warnings_all_on", "Attiva tutti gli avvisi", "Enable all warnings")
t("gui.button.warnings_all_on.hover", "Attiva tutti gli avvisi e le evidenziazioni", "Turns on every warning and slot highlight")
t("gui.button.warnings_all_off", "Disattiva tutti gli avvisi", "Disable all warnings")
t("gui.button.warnings_all_off.hover", "Disattiva tutti gli avvisi di item sbagliati o in eccesso", "Turns off every wrong-item and extra-item warning")
t("gui.button.open_hotkeys", "Hotkey e Config completo", "Hotkeys & Full Config")
t("gui.button.open_hotkeys.hover", "Apre la schermata MaLiLib con tutte le scorciatoie da tastiera", "Opens the MaLiLib screen with all keybinds")

t("gui.label.box.not_linked", "Non collegata", "Not linked")
t("gui.label.box.status_filling", "In corso", "Filling")
t("gui.label.boxes.empty_list", "Nessuna shulker da mostrare per questo filtro", "No boxes to show for this filter")
t("gui.hover.box.linked", "Collegata: %s", "Linked: %s")
t("gui.hover.box.how_to_link", "Apri una shulker in gioco e premi '+ Collega' nell'assistente", "Open a shulker box in-game and click '+ Link' in the assistant")

t("gui.label.materials.header.item", "Materiale", "Material")
t("gui.label.materials.header.needed", "Serve", "Needed")
t("gui.label.materials.header.packed", "Nelle box", "Packed")
t("gui.label.materials.header.carrying", "In inv.", "Carrying")
t("gui.label.materials.header.to_get", "Manca", "To get")
t("gui.label.materials.header.stacks", "Stack", "Stacks")

t("gui.label.raw.header_raw", "MATERIE PRIME SORGENTE", "RAW SOURCE MATERIALS")
t("gui.label.raw.header_steps", "PASSI DI CRAFTING", "CRAFTING STEPS")
t("gui.label.raw.boxes", "Shulker", "Boxes")
t("gui.label.raw.crafts", "%s craft", "%s crafts")
t("gui.label.raw.have", "Hai: %s", "Have: %s")
t("gui.label.raw.leftover", "Avanzo: %s", "Leftover: %s")
t("gui.label.raw.summary1", "%s risorse sorgente (%s item totali) · %s passi di crafting", "%s raw resources (%s total items) · %s crafting steps")
t("gui.label.raw.summary_packing_raw", "Modalità attiva: le shulker contengono le materie prime (%s shulker).", "Active mode: boxes are packed with raw materials (%s boxes).")
t("gui.label.raw.summary_packing_listed", "Modalità attiva: le shulker contengono i blocchi finali (%s oggetti).", "Active mode: boxes are packed with final blocks (%s items).")
t("gui.hover.raw.raw", "Totale da procurare: %s", "Total needed: %s")
t("gui.hover.raw.step", "Servono %s · %s craft (x%s = %s prodotti)", "Need %s · %s crafts (x%s = %s produced)")
t("gui.hover.raw.any_of", "uno tra %s", "any of %s")

t("gui.settings.section.warnings", "AVVISI ED EVIDENZIAZIONI", "WARNINGS & HIGHLIGHTS")
t("gui.settings.section.generic", "OPZIONI GENERALI", "GENERAL OPTIONS")
t("gui.summary.settings", "%s/%s avvisi attivi", "%s/%s warnings enabled")

t("hud.linked", "Collegata", "Linked")
t("hud.followed", "Seguita", "Followed")
t("link.carried", "nell'inventario", "in inventory")
t("link.placed", "piazzata a %s, %s, %s", "placed at %s, %s, %s")

t("assistant.box", "Fase #%s / %s", "Phase #%s / %s")
t("assistant.state.done", "Pronta", "Done")
t("assistant.state.linked", "Collegata", "Linked")
t("assistant.state.preview", "Anteprima", "Preview")
t("assistant.to_put_in", "Da inserire: %s", "Put in: %s")
t("assistant.too_many", "In eccesso: +%s", "Extra: +%s")
t("assistant.hint_unlinked1", "Non collegata a una fase", "Not linked to a phase")
t("assistant.hint_unlinked2", "Scegli con ◀ ▶, poi Collega", "Pick with ◀ ▶, then Link")
t("assistant.link", "Collega a #%s", "Link to #%s")
t("assistant.unlink", "Scollega", "Unlink")
t("assistant.mark_done", "Segna pronta", "Mark done")
t("assistant.undo_done", "Riapri fase", "Reopen")
t("assistant.hover.move", "Trascina per spostare il pannello", "Drag to move the panel")
t("assistant.hover.reset", "Rimetti il pannello accanto alla shulker, con la dimensione automatica", "Put the panel back next to the shulker box, with the automatic size")
t("assistant.hover.hud_on", "Questa fase è sull'HUD: clicca per smettere di seguirla", "This phase is on the HUD: click to stop following it")
t("assistant.hover.hud_off", "Mostra questa fase sull'HUD", "Show this phase on the HUD")
t("assistant.hover.resize", "Trascina per ridimensionare", "Drag to resize")
t("assistant.hover.in_box", "Nella shulker: %s / %s (%s)", "In the box: %s / %s (%s)")
t("assistant.hover.wrong", "Non fa parte di questa fase: %s da togliere", "Not part of this box: %s to remove")
t("assistant.already_linked", "Questa fase ha già un'altra shulker", "Phase already linked to another box")

t("message.box_complete", "Shulker #%s completata!", "Shulker box #%s complete!")
t("message.hold_shift_to_unlink", "Tieni premuto Shift per scollegare la shulker", "Hold Shift to unlink the shulker box")
t("message.plan_locked", "Piano bloccato: ora puoi collegare le tue shulker box", "Plan locked: you can now link your shulker boxes")
t("message.plan_reset", "Piano sbloccato e azzerato", "Plan unlocked and reset")
t("message.plan_updated", "Piano aggiornato alle nuove quantità", "Plan updated to the new material counts")

# Config
t("hotkeys.category.generic", "Hotkey", "Hotkeys")

configs = {
    "generic": [
        ("hudEnabled", "HUD", "HUD", "Mostra a schermo il progetto attivo, il progresso\ne i promemoria aperti", "Shows the active project, the progress\nand the open reminders on screen"),
        ("hudFollowSelectedPlacement", "HUD segue il placement", "HUD follows placement", "Quando selezioni un placement di Litematica che ha già un progetto,\nl'HUD passa a quella schematica", "When you select a Litematica placement that already has a project,\nthe HUD switches to that schematic"),
        ("hudFollowOpenedBox", "HUD segue shulker aperta", "HUD follows opened box", "Quando apri una shulker collegata al piano,\nl'HUD mostra automaticamente quella fase", "When you open a shulker box linked to the plan,\nthe HUD automatically shows that box"),
        ("packingAssistant", "Assistente riempimento", "Packing assistant", "Quando apri una shulker box mostra a lato cosa\ndeve contenere secondo il piano", "When opening a shulker box, shows next to it\nwhat it should contain according to the plan"),
        ("packingAssistantOpacity", "Opacità assistente", "Assistant opacity", "Opacità dello sfondo del pannello accanto alle shulker (10-100%).\nIl pannello si sposta trascinando la barra in alto,\nsi ridimensiona dall'angolo in basso a destra.", "Background opacity of the panel next to shulker boxes (10-100%).\nMove the panel by dragging its title bar,\nresize it from the bottom right corner."),
        ("autoLinkOpenedBoxes", "Collega shulker in automatico", "Auto-link opened boxes", "Sconsigliato: collega automaticamente una shulker appena la apri.\nDisattivato di default per collegare le shulker solo a mano.", "Not recommended: automatically links a shulker box on open.\nOff by default so you link boxes manually."),
        ("autoMarkFilledBoxes", "Segna shulker automaticamente", "Auto mark filled boxes", "Segna come pronte le shulker collegate quando contengono\nesattamente gli item previsti dal piano", "Marks linked boxes as ready when they contain\nthe exact items required by the plan"),
        ("autoDetectInventoryBoxes", "Rileva shulker in inventario", "Detect boxes in inventory", "Conta gli item dentro le shulker e i bundle\nche porti nell'inventario", "Counts items inside shulker boxes and bundles\nin your inventory"),
        ("respectLitematicaIgnored", "Rispetta blocchi ignorati Litematica", "Respect Litematica ignored", "Esclude dai calcoli i blocchi che hai ignorato\nnella lista materiali di Litematica", "Excludes blocks ignored in Litematica's\nmaterial list from calculations"),
        ("addButtonToMaterialList", "Bottone nella lista materiali", "Button in material list", "Aggiunge il bottone Shulker Planner\nalla lista materiali di Litematica", "Adds the Shulker Planner button\nto the Litematica material list"),
        ("addButtonToMainMenu", "Bottone nel menu Litematica", "Button in Litematica menu", "Aggiunge il bottone Shulker Planner\nal menu principale di Litematica", "Adds the Shulker Planner button\nto the Litematica main menu"),
    ],
    "warnings": [
        ("warnWrongItemsInBox", "Avviso item sbagliati nel pannello", "Panel wrong items warning", "Mostra nell'assistente gli item presenti nella shulker\nche non fanno parte di quella fase", "Shows items in the packing assistant that\ndon't belong in this box"),
        ("highlightWrongSlots", "Evidenzia slot sbagliati (rosso)", "Highlight wrong slots (red)", "Colora di rosso gli slot della shulker con item\nsbagliati rispetto al piano", "Highlights slots with wrong items in red"),
        ("highlightOverfilledSlots", "Evidenzia slot in eccesso (giallo)", "Highlight overfilled slots (yellow)", "Colora di giallo gli slot che hanno più item\ndel necessario per quella fase", "Highlights slots with more items than needed in yellow"),
        ("highlightItemsToMove", "Evidenzia item da spostare (verde)", "Highlight items to move (green)", "Colora di verde nell'inventario gli item\nche servono per questa shulker", "Highlights items in your inventory needed for this box in green"),
        ("highlightUnlinkedPreview", "Evidenzia anche senza collegamento", "Highlight even when unlinked", "Mostra gli avvisi colorati sugli slot anche quando\nla shulker aperta non è ancora collegata", "Shows slot highlights even when the opened\nshulker box is not linked yet"),
        ("hudWrongItems", "Avviso item sbagliati nell'HUD", "HUD wrong items warning", "Mostra nell'HUD eventuali item sbagliati o in eccesso\ndentro la shulker collegata", "Shows wrong or extra items in the linked box on the HUD"),
        ("hintUnlinkedBox", "Suggerimento shulker non collegata", "Unlinked box hint", "Mostra nell'assistente il promemoria che la shulker\nnon è collegata finché non premi Collega", "Shows a hint in the assistant when a shulker\nis not linked until you click Link"),
        ("notifyBoxComplete", "Notifica fase completata", "Notify box complete", "Mostra un messaggio nella actionbar quando\ncompleti una shulker collegata", "Shows an actionbar message when a linked\nshulker box is completed"),
        ("warnPlanOutdated", "Avviso piano non aggiornato", "Outdated plan warning", "Mostra un banner nel planner quando la lista materiali\ndi Litematica cambia rispetto al piano bloccato", "Shows a banner when Litematica's material list\nchanges compared to the locked plan"),
    ],
    "hotkeys": [
        ("openShulkerPlanner", "Apri Shulker Planner", "Open Shulker Planner", "Apre il planner per la lista materiali corrente\no per il placement selezionato", "Opens the planner for the current material list\nor for the selected placement"),
        ("openReminders", "Apri promemoria", "Open reminders", "Apre direttamente la pagina dei promemoria", "Opens the reminders page directly"),
        ("toggleReminderHud", "Attiva/disattiva HUD", "Toggle HUD", "Attiva o disattiva l'HUD in gioco (promemoria e shulker in corso)", "Toggles the in-game HUD (reminders and current box)"),
        ("editHudPosition", "Personalizza HUD", "Customize HUD", "Apre l'editor dell'HUD: posizione, dimensione, titolo, colori, temi ed elementi", "Opens the HUD editor: position, size, title, colors, themes and elements"),
        ("cycleHudView", "Cambia vista HUD", "Cycle HUD view", "Passa tra le viste dell'HUD: completa, solo shulker, solo lista, minima, solo promemoria", "Cycles the HUD views: full, shulker box, list only, minimal, reminders only"),
        ("openConfigGui", "Apri configurazione", "Open configuration", "Apre la configurazione di Shulker Planner", "Opens the Shulker Planner configuration"),
    ],
}

for cat, entries in configs.items():
    for name, pit, pen, cit, cen in entries:
        t("config.%s.name.%s" % (cat, name), name, name)
        t("config.%s.prettyName.%s" % (cat, name), pit, pen)
        t("config.%s.comment.%s" % (cat, name), cit, cen)

out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "shulkerplanner", "lang")
os.makedirs(out, exist_ok=True)

for name, data in (("it_it.json", it), ("en_us.json", en)):
    with open(os.path.join(out, name), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=4)

print(len(it), "keys")
