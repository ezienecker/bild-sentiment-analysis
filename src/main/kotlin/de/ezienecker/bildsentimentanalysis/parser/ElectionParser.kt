package de.ezienecker.bildsentimentanalysis.parser

import de.ezienecker.bildsentimentanalysis.data.ElectionDto
import java.text.NumberFormat
import java.text.ParseException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Implementierung des Umfrage Parser. Damit können Umfragewerte von der Forschungsgruppe Wahlen kommend geparsed werden.
 */
class ElectionParser {

    companion object {
        val inputDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val outputYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val format: NumberFormat = NumberFormat.getInstance(Locale.GERMAN)
    }

    /**
     * Methode die eine CSV Datei die ein oder mehrere Umfragewerte enthaelt zu einer Liste von Election Data Transfer Objects umwandelt
     *
     * @param path Pfad zur Umfrage von Forschungsgruppe Wahlen als CSV Datei vorliegend
     * @return Gibt eine Liste von Election Data Transfer Objekte zurueck
     */
    fun parse(path: String): List<ElectionDto> {
        return ElectionParser::class.java.getResourceAsStream(path)?.run {
            val reader = this.bufferedReader()
            @Suppress("UNUSED_VARIABLE") val header = reader.readLine()

            reader.lineSequence()
                .filter { it.isNotBlank() }
                .map {
                    val (inputDate, cdu, spd, fdp, gruene, linke, afd, _, others) = it.split(
                        ';',
                        ignoreCase = false
                    )

                    val date = parseDate(inputDate)

                    listOf(
                        ElectionDto(date = date, party = "AfD", result = parseToDouble(afd)),
                        ElectionDto(
                            date = date,
                            party = "Bündnis 90/Die Grünen",
                            result = parseToDouble(gruene)
                        ),
                        ElectionDto(date = date, party = "CDU", result = parseToDouble(cdu)),
                        ElectionDto(date = date, party = "Die Linken", result = parseToDouble(linke)),
                        ElectionDto(date = date, party = "FDP", result = parseToDouble(fdp)),
                        ElectionDto(date = date, party = "SPD", result = parseToDouble(spd)),
                        ElectionDto(date = date, party = "Andere", result = parseToDouble(others))
                    )
                }.flatten().toList()
        } ?: throw IllegalArgumentException("Could not access resource $path")
    }

    /**
     * Wandelt den uebergebenen [String] in ein Datum um, um es anschließend in ein vordefiniertes, einheitliches Datum als [String] umzuwandeln
     *
     * @return Gibt Ein Datum als [String] in einem vordefinierten, einheitlich Format zurueck
     */
    private fun parseDate(element: String): String =
        element.let {
            LocalDate
                .parse(it, inputDateFormatter)
                .format(outputYearFormatter)
        }

    /**
     * Wandelt den uebergebenen String in [Doubel] um
     *
     * @return Gibt den uebergebenen String als [Doubel] zurueck
     */
    private fun parseToDouble(text: String): Double =
        try {
            format.parse(text.replace("\"", "").trim()).toDouble()
        } catch (exc: ParseException) {
            0.0
        }
}

operator fun <T> List<T>.component6(): T = get(5)

operator fun <T> List<T>.component7(): T = get(6)

operator fun <T> List<T>.component8(): T = get(7)

operator fun <T> List<T>.component9(): T = get(8)
