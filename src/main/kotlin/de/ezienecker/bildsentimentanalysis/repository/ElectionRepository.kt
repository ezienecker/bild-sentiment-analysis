package de.ezienecker.bildsentimentanalysis.repository

import de.ezienecker.bildsentimentanalysis.data.ElectionDto
import de.ezienecker.bildsentimentanalysis.data.Elections
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Repository zur Verwaltung von Umfragewerten
 */
class ElectionRepository {

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Elections)
        }
    }

    /**
     * Teilt die uebergebene Liste in kleinere Listen auf und speichert sie als Batch in die Datenbank
     *
     * @param elections Liste der zu speichernde Umfragewerte
     */
    fun batchInsert(elections: List<ElectionDto>) {
        transaction {
            addLogger(StdOutSqlLogger)

            elections
                .chunked(1000)
                .forEach { chunkedElections ->
                    Elections.batchInsert(
                        chunkedElections,
                        shouldReturnGeneratedValues = false
                    ) { (_, date: String, party: String, result: Double) ->
                        this[Elections.document] = date
                        this[Elections.party] = party
                        this[Elections.result] = result
                    }
                }
        }
    }
}
