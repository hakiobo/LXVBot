data class RPGData(
    val rpgReminders: MutableMap<String, Reminder> = hashMapOf(),
    val patreonMult: Double = 1.0
) {
}