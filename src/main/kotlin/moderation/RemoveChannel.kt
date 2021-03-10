package moderation

import LXVBot
import LXVUser
import ServerData
import commands.utils.Argument
import commands.utils.BotCommand
import commands.utils.CommandCategory
import commands.utils.CommandUsage
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

object RemoveChannel : BotCommand {
    override val name: String
        get() = "revokecustom"
    override val description: String
        get() = "Takes away the given user's custome channel"
    override val aliases: List<String>
        get() = listOf("rc")
    override val category: CommandCategory
        get() = CommandCategory.MODERATION
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(listOf(Argument("User")), "Removes the user's custom channel")
        )

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if (Permission.Administrator in mCE.member!!.getPermissions()) {
            if (args.size == 1) {
                val userId = LXVBot.getUserIdFromString(args.first())
                if (userId == null) {
                    reply(mCE.message, "Fake user")
                    return
                }
                val user = client.getUser(Snowflake(userId))
                if (user == null) {
                    reply(mCE.message, "Fake user")
                    return
                }
                val col = db.getCollection<LXVUser>(LXVUser.DB_NAME)
                val lxvUser = getUserFromDB(Snowflake(userId), user, col)
                if (lxvUser.serverData.customChannel == null) {
                    reply(mCE.message, "That User doesn't have a custom channel")
                } else {
                    col.updateOne(
                        LXVUser::_id eq userId,
                        setValue(LXVUser::serverData / ServerData::customChannel, null)
                    )
                    reply(mCE.message) {
                        title = "Custom Channel Revoked"
                        description = "Revoked Channel: <#${lxvUser.serverData.customChannel}>"
                    }
                }
            } else {
                reply(mCE.message, "bad format")
            }
        } else {
            reply(mCE.message, "Only Admins can do this smh")
        }
    }
}