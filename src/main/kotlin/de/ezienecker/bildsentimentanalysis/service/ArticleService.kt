package de.ezienecker.bildsentimentanalysis.service

import de.ezienecker.bildsentimentanalysis.data.ArticleDto
import de.ezienecker.bildsentimentanalysis.data.Articles
import de.ezienecker.bildsentimentanalysis.data.Sentiment.NEGATIVE_SENTIMENT
import de.ezienecker.bildsentimentanalysis.data.Sentiment.NEUTRAL_SENTIMENT
import de.ezienecker.bildsentimentanalysis.data.Sentiment.POSITIVE_SENTIMENT
import de.ezienecker.bildsentimentanalysis.parser.ArticleParser
import de.ezienecker.bildsentimentanalysis.repository.ArticleRepository
import java.io.File

interface ArticleService {

    /**
     * Selektiert relevante Dateien vom uebergebenen Pfad
     *
     * @return Liefert alle Dateien als Liste die einem oder mehrere Kriterien entsprechen
     */
    fun getRelevantFilesFromDirectory(path: String): List<File>

    /**
     * Methode die eine Datei Artikel Data Transfer Objects umwandelt
     *
     * @param file Datei die ein Artikel entspricht
     * @return Gibt ein Artikel umgewandelt als Artikel Data Transfer Objekte zurueck
     */
    fun parse(file: File): ArticleDto

    /**
     * Methode die die uebergeben Liste als Batch speichert
     *
     * @param articles Liste der zu speichernde Artikel
     */
    fun batchInsert(articles: List<ArticleDto>)

    /**
     * Nachbearbeitung der gelabelten Stimmung
     */
    fun postProcessCalculatedSentiment()
}

class ArticleServiceImpl(
    private val repository: ArticleRepository,
    private val articleParser: ArticleParser
) : ArticleService {

    override fun getRelevantFilesFromDirectory(path: String): List<File> = File(path).walk()
        .filter { file -> filterFileByCharacteristics(file) }
        .toList()

    override fun parse(file: File): ArticleDto = articleParser.parse(file)
    override fun batchInsert(articles: List<ArticleDto>) = repository.batchInsert(articles)

    override fun postProcessCalculatedSentiment() {
        repository.findAllSentencesWhoseDocumentsHaveMixedSentiment()
            .groupBy { it.first }
            .map {
                val positiveCount = getPositiveCountFromRow(it.value)
                val negativeCount = getNegativeCountFromRow(it.value)
                val sentiment = if (positiveCount > negativeCount) {
                    POSITIVE_SENTIMENT
                } else if (positiveCount < negativeCount) {
                    NEGATIVE_SENTIMENT
                } else {
                    NEUTRAL_SENTIMENT
                }
                Pair(it.key, sentiment)
            }.forEach { documentWithSentiment ->
                repository.updateCalculatedSentimentByCondition(
                    { Articles.document eq documentWithSentiment.first },
                    documentWithSentiment.second
                )
            }

        repository.findAllDocumentIdsBySentiment(POSITIVE_SENTIMENT).forEach {
            repository.updateCalculatedSentimentByCondition(
                { Articles.document eq it },
                POSITIVE_SENTIMENT
            )
        }

        repository.findAllDocumentIdsBySentiment(NEGATIVE_SENTIMENT).forEach {
            repository.updateCalculatedSentimentByCondition(
                { Articles.document eq it },
                NEGATIVE_SENTIMENT
            )
        }

        repository.findAllDocumentIdsBySentiment(NEUTRAL_SENTIMENT).forEach {
            repository.updateCalculatedSentimentByCondition(
                { Articles.document eq it },
                NEUTRAL_SENTIMENT
            )
        }
    }

    /**
     * Extrahiert die positive Stimmungsanzahl von der uebergebenen Zeile
     *
     * @param row Zeile von der die Stimmung extrahiert werden soll
     * @return Gibt die Anzahl der positiven Stimmung der Zeile als [Long] zurueck
     */
    private fun getPositiveCountFromRow(row: List<Triple<String, String, Long>>) =
        getCountFromRow(row, POSITIVE_SENTIMENT)

    /**
     * Extrahiert die negative Stimmungsanzahl von der uebergebenen Zeile
     *
     * @param row Zeile von der die Stimmung extrahiert werden soll
     * @return Gibt die Anzahl der negativen Stimmung der Zeile als [Long] zurueck
     */
    private fun getNegativeCountFromRow(row: List<Triple<String, String, Long>>) =
        getCountFromRow(row, NEGATIVE_SENTIMENT)


    /**
     * Extrahiert die Anzahl der den uebergeben String entspricht von der uebergebenen Zeile
     *
     * @param row Zeile von der die Stimmung extrahiert werden soll
     * @param sentiment String der die Zeile identifiziert der die Anzahl enthaelt
     * @return Gibt die Anzahl der negativen Stimmung der Zeile als [Long] zurueck
     */
    private fun getCountFromRow(row: List<Triple<String, String, Long>>, sentiment: String) =
        row.find { it.second == sentiment }!!.third

    /**
     * Prueft ob die uebergebene Datei keine versteckte Datei ist, es sich um eine Datei handelt und es eine html Datei ist
     * @param file Datei die geprueft werden soll
     *
     * @return true wenn es den Pruefkriterien entspricht, andernfalls false
     */
    private fun filterFileByCharacteristics(file: File): Boolean =
        !file.isHidden && file.isFile && file.extension.contains("html")
}
