package bot

interface BotRepository {

    fun checkUserDataInTS(botUserData: BotUserData): Boolean


}