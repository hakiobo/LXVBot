package commands

import LXVBot
import commands.util.BotCommand
import commands.util.CommandCategory
import commands.util.CommandUsage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.allowedMentions
import dev.kord.rest.builder.message.modify.allowedMentions
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

object Ping : BotCommand {
    override val name: String
        get() = "ping"
    override val description: String
        get() = "Pong!"
    override val aliases: List<String>
        get() = listOf("pong")
    override val usages: List<CommandUsage>
        get() = listOf(CommandUsage(listOf(), "Ping ${LXVBot.BOT_NAME}!"))
    override val category: CommandCategory
        get() = CommandCategory.LXVBOT

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        val received = Clock.System.now()
        val msg =
            "\ud83c\udfd3 Pong! Received in ${(received - mCE.message.id.timestamp).inWholeMilliseconds} ms"
        val sent = mCE.message.reply {
            content = "$msg\nReply Sent In `Waiting . . .`"
            allowedMentions {
                repliedUser = false
            }
        }
        val time = (sent.id.timestamp - received).inWholeMilliseconds
        sent.edit {
            content = "$msg\nReply Sent In $time ms"
            allowedMentions {
                repliedUser = false
            }
        }
    }
}