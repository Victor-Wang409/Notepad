package cn.edu.tju.notepad.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 百度文心一言API客户端 - 使用新版API
 */
class WenxinApiClient(private val context: Context) {

    companion object {
        private const val TAG = "WenxinApiClient"

        // 新版API端点
        private const val API_URL = "https://qianfan.baidubce.com/v2/chat/completions"

        // 默认配置
        private const val DEFAULT_MAX_TOKENS = 2000
        private const val DEFAULT_TEMPERATURE = 0.7
    }

    private val configManager = AiConfigManager(context)

    // 配置HTTP客户端，增加读取超时时间
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)  // AI响应可能较慢，设置5分钟超时
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 生成笔记内容
     */
    suspend fun generateNoteContent(
        prompt: String,
        style: GenerationStyle = GenerationStyle.NORMAL
    ): GeneratedNote? = withContext(Dispatchers.IO) {
        try {
            // 检查配置
            if (!configManager.isConfigured()) {
                Log.e(TAG, "API未配置")
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
                1. 一个简洁的标题（不超过20个字）
                2. 主要内容（分段落组织，使用markdown格式）
                3. 如果适用，可以包含要点列表
                
                请严格按照以下JSON格式返回，不要包含markdown代码块标记：
                {
                    "title": "笔记标题",
                    "content": "笔记内容（使用markdown格式）"
                }
            """.trimIndent()

            // 构建请求体
            val requestBody = buildRequestBody(fullPrompt)

            // 发起请求
            val response = executeRequest(requestBody)

            // 解析响应
            return@withContext parseGenerateResponse(response, prompt)

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
            if (!configManager.isConfigured()) {
                Log.e(TAG, "API未配置")
                return@withContext null
            }

            val prompt = when (improveType) {
                ImproveType.GRAMMAR -> "请修正以下笔记的语法错误和拼写错误，保持原意不变。"
                ImproveType.EXPAND -> "请扩展以下笔记的内容，添加更多细节和说明，使其更加充实。"
                ImproveType.SIMPLIFY -> "请简化以下笔记的内容，提炼要点，使其更加简洁明了。"
                ImproveType.STRUCTURE -> "请重新组织以下笔记的结构，添加合适的段落和标题，使其更有条理。"
                ImproveType.PROFESSIONAL -> "请将以下笔记改写得更加专业和正式，适合商务场合。"
            }

            val fullPrompt = """
                $prompt
                
                原始标题：$title
                原始内容：
                $content
                
                请返回优化后的内容，格式要求：
                1. 保持原意不变
                2. 使用markdown格式
                3. 结构清晰，易于阅读
                
                请严格按照以下JSON格式返回：
                {
                    "title": "优化后的标题",
                    "content": "优化后的内容（markdown格式）"
                }
            """.trimIndent()

            val requestBody = buildRequestBody(fullPrompt)
            val response = executeRequest(requestBody)

            return@withContext parseImproveResponse(response, title, content)

        } catch (e: Exception) {
            Log.e(TAG, "优化内容异常", e)
            return@withContext null
        }
    }

    /**
     * 构建请求体
     */
    private fun buildRequestBody(userContent: String): String {
        val modelType = configManager.getModelType()

        val json = JSONObject().apply {
            // 使用配置的模型
            put("model", modelType.modelCode)

            // 构建消息数组
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                })
            })

            // 禁用网络搜索（纯生成任务）
            put("web_search", JSONObject().apply {
                put("enable", false)
                put("enable_citation", false)
                put("enable_trace", false)
            })

            // 空的插件选项
            put("plugin_options", JSONObject())

            // 添加其他参数
            put("temperature", DEFAULT_TEMPERATURE)
            put("max_tokens", DEFAULT_MAX_TOKENS)
        }

        return json.toString()
    }

    /**
     * 执行HTTP请求
     */
    private fun executeRequest(requestBody: String): String? {
        try {
            val appId = configManager.getAppId()
            val bearerToken = configManager.getBearerToken()

            if (bearerToken.isEmpty()) {
                Log.e(TAG, "Bearer Token未配置")
                return null
            }

            val mediaType = "application/json".toMediaType()
            val body = requestBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("appid", appId)
                .addHeader("Authorization", "Bearer $bearerToken")
                .build()

            Log.d(TAG, "发送请求: $API_URL")

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "响应成功: ${responseBody?.take(200)}")
                return responseBody
            } else {
                val errorBody = response.body?.string()
                Log.e(TAG, "请求失败: ${response.code} - $errorBody")
                return null
            }

        } catch (e: Exception) {
            Log.e(TAG, "请求异常", e)
            return null
        }
    }

    /**
     * 解析生成响应
     */
    private fun parseGenerateResponse(response: String?, originalPrompt: String): GeneratedNote? {
        if (response.isNullOrEmpty()) return null

        try {
            val json = JSONObject(response)

            // 检查是否有错误
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                Log.e(TAG, "API错误: ${error.optString("message")}")
                return null
            }

            // 获取AI生成的内容
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                val message = choice.getJSONObject("message")
                val content = message.getString("content")

                // 尝试解析JSON格式的内容
                return try {
                    val cleanContent = content
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    val resultJson = JSONObject(cleanContent)
                    GeneratedNote(
                        title = resultJson.optString("title", "AI生成的笔记"),
                        content = resultJson.optString("content", content)
                    )
                } catch (e: Exception) {
                    // 如果不是JSON格式，直接使用原始内容
                    Log.w(TAG, "无法解析为JSON，使用原始内容")
                    val title = "AI笔记 - ${originalPrompt.take(15)}${if(originalPrompt.length > 15) "..." else ""}"
                    GeneratedNote(title = title, content = content)
                }
            }

            return null

        } catch (e: Exception) {
            Log.e(TAG, "解析响应失败", e)
            return null
        }
    }

    /**
     * 解析优化响应
     */
    private fun parseImproveResponse(response: String?, originalTitle: String, originalContent: String): GeneratedNote? {
        if (response.isNullOrEmpty()) return null

        try {
            val json = JSONObject(response)

            // 检查错误
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                Log.e(TAG, "API错误: ${error.optString("message")}")
                return null
            }

            // 获取内容
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                val message = choice.getJSONObject("message")
                val content = message.getString("content")

                // 尝试解析JSON格式
                return try {
                    val cleanContent = content
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    val resultJson = JSONObject(cleanContent)
                    GeneratedNote(
                        title = resultJson.optString("title", originalTitle),
                        content = resultJson.optString("content", originalContent)
                    )
                } catch (e: Exception) {
                    // 使用原始内容
                    GeneratedNote(title = originalTitle, content = content)
                }
            }

            return null

        } catch (e: Exception) {
            Log.e(TAG, "解析优化响应失败", e)
            return null
        }
    }

    /**
     * 测试连接
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val testPrompt = "测试连接，请回复'连接成功'"
            val requestBody = buildRequestBody(testPrompt)
            val response = executeRequest(requestBody)

            if (!response.isNullOrEmpty()) {
                val json = JSONObject(response)
                return@withContext !json.has("error")
            }

            return@withContext false

        } catch (e: Exception) {
            Log.e(TAG, "测试连接失败", e)
            return@withContext false
        }
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