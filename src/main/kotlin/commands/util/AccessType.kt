package commands.util

enum class AccessType(val desc: String) {
    EVERYONE("Anyone"),
    MOD("Moderator"),
    ADMIN("Server Admin"),
    HAKI("Hakiobo"),
    OWNER("Server Owner"),
}