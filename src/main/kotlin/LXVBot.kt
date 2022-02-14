import commands.*
import rpg.RPGCommand
import commands.meta.HelpCommand
import commands.util.BotCommand
import dev.kord.common.entity.AllowedMentionType
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
import dev.kord.rest.builder.message.create.allowedMentions
import dev.kord.rest.builder.message.create.embed
import entities.LXVUser
import entities.StoredReminder
import entities.UserBattleCount.Companion.countBattle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
import rpg.RPGReminderType
import taco.TacoCommand
import taco.TacoCommand.handleTacoCommand
import taco.TacoReminderType
import java.util.regex.Pattern


class LXVBot(val client: Kord, mongoCon: CoroutineClient) {

    val db = mongoCon.getDatabase(LXV_DB_NAME)
    val hakiDb = mongoCon.getDatabase(HAKI_DB_NAME)

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
        Logout,
        BattleStat,
        BattleLeaderboard,
    )

    suspend fun startup() {
        client.on<ReadyEvent> {
            val p = client.rest.channel.createMessage(LXV_BOT_UPDATE_CHANNEL_ID) {
                content = "LXV Bot is online"
            }
            val curTime = p.id.timestamp.toEpochMilliseconds()
            val reminderCol = db.getCollection<StoredReminder>(StoredReminder.DB_NAME)
            val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
            val reminders = reminderCol.find().toList()
            reminders.forEach {
                client.launch {
                    val msgTime = it.srcMsg.timestamp.toEpochMilliseconds()
                    if (curTime < it.reminderTime) {
                        delay(it.reminderTime - curTime)
                    }
                    val check = getUserFromDB(
                        it.otherData, null, userCol
                    )
                    when (it.category) {
                        "rpg" -> {
                            val reminderSetting = check.rpg.rpgReminders[it.type]
                            val reminder = RPGReminderType.findReminder(it.type)
                            if (reminderSetting == null || reminder == null) {
                                println("wtf should not be null")
                            } else {
                                if (msgTime == reminderSetting.lastUse && reminderSetting.enabled) {
                                    client.rest.channel.createMessage(it.channelId) {
                                        messageReference = it.srcMsg
                                        content = reminder.getReminderMessage(it.oldMsg.split(" "))
                                    }
                                }
                            }

                        }
                        "tacoshack" -> {
                            val reminder = TacoReminderType.findTacoReminder(it.type)
                            if (reminder == null) {
                                println("wtf should not be null")
                            } else {
                                val reminderSetting = reminder.prop.get(check.taco.tacoReminders)
                                if (msgTime == reminderSetting.lastUse && reminderSetting.enabled) {
                                    client.rest.channel.createMessage(it.channelId) {
                                        messageReference = it.srcMsg
                                        content = reminder.getReminderMessage()
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
        if (mCE.message.author?.id == MEE6_ID && mCE.message.channelId == LEVEL_UP_CHANNEL_ID) {
            handleMee6LevelUpMessage(mCE)
        }
        if (mCE.message.author?.id == RPG_BOT_ID) {
            handleRPGMessage(mCE)
        }
        if (mCE.message.author?.id == OWO_ID) {
            handleOwOMessage(mCE)
        }
        if (mCE.message.author?.isBot == true) return
        if (mCE.message.channelId == VERIFY_CHANNEL_ID && mCE.message.content.lowercase() == "owo") {
            val roles = mCE.member!!.roleIds
            if (OWO_ACCESS_ROLE_ID !in roles && OWO_VERIFY_ROLE_ID !in roles) {
                mCE.member!!.addRole(OWO_VERIFY_ROLE_ID, "They said owo in #verify")
            }
        }
        if (mCE.guildId == null) {
            sendMessage(mCE.message.channel, "I don't do DMs, sorry <:pualOwO:782542201837322292>")
            return
        }
        if (mCE.message.content.startsWith("$RPG_PREFIX ", true)) {
            handleRPGCommand(mCE)
        }
        if (mCE.message.content.startsWith(TACO_SHACK_PREFIX, true)) {
            handleTacoCommand(
                mCE, mCE.message.content.drop(TACO_SHACK_PREFIX.length).trim().split(Pattern.compile("\\s+"))
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
        val cmd = args.first().lowercase()
        if (cmd.isBlank()) return
        lookupCMD(cmd)?.runCMD(this, mCE, args.drop(1))
    }

    private suspend fun handleOwOMessage(mCE: MessageCreateEvent) {
        val embeds = mCE.message.embeds
        if (embeds.isNotEmpty()) {
            val id = embeds.first().author?.iconUrl?.split("avatars/")?.last()?.split("/")?.first()?.toULongOrNull()
            val authorText = embeds.first().author?.name
            val fields = embeds.first().fields
            val desc = embeds.first().description
            if (id != null && fields.size >= 2) {
                if (fields.all { it.value.startsWith("L.") } || fields.all { it.value.startsWith("Lvl.") }) {
                    if (desc?.startsWith("Bet amount: ") != true && desc?.endsWith("\n`owo db` to decline the battle!") != true) {
                        if (authorText?.endsWith("'s pets") != true) {
                            countBattle(mCE.message.id, Snowflake(id), mCE.guildId!!)
                        }
                    }
                }
            }
        }
    }
//        reply(mCE.message)


    internal suspend inline fun reply(
        message: MessageBehavior,
        replyContent: String = "",
        ping: Boolean = false,
        rolePing: Boolean = false,
        userPing: Boolean = false,
        embedBuilder: EmbedBuilder.() -> Unit,
    ) {
        message.reply {
            content = replyContent

            allowedMentions {
                repliedUser = ping
                if (rolePing) {
                    add(AllowedMentionType.RoleMentions)
                }
                if (userPing) {
                    add(AllowedMentionType.UserMentions)
                }
            }
            embed(embedBuilder)
        }
    }

    internal suspend fun reply(
        message: MessageBehavior,
        replyContent: String = "",
        ping: Boolean = false,
        rolePing: Boolean = false,
        userPing: Boolean = false,
    ) {
        message.reply {
            content = replyContent
            allowedMentions {
                repliedUser = ping
                if (rolePing) {
                    add(AllowedMentionType.RoleMentions)
                }
                if (userPing) {
                    add(AllowedMentionType.UserMentions)
                }
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
        userID: Snowflake, u: User? = null, col: CoroutineCollection<LXVUser> = db.getCollection(LXVUser.DB_NAME)
    ): LXVUser {
        val query = col.findOne(LXVUser::_id eq userID)
        return if (query == null) {
            val user = if (u == null) {
                LXVUser(userID, client.getUser(userID)?.username ?: "Deleted User#${userID.value}")
            } else {
                LXVUser(userID, u.username)
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
        val BOT_PREFIX = System.getenv("lxv-prefix")!!
        const val RPG_PREFIX = "rpg"
        const val TACO_SHACK_PREFIX = "ts"
        val LXV_DB_NAME = System.getenv("lxv-db-name")!!
        val HAKI_DB_NAME = System.getenv("haki-db-name")!!

        // user ids
        val HAKI_ID = Snowflake(292483348738080769)
        val ERYS_ID = Snowflake(412812867348463636)
        val MEE6_ID = Snowflake(159985870458322944)
        val RPG_BOT_ID = Snowflake(555955826880413696)
        val OWO_ID = Snowflake(408785106942164992)

        // channel ids
        val LEVEL_UP_CHANNEL_ID = Snowflake(763523136238780456)
        val LXV_BOT_UPDATE_CHANNEL_ID = Snowflake(816768818088116225)
        val VERIFY_CHANNEL_ID = Snowflake(841698006800793620)

        // guild ids
        val LXV_GUILD_ID = Snowflake(714152739252338749)

        // role ids
        val OWO_VERIFY_ROLE_ID = Snowflake(841696472461344858)
        val OWO_ACCESS_ROLE_ID = Snowflake(714173846873309224)
        val RPG_PING_ROLE_ID = Snowflake(795936961344831549)


        private const val CHECKMARK_EMOJI = "\u2705"
        private const val CROSSMARK_EMOJI = "\u274c"

        val PST = TimeZone.of("America/Los_Angeles")

        fun getUserIdFromString(s: String?): Snowflake? {
            val id = if (s == null) {
                null
            } else if (s.toULongOrNull() != null) {
                s.toULong()
            } else if (s.startsWith("<@") && s.endsWith(">")) {
                if (s[2] == '!') {
                    s.drop(3).dropLast(1).toULongOrNull()
                } else {
                    s.drop(2).dropLast(1).toULongOrNull()
                }
            } else {
                null
            }
            return if (id == null) null else Snowflake(id)
        }

        fun getChannelIdFromString(s: String?): Snowflake? {
            val id = if (s == null) {
                null
            } else if (s.toULongOrNull() != null) {
                s.toULong()
            } else if (s.startsWith("<#") && s.endsWith(">")) {
                s.drop(2).dropLast(1).toULongOrNull()
            } else {
                null
            }
            return if (id == null) null else Snowflake(id)
        }

        fun getCheckmarkOrCross(checkmark: Boolean): String = if (checkmark) CHECKMARK_EMOJI else CROSSMARK_EMOJI

        fun Snowflake.toDate(): LocalDate = timestamp.toLocalDateTime(PST).date
    }

}