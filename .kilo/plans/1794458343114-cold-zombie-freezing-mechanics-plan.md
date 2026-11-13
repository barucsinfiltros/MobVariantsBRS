# Cold Zombie Freezing Mechanics — Corrected Implementation Plan

**Target branch:** `feat/variant-state-layer`  
**Stack:** Minecraft Java 26.1.2, Fabric Loader 0.19.5, Fabric API 0.155.3+26.1.2, Java 25, Loom 1.18-SNAPSHOT  
**Status:** READ-ONLY planning correction — no source modifications authorized in this session  
**Corrected plan path:** `.kilo/plans/1794458343114-cold-zombie-freezing-mechanics-plan.md`

## 1. Objective and approved gameplay behavior

Implement the `cold_zombie` freezing mechanic using **exclusively Minecraft's native `ticksFrozen` state**.

### A. Cryogenic Body (cold zombie immunity)
- **VERIFIED requirement (approved design):** Cold zombies must never accumulate freezing points from powder snow.
- **VERIFIED requirement:** Cold zombies must never receive vanilla periodic freezing damage.
- **VERIFIED requirement:** Cold zombies must continue receiving unrelated damage normally.
- **VERIFIED requirement:** All other entities' freezing behavior must remain unchanged.
- **VERIFIED requirement:** Variant identification must reuse `VariantAttachments.VARIANT_TEXTURE`; do not introduce another attachment or identity mechanism.

### B. Freezing Touch (successful melee hit by cold zombie)
- **VERIFIED requirement:** After a successful cold zombie melee attack against a `LivingEntity`, on the logical server:
  1. Add exactly 40 points to the victim's native freezing state: `victim.setTicksFrozen(victim.getTicksFrozen() + 40)`.
  2. Apply Slowness I for 120 ticks.
  3. Refresh Slowness duration when another successful hit occurs.
  4. Never accumulate Slowness duration.
- **VERIFIED requirement:** No effect on unsuccessful attacks, unrelated attack types, or other mob variants.
- **VERIFIED requirement:** Native recovery and vanilla freezing interactions remain functional; no custom freezing damage/timers.

### Strict prohibitions (must remain absent)
- No `COLD_FREEZE_REMAINING_TICKS` or any new freezing attachment.
- No independent freezing-duration counters, timers, expiration timestamps, or UUID-based state maps.
- No custom periodic freezing damage.
- No per-tick processing for this mechanic.
- No forcing `ticksFrozen` to remain at 140.
- No suppression of vanilla powder snow behavior for victims.

## 2. Repository and inspection status (VERIFIED)

| Item | Finding | Evidence |
|---|---|---|
| Workspace root | `C:\modding_mc\MobVariantsBRS` | Environment / tool inspection |
| Current branch | `feat/variant-state-layer` | `git rev-parse --abbrev-ref HEAD` |
| HEAD | `a475854 kilo markdown: S-0103_plan_session-ses_e31f.md` | `git log -1 --oneline` |
| Working tree | Clean, branch up to date with origin | `git status --short --branch` → `## feat/variant-state-layer...origin/feat/variant-state-layer` |
| Target branch discrepancy | None | Matches requested `feat/variant-state-layer` |
| Existing user modifications preserved | Yes (none present) | Git status clean; no files modified in this session except the plan file requested |

## 3. Current architecture and existing integration points (VERIFIED)

| Component | Status / responsibility | Evidence |
|---|---|---|
| `VariantAttachments.VARIANT_TEXTURE` | Persistent + synced `AttachmentType<Identifier>` holding resolved variant texture (identity/render proxy). No initializer/copyOnDeath. | `src/main/java/com/baruc/brs/mobvariants/attachment/VariantAttachments.java` |
| `VariantStateLifecycle` | Server-data reload wiring and one-shot `ENTITY_LOAD` selection; guards `isLoadedFromDisk()` and `hasAttached(VARIANT_TEXTURE)`; lazy biome evaluation; sets `VARIANT_TEXTURE` and applies attributes on match. | `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java` |
| Cold zombie definition | `entity_type: minecraft:zombie`, texture `mob_variants_brs:textures/entity/zombie/zombie_cold.png`, snowy/frozen biome conditions. | `src/main/resources/data/mob_variants_brs/variants/cold_zombie.json` |
| Variant id/identity on server | Entity identity for mechanics must be derived from attached texture: cold zombie == `VARIANT_TEXTURE` equals `mob_variants_brs:textures/entity/zombie/zombie_cold.png` (centralized constant recommended). | `VariantAttachments`, `VariantStateLifecycle`, cold variant JSON |
| Common mixin config | `mixins: []` (no common mixins registered yet). | `src/main/resources/mob_variants_brs.mixins.json` |
| Client mixin config | Render-only mixins present (`LivingEntityRenderStateMixin`, `LivingEntityRendererMixin`). | `src/client/resources/mob_variants_brs.client.mixins.json` |
| Existing attack/combat handling | None found in mod sources. | Source inspection (`src/**/*.java`) |
| Existing cold-zombie mechanics/freezing code | None found. | Source inspection |
| Test infrastructure | Fabric GameTest entrypoint and `VariantResourceGameTest` exist; focused attribute verification harness referenced in docs/scripts. No existing cold-zombie freezing tests found. | `src/gametest/java/.../VariantResourceGameTest.java`, `docs/TESTING.md`, `src/test/.../VariantAttributeVerification.java` |

## 4. Prior research reused and availability (VERIFIED / limitation)

| Document | Availability | Reuse notes |
|---|---|---|
| `.kilo/plans/1794458343114-cold-zombie-freezing-mechanics-plan.md` (pre-correction) | **Available** and corrected in-place by this task. | Contained useful verified vanilla/fabric notes but **architecturally defective** (introduced `COLD_FREEZE_REMAINING_TICKS`, per-tick countdown/forcing, powder-snow clamp suppression). All such elements removed from corrected plan. |
| `.kilo/sesions/S-0103_plan_session-ses_e31f.md` | **Available** | Prior planning session; largely incomplete/tool-trace style but confirms intent and repo context. |
| `.kilo/sesions/S-0102_ask_session-ses_e320.md` | **Available** | Detailed read-only investigation concluding native-only duration accumulation is not fully feasible without independent tracking; **this conflicts with the newly approved design**. Findings about vanilla lifecycle/order are reused where evidenced; conclusion is not adopted as a blocker to this approved native-`ticksFrozen` plan. |
| S-0103 "session report" (separate non-plan artifact) | **Not found as a distinct file** beyond S-0103 plan session above. | **Limitation documented:** if a separate S-0103 report exists elsewhere, it was not located under `.kilo/sesions` or repository search in this session. |

## 5. Verified Minecraft 26.1.2 native freezing mechanics (VERIFIED from prior inspected/referenced mapped sources)

> Source basis reused from prior investigations (S-0101/S-0102 excerpts in repo sessions and pre-correction plan): mapped `net.minecraft.world.entity.Entity`, `LivingEntity`, `Mob`, `InsideBlockEffectType`/powder snow behavior, damage sources. Local generated MC sources were not re-extracted in this read-only session; signatures/behavior below are treated as **VERIFIED by cited prior evidence** unless marked otherwise.

| Mechanic | Vanilla behavior | Evidence basis |
|---|---|---|
| Freeze threshold | `BASE_TICKS_REQUIRED_TO_FREEZE = 140`; `getTicksRequiredToFreeze() == 140` | Prior verified notes (Entity) |
| Full freeze | `isFullyFrozen()` => `getTicksFrozen() >= getTicksRequiredToFreeze()` (>=140) | Prior verified notes |
| Getter/setter | `getTicksFrozen()` / `setTicksFrozen(int)` operate on synced entity data; setter does not inherently clamp to 140 in inspected behavior | Prior verified notes |
| Values > 140 | **Can be stored/synced/persisted** as integer state; not produced by vanilla powder-snow accumulation path (which clamps). | Prior verified notes |
| Natural recovery | In `LivingEntity.baseTick()` server-side freezing block: if not in powder snow or cannot freeze, `setTicksFrozen(max(0, getTicksFrozen()-2))` (−2/tick). | Prior verified notes |
| Powder snow accumulation | `InsideBlockEffectType.FREEZE` sets in-powder-snow state and, if `canFreeze()`, `setTicksFrozen(min(getTicksRequiredToFreeze(), getTicksFrozen()+1))` (+1/tick, clamped to 140). | Prior verified notes |
| Periodic freeze damage | Server-side in `LivingEntity.baseTick()`: if `tickCount % 40 == 0 && isFullyFrozen() && canFreeze()` then `hurtServer(... freeze damage source ..., 1.0F)`. | Prior verified notes (`FREEZE_HURT_FREQUENCY=40`) |
| Sync | `ticksFrozen` is synced entity data (freeze overlay/client visuals driven by vanilla state). | Prior verified notes |
| Persistence | Saved/loaded as `TicksFrozen` NBT when applicable. | Prior verified notes |
| Slowness/frost visuals | Vanilla applies frost/speed effects based on frozen percent/state in freezing logic. | Prior verified notes (secondary to this mechanic) |

### Native ordering and implications (VERIFIED by prior analysis)
- Recovery/adjustment, frost/visual updates, and periodic damage checks occur in `LivingEntity.baseTick()` freezing logic on server in a fixed order (recovery before periodic damage check in referenced analysis).
- **Implication:** A direct `+40` can push the victim to/above threshold, but natural −2/tick recovery and the 40-tick damage gate mean **not every hit/accumulation guarantees immediate or subsequent periodic freeze damage**. This is accepted vanilla behavior; plan must not promise guaranteed damage per hit.
- **Powder snow clamp interaction:** If victim is in powder snow, vanilla accumulation clamps toward 140 (`min(140, current+1)` when eligible). Therefore values >140 can be reduced toward 140 by subsequent powder-snow ticks. **Accepted vanilla behavior**; do not add independent state to work around it.

## 6. Corrected architecture (native state only)

### 6.1 Cryogenic Body — technical design
**Goal:** make cold zombies immune to powder-snow freezing accumulation and vanilla periodic freezing damage without affecting unrelated damage or other entities.

**Variant identification (REQUIRED):** treat attacker/self as cold zombie only when `entity.hasAttached(VariantAttachments.VARIANT_TEXTURE)` and attached texture equals `mob_variants_brs:textures/entity/zombie/zombie_cold.png` (or shared constant). Reuse existing attachment; no new identity mechanism.

**Candidate integration:** narrowly scoped common Mixin on `LivingEntity.canFreeze()` (or equivalent effective override point in 26.1.2 hierarchy) to return `false` for cold zombies only.
- **Why plausible (VERIFIED rationale from prior source traces):** powder snow freeze accumulation is gated by `canFreeze()`; periodic freeze damage is gated by `isFullyFrozen() && canFreeze()`; thus a false result for cold zombies blocks those behaviors.
- **Important behavioral note (ASSUMPTION pending source re-check in impl):** `baseTick` recovery applies when not in powder snow **or** cannot freeze in referenced logic; returning false may accelerate decay of any existing `ticksFrozen` on a cold zombie. This is consistent with immunity (cold zombie should not remain freezing) but must be verified during implementation against 26.1.2.
- **Safety:** must not alter `hurtServer`/general damage paths; only freezing eligibility. Must be guarded to cold-zombie texture only.

**Fallback if insufficient (UNRESOLVED):** if inspection shows any freeze accumulation/damage path bypasses `canFreeze()` for `LivingEntity` in 26.1.2, use the smallest additional targeted hook on that specific path (still no new state, no per-tick global logic, no victim behavior changes except as required by vanilla semantics).

### 6.2 Freezing Touch — technical design
**Goal:** on successful cold zombie melee hit, mutate victim native freeze state and refresh Slowness.

**Integration point:** existing/common melee attack path `Mob.doHurtTarget(ServerLevel, Entity)` (server-level context) with TAIL/return-value-aware injection.
- **VERIFIED (prior):** real melee damage logic is in `Mob.doHurtTarget`; `LivingEntity.doHurtTarget` is a non-damaging stub; method returns success (`wasHurt`/boolean) indicating damage application success in inspected sources.
- **Required semantics:** execute only when method returns successful hit (`true`), attacker is cold zombie (by `VARIANT_TEXTURE`), target is `LivingEntity`, and logic runs server-side. Ignore misses/unsuccessful attacks and unrelated interactions.

**Behavior on success (server-side):**
1. `int current = victim.getTicksFrozen();`
2. `int next = saturatingAdd(current, 40)` (overflow-safe; e.g. clamp to `Integer.MAX_VALUE` if addition would overflow). **RECOMMENDATION:** use explicit overflow-safe arithmetic; document choice.
3. `victim.setTicksFrozen(next);` (no arbitrary gameplay cap; do not force to 140).
4. Apply `Slowness I` (`MobEffectInstance` amplifier 0) for 120 ticks with refresh semantics (reapplying replaces/refreshes duration per vanilla effect rules; must not accumulate beyond 120). **ASSUMPTION:** standard `LivingEntity.addEffect(...)` refresh behavior in 26.1.2 matches this; verify API signature/flags during implementation.

**No custom state:** do not store remaining freeze duration, timestamps, counters, or per-victim maps. `ticksFrozen` is the only freezing state.

## 7. Verified / assumed / unresolved technical points

| ID | Point | Status | Notes |
|---|---|---|---|
| T1 | `Entity` freeze API (`get/setTicksFrozen`, `isFullyFrozen`, threshold 140, sync, NBT) in 26.1.2 | VERIFIED | Reused from prior mapped-source investigations (S-0101/S-0102/pre-correction). |
| T2 | `LivingEntity.baseTick` freezing order: recovery (-2), frost, periodic freeze damage every 40 ticks when fully frozen & can freeze | VERIFIED | Prior evidence; critical for expectations. |
| T3 | Powder snow freeze effect: +1/tick, `canFreeze()` gated, clamps to `min(requiredToFreeze, current+1)` | VERIFIED | Explains clamp-to-140 behavior. |
| T4 | `Mob.doHurtTarget(ServerLevel, Entity)` is correct successful melee hook and return indicates hit success | VERIFIED (prior) / **ASSUMPTION for exact 26.1.2 mapping/injection capture details** | Confirm exact signature, mappings, and reliable capture of return/local at injection point during implementation. |
| T5 | `LivingEntity.canFreeze()` interception sufficient for Cryogenic Body without breaking unrelated damage | ASSUMPTION (high confidence) | Supported by traced vanilla gates; must be revalidated against 26.1.2 sources/call sites before/while implementing. |
| T6 | Slowness effect id/class and `addEffect` overload/refresh semantics in 26.1.2 | UNRESOLVED (API detail) | Verify correct `MobEffects` constant and effect application call; requirement is refresh-to-120, no accumulation. |
| T7 | Best Mixin injection style to read boolean return of `doHurtTarget` at TAIL | ASSUMPTION | Standard Mixin capability (`@Inject` at `RETURN`/`TAIL` with `@Return`/callback info or locals as applicable) but must be verified for target mapping. |
| T8 | Whether any other vanilla path increments freeze or deals freeze damage independent of `canFreeze()` for cold zombies | UNRESOLVED | Requires source trace during implementation if T5 uncertain. |
| T9 | Local 26.1.2 mapped/generated sources present for direct citation | UNRESOLVED in this session | Not re-generated/inspected locally now; prior session evidence reused. |

## 8. Proposed implementation file list (exact, minimal; no files created now)

### Files likely to modify
| Repo-relative path | Current responsibility | Proposed modification | Reason | Mixin necessary? | Dependencies |
|---|---|---|---|---|---|
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java` **or** a new small common helper (see note) | Server-side variant selection and attribute application; central place aware of variant identity semantics. | Add/reuse a tiny helper to test cold-zombie identity by `VARIANT_TEXTURE` constant (or centralize texture id) for use by mechanics. Prefer a minimal dedicated helper only if it avoids duplication and fits package structure. | Shared, single-source identity check; avoids scattering texture literals. | No | `VariantAttachments`, `Identifier` |
| `src/main/resources/mob_variants_brs.mixins.json` | Declares common mixins. | Register only the required common mixin(s) (expected: `LivingEntity` cryogenic-body mixin and/or `Mob` attack mixin). | Enable interception points. | N/A (config) | Mixin classes |
| `src/main/java/com/baruc/brs/mobvariants/mixin/<ColdZombieCryogenicMixin>.java` (new, if needed) | N/A currently. | Narrowly intercept freeze eligibility for cold zombies (candidate: `LivingEntity.canFreeze()`), returning false only for cold-zombie variant. | Cryogenic Body with minimal surface area. | **Yes, likely** | `VariantAttachments`, cold texture constant/helper, MC `LivingEntity` |
| `src/main/java/com/baruc/brs/mobvariants/mixin/<MobColdZombieAttackMixin>.java` (new, if needed) | N/A currently. | TAIL/RETURN hook on `Mob.doHurtTarget(ServerLevel, Entity)`; on successful hit by cold zombie vs `LivingEntity`, add 40 to victim `ticksFrozen` (overflow-safe) and apply refreshed Slowness I 120 ticks server-side. | Freezing Touch at smallest reliable melee-success point. | **Yes, likely** | `VariantAttachments`, helper/constants, MC `Mob`/`LivingEntity`/effects API |
| `src/gametest/java/com/baruc/brs/mobvariants/gametest/VariantResourceGameTest.java` **or** new focused gametest class under same package (if infrastructure supports) | Existing resource/gametest patterns. | Add automated scenarios feasible within current GameTest APIs for hit accumulation, Slowness refresh, cryogenic immunity aspects, and server-side behavior. | Validation coverage. | No | Existing gametest harness/Fabric gametest API |
| `docs/TESTING.md` (documentation only, if implementation session updates docs) | Test/validation documentation. | Document new manual/automated validation steps for cold zombie mechanics. | Traceability. | No | N/A |

> **Note:** exact mixin/helper class names/packages are **RECOMMENDATION**; choose names consistent with existing `com.baruc.brs.mobvariants.*` conventions. Do not create unnecessary abstractions.

### Explicitly excluded / removed from previous defective plan
| Removed item | Why prohibited |
|---|---|
| `attachment/ColdZombieAttachments.java`, `COLD_FREEZE_REMAINING_TICKS`, attachment registration/init changes for freeze state | New freezing attachment / independent state — **prohibited**. |
| `variant/cold/ColdZombieMechanics.java` tick/countdown logic or any per-tick freeze handler | Timers, countdowns, per-tick processing — **prohibited**. |
| Mixin logic to force `ticksFrozen >= 140`, manage remaining, suppress victim powder-snow clamp, or drive damage cadence | Custom freeze control/timers — **prohibited**. |
| Any UUID maps, expiration timestamps, custom periodic freeze damage | Prohibited alternative state/damage systems. |
| Gradle/dependency/config changes unrelated to required mixin registration | No unrelated changes. |

## 9. Mixin justification and review

| Candidate | Target / point | Required behavior | Why APIs/mixins needed | Side/exec constraints | Compatibility risks | Necessary? |
|---|---|---|---|---|---|---|
| Cryogenic Body mixin | `LivingEntity.canFreeze()` (candidate, **verify 26.1.2**) — inject at HEAD/override-like cancellable return for cold zombies | Return `false` only for entities whose `VARIANT_TEXTURE` is cold zombie texture; otherwise delegate to vanilla | Vanilla freeze accumulation/damage gates flow through eligibility; no Fabric event provides equivalent variant-specific freeze immunity without global side effects | Server-relevant logic but method may be consulted broadly; keep check cheap and side-effect free | Mapping/name changes across versions; unintended interactions if other mods assume `canFreeze()` semantics for this entity (low, variant-specific). Risk of affecting decay behavior (see T5). | **Likely necessary** if `canFreeze()` is the true gate (ASSUMPTION). |
| Freezing Touch mixin | `Mob.doHurtTarget(ServerLevel, Entity)` at `RETURN`/TAIL with access to boolean success | On `true`, if attacker is cold zombie and target is `LivingEntity`, add 40 to target native `ticksFrozen` (overflow-safe) and apply refreshed Slowness I 120 ticks server-side | No existing mod hook; Fabric damage events do not map cleanly to "successful melee hit by this mob type" with same precision as `doHurtTarget` result | Run only on logical server; no client code | Injection brittleness if mapped signature differs; ensure not triggered on non-melee/unrelated paths (guarded by method semantics + success). | **Likely necessary** |

**Preference:** avoid additional mixins (e.g. no tick mixins, no powder-snow effect mixins). If `canFreeze()` proves insufficient for Cryogenic Body, choose the smallest additional targeted interception on the specific bypassing path (UNRESOLVED until verified).

## 10. Updated acceptance criteria (verifiable)

### Cryogenic Body
- [ ] Cold zombies cannot accumulate freezing points from powder snow.
- [ ] Cold zombies cannot receive vanilla periodic freezing damage.
- [ ] Cold zombies remain vulnerable to unrelated damage (melee, fire, fall, etc., as applicable).
- [ ] Other entities retain normal freezing behavior (including in powder snow).
- [ ] Variant identification uses existing `VARIANT_TEXTURE` identity only.

### Freezing Touch
- [ ] A successful cold zombie melee attack adds exactly 40 to the victim's previous `ticksFrozen` (overflow-safe arithmetic applied if needed).
- [ ] An unsuccessful attack adds nothing and applies no Slowness from this mechanic.
- [ ] Slowness I lasts 120 ticks.
- [ ] Repeated hits refresh Slowness duration instead of accumulating it.
- [ ] Gameplay changes occur server-side.
- [ ] Other mob variants remain unaffected.

### Native State and vanilla preservation
- [ ] No independent freezing state exists (no extra attachments, counters, timers, maps).
- [ ] No custom freezing timer or per-tick mechanic processing exists.
- [ ] Native recovery (-2/tick when applicable) remains functional.
- [ ] Powder snow interactions for victims remain functional (including clamp-to-140 behavior when applicable).
- [ ] Vanilla synchronization and persistence of `ticksFrozen` remain intact.
- [ ] Values above 140 are handled only as native `ticksFrozen` values; no forced hold at 140.

## 11. Validation scenarios

### Automated tests (adapt to existing infrastructure; do not invent APIs)
**Existing infrastructure (VERIFIED):** Fabric GameTest (`VariantResourceGameTest`), attribute verification task/tests per docs. Feasibility of melee simulation/powder snow in current gametest helpers is **ASSUMPTION** and may require manual fallback.

| # | Scenario | Expected | Feasibility note |
|---|---|---|---|
| A1 | Victim `ticksFrozen=0`, successful cold-zombie melee hit | `ticksFrozen == 40` (server-side), Slowness I applied 120t | Automate if helper can trigger successful mob melee |
| A2 | Initial 40 -> successful hit | becomes 80 | Same |
| A3 | Initial 100 -> successful hit | becomes 140 | Same |
| A4 | Initial 140 -> successful hit | becomes 180 (subject to native later updates like recovery/powder-snow clamp) | Same; assert immediate post-hit value |
| A5 | Extreme/high value overflow case | Saturating/overflow-safe result, no wraparound corruption | Unit-like logic test if extracted helper exists |
| A6 | Unsuccessful melee attack | No `ticksFrozen` delta, no Slowness from mechanic | Depends on ability to simulate miss/fail |
| A7 | Slowness application | Amplifier 0, duration 120 ticks | Automate if effect queryable |
| A8 | Repeated hits refresh Slowness | Duration refreshed to 120, not stacked beyond | Automate if tick-steppable |
| A9 | Natural recovery outside powder snow | Decreases by 2/tick per vanilla behavior (when eligible) | May be environment-dependent |
| A10 | Powder snow accumulation | +1/tick and clamps at 140 per vanilla (victim) | May require powder-snow setup |
| A11 | Cold zombie in powder snow | Does not accumulate freeze points from powder snow | Core cryogenic test |
| A12 | Cold zombie freeze damage immunity | No vanilla periodic freeze damage to cold zombie | Needs tick progression + conditions |
| A13 | Unrelated damage to cold zombie | Still applies normally | Compare with control damage source |
| A14 | Other entities in powder snow | Vanilla freezing behavior unchanged | Regression control |
| A15 | Server-side execution | No client-only mutation required for mechanic correctness | Structural/code-path verification |

### Manual in-game validation
| # | Scenario | Expected native/custom distinction |
|---|---|---|
| M1 | Repeated attacks against a player/victim | Custom: +40 per successful hit, Slowness I refresh 120t. Native: `ticksFrozen` visuals/damage governed by vanilla thresholds/timers/recovery. |
| M2 | Victim already partially frozen | Custom: additive +40 from current value. Native: subsequent recovery/powder-snow may alter value. |
| M3 | Victim fully frozen (>=140) then hit | Custom: immediate +40 (e.g. 180). Native: may still take freeze damage on vanilla 40-tick gate if eligible; value may decay or be clamped by powder snow later. |
| M4 | Natural recovery after leaving powder snow/combat | Native: −2/tick when applicable; eventual thaw. |
| M5 | Powder snow interaction after attack | Native: victim in powder snow gains +1/tick when eligible and clamps toward 140; can reduce >140 over time. Accepted. |
| M6 | Periodic freezing damage timing | Native: only when `isFullyFrozen() && canFreeze()` on 40-tick interval; **not guaranteed immediately** after hit. |
| M7 | Multiplayer server/client synchronization | Native: freeze overlay/slowness reflect server-authoritative synced state. |
| M8 | World save/reload persistence | Native: `ticksFrozen` and effects persist per vanilla/entity rules as applicable. |
| M9 | Compare with non-cold zombie variant | Non-cold attacker must not apply mechanic; cold zombie must have cryogenic immunity. |

## 12. Native limitations and accepted behavior

- **Natural recovery can reduce points before periodic damage:** −2/tick recovery and 40-tick damage interval interact; damage is not guaranteed per hit or per threshold crossing.
- **Powder snow can clamp values above 140:** vanilla `min(140, current+1)` accumulation on eligible ticks can pull >140 down toward 140. Accepted; no workaround state.
- **No guarantee of immediate/additional full-freeze duration:** adding 40 is a direct native increment only; subsequent vanilla updates govern actual duration/damage.
- **Slowness is independent:** Slowness duration is refreshed to 120 ticks on hit but not tied to `ticksFrozen` countdown.
- **Cold zombie in damaging freeze states:** cryogenic body prevents powder-snow accumulation and vanilla periodic freeze damage by eligibility rules; unrelated damage remains.

## 13. Risks and unresolved questions

| Risk / question | Impact | Mitigation / next step |
|---|---|---|
| `canFreeze()` interception semantics differ in 26.1.2 than assumed | Cryogenic Body incomplete or side effects (e.g. unwanted decay behavior nuances) | **Implementation-time source verification required**; adjust to smallest targeted hook. |
| `Mob.doHurtTarget` mapping/injection capture differs | Freezing Touch not applied reliably | Verify mapped name/signature and return capture strategy in dev environment. |
| Slowness API details (`MobEffects.MOVEMENT_SLOWDOWN`/equivalent, addEffect params) | Incorrect duration/amplifier/refresh | Verify exact 26.1.2 effect API during implementation. |
| GameTest cannot simulate successful melee/powder snow reliably | Reduced automated coverage | Rely on documented manual matrix; add feasible unit-style checks where possible. |
| Attack method coverage | Some modded/edge melee paths may bypass `Mob.doHurtTarget` | **ASSUMPTION:** requirement targets standard melee hit path; document if gaps found. |
| Interaction with other mods altering freezing/attack hooks | Compatibility risk | Keep mixins narrow, variant-guarded, and server-side. |

## 14. Implementation sequence for a future Code session

1. **Pre-implementation verification (blocking):** inspect mapped 26.1.2 sources for `Entity/LivingEntity` freezing methods, `LivingEntity.baseTick` order, powder-snow freeze effect, `Mob.doHurtTarget` signature/return semantics, and `MobEffects`/Slowness API.
2. **Identity helper:** centralize cold-zombie texture `Identifier`/predicate using `VARIANT_TEXTURE` (no new attachment).
3. **Cryogenic Body:** implement narrow common mixin for `canFreeze()` (or verified equivalent) guarded to cold zombie only; confirm unrelated damage unaffected.
4. **Freezing Touch:** implement common mixin on `Mob.doHurtTarget` success path to add 40 native ticks (overflow-safe) and refresh Slowness I (120 ticks) server-side for cold zombie attacker vs `LivingEntity` target.
5. **Register mixins:** update `mob_variants_brs.mixins.json` with only required entries.
6. **Automated tests:** add/extend GameTests for feasible scenarios (A1–A15 as applicable) using existing harness patterns.
7. **Manual validation:** execute M1–M9 on client/server as relevant.
8. **Cleanup/review:** confirm no prohibited state, no per-tick processing, no unrelated refactors; run `./gradlew build` and available verification tasks.

## 15. Final readiness assessment

**Corrected plan status:** **READY FOR A FUTURE CODE SESSION (with implementation-time verifications)** provided the implementer confirms T4/T5/T6/T7 against the actual 26.1.2 mapped sources/APIs before coding. The architectural defects from the prior plan (independent freeze attachment/tick-driven countdown/forced 140/powder-snow suppression) have been removed; the corrected design uses **native `ticksFrozen` only** and respects all stated prohibitions.
