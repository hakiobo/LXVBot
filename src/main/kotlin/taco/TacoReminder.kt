package taco

import entities.Reminder
import kotlinx.serialization.Serializable

@Serializable
data class TacoReminder(
    val tip: Reminder = Reminder(),
    val work: Reminder = Reminder(),
    val overtime: Reminder = Reminder(),
    val daily: Reminder = Reminder(),
    val clean: Reminder = Reminder(),
    val vote: Reminder = Reminder(),
    val flipper: Reminder = Reminder(),
    val karaoke: Reminder = Reminder(),
    val music: Reminder = Reminder(),
    val airplane: Reminder = Reminder(),
    val chef: Reminder = Reminder(),
    val chairs: Reminder = Reminder(),
    val sail: Reminder = Reminder(),
    val concert: Reminder = Reminder(),
    val tours: Reminder = Reminder(),
    val hammock: Reminder = Reminder(),
)
