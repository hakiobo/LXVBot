package entities

import LXVBot
import LXVBot.Companion.toOwODate
import com.mongodb.client.model.UpdateOptions
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.User
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.serialization.Serializable
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.aggregate
import owo.OwOData
import java.time.Month
import kotlin.reflect.KProperty1

//@Serializable
//data class OldDailyStats(
//    val _id: UserGuildDate,
//    val count: Long = 0L
//) {
//    companion object {
//        const val DB_NAME = "battle-count"
//    }
//}
@Serializable
data class UserDailyStats(
    val _id: UserGuildDate,
    val battleCount: Long = 0L,
    val owoCount: Long = 0L,
) {
    companion object {
        val epoch = LocalDate(2000, Month.JANUARY, 1)
        const val DB_NAME = "owo-stats"
        const val OWO_CD_MS = 10_000
        const val OWO_OFFLINE_LIMIT = 300_000

        suspend fun LXVBot.countBattle(
            timestamp: Instant,
            userId: Snowflake,
            guildId: Snowflake,
            col: CoroutineCollection<UserDailyStats> = db.getCollection(DB_NAME),
        ) {
            col.updateOneById(
                UserGuildDate(
                    userId,
                    guildId,
                    timestamp.toOwODate().getDayId()
                ),
                inc(UserDailyStats::battleCount, 1L),
                UpdateOptions().upsert(true)
            )
        }

        private suspend fun LXVBot.countOwO(
            timestamp: Instant,
            userId: Snowflake,
            guildId: Snowflake,
            col: CoroutineCollection<UserDailyStats> = db.getCollection(DB_NAME),
        ) {
            col.updateOneById(
                UserGuildDate(
                    userId,
                    guildId,
                    timestamp.toOwODate().getDayId()
                ),
                combine(inc(UserDailyStats::owoCount, 1L)),
                UpdateOptions().upsert(true)
            )

        }


        suspend fun LXVBot.handleOwOSaid(
            timestamp: Instant,
            user: User,
            guildId: Snowflake,
            userCol: CoroutineCollection<LXVUser> = db.getCollection(LXVUser.DB_NAME),
        ) {
            val curTime = timestamp.toEpochMilliseconds()
            if (curTime - lastOwOMessage.view() >= OWO_OFFLINE_LIMIT) return
            val data = owoTimestamps.getOrDefault(user.id) {
                getUserFromDB(user.id, user, userCol).owo.lastOwO
            }
            if (data != null) {
                var toCount = false
                data.adjustData { lastTime ->
                    if (curTime - lastTime >= OWO_CD_MS) {
                        toCount = true
                        userCol.updateOneById(user.id, setValue(LXVUser::owo / OwOData::lastOwO, curTime))
                        curTime
                    } else {
                        lastTime
                        // edit here if we want penalty time
                    }
                }
                if (toCount) countOwO(timestamp, user.id, guildId)
            }
        }


        suspend fun getStatInRange(
            col: CoroutineCollection<UserDailyStats>,
            user: Snowflake,
            guild: Snowflake,
            startDate: LocalDate,
            endDate: LocalDate,
            stat: KProperty1<UserDailyStats, Long>,
        ): Long {
            return col.aggregate<CountContainer>(
                match(
                    UserDailyStats::_id / UserGuildDate::user eq user,
                    UserDailyStats::_id / UserGuildDate::guild eq guild,
                    UserDailyStats::_id / UserGuildDate::dayId lte endDate.getDayId(),
                    UserDailyStats::_id / UserGuildDate::dayId gte startDate.getDayId(),
                    stat gt 0L
                ),
                group(null, CountContainer::statCount sum stat),
            ).first()?.statCount ?: 0L
        }

        @Serializable
        data class CountContainer(val statCount: Long)

        @Serializable
        data class TopContainer(val sum: Long, val _id: Snowflake)

        suspend fun getLeaderboardStatInRange(
            col: CoroutineCollection<UserDailyStats>,
            guild: Snowflake,
            startDate: LocalDate,
            endDate: LocalDate,
            toSkip: Int,
            toKeep: Int,
            stat: KProperty1<UserDailyStats, Long>,
        ): List<TopContainer> {
            return col.aggregate<TopContainer>(
                match(
                    UserDailyStats::_id / UserGuildDate::guild eq guild,
                    UserDailyStats::_id / UserGuildDate::dayId lte endDate.getDayId(),
                    UserDailyStats::_id / UserGuildDate::dayId gte startDate.getDayId(),
                ),
                group(
                    UserDailyStats::_id / UserGuildDate::user,
                    TopContainer::sum sum stat
                ),
                sort(descending(TopContainer::sum, TopContainer::_id)),
                skip(toSkip),
                limit(toKeep),
            ).toList()
        }

        fun LocalDate.getDayId(): Int {
            return epoch.daysUntil(this)
        }


        val owoCommands = hashSetOf(
            // battle folder
            "ab", "acceptbattle",
            "battle", "b", "fight",
            "battlesetting", "bs", "battlesettings",
            "crate", "weaponcrate", "wc",
            "db", "declinebattle",
            "pets", "pet",
            "rename",
            "team", "squad", "tm",
            "teams", "setteam", "squads", "useteams",
            "weapon", "w", "weapons", "wep",
            "weaponshard", "ws", "weaponshards", "dismantle",
            // economy folder
            "claim", "reward", "compensation",
            "cowoncy", "money", "currency", "cash", "credit", "balance",
            "daily",
            "give", "send",
            "quest", "q",
            // emotes folder
            "gif", "pic",
            // https://github.com/ChristopherBThai/Discord-OwO-Bot/blob/master/src/data/emotes.json
            // self emotes
            "blush",
            "cry",
            "dance",
            "lewd",
            "pout",
            "shrug",
            "sleepy",
            "smile",
            "smug",
            "thumbsup",
            "wag",
            "thinking",
            "triggered",
            "teehee",
            "deredere",
            "thonking",
            "scoff",
            "happy",
            "thumbs",
            "grin",
            // user emotes
            "cuddle",
            "hug",
            "kiss",
            "lick",
            "nom",
            "pat",
            "poke",
            "slap",
            "stare",
            "highfive",
            "bite",
            "greet",
            "punch",
            "handholding",
            "tickle",
            "kill",
            "hold",
            "pats",
            "wave",
            "boop",
            "snuggle",
            "fuck",
            "sex",
            // gamble folder
            "blackjack", "bj", "21",
            "coinflip", "cf", "coin", "flip",
            "drop", "pickup",
            "lottery", "bet", "lotto",
            "slots", "slot", "s",
            // memgen folder
            "communism", "communismcat",
            "distractedbf", "distracted",
            "drake",
            "eject", "amongus",
            "emergency", "emergencymeeting",
            "headpat",
            "isthisa",
            "slapcar", "slaproof",
            "spongebobchicken", "schicken",
            "tradeoffer",
            "waddle",
            // patreon folder
            "02kiss",
            "alastor",
            "angel", "agl",
            "army",
            "babyyoda",
            "boba",
            "bonk", "bomk",
            "bully",
            "bunny",
            "butterfly", "btf",
            "cake",
            "candycane",
            "catto", "shifu", "ufo",
            "chicken", "jester",
            "choose", "pick", "decide",
            "coffee", "java",
            "compliment", "bnice",
            "crown",
            "cupachicake", "cpc",
            "death",
            "destiny", "dtn",
            "devil", "dvl",
            "roll", "d20",
            "dish",
            "donut",
            "dragon", "dgn",
            "duwasvivu",
            "egg",
            "fate",
            "frogegg",
            "gauntlet",
            "genie",
            "goldenegg",
            "grim",
            "icecream",
            "king",
            "latte",
            "life",
            "lollipop",
            "love",
            "magic",
            "meshi",
            "milk",
            "mochi",
            "moon",
            "nier",
            "obw",
            "pika", "pikapika",
            "piku",
            "pizza",
            "poutine",
            "puppy", "pup",
            "queen",
            "rain", "rainbow", "raindrop",
            "rose", "bouquet",
            "rum",
            "run",
            "sakura",
            "sammy",
            "sharingan",
            "slime",
            "snake",
            "snowball",
            "bell", "strengthtest",
            "sun",
            "sunflower",
            "taco",
            "tarot",
            "tequila",
            "truthordare", "td",
            "turnip",
            "water",
            "wolf",
            "yinyang", "yy",
            "zodiackey", "zk",
            // https://github.com/ChristopherBThai/Discord-OwO-Bot/blob/335ade88e1f452367d9cbf4cc3d2eff243e8708b/src/commands/commandList/patreon/utils/collectibles.json
            "fear", "nommy",
            "bear",
            "ginseng",
            "grizzly",
            "smokeheart", "heart",
            "panda",
            "sonic",
            "teddy",
            "carlspider",
            "des",
            "flame", "flm",
            "penguin", "pgn",
            "star",
            "music", "msc",
            "corgi", "doggo",
            "saturn",
            "spider",
            "doll",
            "martini",
            // ranking folder
            "my", "me", "guild",
            "top", "rank", "ranking",
            "buy",
            "describe", "desc",
            "equip", "use",
            "inventory", "inv",
            "shop", "market",
            "trade", "tr", "gift",
            // social folder
            "acceptmarriage", "am",
            "cookie", "rep",
            "declinemarriage", "dm",
            "define",
            "discordplays", "twitchplays", "emulator",
            "divorce",
            "eightball", "8b", "ask", "8ball",
            "emoji", "enlarge", "jumbo",
            "level", "lvl", "levels", "xp",
            "propose", "marry", "marriage", "wife", "husband",
            "owo", "owoify", "ify",
            "pray", "curse",
            "profile",
            "ship", "combine",
            "translate", "listlang", "tl",
            "wallpaper", "wp", "wallpapers", "background", "backgrounds",
            // utils folder
            "announce", "changelog", "announcement", "announcements",
            "avatar", "user",
            "censor",
            "checklist", "task", "tasks", "cl",
            "color", "randcolor", "colour", "randcolour",
            "covid", "cv", "covid19", "coronavirus",
            "disable",
            "distorted", "dt",
            "enable",
            "guildlink",
            "help",
            "invite", "link",
            "math", "calc", "calculate",
            "patreon", "donate",
            "ping", "pong",
            "prefix",
            "rule", "rules",
            "shards", "shard",
            "stats", "stat", "info",
            "suggest",
            "survey",
            "uncensor",
            "vote",
            // zoo folder
            "autohunt", "huntbot", "hb",
            "hunt", "h", "catch",
            "lootbox", "lb",
            "owodex", "od", "dex", "d",
            "sacrifice", "essence", "butcher", "sac", "sc",
            "sell",
            "upgrade", "upg",
            "zoo", "z",
        )
    }
}
