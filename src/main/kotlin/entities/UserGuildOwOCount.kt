package entities

import dev.kord.core.event.message.MessageCreateEvent
import java.time.Instant
import java.time.Year
import java.time.ZoneId

data class UserGuildOwOCount(
    val _id: String,
    val user: Long,
    val guild: Long,
    var owoCount: Int = 0,
    var dailyCount: Int = 0,
    var yesterdayCount: Int = 0,
    var weeklyCount: Int = 0,
    var lastWeekCount: Int = 0,
    var monthlyCount: Int = 0,
    var lastMonthCount: Int = 0,
    var yearlyCount: Int = 0,
    var lastYearCount: Int = 0,
    var lastOWO: Long = 0,
) {

    fun normalize(mCE: MessageCreateEvent): Boolean {
        val curTime = mCE.message.id.timeStamp.atZone(ZoneId.of("PST", ZoneId.SHORT_IDS)).toLocalDate()
        val oldTime = Instant.ofEpochMilli(lastOWO).atZone(ZoneId.of("PST", ZoneId.SHORT_IDS)).toLocalDate()

        when (curTime.year - oldTime.year) {
            0 -> {
                when (curTime.monthValue - oldTime.monthValue) {
                    0 -> {
                    }
                    1 -> {
                        lastMonthCount = monthlyCount
                        monthlyCount = 0
                    }
                    else -> {
                        lastMonthCount = 0
                        monthlyCount = 0
                    }
                }
                when (curTime.dayOfYear - oldTime.dayOfYear) {
                    0 -> {
                        return false
                    }
                    1 -> {
                        yesterdayCount = dailyCount
                        dailyCount = 0
                    }
                    else -> {
                        yesterdayCount = 0
                        dailyCount = 0
                    }
                }

                //weekly stuff
                val difDoW = curTime.dayOfWeek.value % 7 - oldTime.dayOfWeek.value % 7
                val dif = curTime.dayOfYear - oldTime.dayOfYear
                if (dif == difDoW + 7) {
                    lastWeekCount = weeklyCount
                    weeklyCount = 0
                } else if (dif != difDoW) {
                    lastWeekCount = 0
                    weeklyCount = 0
                }
            }
            1 -> {
                lastYearCount = yearlyCount
                lastMonthCount = if (curTime.monthValue == 1 && oldTime.monthValue == 12) {
                    yesterdayCount = if (curTime.dayOfMonth == 1 && oldTime.dayOfMonth == 31) {
                        dailyCount
                    } else {
                        0
                    }
                    monthlyCount
                } else {
                    yesterdayCount = 0
                    0
                }
                yearlyCount = 0
                monthlyCount = 0
                dailyCount = 0

                //weekly stuff
                val day2 = curTime.dayOfYear + 365 + if (Year.isLeap(oldTime.year.toLong())) 1 else 0
                val difDoW = curTime.dayOfWeek.value % 7 - oldTime.dayOfWeek.value % 7
                val dif = day2 - oldTime.dayOfYear
                if (dif == difDoW + 7) {
                    lastWeekCount = weeklyCount
                    weeklyCount = 0
                } else if (dif != difDoW) {
                    lastWeekCount = 0
                    weeklyCount = 0
                }

            }
            else -> {
                dailyCount = 0
                yesterdayCount = 0
                weeklyCount = 0
                lastWeekCount = 0
                monthlyCount = 0
                lastMonthCount = 0
                yearlyCount = 0
                lastYearCount = 0
            }
        }
        return true
    }

    companion object {
        const val DB_NAME = "owo-count"
    }

}
