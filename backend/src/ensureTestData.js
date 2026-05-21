import bcrypt from "bcrypt";
import { query } from "./db.js";
import { ensureMetricCatalog } from "./ensureMetricCatalog.js";
import { seedSampleData } from "./seedSampleData.js";

const ROLE_ATHLETE = "11111111-1111-1111-1111-111111111103";
const ROLE_COACH = "11111111-1111-1111-1111-111111111102";
const ORG_ID = "22222222-2222-2222-2222-222222222201";

export const TEST_ACCOUNTS = {
  "athlete-male@test.local": {
    password: "test123",
    userId: "33333333-3333-3333-3333-333333333301",
    roleId: ROLE_ATHLETE,
    profile: {
      id: "55555555-5555-5555-5555-555555555501",
      first_name: "Иван",
      last_name: "Петров",
      full_name: "Петров Иван Сергеевич",
      sport: "лёгкая атлетика",
      qualification: "КМС",
      residence_address: "г. Казань, ул. Баумана, 12",
      phone: "+7 (999) 111-22-33",
      coach_full_name: "Смирнов Алексей Петрович",
      coach_phone: "+7 (999) 444-55-66",
      institution: "СШОР №1",
      age_years: 19,
      gender: "male",
      height_cm: 178,
      weight_kg: 72.5,
      chest_cm: 95,
    },
  },
  "athlete-female@test.local": {
    password: "test123",
    userId: "33333333-3333-3333-3333-333333333302",
    roleId: ROLE_ATHLETE,
    profile: {
      id: "55555555-5555-5555-5555-555555555502",
      first_name: "Мария",
      last_name: "Сидорова",
      full_name: "Сидорова Мария Андреевна",
      sport: "лёгкая атлетика",
      qualification: "1 разряд",
      residence_address: "г. Казань, ул. Пушкина, 5",
      phone: "+7 (999) 222-33-44",
      coach_full_name: "Смирнов Алексей Петрович",
      coach_phone: "+7 (999) 444-55-66",
      institution: "СШОР №1",
      age_years: 18,
      gender: "female",
      height_cm: 168,
      weight_kg: 58,
      chest_cm: 88,
    },
  },
  "coach@test.local": {
    password: "test123",
    userId: "33333333-3333-3333-3333-333333333303",
    roleId: ROLE_COACH,
    coachProfileId: "44444444-4444-4444-4444-444444444401",
  },
};

export async function ensureTestData() {
  await query(`
    INSERT INTO roles (id, name) VALUES
      ('11111111-1111-1111-1111-111111111101', 'admin'),
      ('11111111-1111-1111-1111-111111111102', 'coach'),
      ('11111111-1111-1111-1111-111111111103', 'athlete'),
      ('11111111-1111-1111-1111-111111111104', 'doctor'),
      ('11111111-1111-1111-1111-111111111105', 'nutritionist')
    ON CONFLICT (name) DO NOTHING
  `);

  await query(
    `INSERT INTO organizations (id, name, type, country)
     VALUES ($1, 'СШОР №1', 'school', 'RU') ON CONFLICT (id) DO NOTHING`,
    [ORG_ID]
  );

  await query(`
    ALTER TABLE athlete_profiles
      ADD COLUMN IF NOT EXISTS age_years INTEGER,
      ADD COLUMN IF NOT EXISTS chest_cm NUMERIC(5, 2),
      ADD COLUMN IF NOT EXISTS full_name VARCHAR(255),
      ADD COLUMN IF NOT EXISTS residence_address TEXT,
      ADD COLUMN IF NOT EXISTS phone VARCHAR(50),
      ADD COLUMN IF NOT EXISTS coach_full_name VARCHAR(255),
      ADD COLUMN IF NOT EXISTS coach_phone VARCHAR(50),
      ADD COLUMN IF NOT EXISTS institution VARCHAR(255)
  `);
  await query(`
    CREATE UNIQUE INDEX IF NOT EXISTS uq_trainings_athlete_date_self
      ON trainings (athlete_id, date)
      WHERE coach_id IS NULL
  `);

  await query(`
    ALTER TABLE trainings
      ADD COLUMN IF NOT EXISTS preparation_period VARCHAR(30),
      ADD COLUMN IF NOT EXISTS desire_to_train SMALLINT,
      ADD COLUMN IF NOT EXISTS wellbeing_morning SMALLINT,
      ADD COLUMN IF NOT EXISTS part_warmup TEXT,
      ADD COLUMN IF NOT EXISTS part_main TEXT,
      ADD COLUMN IF NOT EXISTS part_cooldown TEXT,
      ADD COLUMN IF NOT EXISTS planned_hr_before INTEGER,
      ADD COLUMN IF NOT EXISTS planned_hr_after INTEGER,
      ADD COLUMN IF NOT EXISTS actual_hr_before INTEGER,
      ADD COLUMN IF NOT EXISTS actual_hr_after INTEGER,
      ADD COLUMN IF NOT EXISTS work_capacity SMALLINT,
      ADD COLUMN IF NOT EXISTS fatigue_training SMALLINT
  `);
  await query(`
    ALTER TABLE athlete_competitions
      ADD COLUMN IF NOT EXISTS outcome VARCHAR(10) DEFAULT 'pending'
  `);

  await query(`
    ALTER TABLE daily_reports
      ADD COLUMN IF NOT EXISTS daily_activity SMALLINT,
      ADD COLUMN IF NOT EXISTS wellbeing_evening SMALLINT,
      ADD COLUMN IF NOT EXISTS fatigue_daily SMALLINT,
      ADD COLUMN IF NOT EXISTS bedtime TIME
  `);

  await query(`
    CREATE TABLE IF NOT EXISTS training_plans (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
      coach_id UUID NOT NULL REFERENCES coach_profiles(id) ON DELETE CASCADE,
      title VARCHAR(255) NOT NULL DEFAULT 'План тренировок',
      notes TEXT,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE (athlete_id)
    )
  `);
  await query(`
    CREATE TABLE IF NOT EXISTS training_plan_items (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      plan_id UUID NOT NULL REFERENCES training_plans(id) ON DELETE CASCADE,
      scheduled_date DATE NOT NULL,
      title VARCHAR(255),
      preparation_period VARCHAR(30),
      part_warmup TEXT,
      part_main TEXT,
      part_cooldown TEXT,
      start_time TIME,
      end_time TIME,
      duration_minutes INTEGER,
      coach_notes TEXT,
      sort_order INTEGER NOT NULL DEFAULT 0
    )
  `);

  await query(`
    CREATE TABLE IF NOT EXISTS coach_recommendations (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      coach_id UUID NOT NULL REFERENCES coach_profiles(id) ON DELETE CASCADE,
      athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
      recommendation_text TEXT NOT NULL,
      category VARCHAR(50) NOT NULL DEFAULT 'general',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    )
  `);

  await query(`
    CREATE TABLE IF NOT EXISTS menstrual_cycles (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
      cycle_start_date DATE NOT NULL,
      cycle_end_date DATE,
      cycle_length_days INTEGER,
      notes TEXT,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    )
  `);

  const hash = await bcrypt.hash("test123", 10);
  const coach = TEST_ACCOUNTS["coach@test.local"];

  await query(
    `INSERT INTO users (id, email, password_hash, role_id, is_active)
     VALUES ($1, 'coach@test.local', $2, $3, true)
     ON CONFLICT (id) DO UPDATE SET
       email = EXCLUDED.email,
       password_hash = EXCLUDED.password_hash,
       role_id = EXCLUDED.role_id,
       is_active = true`,
    [coach.userId, hash, ROLE_COACH]
  );
  await query(
    `INSERT INTO coach_profiles (id, user_id, specialization, experience_years)
     VALUES ($1, $2, 'лёгкая атлетика', 12)
     ON CONFLICT (id) DO UPDATE SET
       user_id = EXCLUDED.user_id,
       specialization = EXCLUDED.specialization,
       experience_years = EXCLUDED.experience_years`,
    [coach.coachProfileId, coach.userId]
  );

  for (const email of ["athlete-male@test.local", "athlete-female@test.local"]) {
    const acc = TEST_ACCOUNTS[email];
    const p = acc.profile;

    await query(
      `INSERT INTO users (id, email, password_hash, role_id, is_active)
       VALUES ($1, $2, $3, $4, true)
       ON CONFLICT (id) DO UPDATE SET
         email = EXCLUDED.email,
         password_hash = EXCLUDED.password_hash,
         role_id = EXCLUDED.role_id,
         is_active = true`,
      [acc.userId, email, hash, acc.roleId]
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
         primary_coach_id = EXCLUDED.primary_coach_id`,
      [
        p.id,
        acc.userId,
        p.first_name,
        p.last_name,
        p.full_name ?? `${p.first_name} ${p.last_name}`,
        `${new Date().getFullYear() - p.age_years}-06-15`,
        p.age_years,
        p.gender,
        p.height_cm,
        p.weight_kg,
        p.chest_cm,
        p.sport ?? "лёгкая атлетика",
        p.qualification ?? null,
        p.residence_address ?? null,
        p.phone ?? null,
        p.coach_full_name ?? null,
        p.coach_phone ?? null,
        p.institution ?? "СШОР №1",
        ORG_ID,
        coach.coachProfileId,
      ]
    );

    await query(
      `INSERT INTO coach_athletes (coach_id, athlete_id)
       VALUES ($1, $2) ON CONFLICT (coach_id, athlete_id) DO NOTHING`,
      [coach.coachProfileId, p.id]
    );
  }

  const femaleId = TEST_ACCOUNTS["athlete-female@test.local"].profile.id;
  await query(`DELETE FROM menstrual_cycles WHERE athlete_id = $1`, [femaleId]);
  await query(
    `INSERT INTO menstrual_cycles (athlete_id, cycle_start_date, cycle_end_date, cycle_length_days, notes)
     VALUES
       ($1, CURRENT_DATE - 28, CURRENT_DATE - 23, 28, 'Цикл 1'),
       ($1, CURRENT_DATE - 2, NULL, 28, 'Текущий цикл')`,
    [femaleId]
  );

  await ensureMetricCatalog();

  if (process.env.RESEED_SAMPLE_DATA === "1") {
    await seedSampleData();
  } else {
    const { rows } = await query(
      `SELECT COUNT(*)::int AS n FROM daily_reports WHERE report_date >= CURRENT_DATE - 5`
    );
    if (rows[0].n === 0) {
      await seedSampleData();
    } else {
      console.log("[setup] Sample diary seed skipped (use RESEED_SAMPLE_DATA=1 to force)");
    }
  }

  const { rows } = await query(`SELECT email FROM users WHERE email LIKE 'athlete-%@test.local' OR email = 'coach@test.local'`);
  console.log(`[setup] Test users: ${rows.map((r) => r.email).join(", ")}`);
  console.log(`[setup] athlete-male@test.local | athlete-female@test.local | coach@test.local / test123`);
}
