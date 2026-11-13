# Cold Zombie Freezing Mechanics — Refined Implementation Plan (S-0104)

**Target branch:** `feat/variant-state-layer`  
**Stack:** Minecraft Java 26.1.2, Fabric Loader 0.19.5, Fabric API 0.155.3+26.1.2, Java 25, Loom 1.18-SNAPSHOT  
**Plan status:** READ-ONLY refinement — **NOT READY FOR CODE** (critical 26.1.2 source/API verification and runtime validation items remain; see Section 8). No implementation authorized.  
**Saved plan:** `.kilo/plans/1794458343114-cold-zombie-freezing-mechanics-plan.md`  
**Session record:** `.kilo/sesions/S-0104_plan_session-cold-zombie-refinement.md` (continuation record created in this refinement; no pre-existing S-0104 file found in `.kilo/sesions/` during inspection)

## 1. Approved gameplay behavior (user-established decisions)

1. **Cryogenic Body (powder snow immunity, `cold_zombie` only):** cold zombies must not accumulate freezing points from powder snow and must not receive vanilla periodic freezing damage. Immunity applies exclusively to the `cold_zombie` variant (identified via existing `VariantAttachments.VARIANT_TEXTURE`). All other entities retain normal vanilla powder snow behavior. Do not interfere with unrelated damage or entity mechanics.
2. **Shared freezing accumulation (single counter):** use existing vanilla `Entity.ticksFrozen` as the only freezing counter. Powder snow preserves vanilla accumulation behavior and never resets/replaces existing freezing progress. Every successful cold zombie melee hit against a `LivingEntity` adds exactly 40 points to the target's current `ticksFrozen` (additive, never replace). Repeated successful hits accumulate. Preserve vanilla freezing thresholds, visuals, recovery, and periodic damage. No other freezing-duration/accumulation system.
3. **Important distinction:** a cold zombie's powder snow immunity must not automatically prevent it from receiving freezing points through another cold zombie's attack.
4. **Slowness:** Slowness I, 120 ticks (6 seconds), applied on successful melee hits. Repeated hits refresh without indefinitely accumulating duration. Preserve an existing stronger or longer Slowness effect when replacing it would be unnecessary. Precise refresh/update conditions must use Minecraft 26.1.2 APIs.
5. **Example:** existing progress 60 + successful cold zombie attack (+40) = 100; remaining in powder snow continues vanilla accumulation from 100.

## 2. Technically verified behavior (supported by prior repository evidence / S-0104 preserved findings)

> Evidence basis reused (not re-derived by local 26.1.2 mapped-source generation in this session): prior read-only investigations in `.kilo/sesions/S-0101_*`, `.kilo/sesions/S-0102_*`, and prior corrected plan citing mapped MC 26.1.2 classes (`net.minecraft.world.entity.Entity`, `LivingEntity`, `Mob`, powder-snow freeze effect/`InsideBlockEffectType.FREEZE`).

| Finding | Verified detail | Evidence |
|---|---|---|
| Variant identity | Server-resolved texture stored in `VariantAttachments.VARIANT_TEXTURE` (persistent+synced `Identifier`); selection in `VariantStateLifecycle`; cold zombie texture `mob_variants_brs:textures/entity/zombie/zombie_cold.png` (`data/mob_variants_brs/variants/cold_zombie.json`). | `src/main/java/.../attachment/VariantAttachments.java`, `variant/VariantStateLifecycle.java`, `MobVariantsBRS.java`, `variants/cold_zombie.json` |
| No existing mechanics/hooks | No common mixins registered (`mixins: []`), no `src/main/.../mixin/` package, no attack/freezing mechanic code. | `src/main/resources/mob_variants_brs.mixins.json`, `src/` inspection |
| Freeze state fields/API (prior) | `getTicksFrozen()`/`setTicksFrozen(int)` backed by synced entity data; `isFullyFrozen()` >= `getTicksRequiredToFreeze()` (140); setter not observed to clamp to 140; >140 storable/syncable/persistable but not produced by vanilla powder-snow path. | S-0101/S-0102 traces reused in prior plan |
| Natural recovery/order (prior) | In server `LivingEntity.baseTick()` freezing block: if `!isInPowderSnow || !canFreeze()` then `setTicksFrozen(max(0, getTicksFrozen()-2))`; periodic freeze damage when `tickCount % 40 == 0 && isFullyFrozen() && canFreeze()` via `hurtServer(...freeze..., 1.0F)`; recovery precedes damage check in referenced order. | S-0101/S-0102 |
| Powder snow accumulation (prior) | `InsideBlockEffectType.FREEZE` sets in-powder-snow state and, if `canFreeze()`, `setTicksFrozen(min(getTicksRequiredToFreeze(), getTicksFrozen()+1))` (+1/tick, clamps to 140). | S-0101/S-0102 |
| Melee success hook (prior) | `Mob.doHurtTarget(ServerLevel, Entity)` performs real melee damage and returns success (`true` when damage applied); `LivingEntity.doHurtTarget` is non-damaging stub. Server-context method. | S-0101/S-0102 |
| Shared-counter implication | Direct additive write `setTicksFrozen(getTicksFrozen()+40)` composes with existing progress (powder snow or prior hits) because it reads current native value; vanilla subsequent updates (recovery/clamp/damage) continue to operate on same counter. | Logical consequence of verified API model (additive, not replacement) |
| Build/test infra | `build.gradle` configures Loom/fabricApi GameTests, `verifyVariantAttributes` task, standard `build/runServer/runClient`; `VariantResourceGameTest` exists. | `build.gradle`, `docs/TESTING.md`, `src/gametest/...` |

## 3. Unresolved assumptions and implementation-time verification gates (must be resolved in 26.1.2 before/during implementation)

### 3.1 Cryogenic Body mechanism — `LivingEntity.canFreeze()` (CRITICAL)
- **UNRESOLVED:** Whether overriding/intercepting `canFreeze()` in 26.1.2 is sufficient to block **both** powder-snow freezing accumulation and vanilla periodic freezing damage for cold zombies, without affecting unrelated damage.
- **UNRESOLVED (key distinction):** `canFreeze()` gates environmental/vanilla eligibility paths in prior traces, but `setTicksFrozen(...)` is a direct setter; manual `+40` writes from another cold zombie's attack are expected to remain possible because they do not depend on victim `canFreeze()`. **This must be explicitly verified** (a cold zombie victim with `canFreeze()==false` must still be able to receive `setTicksFrozen(current+40)` and show/retain the value subject to vanilla recovery rules).
- **UNRESOLVED:** Exact 26.1.2 call sites/signature of `canFreeze()` and whether any freeze accumulation/damage path bypasses it.
- **Fallback (smallest safe, only if verified necessary):** if `canFreeze()` is insufficient, use the narrowest additional targeted interception on the proven bypassing path (prefer API/event over mixin where sufficient). Do not introduce mixins unless native API/event is demonstrably insufficient.

### 3.2 Existing `ticksFrozen` when entity cannot freeze / recovery consequences
- **UNRESOLVED/ASSUMPTION (from prior traces):** referenced logic applies recovery when `!isInPowderSnow || !canFreeze()`, implying a cold zombie that becomes/ is unable to freeze would decay existing `ticksFrozen` by 2/tick and would not receive periodic freeze damage (damage gate requires `canFreeze()`). 
- **Assessment for approved design:** acceptable for Cryogenic Body (cold zombie should not remain environmentally frozen or take freeze damage), but it means **a cold zombie cannot "hold" freeze progress over time if it is permanently freeze-ineligible**. However, receiving an attack-based `+40` write is still possible at the moment of hit; whether that progress persists across ticks depends on verified recovery behavior under `canFreeze()==false`. This is a **runtime-relevant consequence** that must be documented and validated, not "fixed" with extra state.
- **UNRESOLVED:** Behavior if an entity has nonzero `ticksFrozen` and later becomes unable to freeze mid-state in 26.1.2 edge cases.

### 3.3 Shared `ticksFrozen` accumulation and vanilla semantics
- **VERIFIED (prior, to re-confirm):** direct modification via `getTicksFrozen()/setTicksFrozen(int)` is the native mechanism; additive writes preserve existing progress.
- **UNRESOLVED:** Exact 26.1.2 mapped names/signatures/access, any internal clamps on set/decrement paths beyond known powder-snow `min(140, current+1)` and recovery `max(0, current-2)`, and confirmation that visual feedback/damage thresholds respond as expected to direct writes above/below 140.
- **Constraint:** no artificial gameplay caps.

### 3.4 Successful melee detection
- **VERIFIED (prior semantics):** `Mob.doHurtTarget(ServerLevel, Entity)` returns success on actual damage.
- **UNRESOLVED:** Exact 26.1.2 mapped signature/descriptor, reliability of `RETURN`/TAIL capture of boolean result in this mapping set, and confirmation it is not triggered for projectiles/unrelated flows in ways that violate "successful melee hit only".
- **Proposed (smallest, repo-consistent):** TAIL/RETURN injection in common mixin `Mob` (package `com.baruc.brs.mobvariants.mixin`) guarded by `return==true`, attacker is cold zombie (`VARIANT_TEXTURE`), target `instanceof LivingEntity`; server-authoritative by call context.

### 3.5 Slowness refresh logic (precise, 26.1.2)
- **UNRESOLVED:** Exact effect holder/constant for Slowness I (mapped name), `MobEffectInstance` constructors/fields access, and `LivingEntity.addEffect(...)` overload semantics in 26.1.2.
- **Required behavior (approved):** apply Slowness I (amplifier 0) for 120 ticks on successful hit; refresh repeated hits without accumulating duration; preserve existing stronger or longer/equal-strength-longer Slowness when replacement would be unnecessary.
- **Planned logic (to implement only with verified API):** query existing Slowness effect on target; if none, add amp 0 / 120. If existing amplifier > 0, preserve. If existing amplifier == 0 and remaining duration >= 120, preserve. If existing amplifier == 0 and remaining duration < 120, add/refresh to 120 (using 26.1.2-correct update call/flags). Avoid unnecessary replacement. **UNRESOLVED until API confirmed.**

### 3.6 Integer overflow protection (no arbitrary gameplay cap)
- **UNRESOLVED:** Vanilla internal representation/limits for `ticksFrozen` in 26.1.2 and whether unguarded `current + 40` can overflow `int`.
- **Smallest safe approach (planned):** saturating addition before `setTicksFrozen` (e.g. `next = (current > Integer.MAX_VALUE - 40) ? Integer.MAX_VALUE : current + 40`). This prevents integer overflow/wrap without introducing an arbitrary gameplay cap below type limits. **ASSUMPTION:** `ticksFrozen` is `int`-backed; verify.

## 4. Coherent implementation design (single system, minimal)

- **Identity helper (RECOMMENDED):** `src/main/java/com/baruc/brs/mobvariants/variant/ColdZombieSupport.java` with `COLD_ZOMBIE_TEXTURE` and `isColdZombie(Entity)` using `VARIANT_TEXTURE` only.
- **Cryogenic Body:** `src/main/java/com/baruc/brs/mobvariants/mixin/LivingEntityColdZombieCryogenicMixin.java` targeting `LivingEntity.canFreeze()` (signature verified) to return `false` only for cold zombies, delegating otherwise. No impact on unrelated damage. Common mixin, server-relevant behavior.
- **Freezing Touch:** `src/main/java/com/baruc/brs/mobvariants/mixin/MobColdZombieAttackMixin.java` targeting `Mob.doHurtTarget(ServerLevel, Entity)` return path: on success, if attacker cold zombie and target `LivingEntity`, `target.setTicksFrozen(saturatingAdd(target.getTicksFrozen(), 40))` then apply Slowness I per verified refresh rules. Server-authoritative.
- **Mixin config:** register both in `src/main/resources/mob_variants_brs.mixins.json` (only required entries).
- **No other files/systems:** no attachments, no tick processing, no counters/maps/timestamps, no victim powder-snow suppression, no rendering/resource/Gradle/dependency/unrelated variant changes.

## 5. Acceptance criteria matrix (evaluation)

| # | Criterion | Status | Evidence / required validation |
|---|---|---|---|
| 1 | Cold zombie does not accumulate environmental freezing from powder snow | **UNVERIFIED** | Prior traces show powder-snow `FREEZE` gated by `canFreeze()` (supports design), but 26.1.2 call sites/bypasses not re-inspected locally. **Runtime test required:** cold zombie in powder snow must not increase `ticksFrozen`. |
| 2 | Cold zombie does not receive vanilla powder snow periodic freezing damage | **UNVERIFIED** | Damage gate includes `canFreeze()` in prior traces; 26.1.2 confirmation and runtime validation required. |
| 3 | Other entities retain normal powder snow behavior | **UNVERIFIED** | Design is variant-guarded; requires source-level guard correctness and runtime regression test. |
| 4 | Cold zombie attacks add exactly +40 to target's existing `ticksFrozen` | **UNVERIFIED** | Additive setter model verified in prior traces; exact 26.1.2 behavior and implementation correctness require source check + runtime/test verification. |
| 5 | Repeated successful attacks accumulate without replacing previous progress | **UNVERIFIED** | Follows from additive read-modify-write on native counter; runtime verification required. |
| 6 | Powder snow and attacks share the same vanilla counter | **VERIFIED (design/prior API)** | Single `Entity.ticksFrozen` via `get/setTicksFrozen` is the only state per approved design and prior evidence; powder snow and direct writes target same field. |
| 7 | Vanilla freezing thresholds, visuals, recovery and damage remain functional | **UNVERIFIED** | Native mechanisms preserved by design (no forced 140, no suppression), but runtime behavior under mixed accumulation and `canFreeze` interception consequences must be validated. |
| 8 | A cold zombie can still receive attack-based freezing progress from another cold zombie | **UNVERIFIED (CRITICAL)** | `setTicksFrozen` is direct and not expected to consult victim `canFreeze()`, but if cryogenic body makes victim freeze-ineligible, recovery may immediately decay progress; 26.1.2 semantics and runtime outcome must be verified. Document consequence if decay occurs. |
| 9 | Slowness I lasts 120 ticks and uses appropriate refresh logic | **UNVERIFIED** | Effect API/constants and refresh semantics in 26.1.2 not yet verified; runtime confirmation required. |
| 10 | No custom freezing state or unnecessary per-tick processing is required | **VERIFIED (plan design)** | Proposed architecture uses only native `ticksFrozen` + effect application on hit + variant-guarded eligibility hook; no attachments/timers/maps/tick loops. |
| 11 | Integer overflow is handled safely without arbitrary gameplay caps | **UNVERIFIED** | Saturating-add strategy defined; `ticksFrozen` type/limits in 26.1.2 and implementation correctness require verification. |
| 12 | All required APIs and method signatures are compatible with Minecraft 26.1.2 | **UNVERIFIED** | No local 26.1.2 mapped-source re-verification in this session; mappings/signatures for `canFreeze`, `doHurtTarget`, freeze accessors, and Slowness APIs must be confirmed. |

## 6. Validation plan (automated vs manual; runtime-dependent items marked)

### Commands (VERIFIED tasks/config; execution environment-dependent)
- `.\gradlew.bat build` — compile/resource processing (**VERIFIED** task).
- `.\gradlew.bat verifyVariantAttributes` — focused checks (**VERIFIED** in `build.gradle`).
- Fabric GameTest task (commonly `.\gradlew.bat runGameTest`) — **ASSUMPTION/UNVERIFIED** exact task name; confirm via task listing.
- Manual: `.\gradlew.bat runServer`, `.\gradlew.bat runClient` (optional `-PautoConnect=localhost:25565`).

### Scenarios (mark runtime as UNVERIFIED until executed)
| Type | Scenario | Expected |
|---|---|---|
| Manual/runtime | Cold zombie stands in powder snow | No environmental `ticksFrozen` gain, no vanilla periodic freeze damage (C1,C2) |
| Manual/runtime | Normal zombie/player in powder snow | Vanilla behavior unchanged (C3) |
| Manual/runtime | Cold zombie hits target with `ticksFrozen=60` | Becomes 100 immediately post-hit; powder snow continues from 100 if applicable (C4,C6) |
| Manual/runtime | Repeated successful hits | Additive +40 each; no replacement (C5) |
| Manual/runtime | Cold zombie hit by another cold zombie | **Critical:** can receive attack-based `+40` (C8); observe recovery/retention given cryogenic eligibility and document |
| Manual/runtime | Slowness refresh cases (none, existing <120 amp0, existing >=120 amp0, stronger/longer) | 120t apply/refresh only when appropriate; preserve stronger/longer (C9) |
| Manual/runtime | Mixed freeze: powder snow + attacks, leave/enter snow | Shared counter, vanilla thresholds/visuals/recovery/damage behavior (C7) |
| Automated (feasible) | Pure logic/helper tests for saturating add and identity predicate | Overflow-safe, correct cold-zombie detection (C11, identity) |
| Automated (if harness allows) | Effect application/refresh and `ticksFrozen` delta on simulated successful hit | C4,C5,C9 indications (harness-dependent) |

## 7. Risks and remaining blockers

- **Critical blocker:** 26.1.2 API/mapping verification not completed locally (T in prior terms: `canFreeze` sufficiency/bypass, `doHurtTarget` signature/capture, `ticksFrozen` access details, Slowness effect API/refresh). Without this, implementation is speculative.
- **Critical gameplay ambiguity to resolve by evidence:** interaction of Cryogenic Body (`canFreeze()==false` for cold zombie) with **receiving and retaining** attack-based freeze progress on a cold zombie victim (C8) given recovery-on-cannot-freeze behavior.
- **Native limitations (accepted):** recovery may preempt damage ticks; powder snow clamps toward 140; >140 can decay; periodic damage not guaranteed per hit.
- **Test feasibility:** GameTest support for melee/powder-snow timing may be limited; manual validation required.

## 8. Final decision

**NOT READY FOR CODE**

**Rationale:** the refined design is coherent and minimal (single vanilla `ticksFrozen` counter, no parallel state, `VARIANT_TEXTURE` identity, server-authoritative hit hook + narrow immunity hook), and useful S-0104 findings are preserved. However, critical 26.1.2-specific source/API questions remain unverified (Cryogenic Body sufficiency and its effect on attack-received freeze progress for cold-zombie victims, exact method signatures/mappings, Slowness refresh API semantics, overflow/type assumptions), and multiple acceptance criteria require runtime validation. A complete plan does not substitute for version-specific technical verification.
