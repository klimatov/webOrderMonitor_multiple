package bot

class BotRepositoryTS {

    fun checkUserDataInTS(botUserData: BotUsers?): Boolean {
        if (botUserData?.tsLogin == "zzz") return true else return false
    }
}