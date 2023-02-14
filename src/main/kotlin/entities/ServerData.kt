package entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class ServerData(
    val mee6Level: Int = 0,
    val customChannel: Snowflake? = null,
    val customRole: Snowflake? = null,
    val picBanned: Boolean = false,
    val recruitData: RecruitData? = null,
)
