import bcrypt from "bcrypt";
import { query } from "./db.js";
import { seedActivityForAthletes } from "./seedSampleData.js";

const ROLE_ATHLETE = "11111111-1111-1111-1111-111111111103";
const ORG_ID = "22222222-2222-2222-2222-222222222201";
const COACH_PROFILE_ID = "44444444-4444-4444-4444-444444444401";
const COACH_FULL_NAME = "Смирнов Алексей Петрович";
const COACH_PHONE = "+7 (999) 444-55-66";
const INSTITUTION = "СШОР №1";
const PASSWORD = "test123";
export const SLAVE_COUNT = 56;
const ACTIVITY_DAYS = 14;

const FIRST_NAMES_M = [
  "Алексей", "Дмитрий", "Иван", "Сергей", "Андрей", "Никита", "Максим", "Артём",
  "Кирилл", "Павел", "Роман", "Егор", "Владимир", "Олег", "Илья", "Тимур",
  "Даниил", "Михаил", "Глеб", "Степан", "Фёдор", "Ярослав", "Борис", "Григорий", "Лев",
  "Виктор", "Станислав", "Константин", "Вадим",
];

const FIRST_NAMES_F = [
  "Анна", "Мария", "Елена", "Ольга", "Дарья", "Полина", "Виктория", "Алина",
  "Ксения", "София", "Екатерина", "Наталья", "Юлия", "Валерия", "Арина", "Диана",
  "Кристина", "Вероника", "Милана", "Алиса", "Татьяна", "Ирина", "Людмила", "Светлана", "Зоя",
  "Вера", "Надежда", "Лариса", "Галина",
];

const LAST_NAMES = [
  "Иванов", "Петров", "Сидоров", "Козлов", "Новиков", "Морозов", "Волков", "Соколов",
  "Лебедев", "Кузнецов", "Попов", "Васильев", "Смирнов", "Михайлов", "Фёдоров", "Андреев",
  "Алексеев", "Романов", "Орлов", "Зайцев", "Соловьёв", "Борисов", "Яковлев", "Григорьев", "Павлов",
  "Семёнов", "Голубев", "Виноградов", "Богданов", "Воробьёв", "Фролов", "Макаров", "Николаев",
  "Осипов", "Степанов", "Титов", "Фомин", "Чернов", "Шубин", "Егоров", "Данилов",
  "Киселёв", "Медведев", "Беляев", "Комаров", "Жуков", "Крылов", "Тарасов", "Белов", "Марков",
  "Ковалёв", "Лазарев", "Мельников", "Рыбаков", "Сафонов", "Устинов", "Филиппов",
];

const PATRONYMICS_M = [
  "Сергеевич", "Андреевич", "Иванович", "Дмитриевич", "Алексеевич", "Николаевич",
  "Павлович", "Владимирович", "Олегович", "Романович", "Михайлович", "Егорович",
  "Артёмович", "Викторович",
];

const PATRONYMICS_F = [
  "Сергеевна", "Андреевна", "Ивановна", "Дмитриевна", "Алексеевна", "Николаевна",
  "Павловна", "Владимировна", "Олеговна", "Романовна", "Михайловна", "Егоровна",
  "Артёмовна", "Викторовна",
];

const SPORTS = [
  "лёгкая атлетика", "плавание", "гимнастика", "дзюдо", "бокс", "велоспорт",
  "лыжные гонки", "футбол", "хоккей", "теннис",
];

const QUALIFICATIONS = [
  "без разряда", "3 разряд", "2 разряд", "1 разряд", "КМС", "МС",
];

const CITIES = [
  "Казань", "Москва", "Санкт-Петербург", "Нижний Новгород", "Самара",
  "Уфа", "Пермь", "Воронеж", "Ростов-на-Дону", "Краснодар",
];

function pad2(n) {
  return String(n).padStart(2, "0");
}

function uuidForUser(i) {
  return `a1000000-0000-4000-8000-${String(i).padStart(12, "0")}`;
}

function uuidForProfile(i) {
  return `a2000000-0000-4000-8000-${String(i).padStart(12, "0")}`;
}

export function buildSlave(i) {
  const female = i % 2 === 0;
  const gender = female ? "female" : "male";
  const firstName = female
    ? FIRST_NAMES_F[(i - 1) % FIRST_NAMES_F.length]
    : FIRST_NAMES_M[(i - 1) % FIRST_NAMES_M.length];
  const lastName = LAST_NAMES[(i - 1) % LAST_NAMES.length];
  const patronymic = female
    ? PATRONYMICS_F[(i - 1) % PATRONYMICS_F.length]
    : PATRONYMICS_M[(i - 1) % PATRONYMICS_M.length];
  const fullName = `${lastName} ${firstName} ${patronymic}`;
  const ageYears = 15 + ((i * 7) % 12);
  const birthYear = new Date().getFullYear() - ageYears;
  const birthMonth = ((i % 12) + 1).toString().padStart(2, "0");
  const birthDay = ((i % 27) + 1).toString().padStart(2, "0");
  const birthDate = `${birthYear}-${birthMonth}-${birthDay}`;
  const heightBase = female ? 158 : 172;
  const heightCm = heightBase + (i % 18);
  const weightKg = Math.round(((female ? 52 : 68) + (i % 25) + (i % 7) * 0.3) * 10) / 10;
  const chestCm = Math.round(((female ? 82 : 92) + (i % 14) + (i % 5) * 0.5) * 10) / 10;
  const city = CITIES[(i - 1) % CITIES.length];
  const streetNum = 5 + (i % 40);

  return {
    index: i,
    userId: uuidForUser(i),
    profileId: uuidForProfile(i),
    email: `slave-${pad2(i)}@test.local`,
    firstName,
    lastName,
    fullName,
    gender,
    ageYears,
    birthDate,
    heightCm,
    weightKg,
    chestCm,
    sport: SPORTS[(i - 1) % SPORTS.length],
    qualification: QUALIFICATIONS[(i - 1) % QUALIFICATIONS.length],
    residenceAddress: `г. ${city}, ул. Спортивная, ${streetNum}`,
    phone: `+7 (9${String(10 + (i % 89)).padStart(2, "0")}) ${String(100 + (i % 900)).padStart(3, "0")}-${String(10 + (i % 89)).padStart(2, "0")}-${String(10 + ((i * 3) % 89)).padStart(2, "0")}`,
    coachFullName: COACH_FULL_NAME,
    coachPhone: COACH_PHONE,
    institution: INSTITUTION,
  };
}

export async function getSlaveAthleteIds() {
  const { rows } = await query(
    `SELECT ap.id FROM athlete_profiles ap
     JOIN users u ON u.id = ap.user_id
     WHERE u.email LIKE 'slave-%@test.local'
     ORDER BY u.email`
  );
  return rows.map((r) => r.id);
}

async function upsertSlaveProfiles(slaves) {
  const { rows: coachRows } = await query(
    `SELECT id FROM coach_profiles WHERE id = $1`,
    [COACH_PROFILE_ID]
  );
  if (!coachRows.length) {
    throw new Error(
      `Coach profile ${COACH_PROFILE_ID} not found. Run npm run seed first.`
    );
  }

  const passwordHash = await bcrypt.hash(PASSWORD, 10);

  for (const s of slaves) {
    await query(
      `INSERT INTO users (id, email, password_hash, role_id, is_active)
       VALUES ($1, $2, $3, $4, true)
       ON CONFLICT (id) DO UPDATE SET
         email = EXCLUDED.email,
         password_hash = EXCLUDED.password_hash,
         role_id = EXCLUDED.role_id,
         is_active = true`,
      [s.userId, s.email, passwordHash, ROLE_ATHLETE]
    );

    await query(
      `INSERT INTO athlete_profiles (
         id, user_id, first_name, last_name, full_name, birth_date, age_years, gender,
         height_cm, weight_kg, chest_cm, sport, qualification, residence_address, phone,
         coach_full_name, coach_phone, institution, organization_id, primary_coach_id
       ) VALUES (
         $1, $2, $3, $4, $5, $6::date, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20
       )
       ON CONFLICT (id) DO UPDATE SET
         user_id = EXCLUDED.user_id,
         first_name = EXCLUDED.first_name,
         last_name = EXCLUDED.last_name,
         full_name = EXCLUDED.full_name,
         birth_date = EXCLUDED.birth_date,
         age_years = EXCLUDED.age_years,
         gender = EXCLUDED.gender,
         height_cm = EXCLUDED.height_cm,
         weight_kg = EXCLUDED.weight_kg,
         chest_cm = EXCLUDED.chest_cm,
         sport = EXCLUDED.sport,
         qualification = EXCLUDED.qualification,
         residence_address = EXCLUDED.residence_address,
         phone = EXCLUDED.phone,
         coach_full_name = EXCLUDED.coach_full_name,
         coach_phone = EXCLUDED.coach_phone,
         institution = EXCLUDED.institution,
         organization_id = EXCLUDED.organization_id,
         primary_coach_id = EXCLUDED.primary_coach_id`,
      [
        s.profileId,
        s.userId,
        s.firstName,
        s.lastName,
        s.fullName,
        s.birthDate,
        s.ageYears,
        s.gender,
        s.heightCm,
        s.weightKg,
        s.chestCm,
        s.sport,
        s.qualification,
        s.residenceAddress,
        s.phone,
        s.coachFullName,
        s.coachPhone,
        s.institution,
        ORG_ID,
        COACH_PROFILE_ID,
      ]
    );

    await query(
      `INSERT INTO coach_athletes (coach_id, athlete_id)
       VALUES ($1, $2)
       ON CONFLICT (coach_id, athlete_id) DO NOTHING`,
      [COACH_PROFILE_ID, s.profileId]
    );

    if (s.gender === "female") {
      await query(`DELETE FROM menstrual_cycles WHERE athlete_id = $1`, [s.profileId]);
      await query(
        `INSERT INTO menstrual_cycles (athlete_id, cycle_start_date, cycle_end_date, cycle_length_days, notes)
         VALUES
           ($1, CURRENT_DATE - 28, CURRENT_DATE - 23, 28, 'Цикл 1'),
           ($1, CURRENT_DATE - 2, NULL, 28, 'Текущий цикл')`,
        [s.profileId]
      );
    }
  }
}

export async function seedSlaves({ withActivity = true, activityDays = ACTIVITY_DAYS } = {}) {
  const slaves = Array.from({ length: SLAVE_COUNT }, (_, idx) => buildSlave(idx + 1));
  await upsertSlaveProfiles(slaves);

  if (withActivity) {
    const ids = slaves.map((s) => s.profileId);
    await seedActivityForAthletes(ids, { iterations: activityDays, clearFirst: true });
  }

  const { rows } = await query(
    `SELECT COUNT(*)::int AS n FROM coach_athletes WHERE coach_id = $1`,
    [COACH_PROFILE_ID]
  );

  console.log(
    `[seedSlaves] Upserted ${SLAVE_COUNT} slave athletes (coach team total: ${rows[0].n})`
  );
  if (withActivity) {
    console.log(`[seedSlaves] Activity: ${activityDays} days per slave (trainings, nutrition, diary, metrics)`);
  }
  console.log(`[seedSlaves] Login: slave-01@test.local … slave-${pad2(SLAVE_COUNT)}@test.local / ${PASSWORD}`);

  return slaves;
}

/** @deprecated use seedSlaves */
export async function seed50Slaves() {
  return seedSlaves();
}
