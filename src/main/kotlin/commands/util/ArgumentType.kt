package commands.util

enum class ArgumentType(val prefix: String, val suffix: String) {
    FLAG("-", ""),
    FLAG_PARAM("-<", ">"),
    EXACT("", ""),
    PARAMETER("<", ">"),
    TEXT("{", "}"),
    CHOICE("[", "]"),
    VARARG("<", ">..."),
}