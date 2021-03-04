package rpg

import LXVBot
import LXVUser
import Reminder
import commands.utils.*
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import org.litote.kmongo.eq
import kotlin.math.roundToInt

object RPGCommand : BotCommand {

    override val name: String
        get() = "rpg"
    override val description: String
        get() = "View and change your settings for RPG Reminders"
    override val category: CommandCategory
        get() = CommandCategory.OTHER_BOTS
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(
                listOf(Argument("enable", ArgumentType.EXACT), Argument("reminder")),
                "Enables the selected RPG reminder"
            ),
            CommandUsage(
                listOf(Argument("disable", ArgumentType.EXACT), Argument("reminder")),
                "Disables the selected RPG reminder"
            ),
            CommandUsage(
                listOf(Argument("reset", ArgumentType.EXACT), Argument("reminder")),
                "Resets your cooldown for the selected RPG reminder"
            ),
            CommandUsage(
                listOf(Argument(listOf("patreon", "p", "ppatreon", "partner", "pp"))),
                "Checks your current RPG patreon status"
            ),
            CommandUsage(
                listOf(Argument(listOf("patreon", "p")), Argument("patreon level")),
                "Sets your patreon level to the specified value"
            ),
            CommandUsage(
                listOf(Argument(listOf("ppatreon", "partner", "pp")), Argument("patreon level")),
                "Sets your Partner's Patreon level to the specified value"
            ),
            CommandUsage(
                listOf(Argument("info", ArgumentType.EXACT)),
                "Shows all available reminder types"
            ),
        )

    private val lootboxTypes = listOf("c", "common", "u", "uncommon", "r", "rare", "ep", "epic", "ed", "edgy")
    private val lootboxAliases = listOf("lb", "lootbox")
    private val adventureAliases = listOf("adv", "adventure")
    private val petAdvTypes = listOf("find", "learn", "drill")
    private val workAliases = listOf(
        "chop",
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
    )
    private val patreonLevels = listOf(
        RPGPatreonLevel("regular", listOf("donator", "basic", "10%", "10", "0.9"), 0.9),
        RPGPatreonLevel("epic", listOf("20%", "20", "0.8"), 0.8),
        RPGPatreonLevel("super", listOf("35%", "35", "0.65"), 0.65),
        RPGPatreonLevel("none", listOf("0%", "0", "0.0"), 1.0),
    )

    private val reminders = listOf(
        RPGReminderType("hunt", listOf(), 60_000, true, { args ->
            if (args.drop(1).firstOrNull()?.toLowerCase() in listOf("t", "together")) {
                "hunt together"
            } else {
                name
            }
        }),
        RPGReminderType("pet", listOf("pets"), 4 * 3600_000, false, { "Pet Adventure" }) { args ->
            args.size >= 4 && args[1].toLowerCase() in adventureAliases && args[2].toLowerCase() in petAdvTypes
        },
        RPGReminderType("daily", listOf(), 24 * 3600_000, true),
        RPGReminderType("buy", listOf("lootbox", "lb"), 3 * 3600_000, false, { "Buy Lootbox" }) { args ->
            args.size >= 3 && args[0].toLowerCase() == "buy" && args[1].toLowerCase() in lootboxTypes && args[2].toLowerCase() in lootboxAliases
        },
        RPGReminderType("adventure", listOf("adv"), 3600_000, true),
        RPGReminderType("training", listOf("tr"), 15 * 60_000, true),
        RPGReminderType("quest", listOf("epic"), 6 * 3600_000, true) { args ->
            if (args.first().toLowerCase() == "quest") {
                true
            } else {
                args.size >= 2 && args.first().toLowerCase() == "epic" && args[1].toLowerCase() == "quest"
            }
        },
        RPGReminderType("duel", listOf(), 2 * 3600_000, false),
        RPGReminderType(
            "work",
            workAliases,
            300_000,
            true,
            { it.first().capitalize() }
        ) { args ->
            args.first().toLowerCase() in workAliases
        },
        RPGReminderType("horse", listOf(), 24 * 3600_000, true, { "Horse Breeding/Race" }) { args ->
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
        RPGReminderType("arena", listOf("big"), 24 * 3600_000, true) { args ->
            if (args.first().toLowerCase() == "arena") {
                true
            } else {
                args.size >= 3
                        && args.first().toLowerCase() == "big"
                        && args[1].toLowerCase() == "arena"
                        && args[2].toLowerCase() == "join"
            }
        },
        RPGReminderType("miniboss", listOf("not"), 12 * 3600_000, true) { args ->
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

    fun findReminder(args: List<String>, strict: Boolean = true): RPGReminderType? {
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

    private fun findPatreonLevel(p: String): RPGPatreonLevel {
        for (level in patreonLevels) {
            if (p == level.name || p in level.aliases) {
                return level
            }
        }
        return patreonLevels.last()
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
        when (args[0].toLowerCase()) {
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
                        content = "Current RPG Patreon Reduction is ${100 * (1 - user.rpg.patreonMult)}%\n" +
                                "Current RPG Partner Patreon Reduction is ${100 * (1 - user.rpg.partnerPatreon)}%"
                        allowedMentions {
                            repliedUser = false
                        }
                    }
                } else {
                    val new = findPatreonLevel(args[1].toLowerCase())

                    val newUser = user.copy(rpg = user.rpg.copy(patreonMult = new.multiplier))
                    userCol.replaceOne(LXVUser::_id eq user._id, newUser)
                    mCE.message.reply {
                        content =
                            "RPG Patreon Set to ${new.name}. ${(100 * (1 - new.multiplier)).roundToInt()}% reduced cooldowns"
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
                        content = "Current RPG Patreon Reduction is ${100 * (1 - user.rpg.patreonMult)}%\n" +
                                "Current RPG Partner Patreon Reduction is ${100 * (1 - user.rpg.partnerPatreon)}%"
                        allowedMentions {
                            repliedUser = false
                        }
                    }
                } else {
                    val new = findPatreonLevel(args[1].toLowerCase())
                    val newUser = user.copy(rpg = user.rpg.copy(partnerPatreon = new.multiplier))
                    userCol.replaceOne(LXVUser::_id eq user._id, newUser)
                    mCE.message.reply {
                        content =
                            "RPG Partner Patreon Set to ${new.name}. ${(100 * (1 - new.multiplier)).roundToInt()}% reduced cooldowns"
                        allowedMentions {
                            repliedUser = false
                        }
                    }
                }
            }
            "info" -> {
                val reminderInfo = StringBuilder()
                val patreonInfo = StringBuilder()
                for (reminder in reminders) {
                    reminderInfo.append(
                        "${reminder.name.capitalize()} - ${
                            reminder.responseName(
                                reminder,
                                listOf(reminder.name)
                            )
                        }\n"
                    )
                    if (reminder.aliases.isNotEmpty()) {
                        reminderInfo.append("Aliases: ${reminder.aliases.joinToString(", ")}\n")
                    }
                    reminderInfo.append("\n")
                }
                for (level in patreonLevels) {
                    patreonInfo.append("${level.name.capitalize()}: ${(100 * (1 - level.multiplier)).roundToInt()}% Reduced Cooldowns\n")
                    patreonInfo.append("Aliases: ${level.aliases.joinToString(", ")}\n\n")
                }
                reply(mCE.message) {
                    title = "${LXVBot.BOT_NAME} RPG Command Info"
                    field {
                        name = "Available Reminder Types"
                        value = reminderInfo.toString()
                    }

                    field {
                        name = "Availabe Patreon Levels"
                        value = patreonInfo.toString()
                    }
                }
            }
            else -> {
                err("Not a valid rpg subcommand")
            }
        }
    }
}