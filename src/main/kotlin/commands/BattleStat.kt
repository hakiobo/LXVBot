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
        val week = getBattlesInRange(col, userId, guildId, todayDate.minus(7 - 1, DateTimeUnit.DAY), todayDate)
        val fortnight = getBattlesInRange(col, userId, guildId, todayDate.minus(14 - 1, DateTimeUnit.DAY), todayDate)
        val monthy = getBattlesInRange(col, userId, guildId, todayDate.minus(30 - 1, DateTimeUnit.DAY), todayDate)
        val quartery = getBattlesInRange(col, userId, guildId, todayDate.minus(90 - 1, DateTimeUnit.DAY), todayDate)
        val yeary = getBattlesInRange(col, userId, guildId, todayDate.minus(365 - 1, DateTimeUnit.DAY), todayDate)
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
                        "__Past   7 days__: $week\n" +
                        "__Past  14 days__: $fortnight\n" +
                        "__Past  30 days__: $monthy\n" +
                        "__Past  90 days__: $quartery\n" +
                        "__Past 365 days__: $yeary\n"
            }

            color = Color(0xFEDCBA)
        }
    }
}

