package commands.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

fun LocalDate.startOfWeek(): LocalDate = this.minus(this.dayOfWeek.value % 7, DateTimeUnit.DAY)