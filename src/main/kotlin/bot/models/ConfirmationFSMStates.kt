package bot.models

import dev.inmo.tgbotapi.types.IdChatIdentifier

data class ConfirmationStart(override val context: IdChatIdentifier, val webNum: String, val orderId: String) :
    BotState
data class ConfirmationExpectReason(override val context: IdChatIdentifier, val orderSaveParam: OrderSaveParam) :
    BotState

data class ConfirmationExpectShelf(override val context: IdChatIdentifier , val orderSaveParam: OrderSaveParam) :
    BotState

data class ConfirmationExpectPrinter(override val context: IdChatIdentifier, val orderSaveParam: OrderSaveParam) :
    BotState

data class ConfirmationStopState(override val context: IdChatIdentifier, val orderSaveParam: OrderSaveParam) :
    BotState
