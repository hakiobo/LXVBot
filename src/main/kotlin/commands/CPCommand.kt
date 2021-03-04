package commands

import CreationInfo
import CustomPatreon
import LXVBot
import commands.utils.*
import dev.kord.core.event.message.MessageCreateEvent
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

object CPCommand : BotCommand {

    override val name: String
        get() = "cp"

    override val aliases: List<String>
        get() = listOf("custompatreon", "pet", "pets", "cps")

    override val description: String
        get() = "Get or Query cp stats"

    override val usages: List<CommandUsage>
        get() = listOf(
            CommandUsage(listOf(), "Shows the total number of pets stored"),
            CommandUsage(
                listOf(
                    Argument(listOf("dex", "d", "get", "g")),
                    Argument(listOf("CP Name", "Alias"), ChoiceType.DESCRIPTION)
                ),
                "Gets the stats of a specific CP"
            ),
            CommandUsage(
                listOf(Argument(listOf("CP Name", "Alias"), ChoiceType.DESCRIPTION)),
                "Gets the stats of a specific CP"
            ),
            CommandUsage(
                listOf(
                    Argument(listOf("query", "q", "search", "s")),
                    Argument("hp"),
                    Argument("att"),
                    Argument("pr"),
                    Argument("wp"),
                    Argument("mag"),
                    Argument("mr")
                ),
                "Queries for cps that match the given stats. * means any stat"
            ),
            CommandUsage(
                listOf(Argument(listOf("year", "qyear", "qy")), Argument("year")),
                "Gets a Count of cps made in the given year"
            ),
            CommandUsage(
                listOf(
                    Argument(listOf("date", "qdate")),
                    Argument(listOf("month name", "month number"), ChoiceType.DESCRIPTION),
                    Argument("year")
                ), "Gets a list of all cps made in a specific month"
            )
        )

    override suspend fun LXVBot.cmd(mCE: MessageCreateEvent, args: List<String>) {
        val cpCol = hakiDb.getCollection<CustomPatreon>(CustomPatreon.DB_NAME)
        if (args.isEmpty()) {
            sendMessage(mCE.message.channel, "${cpCol.countDocuments()} total pets stored")
            return
        }
        when (val cmd = args.first().toLowerCase()) {
            "get", "dex", "d" -> {
                if (args.size == 2) {
                    val cp = getCP(args[1].toLowerCase(), cpCol)
                    if (cp == null) {
                        sendMessage(mCE.message.channel, "could not find cp ${args[1]}", 10_000)
                    } else {
                        sendMessage(mCE.message.channel) {
                            cp.toEmbed(this)
                        }
                    }
                } else if (args.size > 2) {
                    val cps = getCPs(args, cpCol)
                    sendMessage(mCE.message.channel, cps.joinToString("\n") {
                        it?.simpleString() ?: "CP Not Found"
                    })
                } else {
                    sendMessage(mCE.message.channel, "Invalid syntax! expecting `h!cp $cmd <cp names>`")
                }
            }

            "ga", "getall", "da", "reg" -> {
                if (args.size == 2) {
                    val cps = getCPsRegex(args[1].filter { it.isLetterOrDigit() || it == '_' }.toLowerCase(), cpCol)
                    val msg = "Found Cps\n${
                        cps.map(CustomPatreon::name).sorted().take(20).ifEmpty { listOf("No Matches") }
                            .joinToString("\n")
                    }${if (cps.size > 20) "\n(${cps.size - 20} more)" else ""}"

                    sendMessage(
                        mCE.message.channel,
                        if (msg.length <= 2000) msg else "Result Message too long (>2000 characters)"
                    )
                } else {
                    sendMessage(mCE.message.channel, "Invalid syntax! expecting `h!cp $cmd <partial cp name>`", 5_000)
                }
            }

            "search", "s", "query", "q" -> {
                val filters = mutableListOf<Bson>()
                suspend fun badFormat() {
                    sendMessage(
                        mCE.message.channel,
                        "Correct format is `h!cp $cmd <hp> <att> <pr> <wp> <mag> <mr>` where each stat is a number or * for any value"
                    )
                }
                if (args.size == 7) {
                    val props = arrayOf(
                        CustomPatreon::hp,
                        CustomPatreon::str,
                        CustomPatreon::pr,
                        CustomPatreon::wp,
                        CustomPatreon::mag,
                        CustomPatreon::mr
                    )
                    for (x in 0 until 6) {
                        if (args[x + 1] == "*") continue
                        if (args[x + 1].count { it == '-' } == 1) {
                            val (a, b) = args[x + 1].split('-')
                            if (a == "") {
                                val high = b.toIntOrNull()
                                if (high == null) {
                                    badFormat()
                                    return
                                }
                                filters.add(props[x] lte high)
                            } else if (b == "") {
                                val low = a.toIntOrNull()
                                if (low == null) {
                                    badFormat()
                                    return
                                }
                                filters.add(props[x] gte low)
                            } else {
                                val low = a.toIntOrNull()
                                val high = b.toIntOrNull()
                                if (low == null || high == null || high <= low) {
                                    badFormat()
                                    return
                                }
                                filters.add(props[x] gte low)
                                filters.add(props[x] lte high)
                            }
                        } else {
                            val num = args[x + 1].toIntOrNull()
                            if (num == null) {
                                badFormat()
                                return
                            } else {
                                filters.add(props[x] eq num)
                            }
                        }

                    }
                    val query = cpCol.find(and(filters)).sort(ascending(CustomPatreon::name)).toList()
                    if (query.none()) {
                        sendMessage(mCE.message.channel, "Found no cps with those stats")
                    } else {
                        val sbMessage = StringBuilder("Cps matching query: ${query.count()}\n")
                        for (cp in query) {
                            sbMessage.append("    ").append(cp.name).append("\n")
                            if (sbMessage.length > 2000) break
                        }

                        if (sbMessage.length > 2000) {
                            sendMessage(
                                mCE.message.channel,
                                "${query.count()} Cps. Result too long to fit in single message. I'll eventually add pagination"
                            )
                        } else {
                            sendMessage(mCE.message.channel, sbMessage.toString())
                        }
                    }
                } else {
                    badFormat()
                }
            }

            "y", "qy", "year", "qyear" -> {
                if (args.size == 2) {
                    val year = args.last().toIntOrNull()
                    sendMessage(
                        mCE.message.channel,
                        cpCol.find(CustomPatreon::creationInfo / CreationInfo::year eq year).toList().count().toString()
                    )
                } else {
                    sendMessage(mCE.message.channel, "Correct format is `h!cp $cmd <year>`", 5_000)
                }
            }

            "qdate", "date" -> {
                if (args.size == 3) {
                    val year = args[2].toIntOrNull()
                    val month = args[1].toIntOrNull() ?: CreationInfo.getMonthNum(args[1])
                    val cInfo = if (year == null || month == null) null else CreationInfo(
                        month,
                        if (year < 100) 2000 + year else year
                    )
                    val search =
                        cpCol.find(CustomPatreon::creationInfo eq cInfo).sort(ascending(CustomPatreon::name)).toList()
                            .map { it.name }
                    sendMessage(mCE.message.channel, "${search.joinToString("\n")}\n${search.count()}")
                } else {
                    sendMessage(mCE.message.channel, "Correct format is `h!cp $cmd <month> <year>`", 5_000)
                }
            }
            else -> {
                if (args.size == 1) {
                    val cp = getCP(cmd, cpCol)
                    if (cp == null) {
                        sendMessage(mCE.message.channel, "could not find cp $cmd", 10_000)
                    } else {
                        sendMessage(mCE.message.channel) {
                            cp.toEmbed(this)
                        }
                    }
                } else {
                    val cps = getCPs(args, cpCol)
                    sendMessage(mCE.message.channel, cps.joinToString("\n") {
                        it?.simpleString() ?: "CP Not Found"
                    })
                }
            }
        }
    }

    private suspend fun LXVBot.getCPsRegex(
        name: String,
        cpCol: CoroutineCollection<CustomPatreon> = hakiDb.getCollection(CustomPatreon.DB_NAME)
    ): List<CustomPatreon> {
        return cpCol.find(or(CustomPatreon::name regex name, CustomPatreon::aliases regex name)).toList()
    }

    private suspend fun LXVBot.getCPs(
        names: List<String>,
        cpCol: CoroutineCollection<CustomPatreon> = hakiDb.getCollection(CustomPatreon.DB_NAME)
    ): List<CustomPatreon?> {
        return List(names.size) { idx ->
            getCP(names[idx], cpCol)
        }
    }

    private suspend fun LXVBot.getCP(
        name: String,
        cpCol: CoroutineCollection<CustomPatreon> = hakiDb.getCollection(CustomPatreon.DB_NAME)
    ): CustomPatreon? {
        return cpCol.findOne(or(CustomPatreon::name eq name, CustomPatreon::aliases contains name))
    }
}
