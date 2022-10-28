package data.database.shopWorkers

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object ShopWorkersDB : Table("shop_workers") {
    private val workerId = ShopWorkersDB.uuid("worker_id")
    private val login = ShopWorkersDB.varchar("login", 25)
    private val password = ShopWorkersDB.varchar("password", 25)
    private val shop = ShopWorkersDB.varchar("shop", 4)
    private val ownerTgId = ShopWorkersDB.varchar("owner_tg_id", 20)
    private val isActive = ShopWorkersDB.bool("is_active")
    private val shopOpen = ShopWorkersDB.varchar("shop_open", 2)
    private val shopClose = ShopWorkersDB.varchar("shop_close", 2)
    private val telegramChatId = ShopWorkersDB.varchar("chat_id", 20)

    fun insert(shopWorkersDTO: ShopWorkersDTO) {
        transaction {
            ShopWorkersDB.insert {
                it[workerId] = shopWorkersDTO.workerId
                it[login] = shopWorkersDTO.login
                it[password] = shopWorkersDTO.password
                it[shop] = shopWorkersDTO.shop
                it[ownerTgId] = shopWorkersDTO.ownerTgId.toString()
                it[isActive] = shopWorkersDTO.isActive
                it[shopOpen] = shopWorkersDTO.shopOpen.toString()
                it[shopClose] = shopWorkersDTO.shopClose.toString()
                it[telegramChatId] = shopWorkersDTO.telegramChatId.toString()
            }
        }
    }

    fun getAll(): List<ShopWorkersDTO> {
        return try {
            transaction {
//                addLogger(StdOutSqlLogger)
                ShopWorkersDB.selectAll().toList().map {
                    ShopWorkersDTO(
                        workerId = it[workerId],
                        login = it[login],
                        password = it[password],
                        shop = it[shop],
                        ownerTgId = it[ownerTgId].toLong(),
                        isActive = it[isActive],
                        shopOpen = it[shopOpen].toInt(),
                        shopClose = it[shopClose].toInt(),
                        telegramChatId = it[telegramChatId].toLong()
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}