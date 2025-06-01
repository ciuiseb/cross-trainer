package service.gemini

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
import model.enums.WorkoutType
import model.requests.*
import org.slf4j.LoggerFactory

data class GeminiServiceImpl(val config: GeminiConfig, val json: Json): GeminiService {

    private val logger = LoggerFactory.getLogger(GeminiServiceImpl::class.java)

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    override suspend fun determineFitnessLevel(request: FitnessLevelRequest): FitnessLevel {
        logger.info("Starting fitness level determination for user request")
        logger.debug("Request details: {}", request)

        val requestPrompt = request.getRequestPrompt()

        val requestBody = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject {
                            put("text", requestPrompt)
                        })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("temperature", 0.3)
                put("maxOutputTokens", 10)
            })
        }

        logger.debug("Sending request to Gemini API: {}", requestBody)

        try {
            val response = httpClient.post("${config.baseUrl}/models/${config.model}:generateContent") {
                header("Content-Type", "application/json")
                header("x-goog-api-key", config.apiKey)
                setBody(requestBody.toString())
            }

            val responseText = response.body<String>()
            logger.debug("Received response from Gemini API: {}", responseText)

            val jsonResponse = json.parseToJsonElement(responseText).jsonObject
            val candidates = jsonResponse["candidates"]?.jsonArray
            val content = candidates?.get(0)?.jsonObject?.get("content")?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            val text = parts?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content?.trim()?.uppercase()

            logger.debug("Extracted fitness level text: '{}'", text)

            val fitnessLevel = try {
                FitnessLevel.valueOf(text ?: "NONE")
            } catch (e: IllegalArgumentException) {
                logger.warn("Failed to parse fitness level '{}', defaulting to NONE", text, e)
                FitnessLevel.NONE
            }

            logger.info("Determined fitness level: {}", fitnessLevel)
            return fitnessLevel

        } catch (e: Exception) {
            logger.error("Error determining fitness level", e)
            throw e
        }
    }

    override suspend fun generateTrainingPlan(request: TrainingPlanRequest): Pair<TrainingPlan, List<TrainingDay>> {
        logger.info("Starting training plan generation for user ID: {}", request.userId)
        logger.debug("Training plan request: {}", request)

        val requestPrompt = request.getRequestPrompt()
        logger.info("Generated training plan prompt: {}", requestPrompt)

        val requestBody = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject {
                            put("text", requestPrompt)
                        })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("temperature", 0.7)
            })
        }

        logger.debug("Full request body to Gemini API: {}", requestBody)
        logger.info("Sending training plan request to Gemini API")

        try {
            val response = httpClient.post("${config.baseUrl}/models/${config.model}:generateContent") {
                header("Content-Type", "application/json")
                header("x-goog-api-key", config.apiKey)
                setBody(requestBody.toString())
            }

            val responseText = response.body<String>()
            logger.debug("Received training plan response from Gemini API: {}", responseText)

            // Check if response contains an error
            val jsonResponse = json.parseToJsonElement(responseText).jsonObject
            if (jsonResponse.containsKey("error")) {
                val error = jsonResponse["error"]?.jsonObject
                val errorMessage = error?.get("message")?.jsonPrimitive?.content
                logger.error("Gemini API returned error: {}", errorMessage)
                throw IllegalStateException("Gemini API error: $errorMessage")
            }

            val candidates = jsonResponse["candidates"]?.jsonArray
            val content = candidates?.get(0)?.jsonObject?.get("content")?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            val aiResponseText = parts?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content

            logger.debug("Raw AI response text: {}", aiResponseText)

            if (aiResponseText.isNullOrBlank()) {
                logger.error("AI response text is null or empty")
                throw IllegalStateException("Empty response from AI")
            }

            val cleanedJson = aiResponseText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            logger.debug("Cleaned JSON for parsing: {}", cleanedJson)

            logger.debug("Parsing AI response to training plan")
            val aiJson = try {
                json.parseToJsonElement(cleanedJson).jsonObject
            } catch (e: Exception) {
                logger.error("Failed to parse AI response as JSON: {}", aiResponseText, e)
                throw IllegalStateException("Invalid JSON response from AI: $aiResponseText", e)
            }

            val trainingPlanJson = aiJson["trainingPlan"]?.jsonObject
            if (trainingPlanJson == null) {
                logger.error("No 'trainingPlan' found in AI response: {}", aiJson)
                throw IllegalStateException("Missing trainingPlan in AI response")
            }

            val trainingPlan = TrainingPlan(
                userId = request.userId,
                name = json.decodeFromJsonElement<String>(trainingPlanJson["name"]!!),
                targetDistance = json.decodeFromJsonElement<String>(trainingPlanJson["targetDistance"]!!),
                preparationWeeks = json.decodeFromJsonElement<Int>(trainingPlanJson["preparationWeeks"]!!)
            )

            val trainingDaysJson = aiJson["trainingDays"]?.jsonArray
            if (trainingDaysJson == null) {
                logger.error("No 'trainingDays' found in AI response: {}", aiJson)
                throw IllegalStateException("Missing trainingDays in AI response")
            }

            val trainingDays = trainingDaysJson.map { dayElement ->
                val dayObject = dayElement.jsonObject

                val workoutTypeString = json.decodeFromJsonElement<String>(dayObject["workoutType"]!!)
                val workoutTypeEnum = WorkoutType.values().find { it.displayName == workoutTypeString }
                    ?: run {
                        logger.warn("Unknown workout type '{}', defaulting to EASY_RUN", workoutTypeString)
                        WorkoutType.REST
                    }

                TrainingDay(
                    trainingPlanId = 0,
                    dayNumber = json.decodeFromJsonElement<Int>(dayObject["dayNumber"]!!),
                    workoutType = workoutTypeEnum,
                    distance = dayObject["distance"]?.jsonPrimitive?.content,
                    duration = dayObject["duration"]?.jsonPrimitive?.content,
                    description = json.decodeFromJsonElement<String>(dayObject["description"]!!)
                )
            }

            logger.info("Successfully generated training plan '{}' with {} training days",
                trainingPlan.name, trainingDays.size)
            logger.debug("Training plan details: {}", trainingPlan)

            return Pair(trainingPlan, trainingDays)

        } catch (e: Exception) {
            logger.error("Error generating training plan for user ID: {}", request.userId, e)
            throw e
        }
    }
}