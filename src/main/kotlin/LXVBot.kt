import commands.*
import rpg.RPGCommand
import commands.meta.HelpCommand
import commands.util.BotCommand
import dev.kord.common.Color
import dev.kord.common.entity.AllowedMentionType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.reply
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.allowedMentions
import dev.kord.rest.builder.message.create.embed
import entities.LXVUser
import entities.concurrency.LockedMap
import entities.StoredReminder
import entities.UserDailyStats
import entities.UserDailyStats.Companion.countBattle
import entities.UserDailyStats.Companion.handleOwOSaid
import entities.concurrency.LockedData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import moderation.PicBan
import moderation.customs.*
import moderation.handleMee6LevelUpMessage
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import owo.commands.DeleteOwOCount
import owo.commands.RecruitCmd
import owo.commands.CancelRecruit
import rpg.RPGCommand.handleRPGCommand
import rpg.RPGCommand.handleRPGMessage
import rpg.RPGReminderType
import taco.TacoCommand
import taco.TacoCommand.handleTacoCommand
import taco.TacoReminderType
import java.util.*
import java.util.regex.Pattern
import kotlin.math.max


class LXVBot(val client: Kord, mongoCon: CoroutineClient) {

    val db = mongoCon.getDatabase(LXV_DB_NAME)
    val hakiDb = mongoCon.getDatabase(HAKI_DB_NAME)
    val lastOwOMessage = LockedData(Clock.System.now().toEpochMilliseconds())
    val owoTimestamps = LockedMap<Snowflake, Long>()
    private var started = false

    val commands = listOf(
        DeleteOwOCount,
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
        AssignRole,
        RemoveRole,
        RoleColor,
        RoleName,
        RecruitCmd,
        CancelRecruit,
    )

    suspend fun startup() {
        client.on<ReadyEvent> {
            val p = client.rest.channel.createMessage(LXV_BOT_UPDATE_CHANNEL_ID) {
                content = "$BOT_NAME is online"
            }
            val curTime = p.id.timestamp.toEpochMilliseconds()
            val reminderCol = db.getCollection<StoredReminder>(StoredReminder.DB_NAME)
            val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
            if (!started) {
                started = true
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

                            "recruitment" -> {
                                RecruitCmd.handleExpirationReminder(this@LXVBot, it)
                            }
                        }
                        reminderCol.deleteOne(StoredReminder::srcMsg eq it.srcMsg)
                    }
                }
            }
        }
        client.on<ReactionAddEvent> {
            if (emoji in listOf(
                    ReactionEmoji.Unicode(CHECKMARK_EMOJI),
                    ReactionEmoji.Unicode(CROSSMARK_EMOJI)
                )
            ) {
                val msg = getMessage()
                if (msg.author?.id == client.selfId) {
                    val embed = msg.embeds.firstOrNull()
                    if (embed?.author?.name == userId.toString() && getUserIdFromString(embed.footer!!.text) != null
                        && embed.color?.rgb == 0x0000FF
                    ) {
                        when (embed.title) {
                            CancelRecruit.embedTitle -> {
                                if (emoji == ReactionEmoji.Unicode(CHECKMARK_EMOJI)) {
                                    CancelRecruit.confirmDeletion(this@LXVBot, msg, guildId!!)
                                } else {
                                    CancelRecruit.cancelDeletion(this@LXVBot, msg)
                                }
                            }

                            DeleteOwOCount.embedTitle -> {
                                if (emoji == ReactionEmoji.Unicode(CHECKMARK_EMOJI)) {
                                    DeleteOwOCount.confirmDeletion(this@LXVBot, msg, guildId!!)
                                } else {
                                    DeleteOwOCount.cancelDeletion(this@LXVBot, msg)
                                }
                            }
                        }

                    }
                }
            }
        }
        client.on<MessageCreateEvent> {
            handleMessage(this)
        }
    }


    private suspend fun handleMessage(mCE: MessageCreateEvent) {
        if (mCE.message.author?.id == client.selfId) return
        if (mCE.guildId == null) {
            sendMessage(mCE.message.channel, "I don't do DMs, sorry <:pualOwO:782542201837322292>")
            return
        }
        if (mCE.message.author?.id == MEE6_ID && mCE.message.channelId == LEVEL_UP_CHANNEL_ID) {
            handleMee6LevelUpMessage(mCE)
        }
        if (mCE.message.author?.id == RPG_BOT_ID) {
            handleRPGMessage(mCE)
        }
        if (mCE.message.author?.id == OWO_ID || mCE.message.webhookId == OWO_ID) {
            lastOwOMessage.adjustData { prevRecent ->
                max(prevRecent, mCE.message.id.timestamp.toEpochMilliseconds())
            }
            handleOwOMessage(mCE)
        }
        if (mCE.message.author?.isBot == true) return
        // owo counting is like highest priority i guess
        val maybeCountOwO = mCE.message.content.contains("owo", true) || mCE.message.content.contains("uwu", true)
        if (mCE.message.content.startsWith(GLOBAL_OWO_PREFIX, ignoreCase = true)) {
            handleOWOCommand(
                mCE,
                mCE.message.content.drop(GLOBAL_OWO_PREFIX.length).trim(),
                maybeCountOwO,
            )
        } else if (mCE.message.content.startsWith(LXV_OWO_PREFIX, ignoreCase = true)) {
            handleOWOCommand(
                mCE,
                mCE.message.content.drop(LXV_OWO_PREFIX.length).trim(),
                maybeCountOwO,
            )
        } else if (maybeCountOwO) {
            try {
                handleOwOSaid(mCE.message.id.timestamp, mCE.message.author!!, mCE.guildId!!)
            } catch (npe: NullPointerException) {
                // there's some issue here that occurs when the message gets deleted relatively quicky (e.g. nqn emojis) not sure exactly what comes up null and where though
                println(mCE)
            }
        }



        if (mCE.message.channelId == VERIFY_CHANNEL_ID && mCE.message.content.lowercase() == "owo") {
            val roles = mCE.member!!.roleIds
            if (OWO_ACCESS_ROLE_ID !in roles && OWO_VERIFY_ROLE_ID !in roles) {
                mCE.member!!.addRole(OWO_VERIFY_ROLE_ID, "They said owo in #verify")
            }
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
        if (mCE.message.mentionedUserIds.contains(client.selfId) && mCE.message.content.contains("<@${client.selfId}>")) {
            reply(mCE.message, "Hi, Welcome to LXV!\n$BOT_NAME prefix is $BOT_PREFIX")
        }


    }

    private suspend fun handleDM(mCE: MessageCreateEvent) {
        if (mCE.message.content.takeLast(4).lowercase(Locale.getDefault()) == "when") {
            sendMessage(mCE.message.channel, "when", 10_000)
            return
        }

        client.rest.channel.createMessage(DM_CHANNEL_ID) {
            content = "$BOT_NAME is online"

            embed {
                title = mCE.message.author?.tag ?: "no author"

                description = mCE.message.content
                if (mCE.message.author != null) {
                    footer {
                        text = mCE.message.author!!.id.toString()
                    }
                }
            }
        }


    }

    private suspend fun handleOWOCommand(mCE: MessageCreateEvent, msg: String, hasOwO: Boolean) {

        when (val cmd = msg.split(Pattern.compile("\\s")).firstOrNull()) {
            "points" -> handleOwOSaid(mCE.message.id.timestamp, mCE.message.author!!, mCE.guildId!!)
            in UserDailyStats.owoCommands -> {}
            else -> if (hasOwO) handleOwOSaid(mCE.message.id.timestamp, mCE.message.author!!, mCE.guildId!!)
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
                            countBattle(mCE.message.id.timestamp, Snowflake(id), mCE.guildId!!)
                        }
                    }
                }
            }
        }
    }


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

    internal suspend inline fun log(
        embedBuilder: EmbedBuilder.() -> Unit,
    ) {
        client.rest.channel.createMessage(LOG_CHANNEL_ID) {
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
        val BOT_NAME = System.getenv("lxv-bot-name")!!
        val BOT_PREFIX = System.getenv("lxv-prefix")!!

        val LXV_PINK = Color(0xFFD1DC)
        val LXV_MAGENTA = Color(0xE30F76)
        val LXV_BRIGHT_TEAL = Color(0x6BFFE8)
        val LXV_MINT = Color(0xA2D6BF)
        val LXV_DARK_TEAL = Color(0x3F95A2)
        val LXV_TEAL = Color(0x5BBFBD)


        const val RPG_PREFIX = "rpg"
        const val TACO_SHACK_PREFIX = "ts"
        const val LXV_OWO_PREFIX = "h"
        const val GLOBAL_OWO_PREFIX = "owo"
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
        val LOG_CHANNEL_ID = Snowflake(991966009441406976)
        val DM_CHANNEL_ID = Snowflake(1054655140159819837)

        // guild ids
        val LXV_GUILD_ID = Snowflake(714152739252338749)

        // role ids
        val OWO_VERIFY_ROLE_ID = Snowflake(841696472461344858)
        val OWO_ACCESS_ROLE_ID = Snowflake(714173846873309224)
        val RPG_PING_ROLE_ID = Snowflake(795936961344831549)
        val ADMIN_ROLE_ID = Snowflake(714165560853790741)
        val MOD_ROLE_ID = Snowflake(714197482699227265)
        val LXV_MEMBER_ROLE_ID = Snowflake(767034360640700427)
        val LXV_RECRUIT_ROLE_ID = Snowflake(769135734086959104)
        val RECUIT_BAN_ROLE_ID = Snowflake(991975489801551932)


        private const val CHECKMARK_EMOJI = "\u2705"
        private const val CROSSMARK_EMOJI = "\u274c"

        val LXV_NEON_CHVRCHES_EMOJI = ReactionEmoji.Custom(Snowflake(945535196239921192), "lxv", true)
        val NEW_LXV_GIF_EMOJI = ReactionEmoji.Custom(Snowflake(1052139469798637659), "newlxvgif", true)
        val LXV_HEDGE_WUV_EMOJI = ReactionEmoji.Custom(Snowflake(1080420413630332928), "lxvhedgewuv", false)
        val LXV_HEDGE_HOLD_EMOJI = ReactionEmoji.Custom(Snowflake(821431807425642567), "LXVhedge", false)
        val LXV_HEDGE_EMOJI = ReactionEmoji.Custom(Snowflake(822377757958340640), "LXVhedgeily", false)
        val LXV_HEDGE_PEAK_EMOJI = ReactionEmoji.Custom(Snowflake(822030768981016577), "LXVhedgepeek", false)
        val LXV_HEDGE_SIGH_EMOJI = ReactionEmoji.Custom(Snowflake(846997428699136060), "hedgesigh", false)
        val LXV_HEART_EMOJI = ReactionEmoji.Custom(Snowflake(821431844276666389), "LXVheart", false)
        val LXV_PEEK_EMOJI = ReactionEmoji.Custom(Snowflake(822006949834260511), "LXVpeek", false)
        val LXV_CRY_EMOJI = ReactionEmoji.Custom(Snowflake(822339762484281366), "LXVcry", false)
        val LXV_SQUISH_EMOJI = ReactionEmoji.Custom(Snowflake(821623141972312074), "LXVsquish", false)
        val ARROW_EMOJI = ReactionEmoji.Custom(Snowflake(1080394763494232074), "p_arrowright03", false)


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

        fun getRoleIdFromString(s: String?): Snowflake? {
            val id = if (s == null) {
                null
            } else if (s.toULongOrNull() != null) {
                s.toULong()
            } else if (s.startsWith("<@&") && s.endsWith(">")) {
                s.drop(3).dropLast(1).toULongOrNull()
            } else {
                null
            }
            return if (id == null) null else Snowflake((id))
        }

        fun getCheckmarkOrCross(checkmark: Boolean): String = if (checkmark) CHECKMARK_EMOJI else CROSSMARK_EMOJI

        fun Instant.toOwODate(): LocalDate = toLocalDateTime(PST).date
    }

}