package commands.util

import LXVBot
import dev.kord.common.entity.Permission
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

    suspend fun LXVBot.requireLXVGuild(mCE: MessageCreateEvent, msg: String = "This only works in LXV"): Boolean {
        if (mCE.guildId != LXVBot.LXV_GUILD_ID) {
            reply(mCE.message, msg)
            return true
        }
        return false
    }

    /**
     * Returns true and replies with the given message if the user does not have admin permission in the server
     */
    suspend fun LXVBot.requireAdmin(mCE: MessageCreateEvent, msg: String = "Admins only smh"): Boolean {
        if (Permission.Administrator !in mCE.member!!.getPermissions()) {
            reply(mCE.message, msg)
            return true
        }
        return false
    }

    suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>)
}
