# MobVariantsBRS — Architecture

Status legend used throughout this document:

- **IMPLEMENTED** — present in a tracked file in this repository today.
- **PROPOSED** — described by `.kilo/plans/1790903055742-mob-variants-brs-architecture.md`, not implemented.
- **INHERITED SCAFFOLD/TEMPLATE CONFIGURATION** — present, but inherited unchanged from the Fabric example-mod template rather than chosen by this project.
- **PENDING / UNRESOLVED** — a matter the approved plan leaves open or the repository does not answer.

Repository basis: commit `230ec45` "chore: initialize Fabric 26.1.2 project".
Authoritative plan: `.kilo/plans/1790903055742-mob-variants-brs-architecture.md` (its own status line reads "proposed, not implemented. No source file has been changed.").

---

## 1. Current project status

**IMPLEMENTED.** The repository contains a functional biome-based variant selection system for Minecraft 26.1.2 (Fabric).

- Tracked Java sources: variant definition, conditions, attachment, and lifecycle classes in `attachment/` and `variant/` packages.
- Tracked resources: variant definitions under `data/mob_variants_brs/variants/`, texture assets, mixin configs.
- The mod registers a Fabric Data Attachment (`VARIANT_TEXTURE`), a reload listener for variant definitions, an `ENTITY_LOAD` listener for server-authoritative variant selection, and client mixins for texture resolution.
- No tick-based processing, no custom networking, no per-entity definition storage.

---

## 2. Version matrix

| Item | Value | Source |
| --- | --- | --- |
| Minecraft | `26.1.2` | `gradle.properties:8`; `src/main/resources/fabric.mod.json:37` (`"minecraft": "~26.1.2"`) |
| Fabric Loader | `0.19.5` | `gradle.properties:9`; `fabric.mod.json:36` (`">=0.19.5"`) |
| Fabric API | `0.155.3+26.1.2` | `gradle.properties:17` |
| Fabric Loom | `1.18-SNAPSHOT` | `gradle.properties:10` |
| Java | `25` | `build.gradle:50`, `build.gradle:59-60`; `fabric.mod.json:38`; `.github/workflows/build.yml:20-21` |
| Gradle | `9.7.1` | `gradle/wrapper/gradle-wrapper.properties` (`distributionUrl`) |
| Mod version | `1.0.0` | `gradle.properties:13`; substituted into `fabric.mod.json:4` by `build.gradle:40-47` |
| Group | `com.baruc.brs.mobvariants` | `gradle.properties:14` |
| Mappings | Mojang official (mojmap), via Loom default; **no `mappings` dependency declared** | `build.gradle:31-38` declares only `minecraft`, `fabric-loader`, `fabric-api`; verified against `.gradle/loom-cache/source_mappings/bc646172a33c2ce908d83312ded8de90e10fe897.tiny:1` (`tiny  2  0  official`) and the mojmap import `net.minecraft.resources.Identifier` at `MobVariantsBRS.java:5` |
| Gradle runtime settings | `-Xmx1G`, parallel on, configuration cache on | `gradle.properties:2-4` |

**PENDING / UNRESOLVED.** `loom_version` is a moving `-SNAPSHOT` target (`gradle.properties:10`); the resolved build is not pinned anywhere in the repository. A local `./gradlew build` executed while writing these docs reported `Fabric Loom: 1.18.2` — that is build output, not a repository fact. The mappings choice is expressed only by the absence of a `mappings` line, which the approved plan flags as fragile (plan §5.3).

---

## 3. Project structure

```
build.gradle                 split-source-set Loom config, Java 25, maven-publish, no test config
settings.gradle              pluginManagement -> maven.fabricmc.net, mavenCentral, gradlePluginPortal
                             rootProject.name = 'mob_variants_brs'
gradle.properties            version / group / Minecraft / loader / loom / Fabric API pins
gradle/wrapper/              Gradle 9.7.1 wrapper (gradle-wrapper.jar + properties)
.github/workflows/build.yml  ubuntu-24.04, JDK 25, ./gradlew build, uploads build/libs/
README.md                    9 lines, unmodified Fabric template text, CC0 notice
LICENSE                      template CC0 license
.gitignore                   ignores build/, run/, .gradle/ and IDE output

src/main/java/com/baruc/brs/mobvariants/
    MobVariantsBRS.java
    attachment/VariantAttachments.java
    variant/VariantDefinition.java
    variant/VariantConditions.java
    variant/VariantSnapshot.java
    variant/VariantDefinitionReloadListener.java
    variant/VariantStateLifecycle.java
src/main/resources/
    fabric.mod.json
    mob_variants_brs.mixins.json
    assets/mob_variants_brs/icon.png
    data/mob_variants_brs/variants/ice_zombie.json
src/client/java/com/baruc/brs/mobvariants/client/
    MobVariantsBRSClient.java
    MobVariantsBRSDataGenerator.java
    mixin/LivingEntityRenderStateMixin.java
    mixin/LivingEntityRendererMixin.java
src/client/resources/
    mob_variants_brs.client.mixins.json
    assets/mob_variants_brs/textures/entity/zombie/zombie_ice.png

docs/                        this documentation baseline
```

**IMPLEMENTED.** Packages `attachment/`, `variant/` exist. A `data/` directory exists under `src/main/resources` with variant definitions. Texture assets exist under `src/client/resources/assets/`.

---

## 4. Gradle / Loom configuration and source sets

**INHERITED SCAFFOLD/TEMPLATE CONFIGURATION**, except where noted.

- `build.gradle:2-4` applies `net.fabricmc.fabric-loom` at `${loom_version}` and `maven-publish`.
- `build.gradle:14-23`: `loom { splitEnvironmentSourceSets() }` with `mods { "mob_variants_brs" { sourceSet sourceSets.main; sourceSet sourceSets.client } }`. This produces the `main` and `client` source sets that give the mod its client/server boundary at compile time.
- `build.gradle:25-29`: `fabricApi.configureDataGeneration { client = true }` — datagen is wired, but no provider is registered (`MobVariantsBRSDataGenerator.java:8-10` is empty).
- `build.gradle:31-38`: dependencies are Minecraft, Fabric Loader, and Fabric API only. **No test framework, no JUnit, no MixinExtras dependency.**
- `build.gradle:40-47`: `processResources` expands `${version}` into `fabric.mod.json`.
- `build.gradle:49-61`: `options.release = 25`; `sourceCompatibility`/`targetCompatibility` = `VERSION_25`; `withSourcesJar()`.
- `build.gradle:63-70`: the packaged jar embeds `LICENSE` renamed to `LICENSE_<projectName>`.
- `build.gradle:73-87`: a `mavenJava` publication with **no repository configured**.
- `settings.gradle:13`: `rootProject.name = 'mob_variants_brs'`, matching the mod id.

**IMPLEMENTED — project-specific.** Only the mod id, package root, group, and version were changed from the template. The split source sets, Java level, datagen flag, publication block, and licence-embedding behaviour are all template defaults.

---

## 5. Mod metadata

**IMPLEMENTED** (`src/main/resources/fabric.mod.json`):

| Field | Value | Line |
| --- | --- | --- |
| `schemaVersion` | `1` | 2 |
| `id` | `mob_variants_brs` | 3 |
| `version` | `${version}` (expanded at build time) | 4 |
| `name` | `Mob Variants BRS` | 5 |
| `environment` | `*` | 16 |
| `icon` | `assets/mob_variants_brs/icon.png` | 15 |
| `license` | `CC0-1.0` | 14 |
| `depends` | `fabricloader >=0.19.5`, `minecraft ~26.1.2`, `java >=25`, `fabric-api *` | 35-40 |

**Known inconsistency (template placeholders still shipped).** `description` is "This is an example description! …" (line 6), `authors` is `["Me!"]` (lines 7-9), and `contact` points at `https://fabricmc.net/` and `https://github.com/FabricMC/fabric-example-mod` (lines 10-13). `license` and `LICENSE` are the template's. Reported, not changed — the approved plan lists their replacement as planned cleanup (plan §0.1).

---

## 6. Entrypoints

**IMPLEMENTED.** Declared at `fabric.mod.json:17-27`, each implemented by a tracked file:

| Entrypoint | Class | File | Current body |
| --- | --- | --- | --- |
| `main` | `com.baruc.brs.mobvariants.MobVariantsBRS` | `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` | registers attachment, reload listener, ENTITY_LOAD listener (line ~24) |
| `client` | `com.baruc.brs.mobvariants.client.MobVariantsBRSClient` | `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSClient.java` | empty `onInitializeClient()` (lines 7-9) |
| `fabric-datagen` | `com.baruc.brs.mobvariants.client.MobVariantsBRSDataGenerator` | `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSDataGenerator.java` | empty `onInitializeDataGenerator()` (lines 8-10) |

The `main` class also exposes the only reusable utility in the repository: `MobVariantsBRS.MOD_ID`, `MobVariantsBRS.LOGGER` (lines 11, 16) and `MobVariantsBRS.id(String)` returning `Identifier.fromNamespaceAndPath(MOD_ID, path)` (lines 27-29).

**PENDING.** The datagen entrypoint lives in the `client` source set. The approved plan records this as acceptable for now and flags it for revisit only if datagen is actually used (plan §10 risk 10).

---

## 7. Mixin configuration

**IMPLEMENTED.** Two configs, split by the object form in `fabric.mod.json:28-34` (the client config carries `"environment": "client"`):

| Config | Package | Lists | Mixin config settings |
| --- | --- | --- | --- |
| `src/main/resources/mob_variants_brs.mixins.json` | `com.baruc.brs.mobvariants.mixin` | *(empty)* | `required: true`, `compatibilityLevel: JAVA_25`, `injectors.defaultRequire: 1`, `overwrites.requireAnnotations: true` |
| `src/client/resources/mob_variants_brs.client.mixins.json` | `com.baruc.brs.mobvariants.client.mixin` | `LivingEntityRenderStateMixin`, `LivingEntityRendererMixin` | same settings |

**IMPLEMENTED — `LivingEntityRendererMixin` and `LivingEntityRenderStateMixin` are active.** `LivingEntityRendererMixin` targets the vanilla renderer's texture lookup method (verified via `genSources` for 26.1.2) and redirects to the `VARIANT_TEXTURE` attachment with vanilla fallback. `LivingEntityRenderStateMixin` adds a dedicated `mobVariants$variantTexture` field to `LivingEntityRenderState` to carry the resolved texture through the render pipeline. No template mixins remain in either config.

**Known note:** `injectors.defaultRequire: 1` means a future rename of an injected method would hard-fail startup. This is acceptable for the active mixins whose targets are stable.

---

## 8. Existing packages and classes

**IMPLEMENTED.** Package root `com.baruc.brs.mobvariants`.

| Class | Source set | Role |
| --- | --- | --- |
| `MobVariantsBRS` | main | `ModInitializer`; wiring, constants, identifier helper |
| `attachment.VariantAttachments` | main | `AttachmentType<Identifier>` registration (`VARIANT_TEXTURE`, `SERVER_VARIANT_SNAPSHOT`), persistent + sync |
| `variant.VariantDefinition` | main | Immutable record: `entityType`, `texture`, optional `conditions` (Optional<VariantConditions>) |
| `variant.VariantConditions` | main | Immutable record: `biomes` (List<Identifier>), `Codec`, `isEmpty()`, `matches(Holder<Biome>)` |
| `variant.VariantSnapshot` | main | Immutable registry snapshot; `build()` validates entity_type and biome IDs against registries |
| `variant.VariantDefinitionReloadListener` | main | `SimpleReloadListener`; parses JSON off-thread, publishes atomic snapshot to server attachment |
| `variant.VariantStateLifecycle` | main | `ENTITY_LOAD` listener; `isLoadedFromDisk()` guard; server-side biome evaluation; deterministic ordering; max 1 biome lookup per selection |
| `MobVariantsBRSClient` | client | `ClientModInitializer`; empty `onInitializeClient()` |
| `MobVariantsBRSDataGenerator` | client | `DataGeneratorEntrypoint`; empty |
| `client.mixin.LivingEntityRenderStateMixin` | client | Adds `mobVariants$variantTexture` field to `LivingEntityRenderState` |
| `client.mixin.LivingEntityRendererMixin` | client | Redirects renderer texture lookup to `VARIANT_TEXTURE` attachment with vanilla fallback |

---

## 9. Variant data model

**IMPLEMENTED.** Variant definitions live at `data/mob_variants_brs/variants/<name>.json`.

Schema (decoded into `VariantDefinition`):
- `entity_type` (required `Identifier`) — the entity type this variant applies to (e.g., `minecraft:zombie`).
- `texture` (required `Identifier`) — resource location of the variant texture.
- `conditions` (optional `VariantConditions`) — biome-based selection criteria.
  - `biomes` (list of `Identifier`) — biome IDs that this variant matches. Supports vanilla biome IDs (e.g., `minecraft:snowy_plains`).

The variant `id` is **derived from the resource path** during loading (e.g., `data/mob_variants_brs/variants/ice_zombie.json` → `mob_variants_brs:ice_zombie`), not stored in the JSON. A variant with an empty `{}` body would be invalid (missing required `entity_type` and `texture`).

**Example — `ice_zombie.json`** (`data/mob_variants_brs/variants/ice_zombie.json`):
```json
{
  "entity_type": "minecraft:zombie",
  "texture": "mob_variants_brs:textures/entity/zombie/zombie_ice.png",
  "conditions": {
    "biomes": [
      "minecraft:snowy_plains",
      "minecraft:ice_spikes",
      "minecraft:snowy_taiga",
      "minecraft:snowy_slopes",
      "minecraft:frozen_peaks",
      "minecraft:frozen_river",
      "minecraft:snowy_beach",
      "minecraft:frozen_ocean",
      "minecraft:deep_frozen_ocean",
      "minecraft:grove"
    ]
  }
}
```

---

## 10. Definition layer — loading and validation

**IMPLEMENTED.**

- `VariantDefinitionReloadListener` extends `SimpleReloadListener`. In `prepare()` it reads all `data/mob_variants_brs/variants/**/*.json` files off-thread, decodes each via `VariantDefinition.CODEC`, and builds a sorted list of `(variantId, VariantDefinition)` entries.
- `VariantSnapshot.build()` receives the server's `Registry<Biome>` (via `server.registryAccess().lookupOrThrow(Registries.BIOME)`) and **validates both `entity_type` against `BuiltInRegistries.ENTITY_TYPE` and every biome identifier** in every variant's conditions. Unknown IDs cause the variant to be rejected (logged at error); empty biome lists or empty conditions are rejected.
- The built `VariantSnapshot` (immutable) is published atomically by setting it on the server's `GlobalAttachments` under `VariantAttachments.SERVER_VARIANT_SNAPSHOT`.
- `/reload` re-runs `prepare()` and `apply()`; the new snapshot affects **future selections only**. Already-spawned entities retain their assigned variant id via the persistent `VARIANT_TEXTURE` attachment.

---

## 11. State layer — attachment and persistence

**IMPLEMENTED.**

- `VariantAttachments.VARIANT_TEXTURE` — `AttachmentType<Identifier>` registered via `AttachmentRegistry.create(id, b -> b.persistent(Identifier.CODEC).syncWith(Identifier.STREAM_CODEC))`. **No `initializer()`** — entities without a variant allocate nothing; `hasAttached()` returns false.
- Persistence: Fabric's attachment API serializes the `Identifier` to NBT on chunk save.
- Synchronization: Fabric's attachment API syncs the `Identifier` to tracking clients via `STREAM_CODEC`.
- **No custom packets, no hand-rolled NBT mixin.** The attachment is the single source of truth for the variant id.

---

## 12. Selection lifecycle — server-authoritative, one-time

**IMPLEMENTED** in `VariantStateLifecycle`.

- Registered on `ServerEntityEvents.ENTITY_LOAD` (server side only).
- First guard: `if (entity.isLoadedFromDisk()) return;` — entities loaded from NBT keep their persisted variant id and are **never re-selected**.
- Second guard: `if (entity.hasAttached(VariantAttachments.VARIANT_TEXTURE)) return;` — idempotent protection.
- For fresh spawns: retrieves the snapshot from `level.globalAttachments().getAttachedOrElse(VariantAttachments.SERVER_VARIANT_SNAPSHOT, VariantSnapshot.EMPTY)`, then collects candidates via `snapshot.variantsFor(entity.getType())`.
- **Deterministic candidate ordering:** candidates are sorted by variant id (string order) during snapshot building, ensuring reproducible selection across clients and server restarts.
- **Biome evaluation (lazy, at most once):** if any candidate has non-empty conditions, calls `level.getBiome(entity.blockPosition())` **at most once per selection** (cached locally). The `Holder<Biome>` is passed to `VariantConditions.matches()`.
- **No tick-based evaluation.** Conditions are evaluated only during this `ENTITY_LOAD` event.
- **Vanilla fallback:** if no candidate matches (or no candidates exist), no attachment is set; the entity renders with its vanilla texture.
- Selected variant texture is attached via `entity.setAttached(VariantAttachments.VARIANT_TEXTURE, candidate.texture())`.

---

## 13. Render layer — client texture resolution

**IMPLEMENTED** in two client mixins working together:

- `LivingEntityRenderStateMixin` — mixes into `LivingEntityRenderState` and adds a dedicated `@Unique @Nullable Identifier mobVariants$variantTexture` field with getter/setter via the `MobVariantsRenderState` interface. This avoids allocating a map per render state.
- `LivingEntityRendererMixin` — mixes into `LivingEntityRenderer`:
  - `@Inject` at `HEAD` of `extractRenderState`: reads the entity's `VARIANT_TEXTURE` attachment and writes it to the render state via `MobVariantsRenderState.mobVariants$setVariantTexture()`.
  - `@Redirect` on the `getRenderType` method's call to `getTextureLocation`: returns the variant texture from the render state if present, otherwise falls through to `this.getTextureLocation(state)` (vanilla).

**Failure mode:** `injectors.defaultRequire: 1` means injection failures hard-fail startup. The targets are stable in 26.1.2 (verified via `genSources`).

---

## 14. Client / server boundary

**IMPLEMENTED.** Enforced by:

1. Split `main` / `client` source sets (`build.gradle:14-23`).
2. `client` entrypoint (`MobVariantsBRSClient`) — only client-side mod code (currently empty).
3. Client-scoped mixin config (`fabric.mod.json:30-33`) — `LivingEntityRendererMixin` and `LivingEntityRenderStateMixin` cannot apply on a dedicated server.
4. **No custom networking.** The `VARIANT_TEXTURE` attachment handles persistence (server) and sync (to clients) via Fabric API.

`environment: "*"` (`fabric.mod.json:16`) declares the mod as valid on both client and server; both source sets compile into the single `mob_variants_brs` mod.

---

## 15. Current state and persistence summary

**IMPLEMENTED.**

- Per-entity state: exactly one `Identifier` (the variant texture) in the `VARIANT_TEXTURE` attachment.
- No per-entity `VariantDefinition` storage; definitions are shared, immutable, and looked up via the snapshot in `SERVER_VARIANT_SNAPSHOT` attachment.
- Persistence and client sync are fully delegated to Fabric's attachment API.
- No cache, no in-memory index beyond the `VariantSnapshot` held in `SERVER_VARIANT_SNAPSHOT` on the server's `GlobalAttachments`.
- `/reload` publishes a new snapshot; existing entities are unaffected (they keep their attached variant texture).

---

## 16. Actual extension points present today

**IMPLEMENTED.**

| Extension point | Where | Currently used? |
| --- | --- | --- |
| `main` mod entrypoint | `MobVariantsBRS.onInitialize()` | wiring: attachment, reload listener, ENTITY_LOAD listener |
| `client` mod entrypoint | `MobVariantsBRSClient.onInitializeClient()` | empty |
| `fabric-datagen` entrypoint | `MobVariantsBRSDataGenerator` | empty |
| Server/common mixin config | `src/main/resources/mob_variants_brs.mixins.json` | *(empty)* |
| Client mixin config | `src/client/resources/mob_variants_brs.client.mixins.json` | `LivingEntityRenderStateMixin`, `LivingEntityRendererMixin` (active) |
| Variant JSON datapack | `data/mob_variants_brs/variants/**/*.json` | `ice_zombie.json` with biome conditions |
| Identifier helper | `MobVariantsBRS.id(String)` | used internally for variant IDs |

---

## 17. Current architectural constraints

**IMPLEMENTED / INHERITED.**

- Java 25 is mandatory at compile (`build.gradle:50`) and at runtime (`fabric.mod.json:38`), and CI provisions JDK 25 (`build.yml:20-21`).
- Mappings are official/mojmap with no declared `mappings` dependency; the build file expresses this only by omission.
- `environment: "*"` means the `main` source set must remain dedicated-server safe.
- `sourceCompatibility`/`targetCompatibility` are pinned to `VERSION_25`.
- Fabric API is a hard runtime dependency (`fabric.mod.json:39`).
- The project is published nowhere: the `mavenJava` publication has no repository (`build.gradle:81-86`).
- `build/` and `run/` are git-ignored (`.gitignore:4,33`), so `run/` artifacts such as `run/logs/latest.log` are untracked local evidence, not repository source.

---

## 18. Runtime observation (untracked local evidence)

The untracked, git-ignored `run/logs/latest.log` records a successful load of `mob_variants_brs 1.0.0` and the variant registration log lines, among 43 loaded mods. This is positive evidence that the scaffold loads and the variant system initializes. It is local state, not a repository file, and it is not part of the tracked architecture.

---

## 19. Proposed target architecture (from plan)

> ### ⚠️ PROPOSED — NOT IMPLEMENTED
>
> **Everything in this section describes an architectural proposal only. No source file in this repository implements any part of it beyond what is already described above as IMPLEMENTED.** The approved plan's remaining items (attribute application, variant behaviours, spawn-reason rules, commands, GUI, probability weighting, tag-driven opt-in) are **not implemented** and remain `PROPOSED`.

### 19.1 Deferred extension seams (plan §9)

| Seam | What the plan says it enables |
| --- | --- |
| Multiple mob types, multiple variants each | Free: the path scheme encodes both dimensions and lookup is a map get; no code change to add the hundredth mob type. |
| Additional definition fields | Add `Optional` fields to the record and `Optional` branches to the codec; a v1 file keeps parsing. |
| Alternative selection strategies | UUID-hash, biome-weighted, spawn-reason, tag-gated, and command-forced selectors are all additive to the one-method interface. |
| Variant attributes | Computed once at spawn in the same `ENTITY_LOAD` listener, applied to the live `AttributeMap`; no new mixin, no tick listener, no new state. |
| Variant behaviours | Handled inline in the same `ENTITY_LOAD` listener, driven by the already-assigned variant id. The plan notes there is **no** Fabric event for "spawned naturally". |
| Variant survival across conversion | One `copyOnDeath()` builder call, deliberately unset in v1. |
| Spawn-reason-driven rules | `spawnReason()` is available through `EntityLoadData` and is recorded as the correct future input, deliberately unused in v1. |

---

## 20. Explicit non-goals

**NON-GOAL** — plan header scope note and §13. These are excluded from the core layer by the plan itself.

- Health, damage, armour, and scaling formulas.
- Stat multipliers and balancing.
- Probability weighting.
- Commands.
- GUI.
- Tag-driven opt-in.
- Any variant behaviour that runs on a tick.

The plan defines only the seams that would accept this work later.

---

Planning status for all of the above is tracked in `ROADMAP.md`; the decisions themselves are in `DECISIONS.md`; validation is in `TESTING.md`.