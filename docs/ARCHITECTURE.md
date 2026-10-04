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

**IMPLEMENTED.** The repository is an unmodified Fabric example-mod scaffold. There is
no mob-variant system, and **no user-facing mod feature of any kind**.

- Tracked Java sources: 4 files, all template scaffolding (`git ls-files`).
- Tracked resources: `fabric.mod.json`, two mixin configs, one icon.
- The only executed mod behaviour is `MobVariantsBRS.onInitialize()` logging
  `"Hello Fabric world!"` (`src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java:24`).

The following do **not** exist anywhere in tracked sources or resources, and must not
be read as existing: registries, event subscriptions, data attachments, persistent
state, network packets, configuration, `data/` or `assets/` content beyond the icon,
datagen providers, and tests.

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

**PENDING / UNRESOLVED.** `loom_version` is a moving `-SNAPSHOT` target
(`gradle.properties:10`); the resolved build is not pinned anywhere in the repository.
A local `./gradlew build` executed while writing these docs reported
`Fabric Loom: 1.18.2` — that is build output, not a repository fact. The mappings
choice is expressed only by the absence of a `mappings` line, which the approved plan
flags as fragile (plan §5.3).

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
    mixin/ExampleMixin.java
src/main/resources/
    fabric.mod.json
    mob_variants_brs.mixins.json
    assets/mob_variants_brs/icon.png
src/client/java/com/baruc/brs/mobvariants/client/
    MobVariantsBRSClient.java
    MobVariantsBRSDataGenerator.java
    mixin/ExampleClientMixin.java
src/client/resources/
    mob_variants_brs.client.mixins.json

docs/                        this documentation baseline
```

**IMPLEMENTED.** There is no `src/test` source set and no `data/` directory under
`src/main/resources`.

## 4. Gradle / Loom configuration and source sets

**INHERITED SCAFFOLD/TEMPLATE CONFIGURATION**, except where noted.

- `build.gradle:2-4` applies `net.fabricmc.fabric-loom` at `${loom_version}` and
  `maven-publish`.
- `build.gradle:14-23`: `loom { splitEnvironmentSourceSets() }` with
  `mods { "mob_variants_brs" { sourceSet sourceSets.main; sourceSet sourceSets.client } }`.
  This produces the `main` and `client` source sets that give the mod its
  client/server boundary at compile time.
- `build.gradle:25-29`: `fabricApi.configureDataGeneration { client = true }` — datagen
  is wired, but no provider is registered (`MobVariantsBRSDataGenerator.java:8-10`
  is empty).
- `build.gradle:31-38`: dependencies are Minecraft, Fabric Loader, and Fabric API only.
  **No test framework, no JUnit, no MixinExtras dependency.**
- `build.gradle:40-47`: `processResources` expands `${version}` into `fabric.mod.json`.
- `build.gradle:49-61`: `options.release = 25`; `sourceCompatibility`/`targetCompatibility`
  = `VERSION_25`; `withSourcesJar()`.
- `build.gradle:63-70`: the packaged jar embeds `LICENSE` renamed to `LICENSE_<projectName>`.
- `build.gradle:73-87`: a `mavenJava` publication with **no repository configured**.
- `settings.gradle:13`: `rootProject.name = 'mob_variants_brs'`, matching the mod id.

**IMPLEMENTED — project-specific.** Only the mod id, package root, group, and version
were changed from the template. The split source sets, Java level, datagen flag,
publication block, and licence-embedding behaviour are all template defaults.

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

**Known inconsistency (template placeholders still shipped).** `description` is
"This is an example description! …" (line 6), `authors` is `["Me!"]` (lines 7-9),
and `contact` points at `https://fabricmc.net/` and
`https://github.com/FabricMC/fabric-example-mod` (lines 10-13). `license` and
`LICENSE` are the template's. Reported, not changed — the approved plan lists their
replacement as planned cleanup (plan §0.1).

## 6. Entrypoints

**IMPLEMENTED.** Declared at `fabric.mod.json:17-27`, each implemented by a tracked file:

| Entrypoint | Class | File | Current body |
| --- | --- | --- | --- |
| `main` | `com.baruc.brs.mobvariants.MobVariantsBRS` | `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` | logs `"Hello Fabric world!"` (line 24) |
| `client` | `com.baruc.brs.mobvariants.client.MobVariantsBRSClient` | `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSClient.java` | empty `onInitializeClient()` (lines 7-9) |
| `fabric-datagen` | `com.baruc.brs.mobvariants.client.MobVariantsBRSDataGenerator` | `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSDataGenerator.java` | empty `onInitializeDataGenerator()` (lines 8-10) |

The `main` class also exposes the only reusable utility in the repository:
`MobVariantsBRS.MOD_ID`, `MobVariantsBRS.LOGGER` (lines 11, 16) and
`MobVariantsBRS.id(String)` returning `Identifier.fromNamespaceAndPath(MOD_ID, path)`
(lines 27-29).

**PENDING.** The datagen entrypoint lives in the `client` source set. The approved
plan records this as acceptable for now and flags it for revisit only if datagen is
actually used (plan §10 risk 10).

## 7. Mixin configuration

**IMPLEMENTED.** Two configs, split by the object form in `fabric.mod.json:28-34`
(the client config carries `"environment": "client"`):

| Config | Package | Lists | Mixin config settings |
| --- | --- | --- | --- |
| `src/main/resources/mob_variants_brs.mixins.json` | `com.baruc.brs.mobvariants.mixin` | `ExampleMixin` | `required: true`, `compatibilityLevel: JAVA_25`, `injectors.defaultRequire: 1`, `overwrites.requireAnnotations: true` |
| `src/client/resources/mob_variants_brs.client.mixins.json` | `com.baruc.brs.mobvariants.client.mixin` | `ExampleClientMixin` (under `client`) | same settings |

**IMPLEMENTED, but inert example scaffolding.** Both injected bodies are empty:

- `src/main/java/com/baruc/brs/mobvariants/mixin/ExampleMixin.java:9-14` —
  `@Mixin(MinecraftServer.class)` with `@Inject(at = @At("HEAD"), method = "loadLevel")`
  and an empty body.
- `src/client/java/com/baruc/brs/mobvariants/client/mixin/ExampleClientMixin.java:9-14` —
  `@Mixin(Minecraft.class)` with `@Inject(at = @At("HEAD"), method = "run")` and an
  empty body.

Both are template scaffolding with no MobVariantsBRS behaviour. **Known
inconsistency:** with `defaultRequire: 1`, a future rename of `loadLevel` or `run`
would hard-fail startup for behaviour that does nothing. This directly contradicts the
approved plan, which states that no server-side mixin is required (§6.1) and lists
deleting both as planned cleanup (§0.1).

## 8. Existing packages and classes

**IMPLEMENTED.** Package root `com.baruc.brs.mobvariants`.

| Class | Source set | Role today |
| --- | --- | --- |
| `MobVariantsBRS` | main | `ModInitializer`; `MOD_ID`, `LOGGER`, `id(String)` |
| `mixin.ExampleMixin` | main | inert template mixin on `MinecraftServer.loadLevel` |
| `MobVariantsBRSClient` | client | `ClientModInitializer`; empty |
| `MobVariantsBRSDataGenerator` | client | `DataGeneratorEntrypoint`; empty |
| `client.mixin.ExampleClientMixin` | client | inert template mixin on `Minecraft.run` |

No further packages exist. In particular there are no `attachment/`, `definition/`,
`registry/`, `selection/`, or `render/` packages.

## 9. Existing resources

**IMPLEMENTED.** Exactly one asset: `src/main/resources/assets/mob_variants_brs/icon.png`.
There is no `src/main/resources/data/` directory and no texture, model, lang, recipe,
tag, or loot-table content.

Datagen is wired (`build.gradle:25-29`, `fabric-datagen` entrypoint) but registers no
providers, so **no generated content exists**.

**Known inconsistency — stale build output (untracked, ignored).**
`build/resources/main/data/mob_variants_brs/variants/zombie/ice.json` was present on
disk when these docs were written, with contents
`{"base_entity": "minecraft:zombie", "texture": "mob_variants_brs:zombie/ice"}`.
It has **no source file** anywhere under `src/`, it is git-ignored (`.gitignore:4`), and
it is produced by no current code path. It is repository residue from a deleted
experiment, **not** evidence of any implemented architecture, variant data format, or
variant-system functionality. Its schema also contradicts the approved plan's proposed
`VariantDefinition`, which derives the entity type from the file path and treats
`texture` as optional (plan §8.1). Reported, not treated as design.

Note on state: the `./gradlew build` executed while writing these docs removed the file
as stale Gradle output. That is a generated, git-ignored effect of running the existing
build; no tracked file was involved and no configuration was changed.

## 10. Client / server boundary

**IMPLEMENTED.** The boundary today is enforced by exactly three mechanisms:

1. The split `main` / `client` source sets (`build.gradle:14-23`), which prevent
   client-only classes from being reachable from the `main` source set.
2. The `client` entrypoint (`MobVariantsBRSClient`), which is the only client-side
   mod code.
3. The client-scoped mixin config (`fabric.mod.json:30-33`), so
   `ExampleClientMixin` cannot be applied on a dedicated server.

`environment: "*"` (`fabric.mod.json:16`) declares the mod as valid on both client and
server, and both `main` and `client` source sets are compiled into the single
`mob_variants_brs` mod.

There is **no** synchronisation of any kind between client and server: no packets, no
payloads, no attachment sync, no state replication. Nothing gameplay-relevant crosses
the boundary today.

## 11. Current state and persistence

**IMPLEMENTED.** The mod holds no state.

- No per-entity state, no gameplay state, no persisted or serialised state.
- The only mutable object the mod owns is Fabric's own loader/datagen state.
- The only mod-level state is the immutable pair of constants `MobVariantsBRS.MOD_ID`
  and `MobVariantsBRS.LOGGER` (`MobVariantsBRS.java:11,16`).
- The mod writes no files, no NBT, and no configuration.

No cache, registry, snapshot, or in-memory index exists in any form.

## 12. Actual extension points present today

**IMPLEMENTED.** These are the only extension points the repository actually contains:

| Extension point | Where | Currently used? |
| --- | --- | --- |
| `main` mod entrypoint | `MobVariantsBRS.onInitialize()` | log line only |
| `client` mod entrypoint | `MobVariantsBRSClient.onInitializeClient()` | empty |
| `fabric-datagen` entrypoint | `MobVariantsBRSDataGenerator` | empty |
| Server/common mixin config | `src/main/resources/mob_variants_brs.mixins.json` | one inert template mixin |
| Client mixin config | `src/client/resources/mob_variants_brs.client.mixins.json` | one inert template mixin |
| Identifier helper | `MobVariantsBRS.id(String)` | unused |

## 13. Current architectural constraints

**IMPLEMENTED / INHERITED.**

- Java 25 is mandatory at compile (`build.gradle:50`) and at runtime
  (`fabric.mod.json:38`), and CI provisions JDK 25 (`build.yml:20-21`).
- Mappings are official/mojmap with no declared `mappings` dependency; the build file
  expresses this only by omission.
- `environment: "*"` means the `main` source set must remain dedicated-server safe.
- `sourceCompatibility`/`targetCompatibility` are pinned to `VERSION_25`.
- Fabric API is a hard runtime dependency (`fabric.mod.json:39`).
- The project is published nowhere: the `mavenJava` publication has no repository
  (`build.gradle:81-86`).
- `build/` and `run/` are git-ignored (`.gitignore:4,33`), so `run/` artifacts such as
  `run/logs/latest.log` are untracked local evidence, not repository source.

## 14. Runtime observation (untracked local evidence)

The untracked, git-ignored `run/logs/latest.log` records a successful load of
`mob_variants_brs 1.0.0` (line 46) and the single `Hello Fabric world!` line (line 49),
among 43 loaded mods (line 3). This is positive evidence that the scaffold loads and
that the two template mixins currently apply. It is local state, not a repository file,
and it is not part of the tracked architecture.

---

## 15. Proposed target architecture

> ### ⚠️ PROPOSED — NOT IMPLEMENTED
>
> **Everything in this section describes an architectural proposal only. No source
> file in this repository implements any part of it.** Every class, package, resource,
> and behaviour named below is a *target*, sourced exclusively from
> `.kilo/plans/1790903055742-mob-variants-brs-architecture.md`. The classes
> `VariantDefinition`, `VariantDefinitionLoader`, `VariantRegistry`,
> `VariantAttachments`, `VariantSelector`, `FirstDefinitionSelector`, and
> `MobRendererMixin`, the `data/mob_variants_brs/variants/**` datapack path, the
> attachment-based state model, the reload listener, the networking-free design, and
> the persistence model **do not exist in this repository**.

### 15.1 Three-layer design (plan §1.1)

- **Definition layer** — variant JSON under
  `data/mob_variants_brs/variants/<entity>/<name>.json`, parsed off-thread by a
  reload listener, decoded into immutable definitions, published as an immutable map
  snapshot, and looked up through a `VariantRegistry`.
- **State layer** — exactly one piece of state per entity: the variant's
  `Identifier`, held in a Fabric Data Attachment on `Entity` (plan §1.2, §1.3).
- **Render layer** — the vanilla renderer plus exactly one client mixin that reads
  only the attachment (plan §6.2).

Under the proposal, vanilla `Entity`/AI/movement/combat/equipment/model/renderer are
untouched: the variant is a label plus a definition lookup.

### 15.2 Proposed lifecycle (plan §2)

Register attachment and reload listener at mod init; parse definitions off-thread on
`/reload` and publish an immutable snapshot atomically; assign a variant id once at
entity spawn via an `ENTITY_LOAD` listener guarded by `entity.isLoadedFromDisk()`;
let Fabric's attachment API handle NBT persistence and client sync; do **nothing per
tick**; resolve the texture per render.

### 15.3 Proposed target file tree (plan §7)

Not a description of the current tree — the current tree is in §3 above.

```
src/main/java/com/baruc/brs/mobvariants/
  MobVariantsBRS.java                 (exists; would grow the wiring)
  attachment/VariantAttachments.java  [PROPOSED]
  definition/VariantDefinition.java   [PROPOSED]
  definition/VariantDefinitionLoader.java [PROPOSED]
  registry/VariantRegistry.java       [PROPOSED]
  selection/VariantSelector.java      [PROPOSED]
  selection/FirstDefinitionSelector.java [PROPOSED]
src/client/java/com/baruc/brs/mobvariants/client/
  MobVariantsBRSClient.java           (exists; would host loader registration)
  mixin/MobRendererMixin.java         [PROPOSED]
src/main/resources/
  data/mob_variants_brs/variants/zombie/ice.json  [PROPOSED]
src/client/resources/
  assets/mob_variants_brs/textures/entity/zombie/zombie_ice.png [PROPOSED]
```

The proposal explicitly states that `render/` is *not* created (plan §7).

### 15.4 Proposed data model (plan §8)

An immutable three-field record: variant `id` and `entityType` both derived from the
file path, plus an `Optional<Identifier> texture` as the only authored field. A variant
with an empty `{}` body would be valid. `VariantSelector` is a one-method interface
returning the first candidate in v1, meaning every mob of a type spawns as the same
variant — stated in the plan as a deliberate placeholder.

### 15.5 Proposed mixin posture (plan §6)

- Server side: **no** mixin required.
- Client side: **exactly one** mixin on the vanilla texture lookup.
- The exact 26.1.2 target is **UNVERIFIED** by the plan itself: the 1.21.x name
  `LivingEntityRenderer#getTextureLocation` "may be gone or may have moved" after the
  26.x render-pipeline rework (plan §6.2, §10 risk 1). Verifying it is planned task 1.
- Recommended failure mode is `require = 0` plus a one-time warning, with the plan
  explicitly leaving the loud-failure option open (plan §6.2).

### 15.6 Proposed constraints and open points

- No `mappings` line and no `loom.officialMojangMappings()` call will be added
  (plan §5.3).
- API items the plan marks as *inferred, must be confirmed by the compiler*:
  `EntitySpawnReason` constant names, `Identifier.CODEC` and its `StreamCodec`,
  `PreparableReloadListener.SharedState` resource access, and whether
  `ResourceLoader.registerReloadListener` covers the client as well as the server
  (plan §5.2).
- Server-only datapacks are inherently asymmetric with clients; unknown ids are to
  degrade to the vanilla texture rather than warn loudly (plan §10 risk 4).

Planning status for all of the above is tracked in `ROADMAP.md`; the decisions
themselves are in `DECISIONS.md`; validation is in `TESTING.md`.
