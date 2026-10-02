# MobVariantsBRS — Architectural Documentation Plan

Status: planning only. No `docs/` directory, no documentation files, and no source,
build, or configuration changes are made by this plan. The deliverable of a future
Code Mode session is four Markdown files under `docs/`.

---

## 1. Current architectural findings (verified)

The repository is an **unmodified Fabric example-mod scaffold**. There is no mob
variant implementation at all. Every architectural fact below was read directly
from a tracked file.

### 1.1 Version matrix (authoritative sources only)

| Item | Value | Evidence |
| --- | --- | --- |
| Minecraft | `26.1.2` | `gradle.properties:8`; corroborated by `src/main/resources/fabric.mod.json:37` (`"minecraft": "~26.1.2"`) |
| Fabric Loader | `0.19.5` | `gradle.properties:9`; `fabric.mod.json:36` (`">=0.19.5"`) |
| Fabric API | `0.155.3+26.1.2` | `gradle.properties:17` |
| Fabric Loom | `1.18-SNAPSHOT` | `gradle.properties:10` |
| Java | 25 | `build.gradle:50` (`options.release = 25`), `build.gradle:59-60`; `fabric.mod.json:38` (`">=25"`); `.github/workflows/build.yml:20-21` (Java 25, `microsoft` distribution) |
| Gradle | `9.7.1` | `gradle/wrapper/gradle-wrapper.properties:3`; corroborated by `.gradle/9.7.1/` cache dir |
| Gradle properties | parallel + configuration cache on, `-Xmx1G` | `gradle.properties:2-4` |
| Mod version | `1.0.0` | `gradle.properties:13`; substituted into `fabric.mod.json:4` by `build.gradle:40-47` |
| Group | `com.baruc.brs.mobvariants` | `gradle.properties:14` |
| Mappings | **Mojang official (mojmap)**, via Loom default; no `mappings` dependency declared | `.gradle/loom-cache/source_mappings/bc646172a33c2ce908d83312ded8de90e10fe897.tiny:1` = `tiny  2  0  official`; corroborated by the mojmap import `net.minecraft.resources.Identifier` in `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java:5`. `build.gradle:31-38` declares only `minecraft`, `fabric-loader`, `fabric-api`. |

### 1.2 Project layout

```
build.gradle                 split-source-set Loom config, java 25, maven-publish, no test config
settings.gradle              pluginManagement -> maven.fabricmc.net, mavenCentral, gradlePluginPortal
                             rootProject.name = 'mob_variants_brs'
gradle.properties            version/group/MC/loader/loom/Fabric-API pins
gradle/wrapper/              gradle 9.7.1
.github/workflows/build.yml  ubuntu-24.04, JDK 25, `./gradlew build`, uploads build/libs/
README.md                    9 lines; still the Fabric template text, CC0 notice only
LICENSE                      CC0 template license
src/main/java/com/baruc/brs/mobvariants/
    MobVariantsBRS.java              ModInitializer; MOD_ID, LOGGER, static Identifier id(String)
    mixin/ExampleMixin.java         @Mixin(MinecraftServer.class), @Inject HEAD "loadLevel", empty body
src/main/resources/
    fabric.mod.json                  entrypoints main/client/fabric-datagen; 2 mixin configs; depends block
    mob_variants_brs.mixins.json     lists ExampleMixin; compatibilityLevel JAVA_25; defaultRequire 1
    assets/mob_variants_brs/icon.png only asset
src/client/java/com/baruc/brs/mobvariants/client/
    MobVariantsBRSClient.java        ClientModInitializer, empty onInitializeClient
    MobVariantsBRSDataGenerator.java DataGeneratorEntrypoint, empty onInitializeDataGenerator
    mixin/ExampleClientMixin.java   @Mixin(Minecraft.class), @Inject HEAD "run", empty body
src/client/resources/
    mob_variants_brs.client.mixins.json  client-scoped, lists ExampleClientMixin
```

### 1.3 Systems that do **not** exist (must not be documented as present)

No registries, no event subscriptions, no attachments, no persistent state, no
network packets, no configuration system, no `data/` or `assets/` content beyond
the icon, no datagen providers, no tests, no `src/test/` source set, no JUnit or
test-framework dependency in `build.gradle`.

### 1.4 Runtime facts

- Mod id `mob_variants_brs`; package root `com.baruc.brs.mobvariants`; split source
  sets declared in `build.gradle:14-23`.
- The only executed mod code is `MobVariantsBRS.onInitialize()` logging
  `"Hello Fabric world!"` (`MobVariantsBRS.java:19-25`).
- `run/logs/latest.log:46,49` confirms a successful load of `mob_variants_brs 1.0.0`
  and that single log line. `run/logs/latest.log:3` shows 43 mods loaded.
- Both mixins are inert scaffolding with empty injected bodies; they perform no work.
- Datagen is *wired* (`fabricApi.configureDataGeneration { client = true }`,
  `build.gradle:25-29`, plus the `fabric-datagen` entrypoint) but the generator
  registers no providers.

### 1.5 The only substantive design artifact

`.kilo/plans/1790903055742-mob-variants-brs-architecture.md` (598 lines) — an
architecture proposal whose own line 3 reads *"Status: **proposed, not implemented.**
No source file has been changed."* It contains: a verified environment table (§0),
a three-layer design (§1), lifecycle table (§2), performance analysis (§3),
compatibility analysis (§4), API verification with V/I/U confidence labels (§5),
mixin analysis (§6), proposed file structure (§7), data model (§8), extension
points (§9), risk register (§10), an 11-item task list (§11), validation (§12),
and an out-of-scope list (§13).

Per user decision, this file is the **source of truth for explicitly planned work
and for Proposed decisions**. It must never be recorded as implemented.

---

## 2. Repository evidence inspected

Tracked files (complete, 20 files, confirmed via `git ls-files`):

- `README.md`, `LICENSE`, `.gitignore`, `.gitattributes`
- `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`
- `.github/workflows/build.yml`
- `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java`
- `src/main/java/com/baruc/brs/mobvariants/mixin/ExampleMixin.java`
- `src/main/resources/fabric.mod.json`, `src/main/resources/mob_variants_brs.mixins.json`
- `src/main/resources/assets/mob_variants_brs/icon.png`
- `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSClient.java`
- `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSDataGenerator.java`
- `src/client/java/com/baruc/brs/mobvariants/client/mixin/ExampleClientMixin.java`
- `src/client/resources/mob_variants_brs.client.mixins.json`
- `.kilo/plans/1790903055742-mob-variants-brs-architecture.md` (untracked plan artifact, present on disk)

Untracked / ignored evidence inspected for factual grounding only (must not be
cited as repository source):

- `build/resources/main/data/mob_variants_brs/variants/zombie/ice.json` — stale
  build output from a deleted experiment; contents
  `{"base_entity":"minecraft:zombie","texture":"mob_variants_brs:zombie/ice"}`.
  Its field names (`base_entity`, `texture`) **contradict** the record shape in
  §8.1 of the proposal plan (`entityType` derived from path, `texture` optional).
- `.gradle/loom-cache/source_mappings/*.tiny` — confirms `official` mappings.
- `run/logs/latest.log` — confirms the mod loads and logs once.

---

## 3. Classification rules the writing session must apply

Every statement in the future docs carries one of four labels, stated explicitly
in the docs' front matter so readers can never confuse them:

1. **IMPLEMENTED** — present in a tracked file right now. In this repository that
   is only: the scaffold layout, the three entrypoints, two inert example mixins,
   the icon asset, the Gradle/CI configuration, and the version matrix.
2. **ESTABLISHED DECISION** — explicitly documented by an existing project
   artifact. The only artifact is the proposal plan; its decisions are therefore
   **Proposed**, not Accepted.
3. **EXPLICITLY PLANNED** — named as future work by the proposal plan (§9, §11, §13).
4. **SPECULATIVE / FUTURE IDEA** — anything the writing session thinks of that is
   not traceable to the repository. Must be excluded entirely, or confined to an
   explicitly labelled "not a commitment" appendix. Default: exclude.

No status may be assigned without a citable repository line reference.

---

## 4. Proposed documentation structure

```
docs/
├── ARCHITECTURE.md   current-state architecture only; ends with an explicit
│                     "Proposed, not implemented" forward-reference section
├── DECISIONS.md      ADRs, all with status Proposed, sourced to the plan file
├── ROADMAP.md        Implemented / Planned / Pending / Unresolved / Non-goals
└── TESTING.md        what exists (build only) vs. procedures still to be run
```

Shared header block in all four files:

```
Status legend: IMPLEMENTED | PROPOSED (planned) | SPECULATIVE (not committed)
Repository basis: commit 230ec45 "chore: initialize Fabric 26.1.2 project"
Last verified against: <commit sha> at time of writing
```

---

## 5. Detailed plan — `docs/ARCHITECTURE.md`

Purpose: describe the architecture that **actually exists today**, so a reader is
never misled about what the mod does. Must be written from tracked files only.

Recommended sections, each with its evidence:

1. **Scope and status banner** — scaffold state; variant system not implemented.
   Evidence: all four tracked Java files; proposal plan line 3.
2. **Compatibility matrix** — the §1.1 table verbatim (MC 26.1.2, Loader 0.19.5,
   Fabric API 0.155.3+26.1.2, Loom 1.18-SNAPSHOT, Java 25, Gradle 9.7.1,
   official/mojmap mappings with no declared `mappings` dependency).
3. **Build and project model** — split source sets `main`/`client`
   (`build.gradle:14-23`), datagen client flag (`:25-29`), version expansion
   (`:40-47`), sourcesJar (`:57`), publishing block (`:73-87`), no test config.
   Must state explicitly: **no test framework and no `src/test` source set exist.**
4. **Mod metadata** — id, name, version substitution, icon path, `environment: "*"`,
   `depends` constraints (`fabric.mod.json`).
5. **Entrypoints** — table of the three entrypoints with class paths, declared at
   `fabric.mod.json:17-27` and implemented in the three corresponding files.
   Note each entrypoint's body is currently empty or log-only.
6. **Mixin configuration** — two configs, split client/server via the
   `fabric.mod.json:28-34` array form; `compatibilityLevel: JAVA_25`;
   `defaultRequire: 1`. Document that both registered mixins are **inert example
   scaffolding** (`ExampleMixin` → `MinecraftServer.loadLevel` HEAD;
   `ExampleClientMixin` → `Minecraft.run` HEAD) and perform no behaviour.
7. **Package and class inventory** — the five source files with one-line roles.
   `MobVariantsBRS.id(String)` helper at `MobVariantsBRS.java:27-29` is the only
   reusable utility present.
8. **Resources and assets** — only `assets/mob_variants_brs/icon.png`; no `data/`
   directory exists in `src/main/resources`. Datagen is wired but registers no
   providers, so **no generated content exists**.
9. **Client/server boundary** — what the split source set enforces today (nothing
   beyond the entrypoints and mixin configs). Must NOT describe any synchronisation,
   because none exists.
10. **Authoritative state** — state plainly: there is no gameplay state, no
    per-entity state, and no persisted state. The only mutable state is
    `VariantRegistry`-shaped *nothing*; the sole stateful artifacts are Fabric's
    own loader state and the mod's immutable `MOD_ID`/`LOGGER` constants.
11. **Extension points actually present** — the three entrypoints, the two mixin
    configs, and the datagen entrypoint. These are the *only* stable extension
    points in the repository.
12. **Architectural constraints** — Java 25, official mappings, dedicated-server
    safe main source set, `environment: "*"`.
13. **Proposed target architecture (not implemented)** — a short, clearly fenced
    section summarizing the proposal plan's three layers and one-attachment state
    model, with an explicit "PROPOSED — no source file implements this" banner
    and a link to `DECISIONS.md` / `ROADMAP.md`. No file paths from §7 of the
    plan may be written as if they exist; they must be rendered as a *target tree*.

Facts to verify before writing §3/§7/§8: confirm no `src/test/` and no test
dependency reappear in `build.gradle`; confirm `build/` and `run/` remain ignored
(`.gitignore:4,33`).

Information to exclude: any claim that variants exist, that a registry exists,
that networking is used, that Data Attachments are used, or that the renderer mixin
target has been verified. Mixin necessity (§6.2 of the plan) is **Proposed**, not
current.

---

## 6. Detailed plan — `docs/DECISIONS.md`

Purpose: record verifiable architectural decisions with honest status and
evidence. Format per entry: ID, Title, Status, Date/context, Context, Decision,
Evidence, Consequences, Alternatives (only where evidence exists).

Every entry below must cite `file:line` or a plan-document section.

| ID | Decision | Status | Evidence | Rationale available? |
| --- | --- | --- | --- | --- |
| D-001 | Split source sets `main`/`client` via `loom.splitEnvironmentSourceSets()` | Proposed (currently in effect by template) | `build.gradle:14-23` | No — template default, no repo rationale. Record as inherited scaffold fact, not a project decision. |
| D-002 | Java 25 toolchain | Proposed (in effect) | `build.gradle:50,59-60`; `.github/workflows/build.yml:20-21`; `fabric.mod.json:38` | Only implicit: MC 26.1.2 requires it. Mark rationale as "implied by MC 26.1.2 requirement", not a project choice. |
| D-003 | Official/mojmap mappings, no explicit `mappings` dependency | Proposed (in effect) | no `mappings` line in `build.gradle:31-38`; `.gradle/loom-cache/source_mappings/*.tiny:1`; `Identifier` import at `MobVariantsBRS.java:5`; plan §0 and §5.3 | Yes — plan §5.3 explicitly argues against adding one. Status Proposed. |
| D-004 | Store variant state as a single `AttachmentType<Identifier>` on `Entity` rather than a custom entity/NBT mixin | **Proposed** | plan §1.2, §1.3, §9 | Yes — plan §1.3 (small, server-authoritative, no re-evaluation, survives reload) |
| D-005 | Zero networking code; rely on Fabric's attachment sync | **Proposed** | plan §1.3, §5.1 | Yes — plan §1.3 |
| D-006 | No per-tick listener anywhere; behaviour reacts to the variant id | **Proposed** | plan §2 step 8, §3.3, §9.4 | Yes — plan §3.3, §9.4 |
| D-007 | Guard selection with `entity.isLoadedFromDisk()` in `ENTITY_LOAD` | **Proposed** | plan §2.1 | Yes — plan §2.1 (event fires on both spawn and NBT load) |
| D-008 | Exactly one client-side mixin (texture resolution); zero server mixins | **Proposed** | plan §6.1, §6.2 | Yes — plan §6.2; alternative rejections in §6.3 |
| D-009 | Mixin uses `require = 0` + first-failure warn | **Proposed (explicitly optional — plan says "override this if you would rather fail loudly")** | plan §6.2 failure-mode row, §10 risk 1 | Yes |
| D-010 | Reject renderer wrapping via `EntityRendererRegistry` | **Proposed / Rejected alternative** | plan §6.3 | Yes — stated reasons present |
| D-011 | Reject caching resolved texture in a second attachment | **Proposed / Rejected alternative** | plan §3.4 | Yes — "unmeasurable gain / invalidation problem" |
| D-012 | No `initializer()` on the attachment, so unvarianted entities allocate nothing | **Proposed** | plan §3.5 | Yes |
| D-013 | Definition identity derived from file path `data/mob_variants_brs/variants/<entityTypePath>/<name>.json` | **Proposed** | plan §8.1 | Yes — path is single source of truth |
| D-014 | `VariantSelector` interface as the single selection extension point | **Proposed** | plan §8.3, §9.6 | Yes |
| D-015 | Treat unknown variant id as "no variant", fall back to vanilla | **Proposed** | plan §4.3, §10 risks 4 and 6 | Yes |
| D-016 | Leave `copyOnDeath()` unset in v1 | **Proposed (deliberate)** | plan §10 risk 8 | Yes |
| D-017 | Datagen entrypoint stays in the `client` source set | **Proposed (accepted-as-is)** | `MobVariantsBRSDataGenerator.java` in `src/client`; plan §10 risk 10 | Yes — "works; leave alone" |
| D-018 | Health/stats/probability/commands/GUI/tick-behaviour explicitly out of scope | **Proposed non-goal** | plan §13, header scope note | Yes |

Rules for the writing session:

- Every entry needs at least one concrete evidence citation. No citation → the
  entry is deleted, not softened.
- `Accepted` may only be used for decisions the repository has actually enacted.
  In the current repository that set is empty; if the writing session wants to use
  `Accepted` for the inherited template facts (D-001..D-003), it must add the note
  "accepted by template inheritance, not by an explicit project decision".
- No `Superseded` entries exist yet; the section may exist with a stated
  "none recorded" note.
- Do not restate plan §5.1 API verification tables as decisions — they are
  research notes, not decisions.

---

## 7. Detailed plan — `docs/ROADMAP.md`

Purpose: represent future work without inventing requirements. Structure:

1. **Legend and evidence rule** — each item cites its source section in
   `.kilo/plans/1790903055742-mob-variants-brs-architecture.md`.
2. **Shipped (IMPLEMENTED)** — scaffold, entrypoints, mixin configs, icon,
   Gradle/CI. State explicitly that **no user-facing mod feature exists**.
3. **Cleanup of scaffold residue (EXPLICITLY PLANNED)** — plan §0.1: delete
   `ExampleMixin` / `ExampleClientMixin`, empty the `mixins` array, replace the
   `fabric.mod.json` placeholders (`description` "This is an example description!",
   `authors` `["Me!"]`, `contact` pointing at fabricmc.net and
   fabric-example-mod).
4. **Phase 1 — core (EXPLICITLY PLANNED)** — plan §11 items 3-8:
   `VariantDefinition`, `VariantAttachments`, `VariantDefinitionLoader`,
   `VariantRegistry`, `VariantSelector` + `FirstDefinitionSelector`, entrypoint
   wiring with the `isLoadedFromDisk()` guard.
5. **Phase 2 — content and client (EXPLICITLY PLANNED)** — plan §11 items 9-11:
   `variants/zombie/ice.json` + texture asset, client loader registration,
   `MobRendererMixin`, reload-listener ordering.
6. **Phase 0 — verification gate (EXPLICITLY PLANNED, blocking)** — plan §11
   item 1: `genSources`, confirm renderer method signature, `Identifier` Codec /
   StreamCodec names, `SharedState` resource access, whether
   `ResourceLoader.registerReloadListener` covers the client. Label: must complete
   before Phase 1.
7. **Pending / unresolved (EXPLICITLY PLANNED but undecided)** — plan §5.2 inferred
   items, plan §6.2 `require = 0` vs. fail-loudly, plan §9.3 attribute application
   shape, plan §10 risk 3 client-registration symptom, plan §10 risk 4
   server-only datapack asymmetry.
8. **Deferred extension seams (EXPLICITLY PLANNED, no commitment)** — plan §9.1-§9.6:
   multiple mobs, attributes, behaviours, extra definition fields, alternative
   selectors. Each labelled as a seam, not a scheduled item.
9. **Open risks carried forward** — plan §10 rows 1-10 with severity, restated as
   tracked risks, not tasks.
10. **Explicit non-goals** — plan §13, verbatim scope.
11. **Speculative ideas** — none. Any item the writing session wants to add here
    must be deleted instead. This section exists only to state that it is empty.

Information to exclude: no dates, no effort estimates, no release numbers, no
prioritisation beyond the plan's own ordering, no new features the plan does not
name.

---

## 8. Detailed plan — `docs/TESTING.md`

Purpose: separate *existing automated validation* from *procedures still to be
performed*, without pretending either is more than it is.

1. **Status banner** — the repository has **zero automated tests**. There is no
   `src/test` source set, no test dependency in `build.gradle`, and no
   `test`/`check` configuration block. Evidence: full read of `build.gradle`;
   `git ls-files` shows no test files.
2. **Existing automated validation (IMPLEMENTED)**
   - `./gradlew build` locally — the only compile-level check.
   - `.github/workflows/build.yml` — CI on `[pull_request, push]`, ubuntu-24.04,
     JDK 25 (`microsoft`), wrapper validation, artifact upload to `build/libs/`.
     Note: CI runs `build`, which with no test sources only compiles.
3. **Compile-time verification role** — plan §12 states `gradlew build` is the
   primary verification for every "inferred" item in plan §5.2. Frame this as
   *planned* validation, not current capability.
4. **Procedures to perform when Phase 1 lands (EXPLICITLY PLANNED)** — plan §12:
   - `gradlew runServer`, spawn a zombie, `/data get entity` shows the attachment
     in NBT; unload/reload chunk; id unchanged (proves the disk-load guard).
   - `gradlew runClient`, same zombie renders with the variant texture.
   - Edit JSON + `/reload` → change applies with no respawn.
   - Rename a variant JSON + `/reload` → vanilla render, no crash.
   - Negative test: no mod listener invoked during steady-state ticking.
5. **Functional coverage matrix (all rows currently NOT executed)** — table with
   columns: Area | Scenario | Expected | Status.
   Required rows: build validation; dedicated-server spawn; client render;
   multiplayer with two clients; client/server sync of the attachment;
   persistence across chunk unload/load; persistence across `/reload`;
   persistence across full server restart; player join/rejoin mid-session;
   entity conversion (zombie→drowned) with `copyOnDeath()` unset; unknown-id
   fallback; configuration behaviour (**no configuration system exists — row must
   read "N/A, not implemented"**); `/reload` listener ordering; datagen run
   (no providers registered — row must read "no-op").
6. **Dedicated-server and multiplayer specifics** — server authority of the
   attachment, client receives sync, chunk load/unload, multiple players each
   seeing the same variant, server-only datapack asymmetry (plan §10 risk 4).
7. **Client-side specifics** — the mixin is the only client behaviour; state that
   with `require = 0` a broken mixin degrades silently and the test procedure must
   explicitly assert the texture *changed*, not merely that the game launched.
8. **Regression validation** — CI `./gradlew build` on every push; wrapper
   validation; plus a manual checklist to be defined at implementation time.
9. **Known validation gaps** — no automated coverage of parsing, reload,
   persistence, or rendering; all multiplayer checks are manual-only.

Every row must carry an explicit status token: `EXISTING`, `PROPOSED`, or
`N/A — not implemented`. No row may imply coverage that does not exist.

---

## 9. Inconsistencies and missing information (report; do not fix)

1. **`build/resources/main/data/mob_variants_brs/variants/zombie/ice.json`**
   exists as stale output but has no source. It is git-ignored (`.gitignore:4`).
   Its schema (`base_entity`, required `texture`) contradicts proposal plan §8.1
   (`entityType` derived from path; `texture` is `Optional`). Report; do not resolve.
2. **`README.md` is the unmodified Fabric template** and contains no project
   information. Any claim sourced from it would be false.
3. **`fabric.mod.json` placeholders**: `description` = "This is an example
   description!…", `authors` = `["Me!"]`, `contact.homepage` = `https://fabricmc.net/`,
   `contact.sources` = `https://github.com/FabricMC/fabric-example-mod`. The mod
   advertises itself as an example.
4. **`fabric.mod.json` `license: "CC0-1.0"`** and `LICENSE` are the template's.
5. **Dead scaffold mixins are still registered** with `defaultRequire: 1`, so a
   rename of `MinecraftServer.loadLevel` or `Minecraft.run` in a future Minecraft
   version would hard-fail startup. This directly contradicts proposal plan §6.1
   ("no server-side mixin required") and §0.1 ("delete them").
6. **`ExampleMixin` targets `loadLevel` with no explicit descriptor**, and
   `ExampleClientMixin` targets `run`; whether these resolve on 26.1.2 is
   unverified from the repository (the mod did load successfully per
   `run/logs/latest.log`, which is positive evidence that they do currently apply).
7. **Datagen entrypoint lives in the `client` source set** — plan §10 risk 10
   calls this acceptable but flags it.
8. **`loom_version` is `1.18-SNAPSHOT`** — a moving target; the exact resolved Loom
   build is not recorded anywhere in the repository.
9. **No `mappings` declaration** while the project clearly uses official mappings.
   Plan §5.3 documents the decision but the build file expresses it only by
   omission, which is fragile.
10. **No tests and no validation evidence** beyond "CI runs `./gradlew build`".
    Nothing in the repository demonstrates the mod works at runtime beyond the
    single `"Hello Fabric world!"` log line.
11. **Rationale gaps**: no ADR history, no design doc other than the proposal
    plan, no issue tracker references, no commit history beyond the single
    initializing commit `230ec45`. The template-inherited choices (D-001..D-003)
    have no project rationale.
12. **The proposal plan's §6.2 renderer mixin target is explicitly unverified**
    ("UNVERIFIED for 26.1.2"). This must not be written as fact in any document.
13. **Ownership ambiguity**: `VariantDefinitionLoader` resource reading, the
    client-vs-common reload registration boundary, and the selection trigger are
    all described in the plan but have no assigned owner module in the current
    repository. Document as unresolved.

---

## 10. Acceptance criteria for the future documentation session

1. `docs/` contains exactly the four files and nothing else.
2. No repository file outside `docs/` is modified; `git status` shows only the
   four new untracked files.
3. Every factual claim about the current architecture cites a tracked file, and
   every such file/line reference is verified to exist before the docs are written.
4. `ARCHITECTURE.md` never describes the variant system, attachments, networking,
   persistence, registries, or configuration as implemented.
5. `DECISIONS.md` contains no `Accepted` entry without either an implementation
   artifact or an explicit "accepted by template inheritance" note; no invented
   rationale; no alternatives section unless the plan states them.
6. `ROADMAP.md` separates the four categories with a visible legend; the
   speculative section is empty or explicitly disclaimed; no item lacks a source
   citation in the proposal plan.
7. `TESTING.md` contains no claim of existing test coverage; every matrix row is
   tagged `EXISTING`, `PROPOSED`, or `N/A — not implemented`.
8. Cross-document consistency: a reader who reads all four files cannot conclude
   that any feature is both implemented and planned, or that a speculative idea is
   committed.
9. Inconsistencies in §9 above are documented (at least in a "Known
   inconsistencies" section) rather than silently resolved.
10. Version numbers in `ARCHITECTURE.md` match `gradle.properties` and
    `fabric.mod.json` exactly.

---

## 11. Validation procedure after the docs are written

1. `git status` — confirm only `docs/*.md` are new; no other file changed.
2. Re-read `gradle.properties`, `fabric.mod.json`, `build.gradle` and diff the
   version table in `ARCHITECTURE.md` against them.
3. Verify every cited path exists: `ls` each `src/...`, `build.gradle`,
   `gradle.properties`, `settings.gradle`, `gradle/wrapper/gradle-wrapper.properties`,
   `.github/workflows/build.yml`.
4. Grep the docs for forbidden claim terms and confirm each occurrence is inside
   a clearly labelled Proposed/Planned context: `AttachmentRegistry`, `VariantRegistry`,
   `PayloadTypeRegistry`, `ENTITY_LOAD`, `persistent`, `copyOnDeath`,
   `isLoadedFromDisk`, `MobRendererMixin`.
5. Grep the docs for status tokens (`IMPLEMENTED`, `PROPOSED`, `SPECULATIVE`) and
   confirm each claim carries one.
6. Cross-check `ROADMAP.md` §7 (unresolved) against `TESTING.md` §9 (gaps) and
   `DECISIONS.md` status column — no item may be "Pending" in one document and
   "Accepted" in another.
7. Confirm no proposed class name from plan §7 (e.g. `VariantDefinition.java`,
   `MobRendererMixin.java`) is written as an existing file; each must be inside a
   target-tree block or a Planned table.
8. Run `./gradlew build` before and after; both must succeed (documentation must not
   affect the build). Note: this is the only runnable validation the repo supports.

---

## 12. Scope boundaries for the future session

**In scope:** creating `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`,
`docs/ROADMAP.md`, `docs/TESTING.md`, grounded in tracked repository files plus
`.kilo/plans/1790903055742-mob-variants-brs-architecture.md`.

**Out of scope:** implementing any part of the variant system; deleting or editing
the example mixins; editing `fabric.mod.json`; editing `README.md`; changing
Gradle, dependencies, mappings, or Java version; adding tests; adding an
`AGENTS.md`; creating other docs; refactoring, renaming, or cleanup of any kind;
inventing APIs, classes, decisions, or requirements; promoting speculative ideas
to roadmap items; recommending dependency or compatibility changes; recommending
architectural optimizations.

**Reporting rule:** any unrelated problem found while writing (e.g. the stale
`build/` output, the placeholder metadata) is recorded in the docs' "Known
inconsistencies" section or reported back to the user — never fixed in the
repository.

---

## 13. Findings that must be preserved in the future documentation

1. The mod is an unmodified Fabric scaffold; **no user-facing feature exists**.
2. Official/mojmap mappings are in effect with no declared `mappings` dependency.
3. Java 25, Gradle 9.7.1, MC 26.1.2, Loader 0.19.5, Fabric API 0.155.3+26.1.2,
   Loom 1.18-SNAPSHOT — all from `gradle.properties` / `fabric.mod.json` / `build.gradle`.
4. Three entrypoints exist; two registered mixins are inert example scaffolding.
5. There are **no** registries, events, attachments, persistence, networking,
   configuration, or tests in the repository.
6. The only design artifact is `.kilo/plans/1790903055742-mob-variants-brs-architecture.md`,
   and its own status is "proposed, not implemented".
7. Its target architecture is: one `Identifier` attachment on `Entity`, a
   datapack-driven definition loader, an immutable registry snapshot, a one-method
   `VariantSelector` seam, zero server mixins, and exactly one client mixin.
8. Under that proposal, server state is authoritative and no custom packets exist;
   persistence, chunk-load sync, and client sync would all come from Fabric's
   attachment API.
9. The proposal is event-driven with zero per-tick work.
10. The proposal's renderer mixin target is **unverified for 26.1.2** and must be
    labelled as such.
11. `copyOnDeath()` is deliberately unset; unknown ids degrade to vanilla.
12. `build/resources/.../zombie/ice.json` is stale output whose schema contradicts
    the proposal — report, never treat as design.
13. CI validation is `./gradlew build` on JDK 25; there is no automated test suite.