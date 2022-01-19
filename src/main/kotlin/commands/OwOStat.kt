package commands

import LXVBot
import LXVBot.Companion.getUserIdFromString
import commands.util.*
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import entities.UserBattleCount
import entities.UserGuildDate
import entities.UserGuildOwOCount
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.litote.kmongo.eq

object OwOStat : BotCommand {
    override val name: String
        get() = "owostat"
    override val aliases: List<String>
        get() = listOf("owos", "ostat", "stat")
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
                displayBattleStats(mCE, mCE.member!!.id)
            }
            1 -> {
                val userId = getUserIdFromString(args.first())
                if (userId == null) {
                    sendMessage(mCE.message.channel, "That's not a user", 5_000)
                } else {
                    displayBattleStats(mCE, userId)
                }
            }
            else -> {
                sendMessage(mCE.message.channel, "Invalid Format, expecting h!owostat <userId|mention>", 5_000)
            }
        }
    }

    private suspend fun LXVBot.displayOwOStats(mCE: MessageCreateEvent, userId: Snowflake) {
        val query = hakiDb.getCollection<UserGuildOwOCount>(UserGuildOwOCount.DB_NAME)
            .findOne(UserGuildOwOCount::_id eq "$userId|${mCE.guildId!!.value}")
        if (query == null) {
            sendMessage(mCE.message.channel, "Could not find any OwO's for that user in this server", 10_000)
        } else {
            val now = mCE.message.id.timestamp.toLocalDateTime(LXVBot.PST).date
            query.normalize(mCE)
            val username = getUserFromDB(query.user).username!!
            val guildName = mCE.getGuild()!!.name
            val avatar = client.getUser(userId)?.avatar?.url
            sendMessage(mCE.message.channel) {
                author {
                    name = "$username's OwOs in $guildName"
                    icon = avatar
                }
                description = "__**Total**__: ${query.owoCount}\n"
                field {
                    name = "Current Stats"
                    value = "__Today__: ${query.dailyCount}\n" +
                            "__This Week__: ${query.weeklyCount}\n" +
                            "__${
                                now.month.name.lowercase().replaceFirstChar { it.uppercase() }
                            }__: ${query.monthlyCount}\n" +
                            "__${now.year}__: ${query.yearlyCount}"
                }
                field {
                    name = "Past Stats"
                    value = "__Yesterday__: ${query.yesterdayCount}\n" +
                            "__Last Week__: ${query.lastWeekCount}\n" +
                            "__${
                                now.minus(DateTimeUnit.MONTH).month.name.lowercase().replaceFirstChar { it.uppercase() }
                            }__: ${query.lastMonthCount}\n" +
                            "__${now.year - 1}__: ${query.lastYearCount}"
                }
                color = Color(0xABCDEF)
            }
        }
    }

    private suspend fun LXVBot.displayBattleStats(mCE: MessageCreateEvent, userId: Snowflake) {
        val key = UserGuildDate(userId, mCE.guildId!!, UserBattleCount.getDayId(mCE.message.id))
        val col = db.getCollection<UserBattleCount>(UserBattleCount.DB_NAME)
        val today = col.findOne(UserBattleCount::_id eq key) ?: UserBattleCount(key)
        val yesterday = col.findOne(UserBattleCount::_id eq key.copy(dayId = key.dayId - 1)) ?: UserBattleCount(key)
        val username = getUserFromDB(userId).username!!
        val guildName = mCE.getGuild()!!.name
        val avatar = client.getUser(userId)?.avatar?.url
        sendMessage(mCE.message.channel) {
            author {
                name = "$username's Battles in $guildName"
                icon = avatar
            }
            description = "__**Total**__: idk btw these aren't going to be saved after today\n"
            field {
                name = "Current Stats"
                value = "__Today__: ${today.count}\n"
            }
            field {
                name = "Past Stats"
                value = "__Yesterday__: ${yesterday.count}\n"
            }
            color = Color(0xFEDCBA)

        }
    }

}