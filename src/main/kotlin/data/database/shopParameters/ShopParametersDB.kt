package data.database.shopParameters

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.Logging

object ShopParametersDB : Table("shop_parameters") {
    private val tag = this::class.java.simpleName
    private val shop = ShopParametersDB.varchar("shop", 4)
    private val serializedActiveOrders = ShopParametersDB.text("serialized_active_orders")
    private val currentInfoMsgId = ShopParametersDB.long("current_info_message_id")
    private val dayRecievedCount = ShopParametersDB.integer("day_recieved_count")
    private val dayConfirmedCount = ShopParametersDB.integer("day_confirmed_count")
    private val serializedDayConfirmedByEmployee = ShopParametersDB.text("serialized_day_confirmed_by_employee")

    fun upsert(shopParametersDTO: ShopParametersDTO) {
        try {
            transaction {
                addLogger(StdOutSqlLogger)
                val updatedRows = ShopParametersDB.update({ shop eq shopParametersDTO.shop }) {
                    it[serializedActiveOrders] = shopParametersDTO.serializedActiveOrders
                    it[currentInfoMsgId] = shopParametersDTO.currentInfoMsgId
                    it[dayRecievedCount] = shopParametersDTO.dayRecievedCount
                    it[dayConfirmedCount] = shopParametersDTO.dayConfirmedCount
                    it[serializedDayConfirmedByEmployee] = shopParametersDTO.serializedDayConfirmedByEmployee
                }

                if (updatedRows == 0) {
                    ShopParametersDB.insert {
                        it[shop] = shopParametersDTO.shop
                        it[serializedActiveOrders] = shopParametersDTO.serializedActiveOrders
                        it[currentInfoMsgId] = shopParametersDTO.currentInfoMsgId
                        it[dayRecievedCount] = shopParametersDTO.dayRecievedCount
                        it[dayConfirmedCount] = shopParametersDTO.dayConfirmedCount
                        it[serializedDayConfirmedByEmployee] = shopParametersDTO.serializedDayConfirmedByEmployee
                    }
                }
            }

        } catch (e: Exception) {
            Logging.e(tag, e.message.toString())
        }
    }

    fun updateDayCounts(
        shopPar: String,
        dayRecievedCountPar: Int,
        dayConfirmedCountPar: Int,
        serializedDayConfirmedByEmployeePar: String
    ) {
        try {
            transaction {
                addLogger(StdOutSqlLogger)
                ShopParametersDB.update({ ShopParametersDB.shop.eq(shopPar) }) {
                    it[dayRecievedCount] = dayRecievedCountPar
                    it[dayConfirmedCount] = dayConfirmedCountPar
                    it[serializedDayConfirmedByEmployee] = serializedDayConfirmedByEmployeePar
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
                        dayRecievedCount = parameters[dayRecievedCount],
                        dayConfirmedCount = parameters[dayConfirmedCount],
                        serializedDayConfirmedByEmployee = parameters[serializedDayConfirmedByEmployee]
                    )

            }
        } catch (e: Exception) {
            Logging.e(tag, e.message.toString())
            return null
        }
    }
}