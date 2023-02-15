package de.ezienecker.bildsentimentanalysis.parser

import de.ezienecker.bildsentimentanalysis.data.ArticleDto
import de.ezienecker.bildsentimentanalysis.data.Document
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File

/**
 * Interface fuer Artikel Parser Methoden
 */
interface ArticleParser {

    /**
     * Methode die eine Datei die einen Artikel repraesentiert zu einen Artikel Dto umwandelt
     *
     * @return Gibt ein Artikel Data Transfer Objekt zurueck
     */
    fun parse(file: File): ArticleDto
}

/**
 * Implementierung des Artikel Parser Interface
 */
class BildArticleParser : ArticleParser {

    override fun parse(file: File): ArticleDto {
        val body = Jsoup
            .parse(file)
            .body()

        val title = parseTitle(body)
        val article = parseArticle(body)
        val author = parseAuthor(body)
        val dateTime = parseDateTime(body)

        val document = extractDocumentFromFilePath(file)

        return ArticleDto(document = document, title = title, article = article, dateTime = dateTime, author = author)
    }

    /**
     * Methode um den Titel eines Artikels Dokuments zu extrahieren
     *
     * @return Gibt den Artikel Titel als String zurueck
     */
    private fun parseTitle(body: Element): String = body
        .getElementsByClass("article-header__headline")
        .text()

    /**
     * Methode um den Hauptteil eines Artikels zu extrahieren
     *
     * @return Gibt den Artikel Inhalt als String zurueck
     */
    private fun parseArticle(body: Element): String {
        val article = body.getElementsByClass("article-body")

        article.select("aside.related-topics").forEach { topics -> topics.remove() }

        return article.text()
    }

    /**
     * Methode um den Author eines Artikels Dokuments zu extrahieren
     *
     * @return Gibt den Author des Artikels als String zurueck
     */
    private fun parseAuthor(body: Element): String =
        getTextByCSSQuery(body, "span.author__name").ifEmpty { "No Author" }

    /**
     * Methode um das Veraeffentlichungsdatum eines Artikels Dokuments zu extrahieren
     *
     * @return Gibt das Veraeffentlichungsdatum des Artikels als String zurueck
     */
    private fun parseDateTime(body: Element): String {
        val dateTime = getTextByCSSQuery(body, "time.datetime")
        return if (dateTime.length > 22) {
            dateTime.substring(0, 22)
        } else {
            dateTime
        }
    }

    /**
     * Extrahiert den Datumsteil aus den Dateipfad
     *
     * @return Gibt den Datumsteil des Dateipfad als String zurueck
     */
    private fun extractDocumentFromFilePath(file: File): Document =
        "${file.path.substringAfter("bild/").substringBefore("/articles")}/${file.nameWithoutExtension}"

    /**
     * Methode um mit Hilfe einer CSS-Query Teile aus einem HTML-Element zu extrahieren
     *
     * @return Gibt den mittels der CSS-Query selektieren Text als String zurueck
     */
    private fun getTextByCSSQuery(body: Element, cssQuery: String): String = body
        .select(cssQuery)
        .text()

}
