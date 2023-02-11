package data.database.shopParameters

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.Logging

object ShopParametersDB : Table("shop_parameters") {
    private val tag = this::class.java.simpleName
    private val shop = ShopParametersDB.varchar("shop", 4)
    private val serializedActiveOrders = ShopParametersDB.text("serialized_active_orders")
    private val currentInfoMsgId = ShopParametersDB.long("current_info_message_id")
    private val dayConfirmedCount = ShopParametersDB.integer("day_confirmed_count")

    fun upsert(shopParametersDTO: ShopParametersDTO) {
        try {
            transaction {
                addLogger(StdOutSqlLogger)
                val updatedRows = ShopParametersDB.update({ shop eq shopParametersDTO.shop }) {
                    it[serializedActiveOrders] = shopParametersDTO.serializedActiveOrders
                    it[currentInfoMsgId] = shopParametersDTO.currentInfoMsgId
                    it[dayConfirmedCount] = shopParametersDTO.dayConfirmedCount
                }

                if (updatedRows == 0) {
                    ShopParametersDB.insert {
                        it[shop] = shopParametersDTO.shop
                        it[serializedActiveOrders] = shopParametersDTO.serializedActiveOrders
                        it[currentInfoMsgId] = shopParametersDTO.currentInfoMsgId
                        it[dayConfirmedCount] = shopParametersDTO.dayConfirmedCount
                    }
                }
            }

        } catch (e: Exception) {
            Logging.e(tag, e.message.toString())
        }
    }

    fun updateDayConfirmedCount(shopPar: String, dayConfirmedCountPar: Int) {
        try {
            transaction {
                addLogger(StdOutSqlLogger)
                ShopParametersDB.update({ ShopParametersDB.shop.eq(shopPar) }) {
                    it[dayConfirmedCount] = dayConfirmedCountPar
                }
            }

        } catch (e: Exception) {
            Logging.e(tag, e.message.toString())
        }
    }

    fun getParametersByShop(shop: String): ShopParametersDTO? {
        return try {
            transaction {
                addLogger(StdOutSqlLogger)
                val parameters = ShopParametersDB.select { ShopParametersDB.shop.eq(shop) }.single()
                    ShopParametersDTO(
                        shop = shop,
                        serializedActiveOrders = parameters[serializedActiveOrders],
                        currentInfoMsgId = parameters[currentInfoMsgId],
                        dayConfirmedCount = parameters[dayConfirmedCount]
                    )

            }
        } catch (e: Exception) {
            Logging.e(tag, e.message.toString())
            return null
        }
    }
}