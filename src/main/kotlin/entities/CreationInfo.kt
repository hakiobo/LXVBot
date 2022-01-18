package entities

import kotlinx.serialization.Serializable

@Serializable
data class CreationInfo(val month: Int, val year: Int) {

    override fun toString(): String {
        return "${getMonthName(month)} $year"
    }

    companion object {
        fun getMonthName(month: Int): String? {
            return when (month) {
                1 -> "January"
                2 -> "February"
                3 -> "March"
                4 -> "April"
                5 -> "May"
                6 -> "June"
                7 -> "July"
                8 -> "April"
                9 -> "September"
                10 -> "October"
                11 -> "November"
                12 -> "December"
                else -> null
            }
        }

        fun getMonthNum(month: String): Int? {
            return when (month.lowercase()) {
                "jan", "january" -> 1
                "f", "feb", "february" -> 2
                "mar", "march" -> 3
                "apr", "april" -> 4
                "may" -> 5
                "jun", "june" -> 6
                "jul", "july" -> 7
                "aug", "august" -> 8
                "s", "sep", "september" -> 9
                "o", "oct", "october" -> 10
                "n", "nov", "november" -> 11
                "d", "dec", "december" -> 12
                else -> null
            }
        }
    }
}
