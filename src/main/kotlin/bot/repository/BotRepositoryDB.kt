package bot.repository

import bot.models.BotUser
import bot.models.NewWorker
import dev.inmo.tgbotapi.types.Identifier

interface BotRepositoryDB {

    fun setUserBy(botUser: BotUser): Boolean
    fun getUserBy(userId: Identifier): BotUser?

    fun getAll(): MutableMap<Identifier, BotUser>

    fun checkWorker(requiredShop: String): BotUser?


    /**
     * Записываем нового/измененного воркера в БД
     */

    fun setWorkerBy(newWorker: NewWorker): Boolean

    fun deleteWorkerByShop(shop: String): Boolean

    fun getWorkerByShop(requiredShop: String): NewWorker?

    fun updateWorkerBy(newWorker: NewWorker): Boolean
}