package de.ezienecker.bildsentimentanalysis.data

import org.jetbrains.exposed.sql.Table

/**
 * Datenbank Repraesentation der Wahl bzw. Umfragewert Tabelle
 */
object Elections : Table() {
    private val id = integer("id").autoIncrement()
    val document = varchar("document_without_article", 20)
    val party = varchar("party", 50)
    val result = double("result")

    override val primaryKey = PrimaryKey(id, name = "PK_Election_Politbarometer_ID")
}

/**
 * Data Transfer Object Repraesentation eines Wahl bzw. Umfragewert
 *
 * @property id Automatisch generiertes Merkmal um das Objekt eindeutig zu identifizieren
 * @property date Umfragedatum
 * @property party Name der Partei
 * @property result Umfragewert in Prozent
 */
data class ElectionDto(
    val id: Int? = null,
    val date: String,
    val party: String,
    val result: Double
)
