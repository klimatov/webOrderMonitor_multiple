import bot.BotCore
import data.BotRepositoryDBImpl
import data.ShopWorkersRepositoryImpl
import domain.ShopWorkersManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import orderProcessing.data.SecurityData.POSTGRES_PASSWORD
import orderProcessing.data.SecurityData.POSTGRES_URL
import orderProcessing.data.SecurityData.POSTGRES_USER
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

val job = SupervisorJob()
private val botRepositoryDB = BotRepositoryDBImpl()
private val bot by lazy { BotCore(job = job, botRepositoryDB = botRepositoryDB) }
private val shopWorkersRepository = ShopWorkersRepositoryImpl()
private val shopWorkersManager by lazy(LazyThreadSafetyMode.NONE) { ShopWorkersManager(shopWorkersRepository = shopWorkersRepository) }

fun main() {
    Database.connect(
        url = POSTGRES_URL,
        driver = "org.postgresql.Driver",
        user = POSTGRES_USER,
        password = POSTGRES_PASSWORD
    )

    CoroutineScope(Dispatchers.Default + job).launch {
        bot.start()
        shopWorkersManager.start()
    }.start()

    while (true) {
    }
}