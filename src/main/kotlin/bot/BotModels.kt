package bot

import dev.inmo.tgbotapi.types.Identifier


data class BotUserData(
    var login: String,
    var password: String,
    var shop: String
)

data class BotUsers(
    var tsLogin: String,
    var tsPassword: String,
    var tsShop: String,
    var tgUserId: Identifier,
    var userRole: String
)

enum class UserRole {
    GUEST,
    USER,
    ADMIN,
    ROOT
}
