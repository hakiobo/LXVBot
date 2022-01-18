package commands

import LXVBot
import commands.util.*
import dev.kord.core.event.message.MessageCreateEvent

object Logout: BotCommand {
    override val name: String
        get() = "logout"
    override val description: String
        get() = "Logs the bot out"
    override val category: CommandCategory
        get() = CommandCategory.HIDDEN
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(
                listOf(Argument("Message", ArgumentType.TEXT)),
                "Shuts down LXVBot",
                AccessType.HAKI
            )
        )

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if (mCE.message.author?.id == LXVBot.HAKI_ID) {
            try {
                client.rest.channel.createMessage(LXVBot.LXV_BOT_UPDATE_CHANNEL_ID) {
                    content = "Offline: ${args.joinToString(" ")}!"
                }
            } finally {
                client.shutdown()
            }
        }
    }

}