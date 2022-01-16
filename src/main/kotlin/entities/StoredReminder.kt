package entities

import org.bson.codecs.pojo.annotations.BsonId

data class StoredReminder(
    @BsonId val srcMsg: ULong,
    val reminderTime: Long,
    val category: String,
    val type: String,
    val channelId: ULong,
    val otherData: ULong,
    val oldMsg: String = "",
) {
    companion object {
        const val DB_NAME = "reminders"
    }
}
