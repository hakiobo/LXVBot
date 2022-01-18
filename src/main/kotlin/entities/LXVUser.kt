package entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import rpg.RPGData
import taco.TacoData

@Serializable
data class LXVUser(
    val _id: Snowflake,
    val username: String? = null,
    val rpg: RPGData = RPGData(),
    val taco: TacoData = TacoData(),
    val serverData: ServerData = ServerData(),
) {
    companion object {
        const val DB_NAME = "users"
    }
}