package data.restTS.models

data class Result(
    val success: Boolean,
    val errorMessage: String?,
    val errorCode: Int?
)

data class LoginResult(
    val result: Result,
    val userInfo: UserInfo?,
)

data class WebOrderResult(
    val result: Result,
    val webOrder: WebOrder
)
