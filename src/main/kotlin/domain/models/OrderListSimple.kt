package domain.models

import restTS.models.WebOrderSimply

data class OrderListSimple(
    val errorCode: Int,
    val error: String,
    val listWebOrdersSimply: List<WebOrderSimply>
)
