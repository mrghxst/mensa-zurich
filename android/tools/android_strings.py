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
}

NON_TRANSLATABLE = {
    "app_name": "ETH Mensa",
    "map_attribution": "© OpenFreeMap © OpenStreetMap contributors",
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
        lines.append("</resources>")
        (RES / folder / "strings_android.xml").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {len(STRINGS)} strings in {len(FOLDERS)} locales")


if __name__ == "__main__":
    main()
