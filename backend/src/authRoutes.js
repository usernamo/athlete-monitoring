import bcrypt from "bcrypt";
import { query } from "./db.js";

const ROLE_ATHLETE = "11111111-1111-1111-1111-111111111103";

function birthDateFromAge(age) {
  const y = new Date().getFullYear() - Number(age);
  return `${y}-01-01`;
}

export function registerAuthRoutes(app) {
  app.post("/api/auth/register", async (req, res) => {
    const {
      email,
      password,
      first_name,
      last_name,
      age,
      gender,
      height_cm,
      weight_kg,
      chest_cm,
    } = req.body || {};

    const em = (email || "").trim().toLowerCase();
    if (!em || !password || !first_name || !last_name || age == null || !gender) {
      return res.status(400).json({
        error: "email, password, first_name, last_name, age, gender required",
      });
    }

    const g = String(gender).toLowerCase();
    if (g !== "male" && g !== "female" && g !== "мужской" && g !== "женский") {
      return res.status(400).json({ error: "gender must be male or female" });
    }
    const genderNorm = g === "мужской" || g === "male" ? "male" : "female";

    try {
      const exists = await query(`SELECT id FROM users WHERE email = $1`, [em]);
      if (exists.rows.length) {
        return res.status(409).json({ error: "email already registered" });
      }

      const hash = await bcrypt.hash(password, 10);
      const { rows: userRows } = await query(
        `INSERT INTO users (email, password_hash, role_id)
         VALUES ($1, $2, $3) RETURNING id, email`,
        [em, hash, ROLE_ATHLETE]
      );
      const userId = userRows[0].id;
      const birthDate = birthDateFromAge(age);

      const { rows: profileRows } = await query(
        `INSERT INTO athlete_profiles (
           user_id, first_name, last_name, birth_date, age_years, gender,
           height_cm, weight_kg, chest_cm, sport
         ) VALUES ($1, $2, $3, $4::date, $5, $6, $7, $8, $9, 'общая подготовка')
         RETURNING id, first_name, last_name, age_years, gender, height_cm, weight_kg, chest_cm`,
        [
          userId,
          first_name.trim(),
          last_name.trim(),
          birthDate,
          Number(age),
          genderNorm,
          height_cm ?? null,
          weight_kg ?? null,
          chest_cm ?? null,
        ]
      );

      const profile = profileRows[0];
      res.status(201).json({
        token: `test-${userId}`,
        user: {
          id: userId,
          email: em,
          role: "athlete",
          profile: {
            id: profile.id,
            first_name: profile.first_name,
            last_name: profile.last_name,
            age_years: profile.age_years,
            gender: profile.gender,
            height_cm: profile.height_cm,
            weight_kg: profile.weight_kg,
            chest_cm: profile.chest_cm,
          },
        },
      });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });
}
