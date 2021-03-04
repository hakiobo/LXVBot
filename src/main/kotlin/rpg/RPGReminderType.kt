package rpg

class RPGReminderType(
    val name: String,
    val aliases: List<String>,
    val cooldownMS: Long,
    val patreonAffected: Boolean,
    val responseName: RPGReminderType.(List<String>) -> String = { this.name.capitalize() },
    val validator: (List<String>) -> Boolean = { true }
) {
}