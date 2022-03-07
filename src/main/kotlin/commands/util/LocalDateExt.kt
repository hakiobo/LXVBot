package commands.util

import kotlinx.datetime.*

fun LocalDate.startOfWeek(weeksBack: Int = 0): LocalDate =
    this.minus(7 * weeksBack + (this.dayOfWeek.value % 7), DateTimeUnit.DAY)

fun LocalDate.endOfWeek(weeksBack: Int = 0): LocalDate =
    this.plus(-7 * weeksBack + 6 - (this.dayOfWeek.value % 7), DateTimeUnit.DAY)

fun LocalDate.startOfMonth(monthsBack: Int = 0): LocalDate =
    this.minus(monthsBack, DateTimeUnit.MONTH).toJavaLocalDate().withDayOfMonth(1).toKotlinLocalDate()

fun LocalDate.endOfMonth(monthsBack: Int = 0): LocalDate =
    this.plus(DateTimeUnit.MONTH).startOfMonth(monthsBack).minus(DateTimeUnit.DAY)

fun LocalDate.startOfYear(yearsBack: Int = 0): LocalDate =
    this.minus(yearsBack, DateTimeUnit.YEAR).toJavaLocalDate().withDayOfYear(1).toKotlinLocalDate()

fun LocalDate.endOfYear(yearsBack: Int = 0): LocalDate =
    this.plus(DateTimeUnit.YEAR).startOfYear(yearsBack).minus(DateTimeUnit.DAY)