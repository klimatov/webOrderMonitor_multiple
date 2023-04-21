package data

import bot.BotRepositoryDB
import bot.BotUser
import bot.NewWorker
import bot.UserRole
import data.database.botUsers.BotUsersDB
import data.database.botUsers.mapToBotUser
import data.database.botUsers.mapToBotUserDTO
import data.database.shopWorkers.ShopWorkersDB
import data.database.shopWorkers.mapToNewWorker
import data.database.shopWorkers.mapToShopWorkersDTO
import dev.inmo.tgbotapi.types.Identifier

class DataToBotRepositoryImpl: BotRepositoryDB {

    override fun setUserBy(botUser: BotUser): Boolean {
        BotUsersDB.insert(botUser.mapToBotUserDTO())
        return true
    }

    override fun getUserBy(userId: Identifier): BotUser? {
        val resultUser = BotUsersDB.getUserBy(userId)
        return resultUser?.mapToBotUser()
    }

    override fun getAll(): MutableMap<Identifier, BotUser> {
        return BotUsersDB.getAll().associate { it.telegramUserId to it.mapToBotUser() }.toMutableMap()
    }

    override fun checkWorker(requiredShop: String): BotUser? {
        val resultWorker = ShopWorkersDB.fetchWorkerByShop(requiredShop)
        return if (resultWorker == null) null else BotUser(
            tsLogin = resultWorker.login,
            tsPassword = resultWorker.password,
            tsShop = resultWorker.shop,
            tgUserId = resultWorker.ownerTgId,
            userRole = UserRole.USER.toString(),
            sapFio = null,
            sapPosition = null,
            sapId = null
            )
    }

    override fun setWorkerBy(newWorker: NewWorker): Boolean {
        ShopWorkersDB.insert(newWorker.mapToShopWorkersDTO())
        return true
    }

    override fun deleteWorkerByShop(shop: String): Boolean {
        ShopWorkersDB.delete(shop)
        return true
    }

    override fun getWorkerByShop(requiredShop: String): NewWorker? {
        val resultWorker = ShopWorkersDB.fetchWorkerByShop(requiredShop)
        return if (resultWorker == null) null else resultWorker.mapToNewWorker()
    }

    override fun updateWorkerBy(newWorker: NewWorker): Boolean {
        ShopWorkersDB.update(newWorker.mapToShopWorkersDTO())
        return true
    }

}