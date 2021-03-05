package taco

import Reminder

data class TacoReminder(
    val tip: Reminder = Reminder(),
    val work: Reminder = Reminder(),
    val ot: Reminder = Reminder(),
    val daily: Reminder = Reminder(),
    val flipper: Reminder = Reminder(),
    val karaoke: Reminder = Reminder(),
    val music: Reminder = Reminder(),
    val airplane: Reminder = Reminder(),
    val chef: Reminder = Reminder(),
)
