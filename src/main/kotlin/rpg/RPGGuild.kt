package rpg

import org.bson.codecs.pojo.annotations.BsonId

data class RPGGuild(@BsonId val name: String, val members: MutableList<Long> = mutableListOf())
