package domain.repository

import domain.models.ShopWorkersParam

interface BotRepositoryWorkers {
    var changedWorkers: MutableList<ShopWorkersParam>

}