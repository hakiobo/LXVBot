import commands.Github
import commands.Ping
import rpg.RPGCommand
import commands.meta.HelpCommand
import commands.utils.BotCommand
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import java.time.Instant
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.roundToLong


class LXVBot(val client: Kord, val db: CoroutineDatabase) {

    val commands = listOf(
        RPGCommand,
        Github,
        HelpCommand,
        Ping,
    )

    suspend fun startup() {
        client.on<MessageCreateEvent> {
            client.launch {
                handleMessage(this@on)
            }
        }
    }


    private suspend fun handleMessage(mCE: MessageCreateEvent) {
        if (mCE.message.author?.isBot == true) return
        if (mCE.message.content.startsWith("$RPG_PREFIX ", true)) {
            handleRPGCommand(mCE)
        }
        if (mCE.message.content.startsWith(BOT_PREFIX, true)) {
            handleCommand(mCE, mCE.message.content.drop(BOT_PREFIX.length).trim())
        }


        if (mCE.message.mentionedUserIds.contains(client.selfId)) {
            reply(mCE.message, "Hi, Welcome to LXV!\n$BOT_NAME prefix is $BOT_PREFIX")
        }
    }

    private suspend fun handleCommand(mCE: MessageCreateEvent, msg: String) {
        val args = msg.split(Pattern.compile("\\s+"))
        val cmd = args.first().toLowerCase()
        val toRun = lookupCMD(cmd)
        if (toRun != null) {
            toRun.runCMD(this, mCE, args.drop(1))
        } else {
            reply(mCE.message, "Invalid Command: use ${BOT_PREFIX}help to see available commands")
        }
    }

    private suspend fun handleRPGCommand(mCE: MessageCreateEvent) {
        val args = mCE.message.content.split(Pattern.compile("\\s+")).drop(1)
        val reminder = RPGCommand.findReminder(args)
        if (reminder != null) {
            val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
            val user = getUserFromDb(mCE.message.author!!.id, userCol)
            val data = user.rpg.rpgReminders[reminder.name]
            val curTime = mCE.message.id.toInstant().toEpochMilli()
            val dif =
                if (reminder.name == "hunt" && args.drop(1).firstOrNull()?.toLowerCase() in listOf("t", "together")) {
                    reminder.cooldownMS * max(user.rpg.patreonMult, user.rpg.partnerPatreon)
                } else {
                    reminder.cooldownMS * if (reminder.patreonAffected) user.rpg.patreonMult else 1.0
                }
            if (data?.enabled == true && (curTime - data.lastUse > dif)
            ) {
                client.launch {
                    user.rpg.rpgReminders[reminder.name] = Reminder(data.enabled, curTime, data.count + 1)
                    userCol.replaceOne(LXVUser::_id eq user._id, user)
                    delay(dif.roundToLong())
                    mCE.message.reply {
                        content = "rpg ${
                            reminder.responseName(reminder, args)
                        } cooldown is done"
                    }
                }
            }
        }
    }

    internal suspend fun reply(
        message: MessageBehavior,
        replyContent: String = "",
        ping: Boolean = false,
        embedBuilder: EmbedBuilder.() -> Unit,
    ) {
        message.reply {
            content = replyContent
            allowedMentions {
                repliedUser = ping
            }
            embed(embedBuilder)
        }
    }

    internal suspend fun reply(
        message: MessageBehavior,
        replyContent: String = "",
        ping: Boolean = false,
    ) {
        message.reply {
            content = replyContent
            allowedMentions {
                repliedUser = ping
            }
        }
    }

    internal suspend fun sendMessage(
        channel: MessageChannelBehavior,
        message: String = "",
        deleteAfterMS: Long = 0L,
        mentionsAllowed: Boolean = false,
        embedToSend: (EmbedBuilder.() -> Unit)? = null,
    ) {
        client.launch {
            if (deleteAfterMS == 0L) {
                channel.createMessage {
                    content = message
                    if (!mentionsAllowed) {
                        allowedMentions()
                    }
                    if (embedToSend != null) {
                        embed(embedToSend)
                    }
                }
            } else {
                val msg = channel.createMessage {
                    content = message
                    if (!mentionsAllowed) {
                        allowedMentions()
                    }
                    if (embedToSend != null) {
                        embed(embedToSend)
                    }
                }
                delay(deleteAfterMS)
                msg.delete()
            }
        }
    }

    internal fun lookupCMD(userCMD: String): BotCommand? {
        for (cmd in commands) {
            if (userCMD == cmd.name || userCMD in cmd.aliases) {
                return cmd
            }
        }
        return null
    }


    suspend fun getUserFromDb(id: Snowflake, col: CoroutineCollection<LXVUser>): LXVUser {
        val user = col.findOne(LXVUser::_id eq id.value)
        return if (user == null) {
            val new = LXVUser(id.value)
            col.insertOne(new)
            new
        } else {
            user
        }
    }

    companion object {
        const val BOT_NAME = "LXV Bot"
        const val BOT_PREFIX = "+"
        const val RPG_PREFIX = "rpg"


        fun getUserIdFromString(s: String?): Long? {
            return if (s == null) {
                null
            } else if (s.toLongOrNull() != null) {
                s.toLong()
            } else if (s.startsWith("<@") && s.endsWith(">")) {
                if (s[2] == '!') {
                    s.drop(3).dropLast(1).toLongOrNull()
                } else {
                    s.drop(2).dropLast(1).toLongOrNull()
                }
            } else {
                null
            }
        }
    }

}


fun Snowflake.toInstant(): Instant = Instant.ofEpochMilli((value ushr 22) + 1420070400000L)
