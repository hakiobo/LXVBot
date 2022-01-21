package entities

import LXVBot
import LXVBot.Companion.toDate
import dev.kord.common.entity.Snowflake
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.aggregate
import java.time.Month

@Serializable
data class UserBattleCount(
    val _id: UserGuildDate,
    val count: Long = 0L,
) {
    companion object {
        const val DB_NAME = "battle-count"

        suspend fun LXVBot.countBattle(messageId: Snowflake, userId: Snowflake, guildId: Snowflake) {
            val col = db.getCollection<UserBattleCount>(DB_NAME)
            val key = UserGuildDate(
                userId,
                guildId,
                messageId.toDate().getDayId()
            )
            val user = col.findOne(UserBattleCount::_id eq key)
            if (user == null) {
                col.insertOne(UserBattleCount(key, 1L))
            } else {
                col.updateOne(UserBattleCount::_id eq key, set(UserBattleCount::count setTo (user.count + 1)))
            }
        }

        suspend fun getBattlesInRange(
            col: CoroutineCollection<UserBattleCount>,
            user: Snowflake,
            guild: Snowflake,
            startDate: LocalDate,
            endDate: LocalDate
        ): Long {
            return col.aggregate<CountContainer>(
                match(
                    UserBattleCount::_id / UserGuildDate::user eq user,
                    UserBattleCount::_id / UserGuildDate::guild eq guild,
                    UserBattleCount::_id / UserGuildDate::dayId lte endDate.getDayId(),
                    UserBattleCount::_id / UserGuildDate::dayId gte startDate.getDayId(),
                ),
                group(null, CountContainer::sum sum UserBattleCount::count)
            ).first()?.sum ?: 0L
        }

        @Serializable
        data class CountContainer(val sum: Long)

        @Serializable
        data class TopContainer(val sum: Long, val _id: Snowflake)

        suspend fun getLeaderboardBattlesInRange(
            col: CoroutineCollection<UserBattleCount>,
            guild: Snowflake,
            startDate: LocalDate,
            endDate: LocalDate,
            toSkip: Int,
            toKeep: Int
        ): List<TopContainer> {
            return col.aggregate<TopContainer>(
                match(
                    UserBattleCount::_id / UserGuildDate::guild eq guild,
                    UserBattleCount::_id / UserGuildDate::dayId lte endDate.getDayId(),
                    UserBattleCount::_id / UserGuildDate::dayId gte startDate.getDayId(),
                ),
                group(
                    UserBattleCount::_id / UserGuildDate::user,
                    TopContainer::sum sum UserBattleCount::count
                ),
                sort(descending(TopContainer::sum, TopContainer::_id)),
                skip(toSkip),
                limit(toKeep),
            ).toList()
        }

        val epoch = LocalDate(2000, Month.JANUARY, 1)
        fun LocalDate.getDayId(): Int {
            return epoch.daysUntil(this)
        }
    }
}
