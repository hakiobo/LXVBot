package commands

import LXVBot
import commands.util.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent

object ReadEmbed : BotCommand {
    override val name: String
        get() = "readembed"
    override val description: String
        get() = "Read the embed from the given message"
    override val category: CommandCategory
        get() = CommandCategory.HIDDEN
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(listOf(Argument("messageId")), "checks the embded from the given message", AccessType.HAKI)
        )

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if (mCE.message.author?.id?.value == LXVBot.HAKI_ID) {
            val id = args.firstOrNull()?.toLongOrNull()
            if (id == null) {
                reply(mCE.message, "bad Id")
                return
            }
            val msg = client.rest.channel.getMessage(mCE.message.channelId, Snowflake(id))
            if (msg.embeds.isNotEmpty()) {
                println(msg.embeds.first())
                reply(mCE.message, "```\n${msg.embeds.first()}\n```")
            }
        } else {
            reply(mCE.message, "haki only")
        }
    }
}