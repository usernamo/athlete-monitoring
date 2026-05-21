import bcrypt from "bcrypt";

const password = process.argv[2] || "test123";
const hash = await bcrypt.hash(password, 10);
console.log(`Password: ${password}`);
console.log(`Hash: ${hash}`);
