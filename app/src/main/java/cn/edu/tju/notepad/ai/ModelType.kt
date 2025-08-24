package cn.edu.tju.notepad.ai

/**
 * 支持的模型类型 - 新版API模型代码
 */
enum class ModelType(val modelName: String, val modelCode: String) {
    ERNIE_4_0_8K("ERNIE-4.0-8K", "ernie-4.0-8k"),
    ERNIE_4_0("ERNIE-4.0", "ernie-4.0"),
    ERNIE_3_5_8K("ERNIE-3.5-8K", "ernie-3.5-8k"),
    ERNIE_3_5("ERNIE-3.5", "ernie-3.5"),
    ERNIE_TURBO("ERNIE-Turbo", "ernie-turbo"),
    ERNIE_SPEED("ERNIE-Speed", "ernie-speed")
}