package commands.util

enum class ChoiceType(val argType: ArgumentType) {
    EXACT(ArgumentType.CHOICE), DESCRIPTION(ArgumentType.PARAMETER)
}