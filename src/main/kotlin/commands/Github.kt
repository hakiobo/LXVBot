package commands

import LXVBot
import commands.util.BotCommand
import commands.util.CommandCategory
import dev.kord.core.event.message.MessageCreateEvent

object Github : BotCommand {
    override val name: String
        get() = "github"
    override val description: String
        get() = "Links to the github containing ${LXVBot.BOT_NAME}'s code"
    override val aliases: List<String>
        get() = listOf("git", "code")
    override val category: CommandCategory
        get() = CommandCategory.LXVBOT

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        reply(mCE.message, "${LXVBot.BOT_NAME} code is here: https://github.com/hakiobo/LXVBot")
    }
}