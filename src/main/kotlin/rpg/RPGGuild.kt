package rpg

import kotlinx.serialization.SerialName

data class RPGGuild(@SerialName("_id") val name: String, val members: MutableList<Long> = mutableListOf())
