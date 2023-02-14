package owo.commands

import LXVBot
import LXVBot.Companion.getUserIdFromString
import commands.util.*
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.modify.embed
import entities.UserDailyStats
import entities.UserGuildDate
import kotlinx.coroutines.delay
import org.litote.kmongo.*

object DeleteOwOCount : BotCommand {
    override val name: String
        get() = "resetowo"
    override val description: String
        get() = "Resets the owo count for the specified member in this server"
    override val aliases: List<String>
        get() = listOf("eraseowo", "clearowo", "oworeset")
    override val category: CommandCategory
        get() = CommandCategory.MODERATION
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(
                listOf(Argument(listOf("id", "mention"), ChoiceType.DESCRIPTION)),
                "Resets the specified user's owo count in the server",
                AccessType.ADMIN
            )
        )

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if (requireMod(mCE)) return
        if (args.size == 1) {
            val id = getUserIdFromString(args.first())
            if (id != null) {
                val entry = db.getCollection<UserDailyStats>(UserDailyStats.DB_NAME)
                    .findOne(
                        and(
                            UserDailyStats::_id / UserGuildDate::user eq id,
                            UserDailyStats::_id / UserGuildDate::guild eq mCE.guildId,
                            UserDailyStats::owoInvalid ne true,
                        )
                    )
                if (entry != null) {
                    val msg = mCE.message.channel.createEmbed {
                        title = "Confirm OwO Stats Reset"
                        description =
                            "Are you sure you want to reset the OwO stats for <@$id> in ${mCE.getGuild()!!.name}?"
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
                    sendMessage(mCE.message.channel, "Could not find any owos for that user")
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
        val statCol = bot.db.getCollection<UserDailyStats>(UserDailyStats.DB_NAME)
        val result = statCol.updateMany(
            and(
                UserDailyStats::_id / UserGuildDate::user eq userToDelete,
                UserDailyStats::_id / UserGuildDate::guild eq guildId,
            ),
            setValue(UserDailyStats::owoInvalid, true),
        )
        if (result.modifiedCount != 0L) {
            bot.sendMessage(msg.channel) {
                description = "Succesfully reset <@$userToDelete>'s OwO Stats!"
            }
            msg.edit {
                embed {
                    msg.embeds[0].apply(this)
                    color = Color(0x00FF00)
                }
            }
            bot.log {
                title = "OwO Stats reset"
                description = "<@$userToDelete>'s OwO Stats deleted by <@${msg.embeds.first().author?.name}>"
                color = Color(0xFFFF00)
            }
        } else {
            bot.sendMessage(msg.channel, "Failed to reset their owos", 5000)
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