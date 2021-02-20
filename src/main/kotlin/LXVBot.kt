import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import java.time.Instant
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.roundToLong


class LXVBot(private val client: Kord, val db: MongoDatabase) {

    private val lootboxTypes = listOf("c", "common", "u", "uncommon", "r", "rare", "ep", "epic", "ed", "edgy")
    private val lootboxAliases = listOf("lb", "lootbox")


    private val reminders = listOf(
        ReminderType("hunt", listOf(), 60_000, true, { args ->
            if (args.drop(1).firstOrNull()?.toLowerCase() in listOf("t", "together")) {
                "hunt together"
            } else {
                name
            }
        }),
        ReminderType("daily", listOf(), 24 * 3600_000, true),
        ReminderType("buy", listOf("lootbox", "lb"), 3 * 3600_000, false, { "Buy Lootbox" }) { args ->
            args.size >= 3 && args[0].toLowerCase() == "buy" && args[1].toLowerCase() in lootboxTypes && args[2].toLowerCase() in lootboxAliases
        },
        ReminderType("adventure", listOf("adv"), 3600_000, true),
        ReminderType("training", listOf("tr"), 15 * 60_000, true),
        ReminderType("quest", listOf("epic"), 6 * 3600_000, true) { args ->
            if (args.first().toLowerCase() == "quest") {
                true
            } else {
                args.size >= 2 && args.first().toLowerCase() == "epic" && args[1].toLowerCase() == "quest"
            }
        },
        ReminderType("duel", listOf(), 2 * 3600_000, false),
        ReminderType(
            "chop",
            listOf(
                "axe",
                "bowsaw",
                "chainsaw",
                "fish",
                "net",
                "boat",
                "bigboat",
                "pickup",
                "ladder",
                "tractor",
                "greenhouse",
                "mine",
                "pickaxe",
                "drill",
                "dynamite"
            ),
            300_000,
            true,
            { it.first() }
        ),
        ReminderType("horse", listOf(), 24 * 3600_000, true, { "Horse Breeding/Race" }) { args ->
            if (args.size < 2) {
                false
            } else {
                if (args[1].toLowerCase() == "race") {
                    true
                } else if (args.size >= 3) {
                    args[2].toLowerCase() in listOf(
                        "breed",
                        "breeding"
                    ) && args[3].toLongOrNull() == null && getUserIdFromString(args[3]) != null
                } else {
                    false
                }
            }
        },
        ReminderType("arena", listOf("big"), 24 * 3600_000, true) { args ->
            if (args.first().toLowerCase() == "arena") {
                true
            } else {
                args.size >= 3
                        && args.first().toLowerCase() == "big"
                        && args[1].toLowerCase() == "arena"
                        && args[2].toLowerCase() == "join"
            }
        },
        ReminderType("miniboss", listOf("not"), 12 * 3600_000, true) { args ->
            if (args.first().toLowerCase() == "miniboss") {
                true
            } else {
                args.size >= 4
                        && args.first().toLowerCase() == "not"
                        && args[1].toLowerCase() == "so"
                        && args[2].toLowerCase() == "mini"
                        && args[3].toLowerCase() == "boss"
            }
        },
    )

    suspend fun startup() {
        client.on<MessageCreateEvent> {
            client.launch {
                handleMessage(this@on)
            }
        }
    }

    private fun findReminder(args: List<String>, strict: Boolean = true): ReminderType? {
        val cmd = args.firstOrNull()?.toLowerCase() ?: return null
        for (reminder in reminders) {
            if (cmd == reminder.name || cmd in reminder.aliases) {
                if (!strict || reminder.validator(args)) {
                    return reminder
                }
            }
        }
        return null
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
        if (args.isEmpty()) {
            mCE.message.reply {
                content = "w h y"
                allowedMentions {
                    repliedUser = false
                }
            }
        } else {
            suspend fun err(msg: String = "") {
                mCE.message.reply {
                    content = "Invalid Syntax: $msg <:PaulOwO:721154434297757727>"
                    allowedMentions {
                        repliedUser = false
                    }
                }
            }
            when (args.first()) {
                "rpg" -> {
                    if (args.size < 2) {
                        err("Not enough args for any of the commands")
                    }
                    when (args[1]) {
                        "enable", "disable", "reset" -> {
                            if (args.size < 3) err("Not enough args for ${args[1]}") else {
                                val reminder = findReminder(listOf(args[2]), false)
                                if (reminder != null) {
                                    val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
                                    val user = getUserFromDb(mCE.message.author!!.id, userCol)
                                    if (args[1] == "reset") {
                                        val setting = user.rpg.rpgReminders[reminder.name]
                                        if (setting == null) {
                                            user.rpg.rpgReminders[reminder.name] = Reminder(true)
                                            mCE.message.reply {
                                                content = "Reset and enabled!"
                                                allowedMentions {
                                                    repliedUser = false
                                                }
                                            }
                                        } else {
                                            user.rpg.rpgReminders[reminder.name] = setting.copy(lastUse = 0L)
                                            mCE.message.reply {
                                                content =
                                                    "Reset the Cooldown! Note: The bot may still remind you from your previous usages"
                                                allowedMentions {
                                                    repliedUser = false
                                                }
                                            }
                                        }
                                        userCol.replaceOne(LXVUser::_id eq user._id, user)
                                    } else {
                                        val enable = args[1] == "enable"
                                        val setting = user.rpg.rpgReminders[reminder.name]
                                        if (setting == null) {
                                            user.rpg.rpgReminders[reminder.name] = Reminder(enable)

                                        } else {
                                            user.rpg.rpgReminders[reminder.name] = setting.copy(enabled = enable)
                                        }
                                        userCol.replaceOne(LXVUser::_id eq user._id, user)
                                        mCE.message.reply {
                                            content = "${if (enable) "en" else "dis"}abled!"
                                            allowedMentions {
                                                repliedUser = false
                                            }
                                        }
                                    }
                                } else {
                                    err("Could not find reminder type")
                                }
                            }
                        }
                        "patreon", "p" -> {
                            val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
                            val user = getUserFromDb(mCE.message.author!!.id, userCol)
                            if (args.size == 2) {
                                mCE.message.reply {
                                    content = "Current RPG Patreon Reduction is ${100 * (1 - user.rpg.patreonMult)}%"
                                    allowedMentions {
                                        repliedUser = false
                                    }
                                }
                            } else {
                                val new = when (args[2]) {
                                    "donator", "basic", "10%", "10", "0.9" -> 0.9 to "Regular. 10% reduced cooldowns"
                                    "epic", "20%", "20", "0.8" -> 0.8 to "Epic. 20% reduced cooldowns"
                                    "super", "35%", "35", "0.65" -> 0.65 to "Super. 35% reduced cooldowns"
                                    else -> 1.0 to "None. Regular cooldowns"
                                }
                                val newUser = user.copy(rpg = user.rpg.copy(patreonMult = new.first))
                                userCol.replaceOne(LXVUser::_id eq user._id, newUser)
                                mCE.message.reply {
                                    content = "RPG Patreon Set to ${new.second}"
                                    allowedMentions {
                                        repliedUser = false
                                    }
                                }
                            }
                        }
                        "ppatreon", "partner", "pp" -> {
                            val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
                            val user = getUserFromDb(mCE.message.author!!.id, userCol)
                            if (args.size == 2) {
                                mCE.message.reply {
                                    content = "Current RPG Patreon Reduction is ${100 * (1 - user.rpg.partnerPatreon)}%"
                                    allowedMentions {
                                        repliedUser = false
                                    }
                                }
                            } else {
                                val new = when (args[2]) {
                                    "donator", "basic", "10%", "10", "0.9" -> 0.9 to "Regular. 10% reduced cooldowns"
                                    "epic", "20%", "20", "0.8" -> 0.8 to "Epic. 20% reduced cooldowns"
                                    "super", "35%", "35", "0.65" -> 0.65 to "Super. 35% reduced cooldowns"
                                    else -> 1.0 to "None. Regular cooldowns"
                                }
                                val newUser = user.copy(rpg = user.rpg.copy(partnerPatreon = new.first))
                                userCol.replaceOne(LXVUser::_id eq user._id, newUser)
                                mCE.message.reply {
                                    content = "RPG Partner Patreon Set to ${new.second}"
                                    allowedMentions {
                                        repliedUser = false
                                    }
                                }
                            }
                        }
                        else -> {
                            err("Not a valid rpg subcommand")
                        }
                    }
                }
                else -> {
                    err("Only RPG commands supported currently")
                }
            }
        }
    }

    private fun handleRPGCommand(mCE: MessageCreateEvent) {
        val args = mCE.message.content.split(Pattern.compile("\\s+")).drop(1)
        val reminder = findReminder(args)
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


    private fun getUserFromDb(id: Snowflake, col: MongoCollection<LXVUser>): LXVUser {
        val user = col.findOne(LXVUser::_id eq id.value)
        return if (user == null) {
            val new = LXVUser(id.value)
            col.insertOne(new)
            new
        } else {
            user
        }
    }

    private fun getUserIdFromString(s: String?): Long? {
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

    companion object {
        const val BOT_PREFIX = "+"
    }

}


fun Snowflake.toInstant(): Instant = Instant.ofEpochMilli((value ushr 22) + 1420070400000L)
