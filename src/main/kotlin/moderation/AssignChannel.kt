package moderation

import LXVBot
import entities.LXVUser
import entities.ServerData
import commands.utils.*
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.message.MessageCreateEvent
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

object AssignChannel : BotCommand {
    override val name: String
        get() = "givechannel"
    override val aliases: List<String>
        get() = listOf("assignchannel", "gc")
    override val description: String
        get() = "Give someone a channel manually"
    override val category: CommandCategory
        get() = CommandCategory.MODERATION
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(
                listOf(Argument("user"), Argument("channel")),
                "Gives the specified user the specified channel",
                AccessType.ADMIN
            )
        )

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if(mCE.guildId?.value != LXVBot.LXV_SERVER_ID){
            reply(mCE.message, "This only works in LXV")
            return
        }
        if (Permission.Administrator in mCE.member!!.getPermissions()) {
            if (args.size >= 2) {
                val userId = LXVBot.getUserIdFromString(args[0])
                val channelId = LXVBot.getChannelIdFromString(args[1])
                val force = (args.getOrNull(3)?.toLowerCase() in listOf("f", "force"))
                if (userId == null || channelId == null) {
                    reply(mCE.message, "Fix format when")
                    return
                }
                val channel = client.getChannelOf<GuildMessageChannel>(Snowflake(channelId))
                val user = client.getUser(Snowflake(userId))
                if (user == null) {
                    reply(mCE.message, "That user isn't valid")
                    return
                }
                if (channel == null || channel.guildId.value != LXVBot.LXV_SERVER_ID) {
                    reply(mCE.message, "That channel isn't valid")
                    return
                }
                val col = db.getCollection<LXVUser>(LXVUser.DB_NAME)
                val check = col.find(LXVUser::serverData / ServerData::customChannel eq channelId).toList()
                if (check.isEmpty() || force) {
                    val lxvUser = getUserFromDB(Snowflake(userId), user, col)
                    col.updateOne(
                        LXVUser::_id eq userId,
                        setValue(LXVUser::serverData / ServerData::customChannel, channelId)
                    )
                    val desc = StringBuilder("New Channel: <#$channelId>\n")
                    reply(mCE.message) {
                        title = "Channel Assigned"
                        if (lxvUser.serverData.customChannel != null) {
                            desc.append("Previous Channel: <#${lxvUser.serverData.customChannel}>\n")
                        }
                        desc.append("<@${userId}> <@${LXVBot.MEE6_ID}> Level: ${lxvUser.serverData.mee6Level}")
                        description = desc.toString()
                    }
                } else {
                    val self = client.getSelf()
                    reply(mCE.message) {
                        title = "Channel Assign Error"
                        color = Color(0xFF0000)
                        description =
                            "The following ${if (check.size == 1) "user already has" else "users already have"} that channel assigned to them\n${
                                check.joinToString(
                                    "\n"
                                ) { "<@${it._id}>" }
                            }"
                        footer {
                            text =
                                "You can use \"lxv $name $userId $channelId force\" to add them to this channel anyway"
                            icon = self.avatar.url
                        }
                    }
                }
            } else {
                reply(mCE.message, "Fix format when")
            }
        } else {
            reply(mCE.message, "Admins only smh")
        }
    }
}