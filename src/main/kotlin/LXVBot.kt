import commands.Github
import commands.RPGCommand
import commands.utils.BotCommand
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.MessageBehavior
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
        if (mCE.message.content.startsWith("rpg ", true)) {
            handleRPGCommand(mCE)
        }
        if (mCE.message.content.startsWith(BOT_PREFIX, true)) {
            handleCommand(mCE, mCE.message.content.drop(BOT_PREFIX.length).trim())
        }
    }

    private suspend fun handleCommand(mCE: MessageCreateEvent, msg: String) {
        val args = msg.split(Pattern.compile("\\s+")).map { it.toLowerCase() }
        val cmd = args.first().toLowerCase()
        val toRun = run {
            var toRun: BotCommand? = null
            for (command in commands) {
                if (cmd == command.name || cmd in command.aliases) {
                    toRun = command
                }
            }
            toRun
        }
        if (toRun == null) {
            mCE.message.reply {
                content = "Invalid Command: I'd say use ${BOT_PREFIX}help but there's no help command yet <:PaulOwO:721154434297757727>"
                allowedMentions {
                    repliedUser = false
                }
            }
        } else {
            toRun.runCMD(this, mCE, args.drop(1))
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

    internal suspend inline fun reply(
        message: MessageBehavior,
        replyContent: String = "",
        ping: Boolean = false,
        embedBuilder: (EmbedBuilder.() -> Unit) = { },
    ) {
        message.reply {
            content = replyContent
            allowedMentions {
                repliedUser = ping
            }
            embed(embedBuilder)
        }

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
