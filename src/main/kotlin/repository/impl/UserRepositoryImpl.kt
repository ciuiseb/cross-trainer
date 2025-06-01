package repository.impl

import database.DatabaseHelper
import model.User
import model.enums.FitnessLevel
import model.enums.UserRole
import repository.interfaces.UserRepository
import java.sql.ResultSet

class UserRepositoryImpl(private val dbHelper: DatabaseHelper) : UserRepository {
    private val tableName: String = "Users"
    private val userMapper: (ResultSet) -> User = { resultSet ->
        User(
            id = resultSet.getLong("id"),
            name = resultSet.getString("name"),
            username = resultSet.getString("username"),
            email = resultSet.getString("email"),
            password = resultSet.getString("password").hashCode().toString(),
            fitnessLevel = FitnessLevel.valueOf(resultSet.getString("fitness_level")),
            role = UserRole.valueOf(resultSet.getString("role"))
        )
    }

    override suspend fun findById(id: Long): User? {
        val sql = "SELECT * FROM $tableName WHERE id = ?"
        return dbHelper.executeQuery(sql, listOf(id), userMapper).firstOrNull()
    }

    override suspend fun findAll(): List<User> {
        val sql = "SELECT * FROM $tableName"
        return dbHelper.executeQuery(sql, emptyList(), userMapper)
    }

    override suspend fun save(entity: User): User {
        val sql = """
            INSERT INTO $tableName (name, username, email, password, fitness_level, role)
            VALUES (?, ?, ?, ?, ?, ?)
        """
        val params = listOf(
            entity.name,
            entity.username,
            entity.email,
            entity.password,
            entity.fitnessLevel.name,
            entity.role.name
        )

        val id = dbHelper.executeInsert(sql, params)
        return entity.copy(id = id)
    }

    override suspend fun update(entity: User): User {
        val sql = """
            UPDATE $tableName 
            SET name = ?, username = ?, email = ?, password = ?, fitness_level = ?, role = ?
            WHERE id = ?
        """
        val params = listOf(
            entity.name,
            entity.username,
            entity.email,
            entity.password,
            entity.fitnessLevel.name,
            entity.role.name,
            entity.id
        )

        dbHelper.executeUpdate(sql, params)
        return entity
    }

    override suspend fun delete(entity: User): Boolean {
        return deleteById(entity.id)
    }

    override suspend fun deleteById(id: Long): Boolean {
        val sql = "DELETE FROM $tableName WHERE id = ?"
        val rowsAffected = dbHelper.executeUpdate(sql, listOf(id))
        return rowsAffected > 0
    }

    override suspend fun count(): Long {
        val sql = "SELECT COUNT(*) as count FROM $tableName"
        val result = dbHelper.executeQuery(sql, emptyList()) { rs -> rs.getLong("count") }
        return result.firstOrNull() ?: 0
    }

    override suspend fun findByUsername(username: String): User? {
        val sql = "SELECT * FROM $tableName WHERE username = ?"
        return dbHelper.executeQuery(sql, listOf(username), userMapper).firstOrNull()
    }
}