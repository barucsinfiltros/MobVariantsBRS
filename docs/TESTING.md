# MobVariantsBRS — Testing and Validation

**Status tokens used in every table below:**

- `EXISTING` — validation that exists in this repository today.
- `PROPOSED` — validation the approved plan says should be performed; not implemented.
- `N/A — NOT IMPLEMENTED` — a validation that could exist only once a component exists,
  and that component does not exist.

Repository basis: commit `230ec45`. Plan reference:
`.kilo/plans/1790903055742-mob-variants-brs-architecture.md`.

---

## 1. Current state

**The repository has zero automated tests.**

- There is no `src/test` source set (`git ls-files` lists no test files).
- There is no test framework dependency: `build.gradle:31-38` declares only
  `minecraft`, `fabric-loader`, and `fabric-api`.
- There is no `test` or `check` configuration block anywhere in `build.gradle`.

A Gradle `build` therefore performs compilation, resource processing, and jar/sources-jar
assembly only. It runs no tests, because none exist.

## 2. Validation that already exists

| Validation | Status | Evidence / notes |
| --- | --- | --- |
| `./gradlew build` as the single local compile-level check | `EXISTING` | Verified present in `build.gradle` via the applied plugins; no explicit task is declared. |
| GitHub Actions `build` workflow on every `[pull_request, push]` | `EXISTING` | `.github/workflows/build.yml:7` |
| Runner `ubuntu-24.04` | `EXISTING` | `.github/workflows/build.yml:11` |
| Gradle wrapper validation (`gradle/actions/wrapper-validation@v6`) | `EXISTING` | `.github/workflows/build.yml:15-16` |
| JDK 25, `microsoft` distribution | `EXISTING` | `.github/workflows/build.yml:18-21` |
| `./gradlew build` executed in CI | `EXISTING` | `.github/workflows/build.yml:24-25`. With no test sources this compiles only. |
| Artifact upload of `build/libs/` | `EXISTING` | `.github/workflows/build.yml:26-30` |
| Automatic tests of any kind | `N/A — NOT IMPLEMENTED` | No test source set, no test dependency |
| Static analysis / lint / formatting task | `N/A — NOT IMPLEMENTED` | None configured in `build.gradle` or CI |
| Datagen validation | `N/A — NOT IMPLEMENTED` | Datagen is wired (`build.gradle:25-29`) but `MobVariantsBRSDataGenerator` registers no providers, so a datagen run is a no-op |

**Commands executed while writing this document.** `./gradlew build` (Windows:
`.\gradlew.bat build`) was executed once during this documentation task against
commit `230ec45` and reported `BUILD SUCCESSFUL`, with `test` and `processTestResources`
both `NO-SOURCE`. No other command in this document is claimed as executed. Every
other command listed below as `PROPOSED` or `N/A` has **not** been run, because the
component it validates does not exist.

## 3. Compile-time verification role

`PROPOSED`. The plan states that `gradlew build` is the primary verification for every
API item it marks as *inferred* — `Identifier.CODEC` and its `StreamCodec`,
`PreparableReloadListener.SharedState` resource access, the renderer method signature,
and the `ResourceLoader` client-registration question (plan §12, §5.2).

Today that role is theoretical: there is no code depending on those APIs, so the
current build cannot confirm or refute any of them.

## 4. Planned validation procedures

All of the following are `PROPOSED`, from plan §12. None has been executed.

| # | Procedure | Status | What it would prove |
| --- | --- | --- | --- |
| 1 | `./gradlew build` | `PROPOSED` (command exists; run once during this documentation task on the scaffold) | Compiles, and confirms every inferred API item in plan §5.2 once code depends on them |
| 2 | `./gradlew runServer`; spawn a zombie; `/data get entity` shows the attachment in NBT; unload and reload the chunk; the id is unchanged | `PROPOSED` | The `isLoadedFromDisk()` guard prevents re-selection on disk load |
| 3 | `./gradlew runClient`; the same zombie renders with the variant texture | `PROPOSED` | The single client mixin applies to the correct 26.1.2 method |
| 4 | Edit the variant JSON and run `/reload` | `PROPOSED` | The change takes effect with no respawn |
| 5 | Rename a variant JSON and run `/reload` | `PROPOSED` | The mob renders vanilla without crashing (unknown-id fallback) |
| 6 | Negative test: confirm no listener of ours is invoked during steady-state ticking (temporary `LOGGER` counter, removed afterwards, or a profiler run) | `PROPOSED` | The zero-per-tick claim in plan §3.3 |

## 5. Functional coverage matrix

Every row is currently **not executed**. Status describes whether the validation exists
today, not whether the underlying feature does.

| Area | Scenario | Expected result | Status |
| --- | --- | --- | --- |
| Build | `./gradlew build` succeeds | Compiles and packages | `EXISTING` (executed once during this documentation task: `BUILD SUCCESSFUL`) |
| Build | Automated unit tests run | No tests to run | `N/A — NOT IMPLEMENTED` |
| Build | Lint / static analysis | No task configured | `N/A — NOT IMPLEMENTED` |
| Dedicated server | Zombie spawns with a variant assigned | Variant id present in NBT | `N/A — NOT IMPLEMENTED` (no selection code exists) |
| Dedicated server | Entity loaded from disk keeps its variant | Id unchanged after chunk unload/reload | `N/A — NOT IMPLEMENTED` |
| Dedicated server | Full server restart preserves the variant | Id survives save/load | `N/A — NOT IMPLEMENTED` |
| Client rendering | Zombie renders with the variant texture | Texture differs from vanilla | `N/A — NOT IMPLEMENTED` (no client variant mixin exists) |
| Client rendering | Broken mixin degrades instead of crashing | Vanilla texture, one warning | `N/A — NOT IMPLEMENTED` |
| Client/server sync | Attachment reaches the client | Client shows the same variant | `N/A — NOT IMPLEMENTED` (no networking or attachment sync exists) |
| Multiplayer | Two clients see the same variant | Identical on both clients | `N/A — NOT IMPLEMENTED` |
| Multiplayer | Player joins/rejoins mid-session | Existing mobs unchanged | `N/A — NOT IMPLEMENTED` |
| Reload | `/reload` after editing the JSON | Change applies with no respawn | `N/A — NOT IMPLEMENTED` |
| Reload | Listener ordering: variant listener before texture listener | Correct precedence | `N/A — NOT IMPLEMENTED` |
| Reload | Rename a variant JSON then `/reload` | Vanilla render, no crash | `N/A — NOT IMPLEMENTED` |
| Persistence | Chunk unload/load | Variant preserved | `N/A — NOT IMPLEMENTED` |
| Persistence | Entity conversion (zombie→drowned) with `copyOnDeath()` unset | Variant not carried over | `N/A — NOT IMPLEMENTED` |
| Fallback | Unknown variant id held by a live entity | Renders vanilla, debug-level log | `N/A — NOT IMPLEMENTED` |
| Asymmetry | Server-only datapack adds a variant the client lacks | Client renders vanilla | `N/A — NOT IMPLEMENTED` |
| Performance | No listener invoked during steady-state ticking | Zero per-tick work | `N/A — NOT IMPLEMENTED` |
| Datagen | Datagen run produces content | No-op; no providers registered | `N/A — NOT IMPLEMENTED` |
| Configuration | Configuration system behaves correctly | — | `N/A — NOT IMPLEMENTED` — **no configuration system exists in this repository** |
| Scaffold mixins | `ExampleMixin` / `ExampleClientMixin` still apply on 26.1.2 | Startup succeeds | `EXISTING` — indirect evidence only: the untracked, git-ignored `run/logs/latest.log` records a successful load. Not a test, and not repository-tracked evidence. |

## 6. Dedicated-server and multiplayer specifics

`PROPOSED` (plan §2, §10 risk 4). The proposed design makes the server authoritative for
the variant id and relies on Fabric's attachment API for propagation, so the checks that
matter are:

- the attachment is present in server NBT and survives a save/load cycle;
- chunk load/unload does not re-select;
- every client in a session observes the same variant for a given mob;
- a server-only datapack does not break the client — it degrades to the vanilla texture
  (plan §10 risk 4), which the plan wants logged explicitly at startup rather than
  assumed.

None of these can be executed today: there is no state, no persistence, and no sync in
the repository.

## 7. Client-side specifics

`PROPOSED` (plan §6.2). Under the proposed design the single client mixin is the *only*
client-side behaviour. Two consequences for validation:

- Because the plan recommends `require = 0` plus a one-time warning, a broken mixin
  degrades silently. A client test must therefore assert that the texture **changed**,
  not merely that the game launched.
- Because the 26.1.2 target method is explicitly unverified by the plan, the mixin
  cannot be validated until the Phase 0 verification gate (plan §11 item 1) resolves it.

No client variant rendering exists today.

## 8. Regression validation

| Mechanism | Status | Notes |
| --- | --- | --- |
| CI `./gradlew build` on every push and pull request | `EXISTING` | `.github/workflows/build.yml:7,24-25`. Compilation-only, because no tests exist. |
| Gradle wrapper validation in CI | `EXISTING` | `.github/workflows/build.yml:15-16` |
| Manual regression checklist | `N/A — NOT IMPLEMENTED` | To be defined at implementation time (plan §12 lists only the manual procedures above) |

## 9. Known validation gaps

1. **No automated coverage of anything.** No test source set, no test dependency, no
   lint or static-analysis task.
2. **Parsing is unvalidated.** No code reads variant JSON, and no test asserts how such
   a file would be decoded or how a malformed file would be handled.
3. **Reload behaviour is unvalidated.** No reload listener exists, so listener ordering
   and the atomic snapshot swap are untested.
4. **Persistence is unvalidated.** The mod writes no NBT and holds no state.
5. **Rendering is unvalidated.** No variant rendering code exists; the two registered
   mixins are inert.
6. **Multiplayer checks are manual-only** — and currently impossible, since there is
   nothing to check.
7. **CI is the only automated gate**, and it reduces to "does it compile".
8. **Runtime evidence is anecdotal.** The only runtime trace is the untracked
   `run/logs/latest.log` line `Hello Fabric world!`, which is local state rather than a
   repository fact.

## 10. Test matrix for the future test suite

`PROPOSED` — a planning note only; **no test suite exists and none is scheduled here.**
The rows restate the coverage the approved plan's validation section (plan §12) implies,
organised by layer, so that a future implementation task can decide what to automate.

| Layer | Candidate check | Current status |
| --- | --- | --- |
| Definition loading | Well-formed variant JSON decodes to a definition; id and entity type derive from the path | `N/A — NOT IMPLEMENTED` |
| Definition loading | Empty `{}` body is a valid variant (plan §8.2) | `N/A — NOT IMPLEMENTED` |
| Definition loading | Malformed JSON is rejected without breaking `/reload` | `N/A — NOT IMPLEMENTED` |
| Registry | `get(Identifier)` and `variantsFor(EntityType)` return the right entries | `N/A — NOT IMPLEMENTED` |
| Registry | Reload publishes a new snapshot atomically; old snapshot stays live until then (plan §4.3) | `N/A — NOT IMPLEMENTED` |
| Selection | `isLoadedFromDisk()` guard skips selection on NBT load (plan §2.1) | `N/A — NOT IMPLEMENTED` |
| Selection | `FirstDefinitionSelector` returns the first candidate (plan §8.3) | `N/A — NOT IMPLEMENTED` |
| Attachment | Attachment id survives save/load; no initializer means an unvarianted entity allocates nothing (plan §3.5) | `N/A — NOT IMPLEMENTED` |
| Attachment | `copyOnDeath()` unset → variant not carried across conversion (plan §10 risk 8) | `N/A — NOT IMPLEMENTED` |
| Client mixin | Variant texture is returned; non-variant entities fall through to vanilla (plan §6.2) | `N/A — NOT IMPLEMENTED` |
| Client mixin | Injection still applies after a Minecraft update; warning path exercised (plan §6.2) | `N/A — NOT IMPLEMENTED` |
| Integration | Dedicated server + client session, two players (plan §12) | `N/A — NOT IMPLEMENTED` |
| Performance | No listener invoked during steady-state ticking (plan §3.3) | `N/A — NOT IMPLEMENTED` |

Fabric API surfaces used by a future suite would have to be available from
`build.gradle:31-38`, which currently declares only Minecraft, Fabric Loader, and Fabric
API. Adding any test framework would be a dependency change and is outside the scope of
this documentation task.

---

## 11. Runtime integration testing procedure

Local server + client testing requires the client to auto-connect to the dedicated
server. The launch arguments must match the target Minecraft version.

### 11.1 The argument incompatibility (Minecraft 26.1.2)

Minecraft removed the `--server` and `--port` launch arguments in snapshot **23w14a
(1.20)**. They were replaced by the Quick Play argument family:

| Obsolete (pre-1.20) | Replacement (1.20+) | Format |
|---|---|---|
| `--server localhost` | `--quickPlayMultiplayer localhost:25565` | `host:port` as a single value |
| `--port 25565` | *(merged into the above)* | |
| *(none)* | `--quickPlayPath quickPlay/log.json` | optional: logs the join |

Passing the obsolete arguments to Minecraft 26.1.2 produces:
```
Completely ignored arguments: [--server, localhost, --port, 25565]
```
and the client starts in the main menu without connecting — the server sees no
player-join event.

### 11.2 Launching the client with auto-connect

The `build.gradle` `loom.runs.client` block accepts an `autoConnect` Gradle property
that injects `--quickPlayMultiplayer` (the correct argument for 26.1.2):

```powershell
# Start the server in one terminal:
.\gradlew runServer

# Then, in another terminal, launch the client connected to it:
.\gradlew runClient -PautoConnect=localhost:25565
```

The server port is `25565` (set in `run/server.properties`, `server-port=25565`).
RCON on port `25575` is available for issuing test commands without joining the game
(`rcon_test.py` / `rcon_test.ps1`).

### 11.3 Test session script

`scripts/Run-TestSession.ps1` automates the above: it starts the server (optionally),
waits for the `Done ...` readiness line in the log, then launches the client with
`-PautoConnect`. Usage:

```powershell
.\scripts\Run-TestSession.ps1            # start server, wait, launch connecting client
.\scripts\Run-TestSession.ps1 -NoClient   # start server only
.\scripts\Run-TestSession.ps1 -NoServer   # launch client only (server already running)
```
