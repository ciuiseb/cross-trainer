package service.openapi

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.*
import model.TrainingDay
import model.TrainingPlan
import model.enums.FitnessLevel
import model.requests.*

data class GeminiServiceImpl(val config: GeminiConfig, val json: Json): GeminiService {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }


    override suspend fun determineFitnessLevel(request: FitnessLevelRequest): FitnessLevel {
        val requestBody = """
        {
          "contents": [
            {
              "parts": [
                {
                  "text": "${request.getRequestPrompt()}"
                }
              ]
            }
          ],
          "generationConfig": {
            "temperature": 0.3,
            "maxOutputTokens": 10
          }
        }
        """.trimIndent()

        val response = httpClient.post("${config.baseUrl}/models/${config.model}:generateContent") {
            header("Content-Type", "application/json")
            header("x-goog-api-key", config.apiKey)
            setBody(requestBody)
        }

        val responseText = response.body<String>()
        val jsonResponse = json.parseToJsonElement(responseText).jsonObject
        val candidates = jsonResponse["candidates"]?.jsonArray
        val content = candidates?.get(0)?.jsonObject?.get("content")?.jsonObject
        val parts = content?.get("parts")?.jsonArray
        val text = parts?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content?.trim()?.uppercase()

        return try {
            FitnessLevel.valueOf(text ?: "NONE")
        } catch (e: IllegalArgumentException) {
            FitnessLevel.NONE
        }
    }


    override suspend fun generateTrainingPlan(request: TrainingPlanRequest): Pair<TrainingPlan, List<TrainingDay>> {
        val requestBody = """
        {
          "contents": [
            {
              "parts": [
                {
                  "text": "${request.getRequestPrompt()}"
                }
              ]
            }
          ],
          "generationConfig": {
            "temperature": 0.7
          }
        }
        """.trimIndent()

        val response = httpClient.post("${config.baseUrl}/models/${config.model}:generateContent") {
            header("Content-Type", "application/json")
            header("x-goog-api-key", config.apiKey)
            setBody(requestBody)
        }

        val responseText = response.body<String>()
        val jsonResponse = json.parseToJsonElement(responseText).jsonObject
        val candidates = jsonResponse["candidates"]?.jsonArray
        val content = candidates?.get(0)?.jsonObject?.get("content")?.jsonObject
        val parts = content?.get("parts")?.jsonArray
        val aiResponseText = parts?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content

        val aiJson = json.parseToJsonElement(aiResponseText!!).jsonObject

        val trainingPlanJson = aiJson["trainingPlan"]?.jsonObject!!
        val trainingPlan = TrainingPlan(
            userId = request.userId,
            name = json.decodeFromJsonElement<String>(trainingPlanJson["name"]!!),
            targetDistance = json.decodeFromJsonElement<String>(trainingPlanJson["targetDistance"]!!),
            preparationWeeks = json.decodeFromJsonElement<Int>(trainingPlanJson["preparationWeeks"]!!)
        )

        val trainingDaysJson = aiJson["trainingDays"]?.jsonArray!!
        val trainingDays = trainingDaysJson.map { dayElement ->
            val dayData = json.decodeFromJsonElement<TrainingDay>(dayElement)
            dayData.copy(trainingPlanId = 0)
        }

        return Pair(trainingPlan, trainingDays)
    }

}