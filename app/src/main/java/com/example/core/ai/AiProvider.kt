package com.example.core.ai

enum class ProviderType(val displayName: String, val defaultEndpoint: String, val defaultModel: String) {
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com", "gemini-3.5-flash"),
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "deepseek-coder"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1", "claude-3-7-sonnet"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", "anthropic/claude-3.7-sonnet"),
    CUSTOM("Custom OpenAI-Compatible", "http://localhost:11434/v1", "llama3")
}

data class ModelOption(
    val id: String,
    val name: String,
    val description: String,
    val providerType: ProviderType
)

object SupportedModels {
    val GEMINI_MODELS = listOf(
        ModelOption("gemini-3.5-flash", "Gemini 3.5 Flash", "Fast, high-efficiency model for code generation and chat", ProviderType.GEMINI),
        ModelOption("gemini-3.1-pro-preview", "Gemini 3.1 Pro Preview", "Advanced reasoning and deep code refactoring", ProviderType.GEMINI),
        ModelOption("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite", "Ultra-low latency lightweight assistant", ProviderType.GEMINI)
    )

    val OPENAI_MODELS = listOf(
        ModelOption("gpt-4o", "GPT-4o", "Flagship multimodal intelligence", ProviderType.OPENAI),
        ModelOption("gpt-4o-mini", "GPT-4o Mini", "Fast and lightweight model", ProviderType.OPENAI)
    )

    val DEEPSEEK_MODELS = listOf(
        ModelOption("deepseek-coder", "DeepSeek Coder V2", "Specialized coding and programming intelligence", ProviderType.DEEPSEEK),
        ModelOption("deepseek-chat", "DeepSeek Chat", "General conversation and analysis", ProviderType.DEEPSEEK)
    )

    val ANTHROPIC_MODELS = listOf(
        ModelOption("claude-3-7-sonnet", "Claude 3.7 Sonnet", "Hybrid reasoning and code engineering", ProviderType.ANTHROPIC),
        ModelOption("claude-3-5-haiku", "Claude 3.5 Haiku", "Speed and lightweight code tasks", ProviderType.ANTHROPIC)
    )

    fun getAllForProvider(provider: ProviderType): List<ModelOption> {
        return when (provider) {
            ProviderType.GEMINI -> GEMINI_MODELS
            ProviderType.OPENAI -> OPENAI_MODELS
            ProviderType.DEEPSEEK -> DEEPSEEK_MODELS
            ProviderType.ANTHROPIC -> ANTHROPIC_MODELS
            ProviderType.OPENROUTER -> listOf(ModelOption("auto", "Auto Router", "Best available model for prompt", ProviderType.OPENROUTER))
            ProviderType.CUSTOM -> listOf(ModelOption("custom-model", "Custom Model", "Locally hosted or proxy model", ProviderType.CUSTOM))
        }
    }
}
