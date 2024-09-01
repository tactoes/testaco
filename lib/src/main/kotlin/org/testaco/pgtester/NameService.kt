package org.testaco.pgtester

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val INSERT_NAME = "INSERT INTO names (name) VALUES (:name) RETURNING *"
private const val INSERT_ALIASES = "INSERT INTO aliases (name_id, alias) VALUES (:name_id, :alias) RETURNING *"

@Service
class NameService @Autowired constructor(val db: NamedParameterJdbcTemplate) {
    @Transactional
    @NoAuthorization
    fun create(name: Name): Name {
        val retvalue = db.query(
            INSERT_NAME,
            mapOf("id" to name.name),
        ) { rs, _ -> Name(rs.getLong("id"), rs.getString("name"), listOf()) }.first()
        val aliases = name.aliases.flatMap {
            db.query(
                INSERT_ALIASES,
                mapOf(
                    "name_id" to retvalue.id,
                    "alias" to it.alias,
                ),
            ) { rs, _ -> Alias(rs.getLong("id"), rs.getString("alias")) }
        }
        return retvalue.copy(aliases = aliases)
    }

    @Transactional
    @NoAuthorization
    fun delete(nameId: Long): Int {
        db.query(
            "DELETE FROM aliases WHERE name_id = :id",
            mapOf("id" to nameId),
        ) { _, _ -> }
        return db.query(
            "DELETE FROM names WHERE id = :id",
            mapOf("id" to nameId),
        ) { _, rowNum -> rowNum }
            .last()
    }
}
