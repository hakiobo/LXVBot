package moderation

import LXVBot
import entities.LXVUser
import entities.ServerData
import commands.util.*
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

object PicBan : BotCommand {
    override val name: String
        get() = "picban"
    override val description: String
        get() = "Ban someone from getting the camera role"
    override val category: CommandCategory
        get() = CommandCategory.MODERATION
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(
                listOf(Argument("User")), "Prevents the user from getting the camera role", AccessType.ADMIN
            ),
            CommandUsage(
                listOf(Argument("User"), Argument("unban", ArgumentType.EXACT)),
                "Allows the user to get the camera role again",
                AccessType.ADMIN
            ),
        )

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if (mCE.guildId?.value != LXVBot.LXV_SERVER_ID) {
            reply(mCE.message, "This only works in LXV")
            return
        }
        if (Permission.Administrator in mCE.member!!.getPermissions()) {
            if (args.isEmpty() || args.size > 2) {
                reply(mCE.message, "Invalid Format")
            }
            val userId = LXVBot.getUserIdFromString(args.first())
            val member = if (userId == null) null else client.getUser(Snowflake(userId))?.asMemberOrNull(mCE.guildId!!)
            if (member == null) {
                reply(mCE.message, "Invalid User")
                return
            }
            val col = db.getCollection<LXVUser>(LXVUser.DB_NAME)
            if (args.size == 1) {
                val lxvUser = getUserFromDB(Snowflake(userId!!), member)
                if (Snowflake(CAMERA_ROLE_ID) in member.roleIds) {
                    member.removeRole(Snowflake(CAMERA_ROLE_ID), "User has been pic banned")
                }
                if (lxvUser.serverData.picBanned) {
                    reply(mCE.message, "User already pic banned")
                } else {
                    col.updateOne(LXVUser::_id eq userId, setValue(LXVUser::serverData / ServerData::picBanned, true))
                    reply(mCE.message, "User has been pic banned")
                }
            } else {
                if (args[1].lowercase() == "unban") {
                    val lxvUser = getUserFromDB(Snowflake(userId!!), member)
                    if (lxvUser.serverData.mee6Level >= CAMERA_ROLE_LEVEL && Snowflake(CAMERA_ROLE_ID) !in member.roleIds) {
                        member.addRole(Snowflake(CAMERA_ROLE_ID), "User has been pic unbanned")
                    }
                    if (lxvUser.serverData.picBanned) {
                        col.updateOne(
                            LXVUser::_id eq userId,
                            setValue(LXVUser::serverData / ServerData::picBanned, false)
                        )
                        reply(mCE.message, "User has been pic unbanned")
                    } else {
                        reply(mCE.message, "User isn't even pic banned")
                    }
                } else {
                    reply(mCE.message, "Invalid Format")
                }
            }
        } else {
            reply(mCE.message, "Admins Only")
        }
    }
}