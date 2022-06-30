package moderation.customs

import LXVBot
import commands.util.BotCommand
import dev.kord.core.behavior.edit
import dev.kord.core.event.message.MessageCreateEvent

object RoleName : BotCommand {
    override val name: String
        get() = "rolename"
    override val description: String
        get() = "Change your custom role name"
    override val aliases: List<String>
        get() = listOf("rname")

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if (requireLXVGuild(mCE)) return
        val lxvUser = getUserFromDB(mCE.message.author!!.id, mCE.message.author)
        val roleId = lxvUser.serverData.customRole
        if (roleId == null) {
            reply(mCE.message, "You don't have a custom role")
            return
        }
        val realRole = mCE.getGuild()!!.getRoleOrNull(roleId)
        if (realRole == null) {
            reply(mCE.message, "Looks like your role doesn't exist anymore, RIP")
            return
        }
        if (args.isEmpty()) {
            reply(mCE.message, "that's not a name")
            return
        }
        val oldName = realRole.name
        val newName = args.joinToString(" ")
        try {
            realRole.edit {
                this.name = newName
            }
            reply(mCE.message, "OOO, interesting!")
        } catch (e: Exception) {
            println(e.message)
            reply(mCE.message, "error changing the name, maybe it was too long?")
            return
        }
        log {
            title = "Role Name Changed"
            description = "<@&$roleId> name changed"
            field {
                name = "Old Name"
                value = oldName
                inline = true
            }
            field {
                name = "New Name"
                value = newName
                inline = true
            }
        }

    }
}

