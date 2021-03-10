package entities

data class Reminder(val enabled : Boolean = false, val lastUse: Long = 0L, val count: Int = 0)
