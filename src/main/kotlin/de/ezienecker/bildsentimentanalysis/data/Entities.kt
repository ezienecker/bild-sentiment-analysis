package de.ezienecker.bildsentimentanalysis.data

import org.jetbrains.exposed.sql.Table

/**
 * Datenbank Repraesentation der Entitaet Tabelle
 */
object Entities : Table() {
    private val id = integer("id").autoIncrement()
    val document = varchar("document", 20)
    val documentWithoutArticleAndDay = varchar("document_without_article_and_day", 20)
    val documentWithoutArticle = varchar("document_without_article", 20)
    val entity = varchar("entity", 500)
    val confidenceScore = double("confidence_score")

    override val primaryKey = PrimaryKey(id, name = "PK_Entity_ID")
}

/**
 * Data Transfer Object Repraesentation einer Entitaet
 *
 * @property document Merkmal um das Objekt eindeutig zu identifizieren
 * @property entity Verknüpfter Entitaetstext
 * @property confidenceScore Konfidenzwerte die das Vertrauensniveau über die Richtigkeit der erkannten Entitaet angibt
 */
data class EntityDto(val document: Document, val entity: String, val confidenceScore: Double)
