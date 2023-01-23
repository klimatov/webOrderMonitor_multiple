package bot

import dev.inmo.tgbotapi.types.Identifier

interface BotRepositoryDB {

    fun setBy(botUser: BotUser): Boolean

    fun getAll(): MutableMap<Identifier, BotUser>
}