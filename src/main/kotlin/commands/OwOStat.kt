package commands

import LXVBot
import LXVBot.Companion.getUserIdFromString
import UserGuildOwOCount
import commands.utils.*
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
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
                displayOwOStats(mCE, mCE.member!!.id.value)
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
                sendMessage(mCE.message.channel, "Invalid Format, expecting h!owostat <userId|mention>", 5_000)
            }
        }
    }

    private suspend fun LXVBot.displayOwOStats(mCE: MessageCreateEvent, userId: Long) {
        val query = hakiDb.getCollection<UserGuildOwOCount>(UserGuildOwOCount.DB_NAME)
            .findOne(UserGuildOwOCount::_id eq "$userId|${mCE.guildId!!.value}")
        if (query == null) {
            sendMessage(mCE.message.channel, "Could not find any OwO's for that user in this server", 10_000)
        } else {
            val now = mCE.message.id.timeStamp.atZone(LXVBot.PST)
            query.normalize(mCE)
            val username = getUserFromDB(Snowflake(query.user)).username!!
            val guildName = mCE.getGuild()!!.name
            val avatar = client.getUser(Snowflake(userId))?.avatar?.url
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
                            "__${now.month.name.toLowerCase().capitalize()}__: ${query.monthlyCount}\n" +
                            "__${now.year}__: ${query.yearlyCount}"
                }
                field {
                    name = "Past Stats"
                    value = "__Yesterday__: ${query.yesterdayCount}\n" +
                            "__Last Week__: ${query.lastWeekCount}\n" +
                            "__${
                                now.minusMonths(1).month.name.toLowerCase().capitalize()
                            }__: ${query.lastMonthCount}\n" +
                            "__${now.year - 1}__: ${query.lastYearCount}"
                }
                color = Color(0xABCDEF)
            }
        }
    }
}