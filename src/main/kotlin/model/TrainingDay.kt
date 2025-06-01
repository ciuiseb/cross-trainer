package model

import model.enums.WorkoutType
import jakarta.persistence.*
import jakarta.persistence.Entity
import jakarta.persistence.GenerationType.IDENTITY
import kotlinx.serialization.Serializable

@Entity
@Table(name = "TrainingDays")
@Serializable
data class TrainingDay(
    @Id
    @GeneratedValue(strategy = IDENTITY)
    val id: Long = 0,
    @Column(name = "training_plan_id", nullable = false)
    val trainingPlanId: Long,
    @Column(name = "day_number", nullable = false)
    val dayNumber: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "workout_type", nullable = false)
    val workoutType: WorkoutType,
    @Column(name = "distance")
    val distance: String? = null,
    @Column(name = "duration")
    val duration: String? = null,
    @Column(name = "description")
    val description: String
) {
    constructor() : this(0, 0, 0, WorkoutType.EASY_RUN, null, null, "")
}
