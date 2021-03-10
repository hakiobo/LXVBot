package rpg

import entities.Reminder

data class RPGData(
    val rpgReminders: MutableMap<String, Reminder> = hashMapOf(),
    val patreonMult: Double = 1.0,
    val partnerPatreon: Double = 1.0
)