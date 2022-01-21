package commands

import LXVBot
import LXVBot.Companion.BOT_PREFIX
import LXVBot.Companion.getUserIdFromString
import commands.util.*
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import entities.UserBattleCount
import entities.UserGuildDate
import kotlinx.serialization.Serializable
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.aggregate

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

    private suspend fun LXVBot.getBattlesInRange(
        col: CoroutineCollection<UserBattleCount>,
        user: Snowflake,
        guild: Snowflake,
        startDate: Int,
        endDate: Int
    ): Long {
        return col.aggregate<CountContainer>(
            match(
                UserBattleCount::_id / UserGuildDate::user eq user,
                UserBattleCount::_id / UserGuildDate::guild eq guild,
                UserBattleCount::_id / UserGuildDate::dayId lte endDate,
                UserBattleCount::_id / UserGuildDate::dayId gte startDate,
            ),
            group(null, CountContainer::sum sum UserBattleCount::count)
        ).first()?.sum ?: 0L
    }

    private suspend fun LXVBot.displayBattleStats(mCE: MessageCreateEvent, userId: Snowflake) {
        val guildId = mCE.guildId!!
        val todayId = UserBattleCount.getDayId(mCE.message.id)
        val col = db.getCollection<UserBattleCount>(UserBattleCount.DB_NAME)
        val today = getBattlesInRange(col, userId, guildId, todayId, todayId)
        val week = getBattlesInRange(col, userId, guildId, todayId - 7 + 1, todayId)
        val fortnight = getBattlesInRange(col, userId, guildId, todayId - 14 + 1, todayId)
        val monthy = getBattlesInRange(col, userId, guildId, todayId - 30 + 1, todayId)
        val quartery = getBattlesInRange(col, userId, guildId, todayId - 90 + 1, todayId)
        val yeary = getBattlesInRange(col, userId, guildId, todayId - 365 + 1, todayId)
        val total = getBattlesInRange(col, userId, guildId, 0, todayId)


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

@Serializable
private data class CountContainer(val sum: Long)