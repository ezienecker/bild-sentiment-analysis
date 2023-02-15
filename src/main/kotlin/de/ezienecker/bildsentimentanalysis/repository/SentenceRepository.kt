package de.ezienecker.bildsentimentanalysis.repository

import de.ezienecker.bildsentimentanalysis.data.SentenceDto
import de.ezienecker.bildsentimentanalysis.data.Sentences
import de.ezienecker.bildsentimentanalysis.data.Document
import de.ezienecker.bildsentimentanalysis.data.withoutArticle
import de.ezienecker.bildsentimentanalysis.data.withoutArticleAndDay
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Repository zur Verwaltung von Saetzen
 */
class SentenceRepository {

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Sentences)
        }
    }

    /**
     * Teilt die uebergebene Liste in kleinere Listen auf und speichert sie als Batch in die Datenbank
     *
     * @param sentences Liste der zu speichernden Saetze
     */
    fun batchInsert(sentences: List<SentenceDto>) {
        transaction {
            addLogger(StdOutSqlLogger)

            sentences
                .chunked(1000)
                .forEach { chunkedArticles ->
                    Sentences.batchInsert(
                        chunkedArticles,
                        shouldReturnGeneratedValues = false
                    ) { (document: Document, sentiment: String, positiveScore: Double, neutralScore: Double, negativeScore: Double) ->
                        this[Sentences.document] = document
                        this[Sentences.documentWithoutArticleAndDay] = document.withoutArticleAndDay()
                        this[Sentences.documentWithoutArticle] = document.withoutArticle()
                        this[Sentences.sentiment] = sentiment
                        this[Sentences.positiveScore] = positiveScore
                        this[Sentences.neutralScore] = neutralScore
                        this[Sentences.negativeScore] = negativeScore
                    }
                }
        }
    }
}
