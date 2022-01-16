package taco

enum class TacoPatreonLevel(val aliases: List<String>, val tipReduction: Long, val workReduction: Long) {
    NONE(listOf("none", "0"), 0L, 0L),
    SOUS(listOf("sous", "1"), 0L, 2 * 60_000L),
    HEAD(listOf("head", "2"), 60_000L, 2 * 60_000L),
    GORDON_RAMSEY(listOf("ramsey", "gordon", "3"), 2 * 60_000L, 3 * 60_000L)
    ;

    fun getFormattedName(): String {
        return name.split("_").joinToString(" ") { name -> name.lowercase().replaceFirstChar { c -> c.uppercase() } }
    }

    val id: String
        get() = aliases.first()

    companion object {
        fun findPatreonLevel(arg: String): TacoPatreonLevel {
            for (level in values()) {
                if (arg in level.aliases) {
                    return level
                }
            }
            return NONE
        }
    }
}
