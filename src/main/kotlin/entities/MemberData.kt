package entities

import kotlinx.serialization.Serializable

@Serializable
data class MemberData(
    val recruitmentOwOs: Long,
    val recruitmentBattles: Long,
)
