package restTS.net

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private const val baseUrl = "https://tsapp.eldorado.ru/"

    private val httpLogger = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15L, TimeUnit.SECONDS)
        .readTimeout(15L, TimeUnit.SECONDS)
        .writeTimeout(15L, TimeUnit.SECONDS)
        .addInterceptor(httpLogger)
        .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val eldoApi: EldoAPI = retrofit.create(EldoAPI::class.java)

}