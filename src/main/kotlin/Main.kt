import bot.BotCore
import bot.BotWorkersRepositoryImpl
import data.BotRepositoryDBImpl
import data.ServerTSRepositoryImpl
import data.WorkersDBRepositoryImpl
import domain.ShopWorkersManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import orderProcessing.data.SecurityData.POSTGRES_PASSWORD
import orderProcessing.data.SecurityData.POSTGRES_URL
import orderProcessing.data.SecurityData.POSTGRES_USER
import org.jetbrains.exposed.sql.Database

val job = SupervisorJob()
private val botRepositoryDB = BotRepositoryDBImpl()
val botCore by lazy { BotCore(job = job, botRepositoryDB = botRepositoryDB) }
private val workersRepositoryDB = WorkersDBRepositoryImpl()
private val botWorkersRepository = BotWorkersRepositoryImpl
private val serverTS = ServerTSRepositoryImpl()
private val shopWorkersManager by lazy(LazyThreadSafetyMode.NONE) {
    ShopWorkersManager(
        workersDBRepository = workersRepositoryDB,
        botWorkersRepository = botWorkersRepository,
        serverTSRepository = serverTS,
        shopParametersDBRepository = workersRepositoryDB
    )
}

fun main() {
    Database.connect(
        url = POSTGRES_URL,
        driver = "org.postgresql.Driver",
        user = POSTGRES_USER,
        password = POSTGRES_PASSWORD
    )

    CoroutineScope(Dispatchers.Default + job).launch {
        botCore.start()
        shopWorkersManager.start()
    }.start()

    while (true) {
    }
}