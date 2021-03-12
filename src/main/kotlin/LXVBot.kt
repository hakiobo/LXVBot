import commands.*
import rpg.RPGCommand
import commands.meta.HelpCommand
import commands.util.BotCommand
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.EmbedBuilder
import entities.LXVUser
import entities.StoredReminder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moderation.AssignChannel
import moderation.PicBan
import moderation.RemoveChannel
import moderation.handleMee6LevelUpMessage
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import rpg.RPGCommand.handleRPGCommand
import rpg.RPGCommand.handleRPGMessage
import taco.TacoCommand
import taco.TacoCommand.handleTacoCommand
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
        AssignChannel,
        RemoveChannel,
        PicBan,
        ReadEmbed,
    )

    suspend fun startup() {
        client.on<ReadyEvent> {
            val p = client.rest.channel.createMessage(Snowflake(LXV_BOT_UPDATE_CHANNEL_ID)) {
                content = "LXV Bot is online"
            }
            val curTime = p.id.timeStamp.toEpochMilli()
            val reminderCol = db.getCollection<StoredReminder>(StoredReminder.DB_NAME)
            val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
            val reminders = reminderCol.find().toList()
            reminders.forEach {
                client.launch {
                    val msgTime = Snowflake(it.srcMsg).timeStamp.toEpochMilli()
                    if (curTime < it.reminderTime) {
                        delay(it.reminderTime - curTime)
                    }
                    val check = getUserFromDB(
                        Snowflake(it.otherData),
                        null,
                        userCol
                    )
                    when (it.category) {
                        "rpg" -> {
                            val reminderSetting = check.rpg.rpgReminders[it.type]
                            val reminder = RPGCommand.findReminder(listOf(it.type), false)
                            if (reminderSetting == null || reminder == null) {
                                println("wtf should not be null")
                            } else {
                                if (msgTime == reminderSetting.lastUse && reminderSetting.enabled) {
                                    client.rest.channel.createMessage(Snowflake(it.channelId)) {
                                        messageReference = Snowflake(it.srcMsg)
                                        content = "RPG ${
                                            reminder.responseName(
                                                reminder,
                                                listOf(reminder.name)
                                            )
                                        } cooldown is done"
                                    }
                                }
                            }

                        }
                        "tacoshack" -> {
                            val reminder = TacoCommand.findTacoReminder(it.type)
                            if (reminder == null) {
                                println("wtf should not be null")
                            } else {
                                val reminderSetting = reminder.prop.get(check.taco.tacoReminders)
                                if (msgTime == reminderSetting.lastUse && reminderSetting.enabled) {
                                    client.rest.channel.createMessage(Snowflake(it.channelId)) {
                                        messageReference = Snowflake(it.srcMsg)
                                        content =
                                            "Taco Shack ${reminder.name.toLowerCase().capitalize()} cooldown is done"
                                    }
                                }
                            }
                        }
                    }
                    reminderCol.deleteOne(StoredReminder::srcMsg eq it.srcMsg)
                }
            }
        }

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
        if (mCE.message.author?.id?.value == RPG_BOT_ID) {
            handleRPGMessage(mCE)
        }
        if (mCE.message.author?.isBot == true) return
        if (mCE.guildId == null) {
            sendMessage(mCE.message.channel, "I don't do DMs, sorry <:pualOwO:782542201837322292>")
            return
        }
        if (mCE.message.content.startsWith("$RPG_PREFIX ", true)) {
            handleRPGCommand(mCE)
        }
        if (mCE.message.content.startsWith(TACO_SHACK_PREFIX, false)) {
            handleTacoCommand(
                mCE,
                mCE.message.content.drop(TACO_SHACK_PREFIX.length).trim().split(Pattern.compile("\\s+"))
            )
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
        const val TACO_SHACK_PREFIX = "ts"
        const val DB_NAME = "lxv"
        const val HAKI_ID = 292483348738080769
        const val ERYS_ID = 412812867348463636
        const val MEE6_ID = 159985870458322944
        const val RPG_BOT_ID = 555955826880413696
        const val LEVEL_UP_CHANNEL_ID = 763523136238780456
        const val LXV_BOT_UPDATE_CHANNEL_ID = 816768818088116225
        const val LXV_SERVER_ID = 714152739252338749
        private const val CHECKMARK_EMOJI = "\u2705"
        private const val CROSSMARK_EMOJI = "\u274c"

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

        fun getChannelIdFromString(s: String?): Long? {
            return if (s == null) {
                null
            } else if (s.toLongOrNull() != null) {
                s.toLong()
            } else if (s.startsWith("<#") && s.endsWith(">")) {
                s.drop(2).dropLast(1).toLongOrNull()
            } else {
                null
            }
        }

        fun getCheckmarkOrCross(checkmark: Boolean): String = if (checkmark) CHECKMARK_EMOJI else CROSSMARK_EMOJI
    }

}