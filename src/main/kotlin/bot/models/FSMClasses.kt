package bot.models

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

sealed interface BotState : State
data class UserExpectLogin(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class UserExpectPassword(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class UserExpectShop(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class UserStopState(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class BotExpectChatId(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class BotExpectOpenTime(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class BotExpectCloseTime(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class BotExpectTimezone(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class BotStopState(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class DeleteExpectConfirmation(
    override val context: IdChatIdentifier,
    val sourceMessage: CommonMessage<TextContent>,
) :
    BotState

data class DeleteStopState(override val context: IdChatIdentifier, val sourceMessage: CommonMessage<TextContent>) :
    BotState

data class PasswordUpdateExpectPassword(
    override val context: IdChatIdentifier,
    val sourceMessage: CommonMessage<TextContent>,
) :
    BotState

data class PasswordUpdateStopState(
    override val context: IdChatIdentifier,
    val sourceMessage: CommonMessage<TextContent>,
) :
    BotState