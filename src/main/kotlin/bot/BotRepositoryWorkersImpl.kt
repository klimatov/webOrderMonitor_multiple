package bot

import domain.models.ShopWorkersParam
import domain.repository.BotRepositoryWorkers

object BotRepositoryWorkersImpl : BotRepositoryWorkers {
    override var changedWorkers: MutableList<ShopWorkersParam> = mutableListOf()
}