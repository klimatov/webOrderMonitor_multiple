package data.database.botUsers

import bot.BotUser
import dev.inmo.tgbotapi.types.Identifier
import kotlinx.serialization.Serializable

@Serializable
data class BotUsersDTO(
    val login: String,
    val password: String,
    val shop: String,
    val telegramUserId: Identifier,
    val userRole: String,
    val sapFio: String?,
    val sapPosition: String?,
    val sapId: String?,
)

fun BotUser.mapToBotUserDTO(): BotUsersDTO =
    BotUsersDTO(
        login = tsLogin,
        password = tsPassword,
        shop = tsShop,
        telegramUserId = tgUserId,
        userRole = userRole,
        sapFio = sapFio,
        sapPosition = sapPosition,
        sapId = sapId
    )

fun BotUsersDTO.mapToBotUser(): BotUser =
    BotUser(
        tsLogin = login,
        tsPassword = password,
        tsShop = shop,
        tgUserId = telegramUserId,
        userRole = userRole,
        sapFio = sapFio,
        sapPosition = sapPosition,
        sapId = sapId
    )