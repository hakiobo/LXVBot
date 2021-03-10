package commands.util

import LXVBot
import dev.kord.core.event.message.MessageCreateEvent

interface BotCommand {
    val name: String
    val description: String
    val aliases: List<String>
        get() = emptyList()
    val usages: List<CommandUsage>
        get() = emptyList()
    val category: CommandCategory
        get() = CommandCategory.MISC


    suspend fun runCMD(bot: LXVBot, mCE: MessageCreateEvent, args: List<String>) {
        bot.cmd(mCE, args)
    }

    suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>)
}
