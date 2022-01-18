package taco

import kotlinx.serialization.Serializable

@Serializable
data class TacoData(
    val tacoReminders: TacoReminder = TacoReminder(),
    val donorLevel: String = TacoPatreonLevel.NONE.id
)
