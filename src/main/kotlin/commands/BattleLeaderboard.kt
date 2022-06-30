package commands

import LXVBot
import LXVBot.Companion.PST
import LXVBot.Companion.toDate
import commands.util.*
import dev.kord.common.Color
import dev.kord.core.event.message.MessageCreateEvent
import entities.LXVUser
import entities.UserBattleCount
import entities.UserBattleCount.Companion.epoch
import entities.UserBattleCount.Companion.getDayId
import entities.UserBattleCount.Companion.getLeaderboardBattlesInRange
import kotlinx.datetime.*
import org.litote.kmongo.*

object BattleLeaderboard : BotCommand {
    override val name: String
        get() = "battleleaderboard"
    override val aliases: List<String>
        get() = listOf("battletop", "btop", "bldb", "bt")
    override val description: String
        get() = "Show the owo leaderboard for this server"
    override val category: CommandCategory
        get() = CommandCategory.GUILD

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        var valid = true
        var size = 5
        var page = 1
        val today = mCE.message.id.toDate()
        var startDate = epoch
        var endDate = today
        var titleToUse = ""
        var titleSet = true

        if (args.size <= 3) {
            var typeSet = false
            var sizeSet = false
            var pageSet = false

            for (a in args) {
                val arg = a.lowercase()
                if (arg.toIntOrNull() != null) {
                    if (sizeSet) {
                        valid = false
                    } else {
                        sizeSet = true
                        size = arg.toInt()
                    }
                } else if (arg.startsWith("p") && arg.drop(1).toIntOrNull() != null) {
                    if (pageSet) {
                        valid = false
                    } else {
                        page = arg.drop(1).toInt()
                        pageSet = true
                    }
                } else {
                    if (typeSet) {
                        valid = false
                    } else {
                        typeSet = true
                        titleSet = true
                        when (arg) {
                            "today", "t", "daily" -> {
                                startDate = today
                                endDate = today
                                titleToUse = "Today's "
                            }
                            "yesterday", "y" -> {
                                startDate = today.minus(DateTimeUnit.DAY)
                                endDate = today.minus(DateTimeUnit.DAY)
                                titleToUse = "Yesterday's "
                            }
                            "month", "m", "monthly" -> {
                                startDate = today.startOfMonth()
                                endDate = today.endOfMonth()
                                titleToUse = "${today.month.name.lowercase().replaceFirstChar { it.uppercase() }}'s "
                            }
                            "lm" -> {
                                startDate = today.startOfMonth(1)
                                endDate = today.endOfMonth(1)
                                titleToUse = "${
                                    today.startOfMonth(1).month.name.lowercase().replaceFirstChar { it.uppercase() }
                                }'s "
                            }
                            "weekly", "w" -> {
                                startDate = today.startOfWeek()
                                endDate = today.endOfWeek()
                                titleToUse = "This Weeks's "
                            }
                            "lw" -> {
                                startDate = today.startOfWeek(1)
                                endDate = today.endOfWeek(1)
                                titleToUse = "Last Weeks's "
                            }
                            "yearly", "year" -> {
                                startDate = today.startOfYear()
                                endDate = today.endOfYear()
                                titleToUse = "This Year's "
                            }
                            "ly" -> {
                                startDate = today.startOfYear(1)
                                endDate = today.endOfYear(1)
                                titleToUse = "Last Year's "
                            }
                            else -> titleSet = false
                        }
                        if (!titleSet) {
                            val dates = arg.split("-")
                            when (dates.size) {
                                1 -> {
                                    val date = parseDate(dates.first(), today.year)
                                    if (date == null) {
                                        valid = false
                                    } else {
                                        startDate = date
                                        endDate = date
                                    }
                                }
                                2 -> {
                                    val start = parseDate(dates[0], today.year)
                                    val end = parseDate(dates[1], today.year)
                                    if (start == null || end == null || start > end) {
                                        valid = false
                                    } else {
                                        startDate = start
                                        endDate = end
                                    }
                                }
                                else -> {
                                    valid = false
                                }
                            }
                        }
                    }
                }
            }


        } else {
            valid = false
        }
        if (valid) {
            val userCol = db.getCollection<LXVUser>(LXVUser.DB_NAME)
            val battleCol = db.getCollection<UserBattleCount>(UserBattleCount.DB_NAME)

            size = size.coerceAtLeast(3).coerceAtMost(25)
            page = page.coerceAtLeast(1).coerceAtMost(500 / size)
            val offset = (page - 1) * size

            val result = getLeaderboardBattlesInRange(battleCol, mCE.guildId!!, startDate, endDate, offset, size)

            val filters = List(result.size) {
                LXVUser::_id eq result[it]._id
            }


            val names = userCol.find(or(filters)).toList()
            val usernames = result.map { res ->
                names.find { user ->
                    user._id == res._id
                }?.username ?: getUserFromDB(res._id, col = userCol).username
            }

            val guildName = mCE.getGuild()?.name ?: "No Name????"
            sendMessage(mCE.message.channel) {
                color = Color(0xABCDEF)
                title = if (titleSet) {
                    "${titleToUse}Battle Leaderboard for $guildName"
                } else if (startDate != endDate) {
                    "${startDate.year}/${startDate.monthNumber}/${startDate.dayOfMonth}-${endDate.year}/${endDate.monthNumber}/${endDate.dayOfMonth} Battle Leaderboard for $guildName"
                } else {
                    "${startDate.dayOfMonth} ${
                        startDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
                    } ${startDate.year} Battle Leaderboard for $guildName"
                }
                for (x in result.indices) {
                    val res = result[x]
                    val username = usernames[x] ?: "Deleted User ${res._id}"
                    field {
                        name = "#${x + offset + 1}: $username"
                        value = "${res.sum} Battles"
                    }
                }
                if (endDate.getDayId() >= today.getDayId()) {
                    val timeLeft = endDate.plus(DateTimeUnit.DAY).atStartOfDayIn(PST) - mCE.message.timestamp
                    footer {
                        val d = timeLeft.inWholeDays
                        val h = timeLeft.inWholeHours % 24
                        val m = timeLeft.inWholeMinutes % 60
                        val s = timeLeft.inWholeSeconds % 60

                        text = "Resets in ${d}D ${h}H ${m}M ${s}S"
                    }
                }
            }
        } else {
            sendMessage(mCE.message.channel, "Invalid Format :(", 5_000)
        }
    }

    private fun parseDate(date: String, defaultYear: Int): LocalDate? {
        val parts = date.split("/").map { it.toIntOrNull() }
        return when (parts.size) {
            3 -> {
                try {
                    LocalDate(parts[0]!!, parts[1]!!, parts[2]!!)
                } catch (e: Exception) {
                    null
                }
            }
            2 -> {
                try {
                    LocalDate(defaultYear, parts.first()!!, parts.last()!!)
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }
}