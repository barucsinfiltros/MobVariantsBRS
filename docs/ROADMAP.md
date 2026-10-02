# MobVariantsBRS — Roadmap

**Status legend**

- **IMPLEMENTED** — present in a tracked file in this repository today.
- **EXPLICITLY PLANNED** — named as future work by
  `.kilo/plans/1790903055742-mob-variants-brs-architecture.md` (cited as *plan §N*).
- **PENDING / UNRESOLVED** — raised as an open question by the plan, or unanswered by
  the repository.
- **DEFERRED EXTENSION SEAM** — a seam the plan says exists *in its own design*, with
  no commitment to use it. Not scheduled work.
- **NON-GOAL** — explicitly out of scope per the plan.

Repository basis: commit `230ec45`. No dates, effort estimates, release numbers, or
milestones appear in this document; the plan defines none, and none are invented here.

---

## 1. Implemented

**IMPLEMENTED.** The repository is an unmodified Fabric example-mod scaffold. **No
user-facing mod feature exists.**

| Item | Evidence |
| --- | --- |
| Gradle/Loom build with split `main` / `client` source sets | `build.gradle:14-23` |
| Version pin set (MC `26.1.2`, Loader `0.19.5`, Fabric API `0.155.3+26.1.2`, Loom `1.18-SNAPSHOT`, Java `25`, Gradle `9.7.1`) | `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`, `fabric.mod.json:36-38` |
| Official/mojmap mappings via the Loom default, no declared `mappings` dependency | `build.gradle:31-38` |
| Three declared entrypoints (`main`, `client`, `fabric-datagen`) | `fabric.mod.json:17-27` |
| `MobVariantsBRS` logging `"Hello Fabric world!"` | `MobVariantsBRS.java:19-25` |
| `MobVariantsBRS.id(String)` identifier helper | `MobVariantsBRS.java:27-29` |
| Two mixin configs, client/server split | `fabric.mod.json:28-34` |
| Two **inert** example mixins registered with `defaultRequire: 1` | `ExampleMixin.java`, `ExampleClientMixin.java` |
| Icon asset `assets/mob_variants_brs/icon.png` | `src/main/resources/assets/mob_variants_brs/icon.png` |
| Client datagen wiring with no providers registered | `build.gradle:25-29`, `MobVariantsBRSDataGenerator.java` |
| CI: `./gradlew build` on JDK 25 for every push and pull request | `.github/workflows/build.yml` |
| Architectural documentation baseline under `docs/` | `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/ROADMAP.md`, `docs/TESTING.md` |

Nothing in this table is part of a mob-variant system. No registry, event listener,
attachment, persistence, networking, configuration, or test exists.

---

## 2. Explicitly Planned

All items below are `EXPLICITLY PLANNED` by the approved plan and are **not
implemented**. Technical detail lives in `docs/ARCHITECTURE.md` §15 and
`docs/DECISIONS.md`; this section tracks status only.

### 2.1 Phase 0 — verification gate (blocking)

**EXPLICITLY PLANNED** — plan §11 item 1. The plan states that task 2 must not start
before task 1 reports verified signatures.

- Run `genSources` and confirm: the exact vanilla renderer texture-lookup method and
  receiver for 26.1.2; `Identifier`'s `Codec` and `StreamCodec` constant names (or the
  `Codec.STRING`-based local fallback); how
  `PreparableReloadListener.SharedState` exposes the resource manager; and whether
  `ResourceLoader.registerReloadListener` registers on the client as well as the server.
- Record each confirmed signature in a comment where it is used.

### 2.2 Scaffold cleanup

**EXPLICITLY PLANNED** — plan §0.1 and §11 item 2.

- Delete `src/main/java/com/baruc/brs/mobvariants/mixin/ExampleMixin.java`.
- Delete `src/client/java/com/baruc/brs/mobvariants/client/mixin/ExampleClientMixin.java`.
- Empty the `mixins` array in `src/main/resources/mob_variants_brs.mixins.json`, or
  remove the config and drop its `fabric.mod.json` reference.
- Replace the `fabric.mod.json` template placeholders: `description`
  ("This is an example description!…"), `authors` (`["Me!"]`), `contact.homepage`
  (`https://fabricmc.net/`), and `contact.sources`
  (`https://github.com/FabricMC/fabric-example-mod`).

### 2.3 Phase 1 — core layer

**EXPLICITLY PLANNED** — plan §11 items 3-8.

1. `VariantDefinition` — immutable record plus `Codec` and the identifier codec
   (item 3).
2. `VariantAttachments` — `AttachmentRegistry.create(id, b -> b.persistent(...).syncWith(...))`,
   with **no** `initializer()` (item 4).
3. `VariantDefinitionLoader` — a `SimpleReloadListener` that parses in `prepare` and
   builds the maps in `apply` (item 5).
4. `VariantRegistry` — a `volatile` immutable snapshot with `get(Identifier)` and
   `variantsFor(EntityType)` (item 6).
5. `VariantSelector` plus `FirstDefinitionSelector` (item 7).
6. `MobVariantsBRS` wiring, including the `ENTITY_LOAD` listener whose first statement
   is the `isLoadedFromDisk()` guard (item 8).

### 2.4 Phase 2 — content and client

**EXPLICITLY PLANNED** — plan §11 items 9-11.

1. `src/main/resources/data/mob_variants_brs/variants/zombie/ice.json` plus the
   texture asset (item 9).
2. Client loader registration, then `MobRendererMixin` (item 10).
3. `ResourceLoader.addListenerOrdering` so the variant listener runs before the texture
   listener (item 11).

---

## 3. Pending / Unresolved

Each item is a question the plan raises and does not settle, or a repository fact the
plan depends on. None is a commitment.

| # | Open item | Source |
| --- | --- | --- |
| P-1 | The 26.1.2 renderer texture-lookup method is explicitly **unverified**; the 1.21.x name may have moved in the 26.x render rework. | plan §6.2, §10 risk 1 |
| P-2 | `Identifier.CODEC` and its matching `StreamCodec` constant names are unconfirmed; a `Codec.STRING`-based local derivation is the prepared fallback. The plan says not to guess the constant name. | plan §5.2, §10 risk 2 |
| P-3 | `PreparableReloadListener.SharedState.resourceManager()` — how the variant JSON is actually read. | plan §5.2 |
| P-4 | Whether `ResourceLoader.registerReloadListener` covers the client as well as the server; the client entrypoint exists to host a second registration if not. | plan §5.2, §10 risk 3 |
| P-5 | `EntitySpawnReason` type exists, but its constant names are unconfirmed. | plan §5.2 |
| P-6 | Mixin failure mode: the plan *recommends* `require = 0` plus a one-time warning but explicitly allows a loud failure instead. | plan §6.2, D-017 in `DECISIONS.md` |
| P-7 | The exact shape for applying variant attributes to a live entity's `AttributeMap`, given that `FabricDefaultAttributeRegistry.MODIFY` is per-`EntityType` only. | plan §9.3 |
| P-8 | Server-only datapack asymmetry: a variant the client does not have renders vanilla. The plan treats this as inherent to the mod-datapack model and wants an explicit startup log rather than an assumption. | plan §10 risks 3 and 4 |
| P-9 | Whether Fabric's per-entity attachment storage allocates eagerly (a small per-entity cost). The plan marks this as unavoidable and shared by all attachment-API users. | plan §3.6, §10 risk 5 |
| P-10 | The resolved Loom build is not pinned: `loom_version=1.18-SNAPSHOT`. A local build during this documentation task reported `Fabric Loom: 1.18.2`, but the repository records only the snapshot coordinate. | `gradle.properties:10` |
| P-11 | Ownership boundaries for the reload listener, the client-vs-common loader registration, and the selection trigger are described in the plan but have no assigned module in the current repository. | plan §2, §5.2 |
| P-12 | Whether the two example mixin injection points (`MinecraftServer.loadLevel`, `Minecraft.run`) resolve on 26.1.2 is not verifiable from tracked files; the untracked `run/logs/latest.log` is positive local evidence that they currently do. | `ExampleMixin.java:11`, `ExampleClientMixin.java:11` |

---

## 4. Deferred Extension Seams

**DEFERRED EXTENSION SEAM.** Each is a seam the plan says its *own* design provides.
None exists in the current repository, and none is scheduled work.

| Seam | What the plan says it enables | Source |
| --- | --- | --- |
| Multiple mob types, multiple variants each | Free: the path scheme encodes both dimensions and lookup is a map get; no code change to add the hundredth mob type. | plan §9.1 |
| Additional definition fields | Add `Optional` fields to the record and `Optional` branches to the codec; a v1 file keeps parsing. | plan §9.5 |
| Alternative selection strategies | UUID-hash, biome-weighted, spawn-reason, tag-gated, and command-forced selectors are all additive to the one-method interface. | plan §9.6 |
| Variant attributes | Computed once at spawn in the same `ENTITY_LOAD` listener, applied to the live `AttributeMap`; no new mixin, no tick listener, no new state. | plan §9.3 |
| Variant behaviours | Handled inline in the same `ENTITY_LOAD` listener, driven by the already-assigned variant id. The plan notes there is **no** Fabric event for "spawned naturally". | plan §9.4 |
| Variant survival across conversion | One `copyOnDeath()` builder call, deliberately unset in v1. | plan §10 risk 8 |
| Spawn-reason-driven rules | `spawnReason()` is available through `EntityLoadData` and is recorded as the correct future input, deliberately unused in v1. | plan §2.1 |

---

## 5. Explicit Non-Goals

**NON-GOAL** — plan header scope note and §13. These are excluded from the core layer
by the plan itself.

- Health, damage, armour, and scaling formulas.
- Stat multipliers and balancing.
- Probability weighting.
- Commands.
- GUI.
- Tag-driven opt-in.
- Any variant behaviour that runs on a tick.

The plan defines only the seams that would accept this work later.

---

## 6. Known repository inconsistencies (reported, not fixed)

These are observations, not roadmap items. None was modified while writing these docs.

1. **Stale build output (now removed as a side effect of running the existing build).**
   `build/resources/main/data/mob_variants_brs/variants/zombie/ice.json` was present on
   disk with contents `{"base_entity": "minecraft:zombie", "texture": "mob_variants_brs:zombie/ice"}`,
   had no source under `src/`, and was git-ignored (`.gitignore:4`). It was residue from a
   deleted experiment and is **not** evidence of implemented architecture. Its schema
   also contradicts the plan's proposed `VariantDefinition`, which derives the entity
   type from the path and treats `texture` as optional (plan §8.1). The
   `./gradlew build` run during the documentation task deleted it as stale Gradle
   output; no tracked file was involved and no configuration was changed.
2. **`README.md` is the unmodified Fabric template** and carries no project
   information.
3. **`fabric.mod.json` still advertises the mod as an example** — placeholder
   description, `authors: ["Me!"]`, and `fabricmc.net` / `fabric-example-mod` contact
   URLs.
4. **`fabric.mod.json` `license` and `LICENSE` are the template's** (CC0).
5. **Inert mixins are still registered with `defaultRequire: 1`**, so a rename of an
   injected method would hard-fail startup. This contradicts plan §6.1 ("no server-side
   mixin required") and §0.1 ("delete them").
6. **The datagen entrypoint lives in the `client` source set.** The plan calls this
   acceptable but flags it (plan §10 risk 10).
7. **Mappings are official with no declared `mappings` dependency**, which the build
   file expresses only by omission (plan §5.3).
8. **There is no test suite and no validation evidence** beyond CI running
   `./gradlew build`. Nothing in the repository demonstrates runtime behaviour beyond
   the single `"Hello Fabric world!"` log line recorded in the untracked
   `run/logs/latest.log`.
9. **No decision history.** A single commit (`230ec45`) and one untracked plan file are
   the only record; the template-inherited choices have no project rationale.

---

## 7. Speculative ideas

**None.** No item in this roadmap originates with the documentation task. Every entry
above cites either a tracked repository file or a section of the approved plan.
