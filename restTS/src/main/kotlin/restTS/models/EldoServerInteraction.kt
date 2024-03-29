package restTS.models

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

data class ReasonsForIncompletnessResult(
    val result: Result,
    val reasonsList: List<ShortageReasonDto>
)

data class ShelfsResult(
    val result: Result,
    val shelfsList: List<ShelfItem>
)

data class PrintersListResult(
    val result: Result,
    val printersList: List<PcNameList>
)

data class SaveWebOrderResult(
    val result: Result,
    val saveWebOrder: SaveWebOrderRes
)
data class PrintResult(
    val result: Result
)