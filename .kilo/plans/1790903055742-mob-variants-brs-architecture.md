# Mob Variants BRS — Architecture Proposal (v1)

Status: **proposed, not implemented.** No source file has been changed.

Scope note: this document defines the core layer only. Stat/balance/probability
mathematics are explicitly **out of scope**; only the seams that will accept them
later are defined.

---

## 0. Verified environment facts

Everything in this section was read out of the project's own caches, extracted
module jars, or the version-pinned Fabric API javadoc. Nothing is assumed.

| Item | Value | How it was verified |
| --- | --- | --- |
| Minecraft | 26.1.2 | `gradle.properties:8` |
| Loader | 0.19.5 | `gradle.properties:9` |
| Fabric API | 0.155.3+26.1.2 | `gradle.properties:17`; jar `META-INF/jars` listing |
| Loom | 1.18-SNAPSHOT | `gradle.properties:10` |
| Java | 25 | `build.gradle:50,59-60` |
| **Mappings** | **Mojang official (mojmap)** | `.gradle/loom-cache/source_mappings/bc646172….tiny` line 1 is `tiny  2  0  official`; and `MobVariantsBRS.java:5` imports `net.minecraft.resources.Identifier` (a mojmap name) |
| `mappings` dependency | **not declared** in `build.gradle` | Loom 1.18 default is official mappings; see above. Left as-is (see §5.3) |
| Source sets | `main` + `client` (split) | `build.gradle:15-23` |
| Existing mixins | 2, both example scaffold | `ExampleMixin`, `ExampleClientMixin` |
| Mod source files | 4, all example scaffold | `src/main`, `src/client` trees |
| Mod data/resources | only `assets/mob_variants_brs/icon.png` | `src/main/resources` tree |

### 0.1 Obsolete / leftover files (deliverable item 7)

* `build/resources/main/data/mob_variants_brs/variants/zombie/ice.json` — **stale build
  output from a previous experiment.** It does *not* exist in `src/`; the source was
  already deleted. It is not part of the repo's inputs and disappears on
  `gradlew clean`. No action needed beyond not being confused by it.
* `src/main/java/.../mixin/ExampleMixin.java` — injects into
  `MinecraftServer.loadLevel()`. Dead scaffold, no behaviour. **Delete.**
* `src/client/java/.../client/mixin/ExampleClientMixin.java` — injects into
  `Minecraft.run()`. Dead scaffold. **Delete.**
* Both mixin configs must then list an empty `mixins` array (or the file is removed
  and its `fabric.mod.json` reference dropped).
* `fabric.mod.json` `description`/`authors`/`contact` are still the example-template
  placeholders ("Me!", fabricmc.net URLs). Cosmetic, but worth replacing.
* `.kilo/worktrees/tan-cement/` is an Agent Manager worktree holding a clean copy of
  the scaffold. Unrelated to this design; leave it alone.

### 0.2 Fabric API modules actually present (from `META-INF/jars`)

Relevant ones: `fabric-data-attachment-api-v1-2.2.10`, `fabric-lifecycle-events-v1-4.1.1`,
`fabric-entity-events-v1-5.0.2`, `fabric-resource-loader-v1-2.0.10`,
`fabric-serialization-api-v1-2.0.3`, `fabric-networking-api-v1-6.3.2`,
`fabric-rendering-v1-23.3.1`, `fabric-content-registries-v0-11.3.1`,
`fabric-convention-tags-v2-4.6.2`, `fabric-object-builder-api-v1-23.1.1`,
`fabric-biome-api-v1-18.0.5`.

**Note two breaking changes vs. older Fabric API knowledge, both confirmed here:**

* `ResourceManagerHelper` is **deprecated/removed** → use
  `net.fabricmc.fabric.api.resource.v1.ResourceLoader` and
  `net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener`.
* `PayloadTypeRegistry.playS2C()/c2s()` are **absent** → now
  `PayloadTypeRegistry.clientboundPlay()/serverboundPlay()`.
* `ServerEntityWorldChangeEvents` is **absent** → now
  `ServerEntityLevelChangeEvents`.

---

## 1. Architecture

### 1.1 The three layers

```
 ┌─ DEFINITION LAYER ────────────────────────────────────────────┐
 │ data/mob_variants_brs/variants/<entity>/<name>.json          │
 │   → SimpleReloadListener (async parse)                        │
 │   → Immutable Map snapshot (atomic swap)                      │
 │   → VariantRegistry                                           │
 └───────────────────────────────────────────────────────────────┘
              ▲ selection (spawn only)          ▲ lookup (render)
              │                                 │
 ┌─ STATE LAYER ─────────────┐   ┌─ RENDER LAYER ─────────────────┐
 │ Entity                     │   │ vanilla renderer               │
 │  └ AttachmentType<Id>      │   │  └ 1 client Mixin              │
 │     Identifier (variant id) │◄──┤     reads attachment only     │
 └─────────────────────────────┘   └───────────────────────────────┘
```

Vanilla `Entity` / AI / movement / combat / equipment / model / renderer are
untouched. The variant is a *label* plus a *definition lookup*. Nothing about
the entity's vanilla behaviour is replaced or duplicated.

### 1.2 The one stateful thing: an attachment holding an `Identifier`

The entity carries exactly one piece of variant state: the variant's
`Identifier`. Everything else is derived, immutable, and shared.

This is the single most important decision in the design:

* it is *small* (one NBT string, ~30 bytes/instance);
* it is *authoritative on the server* — the client is told what the id is, it
  does not decide anything;
* it makes "select once, never re-evaluate" structurally impossible to violate,
  because there is nothing to re-evaluate;
* it means a definition change on `/reload` updates *all* varianted mobs at once
  without touching a single entity.

### 1.3 Why the Data Attachment API is the right substrate

`fabric-data-attachment-api-v1` gives persistence, automatic client
synchronisation, and chunk-load synchronisation from Fabric itself. This is
exactly the "prefers stable concepts" list in the brief, and it means the mod
ships **no networking code at all** — not a single packet, not a
`ServerPlayNetworking` receiver, not a hand-rolled NBT mixin.

`Entity` is an attachment target via a class tweaker, not via our mixin:

```
transitive-inject-interface  net/minecraft/world/entity/Entity  …/AttachmentTarget
```
(from `fabric-data-attachment-api-v1.classtweaker` — verified)

---

## 2. Runtime lifecycle

| # | Point | What happens | Cost |
| --- | --- | --- | --- |
| 1 | Mod init (main) | register `AttachmentType`, register reload listener, register `ENTITY_LOAD` listener | once |
| 2 | Client init | register client-side reload listener (if the common one turns out to be server-only) | once |
| 3 | `/reload`, async `prepare` | read `data/mob_variants_brs/variants/**.json`, decode to `VariantDefinition` list | off-thread |
| 4 | `/reload`, sync `apply` | build two immutable maps, publish to a `volatile` field | one atomic swap |
| 5 | **Entity spawn** (`ENTITY_LOAD`) | `if (entity.isLoadedFromDisk()) return;` → `selector.select(...)` → `entity.setAttached(VARIANT, id)` | once per spawn |
| 6 | **Entity load from disk** | NBT decode populates the attachment; selection returns on the boolean check | one boolean |
| 7 | Client sees entity | attachment sync payload arrives automatically; entity re-enters render | automatic |
| 8 | **Per tick** | **nothing.** No tick listener is registered at all. | **zero** |
| 9 | **Per render** | attachment get → `VariantRegistry` get → precomputed texture `Identifier` | 2 hash lookups |

### 2.1 Why selection cannot run on load — the verified guard

This was the highest-risk requirement. It is satisfied *without a Mixin*:

* `ServerEntityEvents.ENTITY_LOAD` fires for both spawn and NBT load, so it is
  not sufficient on its own.
* But in 26.1.2 `net.minecraft.world.entity.Entity` itself implements
  `net.fabricmc.fabric.api.event.lifecycle.v1.EntityLoadData`, which exposes
  `default boolean isLoadedFromDisk()` and
  `default @Nullable EntitySpawnReason spawnReason()`.

So the guard is a plain `entity.isLoadedFromDisk()` call on the entity we are
already handed. No event-argument inspection, no mixin, no reflection.

`spawnReason()` is available for free and is the correct future input for
"spawned naturally vs. summoned" rules — recorded here as the seam, deliberately
unused in v1.

### 2.2 Ordering guarantee for step 5

`ENTITY_LOAD` is documented as "Called when an Entity is loaded into a
ServerLevel. When this event is called, the entity is already in the level."
That is the earliest point at which the entity is a legitimate, addressable
world object, and the last point before it can start acting. Attributes and
equipment applied here are applied to a fully-constructed vanilla entity.

---

## 3. Performance analysis

### 3.1 Operations executed once

* Attachment registration, reload-listener registration, event registration.
* JSON parse and codec decode (async, off the server thread).
* Building the immutable `Map` snapshot at `apply`.
* Precomputing each definition's resolved texture `Identifier` at `apply`, so the
  render path never constructs an `Identifier` or does string work.

### 3.2 Operations executed per entity spawn (server)

```
ENTITY_LOAD callback
  → entity.isLoadedFromDisk()            // boolean
  → entity.getType()                     // field read
  → VariantRegistry.variantsFor(type)    // 1 volatile read + 1 map get
  → selector.select(...)                 // v1: take element 0
  → entity.setAttached(VARIANT, id)      // 1 map insert + queue sync
```

No biome query, no registry iteration, no JSON, no allocation on the common
"no variant exists" path beyond the listener dispatch itself.

### 3.3 Operations executed per tick

**None.** No `ServerTickEvents`, no `EntityTickEvents`, no `ServerEntityEvents`
tick hook, no global scan of `ServerLevel.getAllEntities()`. This is enforced by
construction: the architecture registers no tick listener anywhere.

The one caveat is Fabric's `ServerEntityEvents.EQUIPMENT_CHANGE`, which itself
does work inside `LivingEntity.tick()`. **v1 must not subscribe to it** — it
exists only to feed the future attribute pass (§9).

### 3.4 Operations executed per render (client)

```
Mixin on the vanilla texture lookup
  → entity.hasAttached(VARIANT)          // per-entity attachment map probe
  → entity.getAttached(VARIANT)          // Identifier, already interned
  → VariantRegistry.get(id)              // 1 volatile read + 1 map get
  → definition.texture()                 // precomputed Identifier field
  → return
```

* Zero allocations per frame.
* Zero `Identifier` construction (the ids are interned at reload and stored on
  the definition).
* Zero registry traversal, zero resource loading, zero JSON, zero reflection.
* This runs only for entities actually on screen, not for every entity in a
  chunk.

Two hash lookups per rendered mob is not worth optimising further. The tempting
"optimisation" — caching the resolved texture in a second attachment — would
duplicate state and create an invalidation problem on `/reload` for a gain that
is unmeasurable. **Rejected.**

### 3.5 Avoidable allocations

| Candidate | Decision |
| --- | --- |
| Attachment default instance | **No `initializer()` registered.** An entity with no variant allocates *nothing*; `hasAttached` returns false. This is why the v1 hook is allowed to be a no-op. |
| Per-entity `VariantDefinition` | Rejected. Only the `Identifier` is stored per entity; the definition is shared and immutable. |
| Per-frame texture `Identifier` | Rejected; precomputed at reload. |
| `Map` for the registry | `Map.copyOf` at reload, then only reads. |

### 3.6 Memory cost

* Per varianted entity: one `Identifier` reference (shared, interned) + one entry
  in Fabric's per-entity attachment map + ~30 bytes of NBT string.
* Per *un*varianted entity: zero, provided Fabric's attachment storage is lazily
  allocated. (Inferred — `AttachmentTargetImpl` is not inspectable here. Even if
  it allocates a small map eagerly, that is a fixed per-entity cost shared by
  every Fabric API user and is not a reason to avoid the API.)
* Global: one map of `N` definitions, where `N` is the number of variant JSON
  files. Negligible.

---

## 4. Compatibility analysis

### 4.1 The layer boundary

Everything vanilla does stays vanilla:

```
Vanilla Zombie
    AI, movement, navigation, collision, combat, lifecycle,
    equipment, attributes, animations, model, renderer
        + (unmodified)
Variant
    optional texture        ← the only thing v1 can express
    optional attributes     ← later
    optional behaviour      ← later
```

If Mojang changes Zombie's goals, movement, attributes, equipment or renderer,
the variant inherits the change automatically, because the variant holds an
`Identifier` and never touches the entity's behaviour.

### 4.2 What is *not* assumed about vanilla internals

* No `instanceof Zombie` check anywhere.
* No giant switch on entity type.
* No `Map<String, Object>`.
* No assumption about `Zombie`'s fields, methods, goals, or attributes.
* No custom `EntityType`, no custom entity class, no custom model, no custom
  renderer.

The only vanilla types named in the whole design are `Entity`,
`LivingEntity`/`Mob` (as a render-time parameter), `Identifier`, `Codec`,
`StreamCodec`, `EntityType` — all long-stable, all reachable through Fabric
API signatures rather than through our own mixins.

### 4.3 Resilience to future Minecraft versions

* Variant definitions are data. A vanilla behaviour change needs no action.
* Selection happens once and is persisted. A vanilla spawn-pipeline change
  cannot "re-roll" variants, because there is no re-roll path.
* If a variant definition is deleted, the attachment id simply stops resolving;
  the mod must treat "unknown id" as "no variant" and fall back to vanilla
  rendering. Design this in from the start.
* `/reload` is atomic: old snapshot stays live until the new one is published,
  and entities hold ids (not definitions), so a reload can never desynchronise
  an entity from the registry.

---

## 5. API analysis

Legend: **V** = verified against this project's Fabric API 0.155.3+26.1.2
javadoc or extracted jar · **I** = inferred, must compile to confirm ·
**U** = uncertain, do not rely on.

### 5.1 Verified

| API | Verified fact |
| --- | --- |
| `net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry` | `create(Identifier)`, `create(Identifier, Consumer)`, `createPersistent(Identifier, Codec)`, `createDefaulted(Identifier, Supplier)`. `builder()` exists but is **deprecated** — use `create`. |
| `AttachmentRegistry.Builder<A>` | `initializer(Supplier<A>)`, `persistent(Codec<A>)`, `syncWith(StreamCodec<? super RegistryFriendlyByteBuf, A>, AttachmentSyncPredicate)`, `syncWith(…, int maxSyncSize)`, `copyOnDeath()`, `buildAndRegister(Identifier)` |
| `AttachmentTarget` | `setAttached`, `getAttached`, `hasAttached`, `getAttachedOrCreate`, `getAttachedOrElse`, `getAttachedOrSet`, `getAttachedOrThrow`, `modifyAttached`, `removeAttached`, `onAttachedSet`. **No** `hasAttachments()`/`removeAttachment()`. |
| `AttachmentSyncPredicate` | `all()`, `allButTarget()`, `targetOnly()` |
| `net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents` | `ALLOW_LOAD`, `ENTITY_LOAD`, `ENTITY_UNLOAD`, `EQUIPMENT_CHANGE`; nested `Load.onLoad(Entity, ServerLevel)`, `AllowLoad.onAllowLoad(Entity, ServerLevel, EntitySpawnReason, boolean)` |
| `net.fabricmc.fabric.api.event.lifecycle.v1.EntityLoadData` | `isLoadedFromDisk()`, `spawnReason()`; implemented by vanilla `Entity`, `LivingEntity`, `Player`, `ServerPlayer` in 26.1.2 |
| `net.fabricmc.fabric.api.resource.v1.ResourceLoader` | `registerReloadListener(Identifier, PreparableReloadListener)`, `get(PackType)`, `addListenerOrdering`, `REGISTRY_LOOKUP_KEY` |
| `net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener<T>` | `prepare(SharedState)`, `apply(T, SharedState)`, `reload(SharedState, Executor, PreparationBarrier, Executor)` |
| `…reloader.ResourceReloaderKeys` | `BEFORE_VANILLA`, `AFTER_VANILLA`, `Client.TEXTURES`, `Client.MODELS`, `Client.ENTITY_RENDER_DISPATCHER`, `Client.EQUIPMENT_ASSETS`, `Server.ADVANCEMENTS`, `Server.FUNCTIONS`, `Server.RECIPES` |
| **Absence** of a per-entity texture hook | `LivingEntityFeatureRenderEvents` has exactly one field, `ALLOW_CAPE_RENDER`. No base-texture, no feature, no armour, no head-item hooks in 0.155.3+26.1.2 |
| `net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry` | `MODIFY`, `ModifyContext.modifyAll/modify(Collection,…)/modify(Predicate,…)/modify(EntityType,…)`, `ModifyConsumer.accept(EntityType, AttributeSupplier.Builder)` — **per-EntityType only**; see §9.3 |
| `net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry` | `clientboundPlay()`, `serverboundPlay()`, `register(CustomPacketPayload.Type, StreamCodec)` — present but **not needed** in this design |

### 5.2 Inferred — must be confirmed by the compiler

* `net.minecraft.world.entity.EntitySpawnReason` — the *type* exists (it appears
  in `EntityLoadData.spawnReason()`'s signature, which is verified); its
  constant names are not.
* `Identifier.CODEC` and the matching `StreamCodec<RegistryFriendlyByteBuf,
  Identifier>` for the attachment's `persistent`/`syncWith`. The javadoc confirms
  `Identifier` and `com.mojang.serialization.Codec` are the right types, but not
  the constant names. **Fallback if absent:** derive both locally from
  `Codec.STRING` / `ByteBufCodecs` with a `comapFlatMap` through
  `Identifier` parse / `toString`. Do not guess the constant name at write time.
* `PreparableReloadListener.SharedState.resourceManager()` — how the JSON is
  actually read. Verify before writing the loader body.
* Whether `ResourceLoader.registerReloadListener(Identifier,
  PreparableReloadListener)` registers on **both** the server and client
  resource managers from a single call, or whether the client needs its own
  registration from the client entrypoint. The javadoc shows one
  `registerReloadListener` and a separate `get(PackType)`, which suggests a
  single registration covers both — but this is not stated. **Design for the
  possibility that the client needs its own registration**, and confirm.
* The exact 26.1.2 vanilla signature of the renderer texture lookup (§6).

### 5.3 Deliberately not done

* **No `mappings` line will be added to `build.gradle`.** The project already
  builds against official mappings via the Loom 1.18 default (verified §0).
  Adding a speculative mapping declaration could only break that.
* **No `loom.officialMojangMappings()` reference** — exact Loom 1.18 DSL spelling
  is unverified and must not be guessed.

---

## 6. Mixins

### 6.1 Server-side: **none. No Mixin is required.**

State this explicitly because the brief asks for it. Variant selection,
persistence, and client synchronisation are all fully covered by Fabric API
0.155.3+26.1.2. Fabric's own `fabric-data-attachment-api-v1.mixins.json` does the
`Entity` NBT and chunk-send injection on our behalf.

### 6.2 Client-side: exactly one Mixin

**Target:** the vanilla method on the shared living/mob renderer base class that
resolves an entity's texture.

*In 1.21.x mojmap this was `LivingEntityRenderer#getTextureLocation(LivingEntity)`.
That name is **UNVERIFIED for 26.1.2**.* The 26.x render pipeline was
reworked (the javadoc exposes `AvatarRenderState`, `FabricRenderState`,
`RenderStateDataKey`, and
`LivingEntityRenderLayerRegistrationCallback#registerLayers(EntityType,
LivingEntityRenderer, RegistrationHelper, Context)`, where "layer" now means
something different from the old "model layer"). The first implementation task
is therefore: run `gradlew genSources` and read the actual class before writing
the injection.

| | |
| --- | --- |
| **Purpose** | When the entity carries a variant whose definition declares a texture, return that texture instead of the vanilla one. Otherwise fall through to vanilla. |
| **Why Fabric API is insufficient** | Verified absent: 0.155.3+26.1.2 exposes no per-entity base-texture hook. `LivingEntityFeatureRenderEvents` offers only `ALLOW_CAPE_RENDER`. |
| **Injection point** | One `HEAD` `Inject` with `cancellable = true` returning void and writing the return value, or MixinExtras `@ModifyReturnValue` if `mixinextras-fabric 0.5.5` (present on the dev classpath per `build/loom-cache/argFiles/runClient`) is available to the compile classpath. The one-boolean early-out keeps the cost of the common case at a single interface method call. |
| **Compatibility risk** | Medium. The *concept* (a renderer resolves a texture per entity) is stable across versions; the *name, signature, and receiver type* are not guaranteed. A render-pipeline rework could move the call entirely, in which case a return-value injector silently stops firing. |
| **Containment** | Exactly one class, one injection, no state, no helper reachable from elsewhere. The rest of the mod must remain fully functional if it stops applying. |
| **Failure mode** | Recommend `require = 0` plus a single `LOGGER.warn` on first application failure: a rename then degrades to "variants render with the vanilla texture" instead of hard-crashing the game on a Minecraft update. *Trade-off: a silent-ish degradation versus a loud startup failure. The default recommended here is `require = 0`; override this if you would rather fail loudly.* |

### 6.3 Alternatives considered and rejected

* **Wrap the vanilla renderer per `EntityType` via `EntityRendererRegistry`.**
  Rejected: requires one registration per mob type, and the wrapper must forward
  every behaviour the vanilla renderer has *and every behaviour Mojang adds in
  future*. It duplicates the thing the brief forbids duplicating, and it breaks
  harder than a mixin.
* **Use `LivingEntityRenderLayerRegistrationCallback`.** Rejected: verified it
  registers render *layers*, not the base body texture.
* **Custom `ZombieRenderer` / `ZombieModel` / `ZombieEntity`.** Rejected outright.
* **Encode the texture in a vanilla-synced place (e.g. custom name /
  custom data component).** Rejected: leaks presentation into gameplay-visible
  data, and is a compatibility and cheating surface.

---

## 7. File structure

Deliberately flat. No package is created for a feature that does not exist yet.

```
src/main/java/com/baruc/brs/mobvariants/
  MobVariantsBRS.java                 mod entrypoint: wire everything together
  attachment/
    VariantAttachments.java           the one AttachmentType<Identifier>
  definition/
    VariantDefinition.java            immutable record + Codec + identifier codec
    VariantDefinitionLoader.java      SimpleReloadListener: JSON -> definitions
  registry/
    VariantRegistry.java              volatile immutable snapshot + lookups
  selection/
    VariantSelector.java              interface  ← the v1 extension point
    FirstDefinitionSelector.java      v1 implementation: element 0 of the list

src/client/java/com/baruc/brs/mobvariants/client/
  MobVariantsBRSClient.java           client entrypoint (loader registration)
  mixin/
    MobRendererMixin.java             the single client Mixin (client mixin config)

src/main/resources/
  fabric.mod.json                     clean up placeholders
  mob_variants_brs.mixins.json        emptied (no server mixins)
  data/mob_variants_brs/variants/
    zombie/ice.json                   first definition, proves the path scheme

src/client/resources/
  mob_variants_brs.client.mixins.json contains MobRendererMixin
  assets/mob_variants_brs/textures/entity/zombie/zombie_ice.png
```

`definition/`, `registry/`, `selection/` and `attachment/` earn their existence
now: each has exactly one implementation. `render/` is *not* created — the only
client logic is the mixin plus one lookup helper that can live in the mixin's
own package until it grows.

---

## 8. Data model

### 8.1 Minimal `VariantDefinition` (v1)

```
record VariantDefinition(
    Identifier  id,          // mob_variants_brs:zombie/ice  (derived from file path)
    Identifier  entityType,  // minecraft:zombie             (derived from file path)
    Optional<Identifier> texture   // assets/mob_variants_brs:entity/zombie/zombie_ice
)
```

Three fields. Two are derived from the file path
(`data/mob_variants_brs/variants/<entityTypePath>/<variantName>.json`), so the
path is the single source of truth for identity and no field can contradict it.
`texture` is the only authored field, and it is optional.

`zombie/ice` as a variant *name* is not supported in v1; use
`zombie/frost_ice.json`. A variant is always scoped under exactly one entity
type. That keeps `variantsFor(EntityType)` a simple map get and makes the
"multiple mobs" case fall out of the path scheme for free.

### 8.2 Rules the model follows

* Immutable record, no builders, no mutable collections.
* Every field is either required-and-derived or `Optional`. A variant must be
  expressible with only an identity and nothing else — **a variant with an empty
  `{}` body is valid** and means "plain vanilla mob, tagged with this identity".
  This is what makes the v1 "no selection logic" state coherent.
* Evolvable: later fields are added as `Optional` with defaults in the codec.
  Old files keep parsing.
* No `Map<String, Object>`, no free-form blobs.

### 8.3 Selection seam (v1)

```
interface VariantSelector {
    @Nullable Identifier select(SelectionContext ctx);
}
```

`SelectionContext` carries the entity, the level, the entity type, the
candidate `List<VariantDefinition>`, `spawnReason()`, and the entity UUID.
Passing UUID + spawn reason up front is what lets the future UUID-hash or
biome/probability selectors be written without changing the call site.

**v1 implementation — `FirstDefinitionSelector`: returns the first candidate.**
Consequence, stated plainly: with only this selector, *every* zombie spawns as
the same variant. That is a deliberate placeholder, not a design. The seam
exists so the real selector replaces one class.

---

## 9. Future extension points

Each of these is a *seam that already exists in v1*, not a speculative framework.

### 9.1 Multiple mobs, multiple variants
Free. The path scheme encodes both dimensions and `VariantsFor(EntityType)` is a
map lookup. No code change to add the hundredth mob type.

### 9.2 Variant-specific textures
Implemented in v1.

### 9.3 Attributes
`FabricDefaultAttributeRegistry.MODIFY` operates on `AttributeSupplier.Builder`
per `EntityType` — it can only change *defaults for a type*, never per-instance
values. So it is **not** sufficient on its own. The correct future shape is:
keep the attachment as the source of truth, compute the effective modifiers once
at spawn (in the same `ENTITY_LOAD` listener that assigns the id), and apply
them to the live entity's `AttributeMap` instance there. That keeps vanilla
`AttributeMap` machinery, vanilla equipment modifiers, and vanilla
`AttributeInstance` semantics intact — and it stays zero per tick.
**No new mixin, no new tick listener, no new state.**

### 9.4 Behaviours
The trigger point is the one genuine gap in this Fabric API: there is **no
Fabric event for "a mob was spawned naturally"**. `ServerEntityEvents.ALLOW_LOAD`
is the closest and it carries `EntitySpawnReason`, but v1 must not depend on the
`boolean` parameter of that callback. Future behaviour that must fire exactly
once at spawn is handled *inline in the same `ENTITY_LOAD` listener*, driven by
the already-assigned variant id. Rule: **behaviour reacts to a variant id, never
to a tick.** A tick-based variant behaviour system is explicitly rejected.

### 9.5 Other variant-specific properties
Add `Optional` fields to the record and `Optional` branches to the codec. Field
order in JSON and in the record are independent; missing keys fall back to the
codec default. A file written for v1 parses forever.

### 9.6 Selection strategies
`VariantSelector` is a one-method interface. UUID-hash, biome-weighted, spawn-reason,
tag-gated and command-forced selectors are all additive. The only thing that
should ever be added to the interface is another *context input*, and only after
a real selector needs it.

---

## 10. Risks, by severity

| # | Risk | Severity | Mitigation |
| --- | --- | --- | --- |
| 1 | **The rendering Mixin target is unverified.** 26.x reworked the render pipeline; the 1.21.x name may be gone or may have moved. | **High** | Task 1 of implementation is `genSources` + read the real class. One class only. `require = 0` + warn so a rename degrades instead of crashing. Mod is fully functional without it. |
| 2 | `Identifier.CODEC` / its `StreamCodec` may not exist under those names. | Medium | Task 1 verifies both; a `Codec.STRING`-based local derivation is the prepared fallback. Compiler catches it. |
| 3 | `ResourceLoader.registerReloadListener` may register server-side only, leaving the client with an empty registry → no variant textures. | Medium | Verify in task 1; the client entrypoint exists specifically to host a second registration if needed. Symptom is silent (vanilla texture), so make it an explicit startup assertion/log, not an assumption. |
| 4 | Server-only datapack adds a variant the client does not have → the client cannot render it. | Medium | Inherent to the chosen "mod datapack + override" model. Define "unknown id" as "no texture override" and fall back to vanilla; log at debug, not warn, to avoid spam. |
| 5 | Fabric's per-entity attachment storage might allocate eagerly, costing a small map on *every* entity. | Low | Unavoidable and shared by every attachment-API user. Not a reason to leave the API. |
| 6 | A definition is removed/renamed while entities hold the old id. | Low | By design: entities store ids, lookups return empty, render falls back to vanilla. No crash, no desync. |
| 7 | Reload ordering — the variant listener must run before the texture listener. | Low | `ResourceLoader.addListenerOrdering(Identifier, Identifier)` is verified; use it. |
| 8 | `copyOnDeath()` semantics are not set, so a variant does **not** survive zombie→drowned conversion or a mob conversion. | Low (deliberate) | Leave unset in v1. `copyOnDeath()` is one builder call if the design ever wants it. |
| 9 | "Hook only" v1 means all mobs of a type share one variant. | Cosmetic | Intentional, stated in §8.3. |
| 10 | Datagen entrypoint currently lives in the `client` source set. | Low | Works; leave alone. Revisit only if datagen is actually used. |

---

## 11. Implementation task list

Ordered. Do not start task 2 before task 1 reports verified signatures.

1. **Verify signatures (no design changes).** Run `gradlew genSources`. Confirm
   and record, in a comment in the code that uses them:
   * the exact vanilla renderer method + receiver for the texture lookup;
   * `Identifier`'s `Codec` and `StreamCodec` constants (or the local fallback);
   * `PreparableReloadListener.SharedState` resource access path;
   * whether `ResourceLoader.registerReloadListener` covers the client.
2. Delete `ExampleMixin.java` and `ExampleClientMixin.java`; empty
   `mob_variants_brs.mixins.json`'s `mixins` array; clean the `fabric.mod.json`
   placeholders.
3. `VariantDefinition` — record + `Codec` + the identifier codec.
4. `VariantAttachments` — `AttachmentRegistry.create(id, b -> b.persistent(...).syncWith(...))`.
   **No `initializer()`.**
5. `VariantDefinitionLoader` — `SimpleReloadListener<...>`; parse in `prepare`,
   build maps in `apply`.
6. `VariantRegistry` — `volatile` immutable snapshot; `get(Identifier)` and
   `variantsFor(EntityType)`.
7. `VariantSelector` + `FirstDefinitionSelector`.
8. `MobVariantsBRS` wiring, including the `ENTITY_LOAD` listener with the
   `isLoadedFromDisk()` guard as the very first statement.
9. `src/main/resources/data/mob_variants_brs/variants/zombie/ice.json` + the
   texture asset.
10. Client: loader registration, then `MobRendererMixin`.
11. `ResourceLoader.addListenerOrdering` for variant listener → texture listener.

## 12. Validation

* `gradlew build` — compiles. This is the primary verification of every
  "inferred" item in §5.2.
* `gradlew runServer` — spawn a zombie; `/data get entity` shows the attachment
  in NBT; unload and reload the chunk; the id is unchanged (proves the
  `isLoadedFromDisk()` guard).
* `gradlew runClient` — the same zombie renders with the variant texture.
* `/reload` after editing the JSON — the change takes effect with no respawn.
* Rename a variant JSON and `/reload` — mob renders vanilla, no crash.
* Negative test: confirm no listener of ours is invoked during steady-state
  ticking (a `LOGGER` counter, removed afterwards, or a profiler run).

## 13. Explicitly out of scope

Health/damage/armour/scaling formulas, stat multipliers, balancing, probability
weighting, commands, GUI, tag-driven opt-in, and any form of variant behaviour
that runs on a tick. §9 defines only the seams.
