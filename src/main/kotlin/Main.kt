import bot.BotCore
import bot.BotWorkersRepositoryImpl
import data.BotTSRepositoryImpl
import data.DataToBotRepositoryImpl
import data.DataToDomainRepositoryImpl
import domain.ShopWorkersManager
import kotlinx.coroutines.*
import SecurityData.POSTGRES_PASSWORD
import SecurityData.POSTGRES_URL
import SecurityData.POSTGRES_USER
import org.jetbrains.exposed.sql.Database
import utils.Logging

val job = SupervisorJob()
val botCore by lazy {
    BotCore(
        job = job,
        botRepositoryDB = DataToBotRepositoryImpl(),
        botTSRepository = BotTSRepositoryImpl()
    )
}
private val dataToDomainRepositoryImpl = DataToDomainRepositoryImpl()
private val botWorkersRepository = BotWorkersRepositoryImpl
private val shopWorkersManager by lazy(LazyThreadSafetyMode.NONE) {
    ShopWorkersManager(
        workersDBRepository = dataToDomainRepositoryImpl,
        botWorkersRepository = botWorkersRepository,
        shopParametersDBRepository = dataToDomainRepositoryImpl,
        serverTSFactoryRepository = dataToDomainRepositoryImpl
    )
}

suspend fun main() {
    Database.connect(
        url = POSTGRES_URL,
        driver = "org.postgresql.Driver",
        user = POSTGRES_USER,
        password = POSTGRES_PASSWORD
    )

    CoroutineScope(Dispatchers.Default + job).launch {
        botCore.start()
        shopWorkersManager.start()
    }.join()

    while (true) {
        Logging.d("main", "Tick in main scope!")
        delay(60000L)
    }
}