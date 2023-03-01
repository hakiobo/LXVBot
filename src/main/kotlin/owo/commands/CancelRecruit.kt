package owo.commands

import LXVBot
import LXVBot.Companion.getUserIdFromString
import commands.util.BotCommand
import commands.util.CommandCategory
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.modify.embed
import entities.LXVUser
import entities.ServerData
import kotlinx.coroutines.delay
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.setValue


object CancelRecruit : BotCommand {
    override val name: String
        get() = "removerecruit"

    override val aliases: List<String>
        get() = listOf("rr")
    override val description: String
        get() = "removes someone's recruit role"

    override val category: CommandCategory
        get() = CommandCategory.MODERATION

    const val embedTitle = "Confirm Recruitment Cancellation"

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if (requireMod(mCE)) return
        if (args.size == 1) {
            val id = getUserIdFromString(args.first())
            if (id != null) {
                val col = db.getCollection<LXVUser>(LXVUser.DB_NAME)
                val entry = getUserFromDB(id, null, col)
                if (entry.serverData.recruitData != null) {
                    val msg = mCE.message.channel.createEmbed {
                        title = embedTitle
                        description =
                            "Are you sure you want to cancel the LXV Recruitment for <@$id>?"
                        footer {
                            text = id.toString()
                        }
                        author {
                            name = mCE.message.author!!.id.toString()
                        }
                        color = Color(0x0000FF)
                    }
                    listOf(true, false).forEach {
                        msg.addReaction(ReactionEmoji.Unicode(LXVBot.getCheckmarkOrCross(it)))
                    }
                    delay(30_000)
                    if (client.rest.channel.getMessage(
                            Snowflake(mCE.message.channelId.value),
                            Snowflake(msg.id.value)
                        ).embeds.firstOrNull()?.color.value == 0x0000FF
                    ) {
                        msg.edit {
                            embed {
                                msg.embeds[0].apply(this)
                                color = Color(0)
                            }
                        }
                    }
                } else {
                    sendMessage(mCE.message.channel, "Could not find current recruitment for that user")
                }
            } else {
                sendMessage(mCE.message.channel, "Invalid User!")
            }
        } else {
            sendMessage(mCE.message.channel, "Invalid Format!")
        }
    }

    suspend fun confirmDeletion(bot: LXVBot, msg: Message, guildId: Snowflake) {
        val userToDelete = Snowflake(msg.embeds[0].footer!!.text)
        val userCol = bot.db.getCollection<LXVUser>(LXVUser.DB_NAME)
        try {
            MemberBehavior(LXVBot.LXV_GUILD_ID, userToDelete, bot.client).removeRole(
                LXVBot.LXV_RECRUIT_ROLE_ID,
                "Recruitment Cancelled by <@${msg.embeds.first().author?.name}>"
            )
        } catch (e: Exception) {
            bot.sendMessage(msg.channel, "Failed to cancel their recruitment", 5000)
            msg.edit {
                embed {
                    msg.embeds[0].apply(this)
                    color = Color(0xFF0000)
                }
            }
            return
        }
        val result = userCol.updateOne(
            LXVUser::_id eq userToDelete,
            setValue(LXVUser::serverData / ServerData::recruitData, null),
        )
        if (result.modifiedCount != 0L) {

            bot.sendMessage(msg.channel) {
                description = "Succesfully cancelled <@$userToDelete>'s Recruitment!"
            }
            msg.edit {
                embed {
                    msg.embeds[0].apply(this)
                    color = Color(0x00FF00)
                }
            }
            bot.log {
                title = "LXV Recruitment Cancelled"
                description = "<@$userToDelete>'s recruitment cancelled by <@${msg.embeds.first().author?.name}>"
                color = Color(0xFFFF00)
            }
        } else {
            bot.sendMessage(msg.channel, "Failed to cancel their recruitment", 5000)
            msg.edit {
                embed {
                    msg.embeds[0].apply(this)
                    color = Color(0xFF0000)
                }
            }
        }
    }

    suspend fun cancelDeletion(bot: LXVBot, msg: Message) {
        msg.edit {
            embed {
                msg.embeds[0].apply(this)
                color = Color(0xFF0000)
            }
        }
    }
}