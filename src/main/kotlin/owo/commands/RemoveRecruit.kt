package owo.commands

import LXVBot
import commands.util.BotCommand
import dev.kord.core.event.message.MessageCreateEvent

object RemoveRecruit: BotCommand {
    override val name: String
        get() = "removerecruit"
    override val description: String
        get() = "removes someone's recruit role"

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if(requireMod(mCE)) return

        TODO("Not yet implemented")
    }
}