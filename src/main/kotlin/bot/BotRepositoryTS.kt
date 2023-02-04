package bot

import orderProcessing.data.SecurityData.TS_LOGIN

class BotRepositoryTS {

    fun checkUserDataInTS(botUserData: BotUser?): Boolean {
        if (botUserData?.tsLogin == TS_LOGIN) return true else return false
    }
}