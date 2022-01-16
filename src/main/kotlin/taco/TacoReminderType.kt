package taco

import entities.Reminder
import kotlin.reflect.KProperty1

enum class TacoReminderType(
    val aliases: List<String>,
    val prop: KProperty1<TacoReminder, Reminder>,
    private val time: Long,
    val buy: Boolean = false
) {
    TIPS(listOf("tip", "tips", "t"), TacoReminder::tip, 5 * 60_000L) {
        override fun getCooldown(settings: TacoPatreonLevel): Long {
            return super.getCooldown(settings) - settings.tipReduction
        }
    },
    WORK(listOf("work", "w", "cook"), TacoReminder::work, 10 * 60_000L) {
        override fun getCooldown(settings: TacoPatreonLevel): Long {
            return super.getCooldown(settings) - settings.workReduction
        }
    },
    OVERTIME(listOf("overtime", "ot"), TacoReminder::overtime, 30 * 60_000L),
    DAILY(listOf("daily", "d"), TacoReminder::daily, 24 * 3600_000L),
    CLEAN(listOf("clean"), TacoReminder::clean, 24 * 3600_000L),
    VOTE(listOf("claim", "reward"), TacoReminder::vote, 12 * 3600_000L),
    FLIPPER(listOf("flipper"), TacoReminder::flipper, 8 * 3600_000L, true),
    KARAOKE(listOf("karaoke"), TacoReminder::karaoke, 6 * 3600_000L, true),
    MUSIC(listOf("music"), TacoReminder::music, 4 * 3600_000L, true),
    AIRPLANE(listOf("airplane"), TacoReminder::airplane, 24 * 3600_000L, true),
    CHEF(listOf("chef"), TacoReminder::chef, 4 * 3600_000L, true),
    CHAIRS(listOf("chairs"), TacoReminder::chairs, 8 * 3600_000L, true),
    SAIL(listOf("sail"), TacoReminder::sail, 6 * 3600_000L, true),
    CONCERT(listOf("concert"), TacoReminder::concert, 4 * 3600_000L, true),
    TOURS(listOf("tours"), TacoReminder::tours, 24 * 3600_000L, true),
    HAMMOCK(listOf("hammock"), TacoReminder::hammock, 4 * 3600_000L, true),
    ;

    open fun getCooldown(settings: TacoPatreonLevel): Long {
        return time
    }

    private val formattedName
        get() = "${name.lowercase()}${if (buy) " boost" else ""}"


    fun getReminderMessage(): String {
        return "Your TacoShack `$formattedName` is Ready"
    }

    companion object {
        fun findTacoReminder(nameToFind: String): TacoReminderType? {
            val name = nameToFind.lowercase()
            for (reminder in values()) {
                if (name in reminder.aliases || name == reminder.name.lowercase()) {
                    return reminder
                }
            }
            return null
        }
    }
}