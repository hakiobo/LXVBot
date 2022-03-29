package commands

import LXVBot
import commands.util.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.entity.Embed
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import rpg.RPGCommand
import rpg.RPGCommand.handleEmbed

object ReadEmbed : BotCommand {
    private const val MSG_LIMIT = 1900

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
        if (mCE.message.author?.id == LXVBot.HAKI_ID) {
            val id = args.firstOrNull()?.toLongOrNull()
            if (id == null) {
                reply(mCE.message, "bad Id")
                return
            }
            val msg = client.rest.channel.getMessage(mCE.message.channelId, Snowflake(id))
            if (msg.embeds.isNotEmpty()) {
//                println(msg.embeds.first())
                if (msg.author.id == LXVBot.RPG_BOT_ID) {
                    handleEmbed(mCE, Embed(EmbedData.from(msg.embeds.first()), client))
                } else {
                    val str = msg.embeds.first().toString()
                    var offset = 0
                    while (str.length - offset > MSG_LIMIT) {
                        reply(mCE.message, "```\n${str.substring(offset, offset + MSG_LIMIT)}\n```")
                        offset += MSG_LIMIT
                    }
                    reply(mCE.message, "```\n${str.substring(offset)}\n```")
                }
            }
        } else {
            reply(mCE.message, "haki only")
        }
    }
}