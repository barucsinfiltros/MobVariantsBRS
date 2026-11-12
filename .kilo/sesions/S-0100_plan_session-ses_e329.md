# Planning cold zombie mechanics for MobVariantsBRS mod

**Session ID:** ses_e329502caffeEGkZRi1cHvQSir
**Created:** 11/11/2026, 9:41:43 PM
**Updated:** 11/11/2026, 9:48:26 PM

---

## User

# MOB VARIANTS BRS — COLD ZOMBIE MECHANICS
## TECHNICAL DESIGN AND IMPLEMENTATION PLAN

### KILO CODE SESSION

- **Mode:** Plan.
- **New Session:** YES.
- **Project:** MobVariantsBRS.
- **Repository:** https://github.com/barucsinfiltros/MobVariantsBRS
- **Target branch:** `feat/variant-state-layer`.
- **Minecraft:** Java Edition 26.1.2.
- **Fabric Loader:** 0.19.5.
- **Fabric API:** 0.155.3+26.1.2.
- **Java:** 25.
- **Build system:** Gradle with Fabric Loom.

## 1. OBJECTIVE

Design a technically sound implementation for two exclusive gameplay mechanics belonging to the existing `cold_zombie` variant:

1. **Cryogenic Body:** Passive resistance to Minecraft freezing mechanics.
2. **Freezing Touch:** Successful attacks apply renewable Slowness and progressively accumulated freezing duration.

This is a planning session, not an implementation session.

Investigate the repository, verify the current Minecraft/Fabric APIs, trace the existing architecture, compare implementation options, and produce a concrete implementation plan.

**Do not modify application source code, resources, configuration, dependencies, or build files.**

Do not implement mechanics for other zombie variants.

## 2. MANDATORY INITIAL INVESTIGATION

Before designing the implementation:

1. Verify the workspace root.
2. Inspect Git status, current branch, HEAD commit, and target branch availability.
3. Identify uncommitted changes and preserve them.
4. Inspect relevant `.kilo/` documentation, especially previous architecture decisions and implementation reports.
5. Inspect the existing variant selection, attachment, lifecycle, attributes, and resource-loading architecture.
6. Identify exactly how `cold_zombie` is selected and represented.
7. Inspect the current Minecraft 26.1.2 and Fabric API 0.155.3+26.1.2 APIs and mappings.

Use the actual repository as the implementation source of truth.

Do not assume older Minecraft APIs or mappings are compatible.

Do not invent methods, events, fields, or classes.

### 2.1 Native freezing investigation

Trace the actual vanilla freezing implementation, including:

- `LivingEntity` freezing-related fields and methods.
- `ticksFrozen` and its maximum/threshold behavior.
- `canFreeze`, `isFullyFrozen`, `getTicksFrozen`, and `setTicksFrozen`, if available.
- Powder snow freezing logic.
- Vanilla freezing damage and damage intervals.
- Natural freezing recovery.
- Behavior outside powder snow.
- Entity persistence and client synchronization.

Determine whether the native freezing counter is capped and whether its lifecycle can represent an accumulated duration longer than the native maximum.

Identify the exact methods and call paths responsible for increasing, decreasing, synchronizing, and applying freezing damage.

Document the verified API signatures and relevant source locations.

## 3. MECHANIC A — CRYOGENIC BODY

### Desired behavior

Only entities identified as `cold_zombie` should receive this adaptation.

- They must not accumulate freezing from powder snow.
- They must not receive vanilla freezing damage.
- They must retain normal behavior against unrelated damage sources.
- Their variant identity must remain unchanged.
- Other zombie variants must retain vanilla freezing behavior.

### Design requirements

Investigate the most appropriate Fabric event or vanilla extension point.

Prefer a suitable Fabric event if it reliably prevents both freezing accumulation and freezing damage.

Otherwise, investigate the smallest safe Mixin targeting the exact freezing implementation.

Do not:

- Cancel all damage.
- Globally disable freezing.
- Use biome checks every tick.
- Modify unrelated entity behavior.

Use the existing server-authoritative variant identity.

Explain whether preventing freezing accumulation alone is sufficient to prevent freezing damage, or whether both paths require intervention.

## 4. MECHANIC B — FREEZING TOUCH

The mechanic triggers when a `cold_zombie` successfully hits a living target.

It contains two independent effects.

### 4.1 Renewable Slowness

Initial balancing proposal:

| Parameter | Value |
|---|---:|
| Duration | 6 seconds |
| Amplifier | 0 |

Requirements:

- Apply Slowness after a successful attack.
- Refresh its duration on subsequent successful attacks.
- Never add the previous duration to the new duration.
- Never increase the amplifier through repeated attacks.
- Keep the values centralized and adjustable.

Investigate the appropriate Minecraft 26.1.2 status-effect application API.

Prefer Minecraft's existing status-effect lifecycle instead of introducing another timer.

### 4.2 Accumulative Freezing

Initial balancing proposal:

- Added freezing duration per successful hit: 2 seconds (40 game ticks).

Required behavior:

| Successful hit | Added duration | Total pending duration |
|---|---:|---:|
| 1 | 40 ticks | 40 ticks |
| 2 | 40 ticks | 80 ticks |
| 3 | 40 ticks | 120 ticks |
| 4 | 40 ticks | 160 ticks |

Requirements:

- Every successful hit adds the configured duration.
- Never overwrite previous accumulated duration.
- No arbitrary gameplay duration cap.
- Use safe arithmetic to prevent integer overflow.
- Duration must count down independently of proximity to the attacking zombie.
- Freezing must continue after leaving powder snow.
- When the accumulated duration reaches zero, the victim must recover normally.
- Repeated attacks must extend the pending freezing period, not merely refresh a fixed vanilla effect.

### 4.3 Critical native compatibility investigation

Do not simply add ticks to `ticksFrozen` and assume this satisfies the requirements.

The vanilla counter may be bounded by a maximum value and may recover automatically.

Determine whether it is possible to reuse vanilla freezing visuals and damage while maintaining a separate accumulated duration.

Compare these implementation approaches:

**Approach A — Native counter integration**

Use the vanilla freezing counter and lifecycle directly, if its behavior can faithfully represent the required accumulated duration.

**Approach B — Separate remaining-duration state**

Maintain an independent remaining-freezing-duration state and coordinate it with the native counter.

**Approach C — Controlled vanilla lifecycle integration**

Use a separate duration state while carefully integrating with the native freezing lifecycle, potentially through narrowly scoped Mixins.

For each approach, evaluate:

- Duration accuracy.
- Vanilla freezing damage behavior.
- Damage intervals.
- Freezing visuals.
- Natural recovery.
- Powder snow interaction.
- Client synchronization.
- Entity persistence.
- Multiplayer correctness.
- Compatibility with unrelated freezing sources.

Select the safest approach based on verified source code.

If a custom freezing damage implementation is unavoidable, explain why native damage cannot be reused safely and how duplicate damage will be prevented.

Do not force `ticksFrozen` to its maximum every tick without proving that damage intervals and recovery remain correct.

## 5. ATTACK DETECTION

Find the most reliable way to detect a successful melee hit from a `cold_zombie`.

Requirements:

- Trigger only after an actual successful attack.
- Do not trigger when the zombie misses.
- Do not trigger repeatedly for one attack.
- Do not trigger for unrelated damage sources.
- Do not apply effects to the attacking zombie itself.
- Apply effects only to valid living targets.
- Preserve normal combat damage.

Investigate existing Fabric events first.

Determine whether the selected event exposes sufficient information to identify the actual attacker and victim.

Do not assume a generic damage event reliably identifies the attacker.

If a Mixin is necessary:

- Identify the exact target method.
- Explain why an event is insufficient.
- Select the narrowest reliable injection point.
- Explain how duplicate triggers are avoided.

Provide the exact verified API signatures and execution order.

## 6. STATE, PERSISTENCE AND SYNCHRONIZATION

Determine whether the accumulated duration requires a persistent entity attachment.

If an attachment is appropriate, design it using the existing common-side attachment architecture.

Requirements:

- Register it consistently on logical client and server where required.
- Persist remaining duration when necessary.
- Synchronize only state required by clients.
- Keep the server authoritative.
- Restore the state correctly after entity reload.
- Keep victim freezing state separate from attacker variant identity.
- Avoid global maps keyed by UUID unless the existing architecture provides a compelling reason.
- Avoid memory leaks when entities die, unload, or change dimensions.

Explain whether the state should be represented as:
- Remaining ticks.
- An absolute game-time expiration.
- Another representation justified by the existing lifecycle.

Consider save/reload behavior, world time, dimension changes, and unloaded entities.

The Slowness effect must remain managed by Minecraft's existing status-effect lifecycle.

## 7. ARCHITECTURE AND PERFORMANCE

Respect these constraints:

- Server-authoritative implementation.
- Strict client/common separation.
- Reuse existing variant identity and attachment systems.
- No per-tick biome checks.
- No scanning all world entities.
- No unnecessary global registries or dependencies.
- No changes to other variant definitions.
- No new EntityTypes.
- No arbitrary code execution from JSON.
- Small, focused classes.
- No unnecessary abstractions.
- No changes to texture selection or rendering architecture.

If accumulated freezing requires ticking, select the smallest lifecycle mechanism compatible with the verified Fabric version.

Avoid unnecessary allocations and expensive operations every tick.

## 8. CONFIGURATION AND BALANCING

Centralize these adjustable values:

| Parameter | Initial value |
|---|---:|
| Slowness duration | 6 seconds |
| Slowness amplifier | 0 |
| Freezing added per hit | 40 ticks |

Use clear names and existing project conventions.

Do not introduce a configuration framework or external dependency for three values unless the project already has an appropriate system.

## 9. VALIDATION PLAN

Design validation for all the following scenarios:

1. A normal zombie can still freeze in powder snow.
2. A `cold_zombie` does not accumulate freezing in powder snow.
3. A `cold_zombie` does not receive vanilla freezing damage.
4. A `cold_zombie` still receives unrelated damage normally.
5. A successful `cold_zombie` attack applies Slowness.
6. Repeated attacks refresh Slowness without stacking duration or amplifier.
7. Every successful hit adds the configured freezing duration.
8. Repeated attacks accumulate duration correctly.
9. Freezing continues after the victim leaves powder snow.
10. Remaining duration counts down correctly.
11. The victim recovers when accumulated duration expires.
12. The behavior works in multiplayer with server-authoritative state.
13. Accumulated state survives entity save/reload if persistence is implemented.
14. Other zombie variants remain unaffected.

Include edge cases:

- Rapid consecutive attacks.
- Target death.
- Entity unload and reload.
- Dimension changes.
- Integer overflow protection.
- Existing freezing state before being hit.
- Simultaneous powder snow exposure and accumulated freezing.
- Repeated attacks during natural recovery.

Identify suitable automated tests, GameTests, and manual runtime tests based on the repository's actual testing architecture.

Do not claim a scenario is verified unless it has actually been executed.

## 10. REQUIRED DELIVERABLE

Produce a concise but technically complete design report containing:

### A. Repository status

- Verified workspace root.
- Current branch.
- HEAD commit.
- Working tree status.
- Relevant `.kilo/` documents inspected.

### B. Existing architecture

- Variant identity mechanism.
- Relevant attachments.
- Lifecycle hooks.
- Existing attack handling.
- Relevant entity and resource classes.

### C. Verified API investigation

- Exact Minecraft freezing fields and methods.
- Relevant Fabric events and signatures.
- Relevant status-effect APIs.
- Persistence and synchronization APIs.
- Relevant vanilla source call paths.

### D. Proposed implementation

- Exact attack detection mechanism.
- Exact Cryogenic Body implementation point.
- Selected freezing-duration architecture.
- Native freezing integration.
- Damage and recovery handling.
- Attachment and synchronization strategy.
- Configuration constants.

### E. Expected file changes

List repository-relative paths.

For every proposed file, specify:
- Existing or new.
- Its purpose.
- Why it must change.
- Whether it belongs to common, client, or test code.

Do not invent paths. Verify existing files and identify new paths as proposed.

### F. Risks and unresolved questions

List any remaining API uncertainty, behavioral risks, compatibility concerns, and assumptions.

Do not hide unresolved questions behind an unsupported implementation recommendation.

### G. Validation strategy

Provide concrete automated and manual test cases with expected results.

### H. Implementation sequence

Provide ordered, minimal implementation steps and their dependencies.

The plan must be sufficiently precise for a separate implementation session without requiring another architectural investigation.

## 11. EXPLICIT EXCLUSIONS

Do not:

- Modify other zombie variants.
- Change biome assignments.
- Change textures or rendering.
- Change existing variant selection architecture unless strictly necessary.
- Add new EntityTypes.
- Add unrelated attributes or abilities.
- Introduce arbitrary duration caps.
- Change general Minecraft freezing mechanics for unrelated entities.
- Add dependencies or configuration frameworks unnecessarily.
- Commit or push changes.
- Reset, revert, or discard existing work.
- Modify application source code during this planning session.

## 12. FINAL INSTRUCTIONS

This session is exclusively for technical investigation and planning.

Prioritize the correctness of accumulated freezing over implementation simplicity.

The central acceptance criterion is that repeated successful attacks must produce genuinely cumulative freezing duration while preserving vanilla freezing damage, visual behavior, and natural recovery as faithfully as technically possible.

If the exact Minecraft 26.1.2 implementation makes any requirement impossible without a narrow Mixin or a custom lifecycle integration, demonstrate that limitation using verified source code and propose the smallest safe solution.

**Do not begin implementation. Stop after delivering the technical plan and explicit unresolved questions.**

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

**Tool: bash**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: glob**

**Tool: bash**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: bash**

**Tool: read**

**Tool: read**

**Tool: read**

**Tool: read**

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

**Tool: bash**

**Tool: read**

**Tool: glob**

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

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: bash**

**Tool: bash**

**Tool: bash**

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: bash**

**Tool: read**

**Tool: read**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: bash**

**Tool: bash**

**Tool: read**

**Tool: read**

**Tool: read**

**Tool: grep**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

**Tool: glob**

**Tool: glob**

**Tool: glob**

---

## Assistant

**Tool: context7_resolve-library-id**

**Tool: context7_resolve-library-id**

**Tool: read**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: context7_query-docs**

---

## Assistant

**Tool: context7_query-docs**

**Tool: bash**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: bash**

**Tool: bash**

**Tool: bash**

**Tool: bash**

**Tool: context7_query-docs**

---

## Assistant

**Tool: read**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: bash**

**Tool: write**

---

## Assistant

**Tool: open_plan**

**Tool: plan_exit**

---

