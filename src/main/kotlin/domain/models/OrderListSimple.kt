package domain.models

import data.restTS.data.WebOrderSimply

data class OrderListSimple(
    val errorCode: Int,
    val error: String,
    val listWebOrdersSimply: List<WebOrderSimply>
)
