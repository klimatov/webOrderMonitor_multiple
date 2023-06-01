package bot.models

import dev.inmo.tgbotapi.types.IdChatIdentifier

data class ConfirmationStartState(override val context: IdChatIdentifier, val webNum: String, val orderId: String) :
    BotState

data class ConfirmationMainState(override val context: IdChatIdentifier, val orderSaveParam: OrderSaveParam) :
    BotState

data class ConfirmationItemState(override val context: IdChatIdentifier, val orderSaveParam: OrderSaveParam) :
    BotState

data class ConfirmationChoosingReasonState(override val context: IdChatIdentifier, val orderSaveParam: OrderSaveParam) :
    BotState

data class ConfirmationEnterReasonCommentState(override val context: IdChatIdentifier, val orderSaveParam: OrderSaveParam) :
    BotState

data class ConfirmationChoosingSomeState(override val context: IdChatIdentifier, val orderSaveParam: OrderSaveParam) :
    BotState

data class ConfirmationChoosingShelf(override val context: IdChatIdentifier, val orderSaveParam: OrderSaveParam) :
    BotState

data class ConfirmationChoosingPrinter(override val context: IdChatIdentifier, val orderSaveParam: OrderSaveParam) :
    BotState

data class ConfirmationStopState(override val context: IdChatIdentifier, val orderSaveParam: OrderSaveParam) :
    BotState
