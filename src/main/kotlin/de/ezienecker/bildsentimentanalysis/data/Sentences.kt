package de.ezienecker.bildsentimentanalysis.data

import org.jetbrains.exposed.sql.Table

/**
 * Datenbank Repraesentation der Satz Tabelle
 */
object Sentences : Table() {
    private val id = integer("id").autoIncrement()
    val document = varchar("document", 20)
    val documentWithoutArticleAndDay = varchar("document_without_article_and_day", 20)
    val documentWithoutArticle = varchar("document_without_article", 20)
    val sentiment = varchar("sentiment", 20)
    val positiveScore = double("positive_score")
    val neutralScore = double("neutral_score")
    val negativeScore = double("negative_score")

    override val primaryKey = PrimaryKey(id, name = "PK_Sentence_ID")
}

/**
 * Data Transfer Object Repraesentation eines Satz
 *
 * @property document Merkmal um das Objekt eindeutig zu identifizieren
 * @property sentiment Vorhergesagte Stimmung des Artikels
 * @property positiveScore Positiver Sentiment-Konfidenzwerte auf Dokumentenebene zwischen 0 und 1
 * @property neutralScore Neutraler Sentiment-Konfidenzwerte auf Dokumentenebene zwischen 0 und 1
 * @property negativeScore Negativer Sentiment-Konfidenzwerte auf Dokumentenebene zwischen 0 und 1
 */
data class SentenceDto(
    val document: Document,
    val sentiment: String,
    val positiveScore: Double,
    val neutralScore: Double,
    val negativeScore: Double
)
