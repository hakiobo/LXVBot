package commands

import LXVBot
import LXVBot.Companion.toOwODate
import commands.util.*
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import entities.UserDailyStats
import kotlinx.coroutines.async
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlin.reflect.KProperty1

interface StatCommand : BotCommand {
    val statName: String
    val statType: KProperty1<UserDailyStats, Long>


    suspend fun LXVBot.displayStats(mCE: MessageCreateEvent, userId: Snowflake) {
        val guildId = mCE.guildId!!
        val todayDate = mCE.message.id.timestamp.toOwODate()
        val col = db.getCollection<UserDailyStats>(UserDailyStats.DB_NAME)

        val todayResult = client.async {
            UserDailyStats.getStatInRange(
                col,
                userId,
                guildId,
                todayDate,
                todayDate,
                statType,
            )
        }
        val yesterdayResult = client.async {
            UserDailyStats.getStatInRange(
                col,
                userId,
                guildId,
                todayDate.minus(DateTimeUnit.DAY),
                todayDate.minus(DateTimeUnit.DAY),
                statType,
            )
        }

        val thisWeekResult = client.async {
            UserDailyStats.getStatInRange(
                col,
                userId,
                guildId,
                todayDate.startOfWeek(),
                todayDate.minus(DateTimeUnit.DAY),
                statType,
            )
        }
        val thisMonthResult = client.async {
            UserDailyStats.getStatInRange(
                col,
                userId,
                guildId,
                todayDate.startOfMonth(),
                todayDate.minus(DateTimeUnit.DAY),
                statType,
            )
        }
        val thisYearResult = client.async {
            UserDailyStats.getStatInRange(
                col,
                userId,
                guildId,
                todayDate.startOfYear(),
                todayDate.endOfMonth(1),
                statType,
            )
        }

        val lastWeekResult = client.async {
            UserDailyStats.getStatInRange(
                col,
                userId,
                guildId,
                todayDate.startOfWeek(1),
                todayDate.endOfWeek(1),
                statType,
            )
        }
        val lastMonthResult = client.async {
            UserDailyStats.getStatInRange(
                col,
                userId,
                guildId,
                todayDate.startOfMonth(1),
                todayDate.endOfMonth(1),
                statType,
            )
        }
        val lastYearResult = client.async {
            UserDailyStats.getStatInRange(
                col,
                userId,
                guildId,
                todayDate.startOfYear(1),
                todayDate.endOfYear(1),
                statType,
            )
        }
        val totalResult = client.async {
            UserDailyStats.getStatInRange(
                col,
                userId,
                guildId,
                UserDailyStats.epoch,
                todayDate.endOfYear(2),
                statType,
            )
        }

        val usernameResult = client.async { getUserFromDB(userId).username!! }
        val guildNameResult = client.async { mCE.getGuild()!!.name }
        val avatarResult = client.async { client.getUser(userId)?.avatar?.url }

        val today = todayResult.await()
        val thisWeek = thisWeekResult.await() + today
        val thisMonth = thisMonthResult.await() + today
        val thisYear = thisYearResult.await() + thisMonth
        val yesterday = yesterdayResult.await()
        val lastWeek = lastWeekResult.await()
        val lastMonth = lastMonthResult.await()
        val lastYear = lastYearResult.await()
        val total = totalResult.await() + lastYear + thisYear

        val username = usernameResult.await()
        val guildName = guildNameResult.await()
        val avatar = avatarResult.await()



        sendMessage(mCE.message.channel) {
            author {
                name = "$username's ${statName}s in $guildName"
                icon = avatar
            }
            description = "__**Total**__: $total\n"
            field {
                name = "Current Stats"
                value = "__Today__: $today\n" +
                        "__This Week__: $thisWeek\n" +
                        "__${
                            todayDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
                        }__: ${thisMonth}\n" +
                        "__${todayDate.year}__: $thisYear\n"
            }
            field {
                name = "Past Stats"
                value = "__Yesterday__: $yesterday\n" +
                        "__Last Week__: $lastWeek\n" +
                        "__${
                            todayDate.startOfMonth(1).month.name.lowercase().replaceFirstChar { it.uppercase() }
                        }__: ${lastMonth}\n" +
                        "__${todayDate.year - 1}__: $lastYear\n"
            }

            color = Color(0xABCDEF)
        }
    }
}