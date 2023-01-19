package bot

interface BotRepositoryDB {

    fun setBy(botUsers: BotUsers): Boolean
}