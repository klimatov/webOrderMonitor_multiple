package data

import bot.BotRepositoryDB
import bot.BotUsers
import data.database.botUsers.BotUsersDB
import data.database.botUsers.mapToBotUsersDTO

class BotRepositoryDBImpl: BotRepositoryDB {

    override fun setBy(botUsers: BotUsers): Boolean {

        BotUsersDB.insert(botUsers.mapToBotUsersDTO())
        return true
    }

}