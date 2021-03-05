package commands

import LXVBot
import commands.utils.BotCommand
import commands.utils.CommandCategory
import dev.kord.common.Color
import dev.kord.core.event.message.MessageCreateEvent

object Invite : BotCommand {
    override val name: String
        get() = "invite"
    override val description: String
        get() = "Learn why you can't invite ${LXVBot.BOT_PREFIX} to your own server!"
    override val category: CommandCategory
        get() = CommandCategory.LXVBOT

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        reply(mCE.message){
            title = "No Invite Available <:blobsob:816967841353695244>"
            color = Color(0x8899FF)
            description = "Sorry, LXV Bot is only for Lxvesick Server. You can try [Hakibot](https://discord.com/api/oauth2/authorize?client_id=750534176666550384&permissions=346176&scope=bot) though"
        }
    }

}