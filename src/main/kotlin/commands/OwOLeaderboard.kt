package commands

import LXVBot
import LXVUser
import UserGuildOwOCount
import commands.utils.BotCommand
import commands.utils.CommandCategory
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.aggregate
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KProperty1

object OwOLeaderboard : BotCommand {
    override val name: String
        get() = "owoleaderboard"
    override val aliases: List<String>
        get() = listOf("owotop", "otop", "owoldb", "ldb", "top", "leaderboard")
    override val description: String
        get() = "Show the owo leaderboard for this server"
    override val category: CommandCategory
        get() = CommandCategory.GUILD

    private fun findType(s: String): RankingType? {
        for (t in RankingType.values()) {
            if (s.toLowerCase() in t.triggers) {
                return t
            }
        }
        return null
    }

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if (args.size <= 3) {
            var type = RankingType.TOTAL
            var size = 5
            var page = 1
            var typeSet = false
            var sizeSet = false
            var pageSet = false
            var valid = true
            for (a in args) {
                val arg = a.toLowerCase()
                if (arg.toIntOrNull() != null) {
                    if (arg.startsWith("p")) sendMessage(mCE.message.channel, "This is dumb")
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
                        val t = findType(arg)
                        if (t == null) {
                            valid = false
                        } else {
                            typeSet = true
                            type = t
                        }
                    }
                }
            }
            if (valid) {
                val userCol = db.getCollection<LXVUser>("users")
                size = size.coerceAtLeast(3).coerceAtMost(25)
                page = page.coerceAtLeast(1).coerceAtMost(100 / size)
                val offset = (page - 1) * size + 1
                val result =
                    type.interval.getIdDataPairs(this, mCE, type.unit, size, page)

                val filters = List(result.size) {
                    LXVUser::_id eq result[it].first
                }

                val names = userCol.find(or(filters)).toList()
                val usernames = result.map { res ->
                    names.find { user ->
                        user._id == res.first
                    }?.username ?: getUserFromDB(Snowflake(res.first), col = userCol).username
                }

                val guildName = mCE.getGuild()?.name ?: "No Name????"
                sendMessage(mCE.message.channel) {
                    color = Color(0xABCDEF)
                    title = "${type.desc}OwO Leaderboard for $guildName"
                    for (x in result.indices) {
                        val res = result[x]
                        val username = usernames[x] ?: "Deleted User ${res.first}"
                        field {
                            name = "#${x + offset}: $username"
                            value = "${res.second} OwOs"
                        }
                    }
                    val timeLeft = type.unit.timeUntilEndOfCurrentPeriod(mCE.message.id)
                    if (timeLeft != null) {
                        footer {
                            val d = timeLeft.toDays()
                            val h = timeLeft.toHours() % 24
                            val m = timeLeft.toMinutes() % 60
                            val s = timeLeft.seconds % 60

                            text = "${type.interval.note} ${d}D ${h}H ${m}M ${s}S"
                        }
                    }
                }
            } else {
                sendMessage(mCE.message.channel, "Invalid Format :(", 5_000)
            }
        } else {
            sendMessage(mCE.message.channel, "Invalid Format :(", 5_000)
        }

    }


    private enum class TimeUnit(
        val curStat: KProperty1<UserGuildOwOCount, Int>,
        val prevStat: KProperty1<UserGuildOwOCount, Int>,
    ) {
        TOTAL(UserGuildOwOCount::owoCount, UserGuildOwOCount::owoCount) {
            override fun timeUntilEndOfCurrentPeriod(id: Snowflake): Duration? = null

            override fun startOfCurrentPeriod(id: Snowflake): Instant = Instant.MIN

            override fun startOfPreviousPeriod(id: Snowflake): Instant = Instant.MIN
        },
        YEAR(UserGuildOwOCount::yearlyCount, UserGuildOwOCount::lastYearCount) {
            override fun timeUntilEndOfCurrentPeriod(id: Snowflake): Duration? {
                val time = id.timeStamp.atZone(LXVBot.PST)
                val endTime = time.toLocalDate().plusYears(1).withDayOfYear(1).atStartOfDay(LXVBot.PST)

                return Duration.between(time, endTime)
            }

            override fun startOfCurrentPeriod(id: Snowflake): Instant {
                return id.timeStamp.atZone(LXVBot.PST).toLocalDate().withDayOfYear(1).atStartOfDay(LXVBot.PST)
                    .toInstant()
            }

            override fun startOfPreviousPeriod(id: Snowflake): Instant {
                return id.timeStamp.atZone(LXVBot.PST).toLocalDate().minusYears(1L).withDayOfYear(1)
                    .atStartOfDay(LXVBot.PST).toInstant()
            }
        },
        MONTH(UserGuildOwOCount::monthlyCount, UserGuildOwOCount::lastMonthCount) {
            override fun timeUntilEndOfCurrentPeriod(id: Snowflake): Duration? {
                val time = id.timeStamp.atZone(LXVBot.PST)
                val endTime = time.toLocalDate().plusMonths(1).withDayOfMonth(1).atStartOfDay(LXVBot.PST)

                return Duration.between(time, endTime)
            }

            override fun startOfCurrentPeriod(id: Snowflake): Instant {
                return id.timeStamp.atZone(LXVBot.PST).toLocalDate().withDayOfMonth(1).atStartOfDay(LXVBot.PST)
                    .toInstant()
            }

            override fun startOfPreviousPeriod(id: Snowflake): Instant {
                return id.timeStamp.atZone(LXVBot.PST).toLocalDate().withDayOfMonth(1).minusMonths(1)
                    .atStartOfDay(LXVBot.PST).toInstant()
            }
        },
        WEEK(UserGuildOwOCount::weeklyCount, UserGuildOwOCount::lastWeekCount) {
            override fun timeUntilEndOfCurrentPeriod(id: Snowflake): Duration? {
                val time = id.timeStamp.atZone(LXVBot.PST)
                val endTime = time.toLocalDate().plusDays(7L - (time.dayOfWeek.value % 7)).atStartOfDay(LXVBot.PST)

                return Duration.between(time, endTime)
            }

            override fun startOfCurrentPeriod(id: Snowflake): Instant {
                val time = id.timeStamp.atZone(LXVBot.PST).toLocalDate()
                return time.minusDays(time.dayOfWeek.value % 7L).atStartOfDay(LXVBot.PST).toInstant()
            }

            override fun startOfPreviousPeriod(id: Snowflake): Instant {
                val time = id.timeStamp.atZone(LXVBot.PST).toLocalDate()
                return time.minusDays((time.dayOfWeek.value % 7L)).minusWeeks(1).atStartOfDay(LXVBot.PST).toInstant()
            }
        },
        DAY(UserGuildOwOCount::dailyCount, UserGuildOwOCount::yesterdayCount) {
            override fun timeUntilEndOfCurrentPeriod(id: Snowflake): Duration? {
                val time = id.timeStamp.atZone(LXVBot.PST)
                val endTime = time.toLocalDate().plusDays(1).atStartOfDay(LXVBot.PST)
                return Duration.between(time, endTime)
            }

            override fun startOfCurrentPeriod(id: Snowflake): Instant {
                return id.timeStamp.atZone(LXVBot.PST).toLocalDate().atStartOfDay(LXVBot.PST).toInstant()
            }

            override fun startOfPreviousPeriod(id: Snowflake): Instant {
                return id.timeStamp.atZone(LXVBot.PST).toLocalDate().minusDays(1).atStartOfDay(LXVBot.PST)
                    .toInstant()
            }
        };

        abstract fun timeUntilEndOfCurrentPeriod(id: Snowflake): Duration?

        abstract fun startOfCurrentPeriod(id: Snowflake): Instant

        abstract fun startOfPreviousPeriod(id: Snowflake): Instant
    }


    private enum class IntervalType(
        val note: String?,
    ) {
        TOTAL(null) {
            override suspend fun getIdDataPairs(
                bot: LXVBot,
                mCE: MessageCreateEvent,
                unit: TimeUnit,
                pageSize: Int,
                pages: Int,
            ): List<Pair<Long, Int>> {
                return bot.hakiDb.getCollection<UserGuildOwOCount>("owo-count").aggregate<UserGuildOwOCount>(
                    match(UserGuildOwOCount::guild eq mCE.guildId!!.value),
                    sort(descending(unit.curStat)),
                    limit(pageSize * pages),
                    sort(ascending(unit.curStat)),
                    limit(pageSize),
                    sort(descending(unit.curStat)),
                ).toList().map { Pair(it.user, unit.curStat.get(it)) }
            }
        },
        CURRENT("Resets in") {
            override suspend fun getIdDataPairs(
                bot: LXVBot,
                mCE: MessageCreateEvent,
                unit: TimeUnit,
                pageSize: Int,
                pages: Int,
            ): List<Pair<Long, Int>> {
                return bot.hakiDb.getCollection<UserGuildOwOCount>("owo-count").aggregate<UserGuildOwOCount>(
                    match(UserGuildOwOCount::guild eq mCE.guildId!!.value),
                    match(UserGuildOwOCount::lastOWO gte unit.startOfCurrentPeriod(mCE.message.id).toEpochMilli()),
                    sort(descending(unit.curStat)),
                    limit(pageSize * pages),
                    sort(ascending(unit.curStat)),
                    limit(pageSize),
                    sort(descending(unit.curStat)),
                ).toList().map { Pair(it.user, unit.curStat.get(it)) }
            }
        },
        PREVIOUS("Viewable for") {
            override suspend fun getIdDataPairs(
                bot: LXVBot,
                mCE: MessageCreateEvent,
                unit: TimeUnit,
                pageSize: Int,
                pages: Int,
            ): List<Pair<Long, Int>> {
                val start = unit.startOfCurrentPeriod(mCE.message.id).toEpochMilli()
                val prevStart = unit.startOfPreviousPeriod(mCE.message.id).toEpochMilli()
                val col = bot.hakiDb.getCollection<UserGuildOwOCount>("owo-count")
                return col.aggregate<UserGuildOwOCount>(
                    match(UserGuildOwOCount::guild eq mCE.guildId!!.value),
                    match(UserGuildOwOCount::lastOWO gte start),
                    sort(descending(unit.prevStat)),
                    limit(pageSize * pages)
                ).toList().map { Pair(it.user, unit.prevStat.get(it)) }
                    .union(
                        col.aggregate<UserGuildOwOCount>(
                            match(UserGuildOwOCount::guild eq mCE.guildId!!.value),
                            match(UserGuildOwOCount::lastOWO gte prevStart),
                            match(UserGuildOwOCount::lastOWO lt start),
                            sort(descending(unit.curStat)),
                            limit(pageSize * pages)
                        ).toList().map { Pair(it.user, unit.curStat.get(it)) }
                    ).sortedByDescending { it.second }.drop(pageSize * (pages - 1)).take(pageSize)
            }
        };

        abstract suspend fun getIdDataPairs(
            bot: LXVBot,
            mCE: MessageCreateEvent,
            unit: TimeUnit,
            pageSize: Int,
            pages: Int
        ): List<Pair<Long, Int>>
    }

    private enum class RankingType(
        val triggers: List<String>,
        val desc: String,
        val interval: IntervalType,
        val unit: TimeUnit,
    ) {
        TOTAL(listOf("all", "total"), "", IntervalType.TOTAL, TimeUnit.TOTAL),
        YEAR(listOf("year", "yearly"), "Yearly ", IntervalType.CURRENT, TimeUnit.YEAR),
        LAST_YEAR(listOf("lastyear", "prevyear", "ly", "py"), "Last Year's ", IntervalType.PREVIOUS, TimeUnit.YEAR),
        MONTH(listOf("month", "m", "monthly"), "Monthly ", IntervalType.CURRENT, TimeUnit.MONTH),
        LAST_MONTH(
            listOf("lastmonth", "prevmonth", "lm", "pm"),
            "Last Month's ",
            IntervalType.PREVIOUS,
            TimeUnit.MONTH
        ),
        WEEK(listOf("week", "w", "weekly"), "Weekly ", IntervalType.CURRENT, TimeUnit.WEEK),
        LAST_WEEK(listOf("lastweek", "prevweek", "lw", "pw"), "Last Week's ", IntervalType.PREVIOUS, TimeUnit.WEEK),
        DAY(
            listOf("t", "today", "d", "day", "daily"),
            "Today's ",
            IntervalType.CURRENT,
            TimeUnit.DAY
        ),
        YESTERDAY(
            listOf("y", "yesterday", "yes", "yday", "pday", "prevday", "lday", "lastday"),
            "Yesterday's ",
            IntervalType.PREVIOUS,
            TimeUnit.DAY
        ),
    }
}
