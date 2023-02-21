package domain.repository

import domain.models.ShopWorkersParam
import java.time.LocalDateTime

interface BotWorkersRepository {
    var changedWorkers: MutableList<ShopWorkersParam>
    val botProcessingRepositoryInstance: BotProcessingRepository
    var remoteDbVersion: Int
    var lastErrorMessage: String
    var lastErrorCode: Int?
    var appStartTime: LocalDateTime
    var shopWorkersList: MutableList<ShopWorkersParam>
}