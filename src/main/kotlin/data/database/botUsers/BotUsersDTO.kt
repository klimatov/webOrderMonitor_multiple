package data.database.botUsers

import bot.BotUsers
import dev.inmo.tgbotapi.types.Identifier
import kotlinx.serialization.Serializable

@Serializable
data class BotUsersDTO(
    val login: String,
    val password: String,
    val shop: String,
    val telegramUserId: Identifier,
    val userRole: String
)

fun BotUsers.mapToBotUsersDTO(): BotUsersDTO =
    BotUsersDTO(
        login = tsLogin,
        password = tsPassword,
        shop = tsShop,
        telegramUserId = tgUserId,
        userRole = userRole
    )

fun BotUsersDTO.mapToBotUsers(): BotUsers =
    BotUsers(
        tsLogin = login,
        tsPassword = password,
        tsShop = shop,
        tgUserId = telegramUserId,
        userRole = userRole
        )