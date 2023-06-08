package data.database.botUsers

import dev.inmo.tgbotapi.types.Identifier
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.Logging

object BotUsersDB : Table("bot_users") {
    private val tag = this::class.java.simpleName
    private val login = BotUsersDB.varchar("ts_login", 25)
    private val password = BotUsersDB.varchar("ts_password", 40)
    private val shop = BotUsersDB.varchar("ts_shop", 4)
    private val telegramUserId = BotUsersDB.varchar("tg_user_id", 20)
    private val userRole = BotUsersDB.varchar("user_role", 20)
    private val sapFio = BotUsersDB.varchar("sap_fio", 100)
    private val sapPosition = BotUsersDB.varchar("sap_position", 40)
    private val sapId = BotUsersDB.varchar("sap_id", 20)

    fun insert(botUsersDTO: BotUsersDTO) {

        try {
            transaction {
                addLogger(StdOutSqlLogger)
                BotUsersDB.insert {
                    it[login] = botUsersDTO.login
                    it[password] = botUsersDTO.password
                    it[shop] = botUsersDTO.shop
                    it[telegramUserId] = botUsersDTO.telegramUserId.toString()
                    it[userRole] = botUsersDTO.userRole
                    it[sapFio] = botUsersDTO.sapFio ?: ""
                    it[sapPosition] = botUsersDTO.sapPosition ?: ""
                    it[sapId] = botUsersDTO.sapId ?: ""
                }
            }

        } catch (e: ExposedSQLException) {
            // 23505 ERROR:  duplicate key violates unique constraint
            if (e.sqlState == "23505") {
                transaction {
                    addLogger(StdOutSqlLogger)
                    BotUsersDB.update({ BotUsersDB.telegramUserId eq botUsersDTO.telegramUserId.toString() }) {
                        it[login] = botUsersDTO.login
                        it[password] = botUsersDTO.password
                        it[shop] = botUsersDTO.shop
//                        it[telegramUserId] = botUsersDTO.telegramUserId.toString()
                        it[userRole] = botUsersDTO.userRole
                        it[sapFio] = botUsersDTO.sapFio ?: ""
                        it[sapPosition] = botUsersDTO.sapPosition ?: ""
                        it[sapId] = botUsersDTO.sapId ?: ""
                    }
                }
            } else Logging.e(tag, e.message.toString())
        }
    }

    fun getAll(): List<BotUsersDTO> {
        return try {
            transaction {
                addLogger(StdOutSqlLogger)
                BotUsersDB.selectAll().toList().map {
                    BotUsersDTO(
                        login = it[login],
                        password = it[password],
                        shop = it[shop],
                        telegramUserId = it[telegramUserId].toLong(),
                        userRole = it[userRole],
                        sapFio = it[sapFio],
                        sapPosition = it[sapPosition],
                        sapId = it[sapId]
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getUserBy(userId: Identifier): BotUsersDTO? {
        return try {
            transaction {
                addLogger(StdOutSqlLogger)
                val user = BotUsersDB.select { BotUsersDB.telegramUserId eq userId.toString() }.single()
                BotUsersDTO(
                    login = user[login],
                    password = user[password],
                    shop = user[shop],
                    telegramUserId = user[telegramUserId].toLong(),
                    userRole = user[userRole],
                    sapFio = user[sapFio],
                    sapPosition = user[sapPosition],
                    sapId = user[sapId]
                )
            }
        } catch (e: Exception) {
            Logging.e(tag, e.message.toString())
            null
        }
    }
}