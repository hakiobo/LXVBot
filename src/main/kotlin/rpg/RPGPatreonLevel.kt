package rpg


enum class RPGPatreonLevel(val aliases: List<String>, val multiplier: Double) {
    NONE(listOf("none", "0%", "0", "0.0"), 1.0),
    REGULAR(listOf("regular", "donator", "basic", "10%", "10", "0.9", "1"), 0.9),
    EPIC(listOf("epic", "20%", "20", "0.8", "2"), 0.8),
    SUPER(listOf("super", "35%", "35", "0.65", "3"), 0.65),
    ;

    companion object {
        fun findPatreonLevel(p: String): RPGPatreonLevel {
            for (level in values()) {
                if (p in level.aliases) {
                    return level
                }
            }
            return NONE
        }
    }
}
