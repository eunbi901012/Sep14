#!/usr/bin/env node
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative } from "node:path";

const root = process.cwd();
const evidencePath = join(root, "docs/verification/phase6-evidence.json");
const failures = [];

function fail(message) {
  failures.push(message);
}

function read(path) {
  return readFileSync(join(root, path), "utf8");
}

function assert(condition, message) {
  if (!condition) fail(message);
}

function walk(dir, out = []) {
  const absolute = join(root, dir);
  if (!existsSync(absolute)) return out;
  for (const entry of readdirSync(absolute)) {
    const path = join(absolute, entry);
    const rel = relative(root, path);
    if (
      [
        "node_modules",
        "dist",
        "build",
        "target",
        ".git",
        ".vite",
        ".cache",
      ].includes(entry)
    )
      continue;
    if (statSync(path).isDirectory()) walk(rel, out);
    else out.push(rel);
  }
  return out;
}

assert(
  existsSync(evidencePath),
  "docs/verification/phase6-evidence.json is required",
);
const evidence = JSON.parse(readFileSync(evidencePath, "utf8"));

assert(
  JSON.stringify(evidence.tasks.map((task) => task.id)) ===
    JSON.stringify(["T018", "T019", "T020"]),
  "Phase 6 task evidence must cover T018, T019, T020 in order",
);
assert(
  JSON.stringify(evidence.t018.requirements) ===
    JSON.stringify(["REQ-253", "REQ-255", "REQ-259", "REQ-261"]),
  "T018 must map to REQ-253/255/259/261",
);

const traceRows = evidence.t019.traceability ?? [];
for (const row of traceRows) {
  assert(
    /^REQ-\d{3}$/.test(row.canonical_id ?? ""),
    `invalid canonical_id: ${row.canonical_id}`,
  );
  assert(
    typeof row.source_id === "string" && row.source_id.trim().length > 0,
    `source_id missing for ${row.canonical_id}`,
  );
  assert(
    typeof row.artifact === "string" && existsSync(join(root, row.artifact)),
    `trace artifact missing for ${row.canonical_id}: ${row.artifact}`,
  );
}
for (const required of [
  "REQ-253",
  "REQ-255",
  "REQ-259",
  "REQ-261",
  "REQ-418",
  "REQ-419",
]) {
  assert(
    traceRows.some((row) => row.canonical_id === required),
    `traceability missing ${required}`,
  );
}

const accounting = evidence.t020.requirementAccounting;
const expected = Array.from(
  { length: accounting.last - accounting.first + 1 },
  (_, index) => `REQ-${String(accounting.first + index).padStart(3, "0")}`,
);
assert(
  accounting.coverage === "REQ-211..REQ-427",
  "Requirement Accounting coverage label must be REQ-211..REQ-427",
);
assert(
  expected.length === accounting.expectedCount,
  "Requirement Accounting expected count mismatch",
);
const accounted = accounting.accounted ?? [];
const missing = expected.filter((id) => !accounted.includes(id));
const unexpected = accounted.filter((id) => !expected.includes(id));
assert(
  JSON.stringify(accounted) === JSON.stringify(expected),
  "Requirement Accounting accounted ID list must exactly match REQ-211..REQ-427",
);
assert(
  missing.length === 0,
  `Requirement Accounting missing IDs: ${missing.join(", ")}`,
);
assert(
  unexpected.length === 0,
  `Requirement Accounting unexpected IDs: ${unexpected.join(", ")}`,
);
assert(
  Array.isArray(accounting.missing) && accounting.missing.length === 0,
  "Requirement Accounting must report no missing IDs",
);

const frontendMain = read("frontend/src/main.tsx");
const frontendFiles = walk("frontend/src").filter((path) =>
  /\.(ts|tsx|css|html)$/.test(path),
);
const backendFiles = walk("backend/src/main/java").filter((path) =>
  path.endsWith(".java"),
);
const allSourceText = [...frontendFiles, ...backendFiles]
  .map((path) => `\n--- ${path} ---\n${read(path)}`)
  .join("\n");

assert(
  !/http:\/\/localhost|https?:\/\/127\.0\.0\.1|https?:\/\/[^\s"'`]+\/api\//.test(
    frontendMain,
  ),
  "frontend API calls must use relative /api paths only",
);
assert(
  !/localStorage|sessionStorage|dangerouslySetInnerHTML|\beval\s*\(/.test(
    allSourceText,
  ),
  "unsafe browser storage or DOM execution pattern detected",
);
assert(
  !/console\.(log|debug|trace)|printStackTrace\s*\(/.test(allSourceText),
  "debug logging or stack-trace leakage pattern detected",
);
assert(
  !/password\s*[:=]\s*["'][^"']{4,}["']|secret\s*[:=]\s*["'][^"']+["']|token\s*[:=]\s*["'][^"']+["']/i.test(
    allSourceText,
  ),
  "credential-like literal detected in source",
);

assert(
  frontendMain.includes('aria-label="관리 메뉴"'),
  "navigation must expose an accessible aria-label",
);
assert(
  frontendMain.includes('role="dialog"') &&
    frontendMain.includes('aria-modal="true"') &&
    frontendMain.includes("aria-labelledby="),
  "modal accessibility attributes are required",
);
assert(
  frontendMain.includes("field.required") &&
    frontendMain.includes("<em> *</em>"),
  "required fields must be visibly marked",
);
assert(
  frontendMain.includes('defaultValue = "20"') ||
    read(
      "backend/src/main/java/kr/ac/knue/faculty/common/api/AdminController.java",
    ).includes('defaultValue = "20"'),
  "list APIs must default to 20 rows",
);

for (const path of [
  ".gitignore",
  "frontend/.dockerignore",
  "backend/.dockerignore",
]) {
  const text = read(path);
  for (const required of path === ".gitignore"
    ? [
        "node_modules/",
        ".next/",
        "dist/",
        "build/",
        "coverage/",
        ".vite/",
        ".cache/",
        "*.log",
        "*.local",
      ]
    : ["node_modules", ".next", "dist", "build", "coverage", ".git"]) {
    assert(text.includes(required), `${path} missing ${required}`);
  }
}

if (failures.length) {
  console.error("Phase 6 verification failed:");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log(
  `Phase 6 verification passed: ${evidence.tasks.map((task) => task.id).join(", ")} with ${expected.length} accounted requirements.`,
);
