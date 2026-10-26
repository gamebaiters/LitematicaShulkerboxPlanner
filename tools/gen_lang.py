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
t("gui.button.hide_excluded", "Nascondi esclusi: %s", "Hide excluded: %s")
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
t("gui.button.round_up", "Stack completi: %s", "Full stacks: %s")
t("gui.button.round_up.hover", "Arrotonda ogni item a stack completi (es. 70 -> 128),\ncosì ogni slot è pieno al massimo", "Rounds every item up to full stacks (eg. 70 -> 128),\nso every slot is completely full")
t("gui.button.fill_spare", "Riempi slot vuoti: %s", "Fill empty slots: %s")
t("gui.button.fill_spare.hover", "Riempie gli slot liberi di ogni shulker con stack\nextra dell'item principale di quella shulker (scorta)", "Fills the free slots of every box with extra\nstacks of the main item of that box (spares)")
t("gui.button.color", "Colore: %s", "Color: %s")
t("gui.button.color.hover", "Colore delle shulker create in creativa.\nClick sinistro / destro per cambiare", "Color of the boxes created in creative mode.\nLeft / right click to change")
t("gui.button.hide_ready", "Nascondi pronte: %s", "Hide ready: %s")
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
t("gui.button.show_completed", "Mostra completati: %s", "Show completed: %s")
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
t("hud.progress", "Shulker pronte %s  -  Materiali %s", "Boxes ready %s  -  Materials %s")
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
t("hud.box_totals", "Dentro %s  -  In inventario %s  -  Mancano %s", "Inside %s  -  In inventory %s  -  Missing %s")
t("hud.legend.in_box", " dentro", " inside")
t("hud.legend.in_inventory", " da mettere", " to put in")
t("hud.legend.partial", " in parte", " partial")
t("hud.legend.missing", " manca", " missing")
t("hud.remove", "Da togliere: %s %s", "Remove: %s %s")
t("hud.put_in", "metti %s", "put in %s")
t("hud.missing_have", "mancano %s (hai %s)", "missing %s (have %s)")
t("hud.missing", "mancano %s", "missing %s")
t("hud_view.compact", "Compatta", "Compact")
t("hud_view.shulker", "Shulker", "Shulker box")
t("hud_view.both", "Shulker + lista", "Box + list")
t("hud_count.stacks", "Stack", "Stacks")
t("hud_count.items", "Numero", "Items")
t("hud_editor.view", "Vista: %s", "View: %s")
t("hud_editor.counts", "Conteggi: %s", "Counts: %s")
t("hud_editor.reset", "Posizione iniziale", "Reset position")
t("hud_editor.done", "Fatto", "Done")
t("hud_editor.hint", "Trascina l'HUD con il mouse per spostarlo", "Drag the HUD with the mouse to move it")
t("hud_editor.no_project", "Nessun progetto attivo: apri prima lo Shulker Planner", "No active project: open the Shulker Planner first")
t("gui.button.edit_hud", "Sposta HUD", "Move HUD")
t("gui.button.edit_hud.hover", "Trascina l'HUD dove vuoi e scegli la vista\n(compatta, shulker o entrambe). Hotkey M+J", "Drag the HUD where you want and pick the view\n(compact, shulker box or both). Hotkey M+J")
t("message.hud_view", "Vista HUD: %s", "HUD view: %s")

# Config
t("hotkeys.category.generic", "Hotkey", "Hotkeys")

configs = {
    "generic": [
        ("hudEnabled", "HUD promemoria", "Reminder HUD", "Mostra a schermo il progetto attivo, il progresso\ne i promemoria aperti", "Shows the active project, the progress\nand the open reminders on screen"),
        ("hudOffsetX", "HUD posizione X", "HUD position X", "Posizione orizzontale dell'HUD", "Horizontal position of the HUD"),
        ("hudOffsetY", "HUD posizione Y", "HUD position Y", "Posizione verticale dell'HUD", "Vertical position of the HUD"),
        ("hudMaxReminders", "HUD max promemoria", "HUD max reminders", "Quanti promemoria aperti mostrare nell'HUD", "How many open reminders to show on the HUD"),
        ("hudShowCurrentBox", "HUD shulker in corso", "HUD current box", "Mostra la shulker da riempire e gli item che ti mancano\n(conta solo gli item sciolti nell'inventario)", "Shows the box to fill and the items you still need\n(counts only the loose items in your inventory)"),
        ("hudMaxBoxItems", "HUD max item shulker", "HUD max box items", "Quanti item della shulker in corso mostrare nell'HUD", "How many items of the current box to show on the HUD"),
        ("hudBoxView", "HUD vista shulker", "HUD box view", "Come mostrare la shulker in corso: Compatta (solo cosa manca),\nShulker (interfaccia 9x3 colorata) o entrambe", "How to show the current box: Compact (only what is missing),\nShulker box (colored 9x3 interface) or both"),
        ("hudCountFormat", "HUD conteggi", "HUD counts", "Quantità in stack (2 x 64 + 12) o in numero (140)", "Amounts as stacks (2 x 64 + 12) or as numbers (140)"),
        ("hudFollowSelectedPlacement", "HUD segue il placement", "HUD follows placement", "Quando selezioni un placement di Litematica che ha già un progetto,\nl'HUD passa a quella schematica", "When you select a Litematica placement that already has a project,\nthe HUD switches to that schematic"),
        ("hudShowProgress", "HUD mostra progresso", "HUD show progress", "Mostra shulker pronte e materiali procurati nell'HUD", "Shows the ready boxes and gathered materials on the HUD"),
        ("packingAssistant", "Assistente riempimento", "Packing assistant", "Quando apri una shulker box mostra a lato cosa\ndeve contenere secondo il piano", "When opening a shulker box, shows next to it\nwhat it should contain according to the plan"),
        ("packingHighlightSlots", "Evidenzia slot", "Highlight slots", "Evidenzia in verde gli item da mettere nella shulker\ne in rosso quelli sbagliati", "Highlights the items to put into the box in green\nand the wrong ones in red"),
        ("autoMarkFilledBoxes", "Segna shulker automaticamente", "Auto mark filled boxes", "Segna come pronte le shulker riempite con l'assistente\no presenti nell'inventario con il contenuto giusto", "Marks boxes as ready when filled with the assistant\nor when they are in your inventory with the right contents"),
        ("addButtonToMaterialList", "Bottone nella lista materiali", "Button in material list", "Aggiunge il bottone Shulker Planner\nalla lista materiali di Litematica", "Adds the Shulker Planner button\nto the Litematica material list"),
        ("addButtonToMainMenu", "Bottone nel menu Litematica", "Button in Litematica menu", "Aggiunge il bottone Shulker Planner\nal menu principale di Litematica", "Adds the Shulker Planner button\nto the Litematica main menu"),
    ],
    "hotkeys": [
        ("openShulkerPlanner", "Apri Shulker Planner", "Open Shulker Planner", "Apre il planner per la lista materiali corrente\no per il placement selezionato", "Opens the planner for the current material list\nor for the selected placement"),
        ("openReminders", "Apri promemoria", "Open reminders", "Apre direttamente la pagina dei promemoria", "Opens the reminders page directly"),
        ("toggleReminderHud", "Attiva/disattiva HUD", "Toggle HUD", "Attiva o disattiva l'HUD in gioco (promemoria e shulker in corso)", "Toggles the in-game HUD (reminders and current box)"),
        ("editHudPosition", "Sposta HUD", "Move HUD", "Apre la schermata per trascinare l'HUD e sceglierne la vista", "Opens the screen to drag the HUD and pick its view"),
        ("cycleHudView", "Cambia vista HUD", "Cycle HUD view", "Passa tra vista compatta, shulker ed entrambe", "Cycles between compact, shulker box and both views"),
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
