package commands.util

data class CommandUsage(
    val args: List<Argument>,
    val description: String,
    val accessType: AccessType = AccessType.EVERYONE
)