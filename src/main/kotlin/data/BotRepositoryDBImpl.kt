package data

import bot.BotRepositoryDB
import bot.BotUser
import bot.NewWorker
import bot.UserRole
import data.database.botUsers.BotUsersDB
import data.database.botUsers.mapToBotUsers
import data.database.botUsers.mapToBotUsersDTO
import data.database.shopWorkers.ShopWorkersDB
import data.database.shopWorkers.mapToNewWorker
import data.database.shopWorkers.mapToShopWorkersDTO
import dev.inmo.tgbotapi.types.Identifier

class BotRepositoryDBImpl: BotRepositoryDB {

    override fun setUserBy(botUser: BotUser): Boolean {
        BotUsersDB.insert(botUser.mapToBotUsersDTO())
        return true
    }

    override fun getAll(): MutableMap<Identifier, BotUser> {
        return BotUsersDB.getAll().associate { it.telegramUserId to it.mapToBotUsers() }.toMutableMap()
    }

    override fun checkWorker(requiredShop: String): BotUser? {
        val resultWorker = ShopWorkersDB.fetchWorkerByShop(requiredShop)
        return if (resultWorker == null) null else BotUser(
            tsLogin = resultWorker.login,
            tsPassword = resultWorker.password,
            tsShop = resultWorker.shop,
            tgUserId = resultWorker.ownerTgId,
            userRole = UserRole.USER.toString()
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