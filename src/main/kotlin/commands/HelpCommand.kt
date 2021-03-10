package commands.meta

import LXVBot
import commands.util.*
import dev.kord.common.Color
import dev.kord.core.event.message.MessageCreateEvent
import java.util.*

object HelpCommand : BotCommand {

    override val name: String
        get() = "help"

    override val description: String
        get() = "Displays a list of commands or info about a specific command"

    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(emptyList(), "Displays a list of all commands"),
            CommandUsage(
                listOf(Argument("command", ArgumentType.PARAMETER)),
                "Displays Info about a specific command"
            ),
            CommandUsage(
                listOf(Argument("category", ArgumentType.FLAG_PARAM)),
                "Displays a list of all commands in a specific category"
            ),
        )

    override val category: CommandCategory
        get() = CommandCategory.LXVBOT


    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        when (args.size) {
            0 -> {
                val map: MutableMap<CommandCategory, MutableList<String>> = EnumMap(CommandCategory::class.java)
                for (cmd in commands) {
                    map.getOrPut(cmd.category, { mutableListOf() }).add(cmd.name)
                }
                val self = client.getSelf()
                sendMessage(mCE.message.channel) {
                    title = "${LXVBot.BOT_NAME} Available Commands"
                    color = Color(0x00FF00)
                    footer {
                        icon = self.avatar.url
                        text = "${LXVBot.BOT_PREFIX}help <cmd> for more information about a specific command"
                    }
                    for ((category, cmds) in map) {
                        if (category != CommandCategory.HIDDEN) {
                            field {
                                name = category.category
                                value = "`${cmds.joinToString("` `")}`"
                            }
                        }
                    }
                }
            }
            else -> {
                if (args.first().startsWith("-")) {
                    val categoryStr = args.joinToString(" ").drop(1)
                    val category =
                        CommandCategory.values().find { it.category.toLowerCase() == categoryStr.toLowerCase().trim() }
                    val cmds = commands.filter { it.category == category }
                    if (cmds.isNotEmpty()) {
                        sendMessage(mCE.message.channel) {
                            title = "${category!!.category} Commands"
                            description = cmds.joinToString("\n") { it.name }
                        }
                    } else {
                        sendMessage(mCE.message.channel, "Could find $categoryStr category")
                    }

                } else if (args.size == 1) {
                    val cmd = lookupCMD(args.first())
                    if (cmd != null) {
                        val helpMSG = StringBuilder("Help for `${cmd.name}` command\n")

                        helpMSG.append("**Aliases:** ")
                        if (cmd.aliases.isNotEmpty()) {
                            helpMSG.append("`${cmd.aliases.joinToString("`  `")}`")
                        } else {
                            helpMSG.append("None")
                        }
                        helpMSG.append("\n**Description:** `${cmd.description}`")
                        helpMSG.append("\n**Usage:** ")
                        if (cmd.usages.isEmpty()) {
                            helpMSG.append("**None**")
                        } else {
                            for (usage in cmd.usages) {
                                helpMSG.append("\n`${LXVBot.BOT_PREFIX}${cmd.name}")
                                for (arg in usage.args) {
                                    helpMSG.append(" ").append(arg.argType.prefix).append(arg.text)
                                        .append(arg.argType.suffix)
                                }
                                helpMSG.append("`- ${usage.description}")
                                if (usage.accessType != AccessType.EVERYONE) {
                                    helpMSG.append(" - Requires:`${usage.accessType.desc}`")
                                }
                            }
                        }
                        sendMessage(mCE.message.channel, helpMSG.toString())
                    } else {
                        sendMessage(mCE.message.channel, "No ${args.first()} command found", 5_000)
                    }
                } else {
                    sendMessage(
                        mCE.message.channel,
                        "Invalid help format. Check `${LXVBot.BOT_PREFIX}help help` for more details",
                        5_000
                    )
                }
            }
        }
    }
}