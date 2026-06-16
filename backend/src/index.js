import dotenv from "dotenv";
import { connectionString, safeConnectionLabel } from "./db.js";
import app from "./app.js";
import { bootstrap } from "./bootstrap.js";

dotenv.config();

const PORT = process.env.PORT || 3000;
const HOST = process.env.HOST || "0.0.0.0";

async function start() {
  console.log("");
  console.log("Athlete Monitoring API — starting...");
  console.log(`Database: ${safeConnectionLabel(connectionString)}`);

  try {
    await bootstrap();
    console.log("Database: OK (connected)");
  } catch (e) {
    console.error("");
    console.error("Database: FAILED — API cannot work without PostgreSQL");
    console.error(`  Reason: ${e.message}`);
    console.error("");
    console.error("  Fix:");
    console.error("    docker compose up -d");
    console.error("  Then run npm start again.");
    console.error("");
    process.exit(1);
  }

  app.listen(PORT, HOST, () => {
    console.log("");
    console.log("Server is RUNNING (not frozen — waiting for HTTP requests)");
    console.log(`  Bind:   ${HOST}:${PORT}`);
    console.log(`  API:    http://localhost:${PORT}`);
    console.log(`  Health: http://localhost:${PORT}/health`);
    console.log(`  Login:  POST http://localhost:${PORT}/api/auth/login`);
    console.log("  Test athletes: athlete-male@test.local | athlete-female@test.local / test123");
    console.log("  Test coach (Master): coach@test.local / test123");
    console.log("");
    console.log("Press Ctrl+C to stop.");
    console.log("");
  });
}

start();
