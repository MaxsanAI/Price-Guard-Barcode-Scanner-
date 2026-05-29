package com.priceguard.app

import java.util.Locale

object Localization {
    fun getLanguageKey(): String {
        val lang = Locale.getDefault().language.lowercase()
        return if (listOf("sr", "hr", "bs", "me", "sl").contains(lang)) "sr" else "en"
    }

    private val strings = mapOf(
        "en" to mapOf(
            "hdr_desc" to "Scan, track & compare values worldwide",
            "btn_scan_start" to "Start Scanning",
            "btn_scan_stop" to "Stop",
            "lbl_scan_info" to "Consumer Protection Shield",
            "lbl_scan_prompt" to "Tap 'Start Scanning' and aim your camera at a barcode",
            "lbl_loading_fetch" to "Analyzing Open Food Facts index database...",
            "lbl_price_val" to "Market price estimate",
            "lbl_est" to "Est.",
            "lbl_price_basis" to "Current regional reference average",
            "lbl_nutri_val" to "Nutri-Score Health",
            "lbl_health_security" to "Additive & Processing Alert",
            "lbl_nova_title" to "NOVA Processing",
            "lbl_additives_title" to "Chemical Additives",
            "lbl_comparison_matrix" to "Retailer Comparison Index",
            "lbl_taxonomies" to "Categories / Taxonomies",
            "lbl_op_alert" to "Scan Operation Alert",
            "lbl_history_title" to "Scan history",
            "btn_clear_text" to "Clear All",
            "lbl_empty_hist" to "No scan logs recorded locally on this device.",
            "cam_off_title" to "Camera Off",
            "cam_off_desc" to "Scan items to retrieve global prices, safety alerts, local retailer price matrices, and ingredients analysis.",
            "viewfinder_text" to "Center barcode inside frame",
            "deal_great" to "Great Deal",
            "deal_fair" to "Fair Price",
            "deal_overpriced" to "Overpriced Alert",
            "health_unprocessed" to "Unprocessed / Raw",
            "health_culinary" to "Mildly Processed",
            "health_processed2" to "Processed Food",
            "health_ultra" to "Ultra processed",
            "health_healthy" to "Healthy Profile",
            "health_moderate" to "Moderate Profile",
            "health_caution" to "Caution advised",
            "ready" to "Ready",
            "scanning" to "Scanning",
            "cameras_access_error" to "Camera Access Error",
            "none" to "None",
            "unknown_grade" to "Unknown",
            "no_data" to "No info",
            "not_found" to "Product not found. Ensure it is a valid grocery barcode."
        ),
        "sr" to mapOf(
            "hdr_desc" to "Skenirajte, poredite cene i kontrolišite kvalitet",
            "btn_scan_start" to "Pokreni skeniranje",
            "btn_scan_stop" to "Zaustavi",
            "lbl_scan_info" to "Štit za zaštitu potrošača",
            "lbl_scan_prompt" to "Kliknite na 'Pokreni skeniranje' i usmerite kameru u bar-kod",
            "lbl_loading_fetch" to "Pretraživanje baze podataka...",
            "lbl_price_val" to "Procenjena tržišna cena",
            "lbl_est" to "Proc.",
            "lbl_price_basis" to "Trenutna regionalna referentna vrednost",
            "lbl_nutri_val" to "Nutri-Score zdravstveni profil",
            "lbl_health_security" to "Upozorenje na aditive i preradu",
            "lbl_nova_title" to "NOVA nivo prerade",
            "lbl_additives_title" to "Hemijski aditivi",
            "lbl_comparison_matrix" to "Uporedni indeks maloprodajnih cena",
            "lbl_taxonomies" to "Kategorije / Taksonomija",
            "lbl_op_alert" to "Upozorenje pri skeniranju",
            "lbl_history_title" to "Istorija skeniranja",
            "btn_clear_text" to "Obriši sve",
            "lbl_empty_hist" to "Nema lokalno sačuvanih skeniranih proizvoda.",
            "cam_off_title" to "Kamera je isključena",
            "cam_off_desc" to "Skenirajte proizvode da biste odmah dobili njihove globalne cene, bezbednosne alerte, uporedne cene u prodavnicama i sastojke.",
            "viewfinder_text" to "Pozicionirajte bar-kod unutar okvira",
            "deal_great" to "Odlična cena",
            "deal_fair" to "Dobar odnos",
            "deal_overpriced" to "Precenjeno!",
            "health_unprocessed" to "Neprerađeno / Sveže",
            "health_culinary" to "Slabo prerađeno",
            "health_processed2" to "Prerađena hrana",
            "health_ultra" to "Ultra-prerađeno",
            "health_healthy" to "Zdrav sastav",
            "health_moderate" to "Srednji kvalitet",
            "health_caution" to "Oprez pri ishrani",
            "ready" to "Spreman",
            "scanning" to "Skeniranje",
            "cameras_access_error" to "Greška pri pristupu kameri",
            "none" to "Nema",
            "unknown_grade" to "Nepoznato",
            "no_data" to "Nema",
            "not_found" to "Proizvod nije pronađen. Proverite ispravnost bar-koda."
        )
    )

    fun get(key: String): String {
        val lang = getLanguageKey()
        return strings[lang]?.get(key) ?: strings["en"]?.get(key) ?: key
    }
}
