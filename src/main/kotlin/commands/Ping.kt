package commands

import LXVBot
import commands.utils.BotCommand
import commands.utils.CommandCategory
import commands.utils.CommandUsage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import toInstant
import java.time.Duration
import java.time.Instant

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
        val received = Instant.now()
        val msg =
            "\ud83c\udfd3 Pong! Received in ${Duration.between(mCE.message.id.toInstant(), received).toMillis()} ms"
        val sent = mCE.message.reply {
            content = "$msg\nReply Sent In `Waiting . . .`"
            this.allowedMentions {
                repliedUser = false
            }
        }
        val time = Duration.between(received, sent.id.toInstant()).toMillis()
        sent.edit {
            this.content = "$msg\nReply Sent In $time ms"
            this.allowedMentions {
                repliedUser = false
            }
        }
    }
}