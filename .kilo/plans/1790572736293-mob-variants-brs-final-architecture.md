# Mob Variants BRS — Final Verified Architecture (Minecraft 26.1.2 + Fabric 0.155.3)

---

## A. Verified Facts

### Fabric API 0.155.3+26.1.2 (JAR: `fabric-lifecycle-events-v1-4.1.1+df84eb3d4c.jar`, `fabric-data-attachment-api-v1-2.2.10+44a0bd1d4c.jar`)

| API | Verified Signature |
|-----|-------------------|
| `ServerEntityEvents.ALLOW_LOAD` | `boolean onAllowLoad(Entity entity, ServerLevel level, @Nullable EntitySpawnReason spawnReason, boolean isLoadedFromDisk)` — returns `true` to allow, `false` to cancel |
| `ServerEntityEvents.ENTITY_LOAD` | `void onLoad(Entity entity, ServerLevel level)` — fires after entity is in world |
| `ServerLivingEntityEvents.MOB_CONVERSION` | `void onMobConversion(LivingEntity oldEntity, LivingEntity newEntity, @Nullable Object params)` |
| `ServerLivingEntityEvents.AFTER_DAMAGE` | `void afterDamage(LivingEntity victim, DamageSource source, float baseDamage, float damageTaken, boolean blocked)` — `victim` = entity taking damage, `source.getAttacker()` = attacker |
| `AttachmentRegistry.create` | `AttachmentType<A> create(Identifier id, Consumer<Builder<A>> builder)` |
| `Builder.persistent(Codec<A>)` | Makes attachment persist to NBT |
| `Builder.copyOnDeath()` | Preserves attachment on mob conversion (zombie→drowned), player respawn, dimension travel |
| `Builder.syncWith(StreamCodec, AttachmentSyncPredicate)` | Syncs to clients; `AttachmentSyncPredicate.all()` = all tracking players |
| `Identifier codecs` | `Identifier.CODEC` (NBT), `Identifier.STREAM_CODEC` (network) — both exist |

### Minecraft 26.1.2 (Official Mojang Mappings — confirmed by Loom 1.18 cache)

| Class | Verified Methods (Official Mappings) |
|-------|--------------------------------------|
| `EntitySpawnReason` (enum) | `NATURAL`, `CHUNK_GENERATION`, `SPAWNER`, `STRUCTURE`, `BREEDING`, `MOB_SUMMONED`, `JOCKEY`, `EVENT`, `CONVERSION`, `REINFORCEMENT`, `TRIGGERED`, `BUCKET`, `SPAWN_ITEM_USE`, `COMMAND`, `DISPENSER`, `PATROL`, `TRIAL_SPAWNER`, `LOAD`, `DIMENSION_TRAVEL` |
| `Entity` (implements `EntityLoadData`) | `spawnReason()` → `@Nullable EntitySpawnReason`, `isLoadedFromDisk()` → `boolean` |
| `LivingEntityRenderer<T,S,M>` | `extractRenderState(T entity, S state, float partialTicks)` — **public**, called per-frame per-entity |
| | `getTextureLocation(S state)` — **abstract**, returns `Identifier` |
| | `createRenderState()` — from `EntityRenderer`, returns `S` |
| `LivingEntityRenderState` | Extends `EntityRenderState`; created fresh per-frame via `createRenderState()` |
| `ZombieRenderer` | Extends `MobRenderer<Zombie, ZombieRenderState>` → `LivingEntityRenderer<Zombie, ZombieRenderState, ZombieModel>` |
| `ZombieRenderState` | Extends `MobRenderState` → `LivingEntityRenderState` |

### Vanilla Mob Conversion (26.1.2)
- Zombie → Drowned: 30s submerged → 15s shaking → **new Drowned entity created**, old Zombie discarded
- Equipment preserved; health reset to full
- `ServerLivingEntityEvents.MOB_CONVERSION` fires with old and new entity references
- `copyOnDeath()` on Data Attachments **explicitly covers** "when a mob is converted (e.g. zombie → drowned)" per Fabric API docs

---

## B. Corrections from Previous Architecture

| Previous Claim | Correction |
|----------------|------------|
| `getTexture(S)` method name | **Official mappings: `getTextureLocation(S)`** (Yarn: `getTexture`) |
| `updateRenderState(T, S, float)` | **Official mappings: `extractRenderState(T entity, S state, float partialTicks)`** |
| `isLoadedFromDisk == false` = fresh spawn | **Must also check `spawnReason != LOAD && spawnReason != DIMENSION_TRAVEL`** — chunk loads have `spawnReason = LOAD`, `isLoadedFromDisk = true` |
| `spawnReason = null` for chunk loads | **`spawnReason = LOAD`** for chunk loads (per `EntityLoadData` docs) |
| Weighted selection needed for MVP | **Not needed** — single biome-tag match per entity type suffices |
| `Map<String, Object>` for behaviors | **Rejected** — use discriminated sealed interface with typed Codecs |
| UUID changes when definition changes | **Wrong** — UUID derived from variant ID + attribute path; **stable across reloads** |
| Conversion handler required for MVP | **Deferred** — `copyOnDeath()` handles KEEP policy; conversion support optional |
| Per-mob Mixins needed | **False** — single Mixin on `LivingEntityRenderer` intercepts all subclasses |
| ALLOW_LOAD is safe for biome lookup | **Conditional** — safe ONLY at `entity.getBlockPos()`; deadlock risk if chunk-loading APIs called |

---

## C. Final MVP Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  MOB VARIANTS BRS — MVP (zombie/ice only)                   │
├─────────────────────────────────────────────────────────────┤
│  STORAGE                                                     │
│  └── AttachmentType<Identifier> VARIANT_ATTACHMENT          │
│       .persistent(Identifier.CODEC)                         │
│       .copyOnDeath()                                        │
│       .syncWith(Identifier.STREAM_CODEC, all())             │
├─────────────────────────────────────────────────────────────┤
│  SPAWN SELECTION                                             │
│  └── ServerEntityEvents.ALLOW_LOAD                          │
│       if (hasAttached) return;                              │
│       if (isLoadedFromDisk) return;                         │
│       if (spawnReason == CONVERSION) return;                │
│       if (spawnReason == LOAD || spawnReason == DIMENSION_TRAVEL) return;│
│       select by biome tag → setAttached → apply attributes  │
├─────────────────────────────────────────────────────────────┤
│  ATTRIBUTES                                                  │
│  └── Transient modifiers, UUID = hash("mob_variants_brs:" + variantPath + "." + attributePath) │
│       apply on spawn, reapply on ENTITY_LOAD                │
├─────────────────────────────────────────────────────────────┤
│  BEHAVIOR                                                    │
│  └── Sealed interface BehaviorEntry                         │
│       FreezeOnHitEntry(duration, amplifier, chance)         │
│       BehaviorRegistry.execute(attacker, behaviors, victim, source) via AFTER_DAMAGE │
├─────────────────────────────────────────────────────────────┤
│  RENDERING (2 Mixins)                                        │
│  ├── LivingEntityRenderStateMixin: +Identifier variantId    │
│  └── LivingEntityRendererMixin:                             │
│       extractRenderState(TAIL) → entity attachment → state  │
│       getTextureLocation(HEAD, cancellable) → state.variantId → texture │
├─────────────────────────────────────────────────────────────┤
│  CONVERSION                                                  │
│  └── KEEP via copyOnDeath() — no handler needed for MVP     │
├─────────────────────────────────────────────────────────────┤
│  RESOURCES                                                   │
│  ├── data/mob_variants_brs/variants/zombie/ice.json         │
│  └── assets/mob_variants_brs/textures/entity/zombie/ice.png │
└─────────────────────────────────────────────────────────────┘
```

---

## D. Final Mixin Strategy

| # | Target Class | Target Method | Injection | Purpose | Why Required |
|---|--------------|---------------|-----------|---------|--------------|
| 1 | `LivingEntityRenderState` | (field) | `@Unique` | Add `Identifier variantId` field | No API to extend render state; state created per-frame |
| 2 | `LivingEntityRenderer` | `extractRenderState(T, S, float)` | `@At("TAIL")` | Copy `entity.getAttached(VARIANT_ATTACHMENT)` → `state.variantId` | Only hook with access to both entity and state |
| 3 | `LivingEntityRenderer` | `getTextureLocation(S)` | `@At("HEAD"), cancellable=true` | Return variant texture from `state.variantId` | Abstract method; concrete renderers (ZombieRenderer) call it |

**Why this works for ZombieRenderer**: `ZombieRenderer` extends `MobRenderer` → `LivingEntityRenderer`. The abstract `getTextureLocation(S)` in `LivingEntityRenderer` is implemented by `ZombieRenderer`. Mixin injection at `HEAD` on the **abstract declaration** intercepts the call site in `EntityRenderer.getAndUpdateRenderState()` → `getTextureLocation(state)`.

**Verified by**: Epsilon client (26.1.x) uses `@Mixin(LivingEntityRenderer)`; Tiny-Takeover-Backport uses `@WrapOperation` on `getTextureLocation`.

---

## E. Final Spawn Strategy

| Scenario | `spawnReason` | `isLoadedFromDisk` | Variant Selection |
|----------|---------------|---------------------|-------------------|
| Natural spawn | `NATURAL` | `false` | ✅ Select |
| Chunk generation | `CHUNK_GENERATION` | `false` | ✅ Select |
| Spawner | `SPAWNER` | `false` | ✅ Select |
| Structure (igloo, etc.) | `STRUCTURE` | `false` | ✅ Select |
| Spawn egg | `SPAWN_ITEM_USE` | `false` | ✅ Select |
| `/summon` | `COMMAND` | `false` | ✅ Select |
| Trial spawner | `TRIAL_SPAWNER` | `false` | ✅ Select |
| Patrol | `PATROL` | `false` | ✅ Select |
| Reinforcement | `REINFORCEMENT` | `false` | ✅ Select |
| Dispenser | `DISPENSER` | `false` | ✅ Select |
| Breeding | `BREEDING` | `false` | ❌ MVP: none |
| Conversion | `CONVERSION` | `false` | ❌ Deferred to MOB_CONVERSION |
| **Chunk load** | `LOAD` | `true` | ❌ CONSERVE |
| **Dimension travel** | `DIMENSION_TRAVEL` | `true` | ❌ CONSERVE |
| **Entity restore** | `LOAD` | `true` | ❌ CONSERVE |

**Guard Logic**:
```java
if (living.hasAttached(VARIANT_ATTACHMENT)) return; // CONSERVE
if (isLoadedFromDisk) return; // Pre-mod entity or chunk load
if (spawnReason == EntitySpawnReason.CONVERSION) return; // Handled separately
if (spawnReason == EntitySpawnReason.LOAD || spawnReason == EntitySpawnReason.DIMENSION_TRAVEL) return; // Explicit guards
// Fresh spawn → select
```

**Deadlock Safety**: Callback only reads biome at `entity.getBlockPos()` (already loaded), checks registry (in-memory), sets attachment. No worldgen or chunk loading calls.

---

## F. Final Implementation Order

| Phase | Tasks | Validation |
|-------|-------|------------|
| **0** | Remove template files (`ExampleMixin*`, `ExampleClientMixin*`, their JSONs); complete `fabric.mod.json`; register `VARIANT_ATTACHMENT` in `onInitialize` | `./gradlew build` passes; server/client start |
| **1** | `VariantDefinition`, `SpawnCondition` (biome tag only), `AttributeModifierEntry`, `FreezeOnHitEntry`, `VariantRegistry`, `VariantLoader`, reload listener, datagen | JSON loads; `VariantRegistry.get(id)` returns def |
| **2** | `VariantSelector` (biome tag match only), `ALLOW_LOAD` handler with 4 guards | Zombie in `#is_cold` biome → attachment set; `/data get` shows ID |
| **3** | `VariantApplicator` (transient modifiers, deterministic UUID), `ENTITY_LOAD` reapply, persistence/sync config | Attributes persist save/load; conversion preserves attachment |
| **4** | `BehaviorRegistry`, `freeze_on_hit`, `AFTER_DAMAGE` wiring (attacker-side) | Hit ice zombie → victim gets Slowness |
| **5** | 2 Mixins, `VariantRegistryClient` | Ice zombie renders custom texture |
| **6** | Edge cases, dedicated server test, docs | All MVP criteria met |

---

## G. Approval Blockers — STATUS

| # | Blocker | Resolution |
|---|---------|------------|
| 1 | Rendering API signatures | ✅ Verified: official mappings use `extractRenderState(T, S, float)` and `getTextureLocation(S)` |
| 2 | ALLOW_LOAD deadlock safety | ⚠️ **MITIGATED** — Only read biome at `entity.getBlockPos()` (already loaded chunk). Do NOT call `level.getChunk()`, `level.getBiome(otherPos)`, or any worldgen API. |
| 3 | copyOnDeath() on conversion | ✅ Verified: Fabric API docs explicitly state "when a mob is converted (e.g. zombie → drowned)" |
| 4 | Client texture resolution | ✅ Verified: client loads same datapack JSONs via `AddReloadListenerCallback`; no custom networking needed |

---

## H. Variant Data Model (MVP)

```json
{
  "base_entity": "minecraft:zombie",
  "spawn_condition": { "biome_tag": "minecraft:is_cold" },
  "texture": "mob_variants_brs:entity/zombie/ice",
  "attributes": [
    { "attribute": "minecraft:generic.max_health", "operation": "add_value", "amount": 4.0 },
    { "attribute": "minecraft:generic.armor", "operation": "add_value", "amount": 2.0 }
  ],
  "behavior": { "type": "freeze_on_hit", "duration": 100, "amplifier": 1, "chance": 0.3 }
}
```

- **Variant ID**: Derived from resource path → `mob_variants_brs:zombie/ice`
- **No redundant `variant_id` field in JSON**
- **No weights, dimensions, difficulty, light-level, conversion policies**

---

## I. Proposed Package Structure

```
com.baruc.brs.mobvariants
├── MobVariantsBRS.java
├── variant/
│   ├── VariantAttachment.java          # AttachmentType<Identifier>
│   ├── VariantDefinition.java          # Record + Codec
│   ├── VariantRegistry.java            # Immutable snapshot (server+client)
│   ├── VariantLoader.java              # ReloadListener
│   ├── VariantSelector.java            # Biome tag match
│   └── VariantApplicator.java          # Transient modifiers
├── spawn/
│   └── SpawnCondition.java             # biomeTag only
├── attribute/
│   └── AttributeModifierEntry.java     # Record + Codec + UUID gen
├── behavior/
│   ├── BehaviorEntry.java              # Sealed interface + FreezeOnHitEntry
│   ├── BehaviorRegistry.java           # Typed handler registry
│   └── BuiltinBehaviors.java           # Register freeze_on_hit
├── render/
│   ├── VariantRegistryClient.java      # Client registry access
│   └── mixin/
│       ├── LivingEntityRendererMixin.java
│       └── LivingEntityRenderStateMixin.java
└── conversion/
    └── ConversionHandler.java          # Optional post-MVP
```

---

## J. Ready for Implementation

All critical APIs verified against actual 26.1.2 runtime and Fabric API 0.155.3+26.1.2 JARs. Remaining blocker #2 (ALLOW_LOAD deadlock) is mitigated by strict biome lookup at entity position only.