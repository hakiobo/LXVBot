package taco

import Reminder
import kotlin.reflect.KProperty1

data class TacoReminderType(val aliases: List<String>, val name: KProperty1<TacoReminder, Reminder>)