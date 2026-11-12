# Cold Zombie Mechanics — Technical Design & Implementation Plan

**Target branch:** `feat/variant-state-layer` (plan only; no source changes in this session)  
**Stack (verified):** Minecraft Java 26.1.2, Fabric Loader 0.19.5, Fabric API 0.155.3+26.1.2, Java 25, Loom 1.18-SNAPSHOT — `gradle.properties:8-17`, `build.gradle:53-57,69-70`, `src/main/resources/fabric.mod.json:32-37`.

---

## A. Repository status

- **Verified workspace root:** `C:\modding_mc\MobVariantsBRS` (environment / `Get-Location` context).
- **Current branch:** `feat/variant-state-layer` (`git branch --show-current`).
- **HEAD commit:** `027dc93 Compilacion v1.0.0` (`git log -1 --oneline`).
- **Working tree status:** clean (`git status --short` returned no output). No uncommitted changes to preserve.
- **Target branch availability:** present locally and remotely — `feat/variant-state-layer`, `remotes/origin/feat/variant-state-layer`.
- **Relevant `.kilo/` documents inspected:** `.kilo/plans/1790996850927-variant-state-layer-architecture.md` (authoritative implemented state-layer design, lifecycle/attachment/sync facts), `.kilo/plans/1790903055742-mob-variants-brs-architecture.md`, `.kilo/plans/1790968433924-mob-variant-texture-rendering-plan.md`, `.kilo/plans/1791428203997-condition-based-variant-selection-plan.md` (present in listing; state-layer plan used as primary context). Recent session logs under `.kilo/sesions/` listed (e.g. S-0098 references `cold_zombie` definition).
- **Project docs inspected:** `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/TESTING.md`, `docs/ROADMAP.md` (listed), `README.md` (listed).

---

## B. Existing architecture

### Variant identity mechanism
- **Authoritative variant selection is server-side, one-shot at entity load.** `VariantStateLifecycle.register()` wires `ResourceLoader.get(PackType.SERVER_DATA)` reload listener, `ServerLifecycleEvents.SERVER_STARTING/STOPPED` for snapshot publication, and `ServerEntityEvents.ENTITY_LOAD` → `selectVariant` — `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java:35-45,60-109`.
- **Variant identity vs render state:** the mod persists/syncs **resolved render texture** (`Identifier`) on the entity as `VARIANT_TEXTURE`, not a variant id. Variant definitions are path-derived (`mob_variants_brs:<name>`) in `VariantSnapshot.byVariantId` but entity state stores texture — `VariantAttachments.java:35-39`, `VariantSnapshot.java:29-53,72-162`, `VariantStateLifecycle.java:94-105`.
- **`cold_zombie` (existing, verified):** `src/main/resources/data/mob_variants_brs/variants/cold_zombie.json` → `entity_type: minecraft:zombie`, texture `mob_variants_brs:textures/entity/zombie/zombie_cold.png`, biome conditions (snowy/frozen/peaks set). Selected like any other variant by first-match in deterministic snapshot order when biome matches (`VariantStateLifecycle.java:78-106`, `VariantSnapshot` ordering). **Implication:** mechanics must key off the *selected cold variant texture/identity mapping for this mob instance*, not biome at runtime.

### Relevant attachments
- `VariantAttachments.VARIANT_TEXTURE` — `AttachmentType<Identifier>`, `.persistent(Identifier.CODEC).syncWith(Identifier.STREAM_CODEC, AttachmentSyncPredicate.all())`, common `src/main` registration, forced init in `MobVariantsBRS.onInitialize()` — `attachment/VariantAttachments.java:35-39`, `MobVariantsBRS.java:24-35`. No `initializer()`, no `copyOnDeath()`.
- `VariantAttachments.SERVER_VARIANT_SNAPSHOT` — non-persistent, non-synced attachment on server `GlobalAttachments` — `attachment/VariantAttachments.java:48-49`.

### Lifecycle hooks
- Data/reload: `VariantDefinitionReloadListener` (`SimpleReloadListener`) loads `data/mob_variants_brs/variants/*.json` (namespace-filtered), builds immutable `VariantSnapshot`, publishes on `SERVER_STARTING` and on reload apply — `variant/VariantDefinitionReloadListener.java:41-159`.
- Entity: single `ServerEntityEvents.ENTITY_LOAD` selection with guards `isLoadedFromDisk()` then `hasAttached(VARIANT_TEXTURE)` — `VariantStateLifecycle.java:60-67`. No tick listeners anywhere (D-013, verified in code/docs).
- Client: render-only mixins carry variant texture through `LivingEntityRenderState` — `client/mixin/LivingEntityRendererMixin.java`, `client/mixin/LivingEntityRenderStateMixin.java`.

### Existing attack handling
- **None found in mod sources.** Grep for `hurt`, `attack`, `doHurtTarget`, `ALLOW_DAMAGE`, `AFTER_DAMAGE`, `ServerLivingEntityEvents` in `src/` returned no matches. Combat hooks are a new integration point.

### Relevant entity and resource classes
- Common: `MobVariantsBRS`, `attachment/VariantAttachments`, `variant/{VariantDefinition,VariantConditions,VariantAttributes,VariantDefinitionReloadListener,VariantSnapshot,VariantStateLifecycle}`.
- Client: `MobVariantsBRSClient`, `client/render/MobVariantsRenderState`, `client/mixin/{LivingEntityRendererMixin,LivingEntityRenderStateMixin}`.
- Data: `src/main/resources/data/mob_variants_brs/variants/cold_zombie.json` (and other zombie variants). Mixins: `src/main/resources/mob_variants_brs.mixins.json` (empty `mixins`), client config lists two render mixins.
- Tests: `src/gametest/java/.../VariantResourceGameTest.java`, `src/test/java/.../verify/VariantAttributeVerification.java`.

### Architectural constraints to preserve
- Server-authoritative; strict client/common separation; no per-tick biome checks/world scans; reuse existing variant identity/attachments; no other variant definitions changed; no new EntityTypes; no arbitrary JSON code; no texture/render architecture changes; minimal, focused classes.

---

## C. Verified API investigation

### What is verified in-repo (this session)
- Variant selection/store/sync lifecycle and attachment patterns as above (source-verified).
- `cold_zombie` definition and texture id: `mob_variants_brs:textures/entity/zombie/zombie_cold.png` (`cold_zombie.json:4`).
- Fabric API jar present in Gradle cache: `fabric-entity-events-v1` 26.1.2 (`.../net/fabricmc/fabric-api/fabric-entity-events-v1/2.2.6+157982a097/fabric-entity-events-v1-2.2.6+157982a097.jar` located under `.gradle`). **Binary contents not inspected in-session due to tool restrictions** (no jar listing/extraction performed) — class/event signatures below are **[AS/OQ] pending decompilation/genSources verification**.
- Minecraft 26.1.2 remapped sources not found in `build/` or `.gradle` during search (`LivingEntity` source absent in workspace caches searched) — **vanilla freezing method signatures/call paths are not source-verified here**.

### Vanilla freezing (required trace) — **NOT FULLY VERIFIED in this environment; must be confirmed against 26.1.2 mapped sources before implementation**
> **[OQ/AS]** Exact 26.1.2 mojmap names/signatures for `LivingEntity` freezing (`ticksFrozen` field, `getTicksFrozen/setTicksFrozen`, `canFreeze`, `isFullyFrozen`, `getTicksRequiredToFreeze`, freeze-damage tick, powder-snow tick path) **could not be confirmed** without generated sources. The plan below assumes the modern vanilla model (freeze counter in `Entity`/`LivingEntity`, `canFreeze()` gate, powder-snow increases freeze, damage when fully frozen on interval, recovery when not in freezing source) and treats every signature/call-site as a pre-implementation verification gate.

**Investigation tasks to complete in implementation session (mandatory):**
1. `Entity`/`LivingEntity` (26.1.2 official mappings): locate `getTicksFrozen()`, `setTicksFrozen(int)`, `getTicksRequiredToFreeze()` (commonly 140), `isFullyFrozen()`, `canFreeze()`/freeze-immune logic, data-tracker/sync of freeze ticks.
2. Powder snow path: `PowderSnowBlock.entityInside` / entity freeze increment and whether it respects `canFreeze()` or separate checks.
3. Freeze damage: source (`DamageTypes.FREEZE` / `damageSources().freeze()` naming in 26.1.2), damage amount (typically 1), interval condition (fully frozen + periodic tick), and call site.
4. Natural recovery: decrement when not in powder snow / not freezing source; confirm whether outside-powder recovery conflicts with “continue after leaving powder snow”.
5. Persistence/sync: whether `ticksFrozen` is saved in entity NBT and synced to clients (vanilla does sync freeze state for overlay).
6. Determine **cap/maximum** of freeze counter and whether >max accumulates meaningfully.

**Working hypothesis to validate:** vanilla `ticksFrozen` is capped near `getTicksRequiredToFreeze()`-scaled bounds and auto-recovers quickly outside powder snow; therefore **Approach A alone is unlikely to satisfy long, proximity-independent accumulated duration**. Must be proven/disproven in source.

### Fabric / effect APIs (to verify exact 26.1.2 signatures at implementation)
- **Status effects:** `LivingEntity.addEffect(MobEffectInstance, @Nullable Entity)` / `addEffect(MobEffectInstance)` and `MobEffects.MOVEMENT_SLOWDOWN`/`MobEffects.SLOWNESS` naming in 26.1.2 mojmap — **[OQ] verify class/method names** (`MobEffectInstance` ctor params: effect holder, duration, amplifier, ambient, visible, showIcon in recent versions). Use vanilla renewal semantics (reapply/refresh instance, do not add durations, do not raise amplifier).
- **Damage/attack events (Fabric entity-events-v1, version present but uninspected):** likely candidates `ServerLivingEntityEvents.ALLOW_DAMAGE`, `AFTER_DAMAGE`, `ALLOW_DEATH` exist in module; **exact signatures for 26.1.2 must be read from `fabric-entity-events-v1` API/sources**. Need callback params (entity, source, amount, and whether event fires on blocked/zeroed hits) to judge “successful hit only”.
- **Tracking/persistence/sync:** already verified for attachments via existing mod usage (`AttachmentType` persistent+sync, `ServerEntityEvents.ENTITY_LOAD`) — reuse same Fabric Data Attachment API.

---

## D. Proposed implementation

### Variant identification for mechanics (critical, server-authoritative)
- **Do not** add biome checks. Identify `cold_zombie` by the entity’s selected variant render state: compare `entity.getAttached(VariantAttachments.VARIANT_TEXTURE)` to the canonical cold texture `Identifier` (`mob_variants_brs:textures/entity/zombie/zombie_cold.png`) or a single centralized constant/derived value. **[AD]** Add a small internal helper (e.g. `ColdZombie`/`ColdZombieMechanics` utility in `main`) that returns `true` only when attachment equals the cold zombie texture (and entity is server-side living zombie context as needed). This reuses existing identity without changing selection/JSON schema.
- **Scope:** only entities carrying the cold_zombie resolved texture. Other zombies have different/no texture attachment → unaffected.

### Mechanic A — Cryogenic Body (passive freeze immunity)

**Recommended hook (preferred):** narrow common Mixin on `LivingEntity` (or the exact class owning freeze logic in 26.1.2) at the point that **gates freeze accumulation** — override/inject `canFreeze()` (or equivalent) to return `false` for cold zombies, **only** when identified via `VARIANT_TEXTURE == cold texture`.  
- **Rationale:** preventing accumulation at the vanilla gate is the cleanest way to stop powder-snow freezing at source and, in most vanilla implementations, also prevents `isFullyFrozen()`/freeze-damage conditions from becoming true from new accumulation. **Must be verified:** trace whether any path increments `ticksFrozen` or deals freeze damage without consulting `canFreeze()` in 26.1.2; if so, add the smallest additional targeted guard (e.g. cancel increment in `setTicksFrozen` path for cold zombies or guard damage application) — **do not cancel all damage**.
- **Event-only alternative:** `ServerLivingEntityEvents.ALLOW_DAMAGE` can cancel freeze damage if source is freeze, but does **not** by itself prevent freeze accumulation/visual buildup unless accumulation is separately blocked. Therefore **event alone is likely insufficient** to satisfy “must not accumulate freezing from powder snow”; use as defense-in-depth only if verified necessary.
- **Constraints honored:** no global disable, no per-tick biome checks, server-authoritative identity, unrelated damage untouched.

### Mechanic B — Freezing Touch

#### 4.1 Renewable Slowness (independent)
- On confirmed successful hit by a `cold_zombie` attacker against a valid `LivingEntity` target (not self): `target.addEffect(new MobEffectInstance(SLOWNESS, 120, 0, false, true, true-or-default))` (exact ctor/API per 26.1.2). **Vanilla effect handling** refreshes duration without summing and without amplifier stacking when reapplied appropriately — rely on `MobEffectInstance`/`addEffect` semantics (verify); do **not** add custom timer.
- **Centralized constants:** `ColdZombieMechanics.SLOWNESS_DURATION_TICKS = 120`, `SLOWNESS_AMPLIFIER = 0`.

#### 4.2 / 4.3 Accumulative Freezing — selected architecture

**Comparison:**

| Approach | Duration accuracy | Vanilla damage/intervals | Visuals | Natural recovery | Powder snow interaction | Sync | Persistence | MP correctness | Risk |
|---|---|---|---|---|---|---|---|---|---|
| **A. Native counter only** | **Poor if capped/auto-decays** (likely): cannot represent 160+ pending ticks independent of proximity if vanilla clamps/recovers outside source. | Native if untouched. | Native. | Native may erase accumulated time outside powder snow (conflicts with spec). | Native. | Native. | Native. | Native. | Fails core “accumulate & persist after leaving powder snow” unless proven otherwise. |
| **B. Separate remaining-duration state only** | **Good** for countdown. | Must reimplement or drive native damage carefully; risk of duplicate/missed damage and desync from visuals. | Needs to force `ticksFrozen` for overlay; can fight vanilla recovery. | Controllable. | Needs coordination. | Custom attachment sync possible (existing API). | Custom attachment. | OK if server-auth. | High integration complexity with vanilla damage/visuals. |
| **C. Controlled vanilla lifecycle integration (separate duration + narrow vanilla hook)** | **Good:** authoritative `remainingFreezeTicks` attachment on victim; countdown independent of proximity. | **Best fidelity if** hook applies vanilla-consistent freeze state/damage cadence (or lets vanilla damage run on controlled frozen state) without double-apply. | Reuse vanilla by driving `ticksFrozen` toward required visual threshold while remaining>0. | When remaining hits 0, stop forcing; allow vanilla recovery to clear naturally. | Our remaining state continues regardless of powder snow; powder snow may also affect native counter but must not reduce our remaining unfairly (define precedence). | Sync `remainingFreezeTicks` (or minimal client-needed state) via Fabric attachment sync; visuals also follow synced `ticksFrozen` if driven server-side. | Persistent attachment. | Server-auth. | **Moderate, but smallest safe path to meet all stated requirements.** |

**Selected: Approach C (controlled integration), pending 26.1.2 source verification.**

**Design:**
- **Victim-side persistent attachment (common):** `COLD_FREEZE_REMAINING_TICKS` (`AttachmentType<Integer>` or small record) — `.persistent(Codec.INT or codec).syncWith(...)` as needed. Default/absent = 0. No initializer; lazy storage. **Server-authoritative writer.**
  - Representation: **remaining ticks** (simpler countdown). Persisted value is remaining duration; on tick decrement by 1 while >0. (Absolute game-time alternative possible but more complex across unload/save; prefer remaining ticks.)
  - On successful cold_zombie hit: `remaining = safeAdd(remaining, FREEZE_TICKS_PER_HIT)` with overflow clamp to `Integer.MAX_VALUE` (and optionally a very high soft cap only for arithmetic safety, **not a gameplay cap** stated as arbitrary — document clamp). **Never overwrite**; only add.
  - Constant: `FREEZE_TICKS_PER_HIT = 40`.
- **Lifecycle/ticking:** register a minimal server-side per-entity tick hook **only if** required by verified vanilla behavior. Preferred order: (1) `ServerTickEvents`/level tick scan is forbidden; (2) `ServerLivingEntityEvents` has no generic tick — so likely `ServerEntityEvents`/`EntityTickEvents` equivalent in Fabric 26.1.2? **[OQ]** verify available tick event; if none suitable, use a narrow mixin on `LivingEntity.tick`/`aiStep`/freeze-tick method **scoped to entities with remaining>0** (early-out if attachment absent/0) to: decrement remaining, and coordinate `ticksFrozen`.
- **Vanilla integration rules (to prove in source):**
  - While `remaining > 0`: ensure entity is treated as freezing for visuals/damage progression consistent with vanilla (e.g. maintain `ticksFrozen` at/above fully-frozen threshold without breaking damage interval timing). **Do not blindly force max every tick**; instead drive state so vanilla `freeze damage` triggers on its normal interval when fully frozen — or, if impossible, apply controlled freeze damage via vanilla damage source at the same interval and suppress duplicate vanilla freeze damage for that case (last resort, must be justified).
  - While `remaining > 0` and outside powder snow: prevent vanilla recovery from canceling our accumulated duration (remaining drives truth; reconcile `ticksFrozen` downward only as needed but keep fully-frozen state until remaining depleted or per verified safe strategy).
  - When `remaining == 0`: stop forcing; allow normal vanilla recovery path to reduce `ticksFrozen` naturally so visuals thaw correctly.
  - Powder snow + accumulated: remaining continues; do not let powder-snow recovery/logic remove our pending time. If powder snow adds vanilla freeze, it must not conflict (net effect: victim stays frozen while remaining>0).
- **Cryogenic Body interaction:** cold zombies must not accumulate *native* freezing; their own `remainingFreezeTicks` should remain irrelevant unless design later changes (mechanic targets victims of cold zombie hits, not the zombie itself).

### Attack detection (successful melee hit)
- **Requirement:** trigger only after actual successful attack, no miss, no duplicates, not for unrelated sources, not self, valid living targets only, preserve damage.
- **Preferred investigation order:** (1) Fabric `ServerLivingEntityEvents.AFTER_DAMAGE` (if present in 26.1.2) — use only if it provides damage source/attacker and fires only on applied damage (not cancelled/blocked) and not repeatedly; must filter to melee/direct attacker cases and confirm not invoked for misses. (2) `ServerLivingEntityEvents.ALLOW_DAMAGE` is pre-damage/cancel point — unsuitable for “successful hit” semantics. (3) Mixin on `LivingEntity.doHurtTarget(ServerLevel, Entity)` or `Mob.doHurtTarget` success path: inject at `RETURN` and proceed only when method returns `true` (or equivalent success indicator in 26.1.2) — **narrowest reliable success signal for melee attacks**. **[OQ] exact method name/signature and return semantics must be verified** (Yarn/mojmap differences; 26.1.2 uses official mappings).
- **Chosen default plan:** **Mixin tail on the server-side melee hit method returning success** (target class/method verified in impl: likely `net.minecraft.world.entity.LivingEntity#doHurtTarget` or mob-specific override) guarded by `if (!level.isClientSide && returnValue == true)`, attacker has cold_zombie variant, target instanceof `LivingEntity` and != attacker and alive/valid. This avoids “damage event ambiguity” (projectiles, thorns, environmental) if scoped to melee `doHurtTarget`. If 26.1.2 splits logic differently, pick the exact method that corresponds to a completed melee hit.
- **Duplicate avoidance:** single injection at canonical success return; no HEAD+TAIL double-count.
- **Registration:** common mixin config `mob_variants_brs.mixins.json` (currently empty) — add only the minimal required mixin classes.

### State, persistence and synchronization

| State | Owner | Persistent | Synced | Purpose |
|---|---|---:|---:|---|
| `VARIANT_TEXTURE` (existing) | attacker/victim entity | yes | yes | render/variant identity proxy; used to detect cold_zombie attacker |
| `SERVER_VARIANT_SNAPSHOT` (existing) | server global | no | no | definitions |
| `COLD_FREEZE_REMAINING_TICKS` (proposed, new) | **victim** `LivingEntity` | yes | **yes (recommended minimal)** if client needs any mod-side logic; otherwise **no** but rely on vanilla `ticksFrozen` sync for visuals — **[AD pending]** prefer syncing remaining is unnecessary if `ticksFrozen` is authoritatively driven server-side and vanilla syncs it; keep minimal | accumulated pending freeze duration |

- **Registration:** declare in common `attachment` package (e.g. extend `VariantAttachments` or new `ColdZombieAttachments`) and force class init from `MobVariantsBRS.onInitialize()` to satisfy Fabric sync handshake (existing pattern). **Do not** put in client-only source set.
- **Save/reload/dimensions/unload:** Fabric attachments persist with entity NBT and copy by default on dimension transfer; remaining-ticks countdown must be time-based per tick while loaded — on unload ticks do not advance (acceptable, consistent with entity ticking). Document: pending duration counts in loaded ticks.
- **Memory:** no global UUID maps; attachment storage only.
- **Overflow:** `Math.addExact` in try/catch or `long` intermediate then clamp to `Integer.MAX_VALUE`.

### Configuration and balancing (centralized)
- **Proposed new constants holder (common):** `com.baruc.brs.mobvariants.variant.cold.ColdZombieBalance` or similar — `public static final int SLOWNESS_DURATION_TICKS = 120; SLOWNESS_AMPLIFIER = 0; FREEZE_TICKS_PER_HIT = 40;` and `Identifier COLD_ZOMBIE_TEXTURE = Identifier.fromNamespaceAndPath(MobVariantsBRS.MOD_ID, "textures/entity/zombie/zombie_cold.png")` (verify exact path string matches JSON/asset).
- No external config framework.

### Client/common separation
- All mechanics server-authoritative in `src/main`. No client mixins/entrypoint changes. Client already receives `VARIANT_TEXTURE`; freeze overlay should continue to work via vanilla-synced freeze state if driven correctly.

---

## E. Expected file changes

> **All paths below are proposed (plan-time).** Verify existence before editing; create only where justified.

| Path | Existing/new | Purpose | Why it must change | Source set |
|---|---|---|---|---|
| `src/main/java/com/baruc/brs/mobvariants/variant/cold/ColdZombieBalance.java` | **new (proposed)** | Central constants (slowness, freeze-per-hit, cold texture id helper constant) | Requirement §8: centralized, adjustable values; avoid magic numbers | common (`main`) |
| `src/main/java/com/baruc/brs/mobvariants/variant/cold/ColdZombieIds.java` or inline in balance | **new (proposed, optional)** | Single source of truth for cold zombie texture/identity predicate | Cryogenic Body and Freezing Touch must target only `cold_zombie` via existing variant state | common |
| `src/main/java/com/baruc/brs/mobvariants/attachment/ColdFreezeAttachments.java` (or extend `VariantAttachments`) | **new or modify existing** | Victim `COLD_FREEZE_REMAINING_TICKS` attachment registration (persistent, sync decision per §D) | Accumulated duration requires persistent victim state | common |
| `src/main/java/com/baruc/brs/mobvariants/variant/cold/ColdZombieMechanics.java` | **new (proposed)** | Server-side handlers: apply slowness, add freeze ticks (saturating), tick/decrement helper, cold-attacker predicate | Isolate mechanic logic, reuse from mixin/event hooks, keep classes focused | common |
| `src/main/java/com/baruc/brs/mobvariants/mixin/LivingEntityFreezeMixin.java` (name TBD to exact target) | **new (proposed)** | Cryogenic Body: block freeze accumulation/damage for cold zombies; and/or integrate remaining-ticks with vanilla freeze tick/recovery/damage per verified hooks | No reliable event-only path for accumulation+visual+damage fidelity; smallest safe mixin set required | common |
| `src/main/java/com/baruc/brs/mobvariants/mixin/LivingEntityColdAttackMixin.java` (name TBD) | **new (proposed)** | Detect successful melee hit by cold zombie and invoke `ColdZombieMechanics` | Attack detection likely requires success-return hook not exposed by events | common |
| `src/main/resources/mob_variants_brs.mixins.json` | **existing** | Register new common mixin(s) in `"mixins"` array | Enable injections | common resource |
| `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` | **existing** | Force init of new attachment class(es); register mechanic/tick hookup if event-based | Attachment sync handshake requires class init on both sides; wiring | common |
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java` | **existing (maybe)** | Only if lifecycle registration for tick/event helper is centralized there (prefer separate `cold` registrar) | Keep selection concerns unchanged; avoid unnecessary edits | common |
| `src/gametest/java/com/baruc/brs/mobvariants/gametest/ColdZombieMechanicsGameTest.java` | **new (proposed)** | Automated scenarios feasible in headless GameTest (attachment presence, slowness application, remaining-ticks accumulation/countdown, persistence hooks where possible) | Validation §9 | gametest (`src/gametest`) |
| `src/test/java/.../ColdZombieMechanicsVerification.java` (optional) | **new (proposed)** | Pure/unit-style checks if repo pattern used (saturating add, constants) | Fast verification without full MC runtime | test |

**Explicitly not changed:** other variant JSONs, textures, render mixins/client classes, selection algorithm, `VariantDefinition` schema, build deps (unless a missing Fabric module is proven necessary — avoid).

---

## F. Risks and unresolved questions

1. **[OQ - blocking for impl correctness] Vanilla freezing internals (26.1.2):** exact method names/signatures and call paths for `ticksFrozen` get/set, max/cap, `canFreeze`/immunity, `isFullyFrozen`, freeze-damage source/interval, powder-snow increment, natural recovery, NBT keys, client sync — **not verified in workspace**. Must inspect mapped `net.minecraft.world.entity.Entity`/`LivingEntity` and `PowderSnowBlock` before coding.
2. **[OQ] Can Approach A satisfy “accumulated duration independent of proximity/powder snow and > native cap”?** Hypothesis: no; requires proof from source. If yes, simplify to A.
3. **[OQ] Attack hook API:** Fabric `ServerLivingEntityEvents.AFTER_DAMAGE` presence/signature in 0.155.3+26.1.2 and whether it denotes *successful applied* damage for melee; exact `doHurtTarget` signature/return in 26.1.2 mojmap.
4. **[OQ] Tick integration point:** best Fabric/server-safe hook to decrement victim remaining ticks each tick without global scans and without forbidden per-tick world iteration (per-entity tick via mixin on `LivingEntity` base tick is targeted; confirm performance/side effects).
5. **[Risk] Duplicate freeze damage:** if custom damage is added while vanilla also applies, target takes double freeze damage. Mitigation: prefer driving vanilla frozen state so vanilla damage runs once per interval; only add custom damage with explicit suppression proven necessary.
6. **[Risk] Visual/state desync:** forcing `ticksFrozen` incorrectly can break overlay or damage intervals. Mitigation: follow vanilla thresholds/interval logic exactly.
7. **[Risk] `isLoadedFromDisk`/guard ordering (known KL in state layer):** malformed persisted attachments may not self-heal — applies to any new attachment too; choose codecs/validation accordingly.
8. **[Risk] Cold zombie identification depends on texture equality:** if texture id/path changes for `cold_zombie`, mechanics must update constant; consider deriving allowed texture set from snapshot on reload if needed later (not required now).
9. **[Assumption]** `MobEffectInstance` renewal semantics in 26.1.2 refresh duration without stacking amplifier/duration-add — verify API behavior.
10. **[Assumption]** GameTest environment can exercise freeze/powder snow and melee hits reliably in 26.1.2 structures; some scenarios may require manual runtime validation.

---

## G. Validation strategy

### Automated (GameTest / existing harness) — proposed
- **Cryogenic Body (server logic):** spawn/select a zombie in cold biome (or force selection context per existing test helpers) so it becomes `cold_zombie` (`VARIANT_TEXTURE` equals cold texture); place in powder snow or invoke freeze-increment path; assert `ticksFrozen` does not increase over N ticks and no freeze damage taken. Mirror with normal zombie → freezes. *(Exact setup depends on GameTest APIs/structure helpers — verify `GameTestHelper` capabilities.)*
- **Freezing Touch - Slowness:** simulate successful hit from cold zombie to living target (via mob attack goal tick or direct `doHurtTarget` in controlled context); assert Slowness present, duration refreshed, amplifier remains 0 on repeated hits.
- **Accumulation:** apply N hits; assert victim `COLD_FREEZE_REMAINING_TICKS` equals `N * 40` (saturating). Assert countdown over ticks and persistence across save/load if test harness supports entity serialization/reload.
- **Non-regression:** `VariantResourceGameTest` and `verifyVariantAttributes` still pass.

### Manual runtime tests (client/server as applicable)
1. Normal zombie in powder snow accumulates freezing and takes vanilla freeze damage.
2. `cold_zombie` in powder snow: no freeze accumulation, no vanilla freeze damage.
3. `cold_zombie` receives unrelated damage (melee, fall, fire as applicable) normally.
4. `cold_zombie` successful melee hit applies Slowness 6s amp 0.
5. Repeated hits refresh Slowness (no duration sum, no amp increase).
6. Each successful hit adds 40 ticks remaining; 1→40,2→80,3→120,4→160 observed (via debug/log or command inspection).
7. Repeated hits accumulate correctly during rapid attacks.
8. Victim leaves powder snow / moves away from attacker: freezing continues while remaining>0.
9. Remaining counts down correctly to 0; victim thaws/recovers normally afterward.
10. Multiplayer: server-authoritative application/sync; multiple observers consistent.
11. Save/reload with pending remaining duration: resumes correctly (if persisted).
12. Other zombie variants unaffected (no immunity granted, no freeze-on-hit).

### Edge cases
- Rapid consecutive hits (same tick / sequential) — ensure additive, no lost increments.
- Target death/despawn during pending freeze — no leaks, no crashes.
- Entity unload/reload, dimension change — attachment preserved/copied per Fabric semantics; countdown resumes when ticking.
- Integer overflow — saturating add.
- Target already freezing from powder snow before hit — accumulated remaining must extend/coexist without reducing pending unfairly.
- Simultaneous powder snow + accumulated — victim remains frozen; leaving powder snow does not cancel remaining.
- Hits during natural recovery phase — remaining>0 must prevent premature thaw per design.

**Do not claim verification until executed.**

---

## H. Implementation sequence

1. **Pre-impl verification (blocking):** generate/inspect 26.1.2 mapped sources (`./gradlew genSources` / Loom caches) and Fabric `fabric-entity-events-v1` API for exact freezing methods, damage path, and attack event/hook signatures. Record findings in code comments/PR notes (not in this plan session).
2. **Constants & identity helper (common):** add `ColdZombieBalance` (and optional `ColdZombieIds`) with texture constant matching `cold_zombie.json`; no behavior yet.
3. **Victim freeze-state attachment (common):** register `COLD_FREEZE_REMAINING_TICKS` (persistent; sync only if proven necessary), force-init in `MobVariantsBRS`. Add saturating add helper.
4. **Cryogenic Body (common mixin):** implement minimal `LivingEntity` freeze gate/accumulation block for cold zombies; validate against traced vanilla paths (accumulation + damage) with targeted tests.
5. **Accumulated freeze tick integration (common):** implement per-entity countdown and controlled `ticksFrozen`/damage/recovery coordination per verified Approach C rules (mixin on freeze/tick path scoped by attachment/remaining>0).
6. **Attack detection hook (common mixin or verified event):** wire successful-hit callback for cold zombie attacker → apply Slowness (vanilla effect) + add remaining freeze ticks to victim.
7. **Wire & register:** update `mob_variants_brs.mixins.json`, ensure init/registration order; keep server-authoritative.
8. **Automated tests:** add/extend GameTests for feasible scenarios; run `./gradlew build` and gametest task as configured.
9. **Manual validation:** execute matrix in §G on client+dedicated/multiplayer as available; document gaps.
10. **Cleanup:** confirm no per-tick scans, no unrelated changes, constants centralized.

**Dependencies:** step 1 gates 4–6; step 3 gates 5–6; step 6 depends on 2–3; step 8 depends on 3–6.

---

## Explicit exclusions (reaffirmed)
No changes to other zombie variants/biomes/textures/rendering, no selection-architecture rewrite unless strictly necessary, no new EntityTypes, no unrelated attributes/abilities, no arbitrary duration caps, no global freezing behavior changes, no unnecessary deps/config frameworks, no commits/push/resets, and **no application source modifications in this planning session**.

## Central acceptance criterion
Repeated successful attacks by `cold_zombie` must produce genuinely cumulative freezing duration while preserving vanilla freezing damage, visual behavior, and natural recovery as faithfully as technically possible — implemented via the smallest safe, verified integration (Approach C preferred unless source disproves need).

## Final planning status
**Plan complete for handoff, with blocking source-verification items in §F (vanilla 26.1.2 freezing internals, attack hook signature, tick hook choice).** These must be resolved from mapped Minecraft/Fabric sources at implementation start; do not guess APIs.
