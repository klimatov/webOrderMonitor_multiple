package bot

import domain.models.ShopWorkersParam
import domain.repository.BotProcessingRepository
import domain.repository.BotWorkersRepository
import java.time.LocalDateTime

object BotWorkersRepositoryImpl : BotWorkersRepository {
    override var changedWorkers: MutableList<ShopWorkersParam> = mutableListOf()
    override val botProcessingRepositoryInstance: BotProcessingRepository
        get() = BotProcessingRepositoryImpl()

    override var remoteDbVersion: Int = 0
    override var lastErrorMessage: String = ""
    override var lastErrorCode: Int? = null
    override var appStartTime: LocalDateTime = LocalDateTime.now()
    override var shopWorkersList: MutableList<ShopWorkersParam> = mutableListOf()
}