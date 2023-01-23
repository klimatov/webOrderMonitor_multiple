package bot

class BotRepositoryTS {

    fun checkUserDataInTS(botUserData: BotUser?): Boolean {
        if (botUserData?.tsLogin == "zzz") return true else return false
    }
}