package domain

import domain.models.ShopWorkersParam
import domain.models.WorkerState
import domain.repository.BotRepositoryWorkers
import domain.repository.ShopWorkersRepository
import kotlinx.coroutines.*
import utils.Logging
import java.util.*

class ShopWorkersManager(
    private val shopWorkersRepository: ShopWorkersRepository,
    private val botRepositoryWorkers: BotRepositoryWorkers,
) {
    private val tag = this::class.java.simpleName
    private val scopesList: MutableMap<UUID, Job> = mutableMapOf()
    private var shopWorkersList: MutableMap<UUID, ShopWorkersParam> = mutableMapOf()

    suspend fun start() {
        shopWorkersList = shopWorkersRepository.getAll()
        shopWorkersList.forEach {
            if (it.value.isActive) it.value.workerState = WorkerState.CREATE
        }
        processShopWorkers()

        while (true) {
            if (botRepositoryWorkers.changedWorkers.size != 0) {
                val changedWorkers = botRepositoryWorkers.changedWorkers.toList()
                changedWorkers.forEach{
                    shopWorkersList[it.workerId] = it
                    botRepositoryWorkers.changedWorkers.remove(it)
                }
                processShopWorkers()
            }
            delay(10000)
        }
    }

    private suspend fun processShopWorkers() {
        shopWorkersList.forEach { shopWorker ->
            when (shopWorker.value.workerState) {
                WorkerState.CREATE -> createShopWorker(shopWorker.value)
                WorkerState.DELETE -> deleteShopWorker(shopWorker.value)
                WorkerState.UPDATE -> updateShopWorker(shopWorker.value)
                else -> return@forEach
            }
        }
//        shopWorkersRepository.setAll(shopWorkersList)
    }

    private suspend fun updateShopWorker(shopWorkersParam: ShopWorkersParam) {
        if (scopesList.containsKey(shopWorkersParam.workerId)) {
            Logging.i(tag, "Update worker ${shopWorkersParam.workerId}, UPDATING")
            cancelShopWorker(shopWorkersParam.workerId)
        }
        createShopWorker(shopWorkersParam)
    }

    private suspend fun deleteShopWorker(shopWorkersParam: ShopWorkersParam) {
        Logging.i(tag, "Delete worker ${shopWorkersParam.workerId}, DELETING")
        cancelShopWorker(shopWorkersParam.workerId)
        shopWorkersList[shopWorkersParam.workerId]?.workerState = WorkerState.DELETED
    }

    private suspend fun cancelShopWorker(workerId: UUID) {
        Logging.i(tag, "Cancel scope worker ${workerId}...")
        scopesList[workerId]?.cancel()
        scopesList.remove(workerId)
    }

    private suspend fun createShopWorker(shopWorkersParam: ShopWorkersParam) {
        if ((!scopesList.containsKey(shopWorkersParam.workerId)) && (shopWorkersParam.isActive)) {
            Logging.i(tag, "New worker ${shopWorkersParam.workerId}, STARTING")
            val scope =
                CoroutineScope(Dispatchers.Default).launch(CoroutineName(shopWorkersParam.workerId.toString())) {

//                WorkerScope(bot = bot).processReport(reportsList[workerParam.workerId] ?: ReportWorkerParam())
                    while (true) {
                        val time = (3000..15000).random().toLong()
                        Logging.d(tag, "${shopWorkersParam.workerId} - ${shopWorkersParam.shop} - ${time}ms - ${this.coroutineContext}")
                        delay(time)
                    }

                }
            scope.start()
            scopesList[shopWorkersParam.workerId] = scope
            shopWorkersList[shopWorkersParam.workerId]?.workerState = WorkerState.WORK
        } else {
            Logging.e(
                tag, "Worker ${shopWorkersParam.workerId} is already running or not active and NOT STARTED!"
            )
        }
    }

    suspend fun changeInShopWorkers(shopWorkersParam: ShopWorkersParam) {
        shopWorkersList[shopWorkersParam.workerId] = shopWorkersParam
        processShopWorkers()
        if (shopWorkersParam.workerState == WorkerState.DELETE) shopWorkersList.remove(shopWorkersParam.workerId)
        shopWorkersRepository.setAll(shopWorkersList)
    }


}