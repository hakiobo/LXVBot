package moderation.customs

import LXVBot
import commands.util.*
import dev.kord.common.Color
import dev.kord.core.event.message.MessageCreateEvent
import entities.LXVUser
import entities.ServerData
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

object AssignRole : BotCommand {
    override val name: String
        get() = "assignrole"
    override val description: String
        get() = "Gives the custom role to the user that they can change the name and color of"
    override val aliases: List<String>
        get() = listOf("giverole", "grole")
    override val category: CommandCategory
        get() = CommandCategory.MODERATION
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(
                listOf(Argument("user"), Argument("role")),
                "Assigns the specified user the specified custom role",
                AccessType.ADMIN
            )
        )

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if (requireLXVGuild(mCE)) return
        if (requireAdmin(mCE)) return

        if(args.size < 2) {
            reply(mCE.message, "too few arguments")
            return
        }

        val userId = LXVBot.getUserIdFromString(args[0])
        val roleId = LXVBot.getRoleIdFromString(args[1])
        if (userId == null || roleId == null) {
            reply(mCE.message, "Couldn't read either the user or the role")
            return
        }

        val role = mCE.getGuild()!!.getRoleOrNull(roleId)
        val user = mCE.getGuild()!!.getMemberOrNull(userId)
        if(role == null || user == null) {
            reply(mCE.message, "Couldn't find either the user or the role")
            return
        }

        val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
        val check = userCol.find(LXVUser::serverData / ServerData::customRole eq roleId).toList()
        if(check.isEmpty()) {
            val lxvUser = getUserFromDB(userId, user, userCol)
            userCol.updateOne(
                LXVUser::_id eq userId,
                setValue(LXVUser::serverData / ServerData::customRole, roleId)
            )
            val desc = StringBuilder("Assigned Role: <@&$roleId>\n")
            desc.appendLine("To <@${userId}> <@${LXVBot.MEE6_ID}> Level: ${lxvUser.serverData.mee6Level}")
            desc.appendLine("By ${mCE.message.author!!.mention}")
            if (lxvUser.serverData.customRole != null) {
                desc.append("Previous Role: <@&${lxvUser.serverData.customChannel}>\n")
            }

            reply(mCE.message) {
                title = "Custom Role Assigned"
                description = desc.toString()
            }
            log {
                title = "Custom Role Assigned"
                description = desc.toString()
            }
            user.addRole(roleId, "added as their custom role")
        } else {
            val self = client.getSelf()
            reply(mCE.message) {
                title = "Role Assign Error"
                color = Color(0xFF0000)
                description =
                    "The following ${if (check.size == 1) "user already has" else "users already have"} that role assigned to them:\n${
                        check.joinToString(
                            "\n"
                        ) { "<@${it._id}>" }
                    }"
                footer {
                    text = "tell hika if for some reason you have a good reason to give the same role to multiple people"
                    icon = self.avatar?.url
                }
            }
        }
    }
}