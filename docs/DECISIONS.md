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

### D-011 — Variant state is a single `Identifier` attachment on `Entity`

- **Status:** `PROPOSED`
- **Decision:** The entity carries exactly one piece of variant state: the variant's
  `Identifier`, stored through Fabric's Data Attachment API. Everything else is
  derived, immutable, and shared.
- **Source:** plan §1.2, §1.3, §9.
- **Rationale given by the plan:** small (one NBT string), server-authoritative, makes
  re-evaluation structurally impossible, and lets a definition change on `/reload`
  reach every varianted mob without touching an entity.
- **Not implemented:** no attachment type, no `AttachmentRegistry` call, and no
  attachment data exists in this repository.

### D-012 — No custom networking code

- **Status:** `PROPOSED`
- **Decision:** The mod ships no packets, no payload registration, and no hand-rolled
  NBT mixin. Persistence, client synchronisation, and chunk-load synchronisation come
  from Fabric's attachment API.
- **Source:** plan §1.3, §5.1.
- **Not implemented:** the repository contains no networking code at all.

### D-013 — No per-tick listener anywhere

- **Status:** `PROPOSED`
- **Decision:** The architecture registers no tick hook of any kind. Future variant
  behaviour reacts to the variant id at spawn, never on a tick; a tick-based variant
  behaviour system is explicitly rejected.
- **Source:** plan §2 (step 8), §3.3, §9.4.
- **Not implemented:** trivially true today, because the mod has no listeners at all.

### D-014 — Selection is guarded by `entity.isLoadedFromDisk()`

- **Status:** `PROPOSED`
- **Decision:** Selection runs in an `ENTITY_LOAD` listener whose first statement is
  `if (entity.isLoadedFromDisk()) return;`, so a mob loaded from disk keeps its
  persisted id and is never re-selected.
- **Source:** plan §2.1, §2.2.
- **Rationale given by the plan:** `ENTITY_LOAD` fires for both spawn and NBT load, so
  the event alone is insufficient; the guard is satisfied without a mixin because
  vanilla `Entity` implements `EntityLoadData` exposing `isLoadedFromDisk()`.
- **Not implemented:** no event listener is registered in this repository.

### D-015 — Zero server-side mixins; exactly one client-side mixin

- **Status:** `PROPOSED`
- **Decision:** No server-side mixin is required — selection, persistence, and client
  sync are fully covered by Fabric API. Exactly one client mixin resolves a variant's
  texture, falling through to vanilla when the entity has no variant.
- **Source:** plan §6.1, §6.2.
- **Not implemented:** the only mixins in the repository are the two inert template
  mixins `ExampleMixin` and `ExampleClientMixin`, neither of which performs variant work.

### D-016 — The client mixin's target method is unverified for 26.1.2

- **Status:** `PROPOSED` (and explicitly unverified by the plan)
- **Fact recorded here:** the plan states that in 1.21.x mojmap the target was
  `LivingEntityRenderer#getTextureLocation(LivingEntity)`, but that this name is
  **"UNVERIFIED for 26.1.2"** after the 26.x render-pipeline rework, and that reading
  the real class via `genSources` is the first implementation task.
- **Source:** plan §6.2, §10 risk 1.
- **This document therefore does not name a target method as decided.**

### D-017 — Failure mode of the client mixin: `require = 0` plus one warning

- **Status:** `PROPOSED`, explicitly reversible
- **Decision:** The plan's recommendation is `require = 0` with a single `LOGGER.warn`
  on first application failure, so a rename degrades to "variants render with the
  vanilla texture" instead of hard-crashing on a Minecraft update. The plan explicitly
  says this default may be overridden in favour of a loud startup failure.
- **Source:** plan §6.2 (failure-mode row), §10 risk 1.
- **Status note:** this remains an open choice, not a settled decision.

### D-018 — Path-derived variant identity

- **Status:** `PROPOSED`
- **Decision:** A variant's identity is derived from its file path,
  `data/mob_variants_brs/variants/<entityTypePath>/<name>.json`; the path is the single
  source of truth for both the variant id and the entity type, so no field in the data
  file can contradict it.
- **Source:** plan §8.1, §8.2.
- **Not implemented:** no `data/mob_variants_brs/` directory exists under
  `src/main/resources`.

### D-019 — `VariantSelector` is the single selection extension point

- **Status:** `PROPOSED`
- **Decision:** Selection is a one-method interface; the v1 implementation returns the
  first candidate. The plan states plainly that this makes every mob of a type share
  one variant, and that this is a deliberate placeholder so a real selector replaces
  exactly one class.
- **Source:** plan §8.3, §9.6.
- **Not implemented:** no such interface or implementation exists.

### D-020 — Unknown variant id degrades to vanilla

- **Status:** `PROPOSED`
- **Decision:** A variant id that no longer resolves — because its definition was
  deleted, renamed, or exists only on the server — is treated as "no variant"; the mod
  falls back to vanilla rendering and logs at debug rather than warn.
- **Source:** plan §4.3, §10 risks 4 and 6.

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

- **Status:** `PROPOSED`
- **Decision:** The attachment registers no default initializer, so an entity with no
  variant allocates nothing and `hasAttached` returns false.
- **Source:** plan §3.5, §11 item 4.

### D-023 — `copyOnDeath()` deliberately left unset

- **Status:** `PROPOSED`, deliberate
- **Decision:** A variant does **not** survive a zombie→drowned or other mob conversion
  in v1, because `copyOnDeath()` is left unset. The plan records this as a low-severity
  deliberate choice with a one-builder-call remedy if the design ever wants it.
- **Source:** plan §10 risk 8.

---

## D. Proposed rejected alternatives

Recorded **only** where the approved plan states the alternative and its rejection.
Each remains `PROPOSED`.

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

## E. Proposed non-goals

### D-024 — Explicit scope boundary for the core layer

- **Status:** `PROPOSED`
- **Decision:** Health/damage/armour/scaling formulas, stat multipliers, balancing,
  probability weighting, commands, GUI, tag-driven opt-in, and any variant behaviour
  that runs on a tick are out of scope. Only the seams that would accept them later are
  defined.
- **Source:** plan header scope note, §13.

---

## F. Superseded — none recorded

No decision in this repository has been superseded. The commit history contains a
single commit, `230ec45` "chore: initialize Fabric 26.1.2 project", so there is no
decision history to record.
