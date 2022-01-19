package entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class UserGuildDate(
    val user: Snowflake,
    val guild: Snowflake,
    val dayId: Int,
)
