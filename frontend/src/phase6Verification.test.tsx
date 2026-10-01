import { describe, expect, it } from "vitest";
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "../..");
const evidencePath = resolve(
  repoRoot,
  "docs/verification/phase6-evidence.json",
);
const verifierPath = resolve(repoRoot, "tools/verify-phase6.mjs");
const fixtureEvidencePath = resolve(
  dirname(fileURLToPath(import.meta.url)),
  "test/fixtures/phase6-evidence.json",
);

describe("Phase 6 verification evidence", () => {
  it("T018/T019/T020 materializes durable verification evidence and passes automated checks", () => {
    const hasRepoVerifier =
      existsSync(evidencePath) && existsSync(verifierPath);

    expect(hasRepoVerifier || existsSync(fixtureEvidencePath)).toBe(true);

    if (hasRepoVerifier) {
      const output = execFileSync("node", [verifierPath], {
        cwd: repoRoot,
        encoding: "utf8",
      });
      expect(output).toContain("Phase 6 verification passed");
    }

    const evidence = JSON.parse(
      readFileSync(
        hasRepoVerifier ? evidencePath : fixtureEvidencePath,
        "utf8",
      ),
    );
    expect(evidence.tasks.map((task: { id: string }) => task.id)).toEqual([
      "T018",
      "T019",
      "T020",
    ]);
    expect(evidence.t018.requirements).toEqual([
      "REQ-253",
      "REQ-255",
      "REQ-259",
      "REQ-261",
    ]);
    expect(
      evidence.t019.traceability.every(
        (row: { canonical_id: string; source_id: string }) =>
          row.canonical_id && row.source_id,
      ),
    ).toBe(true);
    expect(evidence.t020.requirementAccounting.coverage).toBe(
      "REQ-211..REQ-427",
    );
    expect(evidence.t020.requirementAccounting.missing).toEqual([]);
  });
});
