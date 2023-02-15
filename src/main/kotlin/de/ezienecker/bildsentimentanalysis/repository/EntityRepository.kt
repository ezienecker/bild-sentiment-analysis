package de.ezienecker.bildsentimentanalysis.repository

import de.ezienecker.bildsentimentanalysis.data.Entities
import de.ezienecker.bildsentimentanalysis.data.EntityDto
import de.ezienecker.bildsentimentanalysis.data.withoutArticle
import de.ezienecker.bildsentimentanalysis.data.withoutArticleAndDay
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Repository zur Verwaltung von Entitaeten
 */
class EntityRepository {

    init {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Entities)
        }
    }

    /**
     * Speichert das uebergebene Entitaet Dto in die Datenbank
     *
     * @param entityDto die zu speichernde Entitaet als Dto
     */
    fun insert(entityDto: EntityDto) {
        transaction {
            addLogger(StdOutSqlLogger)

            Entities.insert {
                it[document] = entityDto.document
                it[documentWithoutArticleAndDay] = entityDto.document.withoutArticleAndDay()
                it[documentWithoutArticle] = entityDto.document.withoutArticle()
                it[entity] = entityDto.entity
                it[confidenceScore] = entityDto.confidenceScore
            }
        }
    }
}
