package commands.utils

import LXVBot
import dev.kord.core.event.message.MessageCreateEvent

interface BotCommand {
    val name: String
    val description: String
    val aliases: List<String>
        get() = emptyList()
    val usages: List<CommandUsage>
        get() = emptyList()
    val category: CommandCategory
        get() = CommandCategory.MISC


    suspend fun runCMD(bot: LXVBot, mCE: MessageCreateEvent, args: List<String>) {
        bot.cmd(mCE, args)
    }

    suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>)
}

data class CommandUsage(
    val args: List<Argument>,
    val description: String,
    val accessType: AccessType = AccessType.EVERYONE
) {
}

enum class AccessType(val desc: String) {
    EVERYONE("Anyone"),
    ADMIN("Server Admin"),
    HAKI("Hakiobo"),
    OWNER("Server Owner"),
}

data class Argument(val text: String, val argType: ArgumentType = ArgumentType.PARAMETER) {
    constructor(args: List<String>, choiceType: ChoiceType = ChoiceType.EXACT) : this(
        args.joinToString("|"),
        choiceType.argType
    )
}

enum class ChoiceType(val argType: ArgumentType) {
    EXACT(ArgumentType.CHOICE), DESCRIPTION(ArgumentType.PARAMETER)
}

enum class ArgumentType(val prefix: String, val suffix: String) {
    FLAG("-", ""),
    FLAG_PARAM("-<", ">"),
    EXACT("", ""),
    PARAMETER("<", ">"),
    TEXT("{", "}"),
    CHOICE("[", "]"),
    VARARG("<", ">..."),
}

enum class CommandCategory(val category: String) {
    HIDDEN("Hidden"),
    REMINDER("Reminders"),
    GUILD("Guild"),
    HAKIBOT("Bot Related"),
    MISC("Miscellaneous"),
}
