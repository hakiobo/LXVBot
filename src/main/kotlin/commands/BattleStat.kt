package commands

import LXVBot
import LXVBot.Companion.BOT_PREFIX
import LXVBot.Companion.getUserIdFromString
import LXVBot.Companion.toDate
import commands.util.*
import kotlinx.datetime.*
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import entities.UserBattleCount
import entities.UserBattleCount.Companion.epoch
import entities.UserBattleCount.Companion.getBattlesInRange
import kotlinx.datetime.DateTimeUnit


object BattleStat : BotCommand {
    override val name: String
        get() = "battlestat"
    override val aliases: List<String>
        get() = listOf("b", "battles", "bs", "bstat")
    override val category: CommandCategory
        get() = CommandCategory.GUILD
    override val description: String
        get() = "Gets a user's OwO battle count stats for this server"
    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(listOf(), "Gets your OwO Battle stats in this server"),
            CommandUsage(
                listOf(
                    Argument(listOf("userId", "userMention"), ChoiceType.DESCRIPTION)
                ),
                "Gets the OwO battle stats for the given user in this server"
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
                sendMessage(
                    mCE.message.channel,
                    "Invalid Format, expecting ${BOT_PREFIX}${this@BattleStat.name} <userId|mention>",
                    5_000
                )
            }
        }
    }

    private suspend fun LXVBot.displayBattleStats(mCE: MessageCreateEvent, userId: Snowflake) {
        val guildId = mCE.guildId!!
        val todayDate = mCE.message.id.toDate()
        val col = db.getCollection<UserBattleCount>(UserBattleCount.DB_NAME)
        val today = getBattlesInRange(col, userId, guildId, todayDate, todayDate)

        val thisWeek = getBattlesInRange(col, userId, guildId, todayDate.startOfWeek(), todayDate.endOfWeek())
        val thisMonth = getBattlesInRange(col, userId, guildId, todayDate.startOfMonth(), todayDate.endOfMonth())
        val thisYear = getBattlesInRange(col, userId, guildId, todayDate.startOfYear(), todayDate.endOfYear())
        val yesterday = getBattlesInRange(
            col,
            userId,
            guildId,
            todayDate.minus(DateTimeUnit.DAY),
            todayDate.minus(DateTimeUnit.DAY)
        )
        val lastWeek = getBattlesInRange(col, userId, guildId, todayDate.startOfWeek(1), todayDate.endOfWeek(1))
        val lastMonth = getBattlesInRange(col, userId, guildId, todayDate.startOfMonth(1), todayDate.endOfMonth(1))
        val lastYear = getBattlesInRange(col, userId, guildId, todayDate.startOfYear(1), todayDate.endOfYear(1))
        val total = getBattlesInRange(col, userId, guildId, epoch, todayDate)


        val username = getUserFromDB(userId).username!!
        val guildName = mCE.getGuild()!!.name
        val avatar = client.getUser(userId)?.avatar?.url
        sendMessage(mCE.message.channel) {
            author {
                name = "$username's Battles in $guildName"
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

            color = Color(0xFEDCBA)
        }
    }
}

