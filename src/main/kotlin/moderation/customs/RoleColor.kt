package moderation.customs

import LXVBot
import commands.util.BotCommand
import dev.kord.common.Color
import dev.kord.core.behavior.edit
import dev.kord.core.event.message.MessageCreateEvent

object RoleColor : BotCommand {
    override val name: String
        get() = "rolecolor"
    override val description: String
        get() = "Change your custom role color"
    override val aliases: List<String>
        get() = listOf("rcolor", "rcolour", "rolecolour")

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        if (requireLXVGuild(mCE)) return
        val lxvUser = getUserFromDB(mCE.message.author!!.id, mCE.message.author)
        val roleId = lxvUser.serverData.customRole
        if (roleId == null) {
            reply(mCE.message, "You don't have a custom role")
            return
        }
        val realRole = mCE.getGuild()!!.getRoleOrNull(roleId)
        if (realRole == null) {
            reply(mCE.message, "Looks like your role doesn't exist anymore, RIP")
            return
        }
        if (args.isEmpty()) {
            reply(mCE.message, "Your current Role color is #${realRole.color.rgb.toString(16)}")
            return
        } else if (args.size != 1) {
            reply(mCE.message, "Invalid format")
            return
        }

        val newColor = stringToColor(args.first())
        if (newColor == null) {
            reply(mCE.message, "invalid color")
            return
        }
        realRole.edit {
            this.color = newColor
        }
        reply(mCE.message, "You look so beautiful \uD83D\uDE2D")
    }

    private fun stringToColor(s: String): Color? {
        val s2 = s.removePrefix("#")
        return when (s2.length) {
            3 -> {
                val num = s2.toIntOrNull(16)
                if (num == null) {
                    null
                } else {
                    val mask = (1 shl 4) - 1
                    val red = num shr 8
                    val green = (num shr 4) and mask
                    val blue = num and mask
                    Color(red * 17, green * 17, blue * 17)
                }
            }
            6 -> {
                val num = s2.toIntOrNull(16)
                if (num == null) {
                    null
                } else {
                    val mask = (1 shl 8) - 1
                    val red = num shr 16
                    val green = (num shr 8) and mask
                    val blue = num and mask
                    Color(red, green, blue)
                }
            }
            else -> null
        }
    }
}