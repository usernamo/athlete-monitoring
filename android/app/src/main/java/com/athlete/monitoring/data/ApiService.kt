package com.athlete.monitoring.data

import com.athlete.monitoring.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("health")
    suspend fun health(): Map<String, @JvmSuppressWildcards Any>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): LoginResponse

    @GET("api/athletes/{id}/profile")
    suspend fun getProfile(@Path("id") athleteId: String): ProfileResponse

    @PATCH("api/athletes/{id}/profile")
    suspend fun updateProfile(
        @Path("id") athleteId: String,
        @Body body: UpdateProfileRequest
    ): ProfileResponse

    @GET("api/coach/{coachId}/athletes/all")
    suspend fun coachAllAthletes(@Path("coachId") coachId: String): AthletesListResponse

    @GET("api/coach/{coachId}/athletes")
    suspend fun coachMyAthletes(@Path("coachId") coachId: String): AthletesListResponse

    @PUT("api/coach/{coachId}/athletes")
    suspend fun coachSetTeam(
        @Path("coachId") coachId: String,
        @Body body: SetTeamRequest
    ): AthletesListResponse

    @GET("api/coach/{coachId}/overview")
    suspend fun coachOverview(@Path("coachId") coachId: String): CoachOverviewResponse

    @GET("api/coach/{coachId}/calendar-month/{yearMonth}")
    suspend fun coachCalendarMonth(
        @Path("coachId") coachId: String,
        @Path("yearMonth") yearMonth: String
    ): CoachCalendarMonthResponse

    @GET("api/coach/{coachId}/day/{date}")
    suspend fun coachDaySlaves(
        @Path("coachId") coachId: String,
        @Path("date") date: String
    ): CoachDaySlavesResponse

    @GET("api/coach/{coachId}/athletes/{athleteId}/day/{date}")
    suspend fun coachAthleteDayStats(
        @Path("coachId") coachId: String,
        @Path("athleteId") athleteId: String,
        @Path("date") date: String
    ): CoachAthleteDayStatsResponse

    @GET("api/athletes/{id}/recommendations")
    suspend fun recommendations(@Path("id") athleteId: String): List<RecommendationDto>

    @GET("api/coach/{coachId}/athletes/{athleteId}/training-plan")
    suspend fun coachTrainingPlan(
        @Path("coachId") coachId: String,
        @Path("athleteId") athleteId: String
    ): TrainingPlanResponse

    @PUT("api/coach/{coachId}/athletes/{athleteId}/training-plan")
    suspend fun saveCoachTrainingPlan(
        @Path("coachId") coachId: String,
        @Path("athleteId") athleteId: String,
        @Body body: SaveTrainingPlanRequest
    ): TrainingPlanResponse

    @GET("api/athletes/{id}/training-plan")
    suspend fun athleteTrainingPlan(@Path("id") athleteId: String): TrainingPlanResponse

    @POST("api/coach/{coachId}/athletes/{athleteId}/competitions")
    suspend fun coachAddCompetition(
        @Path("coachId") coachId: String,
        @Path("athleteId") athleteId: String,
        @Body body: CompetitionRequest
    ): CompetitionEntryDto

    @GET("api/athletes/{id}/dashboard")
    suspend fun dashboard(@Path("id") athleteId: String): DashboardResponse

    @GET("api/metric-types")
    suspend fun metricTypes(): List<MetricTypeDto>

    @POST("api/athletes/{id}/metrics")
    suspend fun addMetric(@Path("id") athleteId: String, @Body body: AddMetricRequest): Map<String, Any>

    @POST("api/athletes/{id}/daily-reports")
    suspend fun addDailyReport(@Path("id") athleteId: String, @Body body: AddDailyReportRequest): Map<String, Any>

    @GET("api/athletes/{id}/nutrition")
    suspend fun nutrition(@Path("id") athleteId: String): List<NutritionLogRow>

    @GET("api/athletes/{id}/nutrition-day/{date}")
    suspend fun nutritionDay(@Path("id") athleteId: String, @Path("date") date: String): NutritionDayDto

    @POST("api/athletes/{id}/nutrition-day")
    suspend fun saveNutritionDay(@Path("id") athleteId: String, @Body body: SaveNutritionDayRequest): NutritionDayDto

    @GET("api/diary/form-schema")
    suspend fun diaryFormSchema(): DiaryFormSchema

    @GET("api/athletes/{id}/trainings/{date}")
    suspend fun athleteTrainingDay(
        @Path("id") athleteId: String,
        @Path("date") date: String
    ): AthleteTrainingDayResponse

    @POST("api/athletes/{id}/trainings/{date}")
    suspend fun saveAthleteTrainingDay(
        @Path("id") athleteId: String,
        @Path("date") date: String,
        @Body body: TrainingDayDto
    ): SaveAthleteTrainingResponse

    @GET("api/athletes/{id}/diary/{date}")
    suspend fun diaryDay(@Path("id") athleteId: String, @Path("date") date: String): DiaryDayDto

    @POST("api/athletes/{id}/diary-entry")
    suspend fun saveDiaryEntry(@Path("id") athleteId: String, @Body body: DiaryEntryRequest): Map<String, Any>

    @GET("api/athletes/{id}/diary-history")
    suspend fun diaryHistory(@Path("id") athleteId: String): DiaryHistoryResponse

    @GET("api/athletes/{id}/calendar-month/{yearMonth}")
    suspend fun calendarMonth(
        @Path("id") athleteId: String,
        @Path("yearMonth") yearMonth: String
    ): CalendarMonthResponse

    @GET("api/athletes/{id}/competitions")
    suspend fun competitions(@Path("id") athleteId: String): List<CompetitionEntryDto>

    @PUT("api/athletes/{id}/competitions/{entryId}")
    suspend fun updateCompetition(
        @Path("id") athleteId: String,
        @Path("entryId") entryId: String,
        @Body body: CompetitionRequest
    ): Map<String, Any>

    @POST("api/athletes/{id}/competitions")
    suspend fun addCompetition(@Path("id") athleteId: String, @Body body: CompetitionRequest): Map<String, Any>

    @GET("api/athletes/{id}/training-camps")
    suspend fun trainingCamps(@Path("id") athleteId: String): List<TrainingCampDto>

    @POST("api/athletes/{id}/training-camps")
    suspend fun addTrainingCamp(@Path("id") athleteId: String, @Body body: TrainingCampRequest): TrainingCampDto

    @GET("api/athletes/{id}/medical-exams")
    suspend fun medicalExams(@Path("id") athleteId: String): List<MedicalExamDto>

    @POST("api/athletes/{id}/medical-exams")
    suspend fun addMedicalExam(@Path("id") athleteId: String, @Body body: MedicalExamRequest): MedicalExamDto

    @GET("api/athletes/{id}/menstrual-cycles")
    suspend fun menstrualCycles(@Path("id") athleteId: String): List<MenstrualCycleDto>

    @POST("api/athletes/{id}/menstrual-cycles")
    suspend fun addMenstrualCycle(@Path("id") athleteId: String, @Body body: MenstrualCycleRequest): MenstrualCycleDto

}
