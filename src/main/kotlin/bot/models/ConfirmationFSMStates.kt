package bot.models

import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

data class ConfirmationExpectReason(override val context: IdChatIdentifier) :
    BotState

data class ConfirmationExpectShelf(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class ConfirmationExpectPrinter(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class ConfirmationStopState(override val context: IdChatIdentifier) :
    BotState {
        var myTest: String = "test"
    }

