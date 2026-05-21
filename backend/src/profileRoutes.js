import { query } from "./db.js";

export const PROFILE_FROM = `
  FROM athlete_profiles ap
  LEFT JOIN organizations o ON o.id = ap.organization_id
`;

export const PROFILE_SELECT = `
  ap.id, ap.first_name, ap.last_name, ap.full_name, ap.sport, ap.qualification,
  ap.residence_address, ap.phone, ap.coach_full_name, ap.coach_phone, ap.institution,
  ap.age_years, ap.gender, ap.height_cm, ap.weight_kg, ap.chest_cm,
  o.name AS organization_name
`;

export function splitFullName(fullName) {
  const parts = String(fullName || "")
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  if (!parts.length) return { first_name: "", last_name: "" };
  if (parts.length === 1) return { first_name: parts[0], last_name: parts[0] };
  return { first_name: parts[0], last_name: parts.slice(1).join(" ") };
}

export function mapProfile(row) {
  if (!row) return null;
  const fullName =
    (row.full_name && String(row.full_name).trim()) ||
    [row.first_name, row.last_name].filter(Boolean).join(" ").trim();
  return {
    id: row.id,
    full_name: fullName,
    first_name: row.first_name,
    last_name: row.last_name,
    sport: row.sport,
    qualification: row.qualification,
    residence_address: row.residence_address,
    phone: row.phone,
    coach_full_name: row.coach_full_name,
    coach_phone: row.coach_phone,
    institution: row.institution || row.organization_name || null,
    age_years: row.age_years,
    gender: row.gender,
    height_cm: row.height_cm != null ? Number(row.height_cm) : null,
    weight_kg: row.weight_kg != null ? Number(row.weight_kg) : null,
    chest_cm: row.chest_cm != null ? Number(row.chest_cm) : null,
  };
}

export function registerProfileRoutes(app) {
  app.get("/api/athletes/:id/profile", async (req, res) => {
    try {
      const { rows } = await query(
        `SELECT ${PROFILE_SELECT} ${PROFILE_FROM} WHERE ap.id = $1`,
        [req.params.id]
      );
      if (!rows.length) return res.status(404).json({ error: "profile not found" });
      res.json({ profile: mapProfile(rows[0]) });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.patch("/api/athletes/:id/profile", async (req, res) => {
    const athleteId = req.params.id;
    const body = req.body || {};

    const fullName = (body.full_name ?? "").trim();
    if (!fullName) {
      return res.status(400).json({ error: "full_name required" });
    }

    const { first_name, last_name } = splitFullName(fullName);

    try {
      const { rows: existing } = await query(
        `SELECT id FROM athlete_profiles WHERE id = $1`,
        [athleteId]
      );
      if (!existing.length) return res.status(404).json({ error: "profile not found" });

      const { rows } = await query(
        `UPDATE athlete_profiles SET
           full_name = $2,
           first_name = $3,
           last_name = $4,
           sport = $5,
           qualification = $6,
           residence_address = $7,
           phone = $8,
           coach_full_name = $9,
           coach_phone = $10,
           institution = $11
         WHERE id = $1
         RETURNING id`,
        [
          athleteId,
          fullName,
          first_name,
          last_name,
          body.sport?.trim() || null,
          body.qualification?.trim() || null,
          body.residence_address?.trim() || null,
          body.phone?.trim() || null,
          body.coach_full_name?.trim() || null,
          body.coach_phone?.trim() || null,
          body.institution?.trim() || null,
        ]
      );

      const { rows: updated } = await query(
        `SELECT ${PROFILE_SELECT} ${PROFILE_FROM} WHERE ap.id = $1`,
        [rows[0].id]
      );
      res.json({ profile: mapProfile(updated[0]) });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });
}
