package com.athlete.monitoring.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athlete.monitoring.ui.components.SportCard
import com.athlete.monitoring.ui.components.SportIconButton
import com.athlete.monitoring.ui.components.SportOutlinedField
import com.athlete.monitoring.ui.components.SportPrimaryButton
import com.athlete.monitoring.ui.components.SportSecondaryButton
import com.athlete.monitoring.ui.theme.SportColors
import com.athlete.monitoring.data.NutritionMealRequest
import com.athlete.monitoring.data.NutritionProductDto
import com.athlete.monitoring.data.NutritionTotalsDto
import java.time.LocalDate

data class ProductForm(
    val name: String = "",
    val grams: String = "",
    val ml: String = "",
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
    val calories: String = ""
)

data class MealForm(
    val mealNumber: String = "1",
    val appetite: Boolean = true,
    val time: String = "08:00",
    val isSnack: Boolean = false,
    val products: List<ProductForm> = listOf(ProductForm())
)

private fun calcTotals(meals: List<MealForm>): NutritionTotalsDto {
    var g = 0.0
    var ml = 0.0
    var p = 0.0
    var f = 0.0
    var c = 0.0
    var k = 0.0
    meals.forEach { meal ->
        meal.products.forEach { pr ->
            g += pr.grams.toDoubleOrNull() ?: 0.0
            ml += pr.ml.toDoubleOrNull() ?: 0.0
            p += pr.protein.toDoubleOrNull() ?: 0.0
            f += pr.fat.toDoubleOrNull() ?: 0.0
            c += pr.carbs.toDoubleOrNull() ?: 0.0
            k += pr.calories.toDoubleOrNull() ?: 0.0
        }
    }
    return NutritionTotalsDto(g, ml, p, f, c, k)
}

private fun mealsToRequest(meals: List<MealForm>): List<NutritionMealRequest> =
    meals.map { m ->
        NutritionMealRequest(
            meal_number = m.mealNumber.toIntOrNull(),
            meal_type = if (m.isSnack) "snack" else "meal",
            time = m.time,
            appetite = m.appetite,
            is_snack = m.isSnack,
            items = m.products
                .filter { it.name.isNotBlank() }
                .map { pr ->
                    NutritionProductDto(
                        product_name = pr.name,
                        grams = pr.grams.toDoubleOrNull(),
                        quantity_ml = pr.ml.toDoubleOrNull(),
                        protein = pr.protein.toDoubleOrNull(),
                        fat = pr.fat.toDoubleOrNull(),
                        carbs = pr.carbs.toDoubleOrNull(),
                        calories = pr.calories.toDoubleOrNull()
                    )
                }
        )
    }.filter { it.items.isNotEmpty() }

@Composable
fun NutritionDayScreen(
    state: UiState,
    vm: AppViewModel,
    date: String,
    onBack: () -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        SportIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack)
        Spacer(Modifier.height(8.dp))
        Text("Питание", style = MaterialTheme.typography.headlineLarge)
        Text(date, style = MaterialTheme.typography.bodyMedium, color = SportColors.TextSecondary)
        Spacer(Modifier.height(8.dp))
    }
    NutritionTab(state, vm, date, PaddingValues(0.dp))
}

@Composable
fun NutritionTab(state: UiState, vm: AppViewModel, date: String, padding: PaddingValues) {
    val meals = remember { mutableStateListOf(MealForm()) }
    var sports by remember { mutableStateOf("") }
    var pharma by remember { mutableStateOf("") }
    var water by remember { mutableStateOf("") }
    var formVersion by remember { mutableStateOf(0) }

    LaunchedEffect(date) {
        vm.loadNutritionDay(date)
    }

    LaunchedEffect(state.nutritionDay?.date) {
        if (state.nutritionDay?.date != date) return@LaunchedEffect
        val day = state.nutritionDay ?: return@LaunchedEffect
        sports = day.sports_nutrition.orEmpty()
        pharma = day.pharmacology.orEmpty()
        water = if (day.water_ml > 0) day.water_ml.toString() else ""
        meals.clear()
        if (day.meals.isEmpty()) {
            meals.add(MealForm())
        } else {
            day.meals.forEach { m ->
                meals.add(
                    MealForm(
                        mealNumber = (m.meal_number ?: 1).toString(),
                        appetite = m.appetite ?: true,
                        time = m.consumed_at?.let { if (it.length >= 16) it.substring(11, 16) else "12:00" } ?: "12:00",
                        isSnack = m.is_snack ?: false,
                        products = if (m.items.isEmpty()) {
                            listOf(ProductForm())
                        } else {
                            m.items.map { it ->
                                ProductForm(
                                    name = it.product_name,
                                    grams = it.grams?.toString() ?: "",
                                    ml = it.quantity_ml?.toString() ?: "",
                                    protein = it.protein?.toString() ?: "",
                                    fat = it.fat?.toString() ?: "",
                                    carbs = it.carbs?.toString() ?: "",
                                    calories = it.calories?.toString() ?: ""
                                )
                            }
                        }
                    )
                )
            }
        }
        formVersion++
    }

    val mealsSnapshot = remember(formVersion, meals.size) { meals.toList() }
    val totals = calcTotals(mealsSnapshot)

    fun updateMeal(index: Int, updated: MealForm) {
        meals[index] = updated
        formVersion++
    }

    val mealColors = listOf(SportColors.PastelPurple, SportColors.PastelGreen, SportColors.PastelBlue, SportColors.PastelPeach)

    LazyColumn(
        Modifier.padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = mealsSnapshot,
            key = { index, meal -> "meal-$index-${meal.mealNumber}-${meal.time}-${meal.appetite}" }
        ) { index, meal ->
            MealCard(
                meal = meal,
                backgroundColor = mealColors[index % mealColors.size],
                onMealChange = { updateMeal(index, it) },
                onRemove = {
                    if (meals.size > 1) {
                        meals.removeAt(index)
                        formVersion++
                    }
                }
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SportSecondaryButton("+ Приём", {
                    meals.add(MealForm(mealNumber = (meals.size + 1).toString()))
                    formVersion++
                }, Modifier.weight(1f))
                SportSecondaryButton("+ Перекус", {
                    meals.add(MealForm(mealNumber = "${meals.size + 1}", isSnack = true, time = "16:00"))
                    formVersion++
                }, Modifier.weight(1f))
            }
        }

        item {
            SportCard(backgroundColor = SportColors.PastelGreen) {
                Text("ИТОГО за сутки", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Белки: ${fmt(totals.protein)} г · Жиры: ${fmt(totals.fat)} г")
                Text("Углеводы: ${fmt(totals.carbs)} г · Ккал: ${fmt(totals.calories)}")
                Text("Масса: ${fmt(totals.grams)} г · Жидкость: ${fmt(totals.ml)} мл", style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            SportCard(backgroundColor = SportColors.Surface) {
                SportOutlinedField(sports, { sports = it }, "Спортивное питание")
                Spacer(Modifier.height(10.dp))
                SportOutlinedField(pharma, { pharma = it }, "Фармподдержка")
                Spacer(Modifier.height(10.dp))
                SportOutlinedField(water, { water = it }, "Вода, мл")
            }
        }
        item {
            SportPrimaryButton("Сохранить за день", {
                vm.saveNutritionDay(date, mealsToRequest(meals.toList()), sports, pharma, water)
            }, enabled = !state.loading)
            if (state.nutritionSaved) {
                Text("Сохранено", color = SportColors.AccentGreen, modifier = Modifier.padding(top = 6.dp))
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun MealCard(
    meal: MealForm,
    backgroundColor: androidx.compose.ui.graphics.Color,
    onMealChange: (MealForm) -> Unit,
    onRemove: () -> Unit
) {
    val title = if (meal.isSnack) "Перекус №${meal.mealNumber}" else "Приём пищи №${meal.mealNumber}"

    SportCard(backgroundColor = backgroundColor) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                SportSecondaryButton("Удалить", onRemove)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = meal.mealNumber,
                    onValueChange = { onMealChange(meal.copy(mealNumber = it)) },
                    label = { Text("№ приёма") },
                    modifier = Modifier.width(100.dp)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = meal.time,
                    onValueChange = { onMealChange(meal.copy(time = it)) },
                    label = { Text("Время") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Аппетит", modifier = Modifier.weight(1f))
                Switch(
                    checked = meal.appetite,
                    onCheckedChange = { onMealChange(meal.copy(appetite = it)) }
                )
                Text(
                    if (meal.appetite) "Был" else "Не было",
                    modifier = Modifier.width(72.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = meal.isSnack,
                    onCheckedChange = { onMealChange(meal.copy(isSnack = it)) }
                )
                Text("Это перекус")
            }

            HorizontalDivider()
            Text("Продукты", fontWeight = FontWeight.Medium)

            meal.products.forEachIndexed { pi, product ->
                ProductFields(
                    product = product,
                    onChange = { updated ->
                        val list = meal.products.toMutableList()
                        list[pi] = updated
                        onMealChange(meal.copy(products = list))
                    }
                )
                if (pi < meal.products.lastIndex) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
            }

            SportSecondaryButton("+ Продукт", {
                onMealChange(meal.copy(products = meal.products + ProductForm()))
            }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProductFields(
    product: ProductForm,
    onChange: (ProductForm) -> Unit
) {
    OutlinedTextField(
        value = product.name,
        onValueChange = { onChange(product.copy(name = it)) },
        label = { Text("Наименование") },
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = product.grams,
            onValueChange = { onChange(product.copy(grams = it)) },
            label = { Text("Граммы") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = product.ml,
            onValueChange = { onChange(product.copy(ml = it)) },
            label = { Text("мл") },
            modifier = Modifier.weight(1f)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = product.protein,
            onValueChange = { onChange(product.copy(protein = it)) },
            label = { Text("Белки") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = product.fat,
            onValueChange = { onChange(product.copy(fat = it)) },
            label = { Text("Жиры") },
            modifier = Modifier.weight(1f)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = product.carbs,
            onValueChange = { onChange(product.copy(carbs = it)) },
            label = { Text("Углев.") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = product.calories,
            onValueChange = { onChange(product.copy(calories = it)) },
            label = { Text("Ккал") },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
