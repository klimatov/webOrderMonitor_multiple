package restTS.models

import com.google.gson.annotations.SerializedName

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

data class SaveItems(
    @SerializedName("quantity") var quantity: Int? = null,
    @SerializedName("itemNo") var itemNo: Int? = null,
    @SerializedName("goodCode") var goodCode: String? = null,
    @SerializedName("incomplet") var incomplet: SaveIncomplet? = SaveIncomplet(),
    @SerializedName("shelf") var shelf: String? = null
)

data class SaveIncomplet(
    @SerializedName("reasonCode") var reasonCode: String? = null,
    @SerializedName("comment") var comment: String? = null
)

