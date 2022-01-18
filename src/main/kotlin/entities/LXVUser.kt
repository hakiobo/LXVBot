package entities

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import rpg.RPGData
import taco.TacoData

@Serializable
data class LXVUser(
    @Contextual val _id: ULong,
    val username: String? = null,
    val rpg: RPGData = RPGData(),
    val taco: TacoData = TacoData(),
    val serverData: ServerData = ServerData(),
) {
    companion object {
        const val DB_NAME = "users"
    }
}