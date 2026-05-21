import { PREPARATION_PERIODS } from "./trainingFields.js";

/**
 * Поля дневника = колонки Excel (одна строка на дату).
 */
export const DIARY_SECTIONS = [
  {
    id: "general",
    title: "Общее",
    fields: [
      { kind: "date", key: "report_date", label: "Дата" },
      { kind: "text", key: "notes", label: "Заметки / комментарий" },
    ],
  },
  {
    id: "period",
    title: "Этап подготовки",
    fields: [
      {
        kind: "training_period",
        key: "preparation_period",
        label: "Период (этап подготовки)",
        options: PREPARATION_PERIODS,
      },
    ],
  },
  {
    id: "morning",
    title: "Утро / перед тренировкой",
    fields: [
      { kind: "training", key: "desire_to_train", label: "Желание тренироваться (1–10)" },
      { kind: "training", key: "wellbeing_morning", label: "Самочувствие (1–10)" },
    ],
  },
  {
    id: "sleep",
    title: "Сон",
    fields: [
      { kind: "time", key: "wake_time", label: "Проснулся (время)" },
      { kind: "metric", name: "sleep_duration", label: "Сон (ч)" },
      { kind: "metric", name: "sleep_quality", label: "Качество сна" },
    ],
  },
  {
    id: "training_time",
    title: "Время тренировки",
    fields: [
      { kind: "time", key: "start_time", label: "Начало тренировки", training: true },
      { kind: "time", key: "end_time", label: "Окончание тренировки", training: true },
      { kind: "training", key: "duration_minutes", label: "Продолжительность всей тренировки (мин)" },
    ],
  },
  {
    id: "training_content",
    title: "Содержание тренировки",
    fields: [
      { kind: "training_text", key: "part_warmup", label: "Подготовительная часть" },
      { kind: "training_text", key: "part_main", label: "Основная часть (бег 100 м, подтягивания 10 раз…)" },
      { kind: "training_text", key: "part_cooldown", label: "Заключительная часть" },
    ],
  },
  {
    id: "hr_plan",
    title: "ЧСС по плану",
    fields: [
      { kind: "training", key: "planned_hr_before", label: "ЧСС по плану — до (покой)" },
      { kind: "training", key: "planned_hr_after", label: "ЧСС по плану — после (нагрузка)" },
    ],
  },
  {
    id: "hr_actual",
    title: "ЧСС фактически",
    fields: [
      { kind: "training", key: "actual_hr_before", label: "ЧСС факт — до (покой)" },
      { kind: "training", key: "actual_hr_after", label: "ЧСС факт — после (нагрузка)" },
    ],
  },
  {
    id: "training_load",
    title: "Нагрузка на тренировке",
    fields: [
      { kind: "training", key: "work_capacity", label: "Работоспособность на тренировке (1–10)" },
      { kind: "training", key: "fatigue_training", label: "Степень утомления на тренировке (1–10)" },
    ],
  },
  {
    id: "evening",
    title: "Вечер / итог дня",
    fields: [
      { kind: "daily", key: "daily_activity", label: "Активность в течение дня без тренировок (1–10)" },
      { kind: "daily", key: "wellbeing_evening", label: "Самочувствие вечером (1–10)" },
      { kind: "daily", key: "fatigue_daily", label: "Степень утомления за день (1–10)" },
      { kind: "daily_time", key: "bedtime", label: "Время отхода ко сну" },
      { kind: "time", key: "sleep_time", label: "Заснул (время)" },
    ],
  },
  {
    id: "wellness",
    title: "Доп. показатели",
    fields: [
      { kind: "metric", name: "motivation", label: "Мотивация (1–10)" },
      { kind: "metric", name: "fatigue", label: "Усталость (1–10)" },
      { kind: "metric", name: "mood", label: "Настроение (1–10)" },
      { kind: "metric", name: "resting_hr", label: "Пульс покоя" },
      { kind: "metric", name: "body_weight", label: "Масса тела (кг)" },
      { kind: "water", key: "water_ml", label: "Вода (мл)" },
    ],
  },
];

export const EXCEL_COLUMN_ALIASES = {
  дата: "report_date",
  период: "preparation_period",
  "этап подготовки": "preparation_period",
  "желание тренироваться": "desire_to_train",
  самочувствие: "wellbeing_morning",
  "самочувствие утром": "wellbeing_morning",
  "начало тренировки": "start_time",
  "окончание тренировки": "end_time",
  "продолжительность тренировки": "duration_minutes",
  "подготовительная часть": "part_warmup",
  "основная часть": "part_main",
  "заключительная часть": "part_cooldown",
  "чсс план до": "planned_hr_before",
  "чсс план после": "planned_hr_after",
  "чсс факт до": "actual_hr_before",
  "чсс факт после": "actual_hr_after",
  работоспособность: "work_capacity",
  "утомление на тренировке": "fatigue_training",
  "активность за день": "daily_activity",
  "самочувствие вечером": "wellbeing_evening",
  "утомление за день": "fatigue_daily",
  "отход ко сну": "bedtime",
  "сон (ч)": "sleep_duration",
  "качество сна": "sleep_quality",
  мотивация: "motivation",
  усталость: "fatigue",
  "пульс покоя": "resting_hr",
  вес: "body_weight",
  вода: "water_ml",
  заснул: "sleep_time",
  проснулся: "wake_time",
};

export function flattenDiaryFields() {
  return DIARY_SECTIONS.flatMap((s) =>
    s.fields.map((f) => ({ ...f, section: s.id, sectionTitle: s.title }))
  );
}
