import commands.*
import rpg.RPGCommand
import commands.meta.HelpCommand
import commands.utils.BotCommand
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moderation.handleMee6LevelUpMessage
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import rpg.RPGCommand.handleRPGCommand
import taco.TacoCommand
import java.time.Instant
import java.time.ZoneId
import java.util.regex.Pattern


class LXVBot(val client: Kord, mongoCon: CoroutineClient) {

    val db = mongoCon.getDatabase(DB_NAME)
    val hakiDb = mongoCon.getDatabase("Hakibot")

    val commands = listOf(
        RPGCommand,
        Github,
        HelpCommand,
        Ping,
        OwOLeaderboard,
        OwOStat,
        CPCommand,
        ServersCommand,
        Invite,
        TacoCommand,
    )

    suspend fun startup() {
        client.on<MessageCreateEvent> {
            client.launch {
                handleMessage(this@on)
            }
        }
    }


    private suspend fun handleMessage(mCE: MessageCreateEvent) {
        if (mCE.message.author?.id?.value == MEE6_ID && mCE.message.channelId.value == LEVEL_UP_CHANNEL_ID) {
            handleMee6LevelUpMessage(mCE)
        }
        if (mCE.message.author?.isBot == true) return
        if (mCE.guildId == null) {
            sendMessage(mCE.message.channel, "I don't do DMs, sorry <:pualOwO:782542201837322292>")
            return
        }
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
        if (cmd.isBlank()) return
        val toRun = lookupCMD(cmd)
        if (toRun != null) {
            toRun.runCMD(this, mCE, args.drop(1))
        } else {
            reply(mCE.message, "Invalid Command: use ${BOT_PREFIX}help to see available commands")
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

    internal suspend fun getUserFromDB(
        userID: Snowflake,
        u: User? = null,
        col: CoroutineCollection<LXVUser> = db.getCollection(LXVUser.DB_NAME)
    ): LXVUser {
        val query = col.findOne(LXVUser::_id eq userID.value)
        return if (query == null) {
            val user = if (u == null) {
                LXVUser(userID.value, client.getUser(userID)?.username ?: "Deleted User#${userID.value}")
            } else {
                LXVUser(userID.value, u.username)
            }
            col.insertOne(user)
            user
        } else {
            if (u != null && u.username != query.username) {
                col.updateOne(LXVUser::_id eq query._id, setValue(LXVUser::username, u.username))
                query.copy(username = u.username)
            } else if (query.username == null) {
                val name = client.getUser(userID)?.username ?: "Deleted User#${userID.value}"
                col.updateOne(LXVUser::_id eq query._id, setValue(LXVUser::username, name))
                query.copy(username = name)
            } else {
                query
            }
        }
    }

    companion object {
        const val BOT_NAME = "LXV Bot"
        const val BOT_PREFIX = "lxv"
        const val RPG_PREFIX = "rpg"
        const val DB_NAME = "lxv"
        const val HAKI_ID = 292483348738080769
        const val ERYS_ID = 412812867348463636
        const val MEE6_ID = 159985870458322944
        const val LEVEL_UP_CHANNEL_ID = 763523136238780456
        const val CHECKMARK_EMOJI = "\u2705"
        const val CROSSMARK_EMOJI = "\u274c"

        val PST: ZoneId = ZoneId.of("PST", ZoneId.SHORT_IDS)

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

        fun getCheckmarkOrCross(checkmark: Boolean): String = if (checkmark) CHECKMARK_EMOJI else CROSSMARK_EMOJI
    }

}

fun Snowflake.toInstant(): Instant = Instant.ofEpochMilli((value ushr 22) + 1420070400000L)
