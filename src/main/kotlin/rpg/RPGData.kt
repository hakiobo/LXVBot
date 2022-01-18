package rpg

import entities.Reminder
import kotlinx.serialization.Serializable

@Serializable
data class RPGData(
    val rpgReminders: MutableMap<String, Reminder> = hashMapOf(),
    val patreonMult: Double = 1.0,
    val partnerPatreon: Double = 1.0
)