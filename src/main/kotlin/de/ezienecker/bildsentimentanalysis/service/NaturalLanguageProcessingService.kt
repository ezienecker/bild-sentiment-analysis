package de.ezienecker.bildsentimentanalysis.service

import com.azure.ai.textanalytics.TextAnalyticsClient
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder
import com.azure.ai.textanalytics.models.*
import com.azure.ai.textanalytics.util.AnalyzeActionsResultPagedIterable
import com.azure.core.credential.AzureKeyCredential
import com.azure.core.util.Context
import com.azure.core.util.IterableStream
import com.azure.core.util.polling.SyncPoller
import de.ezienecker.bildsentimentanalysis.data.ArticleDto
import de.ezienecker.bildsentimentanalysis.data.EntityDto
import de.ezienecker.bildsentimentanalysis.data.SentenceDto
import de.ezienecker.bildsentimentanalysis.repository.ArticleRepository
import de.ezienecker.bildsentimentanalysis.repository.EntityRepository
import de.ezienecker.bildsentimentanalysis.repository.SentenceRepository
import reactor.kotlin.core.publisher.toMono

interface NaturalLanguageProcessingService {

    /**
     * Methode die den uebergebenen Artikel mit Hilfe von Natural Language Processing analysiert
     */
    fun analyzeArticle(article: ArticleDto)
}

class NaturalLanguageProcessingServiceImpl(
    private val articleRepository: ArticleRepository,
    private val sentenceRepository: SentenceRepository,
    private val entityRepository: EntityRepository
) : NaturalLanguageProcessingService {

    private var client: TextAnalyticsClient = TextAnalyticsClientBuilder()
        .credential(AzureKeyCredential("<key>"))
        .endpoint("<endpoint>")
        .defaultLanguage("de")
        .buildClient()

    override fun analyzeArticle(article: ArticleDto) {
        val documents = article
            .article
            .chunked(5_120)
            .mapIndexed { index, document -> TextDocumentInput("$index", document) }

        val textAnalyticsActions: TextAnalyticsActions = TextAnalyticsActions()
            .setDisplayName("{tasks_display_name}")
            .setAnalyzeSentimentActions(AnalyzeSentimentAction().setIncludeOpinionMining(true))
            .setRecognizeLinkedEntitiesActions(RecognizeLinkedEntitiesAction())

        client.beginAnalyzeActions(
            documents, textAnalyticsActions,
            AnalyzeActionsOptions().setIncludeStatistics(true), Context.NONE
        ).toMono().subscribe(
            { value -> processResult(value, article) },
            { error -> println("Error: $error, Could not process article with ID: ${article.id}") },
            { println("Successfully analyzed article with ID: ${article.id}") }
        )
    }

    /**
     * Verarbeitung des Gesamt-Ergebnis Menge
     *
     * @param value Wert der das Ergebnis der Stimmungsanalysen und Entitaetsanalyse enthaelt
     * @param article Artikel der der Stimmungsanalysen unterzogen wurde
     */
    private fun processResult(
        value: SyncPoller<AnalyzeActionsOperationDetail, AnalyzeActionsResultPagedIterable>,
        article: ArticleDto
    ) {
        value.finalResult.forEach { analyzeActionsResult ->
            processResultSentimentResults(
                analyzeActionsResult.analyzeSentimentResults,
                article
            )
            processResultRecognizeEntitiesResults(
                analyzeActionsResult.recognizeLinkedEntitiesResults,
                article.document
            )
        }
    }

    /**
     * Methode die das Ergebnis der Stimmungsanalyse verarbeitet. Das Ergebnis exrahiert und in die Datenbank speichert
     *
     * @param value Wert der das Ergebnis der Stimmungsanalysen enthaelt
     * @param article Artikel der der Stimmungsanalysen unterzogen wurde
     */
    private fun processResultSentimentResults(
        value: IterableStream<AnalyzeSentimentActionResult>,
        article: ArticleDto
    ) {
        value.forEach { result ->
            result.documentsResults.forEach { analyzeResult ->

                val documentSentiment = analyzeResult.documentSentiment
                val scores: SentimentConfidenceScores = documentSentiment.confidenceScores

                if (isFirstResult(analyzeResult)) {
                    articleRepository.update(
                        article.apply {
                            sentiment = documentSentiment.sentiment.toString()
                        }
                    )
                } else {
                    articleRepository.insert(
                        ArticleDto(
                            null,
                            article.document,
                            article.title,
                            article.article,
                            article.dateTime,
                            article.author,
                            documentSentiment.sentiment.toString(),
                            scores.positive,
                            scores.neutral,
                            scores.negative
                        )
                    )
                }

                val sentences = documentSentiment.sentences.map { sentenceSentiment ->
                    val sentenceScores: SentimentConfidenceScores = sentenceSentiment.confidenceScores
                    SentenceDto(
                        article.document,
                        sentenceSentiment.sentiment.toString(),
                        sentenceScores.positive,
                        sentenceScores.neutral,
                        sentenceScores.negative
                    )
                }

                sentenceRepository.batchInsert(sentences)
            }
        }
    }

    /**
     * Methode die ermittelt ob es sich bei den uebergeben Ergebnis um das erste Ergebnis handelt
     *
     * @param analyzeResult Wert der das Ergebnis der Stimmungsanalysen enthaelt
     * @return true wenn es sich um den ersten Teil des Ergebnis der Stimmungsanalyse handelt, andernfalls false
     */
    private fun isFirstResult(analyzeResult: AnalyzeSentimentResult) = analyzeResult.id == "0"

    /**
     * Methode die das Ergebnis der Entitaetsanalyse verarbeitet. Das Ergebnis exrahiert und in die Datenbank speichert
     *
     * @param value Wert der das Ergebnis der Entitaetsanalyse enthaelt
     * @param article Artikel der der Entitaetsanalyse unterzogen wurde
     */
    private fun processResultRecognizeEntitiesResults(
        value: IterableStream<RecognizeLinkedEntitiesActionResult>,
        document: String
    ) {
        value.forEach { linkedEntitiesResult ->
            linkedEntitiesResult.documentsResults.forEach { linkedEntities ->
                linkedEntities.entities.forEach { linkedEntity ->
                    linkedEntity.matches.forEach { entityMatch ->
                        entityRepository.insert(EntityDto(document, entityMatch.text, entityMatch.confidenceScore))
                    }
                }
            }
        }
    }
}
