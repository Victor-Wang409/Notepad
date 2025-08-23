package cn.edu.tju.notepad.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * AI配置管理器
 * 安全地存储和管理API密钥
 */
class AiConfigManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "ai_config_prefs"
        private const val KEY_API_KEY = "wenxin_api_key"
        private const val KEY_SECRET_KEY = "wenxin_secret_key"
        private const val KEY_ENABLED = "ai_enabled"
        private const val KEY_MODEL_TYPE = "model_type"

        // 默认配置
        private const val DEFAULT_API_KEY = "default_api_key"
        private const val DEFAULT_SECRET_KEY = "default_secret_key"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * 获取API Key
     */
    fun getApiKey(): String {
        return encryptedPrefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }

    /**
     * 设置API Key
     */
    fun setApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    /**
     * 获取Secret Key
     */
    fun getSecretKey(): String {
        return encryptedPrefs.getString(KEY_SECRET_KEY, DEFAULT_SECRET_KEY) ?: DEFAULT_SECRET_KEY
    }

    /**
     * 设置Secret Key
     */
    fun setSecretKey(secretKey: String) {
        encryptedPrefs.edit().putString(KEY_SECRET_KEY, secretKey).apply()
    }

    /**
     * 检查是否已配置API密钥
     */
    fun isConfigured(): Boolean {
        val apiKey = getApiKey()
        val secretKey = getSecretKey()
        return apiKey.isNotEmpty() && secretKey.isNotEmpty()
    }

    /**
     * 获取AI功能是否启用
     */
    fun isAiEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_ENABLED, true)
    }

    /**
     * 设置AI功能是否启用
     */
    fun setAiEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * 获取模型类型
     */
    fun getModelType(): ModelType {
        val typeOrdinal = encryptedPrefs.getInt(KEY_MODEL_TYPE, ModelType.ERNIE_4_0.ordinal)
        return ModelType.values().getOrNull(typeOrdinal) ?: ModelType.ERNIE_4_0
    }

    /**
     * 设置模型类型
     */
    fun setModelType(modelType: ModelType) {
        encryptedPrefs.edit().putInt(KEY_MODEL_TYPE, modelType.ordinal).apply()
    }

    /**
     * 清除所有配置
     */
    fun clearConfig() {
        encryptedPrefs.edit().clear().apply()
    }

    /**
     * 验证API密钥格式
     */
    fun validateApiKey(apiKey: String): Boolean {
        // 基本格式验证
        return apiKey.length >= 20 && apiKey.matches(Regex("[a-zA-Z0-9]+"))
    }

    /**
     * 验证Secret Key格式
     */
    fun validateSecretKey(secretKey: String): Boolean {
        // 基本格式验证
        return secretKey.length >= 20 && secretKey.matches(Regex("[a-zA-Z0-9]+"))
    }
}