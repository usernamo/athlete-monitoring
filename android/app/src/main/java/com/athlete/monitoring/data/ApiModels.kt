package com.athlete.monitoring.data

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val email: String,
    val role: String,
    val profile: AthleteProfileDto? = null,
    val coach_profile: CoachProfileDto? = null
)

data class CoachProfileDto(
    val id: String,
    val specialization: String? = null,
    val experience_years: Int? = null
)

data class AthleteBriefDto(
    val id: String,
    val first_name: String,
    val last_name: String,
    val full_name: String? = null,
    val sport: String? = null,
    val age_years: Int? = null,
    val gender: String? = null,
    val assigned: Boolean = false
) {
    val fullName: String
        get() = full_name?.trim()?.takeIf { it.isNotEmpty() }
            ?: "$first_name $last_name".trim()
}

data class CoachCalendarMonthResponse(
    val year_month: String,
    val diary_dates: List<String> = emptyList(),
    val training_dates: List<String> = emptyList(),
    val competitions: List<CoachCalendarCompetitionDto> = emptyList()
)

data class CoachCalendarCompetitionDto(
    val id: String,
    val name: String?,
    val event_date: String?,
    val athlete_id: String? = null,
    val athlete_name: String? = null,
    val outcome: String? = "pending"
)

data class CoachDaySlavesResponse(
    val date: String,
    val athletes: List<CoachDayAthleteStatusDto> = emptyList()
)

data class CoachDayAthleteStatusDto(
    val athlete: AthleteBriefDto,
    val has_diary: Boolean = false,
    val has_training: Boolean = false,
    val has_nutrition: Boolean = false,
    val competition: CoachDayCompetitionBriefDto? = null
)

data class CoachDayCompetitionBriefDto(
    val name: String? = null,
    val outcome: String? = null
)

data class CoachDiaryDayDto(
    val sleep_time: String? = null,
    val wake_time: String? = null,
    val bedtime: String? = null,
    val notes: String? = null,
    val daily_activity: Int? = null,
    val wellbeing_evening: Int? = null,
    val fatigue_daily: Int? = null,
    val water_ml: Int = 0,
    val metrics: Map<String, Double>? = null,
    val training: TrainingDayDto? = null
)

data class CoachAthleteDayStatsResponse(
    val athlete: AthleteBriefDto,
    val date: String,
    val diary: CoachDiaryDayDto? = null,
    val nutrition_meals: Int = 0,
    val competition: CompetitionEntryDto? = null,
    val analytics: AnalyticsRow? = null
)

data class AthletesListResponse(val athletes: List<AthleteBriefDto>)

data class CoachOverviewItem(
    val athlete: AthleteBriefDto,
    val analytics: AnalyticsRow?,
    val recent_trainings: List<TrainingRow>?,
    val recent_recommendations: List<RecommendationDto>?
)

data class CoachOverviewResponse(val overview: List<CoachOverviewItem>)

data class RecommendationDto(
    val id: String,
    val recommendation_text: String,
    val category: String? = null,
    val created_at: String? = null,
    val coach_name: String? = null
)

data class TrainingPlanItemDto(
    val id: String? = null,
    val scheduled_date: String,
    val title: String? = null,
    val preparation_period: String? = null,
    val part_warmup: String? = null,
    val part_main: String? = null,
    val part_cooldown: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val duration_minutes: Int? = null,
    val coach_notes: String? = null,
    val sort_order: Int? = null
)

data class TrainingPlanDto(
    val id: String,
    val title: String,
    val notes: String? = null,
    val updated_at: String? = null,
    val coach_name: String? = null,
    val items: List<TrainingPlanItemDto> = emptyList()
)

data class TrainingPlanResponse(val plan: TrainingPlanDto?)

data class SaveTrainingPlanRequest(
    val title: String = "План тренировок",
    val notes: String? = null,
    val items: List<TrainingPlanItemDto> = emptyList()
)

data class CoachAddTrainingRequest(
    val date: String,
    val start_time: String? = "09:00",
    val end_time: String? = "11:00",
    val duration_minutes: Int? = 90,
    val volume: Double? = null,
    val intensity: Double? = null,
    val exercise_name: String? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val distance: Int? = null,
    val notes: String? = null
)

data class SetTeamRequest(val athlete_ids: List<String>)

data class AthleteProfileDto(
    val id: String,
    val full_name: String? = null,
    val first_name: String? = null,
    val last_name: String? = null,
    val sport: String? = null,
    val qualification: String? = null,
    val residence_address: String? = null,
    val phone: String? = null,
    val coach_full_name: String? = null,
    val coach_phone: String? = null,
    val institution: String? = null,
    val age_years: Int? = null,
    val gender: String? = null,
    val height_cm: Double? = null,
    val weight_kg: Double? = null,
    val chest_cm: Double? = null
) {
    val displayName: String
        get() = full_name?.trim()?.takeIf { it.isNotEmpty() }
            ?: listOfNotNull(first_name, last_name).joinToString(" ").trim().ifBlank { "Спортсмен" }
}

data class UpdateProfileRequest(
    val full_name: String,
    val sport: String? = null,
    val qualification: String? = null,
    val residence_address: String? = null,
    val phone: String? = null,
    val coach_full_name: String? = null,
    val coach_phone: String? = null,
    val institution: String? = null
)

data class ProfileResponse(val profile: AthleteProfileDto)

data class RegisterRequest(
    val email: String,
    val password: String,
    val first_name: String,
    val last_name: String,
    val age: Int,
    val gender: String,
    val height_cm: Double? = null,
    val weight_kg: Double? = null,
    val chest_cm: Double? = null
)

data class MenstrualCycleDto(
    val id: String,
    val cycle_start_date: String,
    val cycle_end_date: String?,
    val cycle_length_days: Int?,
    val notes: String?
)

data class MenstrualCycleRequest(
    val cycle_start_date: String,
    val cycle_end_date: String? = null,
    val cycle_length_days: Int? = null,
    val notes: String? = null
)

data class DashboardResponse(
    val profile: ProfileSummary?,
    val recentMetrics: List<MetricRow>,
    val dailyReports: List<DailyReportRow>,
    val trainings: List<TrainingRow>,
    val analytics: AnalyticsRow?,
    val waterTodayMl: Int
)

data class ProfileSummary(
    val first_name: String,
    val last_name: String,
    val sport: String?,
    val weight_kg: Double?
)

data class MetricRow(
    val name: String,
    val unit: String?,
    val value: Double,
    val measured_at: String
)

data class DailyReportRow(
    val report_date: String,
    val sleep_time: String?,
    val wake_time: String?,
    val notes: String?
)

data class TrainingRow(
    val id: String,
    val date: String,
    val duration_minutes: Int?,
    val volume: Double?,
    val intensity: Double?,
    val readiness_score: Double?,
    val fatigue_score: Double?
)

data class AnalyticsRow(
    val readiness_score: Double?,
    val recovery_score: Double?,
    val fatigue_score: Double?,
    val injury_risk_score: Double?,
    val generated_at: String
)

data class MetricTypeDto(
    val id: String,
    val name: String,
    val unit: String?,
    val min_value: Double?,
    val max_value: Double?,
    val category: String
)

data class AddMetricRequest(
    val metric_type_id: String,
    val value: Double,
    val notes: String? = null
)

data class AddDailyReportRequest(
    val report_date: String? = null,
    val sleep_time: String? = null,
    val wake_time: String? = null,
    val notes: String? = null
)

data class NutritionLogRow(
    val id: String,
    val meal_type: String,
    val consumed_at: String,
    val items: List<NutritionItemRow>?
)

data class NutritionItemRow(
    val product_name: String,
    val grams: Double?,
    val calories: Double?
)

data class NutritionDayDto(
    val date: String,
    val meals: List<NutritionMealDto>,
    val total: NutritionTotalsDto,
    val sports_nutrition: String?,
    val pharmacology: String?,
    val water_ml: Int
)

data class NutritionMealDto(
    val id: String?,
    val meal_number: Int?,
    val meal_type: String?,
    val consumed_at: String?,
    val appetite: Boolean?,
    val is_snack: Boolean?,
    val items: List<NutritionProductDto>,
    val subtotal: NutritionTotalsDto?
)

data class NutritionProductDto(
    val id: String? = null,
    val product_name: String,
    val grams: Double? = null,
    val quantity_ml: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val calories: Double? = null
)

data class NutritionTotalsDto(
    val grams: Double = 0.0,
    val ml: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    val calories: Double = 0.0
)

data class NutritionMealRequest(
    val meal_number: Int?,
    val meal_type: String?,
    val time: String?,
    val consumed_at: String? = null,
    val appetite: Boolean?,
    val is_snack: Boolean = false,
    val items: List<NutritionProductDto>
)

data class SaveNutritionDayRequest(
    val date: String,
    val replace_day: Boolean = true,
    val meals: List<NutritionMealRequest>,
    val sports_nutrition: String?,
    val pharmacology: String?,
    val water_ml: String?
)

data class DiaryFormSchema(val sections: List<DiarySectionDto>)

data class DiarySectionDto(
    val id: String,
    val title: String,
    val fields: List<DiaryFieldDto>
)

data class DiaryFieldOption(val value: String, val label: String)

data class DiaryFieldDto(
    val kind: String,
    val label: String,
    val key: String? = null,
    val name: String? = null,
    val options: List<DiaryFieldOption>? = null,
    val training: Boolean? = null
)

data class TrainingDayDto(
    val preparation_period: String? = null,
    val desire_to_train: Int? = null,
    val wellbeing_morning: Int? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val part_warmup: String? = null,
    val part_main: String? = null,
    val part_cooldown: String? = null,
    val planned_hr_before: Int? = null,
    val planned_hr_after: Int? = null,
    val actual_hr_before: Int? = null,
    val actual_hr_after: Int? = null,
    val duration_minutes: Int? = null,
    val work_capacity: Int? = null,
    val fatigue_training: Int? = null,
    val volume: Double? = null,
    val intensity: Double? = null,
    val readiness_score: Double? = null,
    val fatigue_score: Double? = null
)

data class AthleteTrainingDayResponse(
    val training: TrainingDayDto? = null,
    val coach_training: TrainingDayDto? = null
)

data class SaveAthleteTrainingResponse(
    val ok: Boolean? = null,
    val training: TrainingDayDto?,
    val report_date: String
)

data class DiaryDayDto(
    val report_date: String,
    val sleep_time: String?,
    val wake_time: String?,
    val notes: String?,
    val bedtime: String? = null,
    val daily_activity: Int? = null,
    val wellbeing_evening: Int? = null,
    val fatigue_daily: Int? = null,
    val water_ml: Int,
    val metrics: Map<String, Double>?,
    val training: TrainingDayDto?,
    val coach_training: TrainingDayDto? = null
)

data class DiaryEntryRequest(
    val report_date: String,
    val sleep_time: String? = null,
    val wake_time: String? = null,
    val notes: String? = null,
    val bedtime: String? = null,
    val daily_activity: String? = null,
    val wellbeing_evening: String? = null,
    val fatigue_daily: String? = null,
    val water_ml: String? = null,
    val metrics: Map<String, Double> = emptyMap(),
    val training: TrainingDayDto? = null
)

data class DiaryHistoryResponse(
    val days: Int,
    val history: List<DiaryHistoryDay>
)

data class DiaryHistoryDay(
    val report_date: String,
    val metrics: Map<String, Double>?
)

data class CompetitionEntryDto(
    val id: String,
    val name: String?,
    val event_date: String?,
    val location: String?,
    val planned_result: String?,
    val actual_result: String?,
    val notes: String? = null,
    val outcome: String? = "pending"
)

data class CompetitionRequest(
    val name: String,
    val event_date: String,
    val location: String? = null,
    val planned_result: String? = null,
    val actual_result: String? = null,
    val notes: String? = null,
    val outcome: String? = "pending"
)

data class CalendarMonthResponse(
    val year_month: String,
    val diary_dates: List<String> = emptyList(),
    val training_dates: List<String> = emptyList(),
    val competitions: List<CompetitionEntryDto> = emptyList()
)

data class TrainingCampDto(
    val id: String,
    val location: String?,
    val start_date: String?,
    val end_date: String?,
    val goals: String?,
    val notes: String?
)

data class TrainingCampRequest(
    val location: String? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val goals: String? = null,
    val notes: String? = null
)

data class MedicalExamDto(
    val id: String,
    val type: String?,
    val examination_date: String?,
    val institution: String?,
    val methods: String?,
    val recommendations: String?
)

data class MedicalExamRequest(
    val type: String = "mandatory",
    val examination_date: String,
    val institution: String? = null,
    val methods: String? = null,
    val recommendations: String? = null
)
