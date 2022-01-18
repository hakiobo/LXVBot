package entities

import kotlinx.serialization.Serializable

@Serializable
data class ServerData(
    val mee6Level: Int = 0,
    val customChannel: ULong? = null,
    val picBanned: Boolean = false,
)
