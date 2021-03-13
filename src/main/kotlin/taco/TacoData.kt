package taco

data class TacoData(
    val tacoReminders: TacoReminder = TacoReminder(),
    val patreonLevel: String = TacoPatreonLevel.NONE.id
)
