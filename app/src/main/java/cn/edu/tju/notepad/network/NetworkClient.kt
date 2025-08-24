package cn.edu.tju.notepad.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetworkClient(private val context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("sync_settings", Context.MODE_PRIVATE)

    private val defaultServerUrl = "http://192.168.1.101:3000/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        // 添加自定义拦截器来记录请求详情
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d("NetworkClient", "🌐 HTTP请求: ${request.method} ${request.url}")
            Log.d("NetworkClient", "请求头: ${request.headers}")

            val startTime = System.currentTimeMillis()
            val response = try {
                chain.proceed(request)
            } catch (e: Exception) {
                Log.e("NetworkClient", "🚨 HTTP请求异常: ${request.url}", e)
                throw e
            }
            val duration = System.currentTimeMillis() - startTime

            Log.d("NetworkClient", "📡 HTTP响应: ${response.code} ${response.message} (${duration}ms)")
            Log.d("NetworkClient", "响应头: ${response.headers}")

            response
        }
        .build()

    val apiService: ApiService by lazy {
        val serverUrl = getServerUrl()
        Log.d("NetworkClient", "🔧 初始化 Retrofit")
        Log.d("NetworkClient", "服务器URL: $serverUrl")

        Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun getServerUrl(): String {
        val url = sharedPrefs.getString("server_url", defaultServerUrl) ?: defaultServerUrl
        Log.d("NetworkClient", "📍 获取服务器URL: $url")
        return url
    }

    fun setServerUrl(url: String) {
        Log.d("NetworkClient", "🔄 设置新的服务器URL: $url")
        sharedPrefs.edit().putString("server_url", url).apply()
    }

    // 添加用于调试的方法
    fun getCurrentServerUrl(): String {
        return getServerUrl()
    }

    // 测试网络连接的方法
    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("NetworkClient", "🧪 开始测试网络连接...")
                val response = apiService.healthCheck()
                val success = response.isSuccessful
                Log.d("NetworkClient", "🧪 网络连接测试结果: $success")
                success
            } catch (e: Exception) {
                Log.e("NetworkClient", "🧪 网络连接测试失败", e)
                false
            }
        }
    }
}