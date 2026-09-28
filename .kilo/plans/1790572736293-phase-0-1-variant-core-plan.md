# Mob Variants BRS — Master Architecture & Phased Implementation Plan

## A. Final Master Architecture

### Core Variant Model
- Variants are **data layers** applied to vanilla entities — no custom EntityTypes, models, or renderers
- Each variant definition binds to a base `EntityType` (e.g., `minecraft:zombie`)
- Variant identity is **stable and derived from resource path**: `mob_variants_brs:zombie/ice`
- One variant per entity instance; selected once on fresh spawn, then persisted

### Variant Identity
- `VariantDefinition` loaded from `data/mob_variants_brs/variants/<entity>/<variant>.json`
- Variant ID = namespace + path from resource location (no redundant `variant_id` field)
- Registry keyed by `Identifier`; lookup by base entity type for spawn selection

### Registry
- Immutable snapshot registry built on resource reload via `AddReloadListenerCallback`
- Server and client each load independently from **mod-bundled resources** (same JAR) — no custom networking
- Provides: `get(Identifier)`, `getForEntity(EntityType<?>)`, `all()`

### Persistence
- Fabric Data Attachments API: `AttachmentType<Identifier>` named `VARIANT_ATTACHMENT`
- `.persistent(Identifier.CODEC)` — survives save/load
- `.copyOnDeath()` — preserved on mob conversion (zombie→drowned), player respawn, dimension travel
- `.syncWith(Identifier.STREAM_CODEC, AttachmentSyncPredicate.all())` — visible to all tracking clients

### Selection Lifecycle
- **Hook**: `ServerEntityEvents.ALLOW_LOAD` (fires before entity added to world)
- **Guards** (all must pass to select):
  1. `!entity.hasAttached(VARIANT_ATTACHMENT)` — never overwrite existing
  2. `!isLoadedFromDisk` — excludes chunk loads, dimension travel, restores
  3. `spawnReason != EntitySpawnReason.CONVERSION` — handled by copyOnDeath
  4. `spawnReason != EntitySpawnReason.LOAD && spawnReason != EntitySpawnReason.DIMENSION_TRAVEL` — explicit guards
- **Selection**: Match entity's biome at `entity.getBlockPos()` against variant's `biomeTag` (single match, no weights for MVP)
- **Apply**: Set attachment, then apply transient attribute modifiers

### Attributes
- Transient `AttributeModifier`s (not saved to NBT — variant identity persists via attachment)
- Modifier UUID = deterministic hash: `UUID.nameUUIDFromBytes(("mob_variants_brs:" + variantPath + "." + attributePath).getBytes())`
- Applied on selection; reapplied on `ServerEntityEvents.ENTITY_LOAD` (idempotent — same UUID prevents stacking)
- Operations: `add_value`, `add_multiplied_base`, `add_multiplied_total` (per JSON)

### Rendering
- **Two Mixins** (client-only):
  1. `LivingEntityRenderStateMixin` — `@Unique Identifier variantId` field added to render state
  2. `LivingEntityRendererMixin`:
     - `extractRenderState(T, S, float)` @TAIL: copy `entity.getAttached(VARIANT_ATTACHMENT)` → `state.variantId`
     - `getTextureLocation(S)` @HEAD cancellable: resolve texture from `state.variantId` via client registry
- Preserves vanilla `EntityType`, model, renderer pipeline; only texture changes
- Texture path: `assets/mob_variants_brs/textures/entity/<entity>/<variant>.png`

### Behaviors
- Sealed interface `BehaviorEntry` with typed implementations (no `Map<String,Object>`)
- MVP: `FreezeOnHitEntry(duration, amplifier, chance)` — on `ServerLivingEntityEvents.AFTER_DAMAGE`, if attacker has variant with this behavior, apply Slowness to victim
- Registry: `BehaviorRegistry` maps behavior type → handler; wired in `onInitialize`

### Client/Server Responsibilities
| System | Server | Client |
|--------|--------|--------|
| Variant definition loading | ✅ (mod resources) | ✅ (same mod resources) |
| Spawn selection | ✅ | ❌ |
| Attachment persistence | ✅ | ❌ (receives via sync) |
| Attribute application | ✅ | ❌ |
| Behavior execution | ✅ | ❌ |
| Texture resolution | ❌ | ✅ (Mixin + client registry) |
| Mixin application | ❌ | ✅ (client mixin config) |

### MVP Scope
- One example variant: `zombie/ice` (spawns in `#minecraft:is_cold` biomes, +4 health, +2 armor, 30% chance to inflict Slowness II for 5s on hit)
- Variants remain data layered onto vanilla entities

---

## B. Phased Implementation Plan

### Phase 0 — Project Cleanup

**Objective**: Remove template artifacts; prepare clean codebase. Do not register variant systems yet.

**Files to Remove**:
- `src/main/java/com/baruc/brs/mobvariants/mixin/ExampleMixin.java`
- `src/main/resources/mob_variants_brs.mixins.json`
- `src/client/java/com/baruc/brs/mobvariants/client/mixin/ExampleClientMixin.java`
- `src/client/resources/mob_variants_brs.client.mixins.json`

**Files to Modify**:
- `src/main/resources/fabric.mod.json` — update metadata (description, authors, contact, license); remove datagen entrypoint; keep main/client entrypoints

**Files to Keep**:
- `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` — empty `onInitialize()` for now
- `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSClient.java`
- `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSDataGenerator.java` (empty)
- `build.gradle`, `settings.gradle`, `gradle.properties`

**Dependencies**: None

**Validation**: `./gradlew build` passes; server/client start; no template classes in output

**Non-Goals**: No Mixin configs, no variant logic, no datagen, no attachment registration

---

### Phase 1 — Variant Core (Minimal)

**Objective**: Load variant JSON → Codec → VariantDefinition → Registry → Variant ID → Entity Attachment. Prove the core data pipeline.

**Files to Modify**:
- `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` — register `VARIANT_ATTACHMENT` in `onInitialize()`

**Files to Create**:
- `src/main/java/com/baruc/brs/mobvariants/variant/VariantDefinition.java` — record with Codec: `baseEntity`, `texture` (minimal fields for MVP proof)
- `src/main/java/com/baruc/brs/mobvariants/variant/VariantRegistry.java` — immutable snapshot registry via `AddReloadListenerCallback`; `get(Identifier)`, `getForEntity(EntityType<?>)`, `all()`
- `src/main/java/com/baruc/brs/mobvariants/variant/VariantLoader.java` — `SimpleJsonResourceReloadListener` reading `data/mob_variants_brs/variants/**/*.json`
- `src/main/java/com/baruc/brs/mobvariants/variant/VariantAttachment.java` — `AttachmentType<Identifier>` with `.persistent(Identifier.CODEC)`, `.copyOnDeath()`, `.syncWith(Identifier.STREAM_CODEC, all())`
- `src/main/resources/data/mob_variants_brs/variants/zombie/ice.json` — minimal example variant (baseEntity, texture only; biome/attributes/behavior added in later phases)

**Dependencies**: Phase 0

**Validation**: Build passes; registry loads variant; attachment set/retrieved manually; variant ID = `mob_variants_brs:zombie/ice`

**Non-Goals**: No spawn selection, biome eval, attributes, behaviors, rendering, Mixins, conversion, datagen, config, SpawnCondition, AttributeModifierEntry, BehaviorEntry

---

### Phase 2 — Variant Selection

**Objective**: Automatic variant selection on fresh entity spawn via `ALLOW_LOAD`. Introduce biome condition evaluation.

**Files to Create**:
- `src/main/java/com/baruc/brs/mobvariants/spawn/VariantSelector.java` — biome tag match logic
- `src/main/java/com/baruc/brs/mobvariants/spawn/SpawnHandler.java` — `ALLOW_LOAD` listener with 4 guards
- `src/main/java/com/baruc/brs/mobvariants/variant/SpawnCondition.java` — record with `biomeTag: Identifier` + Codec

**Files to Modify**:
- `src/main/java/com/baruc/brs/mobvariants/variant/VariantDefinition.java` — add `spawnCondition` field
- `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` — register `SpawnHandler`
- `src/main/resources/data/mob_variants_brs/variants/zombie/ice.json` — add `spawn_condition` with biome tag

**Dependencies**: Phase 1 (registry, attachment, variant definitions)

**Validation**: Zombie spawns in cold biome → attachment set; `/data get entity @e[type=zombie,limit=1] mob_variants_brs:variant_id` shows `mob_variants_brs:zombie/ice`; chunk load/dimension travel/conversion do NOT overwrite

**Performance**: Biome lookup at `entity.getBlockPos()` only (no chunk loading); registry O(1) lookup

**Non-Goals**: Attributes, behaviors, rendering, conversion handling

---

### Phase 3 — Persistence and Synchronization

**Objective**: Verify attachment survives save/load and syncs to clients for rendering. No attribute logic.

**Files to Create**:
- `src/client/java/com/baruc/brs/mobvariants/client/VariantRegistryClient.java` — client-side registry (same loader pattern)

**Files to Modify**:
- `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` — ensure attachment registered (already done in Phase 1)

**Dependencies**: Phase 2 (selection working)

**Validation**: Save world → reload → variant attachment persists; client sees variant ID via attachment (debug log); dedicated server works

**Performance**: No additional overhead; uses Fabric API built-in sync

**Non-Goals**: Custom networking, rendering, attribute reapplication

---

### Phase 4 — Attributes

**Objective**: Deterministic, idempotent attribute modifier application with ENTITY_LOAD reapplication.

**Files to Create**:
- `src/main/java/com/baruc/brs/mobvariants/attribute/VariantApplicator.java` — apply/reapply modifiers with stable UUIDs
- `src/main/java/com/baruc/brs/mobvariants/variant/AttributeModifierEntry.java` — record with `attribute: Holder<Attribute>`, `operation`, `amount` + Codec + UUID generation

**Files to Modify**:
- `src/main/java/com/baruc/brs/mobvariants/variant/VariantDefinition.java` — add `attributes` list field
- `src/main/java/com/baruc/brs/mobvariants/spawn/SpawnHandler.java` — call `VariantApplicator.apply()` on selection
- `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` — register `ENTITY_LOAD` listener calling `VariantApplicator.reapply()`
- `src/main/resources/data/mob_variants_brs/variants/zombie/ice.json` — add `attributes` array

**Dependencies**: Phase 3 (persistence/sync verified)

**Validation**: Ice zombie has +4 max health, +2 armor; save/load/reload never stacks modifiers; `/attribute` shows correct values

**Performance**: Modifier application O(n) where n = variant attributes (typically 1-3); UUID generation once per definition load

**Non-Goals**: Behaviors, rendering

---

### Phase 5 — Rendering

**Objective**: Custom texture per variant via render-state Mixins.

**Files to Create**:
- `src/client/java/com/baruc/brs/mobvariants/client/render/mixin/LivingEntityRenderStateMixin.java`
- `src/client/java/com/baruc/brs/mobvariants/client/render/mixin/LivingEntityRendererMixin.java`
- `src/client/resources/mob_variants_brs.client.mixins.json` — client Mixin config
- `src/main/resources/assets/mob_variants_brs/textures/entity/zombie/ice.png` — example texture

**Files to Modify**:
- `src/main/resources/fabric.mod.json` — add client mixin config reference
- `src/main/java/com/baruc/brs/mobvariants/variant/VariantDefinition.java` — ensure `texture` field accessible
- `src/client/java/com/baruc/brs/mobvariants/client/VariantRegistryClient.java` — add texture resolution method

**Dependencies**: Phase 3 (client registry loads definitions; attachment synced)

**Validation**: Ice zombie renders with custom texture in cold biome; vanilla zombies unchanged; no per-frame resource loading

**Performance**: Texture resolved once per render state creation; registry lookup O(1)

**Non-Goals**: Model changes, custom renderers, server-side rendering

---

### Phase 6 — Behavior

**Objective**: Implement `freeze_on_hit` behavior via `AFTER_DAMAGE`.

**Files to Create**:
- `src/main/java/com/baruc/brs/mobvariants/behavior/BehaviorRegistry.java`
- `src/main/java/com/baruc/brs/mobvariants/behavior/BuiltinBehaviors.java` — registers `freeze_on_hit` handler
- `src/main/java/com/baruc/brs/mobvariants/behavior/EffectApplier.java` — applies status effects
- `src/main/java/com/baruc/brs/mobvariants/variant/BehaviorEntry.java` — sealed interface + `FreezeOnHitEntry` record + Codec

**Files to Modify**:
- `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` — register `AFTER_DAMAGE` listener
- `src/main/java/com/baruc/brs/mobvariants/variant/VariantDefinition.java` — add `behavior` optional field
- `src/main/resources/data/mob_variants_brs/variants/zombie/ice.json` — add `behavior` object

**Dependencies**: Phase 4 (attributes working; attacker has variant attachment)

**Validation**: Hit ice zombie → 30% chance victim gets Slowness II for 5s; no effect on vanilla zombies

**Performance**: Event fires only on damage; minimal overhead

**Non-Goals**: Additional behaviors, conversion handling, config system

---

## C. Exact Current Project Files

### Files to Remove (Phase 0)
- `src/main/java/com/baruc/brs/mobvariants/mixin/ExampleMixin.java`
- `src/main/resources/mob_variants_brs.mixins.json`
- `src/client/java/com/baruc/brs/mobvariants/client/mixin/ExampleClientMixin.java`
- `src/client/resources/mob_variants_brs.client.mixins.json`

### Files to Modify
- `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` (Phases 0, 1, 2, 3, 4, 6)
- `src/main/resources/fabric.mod.json` (Phases 0, 5)

### Files to Keep
- `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSClient.java`
- `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSDataGenerator.java`
- `build.gradle`, `settings.gradle`, `gradle.properties`

### Files to Create (by Phase)
- Phase 1: 4 Java files + 1 JSON (minimal variant definition)
- Phase 2: 3 Java files + modifications to 2 existing
- Phase 3: 1 Java file (client)
- Phase 4: 2 Java files + modifications to 4 existing
- Phase 5: 3 Java files + 1 Mixin config + 1 texture + modifications to 2 existing
- Phase 6: 4 Java files + modifications to 3 existing

---

## D. Remaining Genuine Blockers

**None.** All critical APIs verified against Fabric 0.155.3+26.1.2 and Minecraft 26.1.2:
- `ServerEntityEvents.ALLOW_LOAD` signature and `EntitySpawnReason` enum confirmed
- `LivingEntityRenderer` uses `extractRenderState` / `getTextureLocation` (official mappings)
- Data Attachments `copyOnDeath()` explicitly covers mob conversion
- Client loads same mod-bundled resources — no custom networking needed
- ALLOW_LOAD deadlock mitigated by biome lookup at `entity.getBlockPos()` only

---

## E. Recommended First Implementation Task

**Implement Phase 0 and Phase 1 only.**

Phase 0: Clean template files, update `fabric.mod.json` metadata. Leave `MobVariantsBRS.onInitialize()` empty.

Phase 1: Create VariantDefinition, VariantRegistry, VariantLoader, VariantAttachment, and the minimal example `zombie/ice.json` (baseEntity + texture only). Register `VARIANT_ATTACHMENT` in `MobVariantsBRS.onInitialize()`. Validate that the registry loads and attachment can be set/retrieved.

Do not proceed to Phase 2 until Phase 1 validation passes.

---

PLAN READY FOR REVIEW