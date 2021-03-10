package commands.util

data class Argument(val text: String, val argType: ArgumentType = ArgumentType.PARAMETER) {
    constructor(args: List<String>, choiceType: ChoiceType = ChoiceType.EXACT) : this(
        args.joinToString("|"),
        choiceType.argType
    )
}
