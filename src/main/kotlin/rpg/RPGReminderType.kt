package rpg

enum class RPGReminderType(
    val aliases: List<String>,
    val cooldownMS: Long,
    val patreonAffected: Boolean,
    val eventAffected: Boolean,
    val emoji: String = ""
) {
    HUNT(listOf("hunt"), 60_000, true, true, "<a:hunt:820275884245385236>") {
        override fun getResponseName(args: List<String>): String {
            return if (args.drop(1).firstOrNull()?.toLowerCase() in listOf("t", "together")
                || args.drop(2).firstOrNull()?.toLowerCase() in listOf("t", "together")
            ) {
                "Hunt Together"
            } else {
                getFormattedName()
            }
        }
    },
    DAILY(listOf("daily"), (23 * 60 + 50) * 60_000, false, true, "<a:daily:820280260163534868>"),
    WEEKLY(listOf("weekly"), ((6 * 24 + 23) * 60 + 50) * 60_000, false, true, "<a:daily:820280260163534868>"),
    ADVENTURE(listOf("adventure", "adv"), 3600_000, true, true, "<a:adv:820275948053463050>"),
    PET_ADVENTURE(listOf("pet", "pets"), 4 * 3600_000, false, false, "<a:pets:820275963220197376>") {
        private val petAdvTypes = listOf("find", "learn", "drill")
        override fun validate(args: List<String>): Boolean {
            return args.size >= 4 && args[1].toLowerCase() in ADVENTURE.aliases && args[2].toLowerCase() in petAdvTypes
        }
    },
    TRAINING(listOf("training", "tr", "ultraining", "ultr"), 15 * 60_000 + 2_000, true, true, ":stadium:") {
        override fun validate(args: List<String>): Boolean {
            return args.size <= 1 || args.first() !in listOf("ultraining", "ultr") || args[1] !in listOf(
                "p",
                "progress"
            )
        }

        override fun getResponseName(args: List<String>): String {
            return if (args.first().toLowerCase().startsWith("u", ignoreCase = true)) "Ultraining" else "Training"
        }
    },
    BUY_LOOTBOX(listOf("buy", "lootbox", "lb"), 3 * 3600_000, false, true, "<a:lootbox:820275922136596501>") {
        private val lootboxTypes = listOf("c", "common", "u", "uncommon", "r", "rare", "ep", "epic", "ed", "edgy")
        override fun validate(args: List<String>): Boolean {
            return args.size >= 3 &&
                    args[0].toLowerCase() == "buy" &&
                    args[1].toLowerCase() in lootboxTypes &&
                    args[2].toLowerCase() in aliases.drop(1)
        }
    },
    QUEST(listOf("quest", "epic"), 6 * 3600_000, true, true) {
        override fun validate(args: List<String>): Boolean {
            return if (args.first().toLowerCase() == "quest") {
                true
            } else {
                args.size >= 2 && args.first().toLowerCase() == "epic" && args[1].toLowerCase() == "quest"
            }
        }
    },
    DUEL(listOf("duel"), 2 * 3600_000, false, true, "<a:duel:820364624809558068>"),
    WORK(
        listOf(
            "work",
            "chop",
            "axe",
            "bowsaw",
            "chainsaw",
            "fish",
            "net",
            "boat",
            "bigboat",
            "pickup",
            "ladder",
            "tractor",
            "greenhouse",
            "mine",
            "pickaxe",
            "drill",
            "dynamite"
        ),
        300_000, true, true, "<a:work:820275934619500564>"
    ) {
        override fun getResponseName(args: List<String>): String {
            return args.first().capitalize()
        }
    },
    HORSE(listOf("horse"), 24 * 3600_000, true, true, "<a:horses:820368968635121684>") {
        override fun getResponseName(args: List<String>): String {
            return "Horse Breeding/Race"
        }

        override fun validate(args: List<String>): Boolean {
            return if (args.size < 2) {
                false
            } else {
                if (args[1].toLowerCase() == "race") {
                    true
                } else if (args.size >= 3) {
                    args[2].toLowerCase() in listOf(
                        "breed",
                        "breeding"
                    ) && args[3].toLongOrNull() == null && LXVBot.getUserIdFromString(args[3]) != null
                } else {
                    false
                }
            }
        }

    },
    ARENA(listOf("arena", "big"), 24 * 3600_000, true, true, "<a:arena:820366824562360392>") {
        override fun validate(args: List<String>): Boolean {
            return if (args.first().toLowerCase() == "arena") {
                true
            } else {
                args.size >= 3
                        && args.first().toLowerCase() == "big"
                        && args[1].toLowerCase() == "arena"
                        && args[2].toLowerCase() == "join"
            }
        }
    },
    MINIBOSS(listOf("miniboss", "not", "dungeon"), 12 * 3600_000, true, true) {
        override fun validate(args: List<String>): Boolean {
            return if (args.first().toLowerCase() == "miniboss" || args.first().toLowerCase() == "dungeon") {
                true
            } else {
                args.size >= 4
                        && args.first().toLowerCase() == "not"
                        && args[1].toLowerCase() == "so"
                        && args[2].toLowerCase() == "mini"
                        && args[3].toLowerCase() == "boss"
            }
        }
    },
    ;

    fun getFormattedName(): String {
        return name.split("_").map { it.toLowerCase().capitalize() }.joinToString(" ")
    }

    fun getReminderMessage(args: List<String>): String {
        return "Time for __**RPG ${getResponseName(args).toUpperCase()}**__ $emoji"
    }

    protected open fun getResponseName(args: List<String>): String {
        return getFormattedName()
    }

    protected open fun validate(args: List<String>): Boolean {
        return true
    }

    val id: String
        get() = aliases.first()

    companion object {
        const val EVENT_BONUS = 1.0

        fun findReminder(name: String): RPGReminderType? {
            val cmd = name.toLowerCase()
            for (reminder in values()) {
                if (cmd in reminder.aliases) {
                    return reminder
                }
            }
            return null
        }

        fun findValidReminder(args: List<String>): RPGReminderType? {
            val a = args.map { it.toLowerCase() }
            val cmd = a.firstOrNull() ?: return null
            for (reminder in values()) {
                if (cmd in reminder.aliases) {
                    if (reminder.validate(args)) {
                        return reminder
                    }
                }
            }
            return null
        }
    }
}