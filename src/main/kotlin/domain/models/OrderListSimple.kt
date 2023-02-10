package domain.models

import data.restTS.models.WebOrderSimply

data class OrderListSimple(
    val errorCode: Int,
    val error: String,
    val listWebOrdersSimply: List<WebOrderSimply>
)
