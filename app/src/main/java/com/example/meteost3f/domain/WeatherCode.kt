package com.example.meteost3f.domain

/** Mapping des codes WMO renvoyés par Open-Meteo vers une description et un emoji. */
object WeatherCode {
    private val entries = mapOf(
        0 to ("Ciel clair" to "☀️"),
        1 to ("Principalement clair" to "🌤️"),
        2 to ("Partiellement nuageux" to "⛅"),
        3 to ("Couvert" to "☁️"),
        45 to ("Brouillard" to "🌫️"),
        48 to ("Brouillard givrant" to "🌫️"),
        51 to ("Bruine légère" to "🌦️"),
        53 to ("Bruine modérée" to "🌦️"),
        55 to ("Bruine dense" to "🌦️"),
        56 to ("Bruine verglaçante" to "🌧️"),
        57 to ("Bruine verglaçante dense" to "🌧️"),
        61 to ("Pluie légère" to "🌧️"),
        63 to ("Pluie modérée" to "🌧️"),
        65 to ("Pluie forte" to "🌧️"),
        66 to ("Pluie verglaçante" to "🌧️"),
        67 to ("Pluie verglaçante forte" to "🌧️"),
        71 to ("Neige légère" to "🌨️"),
        73 to ("Neige modérée" to "🌨️"),
        75 to ("Neige forte" to "❄️"),
        77 to ("Grains de neige" to "❄️"),
        80 to ("Averses légères" to "🌦️"),
        81 to ("Averses modérées" to "🌧️"),
        82 to ("Averses violentes" to "⛈️"),
        85 to ("Averses de neige légères" to "🌨️"),
        86 to ("Averses de neige fortes" to "🌨️"),
        95 to ("Orage" to "⛈️"),
        96 to ("Orage avec grêle" to "⛈️"),
        99 to ("Orage violent avec grêle" to "⛈️"),
    )

    // Seuls les codes "ciel clair/peu nuageux" ont un rendu jour/nuit distinct dans les jeux
    // d'icônes météo usuels ; au-delà (pluie, neige, orage...), l'icône reste la même.
    private val nightEmoji = mapOf(
        0 to "🌙",
        1 to "🌙",
        2 to "🌙☁️",
    )

    fun describe(code: Int): String = entries[code]?.first ?: "Conditions inconnues"

    fun emoji(code: Int, isDay: Boolean = true): String {
        if (!isDay) nightEmoji[code]?.let { return it }
        return entries[code]?.second ?: "❓"
    }
}
