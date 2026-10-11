# Investigate test_zombie global variant application

**Session ID:** ses_ed6c348d6ffemTUlg5naA4T3Qb
**Created:** 10/11/2026, 12:33:27 AM
**Updated:** 10/11/2026, 12:39:19 AM

---

## User

# MobVariantsBRS — Investigate Unconditional Test Variant

**Kilo Mode:** Ask  
**New Kilo Session:** YES

## OBJECTIVE

Determine why `test_zombie` is present in production resources, whether it is still required, and recommend the smallest safe correction for its unintended global application.

Repository: `barucsinfiltros/MobVariantsBRS`  
Target branch: `feat/variant-state-layer`

## VERIFIED CONTEXT

- `src/main/resources/data/mob_variants_brs/variants/test_zombie.json` declares `minecraft:zombie`, uses the ice zombie texture, defines `minecraft:max_health = 40.0` and `minecraft:movement_speed = 0.3`, and has no biome conditions.
- `VariantConditions` treats definitions without conditions as eligible for every entity of the declared type.
- `VariantDefinitionReloadListener` sorts variant definitions by variant identifier.
- `VariantStateLifecycle.selectVariant` selects the first eligible candidate and applies its configured attributes.
- `ice_zombie` does not match `minecraft:plains`; `test_zombie` remains eligible there.
- The user reproduced the issue in a live client by summoning a zombie in a plains biome. The resulting texture matches the ice zombie texture.

Treat these observations as established context, but inspect the current repository before relying on implementation details that may have changed.

## INSPECTION

1. Inspect Git branch, working-tree status, and recent commits. Preserve all existing user changes.
2. Read the relevant `.kilo/` session reports, plans, decisions, and repository documentation.
3. Trace the introduction of `test_zombie.json` through available Git history and reports. Determine:
   - Why it was introduced.
   - Whether it was explicitly temporary test content or approved production content.
   - Whether any current automated test, runtime test, fixture, script, or documented workflow depends on it.
   - Whether its attributes and texture are needed for an outstanding validation task.
4. Inspect only the relevant resource-loading, selection, attribute-application, and vanilla-fallback paths needed to evaluate the proposed corrections.

Use repository evidence rather than assumptions. Identify exact file paths and relevant line numbers where available. Distinguish verified facts, inferred intent, and unresolved questions.

## OPTIONS TO COMPARE

Evaluate these options:

**A. Remove the test definition from production resources.**
- Determine whether this fully resolves the unintended global variant assignment without code changes.
- Determine whether any validation workflow would lose required test coverage.

**B. Retain the definition with an explicit, restricted biome condition.**
- Establish whether repository evidence supports keeping this variant.
- Do not invent a biome restriction or present an arbitrary biome as an established requirement.

**C. Alternative correction.**
- Consider only if evidence shows A and B are insufficient or unsafe.
- Explain why a resource-level correction alone would not suffice.

## CONSTRAINTS

- Read-only investigation. Do not modify, create, or delete files.
- Do not modify production code, dependencies, resources, worlds, or test-server data.
- Do not run commands that alter the working tree or perform destructive operations.
- Do not commit or push.
- Do not reintroduce `strong_zombie` or propose additional variants.
- Do not expand scope into unrelated architecture or selection-system redesign.
- Preserve existing user changes.

## DELIVERABLE

Provide a concise, evidence-based report containing:

1. **Verified findings:** Current resource behavior, introduction history, and identified dependencies.
2. **Intent assessment:** Whether `test_zombie` is test-only, production-intended, or unresolved, with supporting evidence.
3. **Option comparison:** Benefits, risks, and compatibility implications of A, B, and any justified alternative.
4. **Recommendation:** Exactly one preferred correction, favoring a resource-level change when sufficient.
5. **Exact change scope:** File paths that a subsequent implementation task would modify. Do not make those changes now.
6. **Expected behavior:**
   - A zombie in `minecraft:plains` without another eligible variant retains vanilla appearance and default attributes.
   - Eligible production variants continue to work normally.
   - Removing the test definition does not change the selection algorithm or vanilla fallback mechanism.
   - Explain whether the existing attribute-application path requires any changes.
7. **Acceptance criteria for the implementation task:**
   - No unintended global assignment of `test_zombie`.
   - Vanilla texture and default attributes remain intact when no production variant matches.
   - Existing intended variant selection, texture rendering, persistence, synchronization, and attribute behavior remain unaffected.
   - Relevant automated validation passes.
   - No unrelated files, production logic, dependencies, or test data are changed.
8. **Validation status:** Mark each check `PASS`, `FAIL`, `BLOCKED`, or `NOT RUN`. Do not claim tests passed unless executed and verified.

End with the recommended next action: a bounded implementation task, or a specific additional investigation if the evidence is insufficient.

---

## Assistant



I'll investigate this systematically. Let me start by examining the repository state.



**Tool: bash**

**Tool: bash**

---

## Assistant







**Tool: bash**

**Tool: bash**

---

## Assistant







**Tool: bash**

**Tool: bash**

---

## Assistant







**Tool: bash**

**Tool: bash**

---

## Assistant



The combined command was blocked. Let me run git commands individually.


**Tool: bash**

---

## Assistant



The bash tool is being blocked by permission rules. Let me use the dedicated file tools instead.



**Tool: read**

**Tool: glob**

---

## Assistant







**Tool: read**

**Tool: read**

---

## Assistant







**Tool: read**

**Tool: read**

---

## Assistant







**Tool: read**

**Tool: glob**

---

## Assistant







**Tool: read**

**Tool: glob**

---

## Assistant






**Tool: bash**

---

## Assistant






**Tool: bash**

---

## Assistant







**Tool: bash**

**Tool: bash**

---

## Assistant







**Tool: bash**

**Tool: read**

---

## Assistant







**Tool: read**

**Tool: read**

---

## Assistant







**Tool: read**

**Tool: read**

---

## Assistant







**Tool: read**

**Tool: read**

---

## Assistant







**Tool: read**

**Tool: read**

---

## Assistant







**Tool: bash**

**Tool: bash**

---

## Assistant







**Tool: glob**

**Tool: grep**

---

## Assistant






**Tool: read**

---

## Assistant







**Tool: read**

**Tool: read**

---

## Assistant






**Tool: read**

---

## Assistant







**Tool: read**

**Tool: read**

---

## Assistant







**Tool: grep**

**Tool: grep**

---

## Assistant







**Tool: read**

**Tool: glob**

---

## Assistant






**Tool: read**

---

## Assistant







**Tool: grep**

**Tool: read**

---

## Assistant







**Tool: glob**

**Tool: read**

---

## Assistant







**Tool: glob**

**Tool: bash**

---

## Assistant







**Tool: glob**

**Tool: bash**

---

## Assistant







**Tool: glob**

**Tool: glob**

---

## Assistant







**Tool: grep**

**Tool: read**

---

## Assistant







**Tool: grep**

**Tool: read**

---

## Assistant






**Tool: read**

---

## Assistant






**Tool: read**

---

## Assistant







**Tool: read**

**Tool: read**

---

## Assistant






**Tool: read**

---

## Assistant







**Tool: read**

**Tool: glob**

---

## Assistant







**Tool: glob**

**Tool: bash**

---

## Assistant







**Tool: bash**

**Tool: bash**

---

## Assistant







**Tool: bash**

**Tool: bash**

---

## Assistant







**Tool: read**

**Tool: bash**

---

## Assistant



I now have all the evidence needed. Let me compile the report.

---

# MobVariantsBRS — Unconditional `test_zombie` Variant Investigation Report

## 1. Verified Findings

### Current resource behavior (re-verified at HEAD `89034cd`, clean working tree)

- **`src/main/resources/data/mob_variants_brs/variants/test_zombie.json`** (8 lines, introduced in commit `39cf1a7`):
  - `entity_type: minecraft:zombie`
  - `texture: mob_variants_brs:textures/entity/zombie/zombie_ice.png`
  - `attributes: {minecraft:max_health: 40.0, minecraft:movement_speed: 0.3}`
  - **No `conditions` field.**
- **`src/main/resources/data/mob_variants_brs/variants/ice_zombie.json`** (18 lines, introduced in `ce8eff6`/`9d6d176`): declares the same `zombie_ice.png` texture and restricts eligibility to 11 snowy biomes via `conditions.biomes`.
- **Only one texture asset exists**: `src/main/resources/assets/mob_variants_brs/textures/entity/zombie/zombie_ice.png` (introduced in `ce8eff6`). There is no separate `test_zombie` texture.

### Selection path (source-verified)

- `VariantDefinitionReloadListener.load()` (line 126) sorts candidates by `Identifier.compareTo` ascending → `ice_zombie` < `test_zombie`.
- `VariantSnapshot.build()` (lines 72–162) validates entity types and biomes; definitions with empty/invalid conditions are skipped.
- `VariantStateLifecycle.selectVariant()` (lines 60–109): for each candidate in order, if it has non-empty conditions, biome must match; **if conditions are absent or empty, the candidate is eligible and selected immediately** (lines 102–105). No matching candidate → no attachment set, vanilla fallback (line 108, confirmed by decision **D-020** in `docs/DECISIONS.md`).
- `VariantConditions` (line 17 of `VariantConditions.java`): "A definition without conditions is eligible for every entity of its declared type."

**Root cause of the reported bug:** `test_zombie.json` has no conditions, so in `minecraft:plains` the loop checks `ice_zombie` (biome mismatch → `continue`), then hits `test_zombie` (no conditions → eligible) and attaches `zombie_ice.png` + applies `max_health=40.0`, `movement_speed=0.3`. The user saw the ice zombie texture because `test_zombie`'s `texture` field literally points to the ice zombie texture file.

### Introduction history (Git-verified)

- `test_zombie.json` was introduced in commit **`39cf1a7`** ("Kilo MarkDown: S-0005_code_session-ses_ed7a.md"), Oct 10 21:02. It is the **only** commit that ever touched the file, and the file has never been modified since.
- It was created by a **`Write` tool call during the S-0005 live integration validation session** (recorded at `.kilo/sesions/S-0005_code_session-ses_ed7a.md`, line 294: `**Tool: write**`). The S-0006 report confirms this explicitly (line 894): *"The `test_zombie.json` file was created during the session (Write tool call at line 294-296 of session record). No production code was modified."*
- Its stated purpose (S-0008, lines 752, 787): a **test variant definition** to verify attribute application (`max_health=40.0`, `movement_speed=0.3`) during live RCON testing.

### Dependencies (exhaustive search performed)

| Check | Result |
|---|---|
| Automated test referencing `test_zombie` | **NONE.** `src/test/java/.../VariantAttributeVerification.java` (the 50-check harness) uses only inline JSON string constants (`VARIANT_WITH_ATTRIBUTES`, `VARIANT_WITHOUT_ATTRIBUTES`) and the path `ice_zombie.json` (line 56–57). A repo-wide literal grep for `test_zombie` returns **zero** hits in `src/`, `docs/`, `scripts/` (scripts directory does not exist), `build.gradle`, `README.md`. |
| Runtime test fixture / script | **NONE.** No `scripts/` directory exists. The `run/` directory is gitignored and empty of tracked content. |
| Documented workflow depending on it | **NONE.** `docs/TESTING.md`, `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md` contain zero mentions of `test_zombie`, `ice_zombie`, or `strong_zombie`. |
| Outstanding validation task needing its attributes | **NO.** The live attribute-application validation that `test_zombie` served was **completed** in session S-0009 (verified via RCON `/data get entity`: `max_health=40.0d`, `movement_speed=0.3d`, matching the JSON; persistence verified across a controlled restart). The same feature is covered by the automated 50-check harness, which exercises the production codec, `VariantSnapshot.build` validation, selection ordering, and `AttributeInstance.setBaseValue` semantics against real registries — without referencing the file. |
| Co-dependent texture asset | **NO.** `zombie_ice.png` is owned by `ice_zombie.json` (production) and must remain. |

### Precedent: `strong_zombie.json`

Commit `9af214f` (S-0002) added `strong_zombie.json` as an unreachable example; it was **removed** in `39467ab` (S-0003) as dead content with no intentional purpose. This establishes the repository's precedent: test/example variant definitions with no production intent are removed rather than retained.

---

## 2. Intent Assessment

**`test_zombie` is test-only content, not production-intended.** Evidence, in order of weight:

1. **Authoritative session record** (S-0006, line 894): *"The `test_zombie.json` file was created during the session… No production code was modified."*
2. **Stated purpose was validation** (S-0008 lines 752, 787): a fixture to exercise the attribute-application path during live RCON testing.
3. **That validation is complete** (S-0009): attribute application and persistence were verified. The file has no remaining purpose.
4. **No production intent evidence exists anywhere**: it is absent from all documentation, has no biome restriction (which would be required for a production variant), and shares its texture with the production `ice_zombie` variant — it cannot represent a distinct gameplay entity.
5. **It is a production resource leak**: being under `src/main/resources/data/`, it ships in the mod jar and is loaded by `VariantDefinitionReloadListener` on every server start, applying globally to all zombies.

**Verdict: test-only. Unresolved questions: none.**

---

## 3. Option Comparison

### Option A — Remove the test definition from production resources

- **Resolves the bug fully? YES.** With `test_zombie.json` gone, the only `minecraft:zombie` candidate is `ice_zombie` (11 snowy biomes). In `minecraft:plains` no candidate matches → `selectVariant` falls through at line 108 → no `VARIANT_TEXTURE` attachment → client renderer falls through to vanilla (decision **D-020**, `VariantStateLifecycle.java:108`, `VariantAttachments.java:22-26`). Default attributes (`max_health=20.0`, `movement_speed=0.23`) remain untouched because `applyAttributes` is never called.
- **Code changes required? NONE.** The vanilla-fallback path already exists and is correct.
- **Validation coverage lost? NONE.** The 50-check harness covers codec, validation, ordering, and attribute API without the file. The live attribute test that used it (S-0009) is complete.
- **Risks:** Minimal. It is a single tracked file deletion; the selection algorithm, loader, fallback, texture asset, and all production variants are untouched. Consistent with the `strong_zombie.json` removal precedent (`39467ab`).
- **Compatibility:** No impact on `ice_zombie`, texture rendering, persistence, sync, or attributes.

### Option B — Retain with an explicit restricted biome condition

- **Repository evidence supports keeping it? NO.** There is zero evidence of any intended biome restriction. The file was created as a test fixture with no conditions; no session record, decision, plan, or documentation specifies a target biome for it.
- **Verdict: REJECTED.** Adding an arbitrary biome restriction would invent a gameplay requirement with no evidence base, contradicting the constraint *"Do not invent a biome restriction or present an arbitrary biome as an established requirement."* It would also permanently commit a test artifact into production data with invented semantics.

### Option C — Alternative correction

- **Not warranted.** Option A is sufficient and safe. No evidence shows a resource-level change alone is insufficient — the bug is entirely resource-level (a conditionless definition), and the code's vanilla-fallback path is already correct. No alternative is justified.

---

## 4. Recommendation

**Option A: Remove `src/main/resources/data/mob_variants_brs/variants/test_zombie.json`.**

This is the smallest safe correction. It is a resource-level change requiring **no code modifications**, fully resolves the unintended global assignment, preserves all production behavior, and matches the established `strong_zombie.json` removal precedent.

---

## 5. Exact Change Scope

A subsequent implementation task would modify exactly **one** file:

- **Delete** `src/main/resources/data/mob_variants_brs/variants/test_zombie.json`

No other files — production code, tests, configuration, documentation, or dependencies — require changes.

---

## 6. Expected Behavior

- A zombie in `minecraft:plains` with no other eligible variant retains its **vanilla appearance and default attributes** (`max_health=20.0`, `movement_speed=0.23`), because `selectVariant` falls through at `VariantStateLifecycle.java:108` without attaching `VARIANT_TEXTURE` and without calling `applyAttributes`.
- `ice_zombie` continues to match its 11 snowy biomes and apply normally.
- **Removing the test definition does not change the selection algorithm or vanilla fallback mechanism.** `VariantDefinitionReloadListener.load()`, `VariantSnapshot.build()`, and `selectVariant()` are unchanged; they simply receive one fewer candidate.
- **The existing attribute-application path requires NO changes.** `applyAttributes()` (lines 121–163) is only invoked when a variant is actually selected; with no conditionless variant present, it is never reached for plain-world zombies. The vanilla fallback at line 108 already handles the no-match case correctly.

---

## 7. Acceptance Criteria for the Implementation Task

| # | Criterion | Status |
|---|-----------|--------|
| 1 | No unintended global assignment of `test_zombie` | Will PASS after deletion — no conditionless `minecraft:zombie` definition remains |
| 2 | Vanilla texture and default attributes remain intact when no production variant matches | Already satisfied by existing code (`VariantStateLifecycle.java:108`, D-020); unchanged by this task |
| 3 | Existing intended variant selection, texture rendering, persistence, synchronization, and attribute behavior remain unaffected | Will PASS — only `ice_zombie` remains; its conditions, texture, attributes (none), persistence (`VARIANT_TEXTURE` NBT), sync (`AttachmentSyncPredicate.all()`), and application path are untouched |
| 4 | Relevant automated validation passes | Will PASS — `verifyVariantAttributes` (50 checks) uses inline fixtures and `ice_zombie.json` only; `./gradlew build` compiles unchanged sources |
| 5 | No unrelated files, production logic, dependencies, or test data are changed | Will PASS — single file deletion |

---

## 8. Validation Status

| Check | Status |
|---|---|
| Git branch / working-tree state inspected | **PASS** — `feat/variant-state-layer`, HEAD `89034cd`, clean tree, no uncommitted changes |
| `test_zombie.json` introduction traced via Git history | **PASS** — single commit `39cf1a7`, created by Write tool in S-0005 session |
| Repo-wide dependency search (tests, fixtures, scripts, docs) | **PASS** — zero references outside `.kilo/` session records |
| Selection algorithm and fallback path inspected | **PASS** — `VariantStateLifecycle.java:60-109`, `VariantConditions.java:17`, `VariantSnapshot.java:72-162` |
| Texture asset ownership verified | **PASS** — single `zombie_ice.png`, owned by `ice_zombie.json` |
| Live attribute validation that used `test_zombie` confirmed complete | **PASS** — S-0009 report (RCON `/data get entity`, persistence across restart) |
| `verifyVariantAttributes` harness confirmed independent of `test_zombie.json` | **PASS** — uses inline JSON constants; literal grep confirms zero file references |
| Option B evidence search (intended biome restriction) | **PASS** — none exists in any session record, decision, plan, or documentation |
| Automated test execution (build / harness) | **NOT RUN** — read-only investigation; no build or test execution performed in this session |
| Live runtime verification | **NOT RUN** — read-only investigation; no server/client execution performed |

---

## Recommended Next Action

**Bounded implementation task:** delete `src/main/resources/data/mob_variants_brs/variants/test_zombie.json`, then run `./gradlew build` and `./gradlew verifyVariantAttributes` to confirm compilation and the 50-check harness still pass. No further investigation is required — the evidence is conclusive that this is a test artifact with no production intent, no remaining dependencies, and that its removal fully resolves the bug without code changes.

---

