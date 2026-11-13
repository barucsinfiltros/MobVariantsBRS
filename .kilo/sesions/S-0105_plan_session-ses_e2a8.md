# Cold zombie freezing plan correction

**Session ID:** ses_e2a8c7805ffe4CCv4Wg3qV03yl
**Created:** 11/13/2026, 11:08:00 AM
**Updated:** 11/13/2026, 12:24:47 PM

---

## User

# MOB VARIANTS BRS — CORRECT COLD ZOMBIE FREEZING IMPLEMENTATION PLAN

## KILO SESSION

- **Mode:** Plan
- **New Kilo Session:** YES
- **Repository:** https://github.com/barucsinfiltros/MobVariantsBRS
- **Target branch:** `feat/variant-state-layer`
- **Previous session:** S-0103 (`ses_e31f2a935ffeHAYNTGLvMtrRDE`)
- **Existing plan to correct:** `.kilo/plans/1794458343114-cold-zombie-freezing-mechanics-plan.md`
- **Previous research:** S-0103 session report and associated Minecraft 26.1.2 freezing research.

## OBJECTIVE

Correct the existing cold zombie freezing implementation plan.

The previous research contains useful verified information about Minecraft's native freezing mechanics. However, its proposed architecture introduces an independent freezing-duration attachment, which violates the approved gameplay design.

Produce a technically accurate, implementation-ready plan using exclusively Minecraft's native `ticksFrozen` state.

This is a READ-ONLY planning task. Do not implement anything.

## STEP 1 — INITIAL INSPECTION

1. Identify and verify the repository root and current Git branch.
2. Inspect Git status before proceeding.
3. Preserve all existing user modifications.
4. Read:
   - `.kilo/plans/1794458343114-cold-zombie-freezing-mechanics-plan.md`
   - The S-0103 session report and relevant research documents.
5. Locate the existing cold zombie implementation, variant identification mechanisms, attachment declarations, Mixin configuration, and relevant test infrastructure.
6. Reuse existing verified research instead of repeating investigations unnecessarily.

If the S-0103 report or any referenced document is unavailable, identify the limitation explicitly.

## STEP 2 — APPROVED GAMEPLAY ARCHITECTURE

The corrected plan MUST implement the following design.

### A. Cryogenic Body

Cold zombies must:

- Never accumulate freezing points from powder snow.
- Never receive vanilla periodic freezing damage.
- Continue receiving unrelated damage normally.
- Leave all other entities' freezing behavior unchanged.

Variant identification:

- Reuse `VariantAttachments.VARIANT_TEXTURE`.
- Do not introduce another attachment or identity mechanism.

Investigate whether a narrowly scoped `LivingEntity.canFreeze()` interception can achieve the required behavior.

Verify its actual Minecraft 26.1.2 implementation and call sites before recommending a Mixin.

### B. Freezing Touch

After a successful cold zombie melee attack against a `LivingEntity`:

1. Add exactly 40 points to the victim's native freezing state.
2. Apply Slowness I for 120 ticks.
3. Refresh Slowness duration when another successful hit occurs.
4. Never accumulate Slowness duration.
5. Execute all gameplay modifications server-side.

The intended native operation is:

`victim.setTicksFrozen(victim.getTicksFrozen() + 40)`

Evaluate overflow-safe arithmetic if justified by the actual integer behavior.

Do not introduce an arbitrary gameplay cap.

Investigate the existing `Mob.doHurtTarget(ServerLevel, Entity)` integration point and establish precisely how to ensure that the effect only occurs after a successful melee attack.

The implementation must not affect unsuccessful attacks, unrelated attack types, or other mob variants.

## STEP 3 — STRICT STATE RESTRICTIONS

The following are explicitly prohibited:

- `COLD_FREEZE_REMAINING_TICKS`.
- Any new freezing attachment.
- Independent freezing-duration counters.
- Timers or expiration timestamps.
- UUID-based state maps.
- Custom periodic freezing damage.
- Per-tick processing for this mechanic.
- Forcing `ticksFrozen` to remain at 140.
- Suppressing vanilla powder snow behavior for victims.

**Native `ticksFrozen` must remain the only freezing state.**

Do not reinterpret these restrictions or introduce alternative state storage to reproduce the rejected architecture.

## STEP 4 — PRESERVE VANILLA MECHANICS

The corrected plan must accurately document these native behaviors:

| Mechanic | Vanilla behavior |
|---|---|
| Full freezing threshold | 140 |
| Natural recovery outside powder snow | -2 points per tick |
| Powder snow accumulation | +1 point per tick |
| Powder snow maximum | 140 |
| Full freezing detection | `isFullyFrozen()` uses the native threshold |
| Periodic freezing damage | Depends on vanilla 40-tick interval and eligibility checks |
| Synchronization | Native `ticksFrozen` synchronization |
| Persistence | Native NBT persistence |

Important considerations:

- `setTicksFrozen()` can store values above 140.
- Powder snow can clamp the value back to 140 during its next accumulation update.
- This is accepted vanilla behavior.
- Do not introduce independent state to work around this limitation.
- Do not promise that every attack or every accumulation guarantees periodic freezing damage.
- Explain the interaction between native recovery, accumulation, damage intervals, and eligibility checks.

Preserve verified S-0103 findings. Correct them only when source inspection demonstrates an actual error.

## STEP 5 — REVIEW MIXINS AND INTEGRATION POINTS

Reassess every proposed Mixin.

For each candidate, document:

1. Target class and method.
2. Exact injection point.
3. Required behavior.
4. Why existing APIs are insufficient.
5. Side and execution constraints.
6. Compatibility risks.
7. Whether the Mixin is actually necessary.

In particular:

- Evaluate `LivingEntity.canFreeze()` for Cryogenic Body.
- Evaluate the existing melee attack integration point for Freezing Touch.
- Avoid unnecessary Mixins.
- Prefer existing APIs and abstractions when they satisfy the requirements.

Do not assume an injection point is valid without verifying Minecraft 26.1.2 sources and mappings.

## STEP 6 — CORRECT THE IMPLEMENTATION FILE LIST

Reassess the complete proposed implementation file list.

Identify only files that genuinely need modification.

Potential areas to inspect:

- Existing cold zombie entity implementation.
- Existing variant identity utilities.
- Existing melee attack implementation.
- Existing Mixin classes and configuration.
- Existing automated tests.

Do not automatically create new classes.

Explicitly remove:

- Freezing-duration attachment declarations.
- Attachment registration.
- Attachment serialization or synchronization.
- Countdown implementations.
- Per-tick freezing handlers.
- Unnecessary configuration changes.
- Any implementation scaffolding unrelated to the approved design.

Do not modify these files. Only document the proposed future changes.

## STEP 7 — UPDATE ACCEPTANCE CRITERIA

The corrected plan must include verifiable acceptance criteria.

At minimum:

### Cryogenic Body

- Cold zombies cannot accumulate freezing points from powder snow.
- Cold zombies cannot receive vanilla periodic freezing damage.
- Cold zombies remain vulnerable to unrelated damage.
- Other entities retain normal freezing behavior.
- Variant identification uses the existing `VARIANT_TEXTURE` identity.

### Freezing Touch

- A successful cold zombie melee attack adds exactly 40 to the victim's previous `ticksFrozen`.
- An unsuccessful attack adds nothing.
- Slowness I lasts 120 ticks.
- Repeated hits refresh the duration rather than accumulating it.
- Gameplay changes occur server-side.
- Other mob variants remain unaffected.

### Native State

- No independent freezing state exists.
- No freezing attachment is introduced.
- No custom freezing timer exists.
- No per-tick mechanic processing exists.
- Native recovery and powder snow interactions remain functional.
- Vanilla synchronization and persistence remain intact.

## STEP 8 — UPDATE VALIDATION SCENARIOS

Define automated and manual tests.

### Automated tests

Include scenarios covering:

1. Initial `ticksFrozen = 0`; successful hit results in 40.
2. Initial `ticksFrozen = 40`; successful hit results in 80.
3. Initial `ticksFrozen = 100`; successful hit results in 140.
4. Initial `ticksFrozen = 140`; successful hit results in 180, subject to native behavior.
5. Overflow-safe arithmetic at extreme integer values, if relevant.
6. Unsuccessful melee attack does not modify freezing state.
7. Slowness I is applied for 120 ticks.
8. Repeated hits refresh Slowness duration instead of accumulating it.
9. Natural recovery decreases freezing points by 2 per tick outside powder snow.
10. Powder snow increases freezing points by 1 per tick and clamps at 140.
11. Cold zombies do not accumulate freezing points from powder snow.
12. Cold zombies do not receive vanilla periodic freezing damage.
13. Unrelated damage still affects cold zombies.
14. Other entities retain vanilla freezing behavior.
15. Server-side execution is correctly enforced.

Adapt these scenarios to the actual existing testing infrastructure.

Do not invent test APIs or assume test classes exist.

### Manual validation

Include practical in-game scenarios for:

- Repeated attacks against a player.
- Attacking a victim already partially frozen.
- Attacking a fully frozen victim.
- Natural recovery after leaving powder snow.
- Powder snow interaction following an attack.
- Periodic freezing damage timing.
- Multiplayer server/client synchronization.
- World save and reload persistence.
- Comparison with a non-cold zombie variant.

Clearly distinguish expected native behavior from custom mechanic behavior.

## STEP 9 — KNOWN LIMITATIONS AND OPEN QUESTIONS

Document:

- Native freezing recovery can reduce accumulated points before subsequent periodic damage.
- Powder snow can clamp values above 140.
- Vanilla periodic damage is not guaranteed immediately after a successful attack.
- Native freezing damage depends on its own interval and eligibility conditions.
- Slowness duration is independent of native freezing state.
- Any remaining uncertainty about Mixin injection points, attack success semantics, or variant identification.

Do not resolve uncertainties through invented mechanics.

## STEP 10 — DELIVERABLE

Update the existing plan:

`.kilo/plans/1794458343114-cold-zombie-freezing-mechanics-plan.md`

The resulting document must contain:

1. Objective and approved gameplay behavior.
2. Corrected architecture.
3. Cryogenic Body technical design.
4. Freezing Touch technical design.
5. Verified Minecraft 26.1.2 integration points.
6. Exact proposed source files to modify.
7. Mixin justification.
8. Updated acceptance criteria.
9. Automated test scenarios.
10. Manual validation scenarios.
11. Native limitations.
12. Risks and unresolved questions.
13. Implementation sequence for a future Code session.

Clearly distinguish:

- VERIFIED: supported by inspected source code or reliable existing research.
- ASSUMPTION: reasonable but not yet demonstrated.
- UNRESOLVED: requires further investigation.

The future implementation plan must not contain any prohibited independent freezing state.

## STRICT EXECUTION RESTRICTIONS

- Plan mode only.
- No Java modifications.
- No resource modifications.
- No attachment registration.
- No Mixin registration.
- No Gradle changes.
- No dependency changes.
- No implementation scaffolding.
- No commits.
- No pushes.
- No destructive Git operations.
- Preserve all existing user changes.
- Do not begin implementation after completing the plan.

Only the existing Markdown implementation plan may be modified.

If a technical question cannot be resolved safely, document it rather than guessing.

## FINAL REPORT

Report:

1. Files inspected.
2. Existing architectural defects identified.
3. Corrections made to the plan.
4. Proposed implementation files.
5. Verified technical findings.
6. Remaining assumptions and unresolved questions.
7. Git status and confirmation that no source code was modified.
8. Whether the corrected plan is ready for a future Code session.

Use PASS, FAIL, BLOCKED, or NOT RUN where appropriate.

**Final requirement:** The corrected plan must faithfully implement the approved native freezing design. No independent freezing-duration state is permitted under any circumstances.

---

## Assistant

**Tool: bash**

**Tool: bash**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: bash**

**Tool: bash**

**Tool: read**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: bash**

**Tool: bash**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: glob**

**Tool: glob**

**Tool: grep**

**Tool: grep**

---

## Assistant

**Tool: bash**

**Tool: bash**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: glob**

**Tool: glob**

**Tool: glob**

**Tool: read**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: write**

---

## Assistant

**Tool: open_plan**

**Tool: plan_exit**

---

## User

---

## Assistant

**Tool: bash**

**Tool: read**

**Tool: read**

**Tool: bash**

---

## Assistant

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: write**

---

## Assistant

## 1. Files inspected
- **Git/state:** `.git` metadata via `git rev-parse --abbrev-ref HEAD`, `git status --short --branch`, `git log -1 --oneline`
- **Plan/research:** `.kilo/plans/1794458343114-cold-zombie-freezing-mechanics-plan.md` (read pre-correction, rewritten), `.kilo/sesions/S-0103_plan_session-ses_e31f.md`, `.kilo/sesions/S-0102_ask_session-ses_e320.md`, `.kilo/sesions/` listing (S-0103 distinct report not found)
- **Architecture/source:** `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java`, `src/main/java/com/baruc/brs/mobvariants/attachment/VariantAttachments.java`, `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java`, `src/main/resources/mob_variants_brs.mixins.json`, `src/client/resources/mob_variants_brs.client.mixins.json`, `src/client/java/com/baruc/brs/mobvariants/client/mixin/LivingEntityRendererMixin.java`, `src/main/resources/data/mob_variants_brs/variants/cold_zombie.json`, `src/main/java/com/baruc/brs/mobvariants/` (package listing)
- **Tests/build/docs:** `src/gametest/java/com/baruc/brs/mobvariants/gametest/VariantResourceGameTest.java`, `build.gradle`, `docs/TESTING.md`

## 2. Existing architectural defects identified
- **FAIL (prior plan):** Introduced prohibited independent freezing state/mechanics: `COLD_FREEZE_REMAINING_TICKS` attachment concept, victim-side remaining-duration tracking, per-tick countdown/coordination, forcing/holding `ticksFrozen` at/above 140, and suppressing/reconciling victim powder-snow clamp behavior.
- **FAIL (prior plan):** Proposed creating `ColdZombieAttachments`/mechanics/mixin scaffolding centered on duration tracking and tick integration, contrary to approved native-only design.
- **PASS (corrected):** All above removed; corrected plan mandates native `ticksFrozen` as the only freezing state and no per-tick/custom freeze timers/damage.

## 3. Corrections made to the plan
Updated `.kilo/plans/1794458343114-cold-zombie-freezing-mechanics-plan.md` to:
- Enforce approved architecture: Cryogenic Body via cold-zombie `VARIANT_TEXTURE` identity + narrow `canFreeze()` interception (candidate), Freezing Touch via `Mob.doHurtTarget` success hook adding `+40` to victim native `ticksFrozen` (overflow-safe) and refreshing Slowness I for 120 ticks server-side.
- Remove all prohibited elements (attachments, timers, UUID maps, custom freeze damage, per-tick processing, forced 140, victim powder-snow suppression).
- Document preserved vanilla mechanics (140 threshold, −2/tick recovery, powder snow +1/clamp to 140, 40-tick damage gate/order, sync/persistence, >140 storage/clamp-accepted behavior) and explicitly state no guarantee of periodic damage per hit.
- Add verified/assumption/unresolved classification, minimal proposed file list, mixin justification, acceptance criteria, automated/manual validation, risks, and implementation sequence/readiness gates.

## 4. Proposed implementation files
- **Modify (likely):** `src/main/resources/mob_variants_brs.mixins.json` (register required common mixins only)
- **Add (likely, minimal):** `src/main/java/com/baruc/brs/mobvariants/variant/ColdZombieSupport.java` (identity constant/predicate, reuse `VARIANT_TEXTURE`), `src/main/java/com/baruc/brs/mobvariants/mixin/LivingEntityColdZombieCryogenicMixin.java`, `src/main/java/com/baruc/brs/mobvariants/mixin/MobColdZombieAttackMixin.java`
- **Extend for validation (if feasible):** `src/gametest/java/com/baruc/brs/mobvariants/gametest/VariantResourceGameTest.java` or new `ColdZombieFreezingGameTest.java`; optionally `docs/TESTING.md`
- **Explicitly excluded:** new freeze attachments/registration, tick handlers, countdowns, UUID maps, custom freeze damage, victim powder-snow suppression, Gradle/dependency/unrelated changes

## 5. Verified technical findings
- **VERIFIED:** Branch `feat/variant-state-layer`, HEAD `a475854`, working tree clean/up-to-date at inspection; `cold_zombie` variant JSON/texture id and `VARIANT_TEXTURE` (persistent+synced `Identifier`) identity mechanism exist; common mixins empty, client mixins render-only; no existing cold-zombie mechanics/attack hooks in mod code; GameTest infra present.
- **VERIFIED (from prior mapped-source research reused in repo):** Native freeze core — threshold/full-freeze at 140, `ticksFrozen` synced/persisted and writable >140, server `LivingEntity.baseTick` recovery −2/tick when `!isInPowderSnow || !canFreeze()`, powder snow `FREEZE` +1/tick with `canFreeze()` gate and clamp to 140, periodic freeze damage on `tickCount % 40 && isFullyFrozen() && canFreeze()`, recovery precedes damage check in referenced order; `Mob.doHurtTarget(ServerLevel, Entity)` is the reliable successful melee path/return semantics (prior).
- **VERIFIED (design correction):** Approved mechanic must rely solely on native `ticksFrozen` mutations/effects and must not introduce independent freezing state.

## 6. Remaining assumptions and unresolved questions
- **ASSUMPTION:** `canFreeze()` interception on `LivingEntity` is sufficient for Cryogenic Body in 26.1.2 without bypasses/unwanted unrelated side effects (needs source/call-site recheck).
- **ASSUMPTION:** `Mob.doHurtTarget` mapping/signature and reliable boolean-return capture at chosen injection point in this environment.
- **UNRESOLVED:** Exact 26.1.2 Slowness effect constant/API and refresh semantics (`MobEffects`/`addEffect` details).
- **UNRESOLVED:** No alternate freeze accumulation/damage path bypassing `canFreeze()` for cold zombies.
- **UNRESOLVED:** Locally generated 26.1.2 mapped sources not re-inspected this session; exact configured GameTest Gradle task name (T10) to confirm at run time.
- **UNRESOLVED/Limitation:** Distinct S-0103 session report artifact (separate from `S-0103_plan_session-ses_e31f.md`) not found.
- **Accepted native limitations:** >140 may be reduced by powder-snow clamp/recovery; periodic freeze damage not guaranteed per hit.

## 7. Git status and confirmation that no source code was modified
- **Git status:** `PASS` — `## feat/variant-state-layer...origin/feat/variant-state-layer` (clean/up-to-date). HEAD `a475854`.
- **No source/resource/build/config/mixin-registration code modified:** **PASS** (only plan Markdown updated: `.kilo/plans/1794458343114-cold-zombie-freezing-mechanics-plan.md`). No Java, JSON resources, mixin configs, Gradle, or implementation scaffolding changed; no commits/pushes/destructive operations.

## 8. Whether the corrected plan is ready for a future Code session
- **READY FOR A FUTURE CODE SESSION (with mandatory implementation-time verifications):** **PASS (conditional)**.
- Conditions: verify T4–T8/T10 against actual 26.1.2 mapped sources/APIs (`canFreeze` sufficiency/bypasses, `doHurtTarget` signature/return capture, Slowness effect/addEffect usage, GameTest task) before coding; follow the minimal native-only file/integration sequence in the plan.

**Saved plan:** `.kilo/plans/1794458343114-cold-zombie-freezing-mechanics-plan.md`

---

## User

# MOB VARIANTS BRS — COLD ZOMBIE PLAN REFINEMENT

## SESSION CONFIGURATION

- **Kilo Mode:** Plan
- **New Kilo Session:** NO
- **Project:** MobVariantsBRS
- **Repository:** https://github.com/barucsinfiltros/MobVariantsBRS
- **Target branch:** `feat/variant-state-layer`
- **Previous planning session:** S-0104
- **Primary plan:** `.kilo/plans/1794458343114-cold-zombie-freezing-mechanics-plan.md`

Continue the existing planning conversation and preserve its useful investigation findings. This is a refinement of an existing design, not a new feature investigation from scratch.

## OBJECTIVE

Refine the existing Cold Zombie mechanics plan to implement a natural interaction between powder snow and the variant's freezing attacks.

Use Minecraft's existing `Entity.ticksFrozen` counter as the single source of truth for all freezing accumulation.

The deliverable is an updated planning document and session record. Do not implement the mechanics.

## 1. APPROVED GAMEPLAY DESIGN

### 1.1 Cryogenic Body — Powder Snow Immunity

The `cold_zombie` must be immune to powder snow freezing.

Requirements:

- It must not accumulate freezing points from powder snow.
- It must not receive vanilla periodic freezing damage.
- Immunity must apply exclusively to the `cold_zombie` variant.
- All other entities must retain normal vanilla powder snow behavior.
- Do not interfere with unrelated damage or entity mechanics.

Prefer the existing variant identification mechanism based on `VARIANT_TEXTURE`.

### 1.2 Freezing Touch — Shared Accumulation

Use the existing vanilla `Entity.ticksFrozen` value as the only freezing counter.

Powder snow:
- Preserve vanilla accumulation behavior.
- Never reset or replace existing freezing progress.

Cold Zombie attacks:
- Every successful melee hit against a `LivingEntity` adds exactly 40 points to the target's current `ticksFrozen`.
- Add 40 to the existing value; never replace it.
- Repeated successful hits accumulate.
- Preserve vanilla freezing thresholds, visuals, recovery and periodic damage.

Example:

- Existing freezing progress: 60.
- Successful Cold Zombie attack: +40.
- Result: 100.
- Remaining inside powder snow continues vanilla accumulation from 100.

Do not introduce another freezing-duration or accumulation system.

Important distinction:

A Cold Zombie's powder snow immunity must not automatically prevent it from receiving freezing points through another Cold Zombie's attack.

Investigate whether the proposed immunity mechanism preserves this distinction.

### 1.3 Slowness

Retain the previously approved behavior:

- Slowness I.
- Duration: 120 ticks (6 seconds).
- Applied on successful melee hits.
- Repeated hits refresh the effect without indefinitely accumulating duration.
- Preserve an existing stronger or longer Slowness effect when replacing it would be unnecessary.

Define the precise refresh/update conditions using the APIs available in Minecraft 26.1.2.

## 2. ARCHITECTURAL CONSTRAINTS

Preserve the smallest possible implementation.

Do not introduce:

- Custom freezing counters.
- Attachments for freezing duration.
- Timers or expiration timestamps.
- UUID-based tracking maps.
- Additional per-tick processing.
- Parallel freezing systems.

Prefer existing Minecraft/Fabric APIs and the established `VARIANT_TEXTURE` identification system.

Gameplay modifications must remain server-authoritative.

Do not modify unrelated zombie variants, rendering, textures, entity types, variant selection or other systems.

## 3. REQUIRED TECHNICAL VERIFICATION

Before declaring the plan ready for implementation, investigate the following questions using the actual Minecraft 26.1.2 sources, mappings, Fabric API and repository implementation.

Do not rely on generic Minecraft documentation or APIs from another version.

### 3.1 `LivingEntity.canFreeze()` behavior

Determine whether overriding or otherwise using `LivingEntity.canFreeze()` provides all required protection against:

- Powder snow freezing accumulation.
- Vanilla periodic freezing damage.

Verify the exact call paths in Minecraft 26.1.2.

Determine whether this mechanism affects only environmental freezing or also prevents manually adding freezing progress through `ticksFrozen`.

Specifically verify that a Cold Zombie can still receive +40 freezing points when attacked by another Cold Zombie.

If this mechanism is insufficient, identify the smallest safe alternative.

Do not introduce Mixins unless the native API is demonstrably insufficient and the existing architecture justifies that approach.

### 3.2 Existing freezing progress and recovery

Determine what happens when an entity becomes unable to freeze while it already has nonzero `ticksFrozen`.

Verify whether its freezing progress recovers naturally and whether periodic damage stops.

Assess whether this behavior is acceptable for the approved Cryogenic Body design.

Document any relevant consequences instead of introducing additional state to preserve freezing progress.

### 3.3 Shared `ticksFrozen` accumulation

Verify:

- The exact field and accessors available in Minecraft 26.1.2.
- Whether direct modification is supported and safe.
- Whether adding 40 preserves existing powder snow accumulation.
- How vanilla clamps, increments, decrements and consumes this value.
- Whether setting the counter affects visual feedback and damage thresholds as expected.

Do not introduce artificial gameplay caps.

### 3.4 Successful melee detection

Inspect the existing Cold Zombie attack implementation and the exact Minecraft 26.1.2 APIs.

Identify the most appropriate location to apply the freezing and Slowness effects only after a successful melee hit.

Verify the method signatures, return values and execution side.

Avoid applying effects when an attack misses or fails.

### 3.5 Slowness refresh logic

Verify the available status-effect APIs and update behavior.

Define conditions that:

- Apply Slowness I for 120 ticks when no Slowness effect exists.
- Refresh an existing shorter Slowness I effect.
- Preserve a stronger effect.
- Preserve an equal-strength effect with a longer duration.

Avoid unnecessary effect replacement.

### 3.6 Integer overflow

Determine whether adding 40 to `ticksFrozen` requires explicit overflow protection.

Inspect the actual vanilla counter limits and update behavior.

Use the smallest safe approach that prevents integer overflow without introducing arbitrary gameplay caps or additional state.

## 4. EXISTING PLAN REFINEMENT

Update the existing document directly:

`.kilo/plans/1794458343114-cold-zombie-freezing-mechanics-plan.md`

Preserve useful findings from S-0104.

Do not rewrite unrelated sections or duplicate research unnecessarily.

Explicitly separate:

1. **Approved gameplay behavior:** decisions established by the user.
2. **Technically verified behavior:** conclusions supported by actual Minecraft 26.1.2 source code or repository evidence.
3. **Unresolved assumptions:** questions that still require investigation or validation.

Remove obsolete design statements that conflict with shared `ticksFrozen` accumulation.

Ensure the plan describes one coherent implementation rather than alternative freezing systems.

Include relevant implementation locations only when verified in the repository.

## 5. SESSION RECORD

Update the current planning session record using the existing session documentation conventions.

Locate the appropriate S-0104 continuation record and verify its filename before modifying or creating anything.

Document:

- The updated gameplay decisions.
- Technical investigations performed.
- Source files and API definitions inspected.
- Verified findings and unresolved questions.
- Changes made to the existing plan.
- Why the shared vanilla counter is preferred.
- Remaining implementation risks.
- Final readiness assessment.

Do not invent session identifiers or report findings that were not verified.

## 6. VALIDATION AND ACCEPTANCE CRITERIA

Evaluate each criterion using one of these statuses:

- **VERIFIED:** Supported by concrete source or repository evidence.
- **FAILED:** Evidence demonstrates that the proposed design does not satisfy the requirement.
- **UNVERIFIED:** Evidence is insufficient or the behavior cannot be conclusively established during planning.

Acceptance criteria:

1. Cold Zombie does not accumulate environmental freezing from powder snow.
2. Cold Zombie does not receive vanilla powder snow periodic freezing damage.
3. Other entities retain normal powder snow behavior.
4. Cold Zombie attacks add exactly +40 to the target's existing `ticksFrozen`.
5. Repeated successful attacks accumulate without replacing previous progress.
6. Powder snow and attacks share the same vanilla counter.
7. Vanilla freezing thresholds, visuals, recovery and damage remain functional.
8. A Cold Zombie can still receive attack-based freezing progress from another Cold Zombie.
9. Slowness I lasts 120 ticks and uses appropriate refresh logic.
10. No custom freezing state or unnecessary per-tick processing is required.
11. Integer overflow is handled safely without arbitrary gameplay caps.
12. All required APIs and method signatures are compatible with Minecraft 26.1.2.

Do not mark source-level compatibility as verified without inspecting the actual version-specific implementation.

If a behavior requires runtime testing that cannot be performed in planning mode, explicitly mark it UNVERIFIED and describe the required test.

## 7. SCOPE RESTRICTIONS

THIS IS A PLAN-ONLY SESSION.

Do not:

- Modify Java implementation files.
- Add or register Mixins.
- Modify resources or dependencies.
- Implement any mechanics.
- Perform unrelated refactoring.
- Commit or push changes.
- Start implementation.

You are authorized to modify only the existing planning document and the appropriate planning session record.

Inspect Git status before editing. Preserve all unrelated user changes.

If the current Kilo mode or permissions prevent a requested document update, report the limitation rather than attempting to bypass it.

## 8. FINAL REPORT

Provide:

### A. Updated artifacts
- Exact path of the modified planning document.
- Exact path of the updated session record.

### B. Technical summary
Explain the proposed immunity mechanism, shared counter behavior, melee integration and Slowness refresh strategy.

### C. Acceptance matrix
List every acceptance criterion with VERIFIED, FAILED or UNVERIFIED and supporting evidence.

### D. Remaining risks
Identify unresolved API questions, runtime validation requirements or architectural limitations.

### E. Final decision

Choose exactly one:

- **READY FOR CODE:** The implementation approach is sufficiently established and no critical design blockers remain.
- **NOT READY FOR CODE:** At least one critical technical question remains unresolved or the design requires further refinement.

Do not equate a complete planning document with verified technical correctness.

The final recommendation must be based on evidence from Minecraft 26.1.2 and the current repository.

**Primary objective:** Achieve natural environmental interaction between powder snow and Cold Zombie freezing attacks by reusing vanilla freezing mechanics, with minimal custom code and no parallel state system.

---

## Assistant

**Tool: bash**

**Tool: read**

**Tool: glob**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: write**

**Tool: write**

**Tool: bash**

---

## Assistant

**Tool: write**

**Tool: bash**

---

## Assistant

**Tool: open_plan**

**Tool: plan_exit**

---

