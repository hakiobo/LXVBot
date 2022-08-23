package commands

import LXVBot
import LXVBot.Companion.BOT_PREFIX
import LXVBot.Companion.getUserIdFromString
import LXVBot.Companion.toOwODate
import commands.util.*
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import entities.UserDailyStats
import entities.UserGuildOwOCount
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.litote.kmongo.eq

object OwOStat : BotCommand {
    override val name: String
        get() = "owostat"
    override val aliases: List<String>
        get() = listOf("o", "owos", "ostat", "os", "stat", "s")
    override val category: CommandCategory
        get() = CommandCategory.GUILD
    override val description: String
        get() = "Gets a user's owo count stats for this server"

    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(listOf(), "Gets your OwO stats in this server"),
            CommandUsage(
                listOf(
                    Argument(listOf("userId", "userMention"), ChoiceType.DESCRIPTION)
                ),
                "Gets the OwO stats for the given user in this server"
            ),
        )

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        when (args.size) {
            0 -> {
                displayOwOStats(mCE, mCE.member!!.id)
            }
            1 -> {
                val userId = getUserIdFromString(args.first())
                if (userId == null) {
                    sendMessage(mCE.message.channel, "That's not a user", 5_000)
                } else {
                    displayOwOStats(mCE, userId)
                }
            }
            else -> {
                sendMessage(
                    mCE.message.channel,
                    "Invalid Format, expecting ${BOT_PREFIX}${this@OwOStat.name} <userId|mention>",
                    5_000
                )
            }
        }
    }

    private suspend fun LXVBot.displayOwOStats(mCE: MessageCreateEvent, userId: Snowflake) {
        val guildId = mCE.guildId!!
        val todayDate = mCE.message.id.timestamp.toOwODate()
        val col = db.getCollection<UserDailyStats>(UserDailyStats.DB_NAME)
        val today =
            UserDailyStats.getOwOStatInRange(col, userId, guildId, todayDate, todayDate, UserDailyStats::owoCount)

        val thisWeek = UserDailyStats.getOwOStatInRange(
            col,
            userId,
            guildId,
            todayDate.startOfWeek(),
            todayDate.endOfWeek(),
            UserDailyStats::owoCount
        )
        val thisMonth = UserDailyStats.getOwOStatInRange(
            col,
            userId,
            guildId,
            todayDate.startOfMonth(),
            todayDate.endOfMonth(),
            UserDailyStats::owoCount
        )
        val thisYear = UserDailyStats.getOwOStatInRange(
            col,
            userId,
            guildId,
            todayDate.startOfYear(),
            todayDate.endOfYear(),
            UserDailyStats::owoCount
        )
        val yesterday = UserDailyStats.getOwOStatInRange(
            col,
            userId,
            guildId,
            todayDate.minus(DateTimeUnit.DAY),
            todayDate.minus(DateTimeUnit.DAY),
            UserDailyStats::owoCount
        )
        val lastWeek = UserDailyStats.getOwOStatInRange(
            col,
            userId,
            guildId,
            todayDate.startOfWeek(1),
            todayDate.endOfWeek(1),
            UserDailyStats::owoCount
        )
        val lastMonth = UserDailyStats.getOwOStatInRange(
            col,
            userId,
            guildId,
            todayDate.startOfMonth(1),
            todayDate.endOfMonth(1),
            UserDailyStats::owoCount
        )
        val lastYear = UserDailyStats.getOwOStatInRange(
            col,
            userId,
            guildId,
            todayDate.startOfYear(1),
            todayDate.endOfYear(1),
            UserDailyStats::owoCount
        )
        val total = UserDailyStats.getOwOStatInRange(
            col,
            userId,
            guildId,
            UserDailyStats.epoch,
            todayDate,
            UserDailyStats::owoCount
        )


        val username = getUserFromDB(userId).username!!
        val guildName = mCE.getGuild()!!.name
        val avatar = client.getUser(userId)?.avatar?.url
        sendMessage(mCE.message.channel) {
            author {
                    name = "$username's OwOs in $guildName"
                    icon = avatar
                }
            description = "__**Total**__: $total\n"
            field {
                name = "Current Stats"
                value = "__Today__: $today\n" +
                        "__This Week__: $thisWeek\n" +
                        "__This Month__: $thisMonth\n" +
                        "__This Year__: $thisYear\n"
            }
            field {
                name = "Past Stats"
                value = "__Yesterday__: $yesterday\n" +
                        "__Last Week__: $lastWeek\n" +
                        "__Last Month__: $lastMonth\n" +
                        "__Last Year__: $lastYear\n"
            }

            color = Color(0xABCDEF)
        }
    }
//    private suspend fun LXVBot.displayOwOStats(mCE: MessageCreateEvent, userId: Snowflake) {
//        val query = hakiDb.getCollection<UserGuildOwOCount>(UserGuildOwOCount.DB_NAME)
//            .findOne(UserGuildOwOCount::_id eq "$userId|${mCE.guildId!!.value}")
//        if (query == null) {
//            sendMessage(mCE.message.channel, "Could not find any OwO's for that user in this server", 10_000)
//        } else {
//            val now = mCE.message.id.timestamp.toLocalDateTime(LXVBot.PST).date
//            query.normalize(mCE)
//            val username = getUserFromDB(query.user).username!!
//            val guildName = mCE.getGuild()!!.name
//            val avatar = client.getUser(userId)?.avatar?.url
//            sendMessage(mCE.message.channel) {
//                author {
//                    name = "$username's OwOs in $guildName"
//                    icon = avatar
//                }
//                description = "__**Total**__: ${query.owoCount}\n"
//                field {
//                    name = "Current Stats"
//                    value = "__Today__: ${query.dailyCount}\n" +
//                            "__This Week__: ${query.weeklyCount}\n" +
//                            "__${
//                                now.month.name.lowercase().replaceFirstChar { it.uppercase() }
//                            }__: ${query.monthlyCount}\n" +
//                            "__${now.year}__: ${query.yearlyCount}"
//                }
//                field {
//                    name = "Past Stats"
//                    value = "__Yesterday__: ${query.yesterdayCount}\n" +
//                            "__Last Week__: ${query.lastWeekCount}\n" +
//                            "__${
//                                now.minus(DateTimeUnit.MONTH).month.name.lowercase().replaceFirstChar { it.uppercase() }
//                            }__: ${query.lastMonthCount}\n" +
//                            "__${now.year - 1}__: ${query.lastYearCount}"
//                }
//                color = Color(0xABCDEF)
//            }
//        }
//    }

}