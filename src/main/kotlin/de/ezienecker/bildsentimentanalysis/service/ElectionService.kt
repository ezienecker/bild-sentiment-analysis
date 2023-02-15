package de.ezienecker.bildsentimentanalysis.service

import de.ezienecker.bildsentimentanalysis.data.ElectionDto
import de.ezienecker.bildsentimentanalysis.parser.ElectionParser
import de.ezienecker.bildsentimentanalysis.repository.ElectionRepository

interface ElectionService {

    /**
     * Methode die eine Datei die ein oder mehrere Umfragewerte enthaelt zu einer Liste von Election Data Transfer Objects umwandelt
     *
     * @param path Pfad zu den Umfragewerten
     * @return Gibt eine Liste von Election Data Transfer Objekte zurueck
     */
    fun parse(path: String): List<ElectionDto>

    /**
     * Methode die die uebergeben Liste als Batch speichert
     *
     * @param elections Liste der zu speichernde Umfragewerte
     */
    fun batchInsert(elections: List<ElectionDto>)
}

class ElectionServiceImpl(
    private val repository: ElectionRepository,
    private val electionParser: ElectionParser
) : ElectionService {

    override fun parse(path: String): List<ElectionDto> = electionParser.parse(path)

    override fun batchInsert(elections: List<ElectionDto>) = repository.batchInsert(elections)

}
