# MobVariantsBRS — Next Phase Plan: Condition-Based Variant Selection

Status: **architecture/implementation plan only.** No code, JSON, docs, or build
files were modified by this session. This plan is the contract for a separate
Kilo Code implementation session.

Evidence classification used throughout:

- **[VF]** VERIFIED REPOSITORY FACT — read out of a tracked repository file or
  the git history of `feat/variant-state-layer`.
- **[AD]** ARCHITECTURAL DECISION — decided by this document.
- **[REC]** RECOMMENDATION — proposed, may be overridden by the owner.
- **[AS]** ASSUMPTION — believed true, not pinned.
- **[OQ]** UNRESOLVED QUESTION — must be resolved by the implementation session
  (usually by a compile/runtime verification gate).

Plan basis: `feat/variant-state-layer` @ `74be852` (pushed to
`origin/feat/variant-state-layer`). Authoritative existing architecture:
`.kilo/plans/1790996850927-variant-state-layer-architecture.md` ("state-layer
plan") and `.kilo/plans/1790968433924-mob-variant-texture-rendering-plan.md`
("renderer plan").

---

## 1. Repository Findings

### 1.1 Verified implementation state **[VF]**

The variant-state and custom-texture rendering pipeline is **implemented and
committed** in three commits on top of `main`:

- `ce8eff6` feat: complete zombie variant rendering pipeline
- `53e5988` chore: remove temporary runtime test scripts
- `74be852` chore: remove example mixins and metadata

Tracked implementation files (all read and verified this session):

| File | Role |
|---|---|
| `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` | `ModInitializer`; forces `VariantAttachments` class init on both logical sides (required by Fabric's attachment-sync handshake); calls `VariantStateLifecycle.register()` |
| `src/main/java/com/baruc/brs/mobvariants/attachment/VariantAttachments.java` | `AttachmentType<Identifier> VARIANT_TEXTURE` (`.persistent(Identifier.CODEC)`, `.syncWith(Identifier.STREAM_CODEC, AttachmentSyncPredicate.all())`, no `initializer()`, no `copyOnDeath()`) and `AttachmentType<VariantSnapshot> SERVER_VARIANT_SNAPSHOT` (non-persistent, non-synced, held on `GlobalAttachments`) |
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantDefinition.java` | `record VariantDefinition(Identifier entityType, Identifier texture)`; both fields **required**; `RecordCodecBuilder` codec; no other fields |
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantSnapshot.java` | Immutable record `(byVariantId, byEntityType)`; `Map.copyOf`/`List.copyOf` defensive copies; `EMPTY`; `variantsFor(EntityType<?>)` never null, never allocating; `build()` validates `entity_type` against `BuiltInRegistries.ENTITY_TYPE` via `getOptional` (game thread) |
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantDefinitionReloadListener.java` | Singleton `SimpleReloadListener` on `ResourceLoader.get(PackType.SERVER_DATA)`; `prepare` (off-thread) decodes JSON with `JsonOps`, skips+logs bad files, sorts ascending by variant id; `apply` (game thread) builds and publishes the snapshot via one `server.globalAttachments().setAttached(...)` reference swap; `attach(server)` at `SERVER_STARTING` publishes the startup snapshot (initial pack load runs before `SERVER_STARTING`); `detach()` at `SERVER_STOPPED`; namespace policy: only `mob_variants_brs` namespace definitions accepted |
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java` | Registers the reload listener, `SERVER_STARTING`/`SERVER_STOPPED`, and `ServerEntityEvents.ENTITY_LOAD`; selection callback: guard 1 `isLoadedFromDisk()`, guard 2 `hasAttached(VARIANT_TEXTURE)`, then `snapshot.variantsFor(entity.getType())` and `entity.setAttached(VARIANT_TEXTURE, candidates.getFirst().texture())` |
| `src/client/java/com/baruc/brs/mobvariants/client/render/MobVariantsRenderState.java` | Duck interface on `LivingEntityRenderState`: `@Nullable Identifier mobVariants$variantTexture()` / setter |
| `src/client/java/com/baruc/brs/mobvariants/client/mixin/LivingEntityRenderStateMixin.java` | `@Unique @Nullable Identifier` field on `LivingEntityRenderState` |
| `src/client/java/com/baruc/brs/mobvariants/client/mixin/LivingEntityRendererMixin.java` | `@Shadow getTextureLocation`; `@Inject` HEAD of `extractRenderState` (writes the attachment value onto the render state, unconditionally including null); `@Redirect` of the single `getTextureLocation(state)` INVOKE inside `getRenderType` (returns the variant texture when non-null, else the renderer's own) |
| `src/main/resources/data/mob_variants_brs/variants/ice_zombie.json` | `{"entity_type": "minecraft:zombie", "texture": "mob_variants_brs:textures/entity/zombie/zombie_ice.png"}` — the only shipped definition |
| `src/main/resources/assets/mob_variants_brs/textures/entity/zombie/zombie_ice.png` | The variant texture asset |
| `src/client/resources/mob_variants_brs.client.mixins.json` | Lists `LivingEntityRenderStateMixin`, `LivingEntityRendererMixin` under `client` |
| `src/main/resources/mob_variants_brs.mixins.json` | Empty `mixins` array (server side has zero mixins) |

Scaffold cleanup is **done** **[VF]**: `ExampleMixin`/`ExampleClientMixin` are
deleted (commit `74be852`), `fabric.mod.json` placeholders are replaced
(description "Adds configurable texture variants for mobs", authors `["Baruc"]`,
empty `contact`), and the auto-connect run configuration exists in
`build.gradle:24-36` (`-PautoConnect=...` → `--quickPlayMultiplayer`).

Runtime evidence (untracked, git-ignored local state, not repository fact):
`run/logs/latest.log` records a real client+integrated/dedicated session —
client auto-connected to `localhost:25565`, player joined, two zombies were
summoned, world saved cleanly. The state-layer plan's status line asserts
dedicated-server and client runtime testing confirmed the documented behavior.
This session did not re-run runtime validation.

### 1.2 What is documented but NOT implemented / stale **[VF]**

**`docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/ROADMAP.md`, and
`docs/TESTING.md` are stale.** All four are written against commit `230ec45`
("Repository basis: commit `230ec45`") and assert the scaffold state:
"**no user-facing mod feature of any kind**", "no registry, event listener,
attachment, persistence, networking, configuration, or test exists", "The mod
holds no state", "There is **no** synchronisation of any kind". Every one of
those statements is now false. The state-layer plan's §24 item 4 ("Documentation
phase") was never executed. Specific stale claims:

- `DECISIONS.md` D-011 still says the attachment holds the canonical variant id
  (superseded by Candidate B: resolved texture).
- D-015 still says "exactly one client-side mixin" (there are two).
- D-016 still says the 26.1.2 renderer target is unverified (it was verified
  and implemented).
- D-018 still describes the `variants/<entity>/<name>.json` path layout
  (superseded by the flat `variants/<name>.json` layout with an authored
  `entity_type` field).
- `ROADMAP.md` §2 lists phases 0–2 as "EXPLICITLY PLANNED" (all implemented);
  P-1/P-2/P-3/P-4/P-9 are resolved, P-5/P-6/P-12 resolved, P-7/P-8/P-10 open.
- `TESTING.md` §1 still says "zero automated tests" (true) but every
  "N/A — NOT IMPLEMENTED" row is now at least partially demonstrable.

**Renderer plan §10 open gate** **[VF]**: the renderer plan leaves the
"vanilla renderer-specific texture selection" policy (options A/B/C) explicitly
undecided. The shipped implementation resolves it as **option A** (a selected
variant texture replaces the renderer's own choice wholesale) — the `@Redirect`
substitutes unconditionally when the attachment is non-null. This resolution is
**not recorded anywhere** in `docs/` or the plans.

### 1.3 What the current architecture already supports **[VF]**

- Immutable, complete, per-server definition snapshot published by a single
  atomic reference swap on `MinecraftServer.globalAttachments()`.
- Deterministic per-`EntityType` candidate lists (ascending variant-id order).
- One-time, idempotent, guarded selection at `ENTITY_LOAD` (tracking start).
- Server-authoritative resolution: the client receives only the resolved texture
  `Identifier`; no client registry, no client loader, no custom networking.
- Persistent + synced per-entity state through Fabric's attachment API alone.
- Vanilla-fallback renderer path: absent attachment → `null` → the redirect
  calls the shadowed vanilla `getTextureLocation`.

### 1.4 Limitations and architectural gaps **[VF] + [AD]**

1. **Selection is context-blind.** The rule is "first candidate in ascending
   variant-id order" — every mob of a given type receives the same variant.
   The state-layer plan §9.2 documents this as a "deliberate MVP placeholder".
2. **The shipped content cannot express its own intent.** `ice_zombie.json`
   is an *ice* zombie, but with no condition mechanism it applies to **every
   zombie in every biome**. The content exists; the condition mechanism does
   not.
3. **`VariantDefinition` has no optional-property model** — only two required
   fields. Every future property (conditions, and later attributes/effects)
   needs an additive extension path.
4. **`VariantSnapshot.byVariantId` has no consumer** (state-layer plan Q3).
   Cosmetic; kept as the identity index.
5. **`docs/` contradicts the repository** (§1.2).

---

## 2. Current Architecture Assessment

### 2.1 Preserve (do not change) **[AD]**

- The single `VARIANT_TEXTURE` attachment contract (resolved texture, not
  identity) — state-layer plan §5.2, Candidate B.
- `GlobalAttachments` snapshot ownership and the `attach()`/`detach()` server
  reference mechanism — load-bearing lifecycle design (state-layer plan §7.2–7.3).
- The `SERVER_DATA`-only reload listener registration — it is what makes
  client-side definition loading impossible by construction (state-layer plan §3.3).
- Both `ENTITY_LOAD` guards — guard 2 (`hasAttached`) alone guarantees
  select-once; guard 1 is defence in depth.
- The client render path: render-state field + `extractRenderState` HEAD write +
  `getRenderType` redirect. It is validated and must not be reworked.
- Zero server mixins, zero tick listeners, zero custom networking, zero
  `initializer()`, `copyOnDeath()` unset.

### 2.2 Requires adjustment **[AD]**

- **Selection rule only.** `VariantStateLifecycle.selectVariant` currently takes
  `candidates.getFirst()`. The next phase changes the rule to "first candidate
  whose conditions match the entity's load context". This is the one place the
  architecture anticipates change; everything else is untouched.
- **`VariantDefinition` data model.** Add an optional `conditions` field. The
  codec extension is additive: existing files (including `ice_zombie.json`)
  parse unchanged.
- **`docs/`** must be reconciled (§12, §13; proposed exact changes in the task
  list). The `.kilo/plans/` documents are current and are the authoritative
  architecture; `docs/` is not.

---

## 3. Next Development Phase

**Phase name:** **Condition-Based Variant Selection** (v1 scope: **biome
conditions**).

**Objective:** let a variant definition declare *when* it applies, so the
shipped `ice_zombie` content can be restricted to snowy biomes (and future
content such as a snow pig can be expressed), while preserving every property of
the implemented architecture: server-authoritative one-time selection, a single
resolved-texture attachment, vanilla fallback, zero tick work, zero client logic.

**Why this is the correct next phase (repository evidence):**

1. The first-candidate rule is the architecture's **only remaining documented
   placeholder** (state-layer plan §9.2: "That is a deliberate MVP placeholder").
   Every other planned layer (state, render) is implemented and validated.
2. The repository **already ships condition-dependent content that cannot
   function as intended** — `ice_zombie.json` applies to all zombies everywhere
   **[VF]**. This is a concrete functional gap, not speculation.
3. The task brief explicitly expects biome conditions at a one-time lifecycle
   point, with the example "Zombie + snowy biome → Snow/Ice Zombie variant".
4. The architecture supports the phase with **zero changes** to attachments,
   persistence, synchronization, networking, or the renderer: `ENTITY_LOAD`
   already receives `(Entity, ServerLevel)`; the biome is available from the
   level at that point; the selection result still resolves to a texture.
5. It is the smallest coherent step that adds real functionality without
   touching the validated render path or introducing new state.

**Why not attributes next:** no concrete requirement exists in the repository;
the brief forbids implementing attributes now; `FabricDefaultAttributeRegistry.MODIFY`
is per-`EntityType`-only (state-layer plan R-008 / original plan §9.3), so
per-instance attributes require a different application design that has no
concrete driver yet; and attributes would raise the variant-identity question
(R-005) that likewise has no concrete requirement. Conditions, by contrast, are
needed by content that already exists.

**Why not a variant-identity attachment next:** R-005's bar is "the moment
variant-specific *gameplay* (attributes, sounds, drops) needs the identity
rather than the texture". Conditions are evaluated once at selection and their
result (a texture) is persisted; no gameplay feature needs per-entity identity.
**[AD]**

---

## 4. Architectural Design

### 4.1 Classes and responsibilities **[AD]**

| Class | Responsibility | Change |
|---|---|---|
| `VariantDefinition` | Immutable authored definition: `entityType`, `texture`, plus **optional** `conditions` | modify (additive) |
| `VariantConditions` *(new, proposed)* | Immutable condition set for v1: an ordered list of biome `Identifier`s; `matches(Holder<Biome>)`-style predicate; own `CODEC` | create (name/placement to be confirmed in the verification gate; package follows the existing `com.baruc.brs.mobvariants.variant` convention) |
| `VariantSnapshot` | Unchanged: validates `entity_type`; conditions validation (biome ids resolve) belongs in `build()` alongside it, on the game thread | modify (additive validation) |
| `VariantDefinitionReloadListener` | Unchanged | none |
| `VariantStateLifecycle` | Selection rule becomes: iterate candidates in ascending id order; select the **first whose conditions match the entity's load context**; if none match, no variant | modify (rule only) |
| `VariantAttachments` | Unchanged | none |
| Client classes (`MobVariantsRenderState`, both mixins) | Unchanged | none |

### 4.2 Data flow **[AD]**

```
SERVER_DATA (mod jar + datapacks)
  → VariantDefinitionReloadListener.prepare (off-thread): decode JSON
      incl. optional "conditions" { "biomes": [...] }
  → VariantSnapshot.build (game thread): validate entity_type AND
      condition biome ids against BuiltInRegistries
  → server.globalAttachments() — atomic snapshot swap
                  │
                  ▼  (server only)
ServerEntityEvents.ENTITY_LOAD(entity, level)
  guard: isLoadedFromDisk()          → keep persisted texture
  guard: hasAttached(VARIANT_TEXTURE) → keep existing texture
  snapshot.variantsFor(entity.getType())
  → evaluate biome ONCE: level.getBiome(entity.blockPosition())   [OQ-1: 26.1.2 signature]
  → first candidate whose conditions match the biome
      (no conditions  ⇒ matches everything — backward compatible)
  → entity.setAttached(VARIANT_TEXTURE, matching.texture())
                  │
                  ▼  Fabric attachment persistence + sync (no custom packets)
  Entity NBT ────────────────► tracking clients
  VARIANT_TEXTURE: Identifier ──► renderer: texture, or vanilla when null
```

### 4.3 Entity lifecycle **[AD]**

Unchanged. Selection still happens exactly once, at `ENTITY_LOAD` (fired at
`ServerLevel$EntityCallbacks.onTrackingStart` TAIL — state-layer plan §3.4),
under the same two guards. No new event, no new hook, no tick listener.

### 4.4 Extension points **[AD]**

- **New condition kinds** (dimension, spawn reason, light level, …) are additive:
  new optional fields inside `conditions`, each evaluated against the same
  one-time load context. `ServerEntityEvents.ENTITY_LOAD` already exposes
  `EntityLoadData.spawnReason()` (original plan §2.1) for a future spawn-reason
  condition without any new hook.
- **Future optional properties** (attributes, effects, behaviours) are additive
  `Optional` fields on `VariantDefinition` applied once in the same guarded
  callback (see §10 and §11).
- **Future selection strategies** (UUID-hash, probability) remain possible but
  are **out of scope** and still have no concrete requirement (R-004's rejection
  of a speculative `VariantSelector` interface stands; conditions are data, not
  code, so no interface is introduced).

### 4.5 Interaction with the current variant-state layer **[AD]**

The state layer is consumed, not modified: the snapshot gains a validated
condition payload, and the selection callback gains a matching step. The
attachment contract, renderer contract (state-layer plan §15.1), persistence,
and sync are untouched.

---

## 5. Data Model

**Representation of optional variant properties** **[AD]**

- A variant property is an **optional field**: absent → not declared → vanilla
  behaviour; present → the variant's value applies. In the record this is
  `Optional<...>` with `Codec.optionalFieldOf` (or an optional nested record),
  so a v1 file parses forever (original plan §9.5).
- v1 adds exactly one optional property: `conditions`, itself an object with one
  required non-empty field `biomes` (list of biome `Identifier`s).

Example (the intended shape of `ice_zombie.json` after this phase):

```json
{
  "entity_type": "minecraft:zombie",
  "texture": "mob_variants_brs:textures/entity/zombie/zombie_ice.png",
  "conditions": {
    "biomes": ["minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_slopes", "minecraft:grove", "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:frozen_ocean", "minecraft:deep_frozen_ocean"]
  }
}
```

(The exact biome list is authoring content for the implementation session; the
set above is illustrative, not verified against 26.1.2 biome ids — **[AS]**.)

**Explicit override semantics** **[AD]**

- `conditions` absent ⇒ the variant applies to **all** entities of its type
  (current behaviour, backward compatible).
- `conditions` present ⇒ the variant applies only when **every** declared
  condition kind matches the entity's load context (v1: the biome at the
  entity's position is in the list).
- `conditions: {}` or `"biomes": []` ⇒ **rejected at load** with an error
  naming the file, mirroring R-011's rationale: an empty condition set is an
  authoring trap (observationally identical to no conditions, but implies
  intent that can never fail to match... and an empty list can never match —
  either way it is ambiguous, so it is rejected deterministically).
- Unknown/malformed biome ids in `biomes` ⇒ definition skipped with an error
  (validated in `VariantSnapshot.build` on the game thread, exactly like
  `entity_type`), **[AD]**.

**Vanilla fallback semantics** **[AD]**

- No candidate matches ⇒ no attachment ⇒ renderer falls through to vanilla.
  This is the existing structural fallback; conditions only change *which*
  candidates are eligible, never the fallback.

**Avoid unnecessary persistent state** **[AD]**

- Conditions are **not** persisted, **not** synced, and **not** stored on the
  entity. They are evaluated once; only the resolved texture is stored.

---

## 6. Selection Model

**Biome and future condition evaluation** **[AD]**

- Evaluation point: the existing `ENTITY_LOAD` callback — a one-time lifecycle
  point, **not** tick polling. For naturally spawned mobs, tracking start is
  the spawn tick (state-layer plan §9.1), so the sampled biome is the spawn
  biome.
- Biome lookup: `level.getBiome(entity.blockPosition())` — **[OQ-1]** exact
  26.1.2 signature and return type (`Holder<Biome>` expected) must be verified
  via `genSources` as verification-gate task 1, per the Phase-0 precedent
  (`ROADMAP.md` §2.1 pattern). Do not guess.
- Matching a biome `Identifier` against the resolved holder: either unwrap the
  holder to its id (`unwrapKey()` → `ResourceKey#location`) or compare via
  `BuiltInRegistries.BIOME` — **[OQ-2]** verify the exact 26.1.2 surface.
- **Lazy evaluation:** the biome is looked up only when the first
  conditions-bearing candidate is reached. The common paths — entity type has no
  definitions (two map probes, no allocation), or the first candidate has no
  conditions — perform **zero** biome lookups.
- One biome lookup per selection, reused across all candidates.

**Determinism** **[AD]**

- Candidate order is fixed (ascending variant id, sorted in `prepare`).
- The biome is a pure function of position at load time.
- Same entity, same position, same datapack set ⇒ same variant. No randomness,
  no clock, no tick dependence.

**Avoidance of tick polling** **[AD]**

- Structural: no tick listener is registered anywhere (D-013 holds). An entity
  that wanders into a different biome **keeps** its variant — a direct
  consequence of the select-once semantics the brief requires ("Biome selection
  must occur at an appropriate one-time lifecycle point rather than through
  continuous tick polling").

**Authoring rule to document** **[REC]**

- Overlapping condition sets resolve by variant-id order, not by specificity.
  Authors must keep condition sets disjoint or accept id-order precedence.
  Record this in the definition-file documentation when docs are reconciled.

---

## 7. State Model

**`VARIANT_TEXTURE` remains sufficient** **[AD]**

- Selection still resolves to a texture. Conditions change eligibility only.
- The attachment continues to hold *resolved render state*, not variant
  identity; nothing about conditions creates a need to know *which* variant an
  entity has after selection.

**Exact conditions under which additional persistent state would become
necessary** **[AD]** (R-005's bar, restated precisely):

1. A feature must know an entity's variant **at a point other than the initial
   selection** — e.g. live restyling on `/reload`, variant-keyed client
   behaviour, or a command that reports the variant.
2. A feature must **recompute** per-entity state after the definition set
   changes (attributes recomputed on reload, etc.).
3. A feature must persist data that is **not** self-contained in the value
   itself (e.g. a variant identity that must be re-resolved against a changed
   snapshot).

None of these is required by biome conditions. **Do not add a second attachment
in this phase.**

---

## 8. Server/Client Model

**Server responsibilities** **[AD]** (unchanged + conditions):

- Load and validate definitions **and their conditions**; publish the snapshot.
- Evaluate conditions and select the variant; write the attachment.
- Persist and sync the attachment (Fabric API).

**Client responsibilities** **[AD]** (unchanged):

- Receive the synced attachment; read it once at `extractRenderState`; substitute
  the texture in `getRenderType` or fall through to vanilla. The client
  evaluates **no** conditions, resolves **no** definitions, and holds **no**
  variant state of its own.

**Synchronization requirements** **[AD]** (unchanged):

- `AttachmentSyncPredicate.all()`; immediate fan-out on `setAttached`;
  `START_TRACKING` initial sync for chunk loads, late joins, reconnects;
  configuration-phase capability handshake already handled by Fabric (a client
  without the mod logs Fabric's existing warning and renders vanilla).

**Dedicated server** **[AD]**: identical; `main` source set only, no client
classes, conditions evaluated server-side.

**Integrated server** **[AD]**: identical; single JVM; the `SERVER_DATA`-only
listener registration keeps client resources from ever driving the definition
loader (state-layer plan §3.3).

**Entities saved, unloaded, loaded, transferred** **[AD]**: unchanged — the
persisted attachment is restored before `ENTITY_LOAD`, guard 2 returns, and the
variant survives chunk unload/reload, dimension travel (attachments copied
wholesale), and server restart.

**Players joining/rejoining** **[AD]**: unchanged — initial sync on
`START_TRACKING`.

---

## 9. Persistence and Reload Semantics

| Situation | Behaviour **[AD]** |
|---|---|
| **Existing entities** | Unchanged. Guards skip them; persisted texture stands. |
| **Newly spawned entities** | Conditions evaluated once against the biome at the load position; matching variant's texture attached; otherwise no attachment (vanilla). |
| **Loaded entities (disk/NBT)** | Guard 1 and guard 2 skip them; the persisted texture is authoritative regardless of current biome. |
| **`/reload`** | New snapshot (with new/changed conditions) published atomically. Applies to entities loaded **after** the reload. Existing entities are **not** re-selected or re-styled — this is the already-documented, deliberate behaviour (state-layer plan §14.3): the attachment holds a texture, not an identity, so there is nothing to re-resolve. |
| **Datapack changes** | Same as `/reload`. A datapack that adds/removes conditions changes only future selections. |
| **Definition removed** | No effect on existing entities (self-contained texture id); new entities of that type get no variant. |
| **Malformed conditions JSON** | File skipped with an `error` naming the file; the rest of the reload proceeds (existing failure-isolation behaviour). |
| **Empty conditions object/list** | Rejected at load as an authoring trap (§5). |

---

## 10. Future Configuration Seam

**[AD]** The eventual separation is preserved without introducing any
configuration code now:

- **Server config → global gameplay rules** (future): would be published as a
  server-scoped attachment on `GlobalAttachments` — the exact pattern
  `SERVER_VARIANT_SNAPSHOT` already establishes — and read by the selection
  callback as an additional input. No current code couples to it.
- **Datapack/JSON → variant definitions and conditions** (this phase): content
  stays in `SERVER_DATA`, loaded by the existing reload listener. Conditions are
  *content*, not configuration — they are per-variant authored data, not
  server-wide rules.
- **Entity attachments → persistent per-entity state** (existing):
  `VARIANT_TEXTURE`.
- **Fabric networking → synchronization** (existing): attachment sync only.
- **Vanilla APIs → application of optional modifications** (future): per-instance
  application at the same one-time `ENTITY_LOAD` point.

**How future attributes/effects/behaviours attach without breaking the current
architecture** **[AD]** (design guidance only — nothing implemented):

1. New `Optional` fields on `VariantDefinition` (`optionalFieldOf`), so old
   files keep parsing.
2. Applied **once**, server-side, inside the existing guarded `ENTITY_LOAD`
   callback, immediately after selection — no tick listener, no new mixin, no
   new persistent state for values vanilla itself persists (e.g. attribute
   values live in the entity's own attribute NBT).
3. Per-instance attribute application must go through the entity's `AttributeMap`
   at load time, because `FabricDefaultAttributeRegistry.MODIFY` is
   per-`EntityType`-only (R-008). The exact 26.1.2 `AttributeMap` mutation
   surface is **[OQ-4]** for that future phase.
4. Anything that must know "which variant" later is the trigger for the
   variant-identity attachment (§7) — decided then, not now.

---

## 11. Performance and Compatibility Analysis

**Runtime costs** **[AD]**

- Entity-load cost, no-definition path: unchanged (two map probes + one map get,
  no allocation, no write).
- Entity-load cost, conditions path: one biome lookup (chunk-local; the biome
  container is already loaded with the chunk) **only when a conditions-bearing
  candidate is reached**, plus O(candidates × biomes) list scans on tiny
  immutable lists. No repeated world/chunk access — a single point lookup.
- Variant-selection cost: unchanged structurally; the added work is bounded by
  the authored candidate count.
- Tick cost: **zero** (structural — no tick hook exists).
- Persistence cost: unchanged (one `Identifier` per varianted entity in NBT).
- Sync cost: unchanged (one `Identifier`, sent once per entity and on tracking
  start).
- Unnecessary allocations: none introduced; the stored value is `null` or an
  already-shared `Identifier`; condition lists are immutable codec products.

**Vanilla compatibility** **[AD]**

- Vanilla visual variation continues to work for every entity **without** a
  selected variant (no attachment → `null` → vanilla texture, including
  renderer-specific per-entity choices such as cat/cow/pig variants).
- **Policy decision (resolves renderer plan §10): option A** — when a variant
  with an explicit texture **is** selected, its texture replaces the renderer's
  own texture choice wholesale for that entity. This is what the shipped
  implementation already does, and the brief endorses it ("If a selected variant
  explicitly provides a custom texture, that texture may override the vanilla
  texture for that entity"). Consequence to record in `DECISIONS.md`: for
  renderer-variant types (e.g. pigs), a variant overrides that entity's vanilla
  per-entity variety; vanilla variety is preserved for all non-varianted
  entities. **[REC]** adopt A explicitly and document it; the alternative (B:
  exclude such types; C: override only the default texture) adds per-type
  knowledge that does not exist in the repository and contradicts the brief's
  priority rule.
- No custom texture ⇒ vanilla fallback, structurally guaranteed by the redirect's
  else-branch. Nothing is hardcoded for any entity type.

**Renderer compatibility** **[AD]**

- The client render path is untouched. `ArmorStandRenderer` marker entities keep
  vanilla behaviour (renderer plan §9). `AgeableMobRenderer#submit` calls
  `super`, so families are covered (renderer plan §9).
- Mixin configs keep `defaultRequire: 1`; the two client mixins are validated
  against 26.1.2 **[VF, implemented]**.

---

## 12. Risks and Unresolved Questions

**Confirmed issues (not open questions):**

1. **`docs/` is stale and contradicts the repository** **[VF]** — §1.2 lists the
   specific false claims. This is the one finding that should be corrected
   **before** the implementation session writes code against `docs/`.
2. **Renderer §10 policy was resolved in code (option A) but never recorded**
   **[VF]** — `DECISIONS.md` has no entry; the renderer plan still lists it as
   an open gate.

**Unresolved questions (for the implementation session's verification gate):**

- **[OQ-1]** Exact 26.1.2 `getBiome` signature and return type on
  `ServerLevel`/`LevelReader`.
- **[OQ-2]** Exact id-extraction or comparison path for a resolved biome holder
  (`unwrapKey()` vs. registry lookup) and, if biome **tags** are ever adopted,
  `TagKey` construction and vanilla's available biome tag ids (tags are **not**
  in v1 — explicit id lists are recommended because vanilla's tag set is not
  part of the mod's contract and tag availability is version-dependent).
- **[OQ-3]** Whether `ENTITY_LOAD`'s tracking-start timing is acceptable for
  every spawn path the mod cares about (state-layer plan §9.1 argues yes for
  natural spawns; the select-once consequence for biome-changing mobs is
  intended behaviour, not a defect).
- **[OQ-4]** Future phase only: the 26.1.2 per-instance `AttributeMap`
  mutation surface for variant attributes.

**Open architectural questions (owner decisions, recommendations given):**

- Biome id list vs. biome tags in v1 — **[REC]** id list (verifiable, no
  tag-registry dependency).
- `VariantConditions` as a separate top-level record vs. nested in
  `VariantDefinition` — **[REC]** separate record in the same package
  (single responsibility, codec separation); final placement confirmed at
  implementation.
- Keep or delete `VariantSnapshot.byVariantId` (Q3) — **[REC]** keep (identity
  index; future identity features need it); not touched in this phase.

---

## 13. Validation Strategy

**Verification gate (task 1, before any condition code):** run `genSources`,
confirm the §12 [OQ-1]/[OQ-2] signatures against the generated 26.1.2 sources,
and record each confirmed signature in a comment where it is used (the
Phase-0 precedent from `ROADMAP.md` §2.1). `./gradlew build` is the compile-time
proof of every API name.

**Implementation validation (for the implementation session):**

1. `./gradlew build` — compiles both source sets.
2. `./gradlew runServer`; `/summon zombie` in a snowy biome and in a non-snowy
   biome; `/data get entity` shows the attachment **only** in the matching
   biome.
3. Vanilla fallback: a zombie in a non-matching biome renders vanilla; a pig
   (no definitions) renders vanilla with its normal renderer-specific behaviour.
4. Persistence: chunk unload/reload and server restart keep the texture
   byte-identical.
5. `/reload` after editing the biome list: **new** spawns follow the new list;
   existing entities unchanged.
6. Malformed `conditions` JSON: one `error` naming the file, reload completes,
   other definitions load.
7. `conditions: {}` and `"biomes": []`: rejected at load with an error.
8. A definition without `conditions`: applies everywhere (backward compatibility
   with pre-conditions files).
9. `./gradlew runClient -PautoConnect=localhost:25565` against the server:
   varianted zombie renders the ice texture **in the snowy biome only**; the
   client test must assert the texture *changed* (the mixins are `require`-safe
   but silent on failure — renderer plan §8).
10. Two clients in one session: both render the same entity identically.
11. Late join / reconnect: correct texture on first sight.
12. Negative performance check: no mod listener is invoked during steady-state
    ticking (structural — no tick event is registered; confirm via profiler or
    temporary counter, removed afterwards).
13. Dedicated-server run: server starts and selects variants with no client
    classes loaded.

**No automated test suite exists** **[VF]** (no `src/test`, no test framework
dependency); all validation above is manual, matching the repository's current
validation model (`docs/TESTING.md` §1).

---

## 14. Files/Classes Likely to Change

All paths below were verified to exist in the repository except the one marked
*(new)*, which is a recommendation whose name/package the implementation session
confirms after the verification gate.

| File | Change |
|---|---|
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantDefinition.java` | Add optional `conditions` field + codec branch |
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantConditions.java` *(new)* | Immutable condition record (v1: biome id list), `CODEC`, match predicate |
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantSnapshot.java` | Validate condition biome ids in `build()` (game thread) |
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java` | Selection rule: first candidate whose conditions match; lazy single biome lookup |
| `src/main/resources/data/mob_variants_brs/variants/ice_zombie.json` | Add snowy-biome `conditions` (demonstrates the feature; texture asset already exists) |
| `docs/ARCHITECTURE.md` | Reconcile with the implemented architecture (§1.2) |
| `docs/DECISIONS.md` | Amend D-011/D-015/D-016/D-018; retire superseded R-004/R-005 wording; add entries for `GlobalAttachments` ownership, the `hasAttached` guard, the namespace policy, and the §10 option-A policy decision |
| `docs/ROADMAP.md` | Close phases 0–2 as implemented; add this phase; update P-1…P-12 statuses |
| `docs/TESTING.md` | Update validation status; add this phase's matrix |
| `.kilo/plans/1791428203997-condition-based-variant-selection-plan.md` | This plan |

**Explicitly not touched:** `VariantAttachments.java`,
`VariantDefinitionReloadListener.java` (only its validation call site in
`VariantSnapshot.build` changes), `MobVariantsBRS.java`, all of `src/client/**`,
`build.gradle`, `gradle.properties`, both mixin configs, `fabric.mod.json`.

---

## 15. Explicit Non-Goals

- **No** attributes, effects, behaviours, sounds, drops, commands, or GUI.
- **No** configuration system, config files, config screens, config sync, or
  configuration dependencies.
- **No** variant-identity attachment (§7's bar is not met).
- **No** client-side changes — the render path, mixins, and client mixin config
  are frozen.
- **No** custom networking, packets, or payloads.
- **No** tick listeners, per-tick polling, or continuous biome checks.
- **No** biome tags in v1 (id lists only; tags require [OQ-2] verification and
  an owner decision).
- **No** probability weighting, UUID-hash selectors, or `VariantSelector`
  interface (R-004 rejection stands; conditions are data).
- **No** live restyling of existing entities on `/reload`.
- **No** speculative caching, interning, or pre-warmed arrays.
- **No** commits or pushes from the planning session.

---

## Task list for the implementation session

1. **Verification gate:** `genSources`; confirm [OQ-1]/[OQ-2] signatures; record
   them in comments; `./gradlew build` as the compile proof.
2. **Documentation reconciliation first** (or as a parallel documentation task):
   update `docs/ARCHITECTURE.md`, `DECISIONS.md`, `ROADMAP.md`, `TESTING.md`
   per §1.2/§14 so implementation does not proceed against contradictory docs.
3. Implement `VariantConditions` (biome id list, codec, match predicate).
4. Extend `VariantDefinition` with the optional `conditions` field.
5. Extend `VariantSnapshot.build` with condition-biome-id validation.
6. Change `VariantStateLifecycle.selectVariant` to first-matching-candidate
   with the lazy single biome lookup.
7. Add `conditions` to `ice_zombie.json` (snowy biomes).
8. Run the §13 validation matrix (server, persistence, reload, client via
   `-PautoConnect`, multiplayer, fallback, negative cases).
9. Record the §10 option-A policy decision in `docs/DECISIONS.md` if step 2 did
   not already.

## Answers to the 17 planning questions (summary)

1. **Next phase:** Condition-Based Variant Selection (v1: biome conditions) — §3.
2. **Why:** only remaining documented placeholder + shipped content that cannot
   express its intent + brief's explicit expectation + zero-impact extension — §3.
3. **Architectural changes:** selection rule + optional `conditions` data field +
   condition validation; nothing else — §4.
4. **`VARIANT_TEXTURE` sufficient:** yes — §7.
5. **Persistent identity required:** no; §7 lists the exact conditions that would
   require it.
6. **Optional properties:** `Optional` fields / `optionalFieldOf`; absent ⇒
   vanilla — §5.
7. **Biome × optional properties:** conditions gate *eligibility*; properties
   apply only when selected — orthogonal layers — §4.2/§5.
8. **Vanilla fallback per property:** no attachment/undeclared property ⇒ vanilla
   behaviour, structurally — §5/§11.
9. **Future attributes/effects/behaviours:** additive `Optional` fields applied
   once in the same guarded `ENTITY_LOAD` callback; per-instance `AttributeMap`
   application; identity attachment only when a feature needs identity later — §10.
10. **Future config without premature coupling:** server-scoped attachment on
    `GlobalAttachments` (the `SERVER_VARIANT_SNAPSHOT` pattern); conditions are
    datapack content, not config — §10.
11. **Dedicated/multiplayer:** unchanged; server-authoritative; attachment sync
    covers tracking/late-join/reconnect — §8.
12. **Client/server separation:** server evaluates conditions; client receives
    only the resolved texture — §8.
13. **Persistence/lifecycle:** select once at `ENTITY_LOAD`; persisted texture
    survives everything; biome changes later do not re-select — §6/§9.
14. **`/reload`:** atomic snapshot swap; affects only entities loaded afterwards — §9.
15. **Performance:** one lazy biome lookup per selection only when a
    conditions-bearing candidate is reached; zero tick cost; unchanged persistence
    and sync cost — §11.
16. **Vanilla compatibility:** vanilla variation preserved for non-varianted
    entities; variant texture wins only when explicitly selected (option A) — §11.
17. **Documentation must be revised first:** **yes** — `docs/` currently asserts
    the opposite of the repository; reconcile before/while implementing — §1.2/§14.
