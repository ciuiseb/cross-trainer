package model

import model.enums.FitnessLevel
import model.enums.UserRole
import java.util.*

data class User(
    override val id: Long = 0,
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    val fitnessLevel: FitnessLevel,
    val role: UserRole = UserRole.USER,
    val createdAt: Date = Date()
) : Entity