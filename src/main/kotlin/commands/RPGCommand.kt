package commands

import LXVBot
import LXVUser
import Reminder
import ReminderType
import commands.utils.BotCommand
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.litote.kmongo.eq
import toInstant
import kotlin.math.max
import kotlin.math.roundToLong

object RPGCommand : BotCommand {

    override val name: String
        get() = "rpg"
    override val description: String
        get() = "View and change your settings for RPG Reminders"

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
                    ) && args[3].toLongOrNull() == null && LXVBot.getUserIdFromString(args[3]) != null
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

    fun findReminder(args: List<String>, strict: Boolean = true): ReminderType? {
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

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        suspend fun err(msg: String = "") {
            mCE.message.reply {
                content = "Invalid Syntax: $msg <:PaulOwO:721154434297757727>"
                allowedMentions {
                    repliedUser = false
                }
            }
        }
        if (args.isEmpty()) {
            err("Not enough args for any of the commands")
        }
        when (args[0]) {
            "enable", "disable", "reset" -> {
                if (args.size < 2) err("Not enough args for ${args[0]}") else {
                    val reminder = findReminder(listOf(args[1]), false)
                    if (reminder != null) {
                        val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
                        val user = getUserFromDb(mCE.message.author!!.id, userCol)
                        if (args[0] == "reset") {
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
                            val enable = args[0] == "enable"
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
                if (args.size == 1) {
                    mCE.message.reply {
                        content = "Current RPG Patreon Reduction is ${100 * (1 - user.rpg.patreonMult)}%"
                        allowedMentions {
                            repliedUser = false
                        }
                    }
                } else {
                    val new = when (args[1]) {
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
                if (args.size == 1) {
                    mCE.message.reply {
                        content = "Current RPG Partner Patreon Reduction is ${100 * (1 - user.rpg.partnerPatreon)}%"
                        allowedMentions {
                            repliedUser = false
                        }
                    }
                } else {
                    val new = when (args[1]) {
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
}