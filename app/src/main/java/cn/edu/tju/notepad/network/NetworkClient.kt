package cn.edu.tju.notepad.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetworkClient(private val context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("sync_settings", Context.MODE_PRIVATE)

    private val defaultServerUrl = "http://192.168.1.100:3000/" // 替换为您的MacBook IP

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val apiService: ApiService by lazy {
        val serverUrl = getServerUrl()

        Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun getServerUrl(): String {
        return sharedPrefs.getString("server_url", defaultServerUrl) ?: defaultServerUrl
    }

    fun setServerUrl(url: String) {
        sharedPrefs.edit().putString("server_url", url).apply()
    }
}