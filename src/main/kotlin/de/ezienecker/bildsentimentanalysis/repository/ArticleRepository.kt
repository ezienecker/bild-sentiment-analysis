package de.ezienecker.bildsentimentanalysis.repository

import de.ezienecker.bildsentimentanalysis.data.ArticleDto
import de.ezienecker.bildsentimentanalysis.data.Articles
import de.ezienecker.bildsentimentanalysis.data.Sentences
import de.ezienecker.bildsentimentanalysis.data.Document
import de.ezienecker.bildsentimentanalysis.data.withoutArticle
import de.ezienecker.bildsentimentanalysis.data.withoutArticleAndDay
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.RoundingMode

/**
 * Repository zur Verwaltung von Artikeln
 */
class ArticleRepository {

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Articles)
        }
    }


    /**
     * Liefert alle Artikel aus der Datenbank zurueck
     */
    fun findAll() = transaction {
        Articles.selectAll()
            .map { article ->
                ArticleDto(
                    article[Articles.id],
                    article[Articles.document],
                    article[Articles.title],
                    article[Articles.article],
                    article[Articles.dateTime],
                    article[Articles.author],
                    article[Articles.sentiment],
                    article[Articles.positiveScore],
                    article[Articles.neutralScore],
                    article[Articles.negativeScore],
                    article[Articles.positiveScorePercentage],
                    article[Articles.neutralScorePercentage],
                    article[Articles.negativeScorePercentage]
                )
            }
    }

    /**
     * Teilt die uebergeben Liste in kleinere Listen auf und speichert sie als Batch in die Datenbank
     *
     * @param articles Liste der zu speichernden Artikel
     */
    fun batchInsert(articles: List<ArticleDto>) {
        transaction {
            addLogger(StdOutSqlLogger)

            articles
                .chunked(1000)
                .forEach { chunkedArticles ->
                    Articles.batchInsert(
                        chunkedArticles,
                        shouldReturnGeneratedValues = false
                    ) { (_, document: Document, title: String, article: String, dateTime: String, author: String) ->
                        this[Articles.document] = document
                        this[Articles.documentWithoutArticleAndDay] = document.withoutArticleAndDay()
                        this[Articles.documentWithoutArticle] = document.withoutArticle()
                        this[Articles.title] = title
                        this[Articles.dateTime] = dateTime
                        this[Articles.author] = author
                        this[Articles.article] = article
                    }
                }
        }
    }

    /**
     * Speichert das uebergeben Artikel Dto in die Datenbank
     *
     * @param articleDto der zu speichernde Artikel als Dto
     */
    fun insert(articleDto: ArticleDto) {
        transaction {
            addLogger(StdOutSqlLogger)

            val (positivePercentage, neutralPercentage, negativePercentage) =
                calculateScorePercentage(articleDto.positiveScore, articleDto.neutralScore, articleDto.negativeScore)

            Articles.insert {
                it[document] = articleDto.document
                it[documentWithoutArticleAndDay] = articleDto.document.withoutArticleAndDay()
                it[documentWithoutArticle] = articleDto.document.withoutArticle()
                it[title] = articleDto.title
                it[dateTime] = articleDto.dateTime
                it[author] = articleDto.author
                it[article] = articleDto.article
                it[sentiment] = articleDto.sentiment
                it[positiveScore] = articleDto.positiveScore
                it[neutralScore] = articleDto.neutralScore
                it[negativeScore] = articleDto.negativeScore
                it[positiveScorePercentage] = positivePercentage
                it[neutralScorePercentage] = neutralPercentage
                it[negativeScorePercentage] = negativePercentage
            }
        }
    }

    /**
     * Aktualisiert ein Artikel mit dem uebergeben Artikel Dto in der Datenbank
     *
     * @param articleDto Zu aktualisierender Artikel als Dto
     */
    fun update(articleDto: ArticleDto) {
        transaction {
            addLogger(StdOutSqlLogger)

            val (positivePercentage, neutralPercentage, negativePercentage) =
                calculateScorePercentage(articleDto.positiveScore, articleDto.neutralScore, articleDto.negativeScore)

            Articles.update({ Articles.id eq articleDto.id!! }) {
                it[document] = articleDto.document
                it[documentWithoutArticleAndDay] = articleDto.document.withoutArticleAndDay()
                it[documentWithoutArticle] = articleDto.document.withoutArticle()
                it[title] = articleDto.title
                it[dateTime] = articleDto.dateTime
                it[author] = articleDto.author
                it[article] = articleDto.article
                it[sentiment] = articleDto.sentiment
                it[positiveScore] = articleDto.positiveScore
                it[neutralScore] = articleDto.neutralScore
                it[negativeScore] = articleDto.negativeScore
                it[positiveScorePercentage] = positivePercentage
                it[neutralScorePercentage] = neutralPercentage
                it[negativeScorePercentage] = negativePercentage
            }
        }
    }

    /**
     * Aktualisiert die berechnete Stimmung in einen Artikel nach einer Bedingung
     *
     * @param where Bedingung die bestimmt welcher Artikel aktualisiert werden soll
     * @param sentiment Berechnete Stimmung
     */
    fun updateCalculatedSentimentByCondition(where: (SqlExpressionBuilder.() -> Op<Boolean>), sentiment: String) {
        transaction {
            Articles.update(where) {
                it[calculatedSentiment] = sentiment
            }
        }
    }

    /**
     * Alle Saetze selektieren die eine mit 'mixed' gelabelte Stimmung haben
     *
     * @return Gibt eine Liste von folgenden Attributen zurueck: Document um einen Satz zu identifizieren,
     * Stimmung dieses Satzes und wie oft diese Stimmung innerhalb dieses Satzes vorkommt zurueck
     * die mit 'mixed' gelabelt wurden.
     */
    fun findAllSentencesWhoseDocumentsHaveMixedSentiment() =
        transaction {
            addLogger(StdOutSqlLogger)
            Join(
                Articles, Sentences,
                onColumn = Articles.document, otherColumn = Sentences.document,
                joinType = JoinType.INNER,
                additionalConstraint = { Articles.sentiment eq "mixed" }
            ).slice(Sentences.document, Sentences.sentiment, Sentences.sentiment.count())
                .selectAll()
                .groupBy(Sentences.document, Sentences.sentiment)
                .orderBy(Sentences.document to SortOrder.ASC)
                .map {
                    Triple(it[Sentences.document], it[Sentences.sentiment], it[Sentences.sentiment.count()])
                }
        }

    /**
     * Selektiert alle Artikel die der uebergebenen Stimmung entsprechen
     *
     * @return Gibt eine Liste von Strings zurueck die der uebergebenen Stimmung entsprechen
     */
    fun findAllDocumentIdsBySentiment(sentiment: String) = transaction {
        Articles.select { Articles.sentiment eq sentiment }
            .map {
                it[Articles.document]
            }
    }

    /**
     * Diese Methode berechnet den prozentualen Anteil der jeweiligen Stimmung bezogen auf das Gesamtergebnis.
     *
     * @return Gibt den prozentualen Anteil der jeweiligen Stimmung zurueck
     */
    private fun calculateScorePercentage(
        positiveScore: Double?,
        neutralScore: Double?,
        negativeScore: Double?
    ): Triple<Double, Double, Double> {
        if (positiveScore == null || neutralScore == null || negativeScore == null) {
            return Triple(0.0, 0.0, 0.0)
        }

        val totalScore = positiveScore + neutralScore + negativeScore

        val positivePercentage =
            ((positiveScore * 100) / totalScore).toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()
        val neutralPercentage =
            ((neutralScore * 100) / totalScore).toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()
        val negativePercentage =
            ((negativeScore * 100) / totalScore).toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()

        return Triple(positivePercentage, neutralPercentage, negativePercentage)
    }
}
