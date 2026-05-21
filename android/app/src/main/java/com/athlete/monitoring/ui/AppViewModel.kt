package com.athlete.monitoring.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.athlete.monitoring.data.AddMetricRequest
import com.athlete.monitoring.data.ApiClient
import com.athlete.monitoring.data.ApiService
import com.athlete.monitoring.data.DashboardResponse
import com.athlete.monitoring.data.DiaryEntryRequest
import com.athlete.monitoring.data.TrainingDayDto
import com.athlete.monitoring.data.DiaryFormSchema
import com.athlete.monitoring.data.DiaryHistoryResponse
import com.athlete.monitoring.data.LoginRequest
import com.athlete.monitoring.data.MenstrualCycleDto
import com.athlete.monitoring.data.MenstrualCycleRequest
import com.athlete.monitoring.data.RegisterRequest
import com.athlete.monitoring.data.UpdateProfileRequest
import com.athlete.monitoring.data.MetricTypeDto
import com.athlete.monitoring.data.NutritionDayDto
import com.athlete.monitoring.data.NutritionLogRow
import com.athlete.monitoring.data.NutritionMealRequest
import com.athlete.monitoring.data.NutritionProductDto
import com.athlete.monitoring.data.CalendarMonthResponse
import com.athlete.monitoring.data.CoachAthleteDayStatsResponse
import com.athlete.monitoring.data.CoachCalendarMonthResponse
import com.athlete.monitoring.data.CoachDaySlavesResponse
import com.athlete.monitoring.data.CompetitionEntryDto
import com.athlete.monitoring.data.CompetitionRequest
import com.athlete.monitoring.data.MedicalExamDto
import com.athlete.monitoring.data.MedicalExamRequest
import com.athlete.monitoring.data.SaveNutritionDayRequest
import com.athlete.monitoring.data.SaveTrainingPlanRequest
import com.athlete.monitoring.data.TrainingPlanDto
import com.athlete.monitoring.data.TrainingPlanItemDto
import com.athlete.monitoring.data.TrainingPlanResponse
import com.athlete.monitoring.data.TrainingCampDto
import com.athlete.monitoring.data.TrainingCampRequest
import com.athlete.monitoring.data.AthleteBriefDto
import com.athlete.monitoring.data.CoachOverviewItem
import com.athlete.monitoring.data.AthleteTrainingDayResponse
import com.athlete.monitoring.data.RecommendationDto
import com.athlete.monitoring.data.SetTeamRequest
import com.athlete.monitoring.data.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class UiState(
    val loading: Boolean = false,
    val error: String? = null,
    val user: UserDto? = null,
    val dashboard: DashboardResponse? = null,
    val metricTypes: List<MetricTypeDto> = emptyList(),
    val nutrition: List<NutritionLogRow> = emptyList(),
    val diaryForm: DiaryFormSchema? = null,
    val diaryHistory: DiaryHistoryResponse? = null,
    val diarySaved: Boolean = false,
    val nutritionDay: NutritionDayDto? = null,
    val nutritionSaved: Boolean = false,
    val competitions: List<CompetitionEntryDto> = emptyList(),
    val trainingCamps: List<TrainingCampDto> = emptyList(),
    val medicalExams: List<MedicalExamDto> = emptyList(),
    val calendarVersion: Int = 0,
    val calendarSaved: Boolean = false,
    val menstrualCycles: List<MenstrualCycleDto> = emptyList(),
    val profileSaved: Boolean = false,
    val selectedAthleteId: String? = null,
    val myAthletes: List<AthleteBriefDto> = emptyList(),
    val allAthletes: List<AthleteBriefDto> = emptyList(),
    val coachOverview: List<CoachOverviewItem> = emptyList(),
    val recommendations: List<RecommendationDto> = emptyList(),
    val coachActionSaved: Boolean = false,
    val calendarMonth: CalendarMonthResponse? = null,
    val coachCalendarMonth: CoachCalendarMonthResponse? = null,
    val coachDaySlaves: CoachDaySlavesResponse? = null,
    val coachAthleteDayStats: CoachAthleteDayStatsResponse? = null,
    val coachAthleteNutritionDay: NutritionDayDto? = null,
    val trainingPlan: TrainingPlanDto? = null,
    val athleteTrainingDay: TrainingDayDto? = null,
    val coachTrainingOnDay: TrainingDayDto? = null,
    val trainingSaved: Boolean = false,
    val trainingValidationError: Boolean = false
)

class AppViewModel : ViewModel() {
    private val api: ApiService
        get() = ApiClient.service()
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val coachId: String?
        get() = _state.value.user?.coach_profile?.id

    val athleteId: String?
        get() = when (_state.value.user?.role) {
            "coach" -> _state.value.selectedAthleteId
            else -> _state.value.user?.profile?.id
        }

    val isCoach: Boolean
        get() = _state.value.user?.role == "coach"

    fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        age: Int,
        gender: String,
        heightCm: Double?,
        weightKg: Double?,
        chestCm: Double?
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val res = api.register(
                    RegisterRequest(
                        email = email.trim().lowercase(),
                        password = password,
                        first_name = firstName.trim(),
                        last_name = lastName.trim(),
                        age = age,
                        gender = gender,
                        height_cm = heightCm,
                        weight_kg = weightKg,
                        chest_cm = chestCm
                    )
                )
                _state.value = _state.value.copy(loading = false, user = res.user)
                loadDashboard()
            } catch (e: HttpException) {
                val detail = e.response()?.errorBody()?.string()?.let { body ->
                    Regex(""""error"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
                }
                _state.value = _state.value.copy(loading = false, error = detail ?: e.message())
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun getApiBaseUrl(): String = ApiClient.getBaseUrl()

    fun setApiBaseUrl(url: String) {
        ApiClient.setBaseUrl(url)
        ApiClient.resetCache()
    }

    fun checkApiHealth(onResult: (ok: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = api.health()
                val status = res["status"]?.toString() ?: "ok"
                onResult(true, "Сервер доступен ($status)")
            } catch (e: Exception) {
                onResult(
                    false,
                    e.message ?: "Нет связи с сервером. Проверьте URL и что backend запущен."
                )
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val res = api.login(LoginRequest(email, password))
                _state.value = _state.value.copy(loading = false, user = res.user)
                if (res.user.role == "coach") {
                    loadCoachTeam()
                } else {
                    loadDashboard()
                    loadRecommendations()
                }
            } catch (e: HttpException) {
                val detail = e.response()?.errorBody()?.string()?.let { body ->
                    Regex(""""error"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
                }
                val msg = detail ?: e.message()
                _state.value = _state.value.copy(
                    loading = false,
                    error = "HTTP ${e.code()}: $msg"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Ошибка входа (проверьте API и Docker)"
                )
            }
        }
    }

    fun loadDashboard() {
        val id = athleteId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val dash = api.dashboard(id)
                _state.value = _state.value.copy(loading = false, dashboard = dash)
                if (!isCoach) loadRecommendations()
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun loadCoachTeam() {
        val cid = coachId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val mine = api.coachMyAthletes(cid).athletes
                val all = api.coachAllAthletes(cid).athletes
                val overview = api.coachOverview(cid).overview
                val keepSelected = _state.value.selectedAthleteId?.let { id ->
                    mine.find { it.id == id }?.id
                }
                _state.value = _state.value.copy(
                    loading = false,
                    myAthletes = mine,
                    allAthletes = all,
                    coachOverview = overview,
                    selectedAthleteId = keepSelected
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun loadCoachCalendarMonth(yearMonth: java.time.YearMonth) {
        val cid = coachId ?: return
        val ym = "%04d-%02d".format(yearMonth.year, yearMonth.monthValue)
        viewModelScope.launch {
            try {
                val m = api.coachCalendarMonth(cid, ym)
                _state.value = _state.value.copy(coachCalendarMonth = m)
            } catch (_: Exception) { }
        }
    }

    fun loadCoachDaySlaves(date: String) {
        val cid = coachId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val day = api.coachDaySlaves(cid, date)
                _state.value = _state.value.copy(coachDaySlaves = day, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun loadCoachAthleteDayStats(athleteId: String, date: String) {
        val cid = coachId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                error = null,
                coachAthleteDayStats = null,
                coachAthleteNutritionDay = null
            )
            try {
                val stats = api.coachAthleteDayStats(cid, athleteId, date)
                val nutrition = if (stats.nutrition_meals > 0) {
                    try {
                        api.nutritionDay(athleteId, date)
                    } catch (_: Exception) {
                        null
                    }
                } else null
                _state.value = _state.value.copy(
                    coachAthleteDayStats = stats,
                    coachAthleteNutritionDay = nutrition,
                    selectedAthleteId = athleteId,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun clearCoachDayView() {
        _state.value = _state.value.copy(
            coachDaySlaves = null,
            coachAthleteDayStats = null,
            coachAthleteNutritionDay = null
        )
    }

    fun selectAthlete(athleteId: String) {
        _state.value = _state.value.copy(selectedAthleteId = athleteId, coachActionSaved = false)
        loadDashboard()
        loadRecommendations()
    }

    fun openCoachAthlete(athleteId: String, yearMonth: java.time.YearMonth = java.time.YearMonth.now()) {
        _state.value = _state.value.copy(selectedAthleteId = athleteId, coachActionSaved = false)
        loadCalendarMonth(yearMonth)
        loadDiaryHistory()
        loadTrainingPlan(forCoach = true)
        loadDashboard()
        loadRecommendations()
    }

    fun closeCoachAthleteView() {
        _state.value = _state.value.copy(
            selectedAthleteId = null,
            calendarMonth = null,
            diaryHistory = null,
            dashboard = null
        )
        clearCoachDayView()
    }

    fun saveCoachTeam(selectedIds: List<String>) {
        val cid = coachId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val team = api.coachSetTeam(cid, SetTeamRequest(selectedIds)).athletes
                val keepSelected = _state.value.selectedAthleteId?.let { id ->
                    team.find { it.id == id }?.id
                }
                _state.value = _state.value.copy(
                    loading = false,
                    myAthletes = team,
                    allAthletes = api.coachAllAthletes(cid).athletes,
                    selectedAthleteId = keepSelected,
                    coachActionSaved = true
                )
                if (keepSelected == null) {
                    _state.value = _state.value.copy(calendarMonth = null, diaryHistory = null)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun loadRecommendations() {
        val id = athleteId ?: return
        viewModelScope.launch {
            try {
                val rows = api.recommendations(id)
                _state.value = _state.value.copy(recommendations = rows)
            } catch (_: Exception) { }
        }
    }

    fun loadTrainingPlan(forCoach: Boolean = isCoach) {
        val aid = if (forCoach) athleteId else _state.value.user?.profile?.id
        val cid = coachId
        if (aid == null) return
        viewModelScope.launch {
            try {
                val res = if (forCoach && cid != null) {
                    api.coachTrainingPlan(cid, aid)
                } else {
                    api.athleteTrainingPlan(aid)
                }
                _state.value = _state.value.copy(trainingPlan = res.plan)
            } catch (_: Exception) {
                _state.value = _state.value.copy(trainingPlan = null)
            }
        }
    }

    fun saveCoachTrainingPlan(
        title: String,
        notes: String?,
        items: List<TrainingPlanItemDto>
    ) {
        val cid = coachId ?: return
        val aid = athleteId
        if (aid == null) {
            _state.value = _state.value.copy(error = "Сначала выберите подопечного")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val res = api.saveCoachTrainingPlan(
                    cid,
                    aid,
                    SaveTrainingPlanRequest(
                        title = title.ifBlank { "План тренировок" },
                        notes = notes?.trim()?.ifBlank { null },
                        items = items
                    )
                )
                _state.value = _state.value.copy(
                    loading = false,
                    trainingPlan = res.plan,
                    coachActionSaved = true
                )
                res.plan?.items?.mapNotNull { it.scheduled_date.take(7) }?.distinct()?.forEach { ym ->
                    runCatching { loadCalendarMonth(java.time.YearMonth.parse(ym)) }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun coachAddCompetition(
        name: String,
        eventDate: String,
        location: String? = null,
        plannedResult: String? = null,
        notes: String? = null
    ) {
        val cid = coachId ?: return
        val aid = athleteId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                api.coachAddCompetition(
                    cid, aid,
                    CompetitionRequest(
                        name = name,
                        event_date = eventDate,
                        location = location?.trim()?.ifBlank { null },
                        planned_result = plannedResult?.trim()?.ifBlank { null },
                        notes = notes
                    )
                )
                _state.value = _state.value.copy(loading = false, coachActionSaved = true)
                loadCoachTeam()
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun updateProfile(
        fullName: String,
        sport: String?,
        qualification: String?,
        residenceAddress: String?,
        phone: String?,
        coachFullName: String?,
        coachPhone: String?,
        institution: String?
    ) {
        val id = athleteId ?: return
        val user = _state.value.user ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, profileSaved = false)
            try {
                val res = api.updateProfile(
                    id,
                    UpdateProfileRequest(
                        full_name = fullName.trim(),
                        sport = sport?.trim()?.ifBlank { null },
                        qualification = qualification?.trim()?.ifBlank { null },
                        residence_address = residenceAddress?.trim()?.ifBlank { null },
                        phone = phone?.trim()?.ifBlank { null },
                        coach_full_name = coachFullName?.trim()?.ifBlank { null },
                        coach_phone = coachPhone?.trim()?.ifBlank { null },
                        institution = institution?.trim()?.ifBlank { null }
                    )
                )
                _state.value = _state.value.copy(
                    loading = false,
                    profileSaved = true,
                    user = user.copy(profile = res.profile)
                )
                loadDashboard()
            } catch (e: HttpException) {
                val detail = e.response()?.errorBody()?.string()?.let { body ->
                    Regex(""""error"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
                }
                _state.value = _state.value.copy(loading = false, error = detail ?: e.message())
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun clearProfileSaved() {
        _state.value = _state.value.copy(profileSaved = false, error = null)
    }

    fun loadMetricTypes() {
        viewModelScope.launch {
            try {
                val types = api.metricTypes()
                _state.value = _state.value.copy(metricTypes = types)
            } catch (_: Exception) { }
        }
    }

    fun loadNutrition() {
        loadNutritionDay(java.time.LocalDate.now().toString())
    }

    fun loadNutritionDay(date: String) {
        val id = athleteId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val day = api.nutritionDay(id, date)
                _state.value = _state.value.copy(loading = false, nutritionDay = day, nutritionSaved = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun saveNutritionDay(
        date: String,
        meals: List<NutritionMealRequest>,
        sportsNutrition: String,
        pharmacology: String,
        waterMl: String
    ) {
        val id = athleteId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val day = api.saveNutritionDay(
                    id,
                    SaveNutritionDayRequest(
                        date = date,
                        replace_day = true,
                        meals = meals,
                        sports_nutrition = sportsNutrition.ifBlank { null },
                        pharmacology = pharmacology.ifBlank { null },
                        water_ml = waterMl.ifBlank { null }
                    )
                )
                _state.value = _state.value.copy(loading = false, nutritionDay = day, nutritionSaved = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun submitMetric(typeId: String, value: Double) {
        val id = athleteId ?: return
        viewModelScope.launch {
            try {
                api.addMetric(id, AddMetricRequest(typeId, value))
                loadDashboard()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun loadDiaryForm() {
        viewModelScope.launch {
            try {
                val form = api.diaryFormSchema()
                _state.value = _state.value.copy(diaryForm = form)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun loadDiaryHistory() {
        val id = athleteId ?: return
        viewModelScope.launch {
            try {
                val h = api.diaryHistory(id)
                _state.value = _state.value.copy(diaryHistory = h)
            } catch (_: Exception) { }
        }
    }

    fun loadCalendarMonth(yearMonth: java.time.YearMonth) {
        val id = athleteId ?: return
        val ym = "%04d-%02d".format(yearMonth.year, yearMonth.monthValue)
        viewModelScope.launch {
            try {
                val m = api.calendarMonth(id, ym)
                _state.value = _state.value.copy(calendarMonth = m)
            } catch (_: Exception) { }
        }
    }

    fun addCompetitionForDay(
        eventDate: String,
        name: String,
        location: String?,
        plannedResult: String?,
        actualResult: String?,
        notes: String?,
        outcome: String = "pending",
        onSuccess: (CompetitionEntryDto) -> Unit
    ) {
        val id = athleteId ?: return
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Укажите название соревнования")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val resp = api.addCompetition(
                    id,
                    CompetitionRequest(
                        name = name.trim(),
                        event_date = eventDate,
                        location = location?.trim()?.ifBlank { null },
                        planned_result = plannedResult?.trim()?.ifBlank { null },
                        actual_result = actualResult?.trim()?.ifBlank { null },
                        notes = notes?.trim()?.ifBlank { null },
                        outcome = outcome
                    )
                )
                val entryId = resp["id"]?.toString() ?: return@launch
                val ym = java.time.YearMonth.parse(eventDate.take(7))
                val ymStr = "%04d-%02d".format(ym.year, ym.monthValue)
                val month = api.calendarMonth(id, ymStr)
                _state.value = _state.value.copy(calendarMonth = month)
                onSuccess(
                    CompetitionEntryDto(
                        id = entryId,
                        name = name.trim(),
                        event_date = eventDate,
                        location = location?.trim()?.ifBlank { null },
                        planned_result = plannedResult?.trim()?.ifBlank { null },
                        actual_result = actualResult?.trim()?.ifBlank { null },
                        notes = notes?.trim()?.ifBlank { null },
                        outcome = outcome
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            } finally {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun updateCompetitionOutcome(entry: CompetitionEntryDto, outcome: String) {
        val id = athleteId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                api.updateCompetition(
                    id,
                    entry.id,
                    CompetitionRequest(
                        name = entry.name ?: "Соревнование",
                        event_date = entry.event_date ?: "",
                        location = entry.location,
                        planned_result = entry.planned_result,
                        actual_result = entry.actual_result,
                        notes = entry.notes,
                        outcome = outcome
                    )
                )
                entry.event_date?.take(7)?.let { java.time.YearMonth.parse(it) }?.let { loadCalendarMonth(it) }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            } finally {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    suspend fun loadAthleteTrainingDay(date: String): AthleteTrainingDayResponse? {
        val id = athleteId ?: return null
        return try {
            api.athleteTrainingDay(id, date)
        } catch (_: Exception) {
            null
        }
    }

    fun saveAthleteTraining(date: String, training: TrainingDayDto, onDone: () -> Unit = {}) {
        val id = athleteId
        if (id.isNullOrBlank()) {
            _state.value = _state.value.copy(
                error = "Профиль спортсмена не загружен. Выйдите и войдите снова."
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                error = null,
                trainingSaved = false,
                trainingValidationError = false
            )
            try {
                val res = api.saveAthleteTrainingDay(id, date, training)
                if (res.training == null) {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "Сервер не вернул сохранённую тренировку"
                    )
                    return@launch
                }
                _state.value = _state.value.copy(
                    loading = false,
                    trainingSaved = true,
                    athleteTrainingDay = res.training,
                    trainingValidationError = false,
                    error = null
                )
                try {
                    loadCalendarMonth(java.time.YearMonth.parse(date.take(7)))
                } catch (_: Exception) {
                }
                onDone()
            } catch (e: HttpException) {
                val bodyMsg = e.response()?.errorBody()?.string()?.let { raw ->
                    Regex(""""message"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.get(1)
                        ?: Regex(""""error"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.get(1)
                }
                if (e.code() == 400) {
                    _state.value = _state.value.copy(
                        loading = false,
                        trainingValidationError = true,
                        error = bodyMsg ?: "заполните все поля"
                    )
                } else {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = bodyMsg ?: "Ошибка сохранения (HTTP ${e.code()})"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Нет связи с сервером. Проверьте, что backend запущен."
                )
            }
        }
    }

    fun clearTrainingSaved() {
        _state.value = _state.value.copy(trainingSaved = false, trainingValidationError = false, error = null)
    }

    suspend fun loadDiaryDay(date: String): Map<String, String> {
        val id = athleteId ?: return emptyMap()
        val day = api.diaryDay(id, date)
        val out = mutableMapOf<String, String>()
        out["report_date"] = day.report_date
        day.sleep_time?.let { out["sleep_time"] = it.take(5) }
        day.wake_time?.let { out["wake_time"] = it.take(5) }
        day.bedtime?.let { out["bedtime"] = it.take(5) }
        day.notes?.let { out["notes"] = it }
        day.daily_activity?.let { out["daily_activity"] = it.toString() }
        day.wellbeing_evening?.let { out["wellbeing_evening"] = it.toString() }
        day.fatigue_daily?.let { out["fatigue_daily"] = it.toString() }
        out["water_ml"] = day.water_ml.toString()
        day.metrics?.forEach { (k, v) -> out[k] = v.toString() }
        day.training?.let { t ->
            fun put(key: String, v: Any?) {
                if (v != null) out["training_$key"] = v.toString()
            }
            put("preparation_period", t.preparation_period)
            put("desire_to_train", t.desire_to_train)
            put("wellbeing_morning", t.wellbeing_morning)
            t.start_time?.let { out["training_start_time"] = it.take(5) }
            t.end_time?.let { out["training_end_time"] = it.take(5) }
            put("part_warmup", t.part_warmup)
            put("part_main", t.part_main)
            put("part_cooldown", t.part_cooldown)
            put("planned_hr_before", t.planned_hr_before)
            put("planned_hr_after", t.planned_hr_after)
            put("actual_hr_before", t.actual_hr_before)
            put("actual_hr_after", t.actual_hr_after)
            put("duration_minutes", t.duration_minutes)
            put("work_capacity", t.work_capacity)
            put("fatigue_training", t.fatigue_training)
        }
        return out
    }

    private fun buildTrainingFromFields(fields: Map<String, String>): TrainingDayDto? {
        fun s(key: String) = fields["training_$key"]?.ifBlank { null }
        fun i(key: String) = s(key)?.toIntOrNull()
        val t = TrainingDayDto(
            preparation_period = s("preparation_period"),
            desire_to_train = i("desire_to_train"),
            wellbeing_morning = i("wellbeing_morning"),
            start_time = s("start_time"),
            end_time = s("end_time"),
            part_warmup = s("part_warmup"),
            part_main = s("part_main"),
            part_cooldown = s("part_cooldown"),
            planned_hr_before = i("planned_hr_before"),
            planned_hr_after = i("planned_hr_after"),
            actual_hr_before = i("actual_hr_before"),
            actual_hr_after = i("actual_hr_after"),
            duration_minutes = i("duration_minutes"),
            work_capacity = i("work_capacity"),
            fatigue_training = i("fatigue_training")
        )
        val hasData = listOf(
            t.preparation_period, t.desire_to_train, t.wellbeing_morning, t.start_time, t.end_time,
            t.part_warmup, t.part_main, t.part_cooldown, t.planned_hr_before, t.planned_hr_after,
            t.actual_hr_before, t.actual_hr_after, t.duration_minutes, t.work_capacity, t.fatigue_training
        ).any { it != null }
        return if (hasData) t else null
    }

    fun saveDiaryEntry(fields: Map<String, String>, onDone: () -> Unit = {}) {
        val id = athleteId ?: return
        val date = fields["report_date"] ?: java.time.LocalDate.now().toString()
        val metrics = mutableMapOf<String, Double>()
        val skip = setOf(
            "report_date", "sleep_time", "wake_time", "notes", "water_ml",
            "bedtime", "daily_activity", "wellbeing_evening", "fatigue_daily"
        )
        fields.forEach { (k, v) ->
            if (v.isBlank() || k.startsWith("training_") || k in skip) return@forEach
            v.toDoubleOrNull()?.let { metrics[k] = it }
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, diarySaved = false)
            try {
                api.saveDiaryEntry(
                    id,
                    DiaryEntryRequest(
                        report_date = date,
                        sleep_time = fields["sleep_time"]?.ifBlank { null },
                        wake_time = fields["wake_time"]?.ifBlank { null },
                        notes = fields["notes"]?.ifBlank { null },
                        bedtime = fields["bedtime"]?.ifBlank { null },
                        daily_activity = fields["daily_activity"]?.ifBlank { null },
                        wellbeing_evening = fields["wellbeing_evening"]?.ifBlank { null },
                        fatigue_daily = fields["fatigue_daily"]?.ifBlank { null },
                        water_ml = fields["water_ml"]?.ifBlank { null },
                        metrics = metrics,
                        training = null
                    )
                )
                _state.value = _state.value.copy(loading = false, diarySaved = true)
                loadDashboard()
                loadDiaryHistory()
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun loadCalendar() {
        val id = athleteId ?: return
        viewModelScope.launch {
            try {
                val comp = api.competitions(id)
                val camps = api.trainingCamps(id)
                val med = api.medicalExams(id)
                _state.value = _state.value.copy(
                    competitions = comp,
                    trainingCamps = camps,
                    medicalExams = med,
                    calendarVersion = _state.value.calendarVersion + 1,
                    calendarSaved = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun saveCompetitions(forms: List<com.athlete.monitoring.ui.CompetitionForm>) {
        val id = athleteId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                for (f in forms.filter { it.name.isNotBlank() && it.eventDate.isNotBlank() }) {
                    if (f.id == null) {
                        api.addCompetition(
                            id,
                            CompetitionRequest(
                                name = f.name,
                                event_date = f.eventDate,
                                location = f.location.ifBlank { null },
                                planned_result = f.plannedResult.ifBlank { null },
                                actual_result = f.actualResult.ifBlank { null },
                                notes = f.notes.ifBlank { null }
                            )
                        )
                    }
                }
                loadCalendar()
                _state.value = _state.value.copy(loading = false, calendarSaved = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun saveTrainingCamps(forms: List<com.athlete.monitoring.ui.CampForm>) {
        val id = athleteId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                for (f in forms.filter { it.location.isNotBlank() || it.startDate.isNotBlank() }) {
                    if (f.id == null) {
                        api.addTrainingCamp(
                            id,
                            TrainingCampRequest(
                                location = f.location.ifBlank { null },
                                start_date = f.startDate.ifBlank { null },
                                end_date = f.endDate.ifBlank { null },
                                goals = f.goals.ifBlank { null },
                                notes = f.notes.ifBlank { null }
                            )
                        )
                    }
                }
                loadCalendar()
                _state.value = _state.value.copy(loading = false, calendarSaved = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun saveMedicalExams(forms: List<com.athlete.monitoring.ui.MedicalForm>) {
        val id = athleteId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                for (f in forms.filter { it.examinationDate.isNotBlank() }) {
                    if (f.id == null) {
                        api.addMedicalExam(
                            id,
                            MedicalExamRequest(
                                type = f.type,
                                examination_date = f.examinationDate,
                                institution = f.institution.ifBlank { null },
                                methods = f.methods.ifBlank { null },
                                recommendations = f.recommendations.ifBlank { null }
                            )
                        )
                    }
                }
                loadCalendar()
                _state.value = _state.value.copy(loading = false, calendarSaved = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun loadMenstrualCycles() {
        val id = athleteId ?: return
        if (_state.value.user?.profile?.gender != "female") return
        viewModelScope.launch {
            try {
                val rows = api.menstrualCycles(id)
                _state.value = _state.value.copy(menstrualCycles = rows)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun addMenstrualCycle(start: String, end: String?, length: Int?, notes: String?) {
        val id = athleteId ?: return
        viewModelScope.launch {
            try {
                api.addMenstrualCycle(
                    id,
                    MenstrualCycleRequest(start, end, length, notes)
                )
                loadMenstrualCycles()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun logout() {
        _state.value = UiState()
    }
}
