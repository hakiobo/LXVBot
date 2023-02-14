package entities

import kotlinx.serialization.Serializable

@Serializable
data class RecruitData(
    val startTime: Long,
    val endTime: Long,
    val initOwO: Long,
    val goalOwO: Long,
    val initBattleOwO: Long,
    val goalBattle: Long,
    val active: Boolean,
)
