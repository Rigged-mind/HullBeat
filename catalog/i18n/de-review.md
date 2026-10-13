# Terms to review - de

700 of 700 need confirmation from someone who sails and maintains a boat in this language. `check` is a contested or borrowed term; `descr` means no settled term exists and the wording is descriptive.

One question for each: **is this what it is called on the water?**

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


## All terms

| Key | English | Draft | Status | Correction |
|---|---|---|---|---|
| `axis_cooling` | Engine cooling | **Motorkühlung** | check | |
| `axis_drive` | Drive | **Antrieb** | check | |
| `axis_engine` | Engine | **Motor** | check | |
| `axis_extras` | What she has aboard | **Ausstattung an Bord** | check | |
| `axis_hull` | Hull type | **Rumpfform** | check | |
| `axis_keel` | Keel | **Kiel** | check | |
| `axis_rig` | Rig | **Takelung** | check | |
| `axis_storage` | Where she spends the winter | **Überwinterung** | check | |
| `axis_water` | Water | **Revier** | check | |
| `cat_documents` | Documents & admin | **Dokumente** | check | |
| `cat_drivetrain` | Drivetrain | **Antriebsstrang** | check | |
| `cat_elec_ac` | AC electrical (230/120V) | **Landstrom & 230/120 V** | check | |
| `cat_elec_dc` | DC electrical (12/24V) | **Bordnetz 12/24 V** | check | |
| `cat_engine_main` | Main engine | **Hauptmotor** | check | |
| `cat_fuel` | Fuel system | **Kraftstoffanlage** | check | |
| `cat_galley` | Galley & interior | **Pantry & Innenausbau** | check | |
| `cat_gas` | Gas system (LPG/CNG) | **Gasanlage** | check | |
| `cat_genset` | Generator | **Generator** | check | |
| `cat_ground_tackle` | Anchoring & mooring | **Ankern & Festmachen** | check | |
| `cat_hull` | Hull & underwater | **Rumpf & Unterwasserschiff** | check | |
| `cat_hvac` | Heating, ventilation, air conditioning | **Heizung, Lüftung, Klima** | check | |
| `cat_nav` | Navigation & electronics | **Navigation & Elektronik** | check | |
| `cat_other` | Other / custom | **Sonstiges** | check | |
| `cat_plumbing` | Bilge & water systems | **Bilge & Wassersysteme** | check | |
| `cat_running_rig` | Spars & running rigging | **Rigg & laufendes Gut** | check | |
| `cat_safety` | Safety & emergency | **Sicherheit & Notfall** | check | |
| `cat_sails` | Sails | **Segel** | check | |
| `cat_standing_rig` | Standing rigging | **Stehendes Gut** | check | |
| `cat_tender` | Tender & outboard | **Beiboot & Außenborder** | check | |
| `cat_trailer` | Trailer | **Bootsanhänger** | check | |
| `chk_cm_anodes` | Replace anodes at 50% or less, and keep antifouling and grease off them and off their contact faces | **Anoden ab 50 % Abtrag ersetzen; kein Antifouling und kein Fett auf die Anoden oder ihre Kontaktflächen** | check | |
| `chk_cm_antifouling` | Antifoul; mask the anodes and the transducer face | **Antifouling streichen; Anoden und Geberfläche abkleben** | check | |
| `chk_cm_belt` | Check belt tension and look for black dust under it | **Riemenspannung prüfen und auf schwarzen Abrieb darunter achten** | check | |
| `chk_cm_bilge_alarm` | Test the high water alarm; it is the one thing that wakes you | **Bilgealarm testen; er ist das Einzige, was Sie nachts weckt** | check | |
| `chk_cm_bilge_pumps` | Test both bilge pumps and lift the float switch by hand | **Beide Bilgepumpen testen und den Schwimmerschalter von Hand anheben** | check | |
| `chk_cm_charts` | Update the charts before the first passage, not during it | **Seekarten vor dem ersten Törn aktualisieren, nicht unterwegs** | check | |
| `chk_cm_coolant` | Top up coolant and check the antifreeze strength | **Kühlflüssigkeit auffüllen und Frostschutzgehalt prüfen** | check | |
| `chk_cm_drain_plug` | Fit the transom drain plug and pull on it by hand; carry the spare | **Lenzstopfen im Spiegel einsetzen und von Hand daran ziehen; Ersatzstopfen mitführen** | check | |
| `chk_cm_first_start` | First start: water must show at the exhaust within 15 seconds, or stop | **Erster Start: Innerhalb von 15 Sekunden muss Wasser am Auspuff kommen, sonst abstellen** | check | |
| `chk_cm_fuel_filter` | Change the fine filter and bleed the fuel system | **Feinfilter wechseln und die Kraftstoffanlage entlüften** | check | |
| `chk_cm_gas_leak` | Reconnect the bottle and leak-test every joint with soap solution | **Flasche anschließen und jede Verbindung mit Seifenlösung abdrücken** | check | |
| `chk_cm_gelcoat` | Polish and wax the topsides | **Rumpfseiten polieren und wachsen** | check | |
| `chk_cm_halyards` | Pull the halyards back through and check for chafe at the sheaves | **Fallen wieder einziehen und an den Scheiben auf Schamfilen prüfen** | check | |
| `chk_cm_heads` | Flush the antifreeze out of the heads and check the joker valve | **Frostschutz aus der Toilette spülen und das Joker-Ventil prüfen** | check | |
| `chk_cm_hoses` | Squeeze every hose below the waterline and check both clamps on each | **Jeden Schlauch unter der Wasserlinie abdrücken und beide Schellen an jedem Anschluss prüfen** | check | |
| `chk_cm_impeller` | Fit a new impeller and count the blades of the old one | **Neuen Impeller einbauen und die Flügel des alten zählen** | check | |
| `chk_cm_insurance` | Check insurance covers the cruising area you actually plan to use | **Prüfen, ob die Versicherung das Fahrtgebiet abdeckt, das tatsächlich geplant ist** | check | |
| `chk_cm_keel_joint` | Look along the keel joint for a crack opening up | **Kiel-Rumpf-Fuge auf aufgehende Risse absuchen** | check | |
| `chk_cm_leak_check` | Lift the floorboards while she still hangs in the slings: check every seacock, the log fitting and the stern gland for weeping | **Bodenbretter anheben, solange das Boot noch in den Gurten hängt: alle Seeventile, den Loggeber und die Wellendichtung auf Tropfen prüfen** | check | |
| `chk_cm_masthead` | Go up, or book someone to; check sheaves, wiring and the wind unit | **In den Mast gehen oder jemanden beauftragen; Scheiben, Kabel und Windgeber prüfen** | check | |
| `chk_cm_nav_lights` | Test every navigation light, including the ones you never use | **Alle Navigationslichter testen, auch die, die man nie benutzt** | check | |
| `chk_cm_oil_check` | Check the oil level and look for water in it | **Ölstand prüfen und auf Wasser im Öl achten** | check | |
| `chk_cm_rig_walk` | Walk the rig: every split pin in place and taped, every terminal clean | **Rigg ablaufen: jeder Splint gesichert und abgeklebt, jedes Terminal sauber** | check | |
| `chk_cm_rudder` | Compare rudder bearing play with the figure from lay-up | **Ruderlagerspiel mit dem Wert vom Einwintern vergleichen** | check | |
| `chk_cm_safety_list` | Run the safety check — the whole list, not the three things you remember | **Den Sicherheitscheck machen — die ganze Liste, nicht die drei Punkte, an die man sich erinnert** | check | |
| `chk_cm_saildrive_anode` | Replace the saildrive anode and check the diaphragm date | **Saildrive-Anode ersetzen und das Datum der Manschette prüfen** | check | |
| `chk_cm_sails_on` | Bend on the sails and pull each reef in on the mooring, not at sea | **Segel anschlagen und jedes Reff am Liegeplatz einbinden, nicht auf See** | check | |
| `chk_cm_seacocks` | Work every seacock, grease it, and close them all before the lift | **Jedes Seeventil durchbewegen, fetten und vor dem Einkranen alle schließen** | check | |
| `chk_cm_shaft_anode` | Replace the shaft anode | **Wellenanode ersetzen** | check | |
| `chk_cm_transducer` | Refit the paddlewheel; check the blank is out before you launch | **Logge-Paddelrad wieder einsetzen; vor dem Einkranen prüfen, dass der Blindstopfen draußen ist** | check | |
| `chk_cm_vhf` | Radio check on VHF, and test the DSC distress button on the test mode | **UKW-Funkcheck durchführen und die DSC-Notruftaste im Testmodus prüfen** | check | |
| `chk_cm_water_heater` | Bring the calorifier back into circuit and check for leaks | **Boiler wieder in den Kreislauf nehmen und auf Undichtigkeiten prüfen** | check | |
| `chk_cm_water_sanitise` | Refill and sanitise the fresh water system, then flush it twice | **Frischwassersystem füllen, desinfizieren und danach zweimal spülen** | check | |
| `chk_cm_watermaker` | De-pickle the membrane and run to waste until the taste is clean | **Membran entkonservieren und das Produktwasser verwerfen, bis es sauber schmeckt** | check | |
| `chk_cm_watermaker_isolate` | Isolate the watermaker before you chlorinate the tanks: free chlorine wrecks the membrane at a few ppm, and it does not recover | **Wassermacher absperren, bevor die Tanks gechlort werden: freies Chlor zerstört die Membran schon bei wenigen ppm, und sie erholt sich nicht** | check | |
| `chk_commission` | Spring commissioning | **Auswintern** | check | |
| `chk_pd_bilge` | Bilge dry and nothing fouling the float switch | **Bilge trocken, Schwimmerschalter frei** | check | |
| `chk_pd_blower` | Blower run for four minutes, and the bilge sniffed before the starter | **Motorraumlüfter vier Minuten gelaufen und Bilge vor dem Anlassen auf Benzingeruch geprüft** | check | |
| `chk_pd_drain_plug` | Drain plug in and pulled on by hand | **Lenzstopfen drin und von Hand geprüft** | check | |
| `chk_pd_fuel` | Fuel for there, back and a third more checked, valves set | **Kraftstoff für hin, zurück und ein Drittel Reserve geprüft, Hähne gestellt** | check | |
| `chk_pd_lights` | Navigation lights tested if there is any chance of coming back after dark | **Navigationslichter getestet, falls die Rückkehr nach Dunkelheit möglich ist** | check | |
| `chk_pd_lines` | Lines ready to slip and fenders where they will be needed | **Leinen klar zum Loswerfen, Fender dort, wo sie gebraucht werden** | check | |
| `chk_pd_oil` | Oil read off the dipstick and coolant off the expansion tank, not from memory | **Öl am Peilstab und Kühlmittel am Ausgleichsbehälter abgelesen, nicht aus dem Gedächtnis** | check | |
| `chk_pd_seacocks` | Engine and heads intake seacocks open | **Seeventile für Motor und Toilette offen** | check | |
| `chk_predeparture` | Pre-departure check | **Check vor dem Auslaufen** | check | |
| `chk_safety` | Safety check | **Sicherheitscheck** | check | |
| `chk_sf_blower` | Run the blower and feel the outlet; check the ducting has not come adrift | **Motorraumlüfter laufen lassen und am Auslass fühlen; prüfen, ob sich der Schlauch gelöst hat** | check | |
| `chk_sf_bungs` | Check a tapered bung of the right size hangs on its own seacock, not in a locker | **Prüfen, dass an jedem Seeventil ein passender konischer Holzpfropfen hängt, nicht in einem Schapp** | check | |
| `chk_sf_co_smoke` | Test the detectors and enter the sensor date from the back of the unit; the sensor expires, not just the battery | **Melder testen und das Sensordatum von der Geräterückseite eintragen; der Sensor läuft ab, nicht nur die Batterie** | check | |
| `chk_sf_damage_control` | Check the damage control kit: soft patch, tape, wedges | **Leckabwehr-Set prüfen: Lecksegel, Tape, Keile** | check | |
| `chk_sf_drogue` | Open the drogue out; check the bridle and the points it loads (streamed astern, not from the bow) | **Treibanker auslegen; Hahnepot und die Punkte prüfen, die er belastet (wird achtern ausgebracht, nicht vom Bug)** | check | |
| `chk_sf_emerg_antenna` | Check the emergency antenna and that its connector actually fits your set | **Notantenne prüfen und ob ihr Stecker wirklich an das eigene Funkgerät passt** | check | |
| `chk_sf_emergency_tools` | Check the cutting tools are somewhere the crew can find them in the dark | **Prüfen, dass das Schneidwerkzeug dort liegt, wo die Crew es im Dunkeln findet** | check | |
| `chk_sf_engine_fire` | Enter the date from the cylinder label; check the discharge port is not blocked | **Datum vom Flaschenetikett eintragen; prüfen, dass die Austrittsdüse nicht verdeckt ist** | check | |
| `chk_sf_epirb` | Self-test the beacon and check it sits in its bracket the right way up | **Selbsttest der Bake ausführen und prüfen, dass sie richtig herum in der Halterung sitzt** | check | |
| `chk_sf_epirb_battery` | Check the battery expiry; replacement is a service-centre job, so book it early | **Ablaufdatum der Batterie prüfen; der Tausch ist Sache des Servicecenters, also früh einen Termin machen** | check | |
| `chk_sf_epirb_reg` | Confirm the registration and emergency contacts; enter the expiry from the registration certificate | **Registrierung und Notfallkontakte bestätigen; Ablaufdatum aus der Registrierungsurkunde eintragen** | check | |
| `chk_sf_extinguishers` | Check every gauge is in the green; enter the earliest date from the neck stamp or the label | **Jedes Manometer im grünen Bereich prüfen; frühestes Datum vom Halsstempel oder Etikett eintragen** | check | |
| `chk_sf_fire_blanket` | Check the blanket is within reach of the stove — and not mounted behind it | **Prüfen, dass die Löschdecke vom Herd aus erreichbar ist — und nicht dahinter hängt** | check | |
| `chk_sf_first_aid` | Restock the kit and enter the date of whichever medicine expires first | **Bordapotheke auffüllen und das Datum des zuerst ablaufenden Medikaments eintragen** | check | |
| `chk_sf_flares` | Check every flare is in date, and know where the expired ones can be handed in | **Prüfen, ob alle Signalmittel gültig sind, und wissen, wo abgelaufene abgegeben werden können** | check | |
| `chk_sf_grab_bag` | Open the grab bag, check the dates inside, and put it back where it can be grabbed | **Notfalltasche öffnen, Daten darin prüfen und sie griffbereit zurücklegen** | check | |
| `chk_sf_harnesses` | Pull hard on every stitch line, replace anything sun-bleached, and enter the label date | **An jeder Naht kräftig ziehen, alles Ausgebleichte ersetzen und das Datum vom Etikett eintragen** | check | |
| `chk_sf_jackstays` | Check the jackstays end to end and the points they attach to | **Strecktaue auf ganzer Länge und ihre Befestigungspunkte prüfen** | check | |
| `chk_sf_lifejackets` | Inspect every jacket: bladder, webbing, crotch strap, whistle, light | **Jede Weste prüfen: Auftriebskörper, Gurtband, Schrittgurt, Pfeife, Licht** | check | |
| `chk_sf_liferaft` | Check the service date, and that the painter is tied to a strong point rather than the rail | **Wartungsdatum prüfen und dass die Reißleine an einem festen Punkt belegt ist, nicht an der Reling** | check | |
| `chk_sf_liferaft_hru` | Read the date moulded into the side of the hydrostatic release and enter it | **Das im Gehäuse des hydrostatischen Auslösers eingeprägte Datum ablesen und eintragen** | check | |
| `chk_sf_lj_cylinders` | Weigh every CO2 cylinder on kitchen scales against its stamped gross weight; enter the earliest auto-cartridge date | **Jede CO2-Patrone auf der Küchenwaage wiegen und mit dem eingestanzten Bruttogewicht vergleichen; das früheste Datum der Auslöseautomatik eintragen** | check | |
| `chk_sf_mob` | Check the danbuoy light, the horseshoe attachment, and that the sling actually deploys | **Licht der Danboje, Befestigung des Rettungsrings prüfen und ob die Rettungsschlinge wirklich ausläuft** | check | |
| `chk_sf_pelican` | Work the lifeline gate hooks; salt seizes them shut | **Pelikanhaken der Relingspforte durchbewegen; Salz setzt sie fest** | check | |
| `chk_sf_plan` | Brief the crew: man overboard, fire, flooding, abandon ship — and say where everything is | **Crew einweisen: Mensch über Bord, Feuer, Wassereinbruch, Boot verlassen — und sagen, wo alles liegt** | check | |
| `chk_sf_radar_reflector` | Check the reflector is mounted the way its maker specifies, not just tied on | **Prüfen, dass der Reflektor so montiert ist, wie der Hersteller es vorschreibt, und nicht nur angebunden** | check | |
| `chk_sf_rigging_shears` | Check the rig cutters work, and that everyone aboard knows where they are | **Prüfen, dass der Riggschneider schneidet und alle an Bord wissen, wo er liegt** | check | |
| `chk_sf_sart` | Self-test the SART and enter the battery date from the label on its body | **Selbsttest des SART ausführen und das Batteriedatum vom Gehäuseetikett eintragen** | check | |
| `chk_sf_sea_anchor` | Open the sea anchor out; check the rode, the swivel and the retrieval line | **Seeanker auslegen; Leine, Wirbel und Bergeleine prüfen** | check | |
| `chk_sf_sound` | Test the horn; check the bell and the whistle are aboard, not ashore | **Horn testen; prüfen, dass Glocke und Pfeife an Bord sind und nicht zu Hause** | check | |
| `chk_sf_tiller` | Dig the emergency tiller out, fit it to the rudder stock and steer with it | **Notpinne hervorholen, auf den Ruderschaft setzen und damit steuern** | check | |
| `chk_winterize` | Winterization / lay-up | **Einwintern** | check | |
| `chk_wz_anodes_note` | Photograph the anodes and note how much is left | **Anoden fotografieren und Restmaterial notieren** | check | |
| `chk_wz_batteries` | Charge fully, then take them ashore or leave a maintainer on | **Voll laden, dann an Land bringen oder am Erhaltungsladegerät lassen** | check | |
| `chk_wz_bilge` | Pump the bilge dry and clean it; water left in it freezes | **Bilge trocken lenzen und reinigen; stehendes Wasser friert** | check | |
| `chk_wz_chain` | Wash the chain, and end-for-end it if the working end is worn | **Ankerkette waschen und umdrehen, wenn das Arbeitsende verschlissen ist** | check | |
| `chk_wz_coolant_check` | Check antifreeze strength in the closed circuit | **Frostschutzgehalt im geschlossenen Kreislauf prüfen** | check | |
| `chk_wz_engine_oil` | Change engine oil and filter before lay-up, not in spring | **Motoröl und Filter vor dem Einwintern wechseln, nicht erst im Frühjahr** | check | |
| `chk_wz_expiry` | Note every expiry that falls before next season: insurance, liferaft, flares | **Alle Fristen notieren, die vor der nächsten Saison ablaufen: Versicherung, Rettungsinsel, Signalmittel** | check | |
| `chk_wz_fridge` | Empty the fridge and prop the door open | **Kühlschrank leeren und die Tür offen lassen** | check | |
| `chk_wz_fuel_full` | Fill the tank to about 95% and add stabiliser or biocide | **Tank zu etwa 95 % füllen und Stabilisator oder Biozid zugeben** | check | |
| `chk_wz_fuel_prefilter` | Change the primary filter and drain water from the bowl | **Vorfilter wechseln und Wasser aus dem Schauglas ablassen** | check | |
| `chk_wz_furler` | Rinse the furler with fresh water and work it through its full travel | **Rollanlage mit Süßwasser spülen und über den ganzen Weg durchdrehen** | check | |
| `chk_wz_gas_off` | Close the bottle, disconnect it, and check the locker drain is clear | **Flasche schließen, abklemmen und prüfen, ob der Gaskasten-Ablauf frei ist** | check | |
| `chk_wz_gearbox_oil` | Change gearbox oil | **Getriebeöl wechseln** | check | |
| `chk_wz_halyards` | Wash the halyards, replace them with messengers, and store them dry | **Fallen waschen, durch Vorläufer ersetzen und trocken lagern** | check | |
| `chk_wz_heads` | Shut the heads intake seacock, drop the intake hose into the pink propylene glycol and pump it through the bowl into the tank | **Seeventil des Toilettenzulaufs schließen, Zulaufschlauch in rosa Propylenglykol hängen und durch das Becken in den Tank pumpen** | check | |
| `chk_wz_holding` | Empty and rinse the holding tank | **Fäkalientank leeren und spülen** | check | |
| `chk_wz_impeller_out` | Remove the impeller, rinse it, grease it with silicone and bag it beside the pump; tag the helm so nobody starts the engine | **Impeller ausbauen, spülen, mit Silikonfett einfetten und im Beutel an die Pumpe hängen; Schild an den Steuerstand, damit niemand den Motor startet** | check | |
| `chk_wz_lines` | Wash and dry the mooring lines and fenders; store them out of sunlight | **Festmacher und Fender waschen, trocknen und lichtgeschützt lagern** | check | |
| `chk_wz_ob_flush` | Flush the outboard, then fog the cylinders and run the fuel system dry | **Außenborder spülen, Zylinder einnebeln und die Kraftstoffanlage leerfahren** | check | |
| `chk_wz_ob_gearcase` | Change gearcase oil; milky oil means a seal has gone | **Getriebeöl wechseln; milchiges Öl heißt, ein Dichtring ist undicht** | check | |
| `chk_wz_raw_antifreeze` | Draw antifreeze through until it runs from the exhaust | **Frostschutz durch den Seewasserkreislauf ziehen, bis er am Auspuff austritt** | check | |
| `chk_wz_raw_flush` | Flush the raw-water circuit with fresh water | **Seewasserkreislauf mit Süßwasser spülen** | check | |
| `chk_wz_rig_slack` | Slacken the rig a little if the mast stays stepped | **Rigg etwas entspannen, wenn der Mast stehen bleibt** | check | |
| `chk_wz_rudder` | Check rudder bearing play and write the figure down for comparison | **Spiel im Ruderlager prüfen und den Wert zum Vergleich notieren** | check | |
| `chk_wz_sail_service` | Send anything that needs stitching now, not in April | **Was genäht werden muss, jetzt zum Segelmacher geben, nicht im April** | check | |
| `chk_wz_saildrive_oil` | Change saildrive oil; check it for water | **Saildrive-Öl wechseln und auf Wasser prüfen** | check | |
| `chk_wz_sails_off` | Take the sails off and dry them fully before folding | **Segel abschlagen und vor dem Falten vollständig trocknen** | check | |
| `chk_wz_seacocks_open` | Set every seacock half open, not fully open: a ball valve at either end of its travel still holds water in the seat, and that is what splits the casting | **Alle Seeventile halb öffnen, nicht ganz: ein Kugelhahn hält in beiden Endstellungen Wasser im Sitz, und genau das sprengt das Gehäuse** | check | |
| `chk_wz_seacocks_shut` | Close every seacock except the ones the bilge pump needs | **Alle Seeventile schließen außer denen, die die Bilgepumpe braucht** | check | |
| `chk_wz_shore` | Check the shore cable and inlet for heat damage before leaving it live all winter | **Landstromkabel und Anschluss auf Hitzeschäden prüfen, bevor sie den ganzen Winter unter Strom stehen** | check | |
| `chk_wz_soft` | Take ashore every cushion, sail bag and anything else that holds damp | **Polster, Segelsäcke und alles, was Feuchtigkeit hält, an Land bringen** | check | |
| `chk_wz_stuffing_box` | Grease the stuffing box or check the dripless bellows date | **Stopfbuchse fetten oder das Datum des Wellendichtungsbalgs prüfen** | check | |
| `chk_wz_switches` | Switch everything off except the bilge pump if she stays afloat | **Alles ausschalten außer der Bilgepumpe, wenn das Boot im Wasser bleibt** | check | |
| `chk_wz_terminals` | Clean and grease the terminals | **Polklemmen reinigen und fetten** | check | |
| `chk_wz_transducer` | Pull the paddlewheel log and fit the blank | **Logge-Paddelrad ziehen und Blindstopfen einsetzen** | check | |
| `chk_wz_vent` | Set up ventilation or a dehumidifier; a sealed boat grows mould | **Belüftung oder Luftentfeuchter einrichten; ein dicht verschlossenes Boot schimmelt** | check | |
| `chk_wz_wash` | Pressure-wash the hull the hour she comes out, before growth dries on | **Unterwasserschiff gleich nach dem Auskranen abspritzen, bevor der Bewuchs antrocknet** | check | |
| `chk_wz_water_drain` | Drain tanks and lines, or push potable antifreeze through | **Tanks und Leitungen entleeren oder mit trinkwassertauglichem Frostschutz füllen** | check | |
| `chk_wz_water_heater` | Drain and bypass the calorifier | **Boiler entleeren und mit Bypass überbrücken** | check | |
| `chk_wz_watermaker` | Pickle the membrane | **Membran konservieren** | check | |
| `cmp_ac_outlets` | AC outlets & earth bonding | **Steckdosen & Schutzleiter (230/120 V)** | check | |
| `cmp_air_filter` | Air filter | **Luftfilter** | check | |
| `cmp_aircon` | Air conditioning | **Klimaanlage** | check | |
| `cmp_aircon_pump` | Aircon raw water pump & strainer | **Seewasserpumpe & Filter der Klimaanlage** | check | |
| `cmp_ais` | AIS transponder | **AIS-Transponder** | check | |
| `cmp_alternator` | Alternator | **Lichtmaschine** | check | |
| `cmp_alternator_belt` | Engine drive belt (alternator, pump) | **Keilriemen (Lichtmaschine, Pumpe)** | check | |
| `cmp_anchor_bridle_hook` | Anchor bridle hook | **Kettenhaken der Hahnepot** | check | |
| `cmp_anchor_chain` | Anchor chain | **Ankerkette** | check | |
| `cmp_anchor_light` | Anchor & steaming lights | **Ankerlicht & Dampferlicht** | check | |
| `cmp_anchor_locker` | Anchor locker & drain | **Ankerkasten & Ablauf** | check | |
| `cmp_anchor_swivel` | Swivel & shackles | **Ankerwirbel & Schäkel** | check | |
| `cmp_anodes_hull` | Hull anodes | **Rumpfanoden** | check | |
| `cmp_anti_siphon` | Anti-siphon valve | **Belüftungsventil (Anti-Siphon)** | check | |
| `cmp_antifouling` | Antifouling | **Antifouling** | check | |
| `cmp_autopilot` | Autopilot | **Autopilot** | check | |
| `cmp_autopilot_drive` | Autopilot drive unit | **Antriebseinheit des Autopiloten** | check | |
| `cmp_backstay` | Backstay | **Achterstag** | check | |
| `cmp_bathing_platform` | Bathing platform & ladder | **Badeplattform & Badeleiter** | check | |
| `cmp_battery_bms` | BMS / lithium balancing | **BMS / Lithium-Balancing** | check | |
| `cmp_battery_monitor` | Battery monitor / shunt | **Batteriemonitor / Shunt** | check | |
| `cmp_battery_terminals` | Terminals & battery boxes | **Polklemmen & Batteriekästen** | check | |
| `cmp_berths` | Berths, mattresses, underbunk ventilation | **Kojen, Matratzen, Unterlüftung** | check | |
| `cmp_bilge_alarm` | High water bilge alarm | **Bilgealarm (Hochwasser)** | check | |
| `cmp_bilge_cleanliness` | Bilge cleaning | **Bilge reinigen** | check | |
| `cmp_bilge_emergency_pump` | High-capacity emergency pump | **Hochleistungs-Notlenzpumpe** | check | |
| `cmp_bilge_float_switch` | Bilge float switch | **Schwimmerschalter der Bilgepumpe** | check | |
| `cmp_bilge_hoses` | Bilge hoses & check valves | **Lenzschläuche & Rückschlagventile** | check | |
| `cmp_bilge_pump_auto` | Automatic bilge pump | **Automatische Bilgepumpe** | check | |
| `cmp_bilge_pump_manual` | Manual bilge pump | **Handlenzpumpe** | check | |
| `cmp_bitter_end` | Bitter end lashing to boat | **Kettenende im Ankerkasten befestigen** | check | |
| `cmp_blocks` | Blocks | **Blöcke** | check | |
| `cmp_blower` | Engine bay blower (bilge ventilation fan) | **Motorraumlüfter** | check | |
| `cmp_boat_hook` | Boat hook | **Bootshaken** | check | |
| `cmp_bonding_system` | Bonding system & grounding bus | **Potentialausgleich & Erdungsschiene** | check | |
| `cmp_boom` | Boom & gooseneck | **Großbaum & Lümmelbeschlag** | check | |
| `cmp_bow_thruster_grates` | Bow thruster tunnel grates | **Bugstrahlruder-Tunnelgitter** | check | |
| `cmp_bowsprit` | Bowsprit | **Bugspriet** | check | |
| `cmp_ce_certificate` | CE certificate & manuals | **CE-Konformität & Handbücher** | check | |
| `cmp_chain_end_for_end` | Anchor chain end-for-ending | **Ankerkette umdrehen (Enden tauschen)** | check | |
| `cmp_chain_markings` | Chain length markings | **Kettenmarkierungen** | check | |
| `cmp_chainplates` | Chainplates | **Püttinge** | check | |
| `cmp_charger` | Shore battery charger | **Landstrom-Ladegerät** | check | |
| `cmp_chart_updates` | Chart updates | **Kartenupdates** | check | |
| `cmp_chartplotter` | Chartplotter | **Kartenplotter** | check | |
| `cmp_clutches` | Rope clutches | **Fallenstopper** | check | |
| `cmp_co_smoke_detector` | CO & smoke detectors | **CO- & Rauchmelder** | check | |
| `cmp_code_zero_furler` | Code Zero furler (continuous line) | **Code-Zero-Rollanlage (Endlosleine)** | check | |
| `cmp_compass` | Magnetic compass & deviation card | **Magnetkompass & Deviationstabelle** | check | |
| `cmp_compression_test` | Compression test | **Kompressionsprüfung** | check | |
| `cmp_condensation` | Insulation & condensation control | **Isolierung & Kondenswasser** | check | |
| `cmp_control_lines` | Reefing lines, topping lift, cunningham | **Reffleinen, Dirk, Cunningham** | check | |
| `cmp_coolant` | Coolant (closed circuit) | **Kühlflüssigkeit (Zweikreiskühlung)** | check | |
| `cmp_damage_control` | Damage control kit | **Leckabwehr-Set** | check | |
| `cmp_davits` | Davits & lifting gear | **Davits & Hebezeug** | check | |
| `cmp_dc_dc_charger` | DC-DC charger | **Ladebooster (DC-DC)** | check | |
| `cmp_dc_panel` | DC distribution panel & fuses | **Verteilertafel & Sicherungen (12/24 V)** | check | |
| `cmp_dc_wiring` | DC wiring & terminations | **Verkabelung & Anschlüsse (12/24 V)** | check | |
| `cmp_deck` | Deck & superstructure | **Deck & Aufbau** | check | |
| `cmp_deck_light` | Deck light & searchlight | **Decksstrahler & Suchscheinwerfer** | check | |
| `cmp_deck_rails_lifelines` | Lifelines, stanchions, pulpit & pushpit | **Seereling, Relingsstützen, Bug- & Heckkorb** | check | |
| `cmp_diesel_heater` | Diesel heater | **Dieselheizung** | check | |
| `cmp_doors_catches` | Doors, catches, hinges, storm bars | **Türen, Schnäpper, Scharniere, Sturmriegel** | check | |
| `cmp_drain_plug` | Transom drain plug (garboard plug) | **Lenzstopfen im Spiegel** | check | |
| `cmp_drains_scuppers` | Drains & scuppers | **Abläufe & Speigatten** | check | |
| `cmp_drogue` | Drogue (streamed astern) | **Treibanker (achteraus)** | check | |
| `cmp_dry_exhaust_silencer` | Dry exhaust silencer & stack | **Trockenauspuff: Schalldämpfer & Rohr** | check | |
| `cmp_egt_monitoring` | Exhaust gas temperature monitoring | **Abgastemperatur-Überwachung** | check | |
| `cmp_emergency_plan` | Safety briefing & emergency procedures | **Sicherheitseinweisung & Notfallrollen** | check | |
| `cmp_emergency_tiller` | Emergency tiller | **Notpinne** | check | |
| `cmp_emergency_tools` | Emergency cutting tools (bolt croppers, hacksaw) | **Notwerkzeug (Bolzenschneider, Bügelsäge)** | check | |
| `cmp_emergency_vhf_antenna` | Emergency VHF antenna | **UKW-Notantenne** | check | |
| `cmp_engine_fire_system` | Engine bay automatic fire system | **Automatische Motorraum-Löschanlage** | check | |
| `cmp_engine_mounts` | Engine mounts & alignment | **Motorlager & Ausrichtung** | check | |
| `cmp_engine_oil` | Engine oil | **Motoröl** | check | |
| `cmp_engine_oil_filter` | Engine oil filter | **Ölfilter** | check | |
| `cmp_engine_sensors` | Sensors & alarms (temp, oil pressure) | **Geber & Alarme (Temperatur, Öldruck)** | check | |
| `cmp_engine_wiring` | Engine harness & connections | **Motorkabelbaum & Steckverbindungen** | check | |
| `cmp_entertainment` | Audio, TV, antenna | **Audio, TV, Antenne** | check | |
| `cmp_epirb` | EPIRB / PLB | **Seenotfunkbake (EPIRB / PLB)** | check | |
| `cmp_epirb_battery` | EPIRB battery | **Batterie der Seenotfunkbake** | check | |
| `cmp_epirb_registration` | EPIRB registration & contacts | **Registrierung der Seenotfunkbake & Kontakte** | check | |
| `cmp_exhaust_bellows` | Exhaust expansion bellows | **Auspuff-Kompensator** | check | |
| `cmp_exhaust_blanket` | Exhaust thermal blanket | **Hitzeschutzmatte des Auspuffs** | check | |
| `cmp_exhaust_hose` | Exhaust hose & waterlock | **Abgasschlauch & Wassersammler** | check | |
| `cmp_fenders` | Fenders & fender boards | **Fender & Fenderbretter** | check | |
| `cmp_fire_blanket` | Fire blanket | **Löschdecke** | check | |
| `cmp_fire_extinguishers` | Fire extinguishers | **Feuerlöscher** | check | |
| `cmp_first_aid` | First aid kit | **Bordapotheke** | check | |
| `cmp_flares` | Flares & pyrotechnics | **Seenotsignalmittel (Pyrotechnik)** | check | |
| `cmp_flex_coupling` | Flexible coupling | **Elastische Kupplung** | check | |
| `cmp_folding_prop` | Folding / feathering propeller | **Falt- / Drehflügelpropeller** | check | |
| `cmp_forestay` | Forestay | **Vorstag** | check | |
| `cmp_fridge` | Refrigeration & freezer | **Kühlschrank & Gefrierfach** | check | |
| `cmp_fridge_seals` | Fridge seals, insulation, drain | **Kühlschrank: Dichtungen, Isolierung, Ablauf** | check | |
| `cmp_fuel_filler` | Deck filler & seal | **Tankstutzen & Dichtung** | check | |
| `cmp_fuel_filter_fine` | Secondary fuel filter | **Kraftstofffeinfilter** | check | |
| `cmp_fuel_gauge` | Fuel level sender & gauge | **Tankgeber & Anzeige** | check | |
| `cmp_fuel_hoses` | Fuel hoses | **Kraftstoffschläuche** | check | |
| `cmp_fuel_polishing` | Fuel polishing / biocide | **Kraftstoffreinigung / Biozid** | check | |
| `cmp_fuel_prefilter` | Fuel pre-filter / water separator | **Kraftstoffvorfilter / Wasserabscheider** | check | |
| `cmp_fuel_return_cooler` | Fuel return cooler | **Kraftstoffrücklaufkühler** | check | |
| `cmp_fuel_tank` | Fuel tank | **Kraftstofftank** | check | |
| `cmp_fuel_tank_drain` | Fuel tank drain valve | **Ablasshahn des Kraftstofftanks** | check | |
| `cmp_fuel_tank_inspection` | Fuel tank inspection port & sump | **Tank-Inspektionsdeckel & Sumpf** | check | |
| `cmp_fuel_valves` | Fuel shut-off valves & manifold | **Kraftstoffabsperrhähne & Verteiler** | check | |
| `cmp_fuel_vent` | Tank vent & flame arrestor | **Tankentlüftung & Flammensperre** | check | |
| `cmp_galley_foot_pump` | Manual galley pump | **Fußpumpe in der Pantry** | check | |
| `cmp_galvanic_isolator` | Galvanic isolator | **Galvanischer Trenner** | check | |
| `cmp_gas_bottles` | Gas bottles & locker | **Gasflaschen & Gaskasten** | check | |
| `cmp_gas_detector` | Gas detector | **Gaswarngerät** | check | |
| `cmp_gas_hoses` | Gas hoses | **Gasschläuche** | check | |
| `cmp_gas_leak_test` | LPG leak test (manometer) | **Gasdichtheitsprüfung (Manometer)** | check | |
| `cmp_gas_locker_drain` | LPG locker drain | **Gaskasten-Ablauf** | check | |
| `cmp_gas_regulator` | Gas regulator | **Gasdruckregler** | check | |
| `cmp_gas_solenoid` | Solenoid shut-off valve | **Magnetventil (Gasabsperrung)** | check | |
| `cmp_gearbox` | Gearbox / transmission | **Wendegetriebe** | check | |
| `cmp_gearbox_oil` | Gearbox oil | **Getriebeöl** | check | |
| `cmp_gen_belt` | Generator drive belt | **Antriebsriemen des Generators** | check | |
| `cmp_gen_control` | Control panel & auto-start | **Bedienfeld & Autostart** | check | |
| `cmp_gen_coolant` | Generator coolant | **Kühlflüssigkeit des Generators** | check | |
| `cmp_gen_enclosure` | Genset sound shield | **Schallschutzkapsel des Generators** | check | |
| `cmp_gen_exhaust` | Generator exhaust & elbow | **Auspuff & Krümmer des Generators** | check | |
| `cmp_gen_fuel_filter` | Generator fuel filter | **Kraftstofffilter des Generators** | check | |
| `cmp_gen_impeller` | Generator raw water impeller | **Impeller des Generators** | check | |
| `cmp_gen_oil` | Generator oil & filter | **Generator: Öl & Filter** | check | |
| `cmp_gen_windings` | Generator end, brushes, bearings | **Generatorteil: Wicklung, Bürsten, Lager** | check | |
| `cmp_glow_plugs` | Glow plugs | **Glühkerzen** | check | |
| `cmp_grab_bag` | Grab bag (abandon ship) | **Notfalltasche (Grab Bag)** | check | |
| `cmp_halyards` | Halyards | **Fallen** | check | |
| `cmp_handheld_vhf` | Handheld VHF & battery | **UKW-Handfunkgerät & Akku** | check | |
| `cmp_harnesses_tethers` | Harnesses & tethers | **Sicherheitsgurte & Lifelines** | check | |
| `cmp_hatches_portlights` | Hatches & portlights | **Luken & Fenster** | check | |
| `cmp_heads_hoses` | Sanitation hoses | **Sanitärschläuche** | check | |
| `cmp_heads_pump` | Heads pump & joker valve | **Toilettenpumpe & Joker-Ventil** | check | |
| `cmp_headsail` | Headsail / genoa | **Vorsegel / Genua** | check | |
| `cmp_headsail_furler` | Headsail furler | **Vorsegel-Rollreffanlage** | check | |
| `cmp_headsail_uv` | UV sacrificial strip | **UV-Schutzstreifen** | check | |
| `cmp_heat_exchanger` | Heat exchanger | **Wärmetauscher** | check | |
| `cmp_heat_exchanger_anode` | Heat exchanger anode / pencil zinc | **Anode des Wärmetauschers** | check | |
| `cmp_heater_exhaust` | Heater exhaust & ducting | **Heizungsauspuff & Luftkanäle** | check | |
| `cmp_holding_tank` | Holding tank | **Fäkalientank** | check | |
| `cmp_hose_clamps` | Hose clamps | **Schlauchschellen** | check | |
| `cmp_house_batteries` | House batteries | **Verbraucherbatterien** | check | |
| `cmp_hull_cleaning` | Hull cleaning / scrub | **Unterwasserschiff reinigen** | check | |
| `cmp_hull_gelcoat` | Hull & gelcoat | **Rumpf & Gelcoat** | check | |
| `cmp_hydrogenerator` | Hydrogenerator | **Schleppgenerator (Hydrogenerator)** | check | |
| `cmp_injection_pump` | Injection pump | **Einspritzpumpe** | check | |
| `cmp_injectors` | Injectors | **Einspritzdüsen** | check | |
| `cmp_inner_forestay` | Inner forestay / baby stay | **Kutterstag / Babystag** | check | |
| `cmp_insurance` | Insurance | **Versicherung** | check | |
| `cmp_interior_joinery` | Joinery, headlining, sole | **Innenausbau, Himmel, Bodenbretter** | check | |
| `cmp_interior_lighting` | Interior lighting | **Innenbeleuchtung** | check | |
| `cmp_inverter` | Inverter | **Wechselrichter** | check | |
| `cmp_isolation_transformer` | Isolation transformer | **Trenntransformator** | check | |
| `cmp_jackstays` | Jackstays & clip points | **Strecktaue & Einpickpunkte** | check | |
| `cmp_keel` | Keel | **Kiel** | check | |
| `cmp_keel_bolts` | Keel bolts | **Kielbolzen** | check | |
| `cmp_keel_bulb` | Keel bulb | **Kielbombe** | check | |
| `cmp_keel_cooling` | Keel cooling (dry exhaust) | **Kielkühlung (Trockenauspuff)** | check | |
| `cmp_keel_joint` | Keel-to-hull joint (keel smile) | **Kiel-Rumpf-Verbindung** | check | |
| `cmp_keel_lift_cable` | Keel pennant / lift wire & winch | **Kielhubseil & Winde** | check | |
| `cmp_kill_switch` | Kill switch / lanyard | **Notstoppschalter / Reißleine** | check | |
| `cmp_lifejacket_cylinders` | CO2 cylinders & auto cartridges | **CO2-Patronen & Auslöseautomatik** | check | |
| `cmp_lifejackets` | Lifejackets / PFDs | **Rettungswesten** | check | |
| `cmp_liferaft` | Liferaft | **Rettungsinsel** | check | |
| `cmp_liferaft_hru` | Liferaft hydrostatic release | **Hydrostatischer Auslöser der Rettungsinsel** | check | |
| `cmp_lift_pump` | Fuel lift pump | **Kraftstoffförderpumpe** | check | |
| `cmp_lifting_keel` | Lifting keel / centerboard | **Hubkiel / Schwert** | check | |
| `cmp_macerator` | Macerator & deck pump-out | **Fäkalienpumpe & Absaugstutzen** | check | |
| `cmp_main_battens` | Battens & batten cars | **Segellatten & Lattenrutscher** | check | |
| `cmp_main_switches` | Main switches & busbars | **Hauptschalter & Stromschienen** | check | |
| `cmp_mainsail` | Mainsail | **Großsegel** | check | |
| `cmp_mainsheet_traveler` | Mainsheet traveler & car | **Großschot-Traveller & Wagen** | check | |
| `cmp_marina_contract` | Marina / winter storage contract | **Liegeplatz- / Winterlagervertrag** | check | |
| `cmp_mast` | Mast | **Mast** | check | |
| `cmp_mast_climb_inspection` | Rig inspection aloft | **Riggkontrolle im Mast** | check | |
| `cmp_mast_step` | Mast step & partners | **Mastfuß & Mastdurchführung** | check | |
| `cmp_mast_wiring` | Mast wiring & conduit | **Mastverkabelung & Kabelrohr** | check | |
| `cmp_masthead` | Masthead, sheaves, fittings | **Masttopp, Scheiben, Beschläge** | check | |
| `cmp_mixing_elbow` | Exhaust mixing elbow | **Einspritzkrümmer** | check | |
| `cmp_mob_equipment` | MOB gear (danbuoy, horseshoe, sling) | **MOB-Ausrüstung (Danboje, Rettungsring, Rettungsschlinge)** | check | |
| `cmp_mooring_cleats` | Mooring cleats, fairleads & bollards | **Klampen, Lippklampen & Poller** | check | |
| `cmp_mooring_lines` | Mooring lines & springs | **Festmacher & Springs** | check | |
| `cmp_mooring_snubbers` | Mooring snubbers & compensators | **Ruckdämpfer für Festmacher** | check | |
| `cmp_national_inspection` | National technical inspection | **Technische Prüfung (national)** | check | |
| `cmp_nav_instruments` | Binoculars, barometer, clock | **Fernglas, Barometer, Uhr** | check | |
| `cmp_nav_lights` | Navigation lights | **Navigationslichter** | check | |
| `cmp_nmea_network` | Instrument network & connectors | **Instrumentennetzwerk & Stecker** | check | |
| `cmp_nonskid` | Deck non-skid | **Antirutschbelag** | check | |
| `cmp_ob_anodes` | Outboard anodes | **Anoden des Außenborders** | check | |
| `cmp_ob_flush` | Outboard freshwater flush | **Außenborder mit Süßwasser spülen** | check | |
| `cmp_ob_gearcase_oil` | Outboard gearcase oil | **Getriebeöl des Außenborders** | check | |
| `cmp_ob_spark_plugs` | Outboard spark plugs | **Zündkerzen des Außenborders** | check | |
| `cmp_ob_tilt_trim` | Outboard tilt & trim | **Kipp- & Trimmanlage des Außenborders** | check | |
| `cmp_oil_analysis` | Oil analysis | **Ölanalyse** | check | |
| `cmp_operator_licence` | Operator certificate (ICC, VHF) | **Befähigungsnachweis (ICC, Funkzeugnis)** | check | |
| `cmp_osmosis_check` | Osmosis / moisture check | **Osmose- / Feuchtigkeitsprüfung** | check | |
| `cmp_p_bracket` | P-bracket / shaft strut | **Wellenbock** | check | |
| `cmp_paper_charts` | Paper charts & pilot books | **Papierseekarten & Hafenhandbücher** | check | |
| `cmp_passerelle` | Passerelle / gangway | **Gangway / Passerelle** | check | |
| `cmp_pelican_hooks` | Lifeline pelican hooks | **Pelikanhaken der Seereling** | check | |
| `cmp_portable_tanks` | Portable fuel tanks | **Reservekanister / mobile Tanks** | check | |
| `cmp_primary_anchor` | Primary anchor | **Hauptanker** | check | |
| `cmp_prop_shaft` | Propeller shaft | **Propellerwelle** | check | |
| `cmp_propeller` | Propeller | **Propeller** | check | |
| `cmp_radar` | Radar | **Radar** | check | |
| `cmp_radar_reflector` | Radar reflector | **Radarreflektor** | check | |
| `cmp_radio_licence` | Ship radio licence & MMSI | **Funkzulassung & MMSI** | check | |
| `cmp_raw_water_hoses` | Raw water hoses | **Seewasserschläuche** | check | |
| `cmp_raw_water_impeller` | Raw water pump impeller | **Impeller der Seewasserpumpe** | check | |
| `cmp_raw_water_pump_deck` | Deck wash / raw water pump | **Deckwaschpumpe (Seewasser)** | check | |
| `cmp_raw_water_strainer` | Raw water strainer | **Seewasserfilter** | check | |
| `cmp_rcd_elcb` | RCD / ELCB test | **FI-Schutzschalter prüfen** | check | |
| `cmp_reefing` | Reefing system | **Reffsystem** | check | |
| `cmp_registration` | Vessel registration / flag | **Registrierung / Flagge** | check | |
| `cmp_rig_replacement` | Full standing rigging replacement | **Stehendes Gut komplett erneuern** | check | |
| `cmp_rigging_shears` | Rigging emergency shears | **Notschneider für das Rigg** | check | |
| `cmp_rubbing_strake` | Rub rail / rubbing strake | **Scheuerleiste** | check | |
| `cmp_rudder` | Rudder | **Ruderblatt** | check | |
| `cmp_rudder_bearings` | Rudder bearings & stock | **Ruderlager & Ruderschaft** | check | |
| `cmp_running_backstays` | Running backstays / checkstays | **Backstagen / Checkstagen** | check | |
| `cmp_safety_padeyes` | Safety padeyes & backing plates | **Sicherheitsaugbolzen & Gegenplatten** | check | |
| `cmp_sail_covers` | Sail cover / lazy bag & jacks | **Segelkleid / Lazy Bag & Lazy Jacks** | check | |
| `cmp_sail_service` | Sail valet & loft inspection | **Segel-Service & Kontrolle beim Segelmacher** | check | |
| `cmp_sail_wash` | Sail wash & dry storage | **Segel waschen & trocken lagern** | check | |
| `cmp_saildrive` | Saildrive | **Saildrive** | check | |
| `cmp_saildrive_anode` | Saildrive anode | **Saildrive-Anode** | check | |
| `cmp_saildrive_diaphragm` | Saildrive rubber diaphragm | **Saildrive-Gummimanschette** | check | |
| `cmp_saildrive_oil` | Saildrive oil | **Saildrive-Öl** | check | |
| `cmp_sart` | SART / AIS-SART | **SART / AIS-SART** | check | |
| `cmp_satcom` | Satcom / Starlink | **Satellitenkommunikation / Starlink** | check | |
| `cmp_sea_anchor` | Sea anchor (parachute) | **Seeanker (Fallschirm)** | check | |
| `cmp_sea_chest` | Engine sea chest | **Seekasten** | check | |
| `cmp_seacocks` | Seacocks | **Seeventile** | check | |
| `cmp_secondary_anchor` | Secondary / kedge anchor | **Zweitanker / Heckanker** | check | |
| `cmp_shaft_anode` | Shaft anode | **Wellenanode** | check | |
| `cmp_shaft_bellows` | Shaft seal bellows replacement | **Balg der Wellendichtung tauschen** | check | |
| `cmp_sheets` | Sheets | **Schoten** | check | |
| `cmp_ships_log` | Ship's log book | **Logbuch** | check | |
| `cmp_shore_cable` | Shore power cable & plug | **Landstromkabel & Stecker** | check | |
| `cmp_shore_inlet` | Shore power inlet | **Landstromanschluss** | check | |
| `cmp_shower_sump` | Shower sump & pump | **Duschsumpf & Pumpe** | check | |
| `cmp_shrouds` | Shrouds (cap, lower, intermediate) | **Wanten (Ober-, Unter-, Zwischenwanten)** | check | |
| `cmp_sink_taps` | Sink, taps, filter | **Spüle, Armaturen, Filter** | check | |
| `cmp_skeg` | Rudder skeg | **Ruderskeg** | check | |
| `cmp_snubber` | Snubber / bridle | **Kettenkralle / Ankerhahnepot** | check | |
| `cmp_snuffer` | Snuffer / sock | **Bergeschlauch** | check | |
| `cmp_solar` | Solar panels & MPPT | **Solarmodule & MPPT** | check | |
| `cmp_solid_fuel_stove` | Solid fuel / kerosene (paraffin) heater & flue | **Festbrennstoff- / Petroleumofen & Abzug** | check | |
| `cmp_sound_signals` | Sound signals (horn, bell, whistle) | **Schallsignale (Horn, Glocke, Pfeife)** | check | |
| `cmp_spinnaker` | Spinnaker / gennaker | **Spinnaker / Gennaker** | check | |
| `cmp_spinnaker_pole` | Spinnaker / whisker pole | **Spinnakerbaum / Ausbaumer** | check | |
| `cmp_sprayhood_bimini` | Canvas (sprayhood, bimini, covers) | **Persenning (Sprayhood, Bimini, Abdeckungen)** | check | |
| `cmp_spreaders` | Spreaders | **Salinge** | check | |
| `cmp_ssb` | SSB / HF radio | **Grenz- / Kurzwellenfunk (SSB)** | check | |
| `cmp_start_battery` | Start battery | **Starterbatterie** | check | |
| `cmp_starter_motor` | Starter motor | **Anlasser** | check | |
| `cmp_steering_cables_sheaves` | Steering cables, chains & idler sheaves | **Steuerseile, Ketten & Umlenkrollen** | check | |
| `cmp_steering_quadrant` | Quadrant / steering linkage | **Ruderquadrant & Steuergestänge** | check | |
| `cmp_steering_system` | Steering system (cable / hydraulic) | **Steueranlage (Seilzug / hydraulisch)** | check | |
| `cmp_stern_tube` | Stern tube & cutless bearing | **Stevenrohr & Wellenlager (Cutless)** | check | |
| `cmp_sterndrive` | Sterndrive leg | **Z-Antrieb** | check | |
| `cmp_sterndrive_bellows` | Sterndrive bellows | **Faltenbälge des Z-Antriebs** | check | |
| `cmp_sterndrive_oil` | Sterndrive gear oil | **Getriebeöl des Z-Antriebs** | check | |
| `cmp_storm_sails` | Storm jib & trysail | **Sturmfock & Trysegel** | check | |
| `cmp_stove` | Stove & oven | **Herd & Backofen** | check | |
| `cmp_stove_gimbal` | Stove gimbal & fiddles | **Kardanische Aufhängung & Schlingerleisten** | check | |
| `cmp_stuffing_box` | Stuffing box / dripless seal | **Stopfbuchse / Wellendichtung** | check | |
| `cmp_survey` | Last survey | **Letztes Gutachten** | check | |
| `cmp_teak_deck` | Teak deck & caulking | **Teakdeck & Fugen** | check | |
| `cmp_tender_hull` | Tender hull & tubes | **Beiboot: Rumpf & Schläuche** | check | |
| `cmp_tender_outboard` | Tender outboard | **Außenborder des Beiboots** | check | |
| `cmp_tender_registration` | Tender registration | **Zulassung des Beiboots** | check | |
| `cmp_tender_repair_kit` | Repair kit, glue, pump, oars | **Reparaturset, Kleber, Pumpe, Riemen** | check | |
| `cmp_tender_security` | Tender stowage, cover, security | **Beiboot: Stauung, Persenning, Diebstahlschutz** | check | |
| `cmp_tender_valves` | Tender valves & pressure | **Beiboot: Ventile & Druck** | check | |
| `cmp_thermostat` | Thermostat | **Thermostat** | check | |
| `cmp_throttle_cables` | Throttle & gear cables | **Gas- & Schaltzüge** | check | |
| `cmp_through_hulls` | Through-hulls | **Borddurchlässe** | check | |
| `cmp_thruster` | Bow / stern thruster | **Bug- / Heckstrahlruder** | check | |
| `cmp_thruster_anode` | Thruster anode & tunnel | **Anode & Tunnel des Strahlruders** | check | |
| `cmp_timing_belt` | Timing belt / cam chain | **Zahnriemen / Steuerkette** | check | |
| `cmp_tracks_cars` | Genoa tracks & cars | **Genuaschienen & Holepunktschlitten** | check | |
| `cmp_trailer_bearings` | Wheel bearings | **Radlager** | check | |
| `cmp_trailer_brakes` | Brakes & fluid | **Bremsen & Bremsflüssigkeit** | check | |
| `cmp_trailer_coupling` | Coupling, safety cables, jockey wheel | **Kupplung, Abreißseil, Stützrad** | check | |
| `cmp_trailer_documents` | Trailer registration & inspection | **Zulassung & HU des Anhängers** | check | |
| `cmp_trailer_frame` | Frame corrosion & welds | **Rahmen: Korrosion & Schweißnähte** | check | |
| `cmp_trailer_lights` | Lights & wiring plug | **Beleuchtung & Stecker** | check | |
| `cmp_trailer_rollers` | Rollers, keel support, bunks | **Rollen, Kielauflage, Auflagebretter** | check | |
| `cmp_trailer_tires` | Tires & spare | **Reifen & Reserverad** | check | |
| `cmp_trailer_winch` | Winch & strap | **Winde & Gurt** | check | |
| `cmp_transducers` | Depth & speed transducers | **Geber für Echolot & Logge** | check | |
| `cmp_transom` | Transom | **Spiegel** | check | |
| `cmp_trim_tabs` | Trim tabs & hydraulics | **Trimmklappen & Hydraulik** | check | |
| `cmp_tube_valves` | Tube valves & working pressure | **Ventile & Betriebsdruck der Schläuche** | check | |
| `cmp_tubes` | Inflatable tubes & seams | **Schlauchkörper & Nähte** | check | |
| `cmp_turbo` | Turbocharger / intercooler | **Turbolader / Ladeluftkühler** | check | |
| `cmp_turnbuckles` | Turnbuckles & split pins | **Wantenspanner & Splinte** | check | |
| `cmp_valve_clearance` | Valve clearance adjustment | **Ventilspiel einstellen** | check | |
| `cmp_vang` | Kicker / vang | **Baumniederholer (Kicker)** | check | |
| `cmp_vat_customs` | VAT status & customs papers | **MwSt.-Status & Zollpapiere** | check | |
| `cmp_ventilation` | Ventilation, dorades, fans | **Lüftung, Dorade-Lüfter, Ventilatoren** | check | |
| `cmp_vhf` | VHF & DSC | **UKW-Funk & DSC** | check | |
| `cmp_vhf_antenna` | VHF antenna & coax | **UKW-Antenne & Koaxkabel** | check | |
| `cmp_warranties` | Warranties | **Garantien** | check | |
| `cmp_washboards` | Companionway hatch, washboards & lock | **Niedergang, Steckschotten & Schloss** | check | |
| `cmp_washing_machine` | Laundry & dishwasher | **Waschmaschine & Geschirrspüler** | check | |
| `cmp_water_filter` | Water filter / UV sterilizer | **Wasserfilter / UV-Entkeimung** | check | |
| `cmp_water_heater` | Water heater (calorifier) | **Boiler (Warmwasserbereiter)** | check | |
| `cmp_water_heater_anode` | Calorifier anode | **Boileranode** | check | |
| `cmp_water_pipes` | Water pipework & fittings | **Wasserleitungen & Fittings** | check | |
| `cmp_water_pump` | Pressure water pump & accumulator | **Druckwasserpumpe & Ausdehnungsgefäß** | check | |
| `cmp_water_tank` | Fresh water tank | **Frischwassertank** | check | |
| `cmp_water_tank_sanitise` | Water tank cleaning & sanitizing | **Frischwassertank reinigen & desinfizieren** | check | |
| `cmp_watermaker` | Watermaker | **Wassermacher** | check | |
| `cmp_watermaker_membrane` | Watermaker membrane & pickling | **Membran & Konservierung des Wassermachers** | check | |
| `cmp_watermaker_prefilters` | Watermaker pre-filters | **Vorfilter des Wassermachers** | check | |
| `cmp_winch_handles` | Winch handles & holders | **Winschkurbeln & Halter** | check | |
| `cmp_winches` | Winches | **Winschen** | check | |
| `cmp_wind_gen` | Wind generator | **Windgenerator** | check | |
| `cmp_wind_instrument` | Wind instrument & masthead unit | **Windmessanlage & Mastgeber** | check | |
| `cmp_windlass` | Windlass | **Ankerwinde** | check | |
| `cmp_windlass_clutch` | Windlass clutch & friction cone | **Kupplung & Reibkonus der Ankerwinde** | check | |
| `cmp_windlass_contactor` | Windlass contactor & controls | **Schaltschütz & Bedienung der Ankerwinde** | check | |
| `cmp_windlass_manual` | Windlass emergency handle | **Notkurbel der Ankerwinde** | check | |
| `cmp_wire_terminals` | Standing rigging terminals (swage / mechanical) | **Terminals (gewalzt / mechanisch)** | check | |
| `cmp_wooden_bungs` | Emergency wooden plugs (bungs) | **Notfall-Holzpfropfen** | check | |
| `hint_actuator_type` | Actuator type | **Antriebsart** | check | |
| `hint_alarm_threshold_deg_c` | Alarm threshold (°C) | **Alarmschwelle (°C)** | check | |
| `hint_anode_part_no` | Anode part no | **Teilenummer der Anode** | check | |
| `hint_application_date` | Application date | **Datum des Anstrichs** | check | |
| `hint_batten_count` | Batten count | **Anzahl Latten** | check | |
| `hint_battery_expiry` | Battery expiry | **Ablaufdatum der Batterie** | check | |
| `hint_bearing_kit` | Bearing kit | **Lagersatz** | check | |
| `hint_bearing_part_no` | Bearing part no | **Teilenummer des Lagers** | check | |
| `hint_bellows_part_no` | Bellows part no | **Teilenummer des Balgs** | check | |
| `hint_belt_or_chain` | Belt or chain | **Riemen oder Kette** | check | |
| `hint_belt_part_no` | Belt part no | **Teilenummer des Riemens** | check | |
| `hint_belt_size` | Belt size | **Riemengröße** | check | |
| `hint_blades` | Blades | **Flügel** | check | |
| `hint_bolt_count` | Bolt count | **Anzahl Bolzen** | check | |
| `hint_bottle_size` | Bottle size | **Flaschengröße** | check | |
| `hint_bulb_types` | Bulb types | **Leuchtmitteltypen** | check | |
| `hint_burner_count` | Burner count | **Anzahl Brenner** | check | |
| `hint_cable_part_no` | Cable part no | **Teilenummer des Zugs** | check | |
| `hint_call_sign` | Call sign | **Rufzeichen** | check | |
| `hint_capacity` | Capacity | **Kapazität** | check | |
| `hint_capacity_ah` | Capacity (Ah) | **Kapazität (Ah)** | check | |
| `hint_capacity_l` | Capacity (L) | **Volumen (l)** | check | |
| `hint_cca` | CCA | **Kaltstartstrom (CCA)** | check | |
| `hint_cfm` | CFM | **Luftleistung (CFM)** | check | |
| `hint_chemistry` | Chemistry | **Batterietyp (AGM / Gel / LiFePO₄)** | check | |
| `hint_cloth` | Cloth | **Tuch** | check | |
| `hint_coats` | Coats | **Anzahl Anstriche** | check | |
| `hint_colour` | Colour | **Farbe** | check | |
| `hint_compressor_model` | Compressor model | **Kompressormodell** | check | |
| `hint_connector_type` | Connector type | **Steckertyp** | check | |
| `hint_contents_list` | Contents list | **Inhaltsliste** | check | |
| `hint_controller_model` | Controller model | **Reglermodell** | check | |
| `hint_coolant_capacity_l` | Coolant capacity (L) | **Kühlmittelmenge (l)** | check | |
| `hint_coolant_spec` | Coolant spec | **Kühlmittelspezifikation** | check | |
| `hint_cooler_make` | Cooler make | **Hersteller des Kühlers** | check | |
| `hint_count` | Count | **Anzahl** | check | |
| `hint_counts` | Counts | **Anzahl** | check | |
| `hint_cruising_area` | Cruising area | **Fahrtgebiet** | check | |
| `hint_cylinder_size` | Cylinder size | **Patronengröße** | check | |
| `hint_date` | Date | **Datum** | check | |
| `hint_date_stamped_on_hose` | Date stamped on hose | **Aufgedrucktes Schlauchdatum** | check | |
| `hint_diameter` | Diameter | **Durchmesser** | check | |
| `hint_diaphragm_kit_part_no` | Diaphragm kit part no | **Teilenummer des Membransatzes** | check | |
| `hint_dot_date` | DOT date | **DOT-Datum** | check | |
| `hint_drive_type` | Drive type | **Antriebsart** | check | |
| `hint_duct_condition` | Duct condition | **Zustand der Luftkanäle** | check | |
| `hint_earliest_medicine_expiry` | Earliest medicine expiry | **Frühestes Ablaufdatum der Medikamente** | check | |
| `hint_element_part_no` | Element part no | **Teilenummer des Einsatzes** | check | |
| `hint_element_w` | Element W | **Heizstab (W)** | check | |
| `hint_exhaust_gap` | Exhaust gap | **Ventilspiel Auslass** | check | |
| `hint_expansion_tank_pressure` | Expansion tank pressure | **Vordruck des Ausdehnungsgefäßes** | check | |
| `hint_expiry` | Expiry | **Ablaufdatum** | check | |
| `hint_expiry_date` | Expiry date | **Ablaufdatum** | check | |
| `hint_filter_part_no` | Filter part no | **Teilenummer des Filters** | check | |
| `hint_fitted_date` | Fitted date | **Einbaudatum** | check | |
| `hint_fitted_year` | Fitted year | **Einbaujahr** | check | |
| `hint_flow_l_min` | Flow (l/min) | **Förderleistung (l/min)** | check | |
| `hint_fluid_spec` | Fluid spec | **Flüssigkeitsspezifikation** | check | |
| `hint_fuel_type` | Fuel type | **Kraftstoffart** | check | |
| `hint_fuse` | Fuse | **Sicherung** | check | |
| `hint_fuse_rating` | Fuse rating | **Sicherungswert** | check | |
| `hint_gap` | Gap | **Abstand** | check | |
| `hint_gasket_part_no` | Gasket part no | **Teilenummer der Dichtung** | check | |
| `hint_gearcase_oil` | Gearcase oil | **Getriebeöl (Unterwasserteil)** | check | |
| `hint_gelcoat_colour` | Gelcoat colour | **Gelcoat-Farbe** | check | |
| `hint_glow_plug_part_no` | Glow plug part no | **Teilenummer der Glühkerze** | check | |
| `hint_grade` | Grade | **Qualität** | check | |
| `hint_grease_type` | Grease type | **Fettsorte** | check | |
| `hint_gypsy_size` | Gypsy size | **Größe der Kettennuss** | check | |
| `hint_handheld_vhf` | Handheld VHF | **UKW-Handfunkgerät** | check | |
| `hint_height` | Height | **Höhe** | check | |
| `hint_hex_id` | Hex ID | **Hex ID** | check | |
| `hint_impeller_part_no` | Impeller part no | **Teilenummer des Impellers** | check | |
| `hint_install_date` | Install date | **Einbaudatum** | check | |
| `hint_insurer` | Insurer | **Versicherer** | check | |
| `hint_intake_gap` | Intake gap | **Ventilspiel Einlass** | check | |
| `hint_key_size` | Key size | **Passfedergröße** | check | |
| `hint_last_replaced` | Last replaced | **Zuletzt ersetzt** | check | |
| `hint_length` | Length | **Länge** | check | |
| `hint_lengths` | Lengths | **Längen** | check | |
| `hint_litres_used` | Litres used | **Verbrauchte Liter** | check | |
| `hint_make` | Make | **Hersteller** | check | |
| `hint_maker` | Maker | **Hersteller** | check | |
| `hint_material` | Material | **Material** | check | |
| `hint_material_zn_al_mg` | Material (Zn / Al / Mg) | **Material (Zn / Al / Mg)** | check | |
| `hint_membrane_part_no` | Membrane part no | **Teilenummer der Membran** | check | |
| `hint_micron` | Micron | **Filterfeinheit (µm)** | check | |
| `hint_mmsi` | MMSI | **MMSI** | check | |
| `hint_model` | Model | **Modell** | check | |
| `hint_models` | Models | **Modelle** | check | |
| `hint_mount_part_no` | Mount part no | **Teilenummer des Lagers (Motorlager)** | check | |
| `hint_next_check` | Next check | **Nächste Prüfung** | check | |
| `hint_next_service_due` | Next service due | **Nächste Wartung fällig** | check | |
| `hint_number_of_chambers` | Number of chambers | **Anzahl Luftkammern** | check | |
| `hint_official_number` | Official number | **Registernummer** | check | |
| `hint_oil_grade` | Oil grade | **Viskosität** | check | |
| `hint_oil_quantity` | Oil quantity | **Ölmenge** | check | |
| `hint_oil_type` | Oil type | **Ölsorte** | check | |
| `hint_opening_temp` | Opening temp | **Öffnungstemperatur** | check | |
| `hint_output_a` | Output (A) | **Ladestrom (A)** | check | |
| `hint_packing_size` | Packing size | **Packungsgröße (Stopfbuchse)** | check | |
| `hint_paint_brand` | Paint brand | **Farbmarke** | check | |
| `hint_panel_w` | Panel W | **Modulleistung (W)** | check | |
| `hint_part_number` | Part number | **Teilenummer** | check | |
| `hint_pawl_spring_part_no` | Pawl spring part no | **Teilenummer der Sperrklinkenfeder** | check | |
| `hint_pitch` | Pitch | **Steigung** | check | |
| `hint_plug_type` | Plug type | **Kerzentyp** | check | |
| `hint_policy_no` | Policy no | **Policennummer** | check | |
| `hint_polish_product` | Polish product | **Politur** | check | |
| `hint_pre_filter_micron` | Pre-filter micron | **Vorfilter-Feinheit (µm)** | check | |
| `hint_pressure` | Pressure | **Druck** | check | |
| `hint_pressure_switch` | Pressure switch | **Druckschalter** | check | |
| `hint_probe_part_no` | Probe part no | **Teilenummer des Fühlers** | check | |
| `hint_propeller_size` | Propeller size | **Propellergröße** | check | |
| `hint_quantity` | Quantity | **Menge** | check | |
| `hint_quantity_l` | Quantity (L) | **Menge (l)** | check | |
| `hint_refrigerant` | Refrigerant | **Kältemittel** | check | |
| `hint_registered_to` | Registered to | **Eingetragen auf** | check | |
| `hint_registry` | Registry | **Heimathafen** | check | |
| `hint_rigger` | Rigger | **Rigger** | check | |
| `hint_run_time_before_start` | Run time before start | **Lüfterlaufzeit vor dem Start** | check | |
| `hint_sailmaker` | Sailmaker | **Segelmacher** | check | |
| `hint_scheme_itb_bss_revisione_bootszeugnis` | Scheme (ITB/BSS/Revisione/Bootszeugnis) | **Prüfsystem (ITB / BSS / Revisione / Bootszeugnis)** | check | |
| `hint_screen_part_no` | Screen part no | **Teilenummer des Siebs** | check | |
| `hint_seal_part_numbers` | Seal part numbers | **Teilenummern der Dichtungen** | check | |
| `hint_section` | Section | **Profil** | check | |
| `hint_sensor_expiry` | Sensor expiry | **Ablaufdatum des Sensors** | check | |
| `hint_serial` | Serial | **Seriennummer** | check | |
| `hint_service_kit` | Service kit | **Servicesatz** | check | |
| `hint_service_kit_part_no` | Service kit part no | **Teilenummer des Servicesatzes** | check | |
| `hint_shear_pin_part_no` | Shear pin part no | **Teilenummer des Scherstifts** | check | |
| `hint_sheave_count` | Sheave count | **Anzahl Scheiben** | check | |
| `hint_signaling_mirror` | Signaling mirror | **Signalspiegel** | check | |
| `hint_silencer_make` | Silencer make | **Hersteller des Schalldämpfers** | check | |
| `hint_size` | Size | **Größe** | check | |
| `hint_size_pct` | Size % | **Größe (%)** | check | |
| `hint_sizes` | Sizes | **Größen** | check | |
| `hint_socket_size` | Socket size | **Schlüsselweite** | check | |
| `hint_software_version` | Software version | **Softwareversion** | check | |
| `hint_spare_aboard` | Spare aboard? | **Ersatz an Bord?** | check | |
| `hint_spares_aboard` | Spares aboard | **Ersatzteile an Bord** | check | |
| `hint_stack_insulation_type` | Stack insulation type | **Isolierung des Abgasrohrs** | check | |
| `hint_surveyor` | Surveyor | **Gutachter** | check | |
| `hint_thread_size` | Thread size | **Gewinde** | check | |
| `hint_torque` | Torque | **Anzugsdrehmoment** | check | |
| `hint_torque_spec` | Torque spec | **Anzugsdrehmoment (Vorgabe)** | check | |
| `hint_tube_material` | Tube material | **Schlauchmaterial (PVC / Hypalon)** | check | |
| `hint_type` | Type | **Typ** | check | |
| `hint_type_a_b` | Type (A/B) | **Typ (A / B)** | check | |
| `hint_types` | Types | **Typen** | check | |
| `hint_water` | Water | **Wasser** | check | |
| `hint_weight_kg` | Weight (kg) | **Gewicht (kg)** | check | |
| `hint_wire_dia` | Wire dia | **Drahtdurchmesser** | check | |
| `hint_working_pressure` | Working pressure | **Betriebsdruck** | check | |
| `hint_year` | Year | **Jahr** | check | |
| `opt_cooling_keel_cooled` | Keel cooled, dry exhaust | **Kielkühlung, Trockenauspuff** | check | |
| `opt_cooling_raw_water` | Raw water | **Seewasserkühlung** | check | |
| `opt_drive_none` | No drive | **Kein Antrieb** | check | |
| `opt_drive_outboard` | Outboard | **Außenborder** | check | |
| `opt_drive_saildrive` | Saildrive | **Saildrive** | check | |
| `opt_drive_shaft` | Shaft | **Wellenanlage** | check | |
| `opt_drive_sterndrive` | Sterndrive | **Z-Antrieb** | check | |
| `opt_engine_diesel` | Diesel | **Diesel** | check | |
| `opt_engine_electric` | Electric | **Elektro** | check | |
| `opt_engine_none` | No engine | **Kein Motor** | check | |
| `opt_engine_petrol` | Petrol | **Benzin** | check | |
| `opt_extras_aircon` | Air conditioning | **Klimaanlage** | check | |
| `opt_extras_ais` | AIS | **AIS** | check | |
| `opt_extras_autopilot` | Autopilot | **Autopilot** | check | |
| `opt_extras_cabin` | Cabin | **Kajüte** | check | |
| `opt_extras_davits` | Davits | **Davits** | check | |
| `opt_extras_epirb` | EPIRB | **EPIRB** | check | |
| `opt_extras_fresh_water` | Fresh water system | **Frischwasseranlage** | check | |
| `opt_extras_fridge` | Fridge or freezer | **Kühlschrank oder Gefrierfach** | check | |
| `opt_extras_furler` | Headsail furler | **Vorsegel-Rollreffanlage** | check | |
| `opt_extras_genset` | Generator | **Generator** | check | |
| `opt_extras_heads` | Marine heads | **Bordtoilette** | check | |
| `opt_extras_heater` | Cabin heater | **Kajütheizung** | check | |
| `opt_extras_holding_tank` | Holding tank | **Fäkalientank** | check | |
| `opt_extras_hydrogenerator` | Hydrogenerator | **Schleppgenerator** | check | |
| `opt_extras_instruments` | Instruments | **Instrumente** | check | |
| `opt_extras_inverter` | Inverter | **Wechselrichter** | check | |
| `opt_extras_liferaft` | Liferaft | **Rettungsinsel** | check | |
| `opt_extras_lpg` | Gas system | **Gasanlage** | check | |
| `opt_extras_offshore` | Offshore passages | **Hochseetörns** | check | |
| `opt_extras_passerelle` | Passerelle | **Passerelle** | check | |
| `opt_extras_radar` | Radar | **Radar** | check | |
| `opt_extras_shore_power` | Shore power | **Landstrom** | check | |
| `opt_extras_solar` | Solar panels | **Solarmodule** | check | |
| `opt_extras_tender` | Tender | **Beiboot** | check | |
| `opt_extras_thruster` | Bow or stern thruster | **Bug- oder Heckstrahlruder** | check | |
| `opt_extras_trailer` | Trailer | **Bootsanhänger** | check | |
| `opt_extras_watermaker` | Watermaker | **Wassermacher** | check | |
| `opt_extras_wind_gen` | Wind generator | **Windgenerator** | check | |
| `opt_extras_windlass` | Windlass | **Ankerwinde** | check | |
| `opt_hull_catamaran_sail` | Sailing catamaran | **Segelkatamaran** | check | |
| `opt_hull_dinghy` | Dinghy or tender | **Jolle oder Beiboot** | check | |
| `opt_hull_monohull_sail` | Sailing monohull | **Segelyacht (Einrumpf)** | check | |
| `opt_hull_motor` | Motor boat | **Motorboot** | check | |
| `opt_hull_rib` | RIB | **Festrumpfschlauchboot (RIB)** | check | |
| `opt_hull_trimaran_sail` | Sailing trimaran | **Segeltrimaran** | check | |
| `opt_keel_bilge` | Bilge keels | **Kimmkiele** | check | |
| `opt_keel_centreboard` | Centreboard | **Schwert** | check | |
| `opt_keel_daggerboard` | Daggerboard | **Steckschwert** | check | |
| `opt_keel_fin` | Fin keel | **Flossenkiel** | check | |
| `opt_keel_lifting` | Lifting keel | **Hubkiel** | check | |
| `opt_keel_long` | Long keel | **Langkiel** | check | |
| `opt_keel_none` | No keel | **Kein Kiel** | check | |
| `opt_rig_cat_rig` | Cat rig | **Catboot** | check | |
| `opt_rig_cutter` | Cutter | **Kutter** | check | |
| `opt_rig_junk` | Junk rig | **Dschunkenrigg** | check | |
| `opt_rig_ketch` | Ketch | **Ketsch** | check | |
| `opt_rig_none` | No rig | **Kein Rigg** | check | |
| `opt_rig_schooner` | Schooner | **Schoner** | check | |
| `opt_rig_sloop` | Sloop | **Slup** | check | |
| `opt_rig_yawl` | Yawl | **Yawl** | check | |
| `opt_storage_afloat_year_round` | Afloat all year | **Ganzjährig im Wasser** | check | |
| `opt_storage_dry_stack` | Dry stack | **Trockenlager (Hochregal)** | check | |
| `opt_storage_hauled_winter` | Hauled out for winter | **Winterlager an Land** | check | |
| `opt_storage_trailer` | On a trailer | **Auf dem Trailer** | check | |
| `opt_water_brackish` | Brackish | **Brackwasser** | check | |
| `opt_water_fresh` | Fresh | **Süßwasser** | check | |
| `opt_water_salt` | Salt | **Salzwasser** | check | |
