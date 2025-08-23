package cn.edu.tju.notepad.ai

/**
 * 支持的模型类型
 */
enum class ModelType(val modelName: String, val endpoint: String) {
    ERNIE_4_0("ERNIE-4.0", "completions_pro"),
}
