import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.WriteConcern
import dev.kord.core.Kord
import dev.kord.core.enableEvent
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

val MONGO_CONNECT_URI =
    ConnectionString("mongodb+srv://${System.getenv("db-user")!!}:${System.getenv("db-pass")!!}@${System.getenv("db-address")!!}/${LXVBot.LXV_DB_NAME}")

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    val discordClient = Kord(System.getenv("lxv-token"))
    val mongoSettings =
        MongoClientSettings.builder().applyConnectionString(MONGO_CONNECT_URI).writeConcern(WriteConcern.MAJORITY)
            .retryWrites(true).build()
    val mongoClient = KMongo.createClient(mongoSettings).coroutine
    val bot = LXVBot(discordClient, mongoClient)
    bot.startup()
    discordClient.login {
        intents {
            +Intents.nonPrivileged
            +Intent.MessageContent
        }
        presence {
            watching("https://www.patreon.com/hakibot")
        }
    }
}