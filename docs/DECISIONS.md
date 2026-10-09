# MobVariantsBRS — Architectural Decisions

Each entry cites a tracked repository file with a line reference, or a section of the
approved plan `.kilo/plans/1790903055742-mob-variants-brs-architecture.md`. Entries
without such a citation are not recorded here.

## Status values

| Status | Meaning |
| --- | --- |
| `INHERITED — TEMPLATE` | Present in the repository, inherited unchanged from the Fabric example-mod template. Not a deliberate MobVariantsBRS decision, and no project rationale exists for it. |
| `IMPLEMENTED` | Enacted by a deliberate, MobVariantsBRS-specific artifact in the repository. |
| `PROPOSED` | Decided by the approved architectural plan. **Not implemented.** No source file in this repository enacts it. |

**No entry in this repository carries the status `Accepted`.** The plan's decisions
remain `PROPOSED` regardless of how confidently the plan argues for them; they become
`IMPLEMENTED` only when code exists in a tracked file.

Sections: [A. Project-specific (implemented)](#a-project-specific--implemented) ·
[B. Inherited Fabric scaffold/template configuration](#b-inherited-fabric-scaffoldtemplate-configuration) ·
[C. Proposed architectural decisions](#c-proposed-architectural-decisions) ·
[D. Proposed rejected alternatives](#d-proposed-rejected-alternatives) ·
[E. Non-goals](#e-proposed-non-goals) ·
[F. Superseded](#f-superseded--none-recorded)

---

## A. Project-specific (implemented)

### D-001 — Mod identity and coordinates

- **Status:** `IMPLEMENTED`
- **Decision:** Mod id `mob_variants_brs`; display name `Mob Variants BRS`; package root
  `com.baruc.brs.mobvariants`; Gradle project name `mob_variants_brs`; group
  `com.baruc.brs.mobvariants`; `environment: "*"`.
- **Evidence:** `src/main/resources/fabric.mod.json:3,5,16`; `settings.gradle:13`;
  `gradle.properties:14`; `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java:1,11`.

### D-002 — Version pin set

- **Status:** `IMPLEMENTED`
- **Decision:** Minecraft `26.1.2`, Fabric Loader `0.19.5`, Fabric API `0.155.3+26.1.2`,
  Loom `1.18-SNAPSHOT`, Java `25`, mod version `1.0.0`.
- **Evidence:** `gradle.properties:8-10,13,17`; `build.gradle:50,59-60`;
  `src/main/resources/fabric.mod.json:36-38`; `gradle/wrapper/gradle-wrapper.properties`.
- **Consequence:** `${version}` is expanded into `fabric.mod.json` at build time
  (`build.gradle:40-47`), so `fabric.mod.json` in the repository carries the literal
  placeholder `${version}`, not a number.

---

## B. Inherited Fabric scaffold/template configuration

None of the following was chosen by MobVariantsBRS. They are the state in which the
Fabric example-mod template left the project, and **no project rationale for them
exists in this repository** (the README is still the unmodified template text).

### D-003 — Split `main` / `client` source sets via Loom

- **Status:** `INHERITED — TEMPLATE`
- **Configuration:** `loom { splitEnvironmentSourceSets() }` with
  `mods { "mob_variants_brs" { sourceSet sourceSets.main; sourceSet sourceSets.client } }`.
- **Evidence:** `build.gradle:14-23`.
- **Note:** This is what currently produces the client/server boundary described in
  `docs/ARCHITECTURE.md` §10. The approved plan builds on it but does not originate it
  (plan §7 places new classes into the same two source sets).

### D-004 — Java 25 toolchain

- **Status:** `INHERITED — TEMPLATE`
- **Configuration:** `options.release = 25`, `sourceCompatibility`/
  `targetCompatibility` = `VERSION_25`, `withSourcesJar()`; CI provisions JDK 25;
  `fabric.mod.json` declares `"java": ">=25"`.
- **Evidence:** `build.gradle:49-61`; `.github/workflows/build.yml:20-21`;
  `src/main/resources/fabric.mod.json:38`.
- **Rationale available:** none in the repository. It is implied by the Minecraft
  26.1.2 requirement, but that implication is not documented anywhere in the project.

### D-005 — Client-only Fabric datagen wiring

- **Status:** `INHERITED — TEMPLATE`
- **Configuration:** `fabricApi.configureDataGeneration { client = true }` plus a
  `fabric-datagen` entrypoint class in the `client` source set with an empty body.
- **Evidence:** `build.gradle:25-29`;
  `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSDataGenerator.java:8-10`;
  `src/main/resources/fabric.mod.json:24-26`.
- **Note:** The approved plan records this placement as acceptable and flags it for
  revisit only if datagen is actually used (plan §10 risk 10).

### D-006 — Maven publication with no repository

- **Status:** `INHERITED — TEMPLATE`
- **Configuration:** `maven-publish` with a `mavenJava` publication and an empty
  repository block.
- **Evidence:** `build.gradle:3,73-87`.

### D-007 — License embedding in the jar

- **Status:** `INHERITED — TEMPLATE`
- **Configuration:** `jar { from("LICENSE") { rename { "${it}_$projectName" } } }`.
- **Evidence:** `build.gradle:63-70`.

### D-008 — Mixin compatibility level and injector strictness

- **Status:** `INHERITED — TEMPLATE`
- **Configuration:** `compatibilityLevel: "JAVA_25"`,
  `injectors.defaultRequire: 1`, `overwrites.requireAnnotations: true`, `required: true`
  in both mixin configs.
- **Evidence:** `src/main/resources/mob_variants_brs.mixins.json:2,4,9,12`;
  `src/client/resources/mob_variants_brs.client.mixins.json:2,4,9,12`.
- **Consequence:** with `defaultRequire: 1`, a rename of an injected method hard-fails
  startup. This currently applies to the two inert example mixins.

### D-009 — Official (mojmap) mappings by Loom default, with no declared dependency

- **Status:** `INHERITED — TEMPLATE` for the mapping choice; the *decision not to
  declare it explicitly* is `PROPOSED` (see D-021).
- **Configuration:** `build.gradle:31-38` declares only `minecraft`, `fabric-loader`,
  and `fabric-api`. No `mappings` line exists. The build nevertheless runs on official
  Mojang mappings, evidenced by the Loom mapping cache header
  (`tiny  2  0  official`) and by the mojmap import
  `net.minecraft.resources.Identifier` in `MobVariantsBRS.java:5`.
- **Evidence:** `.gradle/loom-cache/source_mappings/bc646172a33c2ce908d83312ded8de90e10fe897.tiny:1`
  (git-ignored local cache, used for verification only);
  `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java:5`.

### D-010 — CI pipeline

- **Status:** `INHERITED — TEMPLATE`
- **Configuration:** GitHub Actions `build` workflow on `[pull_request, push]`,
  `ubuntu-24.04`, `actions/checkout@v6`, `gradle/actions/wrapper-validation@v6`,
  `actions/setup-java@v5` with JDK 25 (`microsoft` distribution), `./gradlew build`,
  artifact upload of `build/libs/`.
- **Evidence:** `.github/workflows/build.yml:6-30`.
- **Consequence:** CI runs the Gradle `build` task. With no test sources present, that
  means compilation only — see `docs/TESTING.md`.

---

## C. Proposed architectural decisions

**All entries in this section are `PROPOSED`.** Source for every entry:
`.kilo/plans/1790903055742-mob-variants-brs-architecture.md`. No file named in this
section exists in the repository.

### D-011 — Variant state architecture: resolved texture attachment + server snapshot

- **Status:** `IMPLEMENTED`
- **Decision:** Entity persistent state is the resolved texture `Identifier` in
  `VARIANT_TEXTURE`. This attachment is persistent and synchronised to clients. The
  entity does **not** persist a variant ID.
- `SERVER_VARIANT_SNAPSHOT` is separate server/global state stored through
  `GlobalAttachments`. The snapshot is not persistent entity state and is not
  client-synchronised.
- Resolved render state (`VARIANT_TEXTURE`) and definition snapshot
  (`SERVER_VARIANT_SNAPSHOT`) are intentionally separate: the snapshot is rebuilt on
  every reload from datapacks and is needed only for selection; the texture is the
  single value that must survive disk load, chunk unload/reload, teleport, and
  late-join synchronisation.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/attachment/VariantAttachments.java:21-49`;
  `src/main/java/com/baruc/brs/mobvariants/variant/VariantSnapshot.java:16-50`;
  `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java:63-65`.

### D-012 — No custom networking; Fabric Attachment synchronization is used

- **Status:** `IMPLEMENTED`
- **Decision:** No custom networking code; no packets, no payload registration, no
  hand-rolled NBT mixin. Persistence, client synchronisation, and chunk-load
  synchronisation come from Fabric's attachment API on `VARIANT_TEXTURE`.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/attachment/VariantAttachments.java:35-39`;
  `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java:22-23`.

### D-013 — No per-tick processing; selection at ENTITY_LOAD

- **Status:** `IMPLEMENTED`
- **Decision:** The architecture registers no tick hook of any kind. Variant selection and
  biome-condition evaluation run once during the `ENTITY_LOAD` event. The first
  statement of `selectVariant` guards against already-loaded entities; biome lookup is
  performed at most once per selection.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java:38,54-80`.

### D-014 — Two idempotency guards at selection

- **Status:** `IMPLEMENTED`
- **Decision:** Selection is guarded by both `entity.isLoadedFromDisk()` (catches entities
  whose NBT has just been restored) and `entity.hasAttached(VARIANT_TEXTURE)` (the
  load-bearing guard). The persistent attachment is already present on every
  disk-restored, reloaded, teleported, or respawned entity because it survives NBT
  round-trips and Fabric's attachment sync handshake. Together the two guards make
  selection idempotent without depending on `isLoadedFromDisk()` alone.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java:54-61`.

### D-015 — Zero server-side Mixins; two client-side Mixins for the render pipeline

- **Status:** `IMPLEMENTED`
- **Decision:** No server-side mixin is required — selection, persistence, and client
  sync are fully covered by Fabric API. Two client mixins resolve the variant texture:
  - `LivingEntityRenderStateMixin`: extracts the resolved texture from the entity's
    `VARIANT_TEXTURE` attachment and stores it on the `LivingEntityRenderState`.
  - `LivingEntityRendererMixin`: reads the texture from the render state during
    rendering and supplies it to the vanilla renderer, falling through to vanilla when
    no variant texture is present.
- **Evidence:** `src/main/resources/mob_variants_brs.mixins.json:5` (empty mixins array);
  `src/client/resources/mob_variants_brs.client.mixins.json:5-8`;
  `src/client/java/com/baruc/brs/mobvariants/client/mixin/LivingEntityRenderStateMixin.java`;
  `src/client/java/com/baruc/brs/mobvariants/client/mixin/LivingEntityRendererMixin.java`.

### D-016 — Client mixin target method verification (SUPERSEDED)

- **Status:** `SUPERSEDED`
- **Decision:** The plan recorded that the client mixin target was "UNVERIFIED for 26.1.2"
  after the 26.x render-pipeline rework. This concern was resolved: the target methods
  were verified via `genSources` against Minecraft 26.1.2 and the two client mixins
  (`LivingEntityRenderStateMixin`, `LivingEntityRendererMixin`) inject successfully.
- **Source:** plan §6.2, §10 risk 1.
- **Superseded by:** D-015 (implemented mixin list with verified targets).

### D-017 — Client mixin failure mode: `defaultRequire: 1` (hard failure) (SUPERSEDED)

- **Status:** `SUPERSEDED`
- **Decision:** The plan proposed `require = 0` with a warning as a degradation mode. The
  adopted policy is `injectors.defaultRequire: 1` in both mixin configs, making
  injection failure a hard startup failure. This is acceptable because the project
  targets a pinned Minecraft version (26.1.2) and the targets were verified against
  that version.
- **Evidence:** `src/main/resources/mob_variants_brs.mixins.json:7`;
  `src/client/resources/mob_variants_brs.client.mixins.json:10`.
- **Supersedes:** the plan's `require = 0` proposal.

### D-018 — Variant identity derived from resource path

- **Status:** `IMPLEMENTED`
- **Decision:** Variant definition identity is derived from the resource path:
  `data/mob_variants_brs/variants/<name>.json` → `mob_variants_brs:<name>`.
  The directory is flat (no per-entity-type subdirectories). `entity_type` is a required
  JSON field inside the definition and is NOT derived from the path. The persistent
  entity state remains the resolved texture `Identifier` in `VARIANT_TEXTURE`, not the
  variant ID.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/variant/VariantDefinition.java:14-15`;
  `src/main/java/com/baruc/brs/mobvariants/variant/VariantSnapshot.java:70-85`;
  `src/main/resources/data/mob_variants_brs/variants/ice_zombie.json:1-4`.

### D-019 — `VariantSelector` abstraction (REJECTED)

- **Status:** `REJECTED`
- **Decision:** A `VariantSelector` interface was considered as an extension point for
  selection logic but was not adopted. v1 selection is sufficiently small and is
  implemented directly in `VariantStateLifecycle.selectVariant()`; the single-method
  interface would add indirection without benefit.
- **Source:** plan §8.3, §9.6.

### D-020 — No matching candidate → vanilla fallback

- **Status:** `IMPLEMENTED`
- **Decision:** No matching candidate (or no candidate at all) means no `VARIANT_TEXTURE`
  attachment is set on the entity. The client renderer then falls back to vanilla
  rendering because the attachment is absent. The entity does not persist a variant ID,
  so there is no "unknown variant ID" case to degrade from.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java:100-101`;
  `src/main/java/com/baruc/brs/mobvariants/attachment/VariantAttachments.java:22-26`.

### D-021 — Do not declare mappings explicitly; do not guess Loom DSL

- **Status:** `PROPOSED`
- **Decision:** No `mappings` line will be added to `build.gradle`, and no
  `loom.officialMojangMappings()` reference will be written, because the project
  already builds on official mappings through the Loom 1.18 default and the exact Loom
  1.18 DSL spelling is unverified.
- **Source:** plan §0, §5.3.
- **Current state:** this decision is, in effect, already the repository's state
  (no `mappings` line exists), but the plan has not been enacted and the reasoning is
  not encoded anywhere in the build.

### D-022 — No `initializer()` on the attachment

- **Status:** `IMPLEMENTED`
- **Decision:** `VARIANT_TEXTURE` deliberately registers no default initializer. An entity
  with no variant allocates nothing in the attachment map, and `hasAttached` returns
  false, which is used as the second idempotency guard.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/attachment/VariantAttachments.java:28-29,35-39`.

### D-023 — `copyOnDeath()` deliberately left unset

- **Status:** `IMPLEMENTED`
- **Decision:** `VARIANT_TEXTURE` deliberately does not use `copyOnDeath()`. A variant does
  not survive zombie→drowned or other mob conversion in v1.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/attachment/VariantAttachments.java:29-30,35-39`.

---

## D. Rejected alternatives

Recorded where the approved plan or implementation explicitly considered and rejected an
alternative.

| ID | Alternative | Rejected because | Source |
| --- | --- | --- | --- |
| R-001 | Wrap the vanilla renderer per `EntityType` via `EntityRendererRegistry` | Requires one registration per mob type, and the wrapper must forward every current and future vanilla renderer behaviour — duplicating what is to be left untouched, and breaking harder than a mixin. | plan §6.3 |
| R-002 | Use `LivingEntityRenderLayerRegistrationCallback` | Verified in the plan's API review to register render *layers*, not the base body texture. | plan §6.3 |
| R-003 | Custom `ZombieRenderer` / `ZombieModel` / `ZombieEntity` | Rejected outright; the design forbids duplicating vanilla entity, model, or renderer. | plan §6.3, §4.2 |
| R-004 | Encode the variant texture in a vanilla-synced place (e.g. custom name / custom data component) | Leaks presentation into gameplay-visible data, creating a compatibility and cheating surface. | plan §6.3 |
| R-005 | Cache the resolved texture `Identifier` in a second attachment | Duplicates state and creates an invalidation problem on `/reload`, for a gain the plan judges unmeasurable. | plan §3.4 |
| R-006 | Store a per-entity `VariantDefinition` instead of an id | Only the id is stored per entity; the definition is shared and immutable. | plan §3.5 |
| R-007 | Construct the texture `Identifier` per frame | Precomputed at reload instead; the render path does no string work. | plan §3.4, §3.5 |
| R-008 | Apply attributes only through `FabricDefaultAttributeRegistry.MODIFY` | Verified to operate per `EntityType` on `AttributeSupplier.Builder`, so it can change only type defaults, never per-instance values; insufficient on its own. | plan §9.3 |
| R-009 | A tick-based variant behaviour system | Explicitly rejected; behaviour reacts to a variant id, never to a tick. | plan §9.4 |

---

## E. Non-goals

### D-024 — Explicit scope boundary for the core layer

- **Status:** `IMPLEMENTED`
- **Decision:** Health/damage/armour/scaling formulas, stat multipliers, balancing,
  probability weighting, commands, GUI, tag-driven opt-in, and any variant behaviour
  that runs on a tick are out of scope. Only the seams that would accept them later are
  defined.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java:50-52`;
  `src/main/java/com/baruc/brs/mobvariants/attachment/VariantAttachments.java:29-30`.

---

## F. Superseded

The following decisions from the approved plan were superseded by implementation:

| ID | Previous decision | Superseded by | Reason |
| --- | --- | --- | --- |
| D-011 | Single `Identifier` attachment as variant ID | D-011 (implemented) | Actual architecture uses resolved texture `Identifier` + separate server snapshot |
| D-014 | Guard only by `isLoadedFromDisk()` | D-014 (implemented) | Two guards: `isLoadedFromDisk()` + `hasAttached(VARIANT_TEXTURE)` |
| D-015 | Zero server mixins; one client mixin | D-015 (implemented) | Zero server mixins; **two** client mixins |
| D-016 | Client mixin target unverified for 26.1.2 | D-016 (superseded) | Targets verified via `genSources` |
| D-017 | `require = 0` with warning | D-017 (superseded) | Adopted `defaultRequire: 1` (hard failure) |
| D-018 | Path-derived identity with entity type in path | D-018 (implemented) | Flat directory; `entity_type` is JSON field, not path-derived |
| D-019 | `VariantSelector` interface | D-019 (rejected) | Not adopted; selection inlined in `VariantStateLifecycle` |

---

### D-025 — Condition model

- **Status:** `IMPLEMENTED`
- **Decision:** Biome conditions are represented by a dedicated immutable
  `VariantConditions` value object. It is optional in a variant definition and
  encapsulates the condition data and matching logic.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/variant/VariantConditions.java:10-45`.

### D-026 — Optional conditions

- **Status:** `IMPLEMENTED`
- **Decision:** `VariantDefinition` has an optional `conditions` field. Definitions
  without conditions remain unconditional and do not require biome evaluation.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/variant/VariantDefinition.java:17-19,21-26`.

### D-027 — Registry validation at snapshot construction

- **Status:** `IMPLEMENTED`
- **Decision:** Referenced entity types and biome identifiers are validated while
  constructing the immutable server snapshot during resource loading/reload. Invalid
  definitions are isolated instead of causing runtime biome lookups to fail.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/variant/VariantSnapshot.java:70-115`.

### D-028 — Lazy biome evaluation

- **Status:** `IMPLEMENTED`
- **Decision:** Biome lookup occurs only when at least one candidate requires biome
  conditions, and at most one lookup is performed for a given entity selection. The
  result is reused while evaluating candidates.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java:72-80`.

### D-029 — Deterministic candidate ordering

- **Status:** `IMPLEMENTED`
- **Decision:** Variant candidates are sorted deterministically by variant ID during
  resource loading so that first-match selection has reproducible ordering.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/variant/VariantSnapshot.java:34-45,70-72,110-111`.

### D-030 — Server snapshot as global attachment

- **Status:** `IMPLEMENTED`
- **Decision:** The immutable `VariantSnapshot` is published through
  `SERVER_VARIANT_SNAPSHOT` on server `GlobalAttachments`, allowing atomic replacement
  during resource reload without introducing a separate `VariantRegistry` abstraction.
- **Evidence:** `src/main/java/com/baruc/brs/mobvariants/attachment/VariantAttachments.java:41-49`;
  `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java:63-65`.
