import rpg.RPGData
import taco.TacoData

data class LXVUser(
    val _id: Long,
    val username: String? = null,
    val rpg: RPGData = RPGData(),
    val taco: TacoData = TacoData(),
    val serverData: ServerData = ServerData(),
) {
    companion object {
        const val DB_NAME = "users"
    }
}