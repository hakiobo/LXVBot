package commands

import LXVBot
import commands.util.BotCommand
import commands.util.CommandCategory
import commands.util.CommandUsage
import dev.kord.core.entity.Guild
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

object ServersCommand : BotCommand {
    override val name: String
        get() = "guilds"
    override val description: String
        get() = "Lists the guilds ${LXVBot.BOT_NAME} is a part of"
    override val aliases: List<String>
        get() = listOf("guildcount", "servers", "servercount")
    override val category: CommandCategory
        get() = CommandCategory.LXVBOT
    override val usages: List<CommandUsage>
        get() = listOf(CommandUsage(listOf(), "Lists the guilds ${LXVBot.BOT_NAME} is currently in"))

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        val guilds = mutableListOf<Guild>()
        client.guilds.onEach { guilds.add(it) }.collect()
        if (args.firstOrNull() == "all" && mCE.message.author?.id in listOf(LXVBot.ERYS_ID, LXVBot.HAKI_ID)) {
            sendMessage(
                mCE.message.channel,
                "**__${LXVBot.BOT_NAME} Guilds__**\n${guilds.joinToString("\n") { it.name }}"
            )
        } else {
            sendMessage(mCE.message.channel, "${guilds.size} ${LXVBot.BOT_NAME} Guilds")
        }
    }
}