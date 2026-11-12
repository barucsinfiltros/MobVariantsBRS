# Cold zombie freezing mechanics plan

**Session ID:** ses_e31f2a935ffeHAYNTGLvMtrRDE
**Created:** 11/12/2026, 12:39:03 AM
**Updated:** 11/12/2026, 12:51:27 AM

---

## User

# MOB VARIANTS BRS — COLD ZOMBIE FREEZING MECHANICS
## READ-ONLY IMPLEMENTATION PLANNING

### SESSION CONFIGURATION
- Kilo Mode: Plan
- New Kilo Session: YES
- Repository: https://github.com/barucsinfiltros/MobVariantsBRS
- Target branch: feat/variant-state-layer
- Minecraft: Java Edition 26.1.2
- Fabric Loader: 0.19.5
- Fabric API: 0.155.3+26.1.2
- Java: 25

### OBJECTIVE

Prepare a technically accurate, evidence-based implementation plan for the cold_zombie mechanics.

The approved design uses Minecraft's native freezing system. Every successful cold_zombie melee attack adds exactly 40 points to the victim's native ticksFrozen value.

This is strictly a planning and investigation session.

DO NOT implement anything.

### STEP 1 — REPOSITORY AND WORKING TREE

1. Identify and verify the actual workspace root.
2. Verify the current branch, HEAD commit and working-tree status.
3. Inspect existing modifications before investigating source files.
4. Preserve all existing user changes.
5. Do not switch branches, reset, clean, stash, revert or modify files.

If the current branch differs from the target branch, report the discrepancy without switching.

### STEP 2 — CURRENT ARCHITECTURE

Inspect the current implementation and verify whether these components still exist and retain their expected responsibilities:

- VariantAttachments.VARIANT_TEXTURE
- VariantStateLifecycle
- cold_zombie.json
- Common and client Mixin configurations
- Existing variant identification using:

  mob_variants_brs:textures/entity/zombie/zombie_cold.png

Identify the exact repository-relative source paths.

Explain how the variant identity is resolved and accessed on the logical server.

Do not introduce another variant identification mechanism.

### STEP 3 — MINECRAFT NATIVE FREEZING INVESTIGATION

Inspect the actual Minecraft 26.1.2 mapped sources and relevant Fabric API sources.

Do not rely on older Minecraft versions or undocumented assumptions.

Investigate:

- Entity.getTicksFrozen()
- Entity.setTicksFrozen(int)
- Entity.isFullyFrozen()
- LivingEntity.canFreeze()
- Native freezing update methods.
- Powder snow accumulation.
- Natural freezing recovery.
- Periodic freezing damage.
- Entity persistence.
- Entity synchronization.

Verify the following questions:

1. Can setTicksFrozen() safely store values above 140?
2. Does vanilla immediately clamp values above 140?
3. Can subsequent entity updates overwrite or clamp those values?
4. How does powder snow behave when ticksFrozen already exceeds 140?
5. Does natural recovery subtract 2 points per tick above 140?
6. Does isFullyFrozen() remain true above the threshold?
7. Does periodic freezing damage continue normally above 140?
8. Does entity synchronization preserve values above 140?
9. Does entity persistence preserve values above 140?
10. Can direct accumulation coexist with ordinary powder snow accumulation?

Pay special attention to update ordering:

- Natural recovery.
- Powder snow accumulation.
- canFreeze().
- isFullyFrozen().
- Periodic freezing damage.

Trace actual method calls and conditions.

Provide source evidence, method signatures and relevant code locations.

Do not assume that exceeding 140 guarantees additional fully frozen duration.

### STEP 4 — APPROVED GAMEPLAY DESIGN

#### A. Cryogenic Body

The cold_zombie must:

- Be immune to natural powder snow freezing.
- Never accumulate freezing points from powder snow.
- Never receive vanilla periodic freezing damage.
- Remain vulnerable to unrelated damage.
- Leave all other entities' freezing behavior unchanged.

Investigate the smallest variant-specific interception point, especially LivingEntity.canFreeze().

Explain whether overriding or intercepting this method is sufficient to prevent all required freezing behavior.

Verify that the solution does not accidentally disable unrelated damage.

#### B. Freezing Touch

After every successful melee attack by a cold_zombie against a LivingEntity:

**Slowness**
- Apply Slowness I.
- Duration: 120 ticks.
- Repeated hits refresh the duration.
- Never accumulate beyond 120 ticks.

**Native freezing accumulation**
- Read the victim's getTicksFrozen().
- Add exactly 40.
- Write the result through setTicksFrozen(int).
- Allow repeated attacks to accumulate naturally.
- Do not introduce a custom freezing duration or independent state.

Expected progression:

| Successful hits | ticksFrozen |
|---|---:|
| 1 | 40 |
| 2 | 80 |
| 3 | 120 |
| 4 | 160 |

Do not clamp to 140 without documenting the technical necessity and trade-off.

The effect must recover naturally when the victim stops receiving freezing accumulation.

No custom periodic damage or freezing status effect is permitted.

### STEP 5 — MELEE ATTACK INTEGRATION

Investigate:

Mob.doHurtTarget(ServerLevel, Entity)

Verify:

- Exact Minecraft 26.1.2 signature.
- Return-value semantics.
- Whether a successful return reliably indicates successful damage application.
- Whether the method can trigger for non-melee damage or unrelated interactions.

Evaluate a TAIL injection that reads the successful return value.

The proposed integration must:

1. Execute only after successful melee damage.
2. Require the attacker to be the cold_zombie variant.
3. Require the victim to be a LivingEntity.
4. Execute on the logical server.
5. Ignore misses and unsuccessful attacks.

Prefer the smallest reliable integration point.

If a TAIL injection is unsuitable, explain why and recommend the smallest alternative.

### STEP 6 — SYNCHRONIZATION AND PERFORMANCE

Evaluate:

- Dedicated server behavior.
- Multiplayer synchronization of freezing state.
- Slowness synchronization.
- Entity persistence and lifecycle.
- Existing attachment synchronization.
- Repeated melee attacks.
- Minimal per-attack processing.

All gameplay modifications must be server-authoritative.

No additional per-tick processing is permitted.

### STEP 7 — PROPOSED FILE CHANGES

Identify the exact existing files that would need modification.

For each file explain:

- Current responsibility.
- Proposed modification.
- Reason.
- Dependencies.
- Whether a Mixin is necessary.

Do not create files or implementation scaffolding.

Do not modify Java, JSON, Gradle or configuration files.

### STEP 8 — ACCEPTANCE CRITERIA

The proposed implementation must satisfy:

1. Exactly +40 native freezing points per successful cold_zombie melee hit.
2. Slowness I for exactly 120 ticks, refreshed on subsequent hits.
3. No independent freezing counter or duration tracking.
4. No natural powder snow freezing for cold_zombie.
5. No vanilla periodic freezing damage for cold_zombie.
6. Unrelated damage remains functional.
7. Other entities retain vanilla freezing behavior.
8. Natural recovery remains vanilla-controlled.
9. Values above 140 are explicitly analyzed.
10. Server-authoritative execution.
11. No unrelated architectural changes.

### STEP 9 — VALIDATION PLAN

Provide relevant commands for:

- Existing project compilation.
- Relevant automated tests.
- Dedicated server testing.
- Multiplayer testing.

Do not execute destructive or modifying commands.

Define in-game scenarios for:

1. One successful melee hit.
2. Two, three and four consecutive hits.
3. Repeated hits while Slowness is active.
4. Recovery after combat.
5. Victim entering powder snow with existing freezing points.
6. Victim leaving powder snow.
7. Freezing damage at and above 140.
8. Cold zombie standing inside powder snow.
9. Cold zombie receiving unrelated damage.
10. Ordinary zombie freezing behavior.
11. Multiplayer state synchronization.
12. Entity unload/reload persistence.

Distinguish automated validation from manual in-game verification.

### STRICT CONSTRAINTS

This session is READ-ONLY.

DO NOT:

- Modify source files.
- Modify resources.
- Create files or attachments.
- Register Mixins.
- Modify Mixin configurations.
- Change Gradle files.
- Add dependencies.
- Implement gameplay mechanics.
- Create scaffolding.
- Commit or push.
- Reset, revert, stash or discard existing changes.
- Overwrite user modifications.

If any requirement conflicts with vanilla behavior, document the exact conflict and propose the smallest alternative for approval.

### FINAL REPORT

Produce a concrete implementation plan containing:

1. Repository branch, HEAD and working-tree status.
2. Verified current architecture.
3. Exact Minecraft and Fabric API signatures.
4. Cryogenic Body design.
5. Freezing Touch design.
6. Native accumulation and recovery analysis.
7. Powder snow compatibility.
8. Periodic freezing damage behavior.
9. Values above 140: storage, updates, synchronization and persistence.
10. Server/client synchronization.
11. Exact proposed source files.
12. Acceptance criteria.
13. Validation commands and in-game scenarios.
14. Risks and unresolved questions.
15. Recommended implementation sequence.

Classify findings as:

- VERIFIED: supported by inspected source or reproducible evidence.
- ASSUMPTION: not yet established.
- UNRESOLVED: requires further investigation.
- RECOMMENDATION: proposed architectural decision.

Cite exact source files, methods and relevant code locations.

Do not claim that a behavior is verified without evidence.

The final report must remain a plan. No implementation is authorized.

---

## Assistant

**Tool: bash**

**Tool: glob**

---

## Assistant

**Tool: bash**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: glob**

**Tool: glob**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: read**

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

---

## Assistant

Now I need to investigate Minecraft's native freezing system in 26.1.2. Let me use Context7 to look up the relevant documentation.


**Tool: context7_resolve-library-id**

**Tool: context7_resolve-library-id**

---

## Assistant

**Tool: context7_query-docs**

**Tool: context7_query-docs**

---

## Assistant

**Tool: context7_query-docs**

**Tool: context7_query-docs**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: task**

---

## Assistant

**Tool: context7_query-docs**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: task**

---

## Assistant

**Tool: task**

---

## Assistant

Now I have all the verified information needed. Let me create the comprehensive implementation plan.


**Tool: write**

---

## Assistant

**Tool: open_plan**

---

## Assistant

**Tool: plan_exit**

---

