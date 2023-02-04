package data.restTS.net
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.http.*


interface EldoAPI {

    @POST("/login")
    @Headers(*["Cache-Control: no-cache", "Content-Type: application/json"])
    fun login(
        @Header("werk") str: String?,
        @Header("ver") ver: String?,
        @Header("dbvers") dbvers: String?,
        @Body hashMap: HashMap<String, String>
    ): Call<JsonElement?>?

    @GET("version-package-database/")
    fun getDBVersion(@Header("werk") str: String?): Call<JsonElement?>?

    @GET("/web-order/count")
    fun getWebOrderCount(
        @Header("werk") str: String?,
        @Header("ver") str6: String?,
        @Header("dbvers") dbvers: String?,
        @Header("Authorization") str2: String?,
        @Query("dateFrom") str3: String?,
        @Query("dateTo") str4: String?,
        @Query("type") str5: String?
    ): Call<JsonElement?>?

    @GET("/local-remains/{id}") // остаток НН (id) на складе
    fun localRemains(
        @Header("werk") str: String?,
        @Header("ver") str6: String?,
        @Header("dbvers") dbvers: String?,
        @Header("Authorization") str2: String?,
        @Path("id") str3: String?
    ): Call<JsonElement?>?

    @GET("/local-storage") // список складов
    fun localStorage(
        @Header("werk") str: String?,
        @Header("ver") str6: String?,
        @Header("dbvers") dbvers: String?,
        @Header("Authorization") str2: String?
    ): Call<JsonElement?>?

    @POST("/main-remains") // остаток списка НН на складах
    fun mainRemains(
        @Header("werk") str: String?,
        @Header("ver") str6: String?,
        @Header("dbvers") dbvers: String?,
        @Header("Authorization") str2: String?,
        @Body hashMap: HashMap<String, JsonArray?>
    ): Call<JsonElement?>?

    @POST("/web-order/list-simple") // список активных веб-заявок (номер заявки и дата создания)
    fun getWebOrderListSimple(
        @Header("werk") str: String?,
        @Header("ver") str6: String?,
        @Header("dbvers") dbvers: String?,
        @Header("Authorization") str2: String?,
        @Body hashMap: java.util.HashMap<Any?, Any?>
    ): Call<JsonElement?>?

    @POST("/web-order/list") // список активных веб-заявок (подробно)
    fun getWebOrderList(
        @Header("werk") str: String?,
        @Header("ver") str6: String?,
        @Header("dbvers") dbvers: String?,
        @Header("gmt") gmt: String?,
        @Header("Authorization") str2: String?,
        @Body hashMap: HashMap<Any?, Any?>
    ): Call<JsonElement?>?

    @GET("/web-order/detail")
    fun getWebOrderDetail(
        @Header("werk") str: String?,
        @Header("ver") str6: String?,
        @Header("dbvers") dbvers: String?,
        @Header("Authorization") str2: String?,
        @Query("orderId") str3: String?,
        @Query("type") str4: String?
    ): Call<JsonElement?>?

}