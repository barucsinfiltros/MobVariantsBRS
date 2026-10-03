# MobVariantsBRS — Variant State Layer Architecture

Status: **architecture only, not implemented.** No Java, resource, mixin config, Gradle, or
`docs/` file was created or modified by the task that produced this document. The only artefacts
of that task are the three plan files named in §0.3.

Stack (read from the repository, not assumed): MC `26.1.2`, Fabric Loader `0.19.5`,
Fabric API `0.155.3+26.1.2`, Java `25`, Loom `1.18-SNAPSHOT`, official Mojang mappings —
`gradle.properties:8-17`, `build.gradle:50,59-60`, `src/main/java/.../MobVariantsBRS.java:5`.

Classification legend used throughout:

- **[VF] Verified fact** — read out of a tracked repository file, or out of the *installed*
  Fabric API javadoc (`fabric-api-0.155.3+26.1.2`) / `FabricMC/fabric-api` sources / the
  26.1.2 mapping surface. Cited inline.
- **[AD] Architectural decision** — chosen in this document. Not implemented.
- **[AS] Assumption** — believed true, not fully pinned. Listed in §21 with its fallback.
- **[OQ] Open question** — unresolved. Listed in §21 with the decision owner.

---

## 1. Objective

Design the **smallest lifecycle-correct, server-authoritative Variant State Layer** for
MobVariantsBRS: the part of the mod that decides which variant a mob is, stores that decision on
the mob, persists it, and replicates it to clients — and hands the renderer a value it can use
without doing any work of its own.

The layer is responsible for exactly four things:

1. Loading variant definitions from the **server** resource manager.
2. Publishing an immutable, complete snapshot of those definitions, scoped to the running server.
3. Assigning a variant **once**, at the entity's first tracking event, and never again.
4. Persisting and synchronising that decision through Fabric's Data Attachment API.

Explicitly **out of scope** for this layer, and not designed here: rendering (owned by
`1790968433924-mob-variant-texture-rendering-plan.md`), networking code, commands, GUI,
configuration, probability weighting, new `EntityType`s, custom entity classes, health/stat/AI
systems, sounds, drops, and unrelated refactors.

---

## 2. Current repository state

### 2.1 What exists **[VF]**

Tracked Java sources — 4 files, all unmodified Fabric example-mod scaffold:

| File | Body |
|---|---|
| `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` | `ModInitializer`; `MOD_ID`, `LOGGER`, `id(String)`; logs `"Hello Fabric world!"` (line 24) |
| `src/main/java/com/baruc/brs/mobvariants/mixin/ExampleMixin.java` | inert `@Inject` HEAD into `MinecraftServer.loadLevel`, empty body |
| `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSClient.java` | empty `onInitializeClient()` |
| `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSDataGenerator.java` | empty `onInitializeDataGenerator()` |
| `src/client/java/com/baruc/brs/mobvariants/client/mixin/ExampleClientMixin.java` | inert `@Inject` HEAD into `Minecraft.run`, empty body |

Resources: `fabric.mod.json` (mod id `mob_variants_brs`, `environment: "*"`, three entrypoints,
two mixin configs, `depends` on `fabric-api: *`), `mob_variants_brs.mixins.json`
(package `…mobvariants.mixin`, lists `ExampleMixin`), `mob_variants_brs.client.mixins.json`
(package `…mobvariants.client.mixin`, `client` array lists `ExampleClientMixin`),
`assets/mob_variants_brs/icon.png`. **No `data/` directory exists.**

Build: `build.gradle:14-23` split source sets (`main` + `client`), `:31-38` dependencies are
`minecraft`, `fabric-loader`, `fabric-api` **only** — no test framework, no MixinExtras, no
`mappings` line. `:49-61` Java 25. No `src/test`. No lint/static-analysis task.

### 2.2 What does not exist **[VF]**

No registry, no snapshot, no attachment type, no event subscription, no persistence, no packet,
no configuration, no `data/` content, no datagen provider, no test. `docs/ARCHITECTURE.md:25-28`
and `docs/ROADMAP.md:39-40` say the same. `docs/ARCHITECTURE.md:201-214` records a
git-ignored stale `build/resources/main/data/mob_variants_brs/variants/zombie/ice.json` from a
deleted experiment — **not** a design input, and its schema (`base_entity`, optional `texture`)
is superseded by §6 below.

### 2.3 State/registry/attachment abstraction already in the repo **[VF]**

**None.** There are no `attachment/`, `definition/`, `registry/`, `selection/` or `variant/`
packages (`docs/ARCHITECTURE.md:189-190`). Everything in §6–§10 below is new.

### 2.4 Existing mixin usage **[VF]**

Two configs, both `required: true` with `injectors.defaultRequire: 1` and
`compatibilityLevel: "JAVA_25"` (`mob_variants_brs.mixins.json:2-13`,
`mob_variants_brs.client.mixins.json:2-13`). Both registered mixins have **empty** injected
bodies. **This layer adds zero mixins and changes zero mixin configs.**

### 2.5 Existing architectural decisions carried forward **[VF]**

From `docs/DECISIONS.md` (all still `PROPOSED`):

- **D-015** — no server-side mixin is required. Honoured and re-verified in §9.
- **D-013** — no per-tick listener anywhere. Honoured and re-verified in §16.
- **D-018** — path-derived variant identity. *Partly superseded* — see §6.2.
- **D-022** — no `initializer()` on the attachment. Honoured; now **verified** (§10.3).
- **D-012** — no custom networking code. Honoured and re-verified in §11.
- **D-023** — `copyOnDeath()` unset. Honoured (§10.4).

Superseded by this document: **D-011** (attachment payload — see §5) and **R-005**
(no second attachment — the reasoning is retained but the premise changed, see §22).

---

## 3. Verified API facts

All rows below were read from the **installed** Fabric API `0.155.3+26.1.2` javadoc or from
`FabricMC/fabric-api` sources for the matching release line, or from the 26.1.2 mapping surface.

### 3.1 Data Attachment API **[VF]**

| Fact | Source |
|---|---|
| `AttachmentRegistry.create(Identifier)`, `create(Identifier, Consumer<Builder<A>>)`, `createPersistent(Identifier, Codec<A>)`, `createDefaulted(Identifier, Supplier<A>)` all exist; `builder()` is `@Deprecated` | `…attachment.v1.AttachmentRegistry` javadoc |
| `Builder.persistent(Codec<A>)`, `.syncWith(StreamCodec<? super RegistryFriendlyByteBuf, A>, AttachmentSyncPredicate)`, `.syncWith(…, int maxSyncSize)`, `.initializer(Supplier<A>)`, `.copyOnDeath()`, `.buildAndRegister(Identifier)` | `AttachmentRegistry.Builder` javadoc |
| `AttachmentSyncPredicate.all()`, `.targetOnly()`, `.allButTarget()`; interface is `BiPredicate<AttachmentTarget, ServerPlayer>` | `AttachmentSyncPredicate` javadoc |
| `AttachmentTarget` implementors include `net.minecraft.world.entity.Entity`, `LivingEntity`, `Player`, `ServerPlayer`; `getAttached` returns `@Nullable`, `hasAttached` does **not** create data, `setAttached(type, null)` removes | `AttachmentTarget` javadoc |
| Targets are implemented by Fabric mixin/classtweaker on `Entity`, `BlockEntity`, `ServerLevel`, `ChunkAccess`; `GlobalAttachments` adds server-wide targets | `AttachmentTarget` javadoc, `fabric-data-attachment-api-v1.classtweaker` |
| Attachment storage is a **lazily created** `@Nullable IdentityHashMap` — an entity with no attachments allocates nothing | `AttachmentTargetsMixin` field `dataAttachments = null` |
| Persistence for an `Entity` is injected at `Entity.readAdditionalSaveData(ValueInput)` (read) and `Entity.addAdditionalSaveData(ValueOutput)` (write); data is stored under `AttachmentTarget.NBT_ATTACHMENT_KEY` | `…mixin/attachment/EntityMixin.java`, `AttachmentTarget.NBT_ATTACHMENT_KEY` |
| Setting an attachment on a server entity immediately calls `fabric_syncChange`, which fans out to `PlayerLookup.tracking(entity)` filtered by the predicate | `…mixin/attachment/EntityMixin#fabric_syncChange` |
| `fabric_computeInitialSyncChanges(player, …)` replays the full synced state to a **new** tracker; `AttachmentSync` wires it to `EntityTrackingEvents.START_TRACKING` | `…impl/attachment/sync/AttachmentSync.java#onInitialize` |
| `AttachmentSync` also wires `ServerPlayerEvents.JOIN` (global + level + player attachments) and `ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL` | same |
| A configuration-phase handshake (`ClientboundRequestAcceptedAttachmentsPayload` → `ServerboundAcceptedAttachmentsPayload`) records which syncable attachments the client accepts; the server **logs a warning** and skips sync for any the client lacks | `AttachmentSync#decodeResponsePayload` |
| Attachments are copied wholesale by default on respawn / mob conversion / End return / cross-level teleport, except where `copyOnDeath()` gates it | `AttachmentTargetImpl.transfer`, `AttachmentType` javadoc |

### 3.2 `GlobalAttachments` **[VF]**

| Fact | Source |
|---|---|
| `GlobalAttachments extends AttachmentTarget`; "server-wide data attachments that are not tied to any specific Level" | `GlobalAttachments` javadoc |
| Obtained via `GlobalAttachmentsProvider.globalAttachments()`, implemented by **`MinecraftServer`** and by `Level`; on the server its lifecycle is bound to the `MinecraftServer`, on the client to `ClientPacketListener` | `GlobalAttachmentsProvider`, `GlobalAttachments` javadoc; `…data-attachment-api-v1/…/MinecraftServerMixin` injects `initGlobalAttachments` at `<init>` TAIL |
| A `GlobalAttachments` attachment can be synced (`fabric_syncChange` iterates `server.getConnection().getConnections()`), but **this layer never syncs it** | `GlobalAttachmentsImpl#fabric_syncChange` |

### 3.3 Server resource loading **[VF]**

| Fact | Source |
|---|---|
| `ResourceLoader.get(PackType)` returns a **per-`PackType`** instance held in an `EnumMap`; `PackType.SERVER_DATA` resolves to `DataResourceLoaderImpl.INSTANCE`, everything else to a fresh `ResourceLoaderImpl(type)` | `…impl/resource/ResourceLoaderImpl.java#get`, `DataResourceLoaderImpl` |
| `registerReloadListener(Identifier, PreparableReloadListener)` only adds to **that** `PackType`'s listener map. Fabric listeners are injected into a reload pipeline **only** for the pipeline's own `PackType` (`ResourceLoaderImpl.sort(flrm.fabric$getPackType(), reloaders)`) | `ResourceLoaderImpl#registerReloadListener`, `#sort`; `…mixin/resource/SimpleReloadInstanceMixin` |
| ⇒ A listener registered on `get(PackType.SERVER_DATA)` is **never** driven by client resources. This is the mechanism that keeps client and server definition state from merging in an integrated server. | derived from the two rows above |
| `SimpleReloadListener<T>`: `prepare(SharedState)` runs off-thread, `apply(T, SharedState)` is **guaranteed on the game thread** | `…resource/v1/reloader/SimpleReloadListener` javadoc |
| `PreparableReloadListener.SharedState` exposes `resourceManager()`, `get(StateKey<T>)`, `set(StateKey<T>, T)` | 26.1.2 mapping surface for `net.minecraft.server.packs.resources.PreparableReloadListener$SharedState` |
| `net.minecraft.server.packs.resources.ResourceManagerReloadListener` exists in 26.1.2 with `onResourceManagerReload(ResourceManager)`, `reload(...)`, `prepareSharedState(...)` — the fully synchronous alternative | 26.1.2 mapping surface |
| `ResourceLoader.addListenerOrdering(first, second)` exists; ordering constraints are honoured during the **apply** stage only | `ResourceLoader` javadoc |
| Fabric injects a `SetupMarkerResourceReloader` at **index 0** of the server-data listener list and re-inserts it there after sorting, so `SharedState` keys (`DataResourceLoader.REGISTRY_LOOKUP_KEY`, …) are populated before any other listener runs | `ResourceLoaderImpl#sort` (`if (setupReloader != null) reloaders.add(setupReloader);` first), `SetupMarkerResourceReloader` |
| `ResourceManager.listResources(String startingPath, Predicate<Identifier>)` returns `Map<Identifier, Resource>` — exactly **one** `Resource` per id, pack precedence already resolved | 1.21.8 mapping surface, `listResources`; see §21 Q2 for the 26.1 name re-check |
| `DataResourceLoader` javadoc explicitly advises: *"it is best to primarily use reload listeners as stateless loaders, as storing a state may easily lead to incomplete or leaking data"* | `DataResourceLoader` javadoc |

### 3.4 Lifecycle events **[VF]**

| Fact | Source |
|---|---|
| `ServerLifecycleEvents.SERVER_STARTING` fires in `MinecraftServer.runServer` immediately **before** `MinecraftServer.initServer()`, i.e. before the first `reloadResources` and before any level or player exists | `…lifecycle-events-v1/…/mixin/event/lifecycle/MinecraftServerMixin#beforeSetupServer` |
| `SERVER_STOPPED` fires at `stopServer` TAIL, after all levels closed | same |
| `START_DATA_PACK_RELOAD` / `END_DATA_PACK_RELOAD` fire around `MinecraftServer.reloadResources`; `END_…` receives a success flag and is documented *"If reloading data packs was unsuccessful, the current data packs will be kept"* | same + javadoc |
| `ServerEntityEvents.ENTITY_LOAD` fires from `net.minecraft.server.level.ServerLevel$EntityCallbacks.onTrackingStart(Entity)`, TAIL — i.e. when an entity **begins being tracked by a `ServerLevel`**, not at spawn and not at world-add time | `…lifecycle-events-v1/…/ServerLevelEntityCallbacksMixin` |
| `EntityLoadData.isLoadedFromDisk()` — `default boolean`, *"Returns: true if the entity was loaded from disk"*; `net.minecraft.world.entity.Entity` is listed as an implementing class in the installed javadoc | `…event.lifecycle.v1.EntityLoadData` javadoc |
| The 26.1 branch's `fabric-lifecycle-events-v1.classtweaker` does **not** contain the `transitive-inject-interface Entity … EntityLoadData` line; the 26.2 branch's does. The mechanism by which `Entity` implements `EntityLoadData` in `0.155.3+26.1.2` is therefore **not pinned by branch `26.1`**. | `…/fabric-lifecycle-events-v1.classtweaker` on branches `26.1` and `26.2`; see §21 Q1 |
| `EntitySpawnReason` exists (it appears in `EntityLoadData.spawnReason()`'s signature); its constant names are not verified and are **not needed** by this layer | §3.4 row 5 |

### 3.5 Identifier codecs **[VF]**

| Fact | Source |
|---|---|
| `net.minecraft.resources.Identifier.CODEC` : `Codec<Identifier>` | 26.1.2 mapping surface |
| `net.minecraft.resources.Identifier.STREAM_CODEC` : `StreamCodec<ByteBuf, Identifier>` — `ByteBuf` is a supertype of `RegistryFriendlyByteBuf`, so it satisfies `StreamCodec<? super RegistryFriendlyByteBuf, A>` | same |
| Persistence and synchronisation therefore require **different** codec objects, as the brief states | §3.1 `Builder.persistent` / `Builder.syncWith` signatures |

---

## 4. Architectural decision

**[AD]** The Variant State Layer is four cooperating units, each with exactly one job:

```
 SERVER DATA (mod data + datapacks)
   → VariantDefinitionReloadListener   (SERVER_DATA only; parse in prepare, index in apply)
   → VariantSnapshot                   (immutable: byVariantId + byEntityType)
   → published onto MinecraftServer.globalAttachments()   (single reference swap)
                    │
                    ▼  (server only)
 ServerEntityEvents.ENTITY_LOAD  →  VariantStateLifecycle.onEntityLoad(entity, level)
   guard: isLoadedFromDisk()  /  guard: hasAttached(VARIANT_TEXTURE)
   → snapshot.variantsFor(entity.getType())  →  entity.setAttached(VARIANT_TEXTURE, texture)
                    │
                    ▼  Fabric attachment persistence + sync (no custom packets)
 Entity (server NBT)  ─────────────────────────►  Entity (client, tracked)
 VARIANT_TEXTURE : Identifier  (resolved texture)  ──►  renderer reads it, does nothing else
```

Why this shape:

1. **Server-only definition loading is a property of the API, not of discipline.** Registering the
   listener on `ResourceLoader.get(PackType.SERVER_DATA)` guarantees it is only ever driven by the
   server-data pipeline (§3.3). No `PackType` sniffing, no client entrypoint hook, no risk of a
   client registry.
2. **Snapshot ownership is Fabric's, not ours.** `GlobalAttachments` is an `AttachmentTarget`
   whose lifecycle is bound to the `MinecraftServer` (§3.2). That removes the need for a
   `WeakHashMap`, a `MinecraftServer` accessor mixin, or a static mutable registry — all of which
   are explicitly out of bounds.
3. **Selection is one branch, guarded twice**, and is provably idempotent (§9.3).
4. **The client never resolves anything.** The attachment carries the answer, not the question.
5. **Zero per-tick work** is structural: no tick listener is registered anywhere (§16).

---

## 5. Attachment payload decision

### 5.1 Candidate A — attachment holds the canonical `variantId` **[rejected]**

`variantId → definition → texture` would have to be resolved **on the client**, because that is
the only place a texture is consumed. A normal client is not given the server's `SERVER_DATA`
resource manager: Fabric injects listeners into a reload pipeline only for that pipeline's own
`PackType` (§3.3), and a client only reloads `CLIENT_RESOURCES`. Making Candidate A work would
therefore require:

- a second, client-side definition loader and snapshot — precisely the client-side registry the
  constraints forbid and the renderer plan's "no client registry lookup" invariant forbids;
- a resolution rule for server-only datapack overrides, which a client physically cannot see;
- two independent copies of the definition set that can silently diverge.

Rejected. Its only genuine advantage — that a datapack edit changes the appearance of already-spawned
mobs — is worth less than the correctness and simplicity it costs.

### 5.2 Candidate B — attachment holds the **resolved texture `Identifier`** **[AD, selected]**

```
server:  variantId → VariantDefinition → texture Identifier   (during selection, once per spawn)
client:  attachment → renderer                               (one attachment read)
```

The semantics are explicit and narrow:

> **The attachment does not represent a variant identity. It represents the resolved render state
> for one entity: the texture `Identifier` the server has already chosen for that entity.**

Consequences of that narrowing, stated plainly:

- The attachment's name must not say "variant". `VARIANT_TEXTURE`, not `VARIANT`.
- The entity no longer records *which* variant it is. Nothing in this layer needs that; a future
  variant-specific gameplay feature would (§22 R-005).
- A `/reload` that changes a definition does **not** restyle already-spawned mobs (§14).

### 5.3 Comparison **[AD]**

| Criterion | A (variantId) | B (resolved texture) |
|---|---|---|
| Persistence | stores canonical id | stores the resolved texture id; self-contained, no definition needed to interpret it |
| Synchronisation | works | works; one `Identifier` (~30 B), same as A |
| Dedicated server | needs a client mirror | nothing needed on the client |
| Integrated server | needs the client copy kept in sync with the server copy, in one JVM | nothing client-side exists |
| Datapack overrides | client cannot see them → divergence by construction | server resolves the override and sends the result → correct by construction |
| Client/server divergence | possible and silent | impossible by construction |
| Semantic meaning | "which variant am I" | "what texture was resolved for me" — narrower, and exactly what every consumer needs |
| Renderer requirement | forces a client registry + per-frame lookup | one attachment read, zero registry reads |
| Networking complexity | identical | identical |
| `/reload` | live for existing mobs | existing mobs keep their texture (§14) |
| Saved entities | resolve against possibly-renamed definitions | keep a valid identifier regardless of definition churn |
| Removed / changed definitions | unknown id → must degrade to vanilla | nothing to degrade: the value is self-contained |
| Missing texture asset | degrades via the registry to vanilla | client renders vanilla's missing-texture result; no crash |
| Future extensibility | better (identity available per entity) | worse; remedy is a second attachment **only** when a concrete requirement appears (§22) |
| Compatibility | leaks data shape into a client/server contract | self-describing, no client data dependency |

### 5.4 The one attachment **[AD]**

Exactly one entity attachment is created. No second attachment is introduced; no requirement in
this layer proves one necessary.

---

## 6. Variant definition model

### 6.1 The record **[AD]**

```java
public record VariantDefinition(Identifier entityType, Identifier texture) {
    public static final Codec<VariantDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
        Identifier.CODEC.fieldOf("entity_type").forGetter(VariantDefinition::entityType),
        Identifier.CODEC.fieldOf("texture").forGetter(VariantDefinition::texture)
    ).apply(i, VariantDefinition::new));
}
```

Two fields, both required, both decoded from the file body. There is no derived field on the
record: **a definition is exactly what was authored**, and the variant identity lives in the
snapshot's key, derived from the file path at load time. This keeps the codec a plain
two-field `RecordCodecBuilder` with no post-processing step.

Nothing else is present, and nothing else may be added without a new decision: no health, no
attributes, no AI, no sounds, no drops, no probability, no commands, no GUI data.

### 6.2 File layout and identity **[AD, supersedes part of D-018]**

```
data/mob_variants_brs/variants/<name>.json
```

Example — `data/mob_variants_brs/variants/ice_zombie.json`:

```json
{
  "entity_type": "minecraft:zombie",
  "texture": "mob_variants_brs:entity/zombie/zombie_ice"
}
```

- **Variant identity is path-derived**: `mob_variants_brs:<name>` — the single source of truth,
  cannot be contradicted by the file body. (Keeps D-018's intent.)
- **Applicability is an authored field**, not a path segment. This *changes* D-018, which derived
  the entity type from `variants/<entity>/<name>.json`. Reason: an entity type is a namespaced id
  and a resource-id **path may itself contain `/`**, so `variants/<entity>/<name>.json` cannot be
  split unambiguously; the alternatives are a hard-coded `minecraft` namespace (excludes every
  modded mob) or a colon in a directory name, which is illegal on Windows. A namespaced id in a
  JSON field is the vanilla-conventional and unambiguous form, and it is used verbatim as the map
  key, so no contradiction is possible.
- `<name>` is therefore **global to the mod**: two variants of the same mob type need distinct
  file names (`ice_zombie.json`, `frost_zombie.json`). Documented, not a bug.

### 6.3 Validation rules **[AD]**

| Rule | Where | On failure |
|---|---|---|
| File must parse as JSON and match `CODEC` | `prepare` | log `error` naming the file; **skip that file only**; the rest of the reload proceeds |
| `entity_type` must resolve in `BuiltInRegistries.ENTITY_TYPE` | `apply` (game thread) | log `error`; **skip that definition** |
| `texture` must be a syntactically valid `Identifier` | `prepare` (guaranteed by `Identifier.CODEC`) | same as JSON failure |
| `texture` asset existence | **not checked** | see §18 — the server has no asset index and a missing asset is a client-side, non-fatal condition |
| A variant with **no** texture is rejected, not tolerated | `prepare` | under Candidate B a texture-less definition is observationally identical to no definition; accepting it would be a silent authoring trap |

That last row removes the "empty `{}` body is a valid variant" rule from the previous proposal.
It was only coherent while the attachment held a variant *id*; with a resolved texture as the
payload it has no meaning, so the loader rejects it deterministically instead of silently doing
nothing.

---

## 7. Variant snapshot ownership

### 7.1 Shape **[AD]**

```java
public record VariantSnapshot(
        Map<Identifier, VariantDefinition> byVariantId,
        Map<EntityType<?>, List<VariantDefinition>> byEntityType) {

    public static final VariantSnapshot EMPTY = new VariantSnapshot(Map.of(), Map.of());

    public List<VariantDefinition> variantsFor(EntityType<?> type) { ... }  // never null
}
```

Both maps and every list are unmodifiable (`Map.copyOf` / `List.copyOf`). `variantsFor` returns
an empty list for an unknown type and never allocates.

**`byVariantId` has no consumer in this layer.** Under Candidate B the client never resolves an
identifier, so nothing looks a definition up by id. It is nevertheless kept because it is the
snapshot's identity index, it makes duplicate/ordering detection a single map operation, and it
is the exact structure any future variant-specific feature needs. If it is not used by the first
consumer to land, it should be deleted then — flagged in §21 Q3.

### 7.2 Owner **[AD, selected]**

The published snapshot lives as a **non-persistent, non-synced attachment on the server's
`GlobalAttachments`**:

```java
AttachmentRegistry.create(MobVariantsBRS.id("server_variant_snapshot"))   // no persistent(), no syncWith(), no initializer()
```

- `MinecraftServer implements GlobalAttachmentsProvider`; the target is created in the
  `MinecraftServer` constructor and its lifecycle is bound to the server object (§3.2).
- Nothing about it is JVM-global. It is created with the server and dies with it.
- It is never touched on the client. The client's `GlobalAttachments` is bound to
  `ClientPacketListener` (§3.2) and this layer never writes there.
- Read side needs no stored reference at all: `ServerEntityEvents.ENTITY_LOAD` hands the listener
  a `ServerLevel`, and `Level` also implements `GlobalAttachmentsProvider`, so the snapshot is
  read as `level.globalAttachments().getAttached(SERVER_VARIANT_SNAPSHOT)`.

### 7.3 Reaching the server from the reload listener **[AD]**

`apply` receives only `SharedState`, which exposes no server (§3.4). The listener therefore keeps
**one** nullable reference, written only by Fabric lifecycle events:

```java
// in the reload listener, registered once at mod init
private @Nullable MinecraftServer server;

void attach(MinecraftServer s)  { this.server = s; }   // ServerLifecycleEvents.SERVER_STARTING
void detach()                    { this.server = null; } // ServerLifecycleEvents.SERVER_STOPPED
```

`SERVER_STARTING` is injected immediately before `MinecraftServer.initServer()` (§3.4), so the
reference is always set before the first `reloadResources` and before any entity exists.

- This is a **single nullable reference to a lifecycle-bound object**, not a registry, not a cache,
  not a definition store, and it is never read off the server thread.
- `SERVER_STARTING` also publishes `VariantSnapshot.EMPTY`, so the snapshot is never absent and
  the selection path has no null branch.
- If `server` is somehow null in `apply` (it should be unreachable), the listener logs `error`,
  **keeps the previous snapshot**, and returns. It never throws. Deterministic.

### 7.4 Explicitly rejected ownership models **[AD]**

| Model | Rejected because |
|---|---|
| `WeakHashMap<MinecraftServer, VariantSnapshot>` | Explicitly out of bounds; `GlobalAttachments` is the same lifecycle with none of the failure modes (leaked keys, identity vs `equals`, unsynchronised access) |
| `MinecraftServer` accessor/injector mixin | Explicitly out of bounds, and unnecessary: `MinecraftServer` already implements `GlobalAttachmentsProvider` |
| `public static volatile VariantSnapshot` | JVM-global mutable state that outlives the server and cannot be validated against the server it belongs to |
| Snapshot held as a field on the reload-listener singleton | Fabric explicitly advises reload listeners be stateless (§3.3); the listener would also keep the published state alive across server restarts |
| A `SavedData` in `SavedDataStorage` | Introduces world-load save/load semantics for something that is purely derived from the current datapack set |

---

## 8. Server resource loading

### 8.1 Listener **[AD]**

`net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener<Prepared>`, registered once at
mod init through `ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id, listener)`.

- **`prepare` (off-thread, may run on any thread)**
  1. `state.resourceManager().listResources("mob_variants_brs/variants", id -> id.getPath().endsWith(".json"))`
  2. For each entry, in **ascending `Identifier` order**, open the resource and decode
     `VariantDefinition.CODEC` from `JsonOps.INSTANCE`.
     `JsonOps` rather than `RegistryOps`: `Identifier.CODEC` is a plain string codec and no
     registry-bound field exists, so this removes any dependency on a `SharedState` key and on
     listener ordering. `RegistryOps.create(state.get(DataResourceLoader.REGISTRY_LOOKUP_KEY))`
     becomes necessary only when a registry-bound field is added — recorded as a future seam,
     not used now.
  3. Any per-file failure is caught, logged with the file id, and that file is skipped.
     `prepare` never throws for bad content.
  4. Returns `List<Map.Entry<Identifier, VariantDefinition>>` — the id is taken from the file
     path, the value from the body.

- **`apply` (game thread, guaranteed)** — the only place that publishes
  1. Resolve each `entity_type` against `BuiltInRegistries.ENTITY_TYPE`; unknown → `error` + skip.
  2. Build `byVariantId` and `byEntityType`, both in ascending `Identifier` order so the
     candidate list order is **deterministic across reloads** and across restarts.
  3. Wrap in `Map.copyOf` / `List.copyOf`.
  4. `server.globalAttachments().setAttached(SERVER_VARIANT_SNAPSHOT, snapshot)` — one call, one
     reference swap, no partially-built state is ever observable.

### 8.2 Requirements met **[AD]**

| Requirement | How |
|---|---|
| mod-bundled definitions | `src/main/resources/data/mob_variants_brs/variants/**` is part of the mod's built-in data packs |
| datapack definitions | world datapacks are in the same `SERVER_DATA` resource manager |
| normal reload semantics | it is an ordinary `SERVER_DATA` reload listener; it runs on initial world load and on `/reload` |
| `/reload` | same path; see §14 |
| complete before publication | `apply` finishes both indexes before the single `setAttached` |
| no partial state | publication is one reference assignment; readers only ever see a complete snapshot |
| duplicate / override semantics | **there is none to define**: `listResources` yields exactly one `Resource` per id after vanilla has already resolved pack precedence, so a higher datapack's file at the same path **replaces** the lower one wholesale. No field-level merge, no cross-file merge. |
| no repeated parsing outside reload | parsing happens only inside `prepare` |
| no client loader | listener registered on `SERVER_DATA` only (§3.3) |

### 8.3 Ordering **[AD]**

No `addListenerOrdering` call is needed. This listener consumes raw JSON and registries and
produces a snapshot nothing else in the mod depends on during reload. Ordering is only required
if a future field needs `DataResourceLoader.RECIPE_MANAGER_KEY` / `ADVANCEMENT_LOADER_KEY` /
`REGISTRY_LOOKUP_KEY`, at which point `addListenerOrdering(id, ResourceReloaderKeys.Server.…)`
becomes necessary. Recording it now would be speculative.

---

## 9. Variant selection lifecycle

### 9.1 Hook **[AD]**

`ServerEntityEvents.ENTITY_LOAD`, registered once at mod init. This is a Fabric API event;
**no server mixin is introduced**, satisfying D-015.

Verified caveat, stated because it matters: the event is injected at the TAIL of
`ServerLevel$EntityCallbacks.onTrackingStart` (§3.4). It therefore fires when an entity begins
being tracked by a `ServerLevel`, not at the instant of construction. For a freshly spawned mob
those are the same tick; for a mob restored from disk it is after NBT has been read. Both are
acceptable for this design, and §9.3 explains why the second one is handled correctly.

### 9.2 The callback **[AD]**

```java
private static void onEntityLoad(Entity entity, ServerLevel level) {
    if (entity.isLoadedFromDisk()) return;                       // guard 1
    if (entity.hasAttached(VariantAttachments.VARIANT_TEXTURE)) return; // guard 2

    VariantSnapshot snapshot =
        level.globalAttachments().getAttachedOrElse(
            VariantAttachments.SERVER_VARIANT_SNAPSHOT, VariantSnapshot.EMPTY);

    List<VariantDefinition> candidates = snapshot.variantsFor(entity.getType());
    if (candidates.isEmpty()) return;

    entity.setAttached(VariantAttachments.VARIANT_TEXTURE, candidates.getFirst().texture());
}
```

- **No `instanceof` anywhere.** Applicability is keyed by `EntityType<?>`; players, items and
  other non-`EntityType` entities simply miss the map, so no type filter is needed and the layer
  stays independent of the render layer's `LivingEntity` choice.
- **No biome query, no environment read, no `spawnReason()`, no UUID hash.** Selection reads one
  `EntityType` field and one map.
- **Candidate rule for v1:** the first entry in ascending variant-id order. Stated plainly: with
  this rule, every mob of a given type receives the same texture. That is a deliberate MVP
  placeholder, and §22 records the one-line seam that replaces it.

### 9.3 Guard conditions — why "select once" holds **[AD]**

| Requirement | Guarantee |
|---|---|
| entities loaded from disk keep their variant | The attachment is **persistent** (§10), so a disk-restored entity already has it and guard 2 returns. |
| no reselection on ordinary chunk load | A chunk reload re-reads the entity from NBT; guard 2 returns. |
| no reselection on dimension travel | Vanilla recreates the entity across dimensions and Fabric copies attachments wholesale by default (§3.1); guard 2 returns. |
| no reselection on server restart | Same as chunk load; the attachment is in the save. |
| no reselection if `ENTITY_LOAD` re-fires on a live instance | Guard 2 returns. |
| no reselection if `isLoadedFromDisk()` is unavailable or unreliable | **Guard 2 alone is sufficient**, because persistence guarantees the attachment is present on every disk-restored entity. Guard 1 is an optimisation and defence in depth, not the load-bearing guard. |

That last row is the important one: the "select exactly once" invariant does **not** depend on the
unverified `isLoadedFromDisk()` mechanism (§21 Q1). If `EntityLoadData` turns out to be
unreachable, delete guard 1 and nothing else changes.

### 9.4 No per-tick processing **[AD]**

No `ServerTickEvents`, no `EntityTickEvents`, no chunk scan, no entity iteration. Fabric's
`ServerEntityEvents.EQUIPMENT_CHANGE` is **not** subscribed (it fires inside `LivingEntity.tick`).
The zero-per-tick property is structural: no tick hook is registered anywhere in this layer.

---

## 10. Persistence

### 10.1 Mechanism **[AD]**

`Builder.persistent(Identifier.CODEC)`. Fabric injects the read at
`Entity.readAdditionalSaveData(ValueInput)` and the write at
`Entity.addAdditionalSaveData(ValueOutput)`, storing the data under
`AttachmentTarget.NBT_ATTACHMENT_KEY` (§3.1). No NBT mixin of ours, no `SavedData`.

### 10.2 Situation analysis **[AD]**

| Situation | Behaviour |
|---|---|
| world save / load | Fabric writes and reads the value in entity NBT (§3.1). Automatic. |
| server restart | Attachment survives; guard 2 prevents re-selection. |
| entity loaded from disk | Attachment already present; guard 2 prevents re-selection. |
| existing attachment state on a live entity | Never overwritten. The only writer is the `ENTITY_LOAD` callback, and guard 2 makes it idempotent. |
| definition **changed** in a datapack | Existing mobs keep their persisted texture until they despawn. Deliberate — see §14. |
| definition **removed** | No effect on existing mobs. The stored value is a texture id, not a definition reference; there is nothing to dangle. |
| definition was never present on this server (e.g. the entity came from another world) | Attachment simply absent → vanilla texture. |
| stored value no longer corresponds to any definition | Same: the value is self-contained and still valid. |
| stored texture asset missing | The client resolves the id against its own resources; vanilla's missing-texture result is shown. No crash, no server involvement. |
| malformed stored NBT | Fabric's deserialisation handles the codec; a value that fails to decode is not populated, and guard 2 then allows one fresh selection. Self-healing, deterministic. |

### 10.3 No `initializer()` **[AD, D-022 retained and now verified]**

`dataAttachments` is a lazily created `@Nullable IdentityHashMap` (§3.1), so an entity with no
attachment allocates nothing at all. This resolves `docs/ROADMAP.md` P-9 in the favourable
direction, without the mod doing anything.

### 10.4 `copyOnDeath()` deliberately unset **[AD, D-023 retained]**

A variant does not survive zombie→drowned conversion in v1. One builder call reverses this.

---

## 11. Synchronisation

### 11.1 Mechanism **[AD]**

```java
AttachmentRegistry.create(MobVariantsBRS.id("variant_texture"), b -> b
        .persistent(Identifier.CODEC)                                  // Codec<Identifier>
        .syncWith(Identifier.STREAM_CODEC, AttachmentSyncPredicate.all()))  // StreamCodec<ByteBuf, Identifier>
```

`AttachmentSyncPredicate.all()` is the correct predicate: the target is an entity, not a player,
so `targetOnly()` would sync to nobody and `allButTarget()` would behave identically to `all()`
while implying a distinction that does not exist here (§3.1).

`Identifier.STREAM_CODEC` is `StreamCodec<ByteBuf, Identifier>`; `ByteBuf` is a supertype of
`RegistryFriendlyByteBuf`, so it satisfies the required
`StreamCodec<? super RegistryFriendlyByteBuf, A>` (§3.5).

**No custom networking.** No payload type, no receiver, no packet. D-012 holds.

### 11.2 Registration requirements **[AD]**

The `AttachmentType` is declared in **`src/main`**, not `src/client`. Both logical sides therefore
register the same identifier during mod init, which is what the configuration-phase handshake in
`AttachmentSync` requires (§3.1). Declaring it in `main` also means the client classpath can
resolve the field without touching any client-only class.

### 11.3 Delivery paths **[AD]**

| Trigger | Mechanism (verified) |
|---|---|
| attachment set while players already track the entity | `setAttached` → `fabric_syncChange` → `PlayerLookup.tracking(entity)` filtered by predicate (§3.1) |
| a player starts tracking the entity (chunk load, late join, reconnect, dimension travel) | `EntityTrackingEvents.START_TRACKING` → `fabric_computeInitialSyncChanges(player, …)` replays the full synced state (§3.1) |
| the entity was restored from NBT | `fabric_readAttachmentsFromNbt` populates the synced state, so `START_TRACKING` sends it (§3.1) |

All three paths are Fabric's. Because selection happens at `onTrackingStart` and players may or
may not already be tracking at that instant, the design deliberately relies on the *union* of the
immediate and the initial-sync paths rather than on either one alone.

### 11.4 Payload cost **[AD]**

One namespaced `Identifier` per entity, written only once (at spawn) and only on change. Nothing
per frame, nothing per tick, nothing per chunk.

---

## 12. Multiplayer behaviour

The server is authoritative end to end. Only the server runs the definition loader, only the
server performs selection, only the server writes the attachment. The client writes nothing and
resolves nothing.

| Scenario | Behaviour **[AD]** |
|---|---|
| multiple players observing one entity | `AttachmentSyncPredicate.all()`; every tracking player receives the same value. Deterministic and identical by construction. |
| late-joining player | `START_TRACKING` sends the entity's full attachment state when the player begins tracking it (§3.1). |
| reconnect | identical to late join. |
| chunk unload / reload | the entity re-reads its persisted attachment; `START_TRACKING` re-sends it to whoever begins tracking it. |
| dimension travel | Fabric copies attachments wholesale on cross-level teleport (§3.1); the new instance re-syncs via `START_TRACKING`. |
| server restart | the attachment is in the save; same as chunk reload. |
| dedicated server, client without the mod | Fabric logs *"Client does not support the syncable attachments …"* during the configuration handshake and skips the sync (§3.1). That client shows the vanilla texture. No crash, no disconnect. |
| client that has the mod | full fidelity from first frame the entity is tracked. |

No client may independently determine authoritative variant state, and under Candidate B there is
no client-side data from which it could try.

---

## 13. Integrated-server behaviour

Client and integrated server share one JVM. The specific hazards and how each is closed:

| Hazard | Closure **[VF] + [AD]** |
|---|---|
| the same definition loader being driven by both resource managers | impossible: the listener is registered on `ResourceLoader.get(PackType.SERVER_DATA)` and Fabric injects listeners only into the matching `PackType` pipeline (§3.3) |
| client and server definitions merging in one static | impossible: no static definition state exists; the only snapshot is an attachment on the server's `GlobalAttachments` (§7.2) |
| the client writing authoritative state | impossible: no client code path writes the attachment; the renderer only reads it |
| common/server code depending on client-only classes | the whole layer lives in `src/main` and imports no `net.minecraft.client.*`. `VariantSnapshot` is *present* on the client classpath but never instantiated there. |
| the client needing the mod's `data/` pack | it does not; no client loader exists |
| `MinecraftServer.globalAttachments()` being touched client-side | it is not; the client's `GlobalAttachments` is bound to `ClientPacketListener` and this layer never writes there (§3.2) |
| a JVM-wide mutable field leaking across logical sides | the only mutable fields are the attachment-type singletons (immutable) and the loader's `@Nullable MinecraftServer` reference, written solely by server lifecycle events (§7.3) |

`Main source set remains dedicated-server safe` because nothing in it references a client class;
the split source sets (`build.gradle:14-23`) enforce this at compile time.

---

## 14. `/reload` behaviour

### 14.1 Sequence **[AD]**

1. `/reload` → `MinecraftServer.reloadResources` → `START_DATA_PACK_RELOAD` fires (§3.4).
2. `prepare` re-reads every `mob_variants_brs/variants/*.json` from the rebuilt `SERVER_DATA`
   resource manager and decodes each into a `VariantDefinition`.
3. `apply` builds `byVariantId` and `byEntityType` in full, wraps them unmodifiable, and calls
   `setAttached(SERVER_VARIANT_SNAPSHOT, snapshot)` — **one atomic reference swap**. A reader can
   only ever observe the previous complete snapshot or the new complete snapshot.
4. `END_DATA_PACK_RELOAD` fires; if the reload failed, vanilla keeps the old datapacks and this
   layer's `apply` did not publish, so the old snapshot stays live.

### 14.2 What happens to what **[AD]**

| Thing | Result |
|---|---|
| **existing entities** | Unchanged. Their attachments are never rewritten outside the guarded `ENTITY_LOAD` callback. |
| **newly introduced entities** | Use the new snapshot from their first `ENTITY_LOAD`. |
| **persisted entities** | Unchanged; they keep the persisted texture. |
| **removed definitions** | No effect on existing entities (their value is a texture id, not a reference). New entities of that type get no variant. |
| **changed definitions** | Take effect for new entities only. Existing entities keep the old texture until they despawn. |

### 14.3 Why existing entities are not re-resolved **[AD]**

This is stated explicitly rather than left implicit, per the brief. Re-resolution is not merely
omitted for convenience; it is **incompatible with the chosen payload**:

- the attachment holds a texture, not a variant identity, so there is nothing to re-resolve *from*;
- adding identity back purely to enable re-resolution would re-introduce exactly the state the
  payload decision removed, and would need a re-resolve pass over every loaded entity on every
  reload — per-entity work on the reload path, for a visual refresh that is not required;
- the alternative behaviour (live restyling) is a datapack-authoring nicety, not a correctness
  property, and it would make an entity's appearance depend on when it was loaded.

The reversal path, when a real requirement for live restyling appears, is: add a variant-identity
attachment (§22 R-005) and add a reload-time re-resolve pass over loaded entities. Both are
additive and neither changes this layer's other decisions.

---

## 15. Renderer contract

### 15.1 The contract **[AD]**

The state layer guarantees the renderer exactly one operation:

```java
@Nullable Identifier texture = entity.getAttached(VariantAttachments.VARIANT_TEXTURE);
```

- `null` → the entity has no variant; the renderer must fall through to vanilla.
- non-null → the renderer must use this `Identifier` as the body texture.
- **No registry access. No definition access. No resource access. No lookup of any kind.**

This *satisfies* the renderer plan rather than conflicting with it: §5.1 #9 of
`1790968433924` already requires "no attachment lookup during submission" and §12 requires "zero
datapack reads during submission". Candidate B is what makes both achievable.

### 15.2 The concrete incompatibility found, and its minimal amendment **[AD]**

`1790968433924-mob-variant-texture-rendering-plan.md` §1 states the state-layer contract as:

```
Identifier variantId = entity.getAttached(VariantAttachments.VARIANT);   // AttachmentType<Identifier>
Identifier texture   = VariantRegistry.get(variantId).texture();          // precomputed at reload
```

That is Candidate A. Under Candidate B a client-side `VariantRegistry` must not exist, so that
second line has no valid target on the client. This is a concrete incompatibility between the two
plans, and it is resolved by a **contract-only** amendment to the renderer plan — no change to its
verified interception point (§2), its Mixin mechanism (§4), its render-state transport (§5.1 #1),
its behaviour matrix, or its validation list:

| Location in `1790968433924` | Change |
|---|---|
| §1 dependency block | replaced by the one-line contract in §15.1 |
| §5.1 #3 | "resolved in the state layer at reload" → "resolved **on the server, once per spawn**; the client receives the resolved value through the attachment" |
| §5.1 #9 | unchanged in substance; the `null` case is now the *only* no-variant case |
| §5.1 #10 | reworded: an absent attachment resolves to `null` |
| §5.2 table | the `VariantRegistry.get(id)` row is deleted; the remaining cost is one attachment read |
| §6 file list | "the two-lookup resolve helper" → a single attachment read |
| §7 task 5 | rewired to the §15.1 contract; "do not re-read datapacks" retained |
| §8 matrix | row 2 (texture absent/unresolvable) is unreachable under Candidate B and is replaced by the plain "no attachment" row |
| §14 | D-011 → *amended*; R-005/R-007 → *retained but re-based* |

**Status of these amendments.** The edits above are specified *by this document* and are landed in
the renderer plan `1790968433924-mob-variant-texture-rendering-plan.md`, which remains the
authoritative statement of the render layer. They are **plan-file edits only**: no Java, resource,
mixin config, Gradle, dependency, test or `docs/**` file was changed by any part of that work. Do not
trust this paragraph as evidence — verify the applied state by reading the named sections of
`1790968433924`.

Untouched, because they do not depend on the payload contract: §2, §4, §5.1 #1, §9, §10, §13, §15.
Rewired alongside the locations listed above because they still carried the old wording: §5 (the
pipeline diagram) and §12 (the performance invariants, which asserted a registry read). §16 gained a
single line recording the amendment itself.

### 15.3 What the renderer plan does **not** need from this layer **[AD]**

No client-side variant registry, no client snapshot, no client definition loader, no client
resource access, and no per-frame or per-entity registry lookup. The render layer therefore keeps
all of its verified properties: no server mixin, no custom networking, no per-frame allocation,
and exactly one attachment read per rendered living entity at `extractRenderState` HEAD.

---

## 16. Performance considerations

### 16.1 Invariants **[AD]**

| Invariant | How it is achieved |
|---|---|
| zero per-tick variant processing | no tick listener is registered anywhere (§9.4) |
| no per-tick biome or environment checks | no biome/environment read exists in the layer |
| no repeated datapack parsing | parsing happens only inside `prepare` (§8.1) |
| no resource lookup during rendering | the renderer performs no resource access (§15.1) |
| no client registry lookup during rendering | there is no client registry (§5.2) |
| immutable published definition state | `record` + `Map.copyOf` / `List.copyOf` (§7.1) |
| O(1) variant lookup | `byEntityType` is a `HashMap` keyed by `EntityType<?>`; `variantsFor` is one `get` |
| minimal synchronisation payload | one `Identifier` per entity, sent once (§11.4) |
| no allocation in the renderer path | the value is either `null` or an already-interned `Identifier` shared with the snapshot |
| no allocation for unvarianted entities | lazily created attachment map, and no `initializer()` (§10.3) |

### 16.2 Per-spawn cost, server **[AD]**

```
ENTITY_LOAD
  → entity.isLoadedFromDisk()               boolean
  → entity.hasAttached(VARIANT_TEXTURE)     one map probe, no allocation
  → level.globalAttachments().getAttachedOrElse(SNAPSHOT, EMPTY)   one map probe
  → snapshot.variantsFor(entity.getType())  one map get
  → candidates.getFirst().texture()         one list get
  → entity.setAttached(VARIANT_TEXTURE, …)  one map insert + sync fan-out
```

On the common "no definition for this type" path the callback costs two map probes and one map
get, allocates nothing, and writes nothing.

### 16.3 Explicitly not done **[AD]**

No speculative caching, no pre-warmed per-`EntityType` arrays, no interning layer, no
double-buffered snapshot beyond the single atomic swap (which already gives free
snapshot-consistency), no background re-resolution.

---

## 17. Compatibility considerations

| Concern | Result **[AD]** |
|---|---|
| dedicated server | loads `main` only; no client class referenced; nothing to break |
| integrated server | §13 |
| multiplayer | §12 |
| entity tracking | covered by `START_TRACKING` + immediate sync (§11.3) |
| chunk unload / reload | attachment persisted; guard 2 prevents re-selection (§9.3) |
| dimension travel | attachments copied wholesale; guard 2 prevents re-selection (§9.3) |
| player join / rejoin | initial sync on `START_TRACKING` (§12) |
| server restart | attachment persisted (§10.2) |
| `/reload` | §14 |
| datapack overrides | resolved server-side; the client never sees the difference (§5.3) |
| external mods | the mod adds one attachment type under its own namespace, one reload listener under its own namespace, one event listener. No vanilla registry entry, no mixin, no packet type. A client without the mod logs Fabric's existing "client does not support the syncable attachments" warning and renders vanilla. |
| unknown / missing definitions | §18 |
| missing texture resources | §18 |
| client/server class separation | everything in `main`; the client mixin config is untouched (§2.4) |
| existing template mixins | this layer neither relies on nor changes them; deleting them remains the separate scaffold-cleanup item already recorded at `1790903055742` §0.1 |

---

## 18. Failure / fallback behaviour

Rows marked **[API]** are guaranteed by the underlying Fabric/vanilla mechanism. Rows marked
**[AD]** are decisions this document makes. No behaviour is invented beyond these.

| Situation | Result | Kind |
|---|---|---|
| malformed JSON / codec failure in one file | `error` naming the file; that file skipped; every other file loads; reload completes | **[AD]** |
| `entity_type` not in `BuiltInRegistries.ENTITY_TYPE` | `error`; definition skipped; mobs of that "type" simply have no variant | **[AD]** |
| duplicate definition ids | **unreachable by construction**: `listResources` returns one `Resource` per id, and ids are unique file paths. A defensive duplicate branch exists in the Code phase only as an assertion, not as merge logic. | **[VF]** + **[AD]** |
| texture asset missing | server has no asset index and does not check; the client resolves the id and shows vanilla's missing-texture result. No crash, no log spam, no server involvement. | **[API]** + **[AD]** |
| unknown persisted state (value fails to decode) | Fabric does not populate the attachment; guard 2 then permits exactly one fresh selection. Self-healing. | **[API]** + **[AD]** |
| removed variant definition, entity already varianted | no effect; the stored value is a self-contained texture id | **[AD]** |
| empty definition set (valid, no files) | publish `VariantSnapshot.EMPTY`; every mob is vanilla; no error, no warning | **[AD]** |
| resource reload fails (datapack error elsewhere in the pipeline) | vanilla keeps the old datapacks; this layer's `apply` never ran; the old snapshot stays live | **[API]** |
| `apply` throws unexpectedly | caught, logged as `error`, previous snapshot retained; never propagated into the reload future | **[AD]** |
| `server` reference unexpectedly null in `apply` | logged as `error`; previous snapshot retained; never thrown | **[AD]** |
| client lacks the attachment type | Fabric's handshake skips the sync and logs; vanilla texture on that client | **[API]** |
| renderer mixin fails to apply on a future MC version | `require = 0` degrades to vanilla textures; the state layer is unaffected | **[AD]** (from `1790968433924` D-017) |
| an entity has no attachment | `getAttached` returns `null`; renderer falls through to vanilla | **[API]** |

There is **no** fallback that invents a texture, substitutes a default variant, or silently
re-selects.

---

## 19. Files and classes for the later Code phase

Nothing below exists today. Nothing below is created by this session.

### 19.1 To create — `src/main` (new package `…mobvariants.variant` + `…mobvariants.attachment`)

| File | Responsibility |
|---|---|
| `src/main/java/com/baruc/brs/mobvariants/attachment/VariantAttachments.java` | the only attachment declarations: `AttachmentType<Identifier> VARIANT_TEXTURE` (persistent + synced, no initializer) and `AttachmentType<VariantSnapshot> SERVER_VARIANT_SNAPSHOT` (neither). Must be in `main` so both logical sides register the same identifiers (§11.2). |
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantDefinition.java` | the immutable record + `CODEC` (§6.1). |
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantSnapshot.java` | the immutable record, `EMPTY`, `variantsFor(EntityType<?>)`, and the `build` factory that validates and orders (§7.1, §8.1 step 2). |
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantDefinitionReloadListener.java` | `SimpleReloadListener`; `prepare` + `apply`; `attach` / `detach` for the server reference; the single publication call (§8.1, §7.3). |
| `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java` | the server lifecycle wiring: `SERVER_STARTING` (capture server, publish `EMPTY`), `SERVER_STOPPED` (detach), and the `ENTITY_LOAD` callback with both guards (§9.2, §7.3). |

### 19.2 To modify

| File | Change |
|---|---|
| `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` | `onInitialize()` registers the `SERVER_DATA` reload listener and the three lifecycle listeners. No other body changes; `MOD_ID`, `LOGGER`, `id(String)` are reused as-is. |

### 19.3 To create — resources

| File | Purpose |
|---|---|
| `src/main/resources/data/mob_variants_brs/variants/ice_zombie.json` | one sample definition proving the schema in §6.2. |

### 19.4 Explicitly **not** touched by this layer

- `src/client/**` — the render layer's deliverable, per `1790968433924`.
- `src/main/resources/mob_variants_brs.mixins.json` and `src/client/resources/mob_variants_brs.client.mixins.json` — **zero mixins are added**.
- `src/main/resources/fabric.mod.json` — no new entrypoints, no new dependencies.
- `build.gradle`, `gradle.properties`, `settings.gradle` — **no dependency is added**; MixinExtras is not used; no `mappings` line (D-021).
- `docs/**` — see §24.

---

## 20. Validation criteria

### 20.1 Compile level (the primary gate) **[AD]**

`./gradlew build` compiles both source sets and is the proof of every API name used here. It must
confirm, in the code that uses them: `Identifier.CODEC`, `Identifier.STREAM_CODEC`,
`Entity.isLoadedFromDisk()` (or its absence → drop guard 1, §9.3), `ResourceManager.listResources`,
`MinecraftServer.globalAttachments()` / `ServerLevel.globalAttachments()`,
`AttachmentRegistry.create(Identifier, Consumer)`, `AttachmentSyncPredicate.all()`,
`ServerEntityEvents.ENTITY_LOAD`, `ServerLifecycleEvents.SERVER_STARTING` / `SERVER_STOPPED`.
If `EntityLoadData` proves unreachable, the design still holds — guard 1 is deleted, guard 2 alone
guarantees the invariant (§9.3).

### 20.2 Runtime level **[AD]**

1. `./gradlew runServer`, spawn a zombie: `/data get entity @s` (or equivalent) shows the
   attachment under `AttachmentTarget.NBT_ATTACHMENT_KEY`.
2. Spawn it, unload the chunk (`/forceload remove` or move away far enough), reload: the attachment
   value is **byte-identical**. Proves guard 2.
3. Kill the server, restart, load the same chunk: the value is unchanged. Proves persistence.
4. `/reload` after editing `ice_zombie.json`'s texture: existing mobs unchanged, **newly** spawned
   mobs use the new texture. Proves §14.
5. Delete `ice_zombie.json`, `/reload`: no error, no crash, no new variants; existing mobs
   unchanged. Proves the removed-definition row in §18.
6. Malformed JSON in one file, `/reload`: one `error` line naming the file, every other file still
   loads. Proves §18.
7. `entity_type: "minecraft:not_a_mob"`, `/reload`: one `error`, definition skipped. Proves §18.
8. Remove all definitions, `/reload`: no error; every mob vanilla. Proves the empty-set row.
9. `./gradlew runClient` against that server: a varianted mob renders with the variant texture;
   two mob variants of the same type do **not** bleed into each other.
10. Two clients in one session: both render the same mob identically. Proves §12.
11. Join late, then reconnect: the mob's texture is correct on first sight. Proves §12.
12. Move the mob through a Nether portal: texture unchanged. Proves §13/§17.
13. `./gradlew runServer` with a client that does **not** have the mod: server starts, Fabric logs
    the "client does not support the syncable attachments" warning, no crash. Proves §12.
14. Negative performance check: with no profiler, confirm no listener of ours is invoked during
    steady-state ticking (temporary counter removed afterwards, or a profiler run). Proves §16.

### 20.3 Explicitly not validated here

No build was run, no class was compiled, no game was launched, and no profiler was used in this
session. Every row above is owed by the Code phase. Nothing in this document claims otherwise.

---

## 21. Known risks and open questions

| # | Item | Kind | Impact | Fallback / owner |
|---|---|---|---|---|
| Q1 | The mechanism by which `Entity` implements `EntityLoadData` in `0.155.3+26.1.2` is not pinned by branch `26.1` (the `transitive-inject-interface` classtweaker line appears on branch `26.2`, not `26.1`), and the exact vanilla call site that sets `isLoadedFromDisk` is unread. | **[AS]** | Low | Guard 2 alone guarantees "select once" (§9.3). If `isLoadedFromDisk()` does not compile, delete guard 1. No other change. Verified at the first compile. |
| Q2 | `ResourceManager.listResources(String, Predicate<Identifier>)` is confirmed on the 1.21.8 mapping surface; the 26.1.2 spelling has not been re-read. | **[AS]** | Low | The equivalent multi-pack accessor in 26.x has the same shape. Compiler resolves it. |
| Q3 | `VariantSnapshot.byVariantId` has no consumer in this layer. | **[AD]** | Cosmetic | Delete it in the Code phase if the first consumer does not need it. Keeping it is a deliberate one-step-ahead, not a framework. |
| Q4 | Whether `ENTITY_LOAD` can fire more than once for a single live entity instance without an NBT round-trip. | **[OQ]** | None | Guard 2 makes the answer irrelevant (§9.3). No action required. |
| Q5 | Every mob of a given type gets the same texture. | **[AD]** | Cosmetic, stated | §22 R-004. |
| Q6 | `/reload` does not restyle existing mobs. | **[AD]** | Accepted trade-off | §14.3; reversal is additive. |
| Q7 | `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md` and `docs/TESTING.md` still describe the superseded variant-id attachment and the old `VariantRegistry` render lookup. | **[AD]** | Documentation only | Updating `docs/` is a documentation task, not part of this Code phase (plan mode does not edit non-plan documentation). Listed in §24. |
| Q8 | The two inert template mixins remain registered with `defaultRequire: 1`. | **[VF]** | Pre-existing | Separate scaffold-cleanup item already recorded at `1790903055742` §0.1. Not touched here. |

---

## 22. Explicitly rejected alternatives

> **Identifier scope.** The `R-0xx` identifiers in the table below, and the `Q-0xx` identifiers in
> §21, are **local to this plan**. They are numbered independently of
> `docs/DECISIONS.md`, which has its own `D-0xx` / `R-0xx` ledger. In particular, this plan's
> **R-004** (a `VariantSelector` interface plus placeholder) and **R-005** (a second attachment
> holding variant identity) are **not** the same items as `docs/DECISIONS.md` **R-004** ("encode the
> variant texture in a vanilla-synced place") and **R-005** ("cache the resolved texture `Identifier`
> in a second attachment"). Same numbers, different decisions. Always qualify the source when citing
> either ledger; `docs/DECISIONS.md` is not modified by this plan and its numbering is left as is.

| # | Alternative | Rejected because |
|---|---|---|
| R-001 | **Candidate A** — attachment holds the canonical `variantId`, client resolves it. | Requires a client-side definition loader and registry, which the constraints forbid and which cannot see server-only datapack overrides. Full analysis in §5.1, §5.3. |
| R-002 | **A client-side variant registry/definition loader.** | Directly forbidden, and redundant under Candidate B: the client has nothing to resolve. |
| R-003 | **`WeakHashMap<MinecraftServer, …>` for snapshot ownership.** | Explicitly out of bounds; `GlobalAttachments` gives the same lifecycle without leaked keys or unsynchronised access (§7.4). |
| R-004 | **A `VariantSelector` interface plus a `FirstDefinitionSelector` placeholder implementation** (the previous proposal's D-019). | The MVP has exactly one rule — first candidate in id order — and probability/UUID/biome selection is explicitly out of scope. An interface plus a placeholder that exists only to be replaced is speculative structure. The rule lives as one readable statement in `VariantStateLifecycle` (§9.2). Reversal is one interface + one call site, if a real selector ever appears. |
| R-005 | **A second attachment holding the variant identity alongside the texture.** | No concrete requirement in this layer proves it necessary. The brief's restriction holds. It becomes justified the moment variant-specific *gameplay* (attributes, sounds, drops) needs the identity rather than the texture — at which point it is additive and changes nothing else (§14.3). |
| R-006 | **Custom networking packets.** | Fabric's attachment sync already covers immediate change, `START_TRACKING`, join, rejoin, level change and chunk load, with a configuration-phase capability handshake (§3.1). D-012 holds. |
| R-007 | **Any mixin.** | Not needed and not wanted: the loader, the selection hook, the guards, persistence and sync are all Fabric API surfaces (§9.1, §10.1, §11.1). Zero mixins are added and no mixin config changes. |
| R-008 | **Custom `EntityType`, entity class, renderer, or model.** | Unchanged rejection from `1790903055742` §6.3. |
| R-009 | **A tick-based variant behaviour system.** | Unchanged rejection from D-013/R-009. No tick listener is registered. |
| R-010 | **Re-resolving persisted state on `/reload`.** | Incompatible with the chosen payload and not required for correctness; it would also add per-entity work to the reload path. Reasoning and reversal path in §14.3. |
| R-011 | **Tolerating a definition with no `texture`.** | Observationally identical to no definition; accepting it is a silent authoring trap. Rejected deterministically instead (§6.3). |
| R-012 | **`addListenerOrdering` against a vanilla reloader.** | This listener consumes raw JSON and registries and publishes a snapshot nothing else needs during reload. Nothing to order against yet (§8.3). |
| R-013 | **`RegistryOps` for decoding.** | `Identifier.CODEC` is a plain string codec; using `JsonOps.INSTANCE` removes a `SharedState` dependency and an ordering assumption for no loss. Becomes necessary only when a registry-bound field is added (§8.1). |
| R-014 | **Putting the whole state layer into `1790903055742`.** | That document already covers the render layer, which is now owned and verified by `1790968433924`. Folding this in would duplicate the renderer plan's scope and re-open its verified findings. A superseded-by banner on `1790903055742`, pointing at this document for the state layer and at `1790968433924` for the render layer, is cleaner than a rewrite; its body is left as historical material. |
| R-015 | **A texture existence check at load.** | The server holds no asset index for client resources; adding one would mean a client-side resource probe, i.e. exactly the client definition coupling this design removes. Vanilla's missing-texture result is the deterministic fallback (§18). |

---

## 23. Decision summary

| Question | Answer |
|---|---|
| Attachment payload | **Candidate B** — the resolved texture `Identifier`. The attachment is *resolved render state*, not variant identity. |
| Second attachment | none |
| Variant definition | immutable two-field record (`entity_type`, `texture`), both required; identity path-derived |
| Snapshot owner | `GlobalAttachments` on the `MinecraftServer`, via a non-persistent, non-synced `AttachmentType` |
| Server reference | one `@Nullable MinecraftServer` on the reload listener, set in `SERVER_STARTING`, cleared in `SERVER_STOPPED` |
| Definition source | `ResourceLoader.get(PackType.SERVER_DATA)` only; never the client |
| Selection hook | `ServerEntityEvents.ENTITY_LOAD`, guarded by `isLoadedFromDisk()` **and** `hasAttached(VARIANT_TEXTURE)` |
| Persistence | `Identifier.CODEC`; no re-resolution on reload |
| Synchronisation | `Identifier.STREAM_CODEC` + `AttachmentSyncPredicate.all()`; no custom networking |
| Renderer contract | one attachment read; no registry; no client data |
| Per-tick work | zero |
| Mixins added | zero |

---

## 24. Recommended next phase

1. **Code phase — state layer.** Implement §19.1 and §19.2 and the sample definition in §19.3,
   then run §20.1 and §20.2. Do not touch `src/client/**`.
2. **Code phase — render layer.** Implement `1790968433924` against the amended contract in §15.
3. **Scaffold cleanup.** Delete the two inert template mixins and empty the common mixin config —
   the already-recorded `1790903055742` §0.1 item. Best done after the state layer, so the
   `defaultRequire: 1` risk is removed before any real code depends on the mixin configs.
4. **Documentation phase.** Amend `docs/DECISIONS.md` (D-011 amended to Candidate B, D-018 amended
   for the authored `entity_type` field, R-004/R-005 retired, new entries for `GlobalAttachments`
   ownership and the `hasAttached` guard), `docs/ARCHITECTURE.md` §15, `docs/ROADMAP.md` (close
   P-2 and P-9, add the new ones), and `docs/TESTING.md`. Outside this session's boundary.
