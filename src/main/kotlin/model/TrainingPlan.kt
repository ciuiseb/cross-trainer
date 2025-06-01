package model

import java.time.LocalDate
import javax.persistence.*
import javax.persistence.Entity

@Entity
@Table(name = "TrainingPlans")
data class TrainingPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "name", nullable = false)
    val name: String,
    @Column(name = "target_distance", nullable = false)
    val targetDistance: String,
    @Column(name = "preparation_weeks", nullable = false)
    val preparationWeeks: Int,
    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate = LocalDate.now(),
    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate = startDate.plusDays(preparationWeeks * 7L)
)