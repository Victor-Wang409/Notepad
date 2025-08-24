package cn.edu.tju.notepad.ai

import android.content.Context
import android.content.SharedPreferences

/**
 * AI配置管理器 - 支持新版API
 * 管理Bearer Token和App ID
 */
class AiConfigManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "ai_config_prefs"
        private const val KEY_APP_ID = "app_id"
        private const val KEY_BEARER_TOKEN = "bearer_token"
        private const val KEY_ENABLED = "ai_enabled"
        private const val KEY_MODEL_TYPE = "model_type"
        private const val KEY_AUTO_SUGGEST = "auto_suggest"
        private const val KEY_SAVE_HISTORY = "save_history"

        // 默认配置 - 请替换为您的实际认证信息
        private const val DEFAULT_APP_ID = ""
        private const val DEFAULT_BEARER_TOKEN = ""
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 获取App ID
     */
    fun getAppId(): String {
        return prefs.getString(KEY_APP_ID, DEFAULT_APP_ID) ?: DEFAULT_APP_ID
    }

    /**
     * 设置App ID
     */
    fun setAppId(appId: String) {
        prefs.edit().putString(KEY_APP_ID, appId).apply()
    }

    /**
     * 获取Bearer Token
     */
    fun getBearerToken(): String {
        return prefs.getString(KEY_BEARER_TOKEN, DEFAULT_BEARER_TOKEN) ?: DEFAULT_BEARER_TOKEN
    }

    /**
     * 设置Bearer Token
     */
    fun setBearerToken(bearerToken: String) {
        prefs.edit().putString(KEY_BEARER_TOKEN, bearerToken).apply()
    }

    /**
     * 检查是否已配置
     */
    fun isConfigured(): Boolean {
        val bearerToken = getBearerToken()
        return bearerToken.isNotEmpty() && bearerToken != DEFAULT_BEARER_TOKEN
    }

    /**
     * 获取AI功能是否启用
     */
    fun isAiEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLED, true) && isConfigured()
    }

    /**
     * 设置AI功能是否启用
     */
    fun setAiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * 获取模型类型
     */
    fun getModelType(): ModelType {
        val typeOrdinal = prefs.getInt(KEY_MODEL_TYPE, ModelType.ERNIE_3_5_8K.ordinal)
        return try {
            ModelType.values()[typeOrdinal]
        } catch (e: Exception) {
            ModelType.ERNIE_3_5_8K
        }
    }

    /**
     * 设置模型类型
     */
    fun setModelType(modelType: ModelType) {
        prefs.edit().putInt(KEY_MODEL_TYPE, modelType.ordinal).apply()
    }

    /**
     * 获取是否启用自动建议
     */
    fun isAutoSuggestEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SUGGEST, false)
    }

    /**
     * 设置是否启用自动建议
     */
    fun setAutoSuggestEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SUGGEST, enabled).apply()
    }

    /**
     * 获取是否保存AI生成历史
     */
    fun isSaveHistoryEnabled(): Boolean {
        return prefs.getBoolean(KEY_SAVE_HISTORY, true)
    }

    /**
     * 设置是否保存AI生成历史
     */
    fun setSaveHistoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_HISTORY, enabled).apply()
    }

    /**
     * 清除所有配置
     */
    fun clearConfig() {
        prefs.edit().clear().apply()
    }

    /**
     * 验证Bearer Token格式
     * Bearer Token格式: bce-v3/ALTAK-xxxx/xxxxx
     */
    fun validateBearerToken(bearerToken: String): Boolean {
        // 检查基本格式
        if (bearerToken.isEmpty()) return false

        // 检查是否符合百度Bearer Token格式
        val pattern = Regex("^bce-v3/[A-Za-z0-9\\-]+/[a-f0-9]+$")
        return pattern.matches(bearerToken)
    }

    /**
     * 验证App ID格式
     */
    fun validateAppId(appId: String): Boolean {
        // App ID可以为空（某些情况下不需要）
        // 如果不为空，应该是数字或字母数字组合
        if (appId.isEmpty()) return true
        return appId.matches(Regex("[a-zA-Z0-9\\-_]+"))
    }

    /**
     * 保存API测试结果
     */
    fun saveTestResult(success: Boolean, message: String) {
        prefs.edit()
            .putBoolean("last_test_success", success)
            .putString("last_test_message", message)
            .putLong("last_test_time", System.currentTimeMillis())
            .apply()
    }

    /**
     * 获取上次测试结果
     */
    fun getLastTestResult(): Pair<Boolean, String>? {
        val success = prefs.getBoolean("last_test_success", false)
        val message = prefs.getString("last_test_message", "") ?: ""
        val time = prefs.getLong("last_test_time", 0)

        // 如果超过24小时，认为测试结果过期
        if (System.currentTimeMillis() - time > 24 * 60 * 60 * 1000) {
            return null
        }

        return Pair(success, message)
    }

    /**
     * 获取完整的认证信息（用于显示）
     */
    fun getAuthInfo(): String {
        val bearerToken = getBearerToken()
        return if (bearerToken.isNotEmpty() && bearerToken.length > 20) {
            // 隐藏中间部分，只显示前后部分
            "${bearerToken.take(15)}...${bearerToken.takeLast(10)}"
        } else {
            "未配置"
        }
    }

    /**
     * 从旧版API密钥迁移（如果需要）
     */
    fun migrateFromOldApi() {
        // 检查是否有旧的API Key和Secret Key
        val oldApiKey = prefs.getString("wenxin_api_key", null)
        val oldSecretKey = prefs.getString("wenxin_secret_key", null)

        if (!oldApiKey.isNullOrEmpty() && !oldSecretKey.isNullOrEmpty()) {
            // 提示用户需要更新到新的Bearer Token
            prefs.edit()
                .remove("wenxin_api_key")
                .remove("wenxin_secret_key")
                .putBoolean("needs_migration", true)
                .apply()
        }
    }

    /**
     * 检查是否需要迁移
     */
    fun needsMigration(): Boolean {
        return prefs.getBoolean("needs_migration", false)
    }

    /**
     * 完成迁移
     */
    fun completeMigration() {
        prefs.edit().putBoolean("needs_migration", false).apply()
    }
}