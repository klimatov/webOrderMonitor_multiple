package bot

import dev.inmo.tgbotapi.types.Identifier


data class BotUserData(
    var login: String,
    var password: String,
    var shop: String
)

data class BotUsers(
    val tsLogin: String,
    val tsPassword: String,
    val tsShop: String,
    val tgUserId: Identifier,
    val userRole: UserRole
)

enum class UserRole {
    GUEST,
    USER,
    ADMIN,
    ROOT
}
