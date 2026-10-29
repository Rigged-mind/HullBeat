# Interface strings to review - es

412 strings and 29 plurals drafted. None is confirmed. Check two things on each row: is it what a sailor would say, and does it fit a phone screen? Navigation tabs and buttons have the least room.

`%s`, `%1$s`, `%d` are filled in by the app; keep them, move them freely within the sentence.

## Check these first

The places where a wrong word costs most. Each names the rows it is about, with the current draft.

1. **Safety lines: «líneas de vida» (jackstays along the deck) and «líneas de anclaje» (the tether from the harness) sit next to each other in the safety list. Clear enough, or should the tether be «cabo de anclaje» / «cabo de seguridad»?**
   - `cmp:jackstays` - Líneas de vida y puntos de anclaje
   - `cmp:harnesses_tethers` - Arneses y líneas de anclaje
   - `chk:chk_sf_jackstays` - Revisar las líneas de vida de punta a punta y sus puntos de anclaje
   - `chk:chk_sf_harnesses` - Tirar fuerte de cada costura, cambiar lo que esté descolorido por el sol y anotar la fecha de la etiqueta
2. **«inversor» means both the gearbox and the DC-AC inverter. The drafts say «Inversor-reductor» and «Inversor de corriente», and the search alias «inversor» finds both. Is that how Spanish owners tell them apart?**
   - `cmp:gearbox` - Inversor-reductor
   - `cmp:inverter` - Inversor de corriente
   - `syn:gearbox` - inversor; reductora; caja de cambios
   - `syn:inverter` - inversor; inversor de corriente
3. **ITB and ITV are Spanish institutions. Are these labels acceptable for Spanish-speaking owners elsewhere, or should the main label be neutral with ITB only as an example?**
   - `cmp:national_inspection` - Inspección técnica (ITB)
   - `cmp:trailer_documents` - Matrícula e ITV del remolque
   - `hint:Scheme (ITB/BSS/Revisione/Bootszeugnis)` - Régimen (ITB / BSS / Revisione / Bootszeugnis)
4. **«patente» or «antifouling»: which should be the name shown, and which only a search alias?**
   - `cmp:antifouling` - Antifouling (patente)
   - `chk:chk_cm_antifouling` - Dar patente; enmascarar los ánodos y la cara del transductor
   - `syn:antifouling` - patente; pintura antiincrustante; antiincrustante; autopulimentable; matriz dura
5. **«turbina» as a search alias for the raw-water impeller: common on Spanish boats, or confusing next to the turbocharger («turbo»)?**
   - `syn:raw_water_impeller` - impulsor; rodete; turbina; bomba de agua salada; Jabsco
   - `cmp:raw_water_impeller` - Impulsor de la bomba de agua salada
   - `cmp:turbo` - Turbo / intercooler


## All strings

| Key | English | Draft | Correction |
|---|---|---|---|
| `notification_channel_due` | Maintenance due | **Mantenimiento pendiente** | |
| `notification_channel_due_description` | Reminders when a job is due or overdue. | **Avisos cuando un trabajo vence o está vencido.** | |
| `action_mark_done` | Done | **Hecho** | |
| `action_renew` | Renew… | **Renovar…** | |
| `action_mark_done_with_hours` | Done… | **Hecho…** | |
| `due_unknown` | Due date unknown | **Vencimiento desconocido** | |
| `due_unknown_needs_meter` | Enter initial engine hours to start forecast | **Introduce las horas de motor iniciales para empezar la previsión** | |
| `due_unknown_needs_schedule` | Configure maintenance schedule | **Configura el plan de mantenimiento** | |
| `snack_logged` | Logged | **Registrado** | |
| `action_undo` | Undo | **Deshacer** | |
| `snack_deferral_cancelled` | Deferral cancelled | **Aplazamiento cancelado** | |
| `snack_deferred_until` | Deferred until %1$s | **Aplazado hasta el %1$s** | |
| `deferred_until` | deferred until %1$s | **aplazado hasta el %1$s** | |
| `deferred_until_reason` | %1$s · until %2$s | **%1$s · hasta el %2$s** | |
| `placeholder_boat` | Component catalogue and equipment passports. | **Catálogo de componentes y fichas técnicas de los equipos.** | |
| `placeholder_log` | Ship\'s log, timeline and search. | **Historial, cronología y búsqueda.** | |
| `placeholder_store` | Spares, lockers and stock levels. | **Repuestos, pañoles y existencias.** | |
| `summary_overdue` | %d overdue | **Vencidos: %d** | |
| `summary_soon` | %d due soon | **Vencen pronto: %d** | |
| `summary_more` | and %d more | **y %d más** | |
| `record_from_notification` | Logged from the notification | **Registrado desde la notificación** | |
| `nav_now` | Pulse | **Pulso** | |
| `nav_boat` | Boat | **Barco** | |
| `nav_log` | Log | **Historial** | |
| `nav_store` | Spares | **Repuestos** | |
| `departure_check_done_today` | Pre-departure check completed today | **Revisión antes de zarpar hecha hoy** | |
| `departure_checklist_title` | Pre-departure check | **Revisión antes de zarpar** | |
| `departure_check_fleet_partial` | Pre-departure check · done: %s | **Revisión antes de zarpar · hecha: %s** | |
| `action_confirm_departure_check` | Confirm departure check | **Confirmar revisión** | |
| `action_reset_departure_check` | New departure | **Nueva salida** | |
| `departure_check_all_required` | Check all items to confirm | **Marca todos los puntos para confirmar** | |
| `departure_check_empty_list` | Could not load the checklist | **No se pudo cargar la lista** | |
| `action_record_meter` | Log engine hours | **Registrar horas de motor** | |
| `record_meter_title` | Log Engine Hours | **Registrar horas de motor** | |
| `meter_reading_saved` | Reading recorded | **Lectura guardada** | |
| `meter_current_reading` | Current reading | **Lectura actual** | |
| `meter_hours_unit` | hrs | **h** | |
| `meter_decrease_warning` | Reading is less than previous (%.1f hrs). Decreasing will disable hour-based maintenance predictions. | **La lectura es menor que la anterior (%.1f h). Si la bajas, se desactivará la previsión de mantenimiento por horas.** | |
| `meter_decrease_confirm` | I understand, save new reading | **Entendido, guardar la nueva lectura** | |
| `fleet_all` | All | **Todos** | |
| `action_add_vessel` | Add vessel… | **Añadir barco…** | |
| `vessel_select_title` | Select vessel | **Elegir barco** | |
| `vessel_status_all_clear` | All clear | **Todo en orden** | |
| `cd_select_vessel` | Active vessel: %s. Tap to switch or add vessel | **Barco activo: %s. Toca para cambiar o añadir un barco** | |
| `status_overdue` | overdue | **vencido** | |
| `status_soon` | due soon | **vence pronto** | |
| `action_cannot_now_defer` | Cannot now — defer | **Ahora no puedo — aplazar** | |
| `action_cancel_deferral` | Cancel deferral | **Cancelar aplazamiento** | |
| `all_clear_title` | All clear | **Todo en orden** | |
| `all_clear_with_deferred` | All clear · %1$d deferred | **Todo en orden · aplazados: %1$d** | |
| `all_clear_description` | No maintenance due right now. | **Ahora mismo no hay mantenimiento pendiente.** | |
| `now_quick_actions` | Quick actions | **Acciones rápidas** | |
| `now_action_quick_log` | Log service | **Registrar mantenimiento** | |
| `now_action_enter_hours` | Engine hours | **Horas de motor** | |
| `now_action_checklists` | Checklists | **Listas** | |
| `now_section_upcoming` | Upcoming maintenance | **Próximo mantenimiento** | |
| `now_recent_activity` | Latest log entry | **Último registro** | |
| `now_all_journal` | All records › | **Todos los registros ›** | |
| `now_unrecorded_title` | Unrecorded components | **Componentes sin registros** | |
| `now_unrecorded_subtitle` | Add initial hours or service date to activate schedule. | **Añade las horas iniciales o la fecha del último mantenimiento para activar el plan.** | |
| `action_view_nodes` | View | **Ver** | |
| `quick_log_title` | Quick service log | **Registro rápido** | |
| `field_date` | Date | **Fecha** | |
| `field_work_type` | Work type | **Tipo de trabajo** | |
| `field_meter_hours` | Engine hours | **Horas de motor** | |
| `field_new_expiry` | New expiry date | **Nueva fecha de caducidad** | |
| `field_cost` | Cost | **Coste** | |
| `field_notes` | Notes | **Notas** | |
| `action_save` | Save | **Guardar** | |
| `action_cancel` | Cancel | **Cancelar** | |
| `action_previous` | Previous | **Anterior** | |
| `action_next` | Next | **Siguiente** | |
| `service_logged_snackbar` | Service record saved | **Registro de mantenimiento guardado** | |
| `defer_title` | Defer maintenance | **Aplazar mantenimiento** | |
| `defer_until_label` | DEFER UNTIL | **APLAZAR HASTA** | |
| `defer_reason_label` | REASON (OPTIONAL) | **MOTIVO (OPCIONAL)** | |
| `defer_option_one_month` | For 1 month | **1 mes** | |
| `defer_option_haul_out` | Until haul-out | **Hasta la varada** | |
| `defer_option_spring` | Until spring | **Hasta la primavera** | |
| `defer_option_pick_date` | Pick a date… | **Elegir fecha…** | |
| `defer_explanation` | Item will disappear from «Pulse» and return automatically at the chosen time. Overdue status is preserved. | **El punto desaparecerá de «Pulso» y volverá solo en la fecha elegida. El estado de vencido se mantiene.** | |
| `deferred_snackbar` | Deferred | **Aplazado** | |
| `theme_system` | System | **Sistema** | |
| `theme_light` | Light | **Claro** | |
| `theme_dark` | Dark | **Oscuro** | |
| `theme_sunlight` | Sunlight | **Pleno sol** | |
| `theme_night` | Night | **Noche** | |
| `theme_colorful` | Color | **Color** | |
| `detail_back` | Back | **Atrás** | |
| `detail_next_service` | Next maintenance | **Próximo mantenimiento** | |
| `verify_interval_title` | Check this interval against the manual | **Compruebe este intervalo con el manual** | |
| `verify_interval_body` | This interval is common practice, not your engine maker\'s requirement. Where the manual says otherwise, follow the manual and change the interval. | **Este intervalo es la práctica habitual, no un requisito del fabricante de su motor. Si el manual dice otra cosa, siga el manual y cambie el intervalo.** | |
| `verify_interval_confirm` | Checked against the manual | **Comprobado con el manual** | |
| `action_change_interval` | Change interval | **Cambiar intervalo** | |
| `interval_line` | Interval: %s | **Intervalo: %s** | |
| `interval_or` | %1$s or %2$s | **%1$s o %2$s** | |
| `interval_whichever_first` | %s, whichever comes first | **%s, lo que ocurra primero** | |
| `interval_hours` | %s h | **%s h** | |
| `interval_miles` | %s mi | **%s mi** | |
| `interval_dialog_title` | Service interval | **Intervalo de mantenimiento** | |
| `interval_dialog_hint` | Leave a field empty if it does not apply. With both filled in, service is due at whichever comes first. | **Deje un campo vacío si no se aplica. Si ambos están rellenos, el mantenimiento vence con lo que ocurra primero.** | |
| `field_interval_days` | Days | **Días** | |
| `field_interval_miles` | Miles | **Millas** | |
| `interval_error_days` | A whole number of days, above zero | **Un número entero de días, mayor que cero** | |
| `interval_error_number` | A number above zero | **Un número mayor que cero** | |
| `interval_error_none` | Fill in at least one field | **Rellene al menos un campo** | |
| `detail_stock` | On board spares | **Repuestos a bordo** | |
| `detail_passport` | Passport | **Ficha técnica** | |
| `detail_history` | Service history | **Historial de mantenimiento** | |
| `detail_no_spares` | No spares linked to this component. | **No hay repuestos vinculados a este componente.** | |
| `detail_no_history` | No service records yet. | **Aún no hay registros de mantenimiento.** | |
| `detail_no_interval` | No interval set — maintenance on condition | **Sin intervalo — mantenimiento según estado** | |
| `detail_not_serviced_yet` | Not serviced yet · interval %s | **Aún sin mantenimiento · intervalo %s** | |
| `detail_passport_empty_hint` | Fill in the passport once to forever solve the «which filter» question. Part numbers can be photographed from the nameplate. | **Rellena la ficha una vez y olvídate para siempre de la pregunta «¿qué filtro lleva?». Las referencias se pueden fotografiar de la placa del fabricante.** | |
| `passport_make` | Make | **Fabricante** | |
| `passport_model` | Model | **Modelo** | |
| `passport_serial` | Serial | **Número de serie** | |
| `passport_year` | Year | **Año** | |
| `detail_water_advice` | Your cruising area is %1$s, so %2$s is required. | **Navegas en %1$s, así que necesitas un %2$s.** | |
| `water_salt` | salt water | **agua salada** | |
| `water_fresh` | fresh water | **agua dulce** | |
| `water_brackish` | brackish water | **agua salobre** | |
| `anode_zinc` | zinc anode | **ánodo de zinc** | |
| `anode_magnesium` | magnesium anode | **ánodo de magnesio** | |
| `anode_aluminum` | aluminum anode | **ánodo de aluminio** | |
| `spare_in_locker` | in %s | **en %s** | |
| `spare_no_location` | no locker | **sin pañol** | |
| `spare_per_service` | %s / service | **%s / mantenimiento** | |
| `crit_high` | Critical | **Crítico** | |
| `crit_med` | Important | **Importante** | |
| `crit_low` | Routine | **Rutina** | |
| `search_hint` | Search component, part or spec… | **Buscar componente, pieza o dato…** | |
| `search_found_via` | found via «%1$s» | **encontrado por «%1$s»** | |
| `search_found_in_spec` | found in passport | **encontrado en la ficha** | |
| `search_found_in_notes` | found in notes | **encontrado en las notas** | |
| `search_empty` | Nothing found | **No se ha encontrado nada** | |
| `create_component_title` | New custom component | **Nuevo componente propio** | |
| `create_component_name` | Component name | **Nombre del componente** | |
| `create_component_name_hint` | e.g. Watermaker Schenker Zen 30 | **p. ej. potabilizadora Schenker Zen 30** | |
| `create_component_category` | Category | **Categoría** | |
| `create_component_criticality` | Criticality | **Importancia** | |
| `create_component_notes` | Specs / Notes | **Datos / notas** | |
| `create_component_notes_hint` | Part number, model, voltage etc. | **Referencia, modelo, tensión, etc.** | |
| `create_component_action` | Create component | **Crear componente** | |
| `create_component_empty_search` | Create component «%1$s» | **Crear componente «%1$s»** | |
| `component_rename` | Rename | **Renombrar** | |
| `component_rename_title` | Custom component name | **Nombre propio del componente** | |
| `component_rename_hint` | Leave blank to use catalog name | **Déjalo vacío para usar el nombre del catálogo** | |
| `add_custom_component` | + Custom component | **+ Componente propio** | |
| `setup_title` | Your boat | **Tu barco** | |
| `setup_subtitle` | Answer once and the catalog fills itself in - only the parts your boat actually has. | **Responde una vez y el catálogo se rellena solo, con lo que tu barco tiene de verdad.** | |
| `setup_name_label` | Name | **Nombre** | |
| `setup_default_name` | My boat | **Mi barco** | |
| `setup_action` | Build my boat | **Crear mi barco** | |
| `vessel_add_photo` | Photo | **Foto** | |
| `boat_component_count_suffix` | nodes | **componentes** | |
| `action_language` | Language | **Idioma** | |
| `passport_add_field` | + Add field | **+ Añadir campo** | |
| `passport_field_name` | Field name | **Nombre del campo** | |
| `work_type_scheduled` | Scheduled | **Programado** | |
| `work_type_repair` | Repair | **Reparación** | |
| `work_type_inspection` | Inspection | **Inspección** | |
| `work_type_winterization` | Winterization | **Invernaje** | |
| `work_type_commissioning` | Commissioning | **Puesta a punto** | |
| `work_type_haul_out` | Haul-out | **Varada** | |
| `work_type_launch` | Launch | **Botadura** | |
| `work_type_upgrade` | Upgrade | **Mejora** | |
| `work_type_warranty` | Warranty | **Garantía** | |
| `work_type_pre_sale` | Pre-sale | **Antes de la venta** | |
| `detail_photos` | Condition photos | **Fotos del estado** | |
| `detail_add_photo` | Photo | **Foto** | |
| `detail_add_more_photos` | Add photo | **Añadir foto** | |
| `detail_no_photos` | No photos yet. Track wear and corrosion over time. | **Aún no hay fotos. Sirven para seguir el desgaste y la corrosión con el tiempo.** | |
| `action_change_photo` | Change photo | **Cambiar foto** | |
| `action_delete_photo` | Delete photo | **Borrar foto** | |
| `dialog_delete_photo_confirm` | Delete this photo? | **¿Borrar esta foto?** | |
| `photo_caption_label` | Caption (optional) | **Pie de foto (opcional)** | |
| `photo_add_dialog_title` | Add Condition Photo | **Añadir foto del estado** | |
| `picker_gallery` | Gallery | **Galería** | |
| `picker_files` | Files | **Archivos** | |
| `vessel_photo_dialog_title` | Vessel Photo | **Foto del barco** | |
| `date_today` | Today | **Hoy** | |
| `date_yesterday` | Yesterday | **Ayer** | |
| `date_pick_other` | Calendar… | **Calendario…** | |
| `date_picker_title` | Select date | **Elegir fecha** | |
| `action_edit` | Edit | **Editar** | |
| `action_delete` | Delete | **Borrar** | |
| `edit_record_title` | Edit service record | **Editar registro de mantenimiento** | |
| `action_delete_record` | Delete record | **Borrar registro** | |
| `dialog_delete_record_title` | Delete service record? | **¿Borrar el registro de mantenimiento?** | |
| `dialog_delete_record_message` | This service record from %1$s (%2$s) will be permanently deleted from the log. | **Este registro del %1$s (%2$s) se borrará definitivamente del historial.** | |
| `journal_empty_title` | Ship\'s log is empty | **El historial está vacío** | |
| `journal_empty_desc` | All completed maintenance, service, repairs, and inspections are recorded here. Tapping \"Done\" on Pulse or inside a component card writes an entry here with date, engine hours, and photo. | **Aquí se guarda todo el mantenimiento, las reparaciones y las inspecciones hechas. Al tocar «Hecho» en Pulso o en la ficha de un componente se escribe un registro con fecha, horas de motor y foto.** | |
| `journal_all_categories` | All components | **Todos los componentes** | |
| `journal_spent_total` | %1$s spent | **%1$s gastados** | |
| `journal_gap_after_previous` | %1$s after previous | **%1$s después del anterior** | |
| `journal_gap_and` | and | **y** | |
| `journal_imported_from` | from import · sheet \"%1$s\", row %2$d | **importado · hoja «%1$s», fila %2$d** | |
| `journal_precision_year` | only year known | **solo se sabe el año** | |
| `journal_precision_month` | only month known | **solo se sabe el mes** | |
| `journal_uncataloged_category` | code missing in catalog | **código ausente del catálogo** | |
| `boat_search_hint` | Search components on boat… | **Buscar componentes a bordo…** | |
| `boat_add_component` | + Add component | **+ Añadir componente** | |
| `catalog_add_title` | Add component to boat | **Añadir componente al barco** | |
| `catalog_search_hint` | Search catalog… | **Buscar en el catálogo…** | |
| `catalog_already_on_boat` | On boat | **A bordo** | |
| `catalog_action_add` | + Add | **+ Añadir** | |
| `action_hide_from_boat` | Hide from boat | **Ocultar del barco** | |
| `dialog_hide_component_title` | Hide component from boat? | **¿Ocultar el componente del barco?** | |
| `dialog_hide_component_message` | The component \"%s\" will be hidden from the boat list. All journal records and service history will be preserved. | **El componente «%s» se ocultará de la lista del barco. Se conservan todos los registros y el historial de mantenimiento.** | |
| `catalog_from_catalog_tab` | From catalog | **Del catálogo** | |
| `catalog_custom_tab` | Custom component | **Componente propio** | |
| `boat_empty_components` | No components on the boat yet. | **Aún no hay componentes a bordo.** | |
| `settings_title` | Settings | **Ajustes** | |
| `settings_currency` | Currency | **Moneda** | |
| `settings_currency_subtitle` | Select the currency used for service and spare parts costs | **Elige la moneda de los costes de mantenimiento y repuestos** | |
| `settings_currency_custom` | Custom currency | **Moneda personalizada** | |
| `settings_currency_custom_hint` | Symbol or code, e.g. CAD, CHF, AUD… | **Símbolo o código, p. ej. CAD, CHF, AUD…** | |
| `settings_theme` | Color theme | **Tema de color** | |
| `settings_language` | Language | **Idioma** | |
| `currency_uah` | Ukrainian Hryvnia (₴) | **Grivna ucraniana (₴)** | |
| `currency_usd` | US Dollar ($) | **Dólar estadounidense ($)** | |
| `currency_eur` | Euro (€) | **Euro (€)** | |
| `currency_gbp` | British Pound (£) | **Libra esterlina (£)** | |
| `currency_pln` | Polish Zloty (zł) | **Esloti polaco (zł)** | |
| `currency_sek` | Swedish Krona (kr) | **Corona sueca (kr)** | |
| `currency_nok` | Norwegian Krone (kr) | **Corona noruega (kr)** | |
| `currency_try` | Turkish Lira (₺) | **Lira turca (₺)** | |
| `lang_uk` | Українська | **Українська** | |
| `lang_en` | English | **English** | |
| `lang_de` | Deutsch | **Deutsch** | |
| `lang_es` | Español | **Español** | |
| `settings_units_title` | Units of measurement | **Unidades de medida** | |
| `settings_units_subtitle` | Configure measurement units across the app | **Configura las unidades en toda la app** | |
| `settings_units_preset_metric` | Metric | **Métrico** | |
| `settings_units_preset_nautical` | Nautical | **Náutico** | |
| `settings_unit_length` | Boat dimensions &amp; length | **Dimensiones y eslora** | |
| `settings_unit_distance` | Distance &amp; log | **Distancia y corredera** | |
| `settings_unit_volume` | Liquid &amp; tank volume | **Líquidos y capacidad de depósitos** | |
| `settings_unit_pressure` | Pressure | **Presión** | |
| `settings_unit_temperature` | Temperature | **Temperatura** | |
| `unit_metres` | Metres (m) | **Metros (m)** | |
| `unit_feet` | Feet (ft) | **Pies (ft)** | |
| `unit_nautical_miles` | Nautical miles (NM) | **Millas náuticas (M)** | |
| `unit_kilometres` | Kilometres (km) | **Kilómetros (km)** | |
| `unit_statute_miles` | Statute miles (mi) | **Millas terrestres (mi)** | |
| `unit_litres` | Litres (L) | **Litros (l)** | |
| `unit_gallons_us` | US Gallons (gal) | **Galones EE. UU. (gal)** | |
| `unit_gallons_uk` | UK Gallons (gal) | **Galones imperiales (gal)** | |
| `unit_bar` | Bar (bar) | **Bar (bar)** | |
| `unit_psi` | PSI (psi) | **PSI (psi)** | |
| `unit_celsius` | Celsius (°C) | **Celsius (°C)** | |
| `unit_fahrenheit` | Fahrenheit (°F) | **Fahrenheit (°F)** | |
| `unit_short_m` | m | **m** | |
| `unit_short_ft` | ft | **ft** | |
| `unit_short_nm` | NM | **M** | |
| `unit_short_km` | km | **km** | |
| `unit_short_mi` | mi | **mi** | |
| `unit_short_l` | L | **l** | |
| `unit_short_gal_us` | US gal | **gal EE. UU.** | |
| `unit_short_gal_uk` | UK gal | **gal imp.** | |
| `unit_short_bar` | bar | **bar** | |
| `unit_short_psi` | psi | **psi** | |
| `unit_short_celsius` | °C | **°C** | |
| `unit_short_fahrenheit` | °F | **°F** | |
| `unit_short_mm` | mm | **mm** | |
| `unit_short_in` | in | **pulg.** | |
| `unit_short_l_min` | l/min | **l/min** | |
| `unit_short_gal_us_h` | US gal/h | **gal EE. UU./h** | |
| `unit_short_gal_uk_h` | UK gal/h | **gal imp./h** | |
| `passport_unit_recorded` | Unit recorded: %1$s | **Unidad registrada: %1$s** | |
| `spec_label_amount_used` | Paint used | **Pintura usada** | |
| `settings_notifications_title` | Maintenance reminders | **Avisos de mantenimiento** | |
| `settings_notifications_subtitle` | Daily background check of overdue and upcoming service | **Comprobación diaria en segundo plano del mantenimiento vencido y próximo** | |
| `settings_notifications_enabled` | Enable reminders | **Activar avisos** | |
| `settings_notifications_blocked` | Android does not allow HullBeat to show notifications, so no reminders will arrive. | **Android no permite a HullBeat mostrar notificaciones, así que no llegarán avisos.** | |
| `settings_notifications_open_system` | Allow in Android settings | **Permitir en los ajustes de Android** | |
| `settings_notifications_advance` | Advance notice | **Antelación** | |
| `settings_notifications_days_7` | 7 days | **7 días** | |
| `settings_notifications_days_14` | 14 days | **14 días** | |
| `settings_notifications_days_30` | 30 days | **30 días** | |
| `settings_data_title` | Data &amp; storage | **Datos y almacenamiento** | |
| `settings_data_subtitle` | 100% offline-first local storage on this device | **100% sin conexión, guardado en este dispositivo** | |
| `settings_export_journal` | Export ship\'s log (CSV) | **Exportar historial (CSV)** | |
| `settings_about_title` | About HullBeat | **Acerca de HullBeat** | |
| `settings_about_version` | Version %1$s | **Versión %1$s** | |
| `settings_about_desc` | HullBeat is an offline-first ship\'s log and maintenance planner designed for sailboats and motor yachts. | **HullBeat es un historial de mantenimiento y planificador sin conexión para veleros y yates a motor.** | |
| `settings_about_github` | GitHub repository | **Repositorio en GitHub** | |
| `settings_feedback` | Suggestions &amp; feedback | **Sugerencias y comentarios** | |
| `settings_feedback_no_mail_app` | No email app on this phone. Write to %1$s | **No hay ninguna app de correo en este teléfono. Escriba a %1$s** | |
| `store_tab_all` | All spares | **Todos los repuestos** | |
| `store_tab_tobuy` | To buy | **Por comprar** | |
| `store_tab_locations` | Lockers | **Pañoles** | |
| `store_search_hint` | Search part, number, brand… | **Buscar pieza, referencia, marca…** | |
| `store_stat_gone` | Out of stock: %1$d | **Agotados: %1$d** | |
| `store_stat_low` | Low: %1$d | **Pocos: %1$d** | |
| `store_add_part` | Add part | **Añadir pieza** | |
| `store_edit_part` | Edit part | **Editar pieza** | |
| `store_delete_part` | Delete from stock | **Quitar del inventario** | |
| `store_add_location` | Add locker | **Añadir pañol** | |
| `store_edit_location` | Edit locker | **Editar pañol** | |
| `store_delete_location` | Delete locker | **Borrar pañol** | |
| `store_delete_location_confirm` | Delete this locker? Parts in it will remain without an assigned locker. | **¿Borrar este pañol? Las piezas que contiene se quedarán sin pañol asignado.** | |
| `store_delete_part_confirm` | Delete \"%1$s\" from stock? | **¿Quitar «%1$s» del inventario?** | |
| `store_part_name` | Part name | **Nombre de la pieza** | |
| `store_part_name_hint` | e.g. Oil filter | **p. ej. filtro de aceite** | |
| `store_part_number` | Part No. | **Ref.** | |
| `store_part_number_hint` | e.g. 875812 | **p. ej. 875812** | |
| `store_part_mfr` | Manufacturer / Brand | **Fabricante / marca** | |
| `store_part_mfr_hint` | e.g. Volvo Penta, Yanmar | **p. ej. Volvo Penta, Yanmar** | |
| `store_part_qty` | Quantity aboard | **Cantidad a bordo** | |
| `store_part_min_qty` | Stock norm | **Mínimo a bordo** | |
| `store_part_min_qty_hint` | How many to keep aboard | **Cuántas tener siempre a bordo** | |
| `store_location_label` | Storage locker | **Pañol** | |
| `store_no_location` | No locker | **Sin pañol** | |
| `store_new_location_prompt` | + Create new locker… | **+ Crear pañol nuevo…** | |
| `store_location_name` | Locker / compartment name | **Nombre del pañol / compartimento** | |
| `store_location_name_hint` | e.g. Engine bay, Aft locker | **p. ej. cámara del motor, cofre de popa** | |
| `store_location_qr` | Label code (optional) | **Código de etiqueta (opcional)** | |
| `store_location_qr_hint` | e.g. R-1, L-A1 | **p. ej. R-1, L-A1** | |
| `store_part_notes` | Notes | **Notas** | |
| `store_part_notes_hint` | Where bought, compatibility, etc. | **Dónde se compró, compatibilidad, etc.** | |
| `store_linked_components` | Used by components | **Sirve para** | |
| `store_linked_components_hint` | Select components this part fits | **Elige los componentes a los que sirve esta pieza** | |
| `store_filter_components_hint` | Filter components… | **Filtrar componentes…** | |
| `store_empty_title` | Stock is empty | **El inventario está vacío** | |
| `store_empty_desc` | Add your first spare parts or create standard boat lockers for easy inventory. | **Añade tus primeros repuestos o crea los pañoles habituales de un barco.** | |
| `store_seed_locations_btn` | Create standard boat lockers | **Crear los pañoles habituales** | |
| `store_tobuy_empty` | All required spare parts are well-stocked aboard. | **Todos los repuestos necesarios están a bordo en cantidad suficiente.** | |
| `store_locations_empty` | No lockers added yet. Create your first locker or use defaults. | **Aún no hay pañoles. Crea el primero o usa los habituales.** | |
| `store_loc_empty` | No parts in this locker yet. | **Aún no hay piezas en este pañol.** | |
| `store_qty_badge` | %1$s · norm %2$s | **%1$s · mínimo %2$s** | |
| `store_out_of_stock` | OUT | **AGOTADO** | |
| `store_low_stock` | LOW | **POCO** | |
| `store_default_loc_engine` | Engine bay | **Cámara del motor** | |
| `store_default_loc_cockpit` | Cockpit locker (lazarette) | **Cofre de bañera (popa)** | |
| `store_default_loc_chart` | Nav station | **Mesa de cartas** | |
| `store_default_loc_forepeak` | Forepeak locker | **Pañol de proa** | |
| `store_default_loc_galley` | Galley locker | **Armario de la cocina** | |
| `store_items_count` | %1$d pcs | **Cant.: %1$d** | |
| `import_history_title` | Import history | **Importaciones anteriores** | |
| `import_history_empty` | No imports yet | **Aún no hay importaciones** | |
| `import_rollback_btn` | Undo | **Deshacer** | |
| `import_rollback_success` | Import undone | **Importación deshecha** | |
| `field_map_date` | Date | **Fecha** | |
| `field_map_desc` | Description | **Descripción** | |
| `field_map_work_type` | Work type | **Tipo de trabajo** | |
| `field_map_hours` | Hours | **Horas** | |
| `field_map_cost` | Cost | **Coste** | |
| `field_map_currency` | Currency | **Moneda** | |
| `field_map_who` | Performed by | **Realizado por** | |
| `field_map_notes` | Notes | **Notas** | |
| `field_map_component` | Component | **Componente** | |
| `settings_backup_title` | Backup &amp; restore | **Copia de seguridad y restauración** | |
| `settings_backup_subtitle` | Full database backup to storage or cloud | **Copia completa de la base de datos en el almacenamiento o la nube** | |
| `settings_backup_unencrypted` | The file is not encrypted: anyone who opens it sees your records and photos. Keep it somewhere you trust. | **El archivo no está cifrado: quien lo abra verá registros y fotos. Guárdelo en un lugar de confianza.** | |
| `settings_backup_create` | Create backup | **Crear copia de seguridad** | |
| `settings_backup_restore` | Restore from backup | **Restaurar desde copia** | |
| `settings_backup_success` | Backup created successfully | **Copia de seguridad creada** | |
| `settings_backup_failed` | Failed to create backup | **No se pudo crear la copia** | |
| `settings_restore_success` | Database restored successfully | **Base de datos restaurada** | |
| `settings_restore_invalid` | Invalid backup file | **Archivo de copia no válido** | |
| `settings_restore_newer` | Backup from a newer HullBeat. Update the app and retry. | **Copia de un HullBeat más reciente. Actualice la app.** | |
| `settings_restore_no_space` | Not enough space to restore. Your data is unchanged. | **No hay espacio para restaurar. Sus datos no han cambiado.** | |
| `settings_export_store` | Export inventory &amp; lockers (CSV) | **Exportar inventario y pañoles (CSV)** | |
| `settings_export_excel` | Export to Excel (.xlsx) | **Exportar a Excel (.xlsx)** | |
| `settings_export_excel_success` | Excel file exported successfully | **Archivo de Excel exportado** | |
| `settings_export_excel_failed` | Failed to export Excel file | **No se pudo exportar a Excel** | |
| `settings_export_excel_empty` | No data to export | **No hay datos que exportar** | |
| `excel_sheet_journal` | Service log | **Historial** | |
| `excel_sheet_store` | Store &amp; Lockers | **Inventario y pañoles** | |
| `settings_export_pdf_passport` | Boat passport &amp; service book (PDF) | **Ficha del barco y libro de mantenimiento (PDF)** | |
| `settings_export_pdf_success` | Boat passport exported successfully | **Ficha del barco exportada** | |
| `settings_export_pdf_failed` | Failed to create PDF document | **No se pudo crear el PDF** | |
| `pdf_passport_title` | Vessel Passport &amp; Service Book | **Ficha técnica y libro de mantenimiento** | |
| `pdf_section_vessel` | Vessel Specifications | **Datos del barco** | |
| `pdf_section_equipment` | Equipment Registry | **Inventario de equipos** | |
| `pdf_section_equipment_photos` | Equipment | **Equipos** | |
| `pdf_more_photos` | +%1$d more | **+%1$d más** | |
| `pdf_section_service` | Service History | **Historial de mantenimiento** | |
| `pdf_field_summary` | Summary | **Resumen** | |
| `pdf_field_records_count` | Service records: %1$d | **Registros de mantenimiento: %1$d** | |
| `pdf_field_total_cost` | Total expenses: %1$s | **Gasto total: %1$s** | |
| `pdf_field_components_count` | Registered components: %1$d | **Componentes registrados: %1$d** | |
| `pdf_field_make_model` | Make / Model | **Astillero / modelo** | |
| `pdf_field_year` | Year Built | **Año de construcción** | |
| `pdf_field_hin` | HIN / CIN | **HIN / CIN** | |
| `pdf_field_length` | Length | **Eslora** | |
| `pdf_field_hull` | Hull Type | **Tipo de casco** | |
| `pdf_field_engine` | Engine Type | **Tipo de motor** | |
| `pdf_field_drive` | Drive Type | **Transmisión** | |
| `pdf_field_fuel` | Fuel Type | **Combustible** | |
| `pdf_field_serial` | S/N | **N.º de serie** | |
| `pdf_field_installed` | Installed | **Instalado** | |
| `pdf_footer_page` | Page %1$d | **Página %1$d** | |
| `pdf_footer_brand` | HullBeat Ship\'s Log | **HullBeat · historial del barco** | |
| `pdf_compiled_by` | Compiled by owner via HullBeat | **Elaborado por el propietario con HullBeat** | |
| `now_enter_hours` | Enter hours | **Introducir horas** | |
| `error_photo_save_failed` | Could not save the photo | **No se pudo guardar la foto** | |
| `error_number_invalid` | Not a number | **No es un número** | |
| `error_meter_required` | This job needs the engine hours | **Este trabajo necesita las horas de motor** | |
| `error_expiry_before_service` | Must be later than the date of the service | **Debe ser posterior a la fecha del trabajo** | |
| `hint_expiry_needed` | Without this date there will be no reminder | **Sin esta fecha no habrá recordatorio** | |
| `error_record_save_failed` | Could not save the record. Nothing was lost - try again. | **No se pudo guardar el registro. No se perdió nada: inténtelo de nuevo.** | |
| `action_ok` | OK | **Aceptar** | |
| `nav_menu_title` | Menu | **Menú** | |
| `drawer_switch_vessel` | Switch vessel | **Cambiar de barco** | |
| `state_expanded` | Expanded | **Desplegado** | |
| `state_collapsed` | Collapsed | **Plegado** | |
| `action_close` | Close | **Cerrar** | |
| `icons_overview_sections` | App sections | **Secciones de la app** | |
| `icons_overview_categories` | Component categories (%d) | **Categorías de componentes (%d)** | |
| `toast_jobs_saved` | one: Saved %d job / other: Saved %d jobs | **one: %d trabajo guardado / many: %d trabajos guardados / other: %d trabajos guardados** | |
| `checklist_will_log` | one: %d tick will write a record and reset the interval / other: %d ticks will write records and reset their intervals | **one: %d marca creará un registro y reiniciará el intervalo / many: %d marcas crearán registros y reiniciarán sus intervalos / other: %d marcas crearán registros y reiniciarán sus intervalos** | |
| `checklist_not_applicable` | one: %d item does not apply to this boat / other: %d items do not apply to this boat | **one: %d punto no se aplica a este barco / many: %d puntos no se aplican a este barco / other: %d puntos no se aplican a este barco** | |
| `checklist_skipped` | one: %d item skipped / other: %d items skipped | **one: %d punto omitido / many: %d puntos omitidos / other: %d puntos omitidos** | |
| `due_overdue_days` | one: %d day overdue / other: %d days overdue | **one: vencido hace %d día / many: vencido hace %d días / other: vencido hace %d días** | |
| `due_in_hours` | one: in %d engine hour / other: in %d engine hours | **one: en %d hora de motor / many: en %d horas de motor / other: en %d horas de motor** | |
| `onboarding_seeded` | one: %d maintenance item created / other: %d maintenance items created | **one: %d punto de mantenimiento creado / many: %d puntos de mantenimiento creados / other: %d puntos de mantenimiento creados** | |
| `boat_nodes` | one: %d component / other: %d components | **one: %d componente / many: %d componentes / other: %d componentes** | |
| `boat_categories_in` | one: in %d category / other: in %d categories | **one: en %d categoría / many: en %d categorías / other: en %d categorías** | |
| `boat_categories` | one: %d category / other: %d categories | **one: %d categoría / many: %d categorías / other: %d categorías** | |
| `boat_categories_dead` | one: %d category does not apply / other: %d categories do not apply | **one: %d categoría no se aplica / many: %d categorías no se aplican / other: %d categorías no se aplican** | |
| `log_records` | one: %d record / other: %d records | **one: %d registro / many: %d registros / other: %d registros** | |
| `log_gap_days` | one: %d day / other: %d days | **one: %d día / many: %d días / other: %d días** | |
| `log_gap_hours` | one: %d engine hour / other: %d engine hours | **one: %d hora de motor / many: %d horas de motor / other: %d horas de motor** | |
| `log_photos` | one: %d photo / other: %d photos | **one: %d foto / many: %d fotos / other: %d fotos** | |
| `store_qty` | one: %d pc / other: %d pcs | **one: %d ud. / many: %d uds. / other: %d uds.** | |
| `store_items` | one: %d item / other: %d items | **one: %d pieza / many: %d piezas / other: %d piezas** | |
| `store_locations_in` | one: in %d locker / other: in %d lockers | **one: en %d pañol / many: en %d pañoles / other: en %d pañoles** | |
| `interval_days` | one: %d day / other: %d days | **one: %d día / many: %d días / other: %d días** | |
| `due_in_days` | one: in %d day / other: in %d days | **one: en %d día / many: en %d días / other: en %d días** | |
| `boat_own_nodes` | one: %d of them your own / other: %d of them your own | **one: %d de ellos propio / many: %d de ellos propios / other: %d de ellos propios** | |
| `now_needs_meter` | one: %d node is waiting for its first engine hours / other: %d nodes are waiting for their first engine hours | **one: %d componente espera sus primeras horas de motor / many: %d componentes esperan sus primeras horas de motor / other: %d componentes esperan sus primeras horas de motor** | |
| `now_needs_first_record` | one: %d node has no service history yet / other: %d nodes have no service history yet | **one: %d componente aún no tiene historial / many: %d componentes aún no tienen historial / other: %d componentes aún no tienen historial** | |
| `snack_logged_photos_failed` | one: Logged, but %d photo could not be saved / other: Logged, but %d photos could not be saved | **one: Registrado, pero no se pudo guardar %d foto / many: Registrado, pero no se pudieron guardar %d fotos / other: Registrado, pero no se pudieron guardar %d fotos** | |
| `departure_check_chip` | one: Departure check · %d item / other: Departure check · %d items | **one: Revisión antes de zarpar · %d punto / many: Revisión antes de zarpar · %d puntos / other: Revisión antes de zarpar · %d puntos** | |
| `store_stat_items` | one: %d item / other: %d items | **one: %d pieza / many: %d piezas / other: %d piezas** | |
| `store_stat_lockers` | one: in %d locker / other: in %d lockers | **one: en %d pañol / many: en %d pañoles / other: en %d pañoles** | |
| `import_rows_count` | one: %d row / other: %d rows | **one: %d fila / many: %d filas / other: %d filas** | |
| `settings_backup_skipped_files` | one: Backup created, but %d attachment could not be included (file unavailable) / other: Backup created, but %d attachments could not be included (files unavailable) | **one: Copia creada, pero no se pudo incluir %d adjunto (archivo no disponible) / many: Copia creada, pero no se pudieron incluir %d adjuntos (archivos no disponibles) / other: Copia creada, pero no se pudieron incluir %d adjuntos (archivos no disponibles)** | |
