package org.sprachcafe.team.data

object DefaultKioskData {
    val items = listOf(
        // Kalte Getränke
        KioskItem(
            id = "item-01",
            name = "Mate-Limo",
            category = ItemCategory.COLD_DRINKS,
            unit = "Flasche",
            priceCents = 300,
            costCents = 120,
            taxSphere = "Wirtschaftl. Geschäftsbetrieb",
            barcode = "4029764001807",
            icon = "🧉"
        ),
        KioskItem(
            id = "item-02",
            name = "Ostmost (div. Sorten)",
            category = ItemCategory.COLD_DRINKS,
            unit = "Flasche",
            priceCents = 300,
            costCents = 130,
            taxSphere = "Wirtschaftl. Geschäftsbetrieb",
            barcode = "4260384660017",
            icon = "🍎"
        ),
        KioskItem(
            id = "item-03",
            name = "Quetschie / Fruchtmus",
            category = ItemCategory.COLD_DRINKS,
            unit = "Btl",
            priceCents = 200,
            costCents = 80,
            taxSphere = "Wirtschaftl. Geschäftsbetrieb",
            barcode = "4008400404127",
            icon = "🍓"
        ),
        KioskItem(
            id = "item-04",
            name = "Saft (Apfel / Orange)",
            category = ItemCategory.COLD_DRINKS,
            unit = "Glas/Fl",
            priceCents = 250,
            costCents = 110,
            taxSphere = "Wirtschaftl. Geschäftsbetrieb",
            icon = "🧃"
        ),
        KioskItem(
            id = "item-05",
            name = "Wasser 0,5L",
            category = ItemCategory.COLD_DRINKS,
            unit = "Flasche",
            priceCents = 200,
            costCents = 50,
            taxSphere = "Wirtschaftl. Geschäftsbetrieb",
            barcode = "4015533017209",
            icon = "💧"
        ),

        // Warme Getränke
        KioskItem(
            id = "item-06",
            name = "Kaffee klein (Espresso/Filter)",
            category = ItemCategory.HOT_DRINKS,
            unit = "Tasse",
            priceCents = 250,
            costCents = 40,
            taxSphere = "Zweckbetrieb / Kiosk",
            icon = "☕"
        ),
        KioskItem(
            id = "item-07",
            name = "Kaffee groß / Cappuccino",
            category = ItemCategory.HOT_DRINKS,
            unit = "Tasse/Glas",
            priceCents = 350,
            costCents = 70,
            taxSphere = "Zweckbetrieb / Kiosk",
            icon = "☕"
        ),
        KioskItem(
            id = "item-08",
            name = "Tee (div. Bio-Sorten)",
            category = ItemCategory.HOT_DRINKS,
            unit = "Tasse",
            priceCents = 200,
            costCents = 30,
            taxSphere = "Zweckbetrieb / Kiosk",
            icon = "🫖"
        ),
        KioskItem(
            id = "item-09",
            name = "Heiße Schokolade",
            category = ItemCategory.HOT_DRINKS,
            unit = "Becher",
            priceCents = 400,
            costCents = 90,
            taxSphere = "Zweckbetrieb / Kiosk",
            icon = "🍫"
        ),

        // Snacks & Gebäck
        KioskItem(
            id = "item-10",
            name = "Kuchen (frisch)",
            category = ItemCategory.SNACKS,
            unit = "Stück",
            priceCents = 250,
            costCents = 100,
            taxSphere = "Zweckbetrieb / Kiosk",
            icon = "🍰"
        ),
        KioskItem(
            id = "item-11",
            name = "Krówki / Poln. Bonbons",
            category = ItemCategory.SNACKS,
            unit = "Stk",
            priceCents = 30,
            costCents = 10,
            taxSphere = "Wirtschaftl. Geschäftsbetrieb",
            icon = "🍬"
        ),
        KioskItem(
            id = "item-12",
            name = "Lolly",
            category = ItemCategory.SNACKS,
            unit = "Stk",
            priceCents = 50,
            costCents = 15,
            taxSphere = "Wirtschaftl. Geschäftsbetrieb",
            icon = "🍭"
        ),
        KioskItem(
            id = "item-13",
            name = "Prinz Polo / Riegel klein",
            category = ItemCategory.SNACKS,
            unit = "Stk",
            priceCents = 50,
            costCents = 20,
            taxSphere = "Wirtschaftl. Geschäftsbetrieb",
            barcode = "7622210669148",
            icon = "🍫"
        ),
        KioskItem(
            id = "item-14",
            name = "Schokoriegel groß",
            category = ItemCategory.SNACKS,
            unit = "Stk",
            priceCents = 100,
            costCents = 45,
            taxSphere = "Wirtschaftl. Geschäftsbetrieb",
            icon = "🍪"
        ),

        // Kleiner Laden & Merch
        KioskItem(
            id = "item-15",
            name = "T-Shirt (Vereins-Design)",
            category = ItemCategory.SHOP,
            unit = "Stk",
            priceCents = 2000,
            costCents = 850,
            taxSphere = "Wirtschaftl. Geschäftsbetrieb",
            icon = "👕"
        ),
        KioskItem(
            id = "item-16",
            name = "Speak-Dating Kartenspiel",
            category = ItemCategory.SHOP,
            unit = "Set",
            priceCents = 2000,
            costCents = 420,
            taxSphere = "Zweckbetrieb / Merch",
            icon = "🎴"
        ),

        // Spenden & Sonstiges
        KioskItem(
            id = "item-17",
            name = "Spende (Bücher / Kiez)",
            category = ItemCategory.DONATIONS,
            unit = "Vorgang",
            priceCents = 200,
            costCents = 0,
            taxSphere = "Ideeller Bereich / Spende",
            icon = "💛"
        ),
        KioskItem(
            id = "item-18",
            name = "Kaffee-Kasse freiwillig",
            category = ItemCategory.DONATIONS,
            unit = "Vorgang",
            priceCents = 100,
            costCents = 0,
            taxSphere = "Ideeller Bereich / Spende",
            icon = "☕"
        )
    )
}
