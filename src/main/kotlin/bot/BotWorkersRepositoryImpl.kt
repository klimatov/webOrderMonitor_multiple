package bot

import domain.models.ShopWorkersParam
import domain.repository.BotWorkersRepository

object BotWorkersRepositoryImpl : BotWorkersRepository {
    override var changedWorkers: MutableList<ShopWorkersParam> = mutableListOf()
}