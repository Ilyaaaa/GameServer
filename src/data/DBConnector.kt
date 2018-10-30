package data

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.TransactionManager
import data.DBConnector.Players.default
import java.sql.Connection

object DBConnector {
    private const val DB_NAME = "dataBase.sql"
    private const val DRIVER = "org.sqlite.JDBC"
    private const val URL = "jdbc:sqlite:$DB_NAME"

    fun connect() {
        Database.connect(URL, DRIVER)
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction { create(Players, MapObjects) }
    }

    object Players: Table() {
        val id = long("id").autoIncrement().primaryKey()
        val nickname = varchar("nick", 32).uniqueIndex()
        val email = varchar("email", 64).uniqueIndex()
        val x = float("x").default(100F)
        val y = float("y").default(100F)
        val position = varchar("position", 2).default("D")
        val hp = integer("hp").default(100)
        val password = varchar("pass", 64)
    }

    object MapObjects: Table() {
        val id = long("id").autoIncrement().primaryKey()
        val name = varchar("name", 64)
        val x = float("x")
        val y = float("y")
        val type = varchar("type", 16)
        val params = text("params")
    }
}