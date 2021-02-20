import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.WriteConcern
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import org.litote.kmongo.KMongo

val MONGO_CONNECT_URI =
    ConnectionString("mongodb+srv://${System.getenv("db-user")}:${System.getenv("db-pass")}@${System.getenv("db-address")}/LXV")

suspend fun main() {
    val discordClient = Kord(System.getenv("lxv-token"))
    val mongoSettings =
        MongoClientSettings.builder().applyConnectionString(MONGO_CONNECT_URI).writeConcern(WriteConcern.MAJORITY)
            .retryWrites(true).build()
    val mongoClient = KMongo.createClient(mongoSettings)
    val db = mongoClient.getDatabase("lxv")
    val bot = LXVBot(discordClient, db)
    bot.startup()
    discordClient.login()
}