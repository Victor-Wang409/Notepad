package cn.edu.tju.notepad.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 百度文心一言API客户端
 */
class WenxinApiClient(private val context: Context) {

    companion object {
        private const val TAG = "WenxinApiClient"

        // API端点
        private const val TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token"
        private const val CHAT_BASE_URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/"

        // 缓存的access token
        private var accessToken: String? = null
        private var tokenExpireTime: Long = 0
    }

    private val configManager = AiConfigManager(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 获取Access Token
     */
    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            // 检查缓存的token是否有效
            if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
                return@withContext accessToken
            }

            val apiKey = configManager.getApiKey()
            val secretKey = configManager.getSecretKey()

            if (!configManager.isConfigured()) {
                Log.e(TAG, "API密钥未配置")
                return@withContext null
            }

            val url = "$TOKEN_URL?grant_type=client_credentials&client_id=$apiKey&client_secret=$secretKey"
            val request = Request.Builder()
                .url(url)
                .post("".toRequestBody())
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val json = JSONObject(responseBody ?: "{}")

                accessToken = json.optString("access_token")
                val expiresIn = json.optLong("expires_in", 0)

                // 设置过期时间，提前5分钟刷新
                tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000

                Log.d(TAG, "获取Access Token成功")
                return@withContext accessToken
            } else {
                Log.e(TAG, "获取Access Token失败: ${response.code}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取Access Token异常", e)
            return@withContext null
        }
    }

    /**
     * 生成笔记内容
     * @param prompt 用户输入的提示词
     * @param style 生成风格（formal/casual/creative）
     * @return 生成的内容，失败返回null
     */
    suspend fun generateNoteContent(
        prompt: String,
        style: GenerationStyle = GenerationStyle.NORMAL
    ): GeneratedNote? = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()
            if (token == null) {
                Log.e(TAG, "无法获取Access Token")
                return@withContext null
            }

            val systemPrompt = when (style) {
                GenerationStyle.FORMAL -> "你是一个专业的笔记助手，请用正式、专业的语言生成笔记内容。"
                GenerationStyle.CASUAL -> "你是一个友好的笔记助手，请用轻松、口语化的语言生成笔记内容。"
                GenerationStyle.CREATIVE -> "你是一个富有创意的笔记助手，请用生动、有创意的语言生成笔记内容。"
                GenerationStyle.SUMMARY -> "你是一个擅长总结的笔记助手，请生成简洁明了的总结性笔记。"
                GenerationStyle.DETAILED -> "你是一个细致的笔记助手，请生成详细、全面的笔记内容。"
                GenerationStyle.NORMAL -> "你是一个智能笔记助手，请根据用户输入生成合适的笔记内容。"
            }

            val fullPrompt = """
                $systemPrompt
                
                用户需求：$prompt
                
                请生成一篇结构清晰的笔记，包含：
                1. 一个简洁的标题
                2. 主要内容（分段落组织）
                3. 如果适用，可以包含要点列表
                
                请直接返回JSON格式：
                {
                    "title": "笔记标题",
                    "content": "笔记内容"
                }
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", fullPrompt)
                    })
                })
                put("temperature", 0.7)
                put("top_p", 0.8)
                put("penalty_score", 1.0)
            }

            val modelType = configManager.getModelType()
            val chatUrl = CHAT_BASE_URL + modelType.endpoint
            val url = "$chatUrl?access_token=$token"
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val json = JSONObject(responseBody ?: "{}")

                val result = json.optString("result", "")

                // 尝试解析JSON格式的响应
                return@withContext try {
                    val resultJson = JSONObject(result)
                    GeneratedNote(
                        title = resultJson.optString("title", "AI生成的笔记"),
                        content = resultJson.optString("content", result)
                    )
                } catch (e: Exception) {
                    // 如果不是JSON格式，直接使用原始内容
                    GeneratedNote(
                        title = "AI生成的笔记 - ${prompt.take(20)}",
                        content = result
                    )
                }
            } else {
                Log.e(TAG, "生成内容失败: ${response.code}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "生成内容异常", e)
            return@withContext null
        }
    }

    /**
     * 优化已有笔记内容
     */
    suspend fun improveNoteContent(
        title: String,
        content: String,
        improveType: ImproveType
    ): GeneratedNote? = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()
            if (token == null) {
                Log.e(TAG, "无法获取Access Token")
                return@withContext null
            }

            val prompt = when (improveType) {
                ImproveType.GRAMMAR -> "请修正以下笔记的语法错误和拼写错误，保持原意不变。"
                ImproveType.EXPAND -> "请扩展以下笔记的内容，添加更多细节和说明。"
                ImproveType.SIMPLIFY -> "请简化以下笔记的内容，使其更加简洁明了。"
                ImproveType.STRUCTURE -> "请重新组织以下笔记的结构，使其更有条理。"
                ImproveType.PROFESSIONAL -> "请将以下笔记改写得更加专业和正式。"
            }

            val fullPrompt = """
                $prompt
                
                原始标题：$title
                原始内容：$content
                
                请返回优化后的JSON格式：
                {
                    "title": "优化后的标题",
                    "content": "优化后的内容"
                }
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", fullPrompt)
                    })
                })
                put("temperature", 0.5)
            }

            val url = "$CHAT_BASE_URL?access_token=$token"
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val json = JSONObject(responseBody ?: "{}")
                val result = json.optString("result", "")

                return@withContext try {
                    val resultJson = JSONObject(result)
                    GeneratedNote(
                        title = resultJson.optString("title", title),
                        content = resultJson.optString("content", content)
                    )
                } catch (e: Exception) {
                    GeneratedNote(title = title, content = result)
                }
            } else {
                Log.e(TAG, "优化内容失败: ${response.code}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "优化内容异常", e)
            return@withContext null
        }
    }

    /**
     * 根据关键词生成笔记大纲
     */
    suspend fun generateOutline(keywords: List<String>): GeneratedNote? = withContext(Dispatchers.IO) {
        val prompt = "请根据以下关键词生成一个详细的笔记大纲：${keywords.joinToString(", ")}"
        return@withContext generateNoteContent(prompt, GenerationStyle.DETAILED)
    }
}

/**
 * 生成风格
 */
enum class GenerationStyle {
    NORMAL,      // 普通
    FORMAL,      // 正式
    CASUAL,      // 休闲
    CREATIVE,    // 创意
    SUMMARY,     // 总结
    DETAILED     // 详细
}

/**
 * 改进类型
 */
enum class ImproveType {
    GRAMMAR,      // 语法修正
    EXPAND,       // 扩展内容
    SIMPLIFY,     // 简化内容
    STRUCTURE,    // 优化结构
    PROFESSIONAL  // 专业化
}

/**
 * 生成的笔记内容
 */
data class GeneratedNote(
    val title: String,
    val content: String
)