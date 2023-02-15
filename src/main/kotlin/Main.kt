import de.ezienecker.bildsentimentanalysis.parser.ArticleParser
import de.ezienecker.bildsentimentanalysis.parser.BildArticleParser
import de.ezienecker.bildsentimentanalysis.parser.ElectionParser
import de.ezienecker.bildsentimentanalysis.repository.ArticleRepository
import de.ezienecker.bildsentimentanalysis.repository.ElectionRepository
import de.ezienecker.bildsentimentanalysis.repository.EntityRepository
import de.ezienecker.bildsentimentanalysis.repository.SentenceRepository
import de.ezienecker.bildsentimentanalysis.service.*
import org.jetbrains.exposed.sql.Database
import kotlin.streams.toList

fun main(args: Array<String>) {

    connectToDatabase()

    val articleRepository = articleRepository()
    val articleService = articleService(articleRepository, articleParser())

    val naturalLanguageProcessingService =
        naturalLanguageProcessingService(articleRepository, sentenceRepository(), entityRepository())

    val articles = articleService
        // Selektieren der für die analyse relevanten Dateien
        .getRelevantFilesFromDirectory("<path>")
        // Erzeugen eines parallelen Stream auf Basis der zurvor erzeugten Collection
        .parallelStream()
        // Datei in Artikel Dto umwandeln
        .map { file -> articleService.parse(file) }
        // Ergebnis zu einer Liste umwandeln
        .toList()

    articleService.batchInsert(articles)

    // Alle Artikel aus der Datenbank selektieren und mittels einer For Each Schleife verarbeiten
    articleRepository.findAll().forEach { article ->
        naturalLanguageProcessingService.analyzeArticle(article)
    }

    articleService.postProcessCalculatedSentiment()

    val electionService = electionService(electionRepository(), electionParser())
    val elections = electionService.parse("dawum.de_Bundestag_Forschungsgruppe_Wahlen_2022-12-16.csv")

    electionService.batchInsert(elections)
}

/**
 * Verbindung zur Datenbank herstellen
 *
 * @return Aktive Verbindung zur Datenbank
 */
fun connectToDatabase(): Database =
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/postgres?reWriteBatchedInserts=true",
        driver = "org.postgresql.Driver", user = "<user>", password = "<password>"
    )

/**
 * Diese Methode erzeugt das Artikel-Repository
 *
 * @return Gibt eine Artikel-Repository Instanz zurueck
 */
fun articleRepository(): ArticleRepository = ArticleRepository()

/**
 * Diese Methode erzeugt das Satz-Repository
 *
 * @return Gibt eine Satz-Repository Instanz zurueck
 */
fun sentenceRepository(): SentenceRepository = SentenceRepository()

/**
 * Diese Methode erzeugt das Entity-Repository
 *
 * @return Gibt eine Entitaet-Repository Instanz zurueck
 */
fun entityRepository(): EntityRepository = EntityRepository()

/**
 * Diese Methode erzeugt ein Artikel-Parser
 *
 * @return Gibt eine Artikel-Parser Instanz zurueck
 */
fun articleParser(): ArticleParser = BildArticleParser()

/**
 * Diese Methode erzeugt den Artikel-Service
 *
 * @param articleRepository
 * @param articleParser
 * @return Gibt eine Artikel-Service Instanz zurueck
 */
fun articleService(articleRepository: ArticleRepository, articleParser: ArticleParser): ArticleService =
    ArticleServiceImpl(articleRepository, articleParser)

/**
 * Diese Methode erzeugt den Natural-Language-Processing-Service
 *
 * @param articleRepository Artikel Repository was eine Schnittstell zu den zu verarbeitenden Artikel bereitstellt
 * @param sentenceRepository Satz Repository was eine Schnittstell zu den zu verarbeitenden Saetzen bereitstellt
 * @param entityRepository Entitaet Repository was eine Schnittstell zu den zu verarbeitenden Entitaeten bereitstellt
 * @return Gibt eine Natural-Language-Processing-Service Instanz zurueck
 */
fun naturalLanguageProcessingService(
    articleRepository: ArticleRepository,
    sentenceRepository: SentenceRepository,
    entityRepository: EntityRepository
): NaturalLanguageProcessingService =
    NaturalLanguageProcessingServiceImpl(articleRepository, sentenceRepository, entityRepository)

/**
 * Diese Methode erzeugt ein Umfragewerte-Parser
 * @return Gibt eine Umfragewerte-Parser Instanz zurueck
 */
fun electionParser(): ElectionParser = ElectionParser()

/**
 * Diese Methode erzeugt das Umfragewerte-Repository
 *
 * @return Gibt eine Umfragewerte-Repository Instanz zurueck
 */
fun electionRepository(): ElectionRepository = ElectionRepository()

/**
 * Diese Methode erzeugt den Umfragewerte-Service
 *
 * @param electionRepository Umfragewerte-Repository was Umfragewerte verarbeitet
 * @param electionParser Umfragewerte Parser der Umfragewerte entsprechend umwandelt
 * @return Gibt eine Umfragewerte-Service Instanz zurueck
 */
fun electionService(electionRepository: ElectionRepository, electionParser: ElectionParser): ElectionService =
    ElectionServiceImpl(electionRepository, electionParser)
