import rpg.RPGData

data class LXVUser(
    val _id: Long,
    val username: String? = null,
    val rpg: RPGData = RPGData(),
) {
    companion object {
        const val DB_NAME = "users"
    }
}