package moderation.customs

import LXVBot
import entities.LXVUser
import entities.ServerData
import commands.util.Argument
import commands.util.BotCommand
import commands.util.CommandCategory
import commands.util.CommandUsage
import dev.kord.core.event.message.MessageCreateEvent
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

object RemoveRole : BotCommand {
    override val name: String
        get() = "revokerole"
    override val description: String
        get() = "Takes away the given user's custom role"
    override val aliases: List<String>
        get() = listOf("removerole")
    override val category: CommandCategory
        get() = CommandCategory.MODERATION
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(listOf(Argument("User")), "Removes the user's custom role")
        )

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if (requireLXVGuild(mCE)) return
        if (requireAdmin(mCE)) return
        if (args.size != 1) {
            reply(mCE.message, "Invalid format")
            return
        }
        val userId = LXVBot.getUserIdFromString(args.first())
        if (userId == null) {
            reply(mCE.message, "Could not read user")
            return
        }
        val user = client.getUser(userId)
        if (user == null) {
            reply(mCE.message, "Could not find user")
            return
        }
        val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
        val lxvUser = getUserFromDB(userId, user, userCol)
        if (lxvUser.serverData.customRole == null) {
            reply(mCE.message, "<@$userId> doesn't have a custom role")
        } else {
            userCol.updateOne(
                LXVUser::_id eq userId,
                setValue(LXVUser::serverData / ServerData::customRole, null)
            )
            reply(mCE.message) {
                title = "Custom Role Removed"
                description = "Removed Role: <@&${lxvUser.serverData.customRole}>"
            }
            log {
                title = "Custom Role Removed"
                description = "Removed Role: <@&${lxvUser.serverData.customRole}> from <@$userId> by ${mCE.message.author!!.mention}"
            }
            val member = user.asMemberOrNull(mCE.guildId!!)
            member?.removeRole(lxvUser.serverData.customRole, "Custom Role Removed")
        }
    }
}