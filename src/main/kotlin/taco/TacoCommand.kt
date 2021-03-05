package taco

import LXVBot
import commands.utils.BotCommand
import dev.kord.core.event.message.MessageCreateEvent

object TacoCommand : BotCommand {
    override val name: String
        get() = "tacoshack"
    override val description: String
        get() = "<@490707751832649738> stuff"
    override val aliases: List<String>
        get() = listOf("taco", "ts")

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        reply(mCE.message, "<:pualOwO:782542201837322292>")
    }
}