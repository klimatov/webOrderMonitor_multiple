package bot.models

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup

data class BotInstanceParameters(
    var shopOpenTime: Int = 9,
    var shopCloseTime: Int = 22,
    // из староого WOM Bot
    var targetChatId: ChatIdentifier = ChatId(0),
    var msgNotification: Boolean = true,
    var dayConfirmedCount: Int = 0,  //подтверждено за день

    // из старого WOM TGInfoMessage
    var currentInfoMsgId: Long? = null,
    var newInfoMsgId: Long? = null,
    var currentInfoMsg: InlineKeyboardMarkup? = null,
    var notConfirmedOrders: Int = 0,  //активных не подтвержденных
    var gmt: String = "+0300",
)