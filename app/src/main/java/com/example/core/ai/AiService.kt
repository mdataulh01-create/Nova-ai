package com.example.core.ai

import android.content.Context
import com.example.BuildConfig
import com.example.core.security.KeystoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiResponse(
    val content: String,
    val tokensUsed: Int = 0,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

class AiService(private val context: Context) {

    private val keystoreHelper = KeystoreHelper(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    fun getEffectiveApiKey(provider: ProviderType): String {
        val userConfigured = keystoreHelper.getDecryptedString("api_key_${provider.name}", "")
        if (userConfigured.isNotBlank()) {
            return userConfigured
        }
        if (provider == ProviderType.GEMINI) {
            val buildConfigKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                return buildConfigKey
            }
        }
        return ""
    }

    suspend fun generateResponse(
        prompt: String,
        provider: ProviderType = ProviderType.GEMINI,
        modelName: String = "gemini-3.5-flash",
        systemInstruction: String = "You are Nova AI, a senior full-stack software engineer and code assistant inside a native Android mobile IDE. Provide clean, production-grade code, step-by-step reasoning, and concise explanations.",
        projectContext: String? = null
    ): AiResponse = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(provider)
        if (apiKey.isBlank()) {
            return@withContext AiResponse(
                content = "",
                isError = true,
                errorMessage = "API Key not configured for ${provider.displayName}. Please configure your API key in Settings > AI Providers."
            )
        }

        try {
            when (provider) {
                ProviderType.GEMINI -> callGemini(apiKey, modelName, prompt, systemInstruction, projectContext)
                ProviderType.OPENAI, ProviderType.DEEPSEEK, ProviderType.OPENROUTER, ProviderType.CUSTOM -> {
                    callOpenAiCompatible(provider, apiKey, modelName, prompt, systemInstruction, projectContext)
                }
                ProviderType.ANTHROPIC -> callAnthropic(apiKey, modelName, prompt, systemInstruction, projectContext)
            }
        } catch (e: Exception) {
            AiResponse(
                content = "",
                isError = true,
                errorMessage = "Request failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun callGemini(
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String,
        projectContext: String?
    ): AiResponse {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val rootJson = JSONObject()

        // System Instruction
        if (systemInstruction.isNotBlank()) {
            val sysPart = JSONObject().put("text", systemInstruction)
            val sysParts = JSONArray().put(sysPart)
            rootJson.put("system_instruction", JSONObject().put("parts", sysParts))
        }

        // Contents
        val fullPrompt = if (projectContext != null) {
            "--- PROJECT CONTEXT ---\n$projectContext\n-----------------------\n\nUser Request: $prompt"
        } else {
            prompt
        }

        val userPart = JSONObject().put("text", fullPrompt)
        val userContent = JSONObject()
            .put("role", "user")
            .put("parts", JSONArray().put(userPart))

        rootJson.put("contents", JSONArray().put(userContent))

        val body = rootJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                JSONObject(responseBody).optJSONObject("error")?.optString("message")
                    ?: "HTTP ${response.code}: $responseBody"
            } catch (e: Exception) {
                "HTTP ${response.code}: $responseBody"
            }
            return AiResponse(content = "", isError = true, errorMessage = errorMsg)
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
            val textBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    textBuilder.append(parts.getJSONObject(i).optString("text"))
                }
            }
            val usage = json.optJSONObject("usageMetadata")
            val totalTokens = usage?.optInt("totalTokenCount") ?: 0
            return AiResponse(content = textBuilder.toString(), tokensUsed = totalTokens)
        }

        return AiResponse(content = "", isError = true, errorMessage = "Empty response from Gemini API.")
    }

    private fun callOpenAiCompatible(
        provider: ProviderType,
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String,
        projectContext: String?
    ): AiResponse {
        val baseUrl = provider.defaultEndpoint.trimEnd('/')
        val endpoint = "$baseUrl/chat/completions"

        val rootJson = JSONObject()
        rootJson.put("model", model)

        val messages = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemInstruction))
        }

        val fullPrompt = if (projectContext != null) {
            "--- PROJECT CONTEXT ---\n$projectContext\n-----------------------\n\n$prompt"
        } else {
            prompt
        }
        messages.put(JSONObject().put("role", "user").put("content", fullPrompt))
        rootJson.put("messages", messages)

        val body = rootJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return AiResponse(content = "", isError = true, errorMessage = "HTTP ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val messageObj = choices.getJSONObject(0).optJSONObject("message")
            val text = messageObj?.optString("content") ?: ""
            val totalTokens = json.optJSONObject("usage")?.optInt("total_tokens") ?: 0
            return AiResponse(content = text, tokensUsed = totalTokens)
        }

        return AiResponse(content = "", isError = true, errorMessage = "Empty response from ${provider.displayName}")
    }

    private fun callAnthropic(
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String,
        projectContext: String?
    ): AiResponse {
        val endpoint = "https://api.anthropic.com/v1/messages"
        val rootJson = JSONObject()
        rootJson.put("model", model)
        rootJson.put("max_tokens", 4096)
        if (systemInstruction.isNotBlank()) {
            rootJson.put("system", systemInstruction)
        }

        val messages = JSONArray()
        val fullPrompt = if (projectContext != null) {
            "--- PROJECT CONTEXT ---\n$projectContext\n-----------------------\n\n$prompt"
        } else {
            prompt
        }
        messages.put(JSONObject().put("role", "user").put("content", fullPrompt))
        rootJson.put("messages", messages)

        val body = rootJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return AiResponse(content = "", isError = true, errorMessage = "HTTP ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        val contentArray = json.optJSONArray("content")
        if (contentArray != null && contentArray.length() > 0) {
            val text = contentArray.getJSONObject(0).optString("text")
            return AiResponse(content = text)
        }
        return AiResponse(content = "", isError = true, errorMessage = "Empty response from Anthropic")
    }
}
