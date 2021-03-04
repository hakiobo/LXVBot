import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.WriteConcern
import dev.kord.core.Kord
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

val MONGO_CONNECT_URI =
    ConnectionString("mongodb+srv://${System.getenv("db-user")}:${System.getenv("db-pass")}@${System.getenv("db-address")}/LXV")

suspend fun main() {
    val discordClient = Kord(System.getenv("lxv-token"))
    val mongoSettings =
        MongoClientSettings.builder().applyConnectionString(MONGO_CONNECT_URI).writeConcern(WriteConcern.MAJORITY)
            .retryWrites(true).build()
    val mongoClient = KMongo.createClient(mongoSettings).coroutine
    val bot = LXVBot(discordClient, mongoClient)
    bot.startup()
    discordClient.login()
}