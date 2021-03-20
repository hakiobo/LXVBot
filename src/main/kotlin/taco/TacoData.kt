package taco

data class TacoData(
    val tacoReminders: TacoReminder = TacoReminder(),
    val donorLevel: String = TacoPatreonLevel.NONE.id
)
