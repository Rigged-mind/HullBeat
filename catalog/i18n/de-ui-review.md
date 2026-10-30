# Interface strings to review - de

414 strings and 29 plurals drafted. None is confirmed. Check two things on each row: is it what a sailor would say, and does it fit a phone screen? Navigation tabs and buttons have the least room.

`%s`, `%1$s`, `%d` are filled in by the app; keep them, move them freely within the sentence.

## Check these first

The places where a wrong word costs most. Each names the rows it is about, with the current draft.

1. **Bottom-navigation tab for the maintenance log: is „Wartungsbuch“ what you would call it, and does it fit the tab? „Logbuch“ was rejected because it is the voyage log; the agreed fallback is „Historie“.**
   - `ui:nav_log` - Wartungsbuch
   - `ui:placeholder_log` - Wartungsbuch, Zeitleiste und Suche.
   - `ui:journal_empty_title` - Das Wartungsbuch ist leer
2. **Snubber: „Kettenkralle“ is the chain hook, not the line that takes the load off the windlass. What do German sailors call the snubber itself, and is „Ankerhahnepot“ right for the bridle?**
   - `cmp:snubber` - Kettenkralle / Ankerhahnepot
   - `cmp:anchor_bridle_hook` - Kettenhaken der Hahnepot
   - `cmp:mooring_snubbers` - Ruckdämpfer für Festmacher
3. **Country-bound documents: HU, Bootszeugnis and „Technische Prüfung“. Germany has no general periodic inspection for private yachts; Austria and Switzerland have their own rules. Are these labels neutral enough for owners in DE, AT and CH?**
   - `cmp:national_inspection` - Technische Prüfung (national)
   - `cmp:trailer_documents` - Zulassung & HU des Anhängers
   - `cmp:registration` - Registrierung / Flagge
   - `hint:Scheme (ITB/BSS/Revisione/Bootszeugnis)` - Prüfsystem (ITB / BSS / Revisione / Bootszeugnis)
4. **Launch / haul-out are „Einkranen“ / „Auskranen“ everywhere. Wrong for a boat slipped from a trailer or lifted by a travel lift? Would „Einwassern“ / „Auswassern“ be the neutral pair?**
   - `ui:work_type_launch` - Einkranen
   - `ui:work_type_haul_out` - Auskranen
   - `ui:defer_option_haul_out` - Bis zum Auskranen
   - `chk:chk_cm_seacocks` - Jedes Seeventil durchbewegen, fetten und vor dem Einkranen alle schließen
   - `chk:chk_wz_wash` - Unterwasserschiff gleich nach dem Auskranen abspritzen, bevor der Bewuchs antrocknet
5. **Jackstay, tether and lifelines: „Strecktaue“ along the deck, „Lifelines“ for the tether (the German sense), „Seereling“ for the guard wires. Will a German reader keep these three apart?**
   - `cmp:jackstays` - Strecktaue & Einpickpunkte
   - `cmp:harnesses_tethers` - Sicherheitsgurte & Lifelines
   - `cmp:deck_rails_lifelines` - Seereling, Relingsstützen, Bug- & Heckkorb


## All strings

| Key | English | Draft | Correction |
|---|---|---|---|
| `notification_channel_due` | Maintenance due | **Wartung fällig** | |
| `notification_channel_due_description` | Reminders when a job is due or overdue. | **Erinnerungen, wenn eine Arbeit fällig oder überfällig ist.** | |
| `action_mark_done` | Done | **Erledigt** | |
| `action_renew` | Renew… | **Erneuern…** | |
| `action_mark_done_with_hours` | Done… | **Erledigt…** | |
| `due_unknown` | Due date unknown | **Fälligkeit unbekannt** | |
| `due_unknown_needs_meter` | Enter initial engine hours to start forecast | **Motorstunden eintragen, um die Prognose zu starten** | |
| `due_unknown_needs_schedule` | Configure maintenance schedule | **Wartungsplan festlegen** | |
| `snack_logged` | Logged | **Eingetragen** | |
| `action_undo` | Undo | **Rückgängig** | |
| `snack_deferral_cancelled` | Deferral cancelled | **Zurückstellung aufgehoben** | |
| `snack_deferred_until` | Deferred until %1$s | **Zurückgestellt bis %1$s** | |
| `deferred_until` | deferred until %1$s | **zurückgestellt bis %1$s** | |
| `deferred_until_reason` | %1$s · until %2$s | **%1$s · bis %2$s** | |
| `placeholder_boat` | Component catalogue and equipment passports. | **Komponentenkatalog und Gerätedatenblätter.** | |
| `placeholder_log` | Ship\'s log, timeline and search. | **Wartungsbuch, Zeitleiste und Suche.** | |
| `placeholder_store` | Spares, lockers and stock levels. | **Ersatzteile, Stauräume und Bestand.** | |
| `summary_overdue` | %d overdue | **%d überfällig** | |
| `summary_soon` | %d due soon | **%d bald fällig** | |
| `summary_more` | and %d more | **und %d weitere** | |
| `record_from_notification` | Logged from the notification | **Aus der Benachrichtigung eingetragen** | |
| `nav_now` | Pulse | **Puls** | |
| `nav_boat` | Boat | **Boot** | |
| `nav_log` | Log | **Wartungsbuch** | |
| `nav_store` | Spares | **Ersatzteile** | |
| `departure_check_done_today` | Pre-departure check completed today | **Check vor dem Auslaufen heute erledigt** | |
| `departure_checklist_title` | Pre-departure check | **Check vor dem Auslaufen** | |
| `departure_check_fleet_partial` | Pre-departure check · done: %s | **Check vor dem Auslaufen · erledigt: %s** | |
| `action_confirm_departure_check` | Confirm departure check | **Check bestätigen** | |
| `action_reset_departure_check` | New departure | **Neues Auslaufen** | |
| `departure_check_all_required` | Check all items to confirm | **Alle Punkte abhaken, um zu bestätigen** | |
| `departure_check_empty_list` | Could not load the checklist | **Checkliste konnte nicht geladen werden** | |
| `action_record_meter` | Log engine hours | **Motorstunden eintragen** | |
| `record_meter_title` | Log Engine Hours | **Motorstunden eintragen** | |
| `meter_reading_saved` | Reading recorded | **Zählerstand gespeichert** | |
| `meter_current_reading` | Current reading | **Aktueller Zählerstand** | |
| `meter_hours_unit` | hrs | **Std.** | |
| `meter_decrease_warning` | Reading is less than previous (%.1f hrs). Decreasing will disable hour-based maintenance predictions. | **Der Wert ist kleiner als der vorherige (%.1f Std.). Ein niedrigerer Wert schaltet die stundenbasierte Wartungsprognose ab.** | |
| `meter_decrease_confirm` | I understand, save new reading | **Verstanden, neuen Wert speichern** | |
| `fleet_all` | All | **Alle** | |
| `action_add_vessel` | Add vessel… | **Boot hinzufügen…** | |
| `vessel_select_title` | Select vessel | **Boot auswählen** | |
| `vessel_status_all_clear` | All clear | **Alles in Ordnung** | |
| `cd_select_vessel` | Active vessel: %s. Tap to switch or add vessel | **Aktives Boot: %s. Tippen, um zu wechseln oder ein Boot hinzuzufügen** | |
| `status_overdue` | overdue | **überfällig** | |
| `status_soon` | due soon | **bald fällig** | |
| `action_cannot_now_defer` | Cannot now — defer | **Geht gerade nicht — zurückstellen** | |
| `action_cancel_deferral` | Cancel deferral | **Zurückstellung aufheben** | |
| `all_clear_title` | All clear | **Alles in Ordnung** | |
| `all_clear_with_deferred` | All clear · %1$d deferred | **Alles in Ordnung · %1$d zurückgestellt** | |
| `all_clear_description` | No maintenance due right now. | **Zurzeit ist keine Wartung fällig.** | |
| `now_quick_actions` | Quick actions | **Schnellaktionen** | |
| `now_action_quick_log` | Log service | **Wartung eintragen** | |
| `now_action_enter_hours` | Engine hours | **Motorstunden** | |
| `now_action_checklists` | Checklists | **Checklisten** | |
| `now_section_upcoming` | Upcoming maintenance | **Anstehende Wartung** | |
| `now_recent_activity` | Latest log entry | **Letzter Eintrag** | |
| `now_all_journal` | All records › | **Alle Einträge ›** | |
| `now_unrecorded_title` | Unrecorded components | **Komponenten ohne Einträge** | |
| `now_unrecorded_subtitle` | Add initial hours or service date to activate schedule. | **Anfangsstunden oder Datum der letzten Wartung eintragen, um den Wartungsplan zu aktivieren.** | |
| `action_view_nodes` | View | **Anzeigen** | |
| `quick_log_title` | Quick service log | **Schnelleintrag** | |
| `field_date` | Date | **Datum** | |
| `field_work_type` | Work type | **Art der Arbeit** | |
| `field_meter_hours` | Engine hours | **Motorstunden** | |
| `field_new_expiry` | New expiry date | **Neues Ablaufdatum** | |
| `field_cost` | Cost | **Kosten** | |
| `field_notes` | Notes | **Notizen** | |
| `action_save` | Save | **Speichern** | |
| `action_cancel` | Cancel | **Abbrechen** | |
| `action_previous` | Previous | **Zurück** | |
| `action_next` | Next | **Weiter** | |
| `service_logged_snackbar` | Service record saved | **Wartungseintrag gespeichert** | |
| `defer_title` | Defer maintenance | **Wartung zurückstellen** | |
| `defer_until_label` | DEFER UNTIL | **ZURÜCKSTELLEN BIS** | |
| `defer_reason_label` | REASON (OPTIONAL) | **GRUND (OPTIONAL)** | |
| `defer_option_one_month` | For 1 month | **Um 1 Monat** | |
| `defer_option_haul_out` | Until haul-out | **Bis zum Auskranen** | |
| `defer_option_spring` | Until spring | **Bis zum Frühjahr** | |
| `defer_option_pick_date` | Pick a date… | **Datum wählen…** | |
| `defer_explanation` | Item will disappear from «Pulse» and return automatically at the chosen time. Overdue status is preserved. | **Der Punkt verschwindet aus „Puls“ und kommt zum gewählten Zeitpunkt von selbst zurück. Die Überfälligkeit bleibt erhalten.** | |
| `deferred_snackbar` | Deferred | **Zurückgestellt** | |
| `theme_system` | System | **System** | |
| `theme_light` | Light | **Hell** | |
| `theme_dark` | Dark | **Dunkel** | |
| `theme_sunlight` | Sunlight | **Sonnenlicht** | |
| `theme_night` | Night | **Nacht** | |
| `theme_colorful` | Color | **Farbig** | |
| `detail_back` | Back | **Zurück** | |
| `detail_next_service` | Next maintenance | **Nächste Wartung** | |
| `verify_interval_title` | Check this interval against the manual | **Intervall mit dem Handbuch abgleichen** | |
| `verify_interval_body` | This interval is common practice, not your engine maker\'s requirement. Where the manual says otherwise, follow the manual and change the interval. | **Dieses Intervall ist gängige Praxis, keine Vorgabe Ihres Motorenherstellers. Wo das Handbuch etwas anderes sagt, gilt das Handbuch – ändern Sie dann das Intervall.** | |
| `verify_interval_confirm` | Checked against the manual | **Mit dem Handbuch abgeglichen** | |
| `action_change_interval` | Change interval | **Intervall ändern** | |
| `interval_line` | Interval: %s | **Intervall: %s** | |
| `interval_or` | %1$s or %2$s | **%1$s oder %2$s** | |
| `interval_whichever_first` | %s, whichever comes first | **%s, je nachdem, was zuerst eintritt** | |
| `interval_hours` | %s h | **%s Std.** | |
| `interval_miles` | %s mi | **%s mi** | |
| `interval_dialog_title` | Service interval | **Wartungsintervall** | |
| `interval_dialog_hint` | Leave a field empty if it does not apply. With both filled in, service is due at whichever comes first. | **Lassen Sie ein Feld leer, wenn es nicht zutrifft. Sind beide ausgefüllt, ist die Wartung fällig, je nachdem, was zuerst eintritt.** | |
| `field_interval_days` | Days | **Tage** | |
| `field_interval_miles` | Miles | **Meilen** | |
| `interval_error_days` | A whole number of days, above zero | **Ganze Zahl von Tagen, größer als null** | |
| `interval_error_number` | A number above zero | **Eine Zahl größer als null** | |
| `interval_error_none` | Fill in at least one field | **Mindestens ein Feld ausfüllen** | |
| `detail_stock` | On board spares | **Ersatzteile an Bord** | |
| `detail_passport` | Passport | **Datenblatt** | |
| `detail_history` | Service history | **Wartungshistorie** | |
| `detail_no_spares` | No spares linked to this component. | **Keine Ersatzteile mit dieser Komponente verknüpft.** | |
| `detail_no_history` | No service records yet. | **Noch keine Wartungseinträge.** | |
| `detail_no_interval` | No interval set — maintenance on condition | **Kein Intervall — Wartung nach Zustand** | |
| `detail_not_serviced_yet` | Not serviced yet · interval %s | **Noch nicht gewartet · Intervall %s** | |
| `detail_passport_empty_hint` | Fill in the passport once to forever solve the «which filter» question. Part numbers can be photographed from the nameplate. | **Das Datenblatt einmal ausfüllen, und die Frage „welcher Filter?“ ist für immer beantwortet. Teilenummern lassen sich vom Typenschild abfotografieren.** | |
| `passport_make` | Make | **Hersteller** | |
| `passport_model` | Model | **Modell** | |
| `passport_serial` | Serial | **Seriennummer** | |
| `passport_year` | Year | **Baujahr** | |
| `detail_water_advice` | Your cruising area is %1$s, so %2$s is required. | **Ihr Revier ist %1$s, daher wird eine %2$s benötigt.** | |
| `water_salt` | salt water | **Salzwasser** | |
| `water_fresh` | fresh water | **Süßwasser** | |
| `water_brackish` | brackish water | **Brackwasser** | |
| `anode_zinc` | zinc anode | **Zinkanode** | |
| `anode_magnesium` | magnesium anode | **Magnesiumanode** | |
| `anode_aluminum` | aluminum anode | **Aluminiumanode** | |
| `spare_in_locker` | in %s | **in %s** | |
| `spare_no_location` | no locker | **kein Stauraum** | |
| `spare_per_service` | %s / service | **%s / Wartung** | |
| `crit_high` | Critical | **Kritisch** | |
| `crit_med` | Important | **Wichtig** | |
| `crit_low` | Routine | **Routine** | |
| `search_hint` | Search component, part or spec… | **Komponente, Teil oder Angabe suchen…** | |
| `search_found_via` | found via «%1$s» | **gefunden über „%1$s“** | |
| `search_found_in_spec` | found in passport | **im Datenblatt gefunden** | |
| `search_found_in_notes` | found in notes | **in den Notizen gefunden** | |
| `search_empty` | Nothing found | **Nichts gefunden** | |
| `create_component_title` | New custom component | **Neue eigene Komponente** | |
| `create_component_name` | Component name | **Name der Komponente** | |
| `create_component_name_hint` | e.g. Watermaker Schenker Zen 30 | **z. B. Wassermacher Schenker Zen 30** | |
| `create_component_category` | Category | **Kategorie** | |
| `create_component_criticality` | Criticality | **Wichtigkeit** | |
| `create_component_notes` | Specs / Notes | **Daten / Notizen** | |
| `create_component_notes_hint` | Part number, model, voltage etc. | **Teilenummer, Modell, Spannung usw.** | |
| `create_component_action` | Create component | **Komponente anlegen** | |
| `create_component_empty_search` | Create component «%1$s» | **Komponente „%1$s“ anlegen** | |
| `component_rename` | Rename | **Umbenennen** | |
| `component_rename_title` | Custom component name | **Eigener Name der Komponente** | |
| `component_rename_hint` | Leave blank to use catalog name | **Leer lassen für den Katalognamen** | |
| `add_custom_component` | + Custom component | **+ Eigene Komponente** | |
| `setup_title` | Your boat | **Ihr Boot** | |
| `setup_subtitle` | Answer once and the catalog fills itself in - only the parts your boat actually has. | **Einmal beantworten, und der Katalog füllt sich von selbst — nur mit dem, was Ihr Boot wirklich hat.** | |
| `setup_name_label` | Name | **Name** | |
| `setup_default_name` | My boat | **Mein Boot** | |
| `setup_action` | Build my boat | **Mein Boot anlegen** | |
| `vessel_add_photo` | Photo | **Foto** | |
| `boat_component_count_suffix` | nodes | **Komponenten** | |
| `action_language` | Language | **Sprache** | |
| `passport_add_field` | + Add field | **+ Feld hinzufügen** | |
| `passport_field_name` | Field name | **Feldname** | |
| `work_type_scheduled` | Scheduled | **Planmäßig** | |
| `work_type_repair` | Repair | **Reparatur** | |
| `work_type_inspection` | Inspection | **Inspektion** | |
| `work_type_winterization` | Winterization | **Einwintern** | |
| `work_type_commissioning` | Commissioning | **Auswintern** | |
| `work_type_haul_out` | Haul-out | **Auskranen** | |
| `work_type_launch` | Launch | **Einkranen** | |
| `work_type_upgrade` | Upgrade | **Nachrüstung** | |
| `work_type_warranty` | Warranty | **Garantie** | |
| `work_type_pre_sale` | Pre-sale | **Vor dem Verkauf** | |
| `detail_photos` | Condition photos | **Zustandsfotos** | |
| `detail_add_photo` | Photo | **Foto** | |
| `detail_add_more_photos` | Add photo | **Foto hinzufügen** | |
| `detail_no_photos` | No photos yet. Track wear and corrosion over time. | **Noch keine Fotos. So lassen sich Verschleiß und Korrosion über die Zeit verfolgen.** | |
| `action_change_photo` | Change photo | **Foto ändern** | |
| `action_delete_photo` | Delete photo | **Foto löschen** | |
| `dialog_delete_photo_confirm` | Delete this photo? | **Dieses Foto löschen?** | |
| `photo_caption_label` | Caption (optional) | **Bildunterschrift (optional)** | |
| `photo_add_dialog_title` | Add Condition Photo | **Zustandsfoto hinzufügen** | |
| `picker_gallery` | Gallery | **Galerie** | |
| `picker_files` | Files | **Dateien** | |
| `vessel_photo_dialog_title` | Vessel Photo | **Foto des Boots** | |
| `vessel_photo_change_label` | change the vessel photo | **das Foto des Boots ändern** | |
| `vessel_photo_add_label` | add a vessel photo | **ein Foto des Boots hinzufügen** | |
| `date_today` | Today | **Heute** | |
| `date_yesterday` | Yesterday | **Gestern** | |
| `date_pick_other` | Calendar… | **Kalender…** | |
| `date_picker_title` | Select date | **Datum wählen** | |
| `action_edit` | Edit | **Bearbeiten** | |
| `action_delete` | Delete | **Löschen** | |
| `edit_record_title` | Edit service record | **Wartungseintrag bearbeiten** | |
| `action_delete_record` | Delete record | **Eintrag löschen** | |
| `dialog_delete_record_title` | Delete service record? | **Wartungseintrag löschen?** | |
| `dialog_delete_record_message` | This service record from %1$s (%2$s) will be permanently deleted from the log. | **Dieser Wartungseintrag vom %1$s (%2$s) wird endgültig aus dem Wartungsbuch gelöscht.** | |
| `journal_empty_title` | Ship\'s log is empty | **Das Wartungsbuch ist leer** | |
| `journal_empty_desc` | All completed maintenance, service, repairs, and inspections are recorded here. Tapping \"Done\" on Pulse or inside a component card writes an entry here with date, engine hours, and photo. | **Hier stehen alle erledigten Wartungen, Reparaturen und Inspektionen. Ein Tipp auf „Erledigt“ in „Puls“ oder auf der Komponentenkarte schreibt einen Eintrag mit Datum, Motorstunden und Foto.** | |
| `journal_all_categories` | All components | **Alle Komponenten** | |
| `journal_spent_total` | %1$s spent | **%1$s ausgegeben** | |
| `journal_gap_after_previous` | %1$s after previous | **%1$s nach dem vorigen** | |
| `journal_gap_and` | and | **und** | |
| `journal_imported_from` | from import · sheet \"%1$s\", row %2$d | **aus Import · Blatt „%1$s“, Zeile %2$d** | |
| `journal_precision_year` | only year known | **nur Jahr bekannt** | |
| `journal_precision_month` | only month known | **nur Monat bekannt** | |
| `journal_uncataloged_category` | code missing in catalog | **Code fehlt im Katalog** | |
| `boat_search_hint` | Search components on boat… | **Komponenten an Bord suchen…** | |
| `boat_add_component` | + Add component | **+ Komponente hinzufügen** | |
| `catalog_add_title` | Add component to boat | **Komponente zum Boot hinzufügen** | |
| `catalog_search_hint` | Search catalog… | **Katalog durchsuchen…** | |
| `catalog_already_on_boat` | On boat | **An Bord** | |
| `catalog_action_add` | + Add | **+ Hinzufügen** | |
| `action_hide_from_boat` | Hide from boat | **Vom Boot ausblenden** | |
| `dialog_hide_component_title` | Hide component from boat? | **Komponente vom Boot ausblenden?** | |
| `dialog_hide_component_message` | The component \"%s\" will be hidden from the boat list. All journal records and service history will be preserved. | **Die Komponente „%s“ wird in der Bootsliste ausgeblendet. Alle Einträge und die Wartungshistorie bleiben erhalten.** | |
| `catalog_from_catalog_tab` | From catalog | **Aus dem Katalog** | |
| `catalog_custom_tab` | Custom component | **Eigene Komponente** | |
| `boat_empty_components` | No components on the boat yet. | **Noch keine Komponenten an Bord.** | |
| `settings_title` | Settings | **Einstellungen** | |
| `settings_currency` | Currency | **Währung** | |
| `settings_currency_subtitle` | Select the currency used for service and spare parts costs | **Währung für Wartungs- und Ersatzteilkosten wählen** | |
| `settings_currency_custom` | Custom currency | **Eigene Währung** | |
| `settings_currency_custom_hint` | Symbol or code, e.g. CAD, CHF, AUD… | **Symbol oder Code, z. B. CAD, CHF, AUD…** | |
| `settings_theme` | Color theme | **Farbschema** | |
| `settings_language` | Language | **Sprache** | |
| `currency_uah` | Ukrainian Hryvnia (₴) | **Ukrainische Hrywnja (₴)** | |
| `currency_usd` | US Dollar ($) | **US-Dollar ($)** | |
| `currency_eur` | Euro (€) | **Euro (€)** | |
| `currency_gbp` | British Pound (£) | **Britisches Pfund (£)** | |
| `currency_pln` | Polish Zloty (zł) | **Polnischer Złoty (zł)** | |
| `currency_sek` | Swedish Krona (kr) | **Schwedische Krone (kr)** | |
| `currency_nok` | Norwegian Krone (kr) | **Norwegische Krone (kr)** | |
| `currency_try` | Turkish Lira (₺) | **Türkische Lira (₺)** | |
| `lang_uk` | Українська | **Українська** | |
| `lang_en` | English | **English** | |
| `lang_de` | Deutsch | **Deutsch** | |
| `lang_es` | Español | **Español** | |
| `settings_units_title` | Units of measurement | **Maßeinheiten** | |
| `settings_units_subtitle` | Configure measurement units across the app | **Maßeinheiten für die ganze App festlegen** | |
| `settings_units_preset_metric` | Metric | **Metrisch** | |
| `settings_units_preset_nautical` | Nautical | **Nautisch** | |
| `settings_unit_length` | Boat dimensions &amp; length | **Bootsmaße &amp; Länge** | |
| `settings_unit_distance` | Distance &amp; log | **Distanz &amp; Logge** | |
| `settings_unit_volume` | Liquid &amp; tank volume | **Flüssigkeiten &amp; Tankvolumen** | |
| `settings_unit_pressure` | Pressure | **Druck** | |
| `settings_unit_temperature` | Temperature | **Temperatur** | |
| `unit_metres` | Metres (m) | **Meter (m)** | |
| `unit_feet` | Feet (ft) | **Fuß (ft)** | |
| `unit_nautical_miles` | Nautical miles (NM) | **Seemeilen (sm)** | |
| `unit_kilometres` | Kilometres (km) | **Kilometer (km)** | |
| `unit_statute_miles` | Statute miles (mi) | **Landmeilen (mi)** | |
| `unit_litres` | Litres (L) | **Liter (l)** | |
| `unit_gallons_us` | US Gallons (gal) | **US-Gallonen (gal)** | |
| `unit_gallons_uk` | UK Gallons (gal) | **Britische Gallonen (gal)** | |
| `unit_bar` | Bar (bar) | **Bar (bar)** | |
| `unit_psi` | PSI (psi) | **PSI (psi)** | |
| `unit_celsius` | Celsius (°C) | **Celsius (°C)** | |
| `unit_fahrenheit` | Fahrenheit (°F) | **Fahrenheit (°F)** | |
| `unit_short_m` | m | **m** | |
| `unit_short_ft` | ft | **ft** | |
| `unit_short_nm` | NM | **sm** | |
| `unit_short_km` | km | **km** | |
| `unit_short_mi` | mi | **mi** | |
| `unit_short_l` | L | **l** | |
| `unit_short_gal_us` | US gal | **US gal** | |
| `unit_short_gal_uk` | UK gal | **UK gal** | |
| `unit_short_bar` | bar | **bar** | |
| `unit_short_psi` | psi | **psi** | |
| `unit_short_celsius` | °C | **°C** | |
| `unit_short_fahrenheit` | °F | **°F** | |
| `unit_short_mm` | mm | **mm** | |
| `unit_short_in` | in | **in** | |
| `unit_short_l_min` | l/min | **l/min** | |
| `unit_short_gal_us_h` | US gal/h | **US gal/h** | |
| `unit_short_gal_uk_h` | UK gal/h | **UK gal/h** | |
| `passport_unit_recorded` | Unit recorded: %1$s | **Erfasste Einheit: %1$s** | |
| `spec_label_amount_used` | Paint used | **Verbrauchte Farbe** | |
| `settings_notifications_title` | Maintenance reminders | **Wartungserinnerungen** | |
| `settings_notifications_subtitle` | Daily background check of overdue and upcoming service | **Tägliche Prüfung im Hintergrund auf überfällige und anstehende Wartung** | |
| `settings_notifications_enabled` | Enable reminders | **Erinnerungen aktivieren** | |
| `settings_notifications_blocked` | Android does not allow HullBeat to show notifications, so no reminders will arrive. | **Android erlaubt HullBeat keine Benachrichtigungen – Erinnerungen kommen nicht an.** | |
| `settings_notifications_open_system` | Allow in Android settings | **In den Android-Einstellungen erlauben** | |
| `settings_notifications_advance` | Advance notice | **Vorlauf** | |
| `settings_notifications_days_7` | 7 days | **7 Tage** | |
| `settings_notifications_days_14` | 14 days | **14 Tage** | |
| `settings_notifications_days_30` | 30 days | **30 Tage** | |
| `settings_data_title` | Data &amp; storage | **Daten &amp; Speicher** | |
| `settings_data_subtitle` | 100% offline-first local storage on this device | **100% offline, lokal auf diesem Gerät gespeichert** | |
| `settings_export_journal` | Export ship\'s log (CSV) | **Wartungsbuch exportieren (CSV)** | |
| `settings_about_title` | About HullBeat | **Über HullBeat** | |
| `settings_about_version` | Version %1$s | **Version %1$s** | |
| `settings_about_desc` | HullBeat is an offline-first ship\'s log and maintenance planner designed for sailboats and motor yachts. | **HullBeat ist ein Offline-Wartungsbuch und Wartungsplaner für Segelboote und Motoryachten.** | |
| `settings_about_github` | GitHub repository | **GitHub-Repository** | |
| `settings_feedback` | Suggestions &amp; feedback | **Vorschläge &amp; Feedback** | |
| `settings_feedback_no_mail_app` | No email app on this phone. Write to %1$s | **Keine E-Mail-App auf diesem Telefon. Schreiben Sie an %1$s** | |
| `store_tab_all` | All spares | **Alle Ersatzteile** | |
| `store_tab_tobuy` | To buy | **Einkaufen** | |
| `store_tab_locations` | Lockers | **Stauräume** | |
| `store_search_hint` | Search part, number, brand… | **Teil, Nummer, Marke suchen…** | |
| `store_stat_gone` | Out of stock: %1$d | **Nicht vorrätig: %1$d** | |
| `store_stat_low` | Low: %1$d | **Knapp: %1$d** | |
| `store_add_part` | Add part | **Teil hinzufügen** | |
| `store_edit_part` | Edit part | **Teil bearbeiten** | |
| `store_delete_part` | Delete from stock | **Aus dem Bestand löschen** | |
| `store_add_location` | Add locker | **Stauraum hinzufügen** | |
| `store_edit_location` | Edit locker | **Stauraum bearbeiten** | |
| `store_delete_location` | Delete locker | **Stauraum löschen** | |
| `store_delete_location_confirm` | Delete this locker? Parts in it will remain without an assigned locker. | **Diesen Stauraum löschen? Die Teile darin bleiben ohne Stauraum erhalten.** | |
| `store_delete_part_confirm` | Delete \"%1$s\" from stock? | **„%1$s“ aus dem Bestand löschen?** | |
| `store_part_name` | Part name | **Bezeichnung** | |
| `store_part_name_hint` | e.g. Oil filter | **z. B. Ölfilter** | |
| `store_part_number` | Part No. | **Teilenr.** | |
| `store_part_number_hint` | e.g. 875812 | **z. B. 875812** | |
| `store_part_mfr` | Manufacturer / Brand | **Hersteller / Marke** | |
| `store_part_mfr_hint` | e.g. Volvo Penta, Yanmar | **z. B. Volvo Penta, Yanmar** | |
| `store_part_qty` | Quantity aboard | **Menge an Bord** | |
| `store_part_min_qty` | Stock norm | **Sollbestand** | |
| `store_part_min_qty_hint` | How many to keep aboard | **Wie viele an Bord bleiben sollen** | |
| `store_location_label` | Storage locker | **Stauraum** | |
| `store_no_location` | No locker | **Kein Stauraum** | |
| `store_new_location_prompt` | + Create new locker… | **+ Neuen Stauraum anlegen…** | |
| `store_location_name` | Locker / compartment name | **Name des Stauraums / Fachs** | |
| `store_location_name_hint` | e.g. Engine bay, Aft locker | **z. B. Motorraum, Achterpiek** | |
| `store_location_qr` | Label code (optional) | **Etikettcode (optional)** | |
| `store_location_qr_hint` | e.g. R-1, L-A1 | **z. B. R-1, L-A1** | |
| `store_part_notes` | Notes | **Notizen** | |
| `store_part_notes_hint` | Where bought, compatibility, etc. | **Bezugsquelle, Kompatibilität usw.** | |
| `store_linked_components` | Used by components | **Verwendet für** | |
| `store_linked_components_hint` | Select components this part fits | **Komponenten wählen, zu denen das Teil passt** | |
| `store_filter_components_hint` | Filter components… | **Komponenten filtern…** | |
| `store_empty_title` | Stock is empty | **Der Bestand ist leer** | |
| `store_empty_desc` | Add your first spare parts or create standard boat lockers for easy inventory. | **Fügen Sie die ersten Ersatzteile hinzu oder legen Sie die üblichen Stauräume an.** | |
| `store_seed_locations_btn` | Create standard boat lockers | **Übliche Stauräume anlegen** | |
| `store_tobuy_empty` | All required spare parts are well-stocked aboard. | **Alle nötigen Ersatzteile sind ausreichend an Bord.** | |
| `store_locations_empty` | No lockers added yet. Create your first locker or use defaults. | **Noch keine Stauräume. Legen Sie den ersten an oder nutzen Sie die Vorgaben.** | |
| `store_loc_empty` | No parts in this locker yet. | **In diesem Stauraum liegen noch keine Teile.** | |
| `store_qty_badge` | %1$s · norm %2$s | **%1$s · Soll %2$s** | |
| `store_out_of_stock` | OUT | **LEER** | |
| `store_low_stock` | LOW | **KNAPP** | |
| `store_default_loc_engine` | Engine bay | **Motorraum** | |
| `store_default_loc_cockpit` | Cockpit locker (lazarette) | **Backskiste (Cockpit)** | |
| `store_default_loc_chart` | Nav station | **Navigationsecke** | |
| `store_default_loc_forepeak` | Forepeak locker | **Vorschiff-Stauraum** | |
| `store_default_loc_galley` | Galley locker | **Pantry-Schapp** | |
| `store_items_count` | %1$d pcs | **%1$d Stk.** | |
| `import_history_title` | Import history | **Importverlauf** | |
| `import_history_empty` | No imports yet | **Noch keine Importe** | |
| `import_rollback_btn` | Undo | **Rückgängig** | |
| `import_rollback_success` | Import undone | **Import rückgängig gemacht** | |
| `field_map_date` | Date | **Datum** | |
| `field_map_desc` | Description | **Beschreibung** | |
| `field_map_work_type` | Work type | **Art der Arbeit** | |
| `field_map_hours` | Hours | **Stunden** | |
| `field_map_cost` | Cost | **Kosten** | |
| `field_map_currency` | Currency | **Währung** | |
| `field_map_who` | Performed by | **Ausgeführt von** | |
| `field_map_notes` | Notes | **Notizen** | |
| `field_map_component` | Component | **Komponente** | |
| `settings_backup_title` | Backup &amp; restore | **Sicherung &amp; Wiederherstellung** | |
| `settings_backup_subtitle` | Full database backup to storage or cloud | **Vollständige Datenbanksicherung in den Speicher oder die Cloud** | |
| `settings_backup_unencrypted` | The file is not encrypted: anyone who opens it sees your records and photos. Keep it somewhere you trust. | **Die Datei ist nicht verschlüsselt: Wer sie öffnet, sieht Einträge und Fotos. Bewahren Sie sie an einem vertrauenswürdigen Ort auf.** | |
| `settings_backup_create` | Create backup | **Sicherung erstellen** | |
| `settings_backup_restore` | Restore from backup | **Aus Sicherung wiederherstellen** | |
| `settings_backup_success` | Backup created successfully | **Sicherung erstellt** | |
| `settings_backup_failed` | Failed to create backup | **Sicherung fehlgeschlagen** | |
| `settings_restore_success` | Database restored successfully | **Datenbank wiederhergestellt** | |
| `settings_restore_invalid` | Invalid backup file | **Ungültige Sicherungsdatei** | |
| `settings_restore_newer` | Backup from a newer HullBeat. Update the app and retry. | **Sicherung aus neuerem HullBeat. Bitte App aktualisieren.** | |
| `settings_restore_no_space` | Not enough space to restore. Your data is unchanged. | **Zu wenig Speicher. Ihre Daten bleiben unverändert.** | |
| `settings_export_store` | Export inventory &amp; lockers (CSV) | **Bestand &amp; Stauräume exportieren (CSV)** | |
| `settings_export_excel` | Export to Excel (.xlsx) | **Nach Excel exportieren (.xlsx)** | |
| `settings_export_excel_success` | Excel file exported successfully | **Excel-Datei exportiert** | |
| `settings_export_excel_failed` | Failed to export Excel file | **Excel-Export fehlgeschlagen** | |
| `settings_export_excel_empty` | No data to export | **Keine Daten zum Exportieren** | |
| `excel_sheet_journal` | Service log | **Wartungsbuch** | |
| `excel_sheet_store` | Store &amp; Lockers | **Bestand &amp; Stauräume** | |
| `settings_export_pdf_passport` | Boat passport &amp; service book (PDF) | **Bootsdatenblatt &amp; Serviceheft (PDF)** | |
| `settings_export_pdf_success` | Boat passport exported successfully | **Bootsdatenblatt exportiert** | |
| `settings_export_pdf_failed` | Failed to create PDF document | **PDF konnte nicht erstellt werden** | |
| `pdf_passport_title` | Vessel Passport &amp; Service Book | **Bootsdatenblatt &amp; Serviceheft** | |
| `pdf_section_vessel` | Vessel Specifications | **Bootsdaten** | |
| `pdf_section_equipment` | Equipment Registry | **Ausrüstungsverzeichnis** | |
| `pdf_section_equipment_photos` | Equipment | **Ausrüstung** | |
| `pdf_more_photos` | +%1$d more | **+%1$d weitere** | |
| `pdf_section_service` | Service History | **Wartungshistorie** | |
| `pdf_field_summary` | Summary | **Zusammenfassung** | |
| `pdf_field_records_count` | Service records: %1$d | **Wartungseinträge: %1$d** | |
| `pdf_field_total_cost` | Total expenses: %1$s | **Gesamtkosten: %1$s** | |
| `pdf_field_components_count` | Registered components: %1$d | **Erfasste Komponenten: %1$d** | |
| `pdf_field_make_model` | Make / Model | **Werft / Modell** | |
| `pdf_field_year` | Year Built | **Baujahr** | |
| `pdf_field_hin` | HIN / CIN | **HIN / CIN** | |
| `pdf_field_length` | Length | **Länge** | |
| `pdf_field_hull` | Hull Type | **Rumpfform** | |
| `pdf_field_engine` | Engine Type | **Motorart** | |
| `pdf_field_drive` | Drive Type | **Antriebsart** | |
| `pdf_field_fuel` | Fuel Type | **Kraftstoff** | |
| `pdf_field_serial` | S/N | **S/N** | |
| `pdf_field_installed` | Installed | **Eingebaut** | |
| `pdf_footer_page` | Page %1$d | **Seite %1$d** | |
| `pdf_footer_brand` | HullBeat Ship\'s Log | **HullBeat Wartungsbuch** | |
| `pdf_compiled_by` | Compiled by owner via HullBeat | **Vom Eigner mit HullBeat erstellt** | |
| `now_enter_hours` | Enter hours | **Stunden eintragen** | |
| `error_photo_save_failed` | Could not save the photo | **Foto konnte nicht gespeichert werden** | |
| `error_number_invalid` | Digits and one decimal point only, e.g. 512.5 | **Nur Ziffern und ein Komma, z. B. 512,5** | |
| `error_meter_required` | This job needs the engine hours | **Für diese Arbeit sind die Betriebsstunden nötig** | |
| `error_expiry_before_service` | Must be later than the date of the service | **Muss nach dem Datum der Arbeiten liegen** | |
| `hint_expiry_needed` | Without this date there will be no reminder | **Ohne dieses Datum gibt es keine Erinnerung** | |
| `error_record_save_failed` | Could not save the record. Nothing was lost - try again. | **Eintrag konnte nicht gespeichert werden. Nichts ist verloren - bitte erneut versuchen.** | |
| `action_ok` | OK | **OK** | |
| `nav_menu_title` | Menu | **Menü** | |
| `drawer_switch_vessel` | Switch vessel | **Boot wechseln** | |
| `state_expanded` | Expanded | **Ausgeklappt** | |
| `state_collapsed` | Collapsed | **Eingeklappt** | |
| `action_close` | Close | **Schließen** | |
| `icons_overview_sections` | App sections | **Bereiche der App** | |
| `icons_overview_categories` | Component categories (%d) | **Komponentenkategorien (%d)** | |
| `toast_jobs_saved` | one: Saved %d job / other: Saved %d jobs | **one: %d Arbeit gespeichert / other: %d Arbeiten gespeichert** | |
| `checklist_will_log` | one: %d tick will write a record and reset the interval / other: %d ticks will write records and reset their intervals | **one: %d Haken schreibt einen Eintrag und setzt das Intervall zurück / other: %d Haken schreiben Einträge und setzen ihre Intervalle zurück** | |
| `checklist_not_applicable` | one: %d item does not apply to this boat / other: %d items do not apply to this boat | **one: %d Punkt gilt nicht für dieses Boot / other: %d Punkte gelten nicht für dieses Boot** | |
| `checklist_skipped` | one: %d item skipped / other: %d items skipped | **one: %d Punkt übersprungen / other: %d Punkte übersprungen** | |
| `due_overdue_days` | one: %d day overdue / other: %d days overdue | **one: %d Tag überfällig / other: %d Tage überfällig** | |
| `due_in_hours` | one: in %d engine hour / other: in %d engine hours | **one: in %d Motorstunde / other: in %d Motorstunden** | |
| `onboarding_seeded` | one: %d maintenance item created / other: %d maintenance items created | **one: %d Wartungspunkt angelegt / other: %d Wartungspunkte angelegt** | |
| `boat_nodes` | one: %d component / other: %d components | **one: %d Komponente / other: %d Komponenten** | |
| `boat_categories_in` | one: in %d category / other: in %d categories | **one: in %d Kategorie / other: in %d Kategorien** | |
| `boat_categories` | one: %d category / other: %d categories | **one: %d Kategorie / other: %d Kategorien** | |
| `boat_categories_dead` | one: %d category does not apply / other: %d categories do not apply | **one: %d Kategorie trifft nicht zu / other: %d Kategorien treffen nicht zu** | |
| `log_records` | one: %d record / other: %d records | **one: %d Eintrag / other: %d Einträge** | |
| `log_gap_days` | one: %d day / other: %d days | **one: %d Tag / other: %d Tage** | |
| `log_gap_hours` | one: %d engine hour / other: %d engine hours | **one: %d Motorstunde / other: %d Motorstunden** | |
| `log_photos` | one: %d photo / other: %d photos | **one: %d Foto / other: %d Fotos** | |
| `store_qty` | one: %d pc / other: %d pcs | **one: %d Stk. / other: %d Stk.** | |
| `store_items` | one: %d item / other: %d items | **one: %d Teil / other: %d Teile** | |
| `store_locations_in` | one: in %d locker / other: in %d lockers | **one: in %d Stauraum / other: in %d Stauräumen** | |
| `interval_days` | one: %d day / other: %d days | **one: %d Tag / other: %d Tage** | |
| `due_in_days` | one: in %d day / other: in %d days | **one: in %d Tag / other: in %d Tagen** | |
| `boat_own_nodes` | one: %d of them your own / other: %d of them your own | **one: davon %d eigene / other: davon %d eigene** | |
| `now_needs_meter` | one: %d node is waiting for its first engine hours / other: %d nodes are waiting for their first engine hours | **one: %d Komponente wartet auf die ersten Motorstunden / other: %d Komponenten warten auf die ersten Motorstunden** | |
| `now_needs_first_record` | one: %d node has no service history yet / other: %d nodes have no service history yet | **one: %d Komponente hat noch keine Wartungshistorie / other: %d Komponenten haben noch keine Wartungshistorie** | |
| `snack_logged_photos_failed` | one: Logged, but %d photo could not be saved / other: Logged, but %d photos could not be saved | **one: Eingetragen, aber %d Foto konnte nicht gespeichert werden / other: Eingetragen, aber %d Fotos konnten nicht gespeichert werden** | |
| `departure_check_chip` | one: Departure check · %d item / other: Departure check · %d items | **one: Check vor dem Auslaufen · %d Punkt / other: Check vor dem Auslaufen · %d Punkte** | |
| `store_stat_items` | one: %d item / other: %d items | **one: %d Teil / other: %d Teile** | |
| `store_stat_lockers` | one: in %d locker / other: in %d lockers | **one: in %d Stauraum / other: in %d Stauräumen** | |
| `import_rows_count` | one: %d row / other: %d rows | **one: %d Zeile / other: %d Zeilen** | |
| `settings_backup_skipped_files` | one: Backup created, but %d attachment could not be included (file unavailable) / other: Backup created, but %d attachments could not be included (files unavailable) | **one: Sicherung erstellt, aber %d Anhang konnte nicht aufgenommen werden (Datei nicht verfügbar) / other: Sicherung erstellt, aber %d Anhänge konnten nicht aufgenommen werden (Dateien nicht verfügbar)** | |
