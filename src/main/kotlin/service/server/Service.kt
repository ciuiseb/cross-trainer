package service.server

import model.TrainingPlan
import model.User
import model.enums.FitnessLevel
import model.requests.FitnessLevelRequest
import model.requests.TrainingPlanRequest

interface Service {
    suspend fun register(name: String, username: String, email: String, password: String): User?
    suspend fun login(username: String, password: String): User?
    suspend fun getUserTrainingPlan(user: User): TrainingPlan?
    suspend fun updateFitnessLevel(user: User, fitnessLevel: FitnessLevel): User?
    suspend fun updateTrainingPlan(user: User, plan: TrainingPlan): TrainingPlan?
    suspend fun assessFitnessLevel(fitnessLevelRequest: FitnessLevelRequest): FitnessLevel
    suspend fun generateTrainingPlan(trainingPlanRequest: TrainingPlanRequest): TrainingPlan
}