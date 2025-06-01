import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import database.DatabaseHelper
import gui.FitnessLevelForm
import gui.navigation.RunningTrainingApp
import kotlinx.serialization.json.Json
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import repository.impl.TrainingDayRepositoryImpl
import repository.impl.TrainingPlanRepositoryImpl
import repository.impl.UserRepositoryImpl
import service.openapi.GeminiConfig
import service.openapi.GeminiServiceImpl
import service.server.ServiceImpl
import java.io.FileNotFoundException
import java.util.*

fun main() = application {

    Window(
        onCloseRequest = ::exitApplication,
        title = "Running Training App"
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                RunningTrainingApp()
            }
        }
    }
}
