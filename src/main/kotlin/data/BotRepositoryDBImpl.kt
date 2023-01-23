package data

import bot.BotRepositoryDB
import bot.BotUser
import data.database.botUsers.BotUsersDB
import data.database.botUsers.mapToBotUsers
import data.database.botUsers.mapToBotUsersDTO
import data.database.shopWorkers.ShopWorkersDB
import data.database.shopWorkers.mapToShopWorkersParam
import dev.inmo.tgbotapi.types.Identifier
import domain.models.ShopWorkersParam
import java.util.*

class BotRepositoryDBImpl: BotRepositoryDB {

    override fun setBy(botUser: BotUser): Boolean {
        BotUsersDB.insert(botUser.mapToBotUsersDTO())
        return true
    }

    override fun getAll(): MutableMap<Identifier, BotUser> {
        return BotUsersDB.getAll().associate { it.telegramUserId to it.mapToBotUsers() }.toMutableMap()
    }
}