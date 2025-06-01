package service.server

import model.TrainingPlan
import model.User
import model.enums.FitnessLevel
import model.requests.FitnessLevelRequest
import model.requests.TrainingPlanRequest
import repository.interfaces.TrainingDayRepository
import repository.interfaces.TrainingPlanRepository
import repository.interfaces.UserRepository
import service.openapi.GeminiService

class ServiceImpl(
    private val userRepository: UserRepository,
    private val trainingPlanRepository: TrainingPlanRepository,
    private val traningDayRepository: TrainingDayRepository,
    private val geminiService: GeminiService
) : Service {

    override suspend fun register(name: String, username: String, email: String, password: String): User? {
        if (userRepository.findByUsername(username) != null) {
            return null
        }

        val user = User(
            name = name,
            username = username,
            email = email,
            password = password.hashCode().toString(),
            fitnessLevel = FitnessLevel.NONE
        )

        return userRepository.save(user)
    }

    override suspend fun login(username: String, password: String): User? {
        val supposedUser = userRepository.findByUsername(username)
        if (supposedUser == null) {
            return null
        }
        if (supposedUser.password != password.hashCode().toString()) {
            return null
        }
        return supposedUser
    }

    override suspend fun getUserTrainingPlan(user: User): TrainingPlan? {
        if (user.fitnessLevel == FitnessLevel.NONE) {
            return null
        }
        return trainingPlanRepository.findByUserId(user.id).firstOrNull()
    }

    override suspend fun updateFitnessLevel(user: User, fitnessLevel: FitnessLevel): User {
        val updatedUser = user.copy(fitnessLevel = fitnessLevel)

        return userRepository.update(updatedUser)
    }

    override suspend fun updateTrainingPlan(user: User, plan: TrainingPlan): TrainingPlan? {
        if (plan.userId != user.id) {
            return null
        }

        return trainingPlanRepository.save(plan)
    }

    override suspend fun assessFitnessLevel(fitnessLevelRequest: FitnessLevelRequest): FitnessLevel {
        return geminiService.determineFitnessLevel(fitnessLevelRequest)
    }

    override suspend fun generateTrainingPlan(trainingPlanRequest: TrainingPlanRequest): TrainingPlan {
        val (trainingPlan, trainingDays) = geminiService.generateTrainingPlan(trainingPlanRequest)
        val savedTrainingPlan = trainingPlanRepository.save(trainingPlan)

        trainingDays.map { day ->
            day.copy(trainingPlanId = savedTrainingPlan.id)
        }.forEach { day ->
            traningDayRepository.save(day)
        }

        return savedTrainingPlan
    }
}