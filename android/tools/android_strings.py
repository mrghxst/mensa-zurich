# SPDX-License-Identifier: GPL-3.0-or-later
"""Android-only strings in en/de/fr/it. Writes res/values*/strings_android.xml.

Usage (from the repository root):
    python android/tools/android_strings.py

Edit the table below, not the XML. Use a typographic apostrophe (’) so no escaping is needed.
"""
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "android" / "app" / "src" / "main" / "res"
FOLDERS = ["values", "values-de", "values-fr", "values-it"]

# key: (en, de, fr, it)
STRINGS = {
    # List
    "offline_banner": ("Offline · showing saved menus", "Offline · gespeicherte Menüs", "Hors ligne · menus enregistrés", "Offline · menu salvati"),
    "update_failed": ("Couldn’t update %1$s menus", "%1$s-Menüs konnten nicht aktualisiert werden", "Impossible de mettre à jour les menus %1$s", "Impossibile aggiornare i menu %1$s"),
    "slow_connection": ("Slow connection", "Langsame Verbindung", "Connexion lente", "Connessione lenta"),
    "cancel": ("Cancel", "Abbrechen", "Annuler", "Annulla"),
    "open_only_disabled_hint": ("Not available when a weekday is selected", "Nicht verfügbar, wenn ein Wochentag gewählt ist", "Indisponible lorsqu’un jour est sélectionné", "Non disponibile quando è selezionato un giorno"),
    "status": ("Status", "Status", "Statut", "Stato"),
    "day": ("Day", "Tag", "Jour", "Giorno"),
    "layout": ("Layout", "Darstellung", "Affichage", "Layout"),
    "share": ("Share", "Teilen", "Partager", "Condividi"),
    "sort_smart": ("Smart", "Smart", "Intelligent", "Intelligente"),
    "sort_name": ("Name", "Name", "Nom", "Nome"),
    "layout_minimal": ("Minimal", "Minimal", "Minimal", "Minimale"),
    "layout_standard": ("Standard", "Standard", "Standard", "Standard"),
    "layout_large": ("Large", "Gross", "Grand", "Grande"),
    "a11y_open_now": ("%1$s, open now", "%1$s, jetzt geöffnet", "%1$s, ouvert maintenant", "%1$s, aperto ora"),
    "a11y_closed": ("%1$s, closed", "%1$s, geschlossen", "%1$s, fermé", "%1$s, chiuso"),
    "clear_search": ("Clear search", "Suche löschen", "Effacer la recherche", "Cancella ricerca"),
    "filters_active": ("Filters active", "Filter aktiv", "Filtres actifs", "Filtri attivi"),
    # Detail
    "back": ("Back", "Zurück", "Retour", "Indietro"),
    "open_in_maps": ("Open in Maps", "In Karten öffnen", "Ouvrir dans Plans", "Apri in Mappe"),
    "meal_photo": ("Meal photo", "Foto des Gerichts", "Photo du plat", "Foto del piatto"),
    "nutrition": ("Nutrition", "Nährwerte", "Valeurs nutritives", "Valori nutrizionali"),
    "per_100g": ("per 100 g", "pro 100 g", "pour 100 g", "per 100 g"),
    "per_serving": ("per serving", "pro Portion", "par portion", "per porzione"),
    "nutrition_energy": ("Energy", "Energie", "Énergie", "Energia"),
    "nutrition_protein": ("Protein", "Eiweiss", "Protéines", "Proteine"),
    "nutrition_fat": ("Fat", "Fett", "Lipides", "Grassi"),
    "nutrition_saturated": ("of which saturates", "davon gesättigte Fettsäuren", "dont acides gras saturés", "di cui saturi"),
    "nutrition_carbohydrates": ("Carbohydrates", "Kohlenhydrate", "Glucides", "Carboidrati"),
    "nutrition_sugar": ("of which sugars", "davon Zucker", "dont sucres", "di cui zuccheri"),
    "nutrition_fibers": ("Fibre", "Ballaststoffe", "Fibres", "Fibre"),
    "nutrition_salt": ("Salt", "Salz", "Sel", "Sale"),
    "diet_vegan": ("Vegan", "Vegan", "Végétalien", "Vegano"),
    "diet_vegetarian": ("Vegetarian", "Vegetarisch", "Végétarien", "Vegetariano"),
    "diet_fish": ("Fish", "Fisch", "Poisson", "Pesce"),
    "meat_beef": ("Beef", "Rind", "Bœuf", "Manzo"),
    "meat_chicken": ("Chicken", "Huhn", "Poulet", "Pollo"),
    "meat_duck": ("Duck", "Ente", "Canard", "Anatra"),
    "meat_veal": ("Veal", "Kalb", "Veau", "Vitello"),
    "meat_turkey": ("Turkey", "Truthahn", "Dinde", "Tacchino"),
    "meat_pork": ("Pork", "Schwein", "Porc", "Maiale"),
    "meat_lamb": ("Lamb", "Lamm", "Agneau", "Agnello"),
    "meat_game": ("Game", "Wild", "Gibier", "Selvaggina"),
    # Settings
    "theme": ("Theme", "Design", "Thème", "Tema"),
    "theme_system": ("System", "System", "Système", "Sistema"),
    "theme_light": ("Light", "Hell", "Clair", "Chiaro"),
    "theme_dark": ("Dark", "Dunkel", "Sombre", "Scuro"),
    "wallpaper_colors": ("Wallpaper colors", "Hintergrundfarben", "Couleurs du fond d’écran", "Colori dello sfondo"),
    "wallpaper_colors_footer": ("Use colors from your wallpaper instead of the ETH blue.", "Farben aus deinem Hintergrundbild statt ETH-Blau verwenden.", "Utiliser les couleurs de votre fond d’écran au lieu du bleu ETH.", "Usa i colori dello sfondo invece del blu ETH."),
    "backup": ("Backup", "Sicherung", "Sauvegarde", "Backup"),
    "backup_footer": ("Your settings are saved with your Android device backup and restored on a new phone.", "Deine Einstellungen werden mit der Android-Gerätesicherung gespeichert und auf einem neuen Telefon wiederhergestellt.", "Vos réglages sont inclus dans la sauvegarde de votre appareil Android et restaurés sur un nouveau téléphone.", "Le impostazioni vengono salvate con il backup del dispositivo Android e ripristinate su un nuovo telefono."),
    "open_links": ("Open ethmensa.ch links in the app", "ethmensa.ch-Links in der App öffnen", "Ouvrir les liens ethmensa.ch dans l’app", "Apri i link ethmensa.ch nell’app"),
    "open_links_footer": ("Android asks once: tap “Add link” and select ethmensa.ch.", "Android fragt einmal: Tippe auf „Link hinzufügen“ und wähle ethmensa.ch.", "Android le demande une fois : touchez « Ajouter un lien » et choisissez ethmensa.ch.", "Android lo chiede una volta: tocca “Aggiungi link” e scegli ethmensa.ch."),
    "language_system": ("System default", "Systemstandard", "Langue du système", "Predefinita di sistema"),
    "language_applies_now": ("Applies immediately.", "Wird sofort übernommen.", "S’applique immédiatement.", "Si applica subito."),
    "irreversible": ("These actions can’t be undone.", "Diese Aktionen können nicht rückgängig gemacht werden.", "Ces actions sont irréversibles.", "Queste azioni non possono essere annullate."),
    "reset_confirm_title": ("%1$s?", "%1$s?", "%1$s ?", "%1$s?"),
    "about_section": ("About", "Über", "À propos", "Info"),
    "about_app": ("ETH Mensa for Android", "ETH Mensa für Android", "ETH Mensa pour Android", "ETH Mensa per Android"),
    "version": ("Version %1$s (%2$d)", "Version %1$s (%2$d)", "Version %1$s (%2$d)", "Versione %1$s (%2$d)"),
    "based_on": ("Based on ETH Mensa for iOS by Alexandre Reol", "Basiert auf ETH Mensa für iOS von Alexandre Reol", "Basé sur ETH Mensa pour iOS d’Alexandre Reol", "Basato su ETH Mensa per iOS di Alexandre Reol"),
    "unofficial": ("Unofficial port, not affiliated with the original app.", "Inoffizielle Portierung, nicht mit der Original-App verbunden.", "Portage non officiel, sans lien avec l’app originale.", "Port non ufficiale, non collegato all’app originale."),
    "source_code": ("Source code", "Quellcode", "Code source", "Codice sorgente"),
    "source_code_private": ("github.com/mrghxst/ethmensa (private repository)", "github.com/mrghxst/ethmensa (privates Repository)", "github.com/mrghxst/ethmensa (dépôt privé)", "github.com/mrghxst/ethmensa (repository privato)"),
    "license": ("License", "Lizenz", "Licence", "Licenza"),
    "open_source_licenses": ("Open-source licenses", "Open-Source-Lizenzen", "Licences open source", "Licenze open source"),
    "privacy": ("Privacy", "Datenschutz", "Confidentialité", "Privacy"),
    "privacy_statement": ("No tracking or analytics. The app only loads menus from ETH Zürich and ZFV, mensa photos, and nothing else. Your settings stay on your device and in your own Android backup.", "Kein Tracking, keine Analyse. Die App lädt nur Menüs von der ETH Zürich und vom ZFV sowie Mensa-Fotos, sonst nichts. Deine Einstellungen bleiben auf deinem Gerät und in deiner eigenen Android-Sicherung.", "Aucun suivi ni analyse. L’app ne charge que les menus de l’ETH Zurich et du ZFV ainsi que les photos des mensas, rien d’autre. Vos réglages restent sur votre appareil et dans votre propre sauvegarde Android.", "Nessun tracciamento né analisi. L’app carica solo i menu dell’ETH di Zurigo e di ZFV e le foto delle mense, nient’altro. Le impostazioni restano sul dispositivo e nel tuo backup Android."),
    # Onboarding (Android wording for the Legi feature and the privacy notice)
    "onboarding_legi_description": ("The ETH Zürich Legi Card is digital. Open it in the app, from the Quick Settings tile or by long-pressing the app icon. Works at any ETH and UZH Mensa.", "Die Legi der ETH Zürich ist digital. Öffne sie in der App, über die Schnelleinstellungen oder durch langes Drücken auf das App-Symbol. Funktioniert in jeder ETH- und UZH-Mensa.", "La carte Legi de l’ETH Zurich est numérique. Ouvrez-la dans l’app, depuis les Réglages rapides ou par un appui long sur l’icône de l’app. Fonctionne dans toutes les Mensas ETH et UZH.", "La Legi Card dell’ETH di Zurigo è digitale. Aprila nell’app, dalle Impostazioni rapide o tenendo premuta l’icona dell’app. Funziona in ogni Mensa ETH e UZH."),
    "onboarding_privacy": ("No tracking or analytics whatsoever.", "Keinerlei Tracking oder Analyse.", "Aucun suivi ni analyse, d’aucune sorte.", "Nessun tracciamento né analisi, di alcun tipo."),
    # Allergen display names (the iOS catalog only has raw ids for some)
    "allergen_gluten": ("Gluten", "Gluten", "Gluten", "Glutine"),
    "allergen_crustaceans": ("Crustaceans", "Krebstiere", "Crustacés", "Crostacei"),
    "allergen_eggs": ("Eggs", "Eier", "Œufs", "Uova"),
    "allergen_fish": ("Fish", "Fisch", "Poisson", "Pesce"),
    "allergen_peanuts": ("Peanuts", "Erdnüsse", "Arachides", "Arachidi"),
    "allergen_soya": ("Soya", "Soja", "Soja", "Soia"),
    "allergen_lactose": ("Lactose", "Laktose", "Lactose", "Lattosio"),
    "allergen_nuts": ("Nuts", "Schalenfrüchte", "Fruits à coque", "Frutta a guscio"),
    "allergen_celery": ("Celery", "Sellerie", "Céleri", "Sedano"),
    "allergen_mustard": ("Mustard", "Senf", "Moutarde", "Senape"),
    "allergen_sesame": ("Sesame", "Sesam", "Sésame", "Sesamo"),
    "allergen_sulphurdioxide": ("Sulphur dioxide", "Schwefeldioxid", "Anhydride sulfureux", "Anidride solforosa"),
    "allergen_lupin": ("Lupin", "Lupinen", "Lupin", "Lupini"),
    "allergen_molluscs": ("Molluscs", "Weichtiere", "Mollusques", "Molluschi"),
    "allergen_wheat": ("Wheat", "Weizen", "Blé", "Frumento"),
    # Phase 9 extras
    "diet": ("Diet", "Ernährung", "Régime", "Dieta"),
    "diet_all": ("All", "Alle", "Tout", "Tutti"),
    "layout_expanded": ("Menus", "Menüs", "Menus", "Menu"),
    "favorite": ("Add to favorites", "Zu Favoriten", "Ajouter aux favoris", "Aggiungi ai preferiti"),
    "unfavorite": ("Remove from favorites", "Aus Favoriten entfernen", "Retirer des favoris", "Rimuovi dai preferiti"),
    "hide_mensa": ("Hide", "Ausblenden", "Masquer", "Nascondi"),
    "hidden_snackbar": ("%1$s hidden", "%1$s ausgeblendet", "%1$s masqué", "%1$s nascosta"),
    "undo": ("Undo", "Rückgängig", "Annuler", "Annulla"),
    "favorite_a11y": ("Favorite", "Favorit", "Favori", "Preferito"),
    "my_mensas": ("Favorites and hidden", "Favoriten und ausgeblendete", "Favoris et masquées", "Preferite e nascoste"),
    "my_mensas_footer": ("Long-press a mensa in the list to add it to your favorites or hide it.", "Halte eine Mensa in der Liste gedrückt, um sie zu den Favoriten hinzuzufügen oder auszublenden.", "Appuie longuement sur une mensa dans la liste pour l’ajouter aux favoris ou la masquer.", "Tieni premuta una mensa nell’elenco per aggiungerla ai preferiti o nasconderla."),
    "favorites": ("Favorites", "Favoriten", "Favoris", "Preferite"),
    "hidden_mensas": ("Hidden", "Ausgeblendet", "Masquées", "Nascoste"),
    "no_favorites": ("No favorites yet", "Noch keine Favoriten", "Pas encore de favoris", "Ancora nessuna preferita"),
    "no_hidden": ("No hidden mensas", "Keine ausgeblendeten Mensen", "Aucune mensa masquée", "Nessuna mensa nascosta"),
    "show_again": ("Show again", "Wieder anzeigen", "Afficher à nouveau", "Mostra di nuovo"),
    "no_diet_meal": ("No matching meal", "Kein passendes Menü", "Aucun menu correspondant", "Nessun menu adatto"),
    "no_diet_meal_message": ("No meal on this day fits your diet filter.", "An diesem Tag passt kein Menü zu deinem Ernährungsfilter.", "Aucun menu de ce jour ne correspond à ton filtre de régime.", "Nessun menu di questo giorno corrisponde al tuo filtro dieta."),
    "widget_name": ("Today’s menu", "Menü heute", "Menu du jour", "Menu di oggi"),
    "widget_description": ("Today’s meals at your favorite or most-opened mensa", "Die heutigen Menüs deiner Lieblingsmensa oder der am häufigsten geöffneten", "Les menus du jour de ta mensa favorite ou la plus consultée", "I menu di oggi della tua mensa preferita o più aperta"),
    "widget_open": ("Open", "Geöffnet", "Ouvert", "Aperto"),
    "widget_closed": ("Closed", "Geschlossen", "Fermé", "Chiuso"),
    "widget_loading": ("Loading menus…", "Menüs werden geladen…", "Chargement des menus…", "Caricamento dei menu…"),
    "open_website": ("Open website", "Webseite öffnen", "Ouvrir le site web", "Apri il sito web"),
    "this_week": ("This week", "Diese Woche", "Cette semaine", "Questa settimana"),
    "next_week": ("Next week", "Nächste Woche", "Semaine prochaine", "Settimana prossima"),
    "next_week_unavailable": ("No menus for next week yet", "Noch keine Menüs für nächste Woche", "Pas encore de menus pour la semaine prochaine", "Nessun menu per la settimana prossima"),
    # Notifications
    "sound_system": ("System default", "Systemstandard", "Par défaut du système", "Predefinito di sistema"),
    "sound_click": ("Click", "Klick", "Clic", "Clic"),
    "channel_reminder_system": ("Menu reminder (system sound)", "Menü-Erinnerung (Systemton)", "Rappel du menu (son du système)", "Promemoria menu (suono di sistema)"),
    "channel_reminder_default": ("Menu reminder (ETH Mensa sound)", "Menü-Erinnerung (ETH-Mensa-Ton)", "Rappel du menu (son ETH Mensa)", "Promemoria menu (suono ETH Mensa)"),
    "channel_reminder_click": ("Menu reminder (click sound)", "Menü-Erinnerung (Klickton)", "Rappel du menu (son clic)", "Promemoria menu (suono clic)"),
    "notifications_denied_message": ("Notifications are off for ETH Mensa. Turn them on in the system settings.", "Benachrichtigungen sind für ETH Mensa ausgeschaltet. Schalte sie in den Systemeinstellungen ein.", "Les notifications sont désactivées pour ETH Mensa. Active-les dans les réglages du système.", "Le notifiche sono disattivate per ETH Mensa. Attivale nelle impostazioni di sistema."),
    "location_denied_message": ("Location-based notifications need location access “Allow all the time”.", "Standortbasierte Benachrichtigungen brauchen den Standortzugriff „Immer zulassen“.", "Les notifications basées sur la position nécessitent l’accès « Toujours autoriser ».", "Le notifiche basate sulla posizione richiedono l’accesso «Consenti sempre»."),
    "background_location_title": ("Allow location all the time", "Standort immer zulassen", "Autoriser la position en permanence", "Consenti sempre la posizione"),
    "background_location_message": ("To notify you only on campus, Android needs location access all the time. On the next screen, choose “Allow all the time”.", "Damit du nur auf dem Campus Benachrichtigungen bekommst, braucht Android immer Zugriff auf den Standort. Wähle im nächsten Bildschirm „Immer zulassen“.", "Pour te notifier seulement sur le campus, Android doit accéder à ta position en permanence. À l’écran suivant, choisis « Toujours autoriser ».", "Per avvisarti solo al campus, Android ha bisogno dell’accesso alla posizione sempre. Nella schermata successiva scegli «Consenti sempre»."),
    "notification_time_footer_android": ("Reminders arrive on weekdays within ten minutes of this time.", "Erinnerungen kommen an Wochentagen innerhalb von zehn Minuten nach dieser Zeit.", "Les rappels arrivent en semaine dans les dix minutes suivant cette heure.", "I promemoria arrivano nei giorni feriali entro dieci minuti da quest’ora."),
    "locations_based_notifications_footer_android": ("Only get notifications near ETH Zentrum, Hönggerberg or Irchel. Android tells the app only when you enter or leave a campus (geofencing). This runs on your device, and your location never leaves it.", "Benachrichtigungen nur in der Nähe von ETH Zentrum, Hönggerberg oder Irchel. Android meldet der App nur, wenn du einen Campus betrittst oder verlässt (Geofencing). Das läuft auf deinem Gerät, dein Standort verlässt es nie.", "Notifications seulement près d’ETH Zentrum, Hönggerberg ou Irchel. Android prévient l’app uniquement quand tu entres sur un campus ou le quittes (geofencing). Tout se passe sur ton appareil, ta position ne le quitte jamais.", "Notifiche solo vicino a ETH Zentrum, Hönggerberg o Irchel. Android avvisa l’app solo quando entri o esci da un campus (geofencing). Tutto avviene sul tuo dispositivo, la tua posizione non lo lascia mai."),
}

# key: {quantity: (en, de, fr, it)}. fr and it also need "many".
PLURALS = {
    "widget_more": {
        "one": ("+%1$d more", "+%1$d weitere", "+%1$d de plus", "+%1$d altro"),
        "many": ("+%1$d more", "+%1$d weitere", "+%1$d de plus", "+%1$d altri"),
        "other": ("+%1$d more", "+%1$d weitere", "+%1$d de plus", "+%1$d altri"),
    },
}

NON_TRANSLATABLE = {
    "app_name": "ETH Mensa",
}


def main() -> None:
    for index, folder in enumerate(FOLDERS):
        lines = [
            '<?xml version="1.0" encoding="utf-8"?>',
            "<!-- Generated by android/tools/android_strings.py. Don't edit. -->",
            "<resources>",
        ]
        if folder == "values":
            for key, value in NON_TRANSLATABLE.items():
                lines.append(f'    <string name="{key}" translatable="false">{escape(value)}</string>')
        for key, values in STRINGS.items():
            lines.append(f'    <string name="{key}">{escape(values[index])}</string>')
        for key, quantities in PLURALS.items():
            lines.append(f'    <plurals name="{key}">')
            for quantity, values in quantities.items():
                # "many" exists only in French and Italian.
                if quantity == "many" and folder not in ("values-fr", "values-it"):
                    continue
                lines.append(f'        <item quantity="{quantity}">{escape(values[index])}</item>')
            lines.append("    </plurals>")
        lines.append("</resources>")
        (RES / folder / "strings_android.xml").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {len(STRINGS)} strings in {len(FOLDERS)} locales")


if __name__ == "__main__":
    main()
