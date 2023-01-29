package bot

import dev.inmo.tgbotapi.types.Identifier

interface BotRepositoryDB {

    fun setUserBy(botUser: BotUser): Boolean

    fun getAll(): MutableMap<Identifier, BotUser>

    fun checkWorker(requiredShop: String): BotUser?


    /**
     * Записываем нового/измененного воркера в БД
     */

    fun setWorkerBy(newWorker: NewWorker): Boolean

    fun deleteWorkerByShop(shop: String): Boolean

    fun getWorkerByShop(shop: String): NewWorker?
}