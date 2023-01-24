package bot

import dev.inmo.tgbotapi.types.Identifier

interface BotRepositoryDB {

    fun setUserBy(botUser: BotUser): Boolean

    fun getAll(): MutableMap<Identifier, BotUser>

    fun checkWorker(shop: String): BotUser?

    fun setWorkerBy(newWorker: NewWorker): Boolean
}