import express from "express";
import cors from "cors";
import bcrypt from "bcrypt";
import { query } from "./db.js";
import { TEST_ACCOUNTS } from "./ensureTestData.js";
import { registerDiaryRoutes } from "./diaryRoutes.js";
import { registerNutritionRoutes } from "./nutritionRoutes.js";
import { registerCalendarRoutes } from "./calendarRoutes.js";
import { registerAuthRoutes } from "./authRoutes.js";
import { registerMenstrualRoutes } from "./menstrualRoutes.js";
import { registerProfileRoutes, mapProfile, PROFILE_SELECT, PROFILE_FROM } from "./profileRoutes.js";
import { registerCoachRoutes } from "./coachRoutes.js";
import { registerTrainingPlanRoutes } from "./trainingPlanRoutes.js";
import { registerAthleteTrainingRoutes } from "./athleteTrainingRoutes.js";
import { bootstrap } from "./bootstrap.js";

const app = express();

app.use(cors());
app.use(express.json());

app.use((req, _res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

app.use(async (req, res, next) => {
  if (req.path === "/health" && req.method === "GET") {
    return next();
  }
  try {
    await bootstrap();
    next();
  } catch (e) {
    console.error("[bootstrap] failed:", e.message);
    res.status(503).json({
      error: "database unavailable",
      detail: e.message,
    });
  }
});

registerDiaryRoutes(app);
registerNutritionRoutes(app);
registerCalendarRoutes(app);
registerAuthRoutes(app);
registerMenstrualRoutes(app);
registerProfileRoutes(app);
registerCoachRoutes(app);
registerTrainingPlanRoutes(app);
registerAthleteTrainingRoutes(app);

app.get("/health", async (_req, res) => {
  try {
    await bootstrap();
    res.json({ status: "ok" });
  } catch (e) {
    res.status(503).json({ status: "error", error: e.message });
  }
});

app.get("/api/debug/users", async (_req, res) => {
  try {
    const { rows } = await query(
      `SELECT u.email, r.name AS role, u.is_active
       FROM users u JOIN roles r ON r.id = u.role_id
       ORDER BY u.email`
    );
    res.json({ count: rows.length, users: rows });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

async function verifyPassword(email, password, passwordHash) {
  const testAcc = TEST_ACCOUNTS[email];
  if (testAcc && testAcc.password === password) return true;

  if (await bcrypt.compare(password, passwordHash)) return true;
  if (password === "test123" && (await bcrypt.compare("password", passwordHash))) {
    return "legacy";
  }
  return false;
}

app.post("/api/auth/login", async (req, res) => {
  const email = (req.body?.email || "").trim().toLowerCase();
  const password = req.body?.password || "";
  if (!email || !password) {
    return res.status(400).json({ error: "email and password required" });
  }
  try {
    const { rows } = await query(
      `SELECT u.id, u.email, u.password_hash, r.name AS role
       FROM users u JOIN roles r ON r.id = u.role_id
       WHERE u.email = $1 AND u.is_active = TRUE`,
      [email]
    );
    if (!rows.length) {
      console.log(`[login] user not found: ${email}`);
      return res.status(401).json({ error: "invalid credentials" });
    }
    const user = rows[0];
    const check = await verifyPassword(email, password, user.password_hash);
    if (!check) {
      console.log(`[login] wrong password for: ${email}`);
      return res.status(401).json({ error: "invalid credentials" });
    }
    if (check === "legacy") {
      const newHash = await bcrypt.hash("test123", 10);
      await query("UPDATE users SET password_hash = $1 WHERE id = $2", [newHash, user.id]);
      console.log(`[login] fixed legacy password hash for: ${email}`);
    }

    let profile = null;
    let coachProfile = null;
    if (user.role === "athlete") {
      const ap = await query(
        `SELECT ${PROFILE_SELECT} ${PROFILE_FROM} WHERE ap.user_id = $1`,
        [user.id]
      );
      profile = ap.rows[0] ? mapProfile(ap.rows[0]) : null;
    } else if (user.role === "coach") {
      const cp = await query(
        `SELECT id, specialization, experience_years FROM coach_profiles WHERE user_id = $1`,
        [user.id]
      );
      coachProfile = cp.rows[0] || null;
    }

    res.json({
      token: `test-${user.id}`,
      user: { id: user.id, email: user.email, role: user.role, profile, coach_profile: coachProfile },
    });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: "server error" });
  }
});

app.get("/api/athletes/:id/dashboard", async (req, res) => {
  const athleteId = req.params.id;
  try {
    const [profile, metrics, daily, training, analytics, water] = await Promise.all([
      query(
        `SELECT first_name, last_name, sport, weight_kg FROM athlete_profiles WHERE id = $1`,
        [athleteId]
      ),
      query(
        `SELECT mt.name, mt.unit, am.value, am.measured_at
         FROM athlete_metrics am
         JOIN metric_types mt ON mt.id = am.metric_type_id
         WHERE am.athlete_id = $1
         ORDER BY am.measured_at DESC LIMIT 20`,
        [athleteId]
      ),
      query(
        `SELECT report_date, sleep_time, wake_time, notes FROM daily_reports
         WHERE athlete_id = $1 ORDER BY report_date DESC LIMIT 7`,
        [athleteId]
      ),
      query(
        `SELECT id, date, duration_minutes, volume, intensity, readiness_score, fatigue_score
         FROM trainings WHERE athlete_id = $1 ORDER BY date DESC LIMIT 5`,
        [athleteId]
      ),
      query(
        `SELECT readiness_score, recovery_score, fatigue_score, injury_risk_score, generated_at
         FROM analytics_snapshots WHERE athlete_id = $1 ORDER BY generated_at DESC LIMIT 1`,
        [athleteId]
      ),
      query(
        `SELECT SUM(amount_ml) AS total_ml FROM water_intake
         WHERE athlete_id = $1 AND recorded_at::date = CURRENT_DATE`,
        [athleteId]
      ),
    ]);

    res.json({
      profile: profile.rows[0],
      recentMetrics: metrics.rows,
      dailyReports: daily.rows,
      trainings: training.rows,
      analytics: analytics.rows[0] || null,
      waterTodayMl: Number(water.rows[0]?.total_ml || 0),
    });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: "server error" });
  }
});

app.get("/api/metric-types", async (_req, res) => {
  try {
    const { rows } = await query(
      `SELECT mt.id, mt.name, mt.unit, mt.min_value, mt.max_value, mc.name AS category
       FROM metric_types mt
       JOIN metric_categories mc ON mc.id = mt.category_id
       ORDER BY mc.name, mt.name`
    );
    res.json(rows);
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: "server error" });
  }
});

app.post("/api/athletes/:id/metrics", async (req, res) => {
  const { metric_type_id, value, measured_at, notes } = req.body || {};
  if (!metric_type_id || value == null) {
    return res.status(400).json({ error: "metric_type_id and value required" });
  }
  try {
    const { rows } = await query(
      `INSERT INTO athlete_metrics (athlete_id, metric_type_id, value, measured_at, notes)
       VALUES ($1, $2, $3, COALESCE($4::timestamptz, NOW()), $5)
       RETURNING id, value, measured_at`,
      [req.params.id, metric_type_id, value, measured_at, notes]
    );
    res.status(201).json(rows[0]);
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: "server error" });
  }
});

app.post("/api/athletes/:id/daily-reports", async (req, res) => {
  const { report_date, sleep_time, wake_time, notes } = req.body || {};
  try {
    const { rows } = await query(
      `INSERT INTO daily_reports (athlete_id, report_date, sleep_time, wake_time, notes)
       VALUES ($1, COALESCE($2::date, CURRENT_DATE), $3, $4, $5)
       ON CONFLICT (athlete_id, report_date) DO UPDATE SET
         sleep_time = EXCLUDED.sleep_time,
         wake_time = EXCLUDED.wake_time,
         notes = EXCLUDED.notes
       RETURNING *`,
      [req.params.id, report_date, sleep_time, wake_time, notes]
    );
    res.status(201).json(rows[0]);
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: "server error" });
  }
});

app.get("/api/athletes/:id/nutrition", async (req, res) => {
  try {
    const { rows } = await query(
      `SELECT nl.id, nl.meal_type, nl.consumed_at,
              json_agg(json_build_object(
                'product_name', ni.product_name,
                'grams', ni.grams,
                'calories', ni.calories
              )) AS items
       FROM nutrition_logs nl
       LEFT JOIN nutrition_items ni ON ni.nutrition_log_id = nl.id
       WHERE nl.athlete_id = $1
       GROUP BY nl.id ORDER BY nl.consumed_at DESC LIMIT 10`,
      [req.params.id]
    );
    res.json(rows);
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: "server error" });
  }
});

export default app;
