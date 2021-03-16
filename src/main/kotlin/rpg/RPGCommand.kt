package rpg

import LXVBot
import entities.LXVUser
import entities.Reminder
import commands.meta.HelpCommand
import commands.util.*
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Embed
import dev.kord.core.event.message.MessageCreateEvent
import entities.StoredReminder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.litote.kmongo.div
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object RPGCommand : BotCommand {

    override val name: String
        get() = "rpg"
    override val description: String
        get() = "View and change your settings for RPG Reminders"
    override val category: CommandCategory
        get() = CommandCategory.OTHER_BOTS
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(listOf(), "Shows this help screen"),
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
            CommandUsage(
                listOf(Argument(listOf("status", "settings", "stats", "stat"))),
                "Shows your reminder count and status for each RPG reminder"
            ),
        )

    private val togetherAliases = listOf("together", "t")
    private val hardmodeAliases = listOf("hardmode", "h")


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
            HelpCommand.runCMD(this, mCE, listOf(this@RPGCommand.name))
            return
        }
        when (args[0].toLowerCase()) {
            "enable", "disable" -> handleEnableDisableSubcommand(mCE, args.map { it.toLowerCase() })
            "reset" -> {
                handleResetSubcommand(mCE, args.map { it.toLowerCase() })
            }
            "patreon", "p" -> {
                val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
                val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
                if (args.size == 1) {
                    mCE.message.reply {
                        content = "Current RPG Patreon Reduction is ${100 * (1 - user.rpg.patreonMult)}%\n" +
                                "Current RPG Partner Patreon Reduction is ${100 * (1 - user.rpg.partnerPatreon)}%"
                        allowedMentions {
                            repliedUser = false
                        }
                    }
                } else {
                    val new = RPGPatreonLevel.findPatreonLevel(args[1].toLowerCase())

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
                val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
                if (args.size == 1) {
                    mCE.message.reply {
                        content = "Current RPG Patreon Reduction is ${100 * (1 - user.rpg.patreonMult)}%\n" +
                                "Current RPG Partner Patreon Reduction is ${100 * (1 - user.rpg.partnerPatreon)}%"
                        allowedMentions {
                            repliedUser = false
                        }
                    }
                } else {
                    val new = RPGPatreonLevel.findPatreonLevel(args[1].toLowerCase())
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
                for (reminder in RPGReminderType.values()) {
                    reminderInfo.append(reminder.getFormattedName())
                    if (reminder.aliases.isNotEmpty()) {
                        reminderInfo.append("Aliases: ${reminder.aliases.joinToString(", ")}\n")
                    }
                    reminderInfo.append("\n")
                }
                for (level in RPGPatreonLevel.values()) {
                    patreonInfo.append("${level.name.capitalize()}: ${(100 * (1 - level.multiplier)).roundToInt()}% Reduced Cooldowns\n")
                    patreonInfo.append("Aliases: ${level.aliases.joinToString(", ")}\n\n")
                }
                val self = client.getSelf()
                reply(mCE.message) {
                    title = "${LXVBot.BOT_NAME} RPG Command Info"
                    footer {
                        text =
                            "You can also use \"${LXVBot.BOT_PREFIX} rpg [enable/disable/reset] all\" to enable/disable/reset all of the cooldowns"
                        icon = self.avatar.url
                    }
                    field {
                        inline = true
                        name = "Available Reminder Types"
                        value = reminderInfo.toString()
                    }

                    field {
                        inline = true
                        name = "Available Patreon Levels"
                        value = patreonInfo.toString()
                    }
                }
            }
            "status", "settings", "stats", "stat" -> {
                val col = db.getCollection<LXVUser>(LXVUser.DB_NAME)
                val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, col)
                val self = client.getSelf()
                reply(mCE.message) {
                    author {
                        name = "${user.username}'s RPG Reminder Settings"
                        icon = mCE.message.author?.avatar?.url
                    }
                    for (reminder in RPGReminderType.values()) {
                        field {
                            inline = true
                            name = reminder.getFormattedName()
                            val r = user.rpg.rpgReminders[reminder.id]
                            value =
                                "Enabled: ${LXVBot.getCheckmarkOrCross(r?.enabled ?: false)}\nReminder Count: ${r?.count ?: 0}"
                        }
                    }
                    footer {
                        text =
                            "\"${LXVBot.BOT_PREFIX} rpg [enable/disable] <reminder>\" to set a specific reminder!\n" +
                                    "\"${LXVBot.BOT_PREFIX} rpg [enable/disable] all\" to set all reminders!"
                        icon = self.avatar.url
                    }
                }
            }
            "event" -> {
                if (args.size == 1) {
                    reply(mCE.message, "Current cooldown reduction is ${100 * (1 - RPGReminderType.EVENT_BONUS)}%")
                } else if (Permission.Administrator in mCE.member!!.getPermissions()) {
                    val pct = args[1].toDoubleOrNull()
                    if (pct != null) {
                        when (pct) {
                            in 0.0..0.8 -> {
                                RPGReminderType.EVENT_BONUS = 1 - pct
                                reply(mCE.message, "Set the event cooldown reduction to ${100 * pct}%")
                            }
                            in 0.0..80.0 -> {
                                RPGReminderType.EVENT_BONUS = 1 - pct / 100
                                reply(mCE.message, "Set the event cooldown reduction to ${pct}%")
                            }
                            else -> {
                                reply(mCE.message, "That number doesn't even make sense")
                            }
                        }

                    } else {
                        reply(mCE.message, "bad number")
                    }
                }
            }
            else -> {
                err("Not a valid rpg subcommand")
            }
        }
    }

    private suspend fun LXVBot.handleResetSubcommand(mCE: MessageCreateEvent, args: List<String>) {
        if (args.size < 2) {
            reply(mCE.message, "You need to specify which reminder you're resetting")
            return
        }
        val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
        if (args[1] == "all") {
            val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
            val rpgData = user.rpg.rpgReminders
            for (rpgReminder in RPGReminderType.values()) {
                if (rpgData[rpgReminder.id] == null) {
                    rpgData[rpgReminder.id] = Reminder()
                } else {
                    rpgData[rpgReminder.id] =
                        rpgData[rpgReminder.id]!!.copy(lastUse = 0L)
                }
            }
            userCol.updateOne(LXVUser::_id eq user._id, setValue(LXVUser::rpg / RPGData::rpgReminders, rpgData))
            reply(
                mCE.message,
                "Reset All the Cooldowns!"
            )
        } else {
            val reminder = RPGReminderType.findReminder(args[1])
            if (reminder != null) {
                val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
                val setting = user.rpg.rpgReminders[reminder.id]
                if (setting == null) {
                    user.rpg.rpgReminders[reminder.id] = Reminder(true)
                    reply(mCE.message, "Reset and Enabled")
                } else {
                    user.rpg.rpgReminders[reminder.id] = setting.copy(lastUse = 0L)
                    reply(
                        mCE.message,
                        "Reset the Cooldown!"
                    )
                }
                userCol.updateOne(
                    LXVUser::_id eq user._id,
                    setValue(LXVUser::rpg / RPGData::rpgReminders, user.rpg.rpgReminders)
                )
            } else {
                reply(mCE.message, "Could not find that RPG reminder")
            }
        }
    }

    private suspend fun LXVBot.handleEnableDisableSubcommand(mCE: MessageCreateEvent, args: List<String>) {
        if (args.size < 2) {
            reply(mCE.message, "You need to specify which reminder you're editing")
            return
        }
        val enable = args[0] == "enable"
        val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)

        if (args[1] == "all") {
            val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
            val rpgData = user.rpg.rpgReminders

            for (rpgReminder in RPGReminderType.values()) {
                if (rpgData[rpgReminder.id] == null) {
                    rpgData[rpgReminder.id] = Reminder(enable)
                } else {
                    rpgData[rpgReminder.id] = rpgData[rpgReminder.id]!!.copy(enabled = enable)
                }
            }
            userCol.updateOne(LXVUser::_id eq user._id, setValue(LXVUser::rpg / RPGData::rpgReminders, rpgData))
            reply(mCE.message, "All Rpg Reminders ${if (enable) "En" else "Dis"}abled!")
        } else {
            val reminder = RPGReminderType.findReminder(args[1])
            if (reminder != null) {
                val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
                val setting = user.rpg.rpgReminders[reminder.id]
                if (setting == null) {
                    user.rpg.rpgReminders[reminder.id] = Reminder(enable)
                } else {
                    user.rpg.rpgReminders[reminder.id] = setting.copy(enabled = enable)
                }
                userCol.replaceOne(LXVUser::_id eq user._id, user)
                reply(
                    mCE.message,
                    "RPG ${reminder.getFormattedName()} Reminder ${if (enable) "En" else "Dis"}abled!"
                )
            } else {
                reply(mCE.message, "Could not find that RPG reminder type")
            }
        }
    }

    internal suspend fun LXVBot.handleRPGCommand(mCE: MessageCreateEvent) {
        val args = run {
            val a = mCE.message.content.split(Pattern.compile("\\s+")).drop(1)
            if (a.firstOrNull()?.toLowerCase() == "ascended") {
                a.drop(1).map { it.toLowerCase() }
            } else {
                a.map { it.toLowerCase() }
            }
        }
        val reminder = RPGReminderType.findValidReminder(args)
        if (reminder != null) {
            val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
            val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
            val data = user.rpg.rpgReminders[reminder.id]
            val curTime = mCE.message.id.timeStamp.toEpochMilli()
            val pMult = if (reminder.patreonAffected) user.rpg.patreonMult else 1.0
            val eMult = if (reminder.eventAffected) RPGReminderType.EVENT_BONUS else 1.0
            val dif = if (reminder.id == "hunt") {
                if (args.drop(1).firstOrNull()?.toLowerCase() in togetherAliases) {
                    reminder.cooldownMS * max(user.rpg.patreonMult, user.rpg.partnerPatreon) * eMult
                } else if (args.drop(1).firstOrNull()?.toLowerCase() in hardmodeAliases
                    && args.drop(2).firstOrNull()?.toLowerCase() in togetherAliases
                ) {
                    reminder.cooldownMS * max(user.rpg.patreonMult, user.rpg.partnerPatreon) * eMult
                } else {
                    reminder.cooldownMS * pMult * eMult
                }
            } else {
                reminder.cooldownMS * pMult * eMult
            }

            if (data?.enabled == true && (curTime - data.lastUse > dif)
            ) {
                val reminderCol = db.getCollection<StoredReminder>(StoredReminder.DB_NAME)
                client.launch {
                    user.rpg.rpgReminders[reminder.id] = Reminder(data.enabled, curTime, data.count + 1)
                    userCol.replaceOne(LXVUser::_id eq user._id, user)
                    reminderCol.insertOne(
                        StoredReminder(
                            mCE.message.id.value,
                            curTime + dif.roundToLong(),
                            "rpg",
                            reminder.id,
                            mCE.message.channelId.value,
                            mCE.message.author!!.id.value,
                        )
                    )
                    delay(dif.roundToLong())
                    val check = getUserFromDB(
                        mCE.message.author!!.id,
                        mCE.message.author,
                        userCol
                    ).rpg.rpgReminders[reminder.id]
                    if (check?.lastUse == curTime && check.enabled) {
                        reply(mCE.message, reminder.getReminderMessage(args), true)
                    }
                    reminderCol.deleteOne(StoredReminder::srcMsg eq mCE.message.id.value)
                }
            }
        }
    }

    internal suspend fun LXVBot.handleEmbed(mCE: MessageCreateEvent, embed: Embed) {
        val field = embed.fields.firstOrNull()
        when {
            field?.value?.startsWith("The first player who types the following sentence will get") == true -> {
                val s = field.value.split("**").getOrNull(1)
                reply(mCE.message, s ?: "<@${LXVBot.HAKI_ID}>") {
                    description = s
                    color = if (description == null) Color(0xFF0000) else Color(0xFFFFFF)
                }
            }
            field?.value?.startsWith("Type **") == true -> {
                reply(mCE.message, "<@&${LXVBot.RPG_PING_ROLE_ID}> ${field.value.split("**")[1]}", rolePing = true)
            }
            field?.name?.startsWith("Type `") == true -> {
                reply(mCE.message, "<@&${LXVBot.RPG_PING_ROLE_ID}> ${field.name.split("`")[1]}", rolePing = true)
            }
            embed.footer?.text == "Type \"info\" to get information about pets" -> {
                var (happiness, hunger) = field!!.value.split("\n").map { it.split("**").last().trim().toInt() }
                val actions = mutableListOf<String>()
                reply(mCE.message) {
                    title = "Pet Taming Helper"
                    color = Color(0x406da2)
                    for (count in 1..6) {
                        val hungerGain = min(hunger, 20)
                        val happinessGain = min(100 - happiness, 10)
                        if (hungerGain > happinessGain) {
                            hunger -= hungerGain
                            actions += "feed"
                        } else {
                            happiness += happinessGain
                            actions += "pat"
                        }
                        val pct =
                            (((happiness - hunger) * 10000) / 85)
                                .coerceAtLeast(0)
                                .coerceAtMost(10000)
                                .toString()
                                .padStart(3, '0')


                        field {
                            name = actions.joinToString(" ")
                            value =
                                "Estimated Happiness: $happiness\nEstimated Hunger: $hunger\nEstimated Catch Odds: **${
                                    pct.dropLast(2)
                                }.${pct.takeLast(2)}%**"
                            inline = true
                        }
                    }
                }
            }
        }
    }

    internal suspend fun LXVBot.handleRPGMessage(mCE: MessageCreateEvent) {
        if (mCE.message.embeds.isNotEmpty()) {
            handleEmbed(mCE, mCE.message.embeds.first())
        }
    }
}