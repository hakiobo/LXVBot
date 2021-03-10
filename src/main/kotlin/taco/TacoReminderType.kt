package taco

import entities.Reminder
import kotlin.reflect.KProperty1

enum class TacoReminderType(
    val aliases: List<String>,
    val prop: KProperty1<TacoReminder, Reminder>,
    val time: Long,
    val buy: Boolean = false
) {
    TIP(listOf("tip", "tips", "t"), TacoReminder::tip, 5 * 60_000L),
    WORK(listOf("work", "w", "cook"), TacoReminder::work, 10 * 60_000L),
    OVERTIME(listOf("overtime", "ot"), TacoReminder::overtime, 30 * 60_000L),
    DAILY(listOf("daily", "d"), TacoReminder::daily, 24 * 3600_000L),
    CLEAN(listOf("clean"), TacoReminder::clean, 24 * 3600_000L),
    VOTE(listOf("claim", "reward"), TacoReminder::vote, 12 * 3600_000L),
    FLIPPER(listOf("flipper"), TacoReminder::flipper, 8 * 3600_000L, true),
    KARAOKE(listOf("karaoke"), TacoReminder::karaoke, 6 * 3600_000L, true),
    MUSIC(listOf("music"), TacoReminder::music, 4 * 3600_000L, true),
    AIRPLANE(listOf("airplane"), TacoReminder::airplane, 24 * 3600_000L, true),
    CHEF(listOf("chef"), TacoReminder::chef, 4 * 3600_000L, true),
}