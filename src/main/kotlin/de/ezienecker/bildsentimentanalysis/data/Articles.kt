package de.ezienecker.bildsentimentanalysis.data

import org.jetbrains.exposed.sql.Table

/**
 * Datenbank Repraesentation der Artikel Tabelle
 */
object Articles : Table() {
    val id = integer("id").autoIncrement()
    val document = varchar("document", 20)
    val documentWithoutArticle = varchar("document_without_article", 20)
    val documentWithoutArticleAndDay = varchar("document_without_article_and_day", 20)
    val title = varchar("title", 250)
    val article = text("article")
    val dateTime = varchar("date_time", 50)
    val author = varchar("author", 500)
    val sentiment = varchar("sentiment", 20).nullable()
    val calculatedSentiment = varchar("calculated_sentiment", 20).nullable()
    val positiveScore = double("positive_score").nullable()
    val neutralScore = double("neutral_score").nullable()
    val negativeScore = double("negative_score").nullable()
    val positiveScorePercentage = double("positive_score_percentage").nullable()
    val neutralScorePercentage = double("neutral_score_percentage").nullable()
    val negativeScorePercentage = double("negative_score_percentage").nullable()

    @Suppress("unused")
    override val primaryKey = PrimaryKey(id, name = "PK_Article_ID")

}

/**
 * Data Transfer Object Repraesentation eines Artikels
 *
 * @property id Automatisch generiertes Merkmal um das Objekt eindeutig zu identifizieren
 * @property document Merkmal um das Objekt eindeutig zu identifizieren
 * @property title Titel des Artikels
 * @property article Inhalt des Artikels
 * @property dateTime Veroeffentlichungsdatum des Artikels
 * @property author Author des Artikels
 * @property sentiment Vorhergesagte Stimmung des Artikels
 * @property positiveScore Positiver Sentiment-Konfidenzwerte auf Dokumentenebene zwischen 0 und 1
 * @property neutralScore Neutraler Sentiment-Konfidenzwerte auf Dokumentenebene zwischen 0 und 1
 * @property negativeScore Negativer Sentiment-Konfidenzwerte auf Dokumentenebene zwischen 0 und 1
 * @property positiveScorePercentage Prozentualer positiver Sentiment-Konfidenzwerte auf Dokumentenebene
 * @property neutralScorePercentage Prozentualer neutraler Sentiment-Konfidenzwerte auf Dokumentenebene
 * @property negativeScorePercentage Prozentualer negativer Sentiment-Konfidenzwerte auf Dokumentenebene
 */
data class ArticleDto(
    val id: Int? = null,
    val document: String,
    val title: String,
    val article: String,
    val dateTime: String,
    val author: String,
    var sentiment: String? = null,
    val positiveScore: Double? = null,
    val neutralScore: Double? = null,
    val negativeScore: Double? = null,
    val positiveScorePercentage: Double? = null,
    val neutralScorePercentage: Double? = null,
    val negativeScorePercentage: Double? = null
)
