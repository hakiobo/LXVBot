package moderation.customs

import LXVBot
import entities.LXVUser
import entities.ServerData
import commands.util.Argument
import commands.util.BotCommand
import commands.util.CommandCategory
import commands.util.CommandUsage
import dev.kord.common.entity.Permission
import dev.kord.core.event.message.MessageCreateEvent
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

object RemoveChannel : BotCommand {
    override val name: String
        get() = "revokechannel"
    override val description: String
        get() = "Takes away the given user's custom channel"
    override val aliases: List<String>
        get() = listOf("removechannel")
    override val category: CommandCategory
        get() = CommandCategory.MODERATION
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(listOf(Argument("User")), "Removes the user's custom channel")
        )

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if(mCE.guildId != LXVBot.LXV_GUILD_ID){
            reply(mCE.message, "This only works in LXV")
            return
        }
        if (Permission.Administrator in mCE.member!!.getPermissions()) {
            if (args.size == 1) {
                val userId = LXVBot.getUserIdFromString(args.first())
                if (userId == null) {
                    reply(mCE.message, "Fake user")
                    return
                }
                val user = client.getUser(userId)
                if (user == null) {
                    reply(mCE.message, "Fake user")
                    return
                }
                val col = db.getCollection<LXVUser>(LXVUser.DB_NAME)
                val lxvUser = getUserFromDB(userId, user, col)
                if (lxvUser.serverData.customChannel == null) {
                    reply(mCE.message, "<@$userId> doesn't have a custom channel")
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