package database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

class DatabaseHelper(private val dbPath: String) {
    private val connection: Connection by lazy {
        try {
            DriverManager.getConnection(dbPath)
        } catch (e: Exception) {
            throw RuntimeException("Failed to connect to database: $dbPath", e)
        }
    }

    init {
        Class.forName("org.sqlite.JDBC")
    }

    suspend fun <T> executeQuery(
        sql: String,
        params: List<Any?> = emptyList(),
        mapper: (ResultSet) -> T
    ): List<T> = withContext(Dispatchers.IO) {
        val results = mutableListOf<T>()
        connection.prepareStatement(sql).use { statement ->
            bindParameters(statement, params)
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    results.add(mapper(resultSet))
                }
            }
        }
        results
    }

    suspend fun executeUpdate(
        sql: String,
        params: List<Any?> = emptyList()
    ): Int = withContext(Dispatchers.IO) {
        connection.prepareStatement(sql).use { statement ->
            bindParameters(statement, params)
            statement.executeUpdate()
        }
    }

    suspend fun executeInsert(
        sql: String,
        params: List<Any?> = emptyList()
    ): Long = withContext(Dispatchers.IO) {
        connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS).use { statement ->
            bindParameters(statement, params)
            statement.executeUpdate()

            statement.generatedKeys.use { keys ->
                if (keys.next()) {
                    keys.getLong(1)
                } else {
                    throw RuntimeException("Failed to get generated ID")
                }
            }
        }
    }

    private fun bindParameters(statement: PreparedStatement, params: List<Any?>) {
        params.forEachIndexed { index, param ->
            when (param) {
                null -> statement.setNull(index + 1, java.sql.Types.NULL)
                is String -> statement.setString(index + 1, param)
                is Int -> statement.setInt(index + 1, param)
                is Long -> statement.setLong(index + 1, param)
                is Double -> statement.setDouble(index + 1, param)
                is Boolean -> statement.setBoolean(index + 1, param)
                else -> throw IllegalArgumentException("Unsupported parameter type: ${param::class.java}")
            }
        }
    }

    fun close() {
        try {
            if (!connection.isClosed) {
                connection.close()
            }
        } catch (e: Exception) {
            // Log error or handle as appropriate
        }
    }
}