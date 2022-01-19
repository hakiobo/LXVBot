package entities

import LXVBot
import dev.kord.common.entity.Snowflake
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import org.litote.kmongo.set
import org.litote.kmongo.setTo
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
                getDayId(messageId)
            )
            val user = col.findOne(UserBattleCount::_id eq key)
            if (user == null) {
                col.insertOne(UserBattleCount(key, 1L))
            } else {
                col.updateOne(UserBattleCount::_id eq key, set(UserBattleCount::count setTo (user.count + 1)))
            }
        }

        val epoch = LocalDate(2000, Month.JANUARY, 1)
        fun getDayId(messageId: Snowflake): Int {
            return epoch.daysUntil(messageId.timestamp.toLocalDateTime(LXVBot.PST).date)
        }

    }
}
