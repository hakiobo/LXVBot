package entities

import dev.kord.common.Color
import dev.kord.rest.builder.message.EmbedBuilder
import org.bson.codecs.pojo.annotations.BsonId
import kotlin.random.Random

data class CustomPatreon(
    @BsonId val name: String,
    val hp: Int,
    val str: Int,
    val pr: Int,
    val wp: Int,
    val mag: Int,
    val mr: Int,
    val aliases: List<String> = emptyList(),
    val creationInfo: CreationInfo? = null,
    val lastUpdatedMS: Long = 0,
    val imageLink: String? = null
) {
    constructor(
        name: String,
        stats: List<Int>,
        aliases: List<String> = emptyList(),
        creationInfo: CreationInfo? = null,
        lastUpdatedMS: Long = 0,
        imageLink: String? = null
    ) : this(
        name,
        stats[0],
        stats[1],
        stats[2],
        stats[3],
        stats[4],
        stats[5],
        aliases,
        creationInfo,
        lastUpdatedMS,
        imageLink
    )


    override fun toString(): String {
        val sb =
            StringBuilder("Name: $name\n    Stats:\n        hp: $hp str: $str pr: $pr\n        wp: $wp mag: $mag mr: $mr")
        sb.append("\n    Aliases: ${if (aliases.isEmpty()) "None" else aliases.joinToString(", ")}")
        if (creationInfo != null) {
            sb.append("\n    Created: $creationInfo")
        }
        if (imageLink != null) {
            sb.append("\n    With Image")
        }
        return sb.toString()
    }

    fun simpleString(): String {
        return "$name\n$hp $str $pr $wp $mag $mr"
    }


    fun toEmbed(embed: EmbedBuilder = EmbedBuilder()): EmbedBuilder = embed.apply {
        color = Color(random.nextInt(0x1000000))
        title = name
        if (imageLink != null) {
            thumbnail {
                url = imageLink
            }
        }
        field {

            name = "Aliases"
            value = if (aliases.isEmpty()) {
                "*None*"
            } else {
                aliases.joinToString(", ")
            }
        }
        if (creationInfo != null) {
            footer {
                text = "Created $creationInfo"
            }
        }

        field {
            name = "Stats"
            value =
                "<:hp:816897952345751582> `$hp` <:att:816897930761469981> `$str` <:pr:816897908481589258> `$pr`\n<:wp:816897962181263411> `$wp` <:mag:816897941088108614> `$mag` <:mr:816897919093309440> `$mr`"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomPatreon

        if (name != other.name) return false
        if (hp != other.hp) return false
        if (str != other.str) return false
        if (pr != other.pr) return false
        if (wp != other.wp) return false
        if (mag != other.mag) return false
        if (mr != other.mr) return false
        if (aliases != other.aliases) return false
        if (creationInfo != other.creationInfo) return false
        if (imageLink != other.imageLink) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + hp
        result = 31 * result + str
        result = 31 * result + pr
        result = 31 * result + wp
        result = 31 * result + mag
        result = 31 * result + mr
        return result
    }

    companion object {
        val random = Random.Default
        val DB_NAME = "cp"
    }
}
