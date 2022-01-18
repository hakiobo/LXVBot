package entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoredReminder(
    @SerialName("_id") val srcMsg: Snowflake,
    val reminderTime: Long,
    val category: String,
    val type: String,
    val channelId: Snowflake,
    val otherData: Snowflake,
    val oldMsg: String = "",
) {
    companion object {
        const val DB_NAME = "reminders"
    }
}
