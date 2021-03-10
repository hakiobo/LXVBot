package entities

import org.bson.codecs.pojo.annotations.BsonId

data class StoredReminder(
    @BsonId val srcMsg: Long,
    val reminderTime: Long,
    val category: String,
    val type: String,
    val channelId: Long,
    val otherData: Long,
) {
    companion object {
        const val DB_NAME = "reminders"
    }
}
