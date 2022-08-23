package commands

import LXVBot
import LXVBot.Companion.PST
import LXVBot.Companion.toOwODate
import commands.util.*
import dev.kord.common.Color
import dev.kord.core.event.message.MessageCreateEvent
import entities.LXVUser
import entities.UserDailyStats
import entities.UserDailyStats.Companion.epoch
import entities.UserDailyStats.Companion.getDayId
import entities.UserDailyStats.Companion.getLeaderboardStatInRange
import kotlinx.datetime.*
import org.litote.kmongo.*

object OwOLeaderboard : BotCommand {
    override val name: String
        get() = "owoleaderboard"
    override val aliases: List<String>
        get() = listOf("owotop", "otop", "ot", "t", "owoldb", "ldb", "top", "leaderboard")
    override val description: String
        get() = "Show the owo leaderboard for this server"
    override val category: CommandCategory
        get() = CommandCategory.GUILD

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        var valid = true
        var size = 5
        var page = 1
        val today = mCE.message.id.timestamp.toOwODate()
        var startDate = epoch
        var endDate = today
        var titleToUse = ""
        var titleSet = true
        var showIds = false

        if (args.size <= 4) {
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
                } else if (arg == "id") {
                    if (showIds) {
                        valid = false
                    }
                    showIds = true
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
            val statCol = db.getCollection<UserDailyStats>(UserDailyStats.DB_NAME)

            size = size.coerceAtLeast(3).coerceAtMost(25)
            page = page.coerceAtLeast(1).coerceAtMost(500 / size)
            val offset = (page - 1) * size

            val result =
                getLeaderboardStatInRange(
                    statCol,
                    mCE.guildId!!,
                    startDate,
                    endDate,
                    offset,
                    size,
                    UserDailyStats::owoCount,
                )

            val filters = List(result.size) {
                LXVUser::_id eq result[it]._id
            }


            val names = userCol.find(or(filters)).toList()

            val usernames = if (showIds) {
                result.map { res -> res._id }
            } else {
                result.map { res ->
                    names.find { user ->
                        user._id == res._id
                    }?.username ?: getUserFromDB(res._id, col = userCol).username
                }
            }


            val guildName = mCE.getGuild()?.name ?: "No Name????"
            sendMessage(mCE.message.channel) {
                color = Color(0xABCDEF)
                title = if (titleSet) {
                    "${titleToUse}OwO Leaderboard for $guildName"
                } else if (startDate != endDate) {
                    "${startDate.year}/${startDate.monthNumber}/${startDate.dayOfMonth}-${endDate.year}/${endDate.monthNumber}/${endDate.dayOfMonth} OwO Leaderboard for $guildName"
                } else {
                    "${startDate.dayOfMonth} ${
                        startDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
                    } ${startDate.year} OwO Leaderboard for $guildName"
                }
                for (x in result.indices) {
                    val res = result[x]
                    val username = usernames[x] ?: "Deleted User ${res._id}"
                    field {
                        name = "#${x + offset + 1}: $username"
                        value = "${res.sum} OwOs"
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

//    private fun findType(s: String): OwORankingType? {
//        for (t in OwORankingType.values()) {
//            if (s.lowercase() in t.triggers) {
//                return t
//            }
//        }
//        return null
//    }
//
//    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
//        if (args.size <= 3) {
//            var type = OwORankingType.TOTAL
//            var size = 5
//            var page = 1
//            var typeSet = false
//            var sizeSet = false
//            var pageSet = false
//            var showIds = false
//            var valid = true
//            for (a in args) {
//                val arg = a.lowercase()
//                if (arg.toIntOrNull() != null) {
//                    if (sizeSet) {
//                        valid = false
//                    } else {
//                        sizeSet = true
//                        size = arg.toInt()
//                    }
//                } else if (arg.startsWith("p") && arg.drop(1).toIntOrNull() != null) {
//                    if (pageSet) {
//                        valid = false
//                    } else {
//                        page = arg.drop(1).toInt()
//                        pageSet = true
//                    }
//                } else if (arg == "id") {
//                    if (showIds) valid = false else showIds = true
//                } else {
//                    if (typeSet) {
//                        valid = false
//                    } else {
//                        val t = findType(arg)
//                        if (t == null) {
//                            valid = false
//                        } else {
//                            typeSet = true
//                            type = t
//                        }
//                    }
//                }
//            }
//            if (valid) {
//                val userCol = db.getCollection<LXVUser>("users")
//                size = size.coerceAtLeast(3).coerceAtMost(25)
//                page = page.coerceAtLeast(1).coerceAtMost(100 / size)
//                val offset = (page - 1) * size + 1
//                val result = type.interval.getIdDataPairs(this, mCE, type.unit, size, page)
//
//                val filters = List(result.size) {
//                    LXVUser::_id eq result[it].first
//                }
//
//                val names = userCol.find(or(filters)).toList()
//                val usernames = if (showIds) {
//                    result.map { res -> res.first }
//                } else {
//                    result.map { res ->
//                        names.find { user ->
//                            user._id == res.first
//                        }?.username ?: getUserFromDB(res.first, col = userCol).username
//                    }
//                }
//
//                val guildName = mCE.getGuild()?.name ?: "No Name????"
//                sendMessage(mCE.message.channel) {
//                    color = Color(0xABCDEF)
//                    title = "${type.desc}OwO Leaderboard for $guildName"
//                    for (x in result.indices) {
//                        val res = result[x]
//                        val username = usernames[x] ?: "Deleted User ${res.first}"
//                        field {
//                            name = "#${x + offset}: $username"
//                            value = "${res.second} OwOs"
//                        }
//                    }
//                    val timeLeft = type.unit.timeUntilEndOfCurrentPeriod(mCE.message.id)
//                    if (timeLeft != null) {
//                        footer {
//                            val d = timeLeft.inWholeDays
//                            val h = timeLeft.inWholeHours % 24
//                            val m = timeLeft.inWholeMinutes % 60
//                            val s = timeLeft.inWholeSeconds % 60
//
//                            text = "${type.interval.note} ${d}D ${h}H ${m}M ${s}S"
//                        }
//                    }
//                }
//            } else {
//                sendMessage(mCE.message.channel, "Invalid Format :(", 5_000)
//            }
//        } else {
//            sendMessage(mCE.message.channel, "Invalid Format :(", 5_000)
//        }
//
//    }
//
//
//    private enum class TimeUnit(
//        val curStat: KProperty1<UserGuildOwOCount, Int>,
//        val prevStat: KProperty1<UserGuildOwOCount, Int>,
//    ) {
//        TOTAL(UserGuildOwOCount::owoCount, UserGuildOwOCount::owoCount) {
//            override fun timeUntilEndOfCurrentPeriod(id: Snowflake): Duration? = null
//
//            override fun startOfCurrentPeriod(id: Snowflake): Instant = Instant.DISTANT_PAST
//
//            override fun startOfPreviousPeriod(id: Snowflake): Instant = Instant.DISTANT_PAST
//        },
//        YEAR(UserGuildOwOCount::yearlyCount, UserGuildOwOCount::lastYearCount) {
//            override fun timeUntilEndOfCurrentPeriod(id: Snowflake): Duration {
//                val time = id.timestamp
//                val endTime =
//                    time.toLocalDateTime(LXVBot.PST).date.plus(DateTimeUnit.YEAR).toJavaLocalDate().withDayOfYear(1)
//                        .toKotlinLocalDate().atStartOfDayIn(LXVBot.PST)
//                return endTime - time
//            }
//
//            override fun startOfCurrentPeriod(id: Snowflake): Instant {
//                return id.timestamp.toLocalDateTime(LXVBot.PST).date.toJavaLocalDate().withDayOfYear(1)
//                    .toKotlinLocalDate().atStartOfDayIn(LXVBot.PST)
//            }
//
//            override fun startOfPreviousPeriod(id: Snowflake): Instant {
//                return id.timestamp.toLocalDateTime(LXVBot.PST).date.minus(DateTimeUnit.YEAR).toJavaLocalDate()
//                    .withDayOfYear(1).toKotlinLocalDate().atStartOfDayIn(LXVBot.PST)
//            }
//        },
//        MONTH(UserGuildOwOCount::monthlyCount, UserGuildOwOCount::lastMonthCount) {
//            override fun timeUntilEndOfCurrentPeriod(id: Snowflake): Duration {
//                val time = id.timestamp
//                val endTime =
//                    time.toLocalDateTime(LXVBot.PST).date.plus(DateTimeUnit.MONTH).toJavaLocalDate().withDayOfMonth(1)
//                        .toKotlinLocalDate().atStartOfDayIn(LXVBot.PST)
//
//                return endTime - time
//            }
//
//            override fun startOfCurrentPeriod(id: Snowflake): Instant {
//                return id.timestamp.toLocalDateTime(LXVBot.PST).date.toJavaLocalDate().withDayOfMonth(1)
//                    .toKotlinLocalDate().atStartOfDayIn(LXVBot.PST)
//            }
//
//            override fun startOfPreviousPeriod(id: Snowflake): Instant {
//                return id.timestamp.toLocalDateTime(LXVBot.PST).date.minus(DateTimeUnit.MONTH).toJavaLocalDate()
//                    .withDayOfMonth(1).toKotlinLocalDate().atStartOfDayIn(LXVBot.PST)
//            }
//        },
//        WEEK(UserGuildOwOCount::weeklyCount, UserGuildOwOCount::lastWeekCount) {
//            override fun timeUntilEndOfCurrentPeriod(id: Snowflake): Duration {
//                val time = id.timestamp
//                val curDate = time.toLocalDateTime(LXVBot.PST).date
//                val endTime =
//                    curDate.plus(7 - (curDate.dayOfWeek.value % 7), DateTimeUnit.DAY).atStartOfDayIn(LXVBot.PST)
//
//                return endTime - time
//            }
//
//            override fun startOfCurrentPeriod(id: Snowflake): Instant {
//                val curDate = id.timestamp.toLocalDateTime(LXVBot.PST).date
//                return curDate.minus(curDate.dayOfWeek.value % 7, DateTimeUnit.DAY).atStartOfDayIn(LXVBot.PST)
//            }
//
//            override fun startOfPreviousPeriod(id: Snowflake): Instant {
//                val curDate = id.timestamp.toLocalDateTime(LXVBot.PST).date
//                return curDate.minus(7 + (curDate.dayOfWeek.value % 7), DateTimeUnit.DAY).atStartOfDayIn(LXVBot.PST)
//            }
//        },
//        DAY(UserGuildOwOCount::dailyCount, UserGuildOwOCount::yesterdayCount) {
//            override fun timeUntilEndOfCurrentPeriod(id: Snowflake): Duration {
//                val time = id.timestamp
//                val endTime = time.toLocalDateTime(LXVBot.PST).date.plus(DateTimeUnit.DAY).atStartOfDayIn(LXVBot.PST)
//                return endTime - time
//            }
//
//            override fun startOfCurrentPeriod(id: Snowflake): Instant {
//                return id.timestamp.toLocalDateTime(LXVBot.PST).date.atStartOfDayIn(LXVBot.PST)
//
//            }
//
//            override fun startOfPreviousPeriod(id: Snowflake): Instant {
//                return id.timestamp.toLocalDateTime(LXVBot.PST).date.minus(DateTimeUnit.DAY).atStartOfDayIn(LXVBot.PST)
//            }
//        };
//
//        abstract fun timeUntilEndOfCurrentPeriod(id: Snowflake): Duration?
//
//        abstract fun startOfCurrentPeriod(id: Snowflake): Instant
//
//        abstract fun startOfPreviousPeriod(id: Snowflake): Instant
//    }
//
//
//    private enum class IntervalType(
//        val note: String?,
//    ) {
//        TOTAL(null) {
//            override suspend fun getIdDataPairs(
//                bot: LXVBot,
//                mCE: MessageCreateEvent,
//                unit: TimeUnit,
//                pageSize: Int,
//                pages: Int,
//            ): List<Pair<Snowflake, Int>> {
//                return bot.hakiDb.getCollection<UserGuildOwOCount>("owo-count").aggregate<UserGuildOwOCount>(
//                    match(UserGuildOwOCount::guild eq mCE.guildId!!),
//                    sort(descending(unit.curStat)),
//                    limit(pageSize * pages),
//                    sort(ascending(unit.curStat)),
//                    limit(pageSize),
//                    sort(descending(unit.curStat)),
//                ).toList().map { Pair(it.user, unit.curStat.get(it)) }
//            }
//        },
//        CURRENT("Resets in") {
//            override suspend fun getIdDataPairs(
//                bot: LXVBot,
//                mCE: MessageCreateEvent,
//                unit: TimeUnit,
//                pageSize: Int,
//                pages: Int,
//            ): List<Pair<Snowflake, Int>> {
//                return bot.hakiDb.getCollection<UserGuildOwOCount>("owo-count").aggregate<UserGuildOwOCount>(
//                    match(UserGuildOwOCount::guild eq mCE.guildId!!),
//                    match(
//                        UserGuildOwOCount::lastOWO gte unit.startOfCurrentPeriod(mCE.message.id).toEpochMilliseconds()
//                    ),
//                    sort(descending(unit.curStat)),
//                    limit(pageSize * pages),
//                    sort(ascending(unit.curStat)),
//                    limit(pageSize),
//                    sort(descending(unit.curStat)),
//                ).toList().map { Pair(it.user, unit.curStat.get(it)) }
//            }
//        },
//        PREVIOUS("Viewable for") {
//            override suspend fun getIdDataPairs(
//                bot: LXVBot,
//                mCE: MessageCreateEvent,
//                unit: TimeUnit,
//                pageSize: Int,
//                pages: Int,
//            ): List<Pair<Snowflake, Int>> {
//                val start = unit.startOfCurrentPeriod(mCE.message.id).toEpochMilliseconds()
//                val prevStart = unit.startOfPreviousPeriod(mCE.message.id).toEpochMilliseconds()
//                val col = bot.hakiDb.getCollection<UserGuildOwOCount>("owo-count")
//                return col.aggregate<UserGuildOwOCount>(
//                    match(UserGuildOwOCount::guild eq mCE.guildId!!),
//                    match(UserGuildOwOCount::lastOWO gte start),
//                    sort(descending(unit.prevStat)),
//                    limit(pageSize * pages)
//                ).toList().map { Pair(it.user, unit.prevStat.get(it)) }.union(col.aggregate<UserGuildOwOCount>(
//                    match(UserGuildOwOCount::guild eq mCE.guildId!!),
//                    match(UserGuildOwOCount::lastOWO gte prevStart),
//                    match(UserGuildOwOCount::lastOWO lt start),
//                    sort(descending(unit.curStat)),
//                    limit(pageSize * pages)
//                ).toList().map { Pair(it.user, unit.curStat.get(it)) }).sortedByDescending { it.second }
//                    .drop(pageSize * (pages - 1)).take(pageSize)
//            }
//        };
//
//        abstract suspend fun getIdDataPairs(
//            bot: LXVBot, mCE: MessageCreateEvent, unit: TimeUnit, pageSize: Int, pages: Int
//        ): List<Pair<Snowflake, Int>>
//    }
//
//    private enum class OwORankingType(
//        val triggers: List<String>,
//        val desc: String,
//        val interval: IntervalType,
//        val unit: TimeUnit,
//    ) {
//        TOTAL(listOf("all", "total"), "", IntervalType.TOTAL, TimeUnit.TOTAL), YEAR(
//            listOf("year", "yearly"), "Yearly ", IntervalType.CURRENT, TimeUnit.YEAR
//        ),
//        LAST_YEAR(
//            listOf("lastyear", "prevyear", "ly", "py"), "Last Year's ", IntervalType.PREVIOUS, TimeUnit.YEAR
//        ),
//        MONTH(listOf("month", "m", "monthly"), "Monthly ", IntervalType.CURRENT, TimeUnit.MONTH), LAST_MONTH(
//            listOf("lastmonth", "prevmonth", "lm", "pm"), "Last Month's ", IntervalType.PREVIOUS, TimeUnit.MONTH
//        ),
//        WEEK(
//            listOf("week", "w", "weekly"), "Weekly ", IntervalType.CURRENT, TimeUnit.WEEK
//        ),
//        LAST_WEEK(
//            listOf("lastweek", "prevweek", "lw", "pw"), "Last Week's ", IntervalType.PREVIOUS, TimeUnit.WEEK
//        ),
//        DAY(
//            listOf("t", "today", "d", "day", "daily"), "Today's ", IntervalType.CURRENT, TimeUnit.DAY
//        ),
//        YESTERDAY(
//            listOf("y", "yesterday", "yes", "yday", "pday", "prevday", "lday", "lastday"),
//            "Yesterday's ",
//            IntervalType.PREVIOUS,
//            TimeUnit.DAY
//        ),
//    }
}
