package entities

import kotlinx.serialization.Serializable

@Serializable
data class Reminder(val enabled : Boolean = false, val lastUse: Long = 0L, val count: Int = 0)
