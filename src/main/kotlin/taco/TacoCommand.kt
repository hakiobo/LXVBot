package taco

import LXVBot
import LXVUser
import Reminder
import commands.meta.HelpCommand
import commands.utils.*
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.litote.kmongo.eq
import org.litote.kmongo.div
import org.litote.kmongo.setValue

object TacoCommand : BotCommand {
    override val name: String
        get() = "tacoshack"
    override val description: String
        get() = "TacoShack bot stuff"
    override val aliases: List<String>
        get() = listOf("taco", "ts")
    override val category: CommandCategory
        get() = CommandCategory.OTHER_BOTS
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(listOf(), "Shows this help screen"),
            CommandUsage(
                listOf(Argument("enable", ArgumentType.EXACT), Argument("reminder")),
                "Enables a specific TacoShack reminder"
            ),
            CommandUsage(
                listOf(Argument("disable", ArgumentType.EXACT), Argument("reminder")),
                "Disables a specific TacoShack reminder"
            ),
            CommandUsage(
                listOf(Argument("reset", ArgumentType.EXACT), Argument("reminder")),
                "Resets the cooldown for a specific TacoShack reminder"
            ),
            CommandUsage(
                listOf(Argument("info", ArgumentType.EXACT)),
                "Shows a list of available reminders for TacoShack"
            ),
            CommandUsage(
                listOf(Argument(listOf("status", "settings", "stats", "stat"))),
                "Shows your reminder count and status for each TacoShack reminder"
            ),
            )

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if (args.isEmpty()) {
            HelpCommand.runCMD(this, mCE, listOf(this@TacoCommand.name))
        } else {
            when (args.first().toLowerCase()) {
                "enable", "disable" -> handleEnableDisableSubCommand(mCE, args)
                "reset" -> handleResetSubcommand(mCE, args)
                "info" -> handleInfoSubcommand(mCE)
                "status", "settings", "stats", "stat" -> handleStatusSubcommand(mCE)
                else -> reply(mCE.message, "Not a valid $name subcommand")
            }
        }
    }

    private fun findTacoReminder(name: String): TacoReminderType? {
        for (reminder in TacoReminderType.values()) {
            if (name in reminder.aliases || name == reminder.name.toLowerCase()) {
                return reminder
            }
        }
        return null
    }

    private suspend fun LXVBot.handleEnableDisableSubCommand(mCE: MessageCreateEvent, args: List<String>) {
        if (args.size < 2) {
            reply(mCE.message, "You need to specify which reminder you're editing")
            return
        }
        val enable = args.first().toLowerCase() == "enable"
        val reminderArg = args[1].toLowerCase()
        val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
        if (reminderArg == "all") {
            val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
            val userReminder = user.taco.tacoReminders
            val newReminderSettings = userReminder.copy(
                tip = userReminder.tip.copy(enabled = enable),
                work = userReminder.work.copy(enabled = enable),
                overtime = userReminder.overtime.copy(enabled = enable),
                daily = userReminder.daily.copy(enabled = enable),
                clean = userReminder.clean.copy(enabled = enable),
                vote = userReminder.vote.copy(enabled = enable),
                flipper = userReminder.flipper.copy(enabled = enable),
                karaoke = userReminder.karaoke.copy(enabled = enable),
                music = userReminder.music.copy(enabled = enable),
                airplane = userReminder.airplane.copy(enabled = enable),
                chef = userReminder.chef.copy(enabled = enable),
            )
            userCol.updateOne(
                LXVUser::_id eq user._id,
                setValue(LXVUser::taco / TacoData::tacoReminders, newReminderSettings)
            )
            reply(mCE.message, "All Taco Shack Reminders ${if (enable) "En" else "Dis"}abled!")
        } else {
            val reminder = findTacoReminder(reminderArg)
            if (reminder != null) {
                val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
                if (reminder.prop.get(user.taco.tacoReminders).enabled == enable) {
                    reply(
                        mCE.message,
                        "TacoShack ${
                            reminder.name.toLowerCase().capitalize()
                        } Reminder Already ${if (enable) "En" else "Dis"}abled!"
                    )
                } else {
                    userCol.updateOne(
                        LXVUser::_id eq user._id,
                        setValue(LXVUser::taco / TacoData::tacoReminders / reminder.prop / Reminder::enabled, enable)
                    )
                    reply(
                        mCE.message,
                        "TacoShack ${
                            reminder.name.toLowerCase().capitalize()
                        } Reminder ${if (enable) "En" else "Dis"}abled!"
                    )
                }
            } else {
                reply(
                    mCE.message,
                    "That's not a valid Taco Shack Reminder. Use `${LXVBot.BOT_PREFIX}${name} info to see a list of Valid Reminders`"
                )
            }
        }
    }

    private suspend fun LXVBot.handleResetSubcommand(mCE: MessageCreateEvent, args: List<String>) {
        if (args.size < 2) {
            reply(mCE.message, "You need to specify which reminder you're resetting")
            return
        }
        val reminderArg = args[1].toLowerCase()
        val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
        if (reminderArg == "all") {
            val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
            val userReminder = user.taco.tacoReminders
            val newReminderSettings = userReminder.copy(
                tip = userReminder.tip.copy(lastUse = 0L),
                work = userReminder.work.copy(lastUse = 0L),
                overtime = userReminder.overtime.copy(lastUse = 0L),
                daily = userReminder.daily.copy(lastUse = 0L),
                clean = userReminder.clean.copy(lastUse = 0L),
                vote = userReminder.vote.copy(lastUse = 0L),
                flipper = userReminder.flipper.copy(lastUse = 0L),
                karaoke = userReminder.karaoke.copy(lastUse = 0L),
                music = userReminder.music.copy(lastUse = 0L),
                airplane = userReminder.airplane.copy(lastUse = 0L),
                chef = userReminder.chef.copy(lastUse = 0L),
            )
            userCol.updateOne(
                LXVUser::_id eq user._id,
                setValue(LXVUser::taco / TacoData::tacoReminders, newReminderSettings)
            )
            reply(mCE.message, "All Taco Shack Reminder Cooldowns Reset!")
        } else {
            val reminder = findTacoReminder(reminderArg)
            if (reminder != null) {
                val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
                if (reminder.prop.get(user.taco.tacoReminders).lastUse == 0L) {
                    reply(
                        mCE.message,
                        "There's nothing to reset smh"
                    )
                } else {
                    userCol.updateOne(
                        LXVUser::_id eq user._id,
                        setValue(LXVUser::taco / TacoData::tacoReminders / reminder.prop / Reminder::lastUse, 0L)
                    )
                    reply(
                        mCE.message,
                        "TacoShack ${reminder.name.toLowerCase().capitalize()} Reminder Cooldown Reset!"
                    )
                }
            } else {
                reply(
                    mCE.message,
                    "That's not a valid Taco Shack Reminder. Use `${LXVBot.BOT_PREFIX}${name} info to see a list of Valid Reminders`"
                )
            }
        }
    }

    private suspend fun LXVBot.handleInfoSubcommand(mCE: MessageCreateEvent) {
        val self = client.getSelf()
        reply(mCE.message) {
            title = "${LXVBot.BOT_NAME} TacoShack Command Info"
            description = TacoReminderType.values().joinToString("\n\n") { reminder ->
                "${reminder.name.toLowerCase().capitalize()}\nAliases: ${reminder.aliases.joinToString(", ")}"
            }
            footer {
                text =
                    "You can also use \"${LXVBot.BOT_PREFIX} ts [enable/disable/reset] all\" to enable/disable/reset all of the cooldowns"
                icon = self.avatar.url
            }
        }
    }

    private suspend fun LXVBot.handleStatusSubcommand(mCE: MessageCreateEvent) {
        val col = db.getCollection<LXVUser>(LXVUser.DB_NAME)
        val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, col)
        val self = client.getSelf()
        reply(mCE.message) {
            author {
                name = "${user.username}'s TacoShack Reminder Settings"
                icon = mCE.message.author?.avatar?.url
            }
            for (reminder in TacoReminderType.values()) {
                field {
                    inline = true
                    name = "${reminder.name.toLowerCase().capitalize()}${if (reminder.buy) " Boost" else ""}"
                    val r = reminder.prop.get(user.taco.tacoReminders)
                    value =
                        "Enabled: ${LXVBot.getCheckmarkOrCross(r.enabled)}\nReminder Count: ${r.count}"
                }
            }
            footer {
                text =
                    "\"${LXVBot.BOT_PREFIX} ts [enable/disable] <reminder>\" to set a specific reminder!\n" +
                            "\"${LXVBot.BOT_PREFIX} ts [enable/disable] all\" to set all reminders!"
                icon = self.avatar.url
            }
        }
    }

    suspend fun LXVBot.handleTacoCommand(mCE: MessageCreateEvent, args: List<String>) {
        val (id, buy) = when (args.firstOrNull()?.toLowerCase()) {
            "buy" -> {
                if (args.size < 2) return
                args[1].toLowerCase() to true
            }
            null -> return
            else -> {
                args.first().toLowerCase() to false
            }
        }
        for (reminder in TacoReminderType.values()) {
            if (reminder.buy == buy && id in reminder.aliases) {
                val curTime = mCE.message.id.timeStamp.toEpochMilli()
                val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
                val user = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
                val userReminder = reminder.prop.get(user.taco.tacoReminders)
                if (userReminder.enabled && (curTime - userReminder.lastUse) >= reminder.time) {
                    client.launch {
                        val new = userReminder.copy(lastUse = curTime, count = userReminder.count + 1)
                        userCol.updateOne(
                            LXVUser::_id eq user._id,
                            setValue(LXVUser::taco / TacoData::tacoReminders / reminder.prop, new)
                        )
                        delay(reminder.time)
                        val check = getUserFromDB(mCE.message.author!!.id, mCE.message.author, userCol)
                        if (reminder.prop.get(check.taco.tacoReminders).lastUse == curTime) {
                            reply(
                                mCE.message,
                                "Taco Shack ${reminder.aliases.first().capitalize()} cooldown is done",
                                true
                            )
                        }
                    }
                }
            }
        }
    }
}