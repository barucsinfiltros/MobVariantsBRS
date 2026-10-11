# Review and verify optional variant attributes

**Session ID:** ses_ed8db7d03ffe5ViePzuT1xIvJ5
**Created:** 10/10/2026, 2:47:46 PM
**Updated:** 10/10/2026, 3:32:03 PM

---

## User

# MobVariantsBRS — Review and Verify Optional Variant Attributes

## ROLE

Act as a senior Java and Fabric mod developer. Review the existing optional variant attribute implementation, correct the unreachable example variant, and produce executable evidence for codec and validation behavior.

## OBJECTIVE

Complete a focused review and verification of the implementation from session S-0002.

Preserve the existing architecture and working-tree changes. Fix only demonstrated problems within this scope. Do not expand the feature, redesign existing systems, or claim verification without supporting evidence.

## CONTEXT

- Repository: `barucsinfiltros/MobVariantsBRS`
- Branch: `feat/variant-state-layer`
- Minecraft: 26.1.2
- Fabric Loader: 0.19.5
- Fabric API: 0.155.3+26.1.2
- Java: 25
- Gradle: 9.7.1
- Previous implementation report: `.kilo/sesions/S-0002_variant-attributes-implementation_session-ses_ed90.md`

The existing implementation introduces optional JSON-defined absolute base attribute values for entity variants.

## 1. INSPECT THE CURRENT STATE

Before modifying files:

1. Confirm the current branch and run `git status --short --branch`.
2. Read the previous implementation report.
3. Inspect the current diffs and relevant surrounding code for:
   - `VariantAttributes.java`
   - `VariantSnapshot.java`
   - `VariantStateLifecycle.java`
   - `VariantDefinition.java`
   - `strong_zombie.json`
4. Locate `ice_zombie.json`, the variant-selection ordering, and the relevant codec and validation code.
5. Inspect the existing test infrastructure, Gradle tasks, and available Minecraft runtime or test initialization mechanisms.

Preserve every pre-existing modification and untracked file. Do not overwrite user changes, reset the working tree, or use destructive Git commands.

Do not repeat completed API research unless a specific unresolved question requires it.

## 2. FIX THE UNREACHABLE EXAMPLE VARIANT

Inspect the selection algorithm and both variant definitions to verify the reported conflict:

- `strong_zombie.json` uses `minecraft:snowy_plains`.
- `ice_zombie.json` already matches that condition.
- Candidates are processed in ascending variant-ID order.
- Selection returns after the first matching candidate.

If these conditions are confirmed, correct the example by removing `strong_zombie.json` if it has no intentional purpose.

Do not silently change gameplay behavior to preserve the example. Do not alter the existing ice-zombie selection conditions, texture, or gameplay solely to accommodate `strong_zombie`.

If repository evidence establishes that `strong_zombie` has an intentional purpose, identify the conflict and make only a minimal, explicitly justified correction that preserves existing intended behavior. Do not invent new gameplay requirements.

## 3. VERIFY THE ATTRIBUTE CODEC

Implement the smallest reliable executable checks compatible with the existing project. Inspect available test infrastructure before choosing the approach.

Verify the actual codec behavior for these cases:

1. A variant definition containing the intended top-level JSON `attributes` object decodes successfully.
2. Encoding produces the intended top-level `attributes` object without an accidental nested `attributes` object.
3. A definition omitting `attributes` remains valid and receives the intended default behavior.
4. Existing fields retain their expected values through decoding and, where appropriate, an encode/decode round trip.

Use representative JSON fixtures based on the real definition schema.

Exercise the actual production codec rather than duplicating its logic in a test. Assert results explicitly; do not treat successful test initialization alone as proof of correctness.

Do not introduce new dependencies unless existing project facilities cannot provide reliable verification and the dependency is demonstrably necessary.

## 4. VERIFY ATTRIBUTE VALIDATION

Provide executable checks or appropriate automated tests for:

- Unknown attribute identifiers.
- `NaN`.
- Positive infinity.
- Negative infinity.
- Valid finite values within supported attribute bounds.
- Values below and above supported bounds.
- Rejection of invalid definitions before they can participate in variant selection.

Use the actual Minecraft 26.1.2 attribute registry and attribute implementation wherever registry resolution or supported bounds are involved.

Do not substitute mocks or fabricated registries for real registry-resolution checks. If real registry initialization is required, use an available project-compatible test or runtime initialization path.

For each case, verify the actual outcome with assertions or captured execution results.

Distinguish clearly between:
- JSON decoding;
- definition validation;
- registry resolution;
- selection eligibility;
- attribute application.

A failure at one stage must not be presented as proof that a different stage works correctly.

If a check cannot be executed economically or reliably, document the precise blocker and mark that behavior UNVERIFIED. Do not weaken assertions or silently skip failed cases.

## 5. REVIEW IMPLEMENTATION CORRECTNESS

Inspect the production implementation and verify the following against actual code:

### Attribute semantics
- Configured values represent absolute base attribute values, not additive modifiers.
- Each configured value is applied at most once per selection operation.
- Attribute lookup and application use the appropriate Minecraft 26.1.2 APIs.
- Supported bounds and finite-value checks are enforced consistently.

### Selection and lifecycle
- Attribute application occurs only in the existing server-side selection path.
- Invalid definitions are rejected before selection.
- Attribute application failures cannot prevent texture assignment.
- Existing selection guards, persistence, synchronization, and rendering behavior remain unchanged.

### Scope and maintainability
- No unrelated behavior has changed.
- No unnecessary dependencies, test frameworks, runtime infrastructure, or duplicated validation logic have been introduced.
- New tests reuse the production codec and validation paths wherever practical.

Use source inspection to establish control flow and executable checks to establish runtime behavior. Do not infer runtime correctness from code appearance alone.

## 6. IMPLEMENTATION CONSTRAINTS

- Preserve the existing variant-selection architecture.
- Do not redesign the attribute system.
- Do not modify unrelated source files or assets.
- Do not alter existing ice-zombie gameplay or texture conditions to accommodate `strong_zombie`.
- Preserve all existing user changes and untracked files.
- Do not commit or push.
- Do not run destructive Git commands.
- Avoid repeated exploratory loops: once the relevant implementation, API, and test path are understood, execute the checks and resolve concrete failures.

Make only changes necessary to fix the confirmed example conflict, add justified focused checks, and correct verified defects.

## 7. VALIDATION AND BUILD VERIFICATION

Execute the following in order.

### A. Focused checks

Run all newly added or existing relevant codec and validation tests.

Capture:
- Exact commands.
- Test names or individual check names.
- Assertions executed.
- Actual pass/fail results.
- Any skipped or unverified cases and their reasons.

If tests require Minecraft registry initialization, confirm that the actual registry was initialized and used.

### B. Fresh compilation

Run a fresh compilation of the changed production sources using the project's supported Gradle tasks.

Do not rely solely on an `UP-TO-DATE` result. Use an appropriate non-destructive Gradle option, such as `--rerun-tasks` on the relevant compilation task, when necessary to demonstrate that compilation actually ran.

Then run the project's normal build:

`./gradlew build`

On Windows, use the equivalent wrapper command supported by the environment.

Capture the exact commands and final outcomes. Do not claim success if a task fails or does not execute.

### C. Final inspection

Run:
- `git diff --check`
- `git diff --stat`
- `git diff`
- `git status --short --branch`

Review the final diff for unintended modifications, accidental deletions, generated artifacts, and changes outside the task scope.

Confirm that all pre-existing user changes and untracked files remain preserved. Distinguish pre-existing changes from changes introduced during this task using the initial and final repository state.

Do not discard unrelated changes to make the working tree clean.

## 8. ACCEPTANCE CRITERIA

The task is complete only when:

1. The `strong_zombie` selection conflict has been verified and addressed with the smallest justified change.
2. Codec checks provide executable evidence for top-level decoding, encoding structure, optional-field compatibility, and preservation of existing fields.
3. Validation checks provide executable evidence for unknown identifiers, non-finite values, supported bounds, and rejection before selection, to the extent supported by the available test environment.
4. The implementation review confirms the existing architecture and required failure isolation, or reports specific defects that remain unresolved.
5. Changed production sources have undergone a fresh compilation attempt.
6. The normal project build has been executed and its actual result recorded.
7. The final diff and Git status have been inspected.
8. Every unverified behavior is explicitly identified.

Do not declare the feature fully verified if any required behavior remains unverified or if the evidence does not support the claim.

## 9. REQUIRED REPORT

Provide a concise, evidence-based report containing:

1. **Initial repository state:** branch and initial working-tree status.
2. **Files changed:** each file and the reason for its modification.
3. **Example variant:** whether `strong_zombie.json` was removed or corrected, with the confirmed selection-order rationale.
4. **Codec verification:** exact commands, individual cases, assertions, and results.
5. **Validation verification:** exact commands, individual cases, real-registry usage, and results.
6. **Implementation review:** findings for absolute base values, server-side application, once-per-selection behavior, failure isolation, and preservation of existing selection, persistence, synchronization, and rendering behavior.
7. **Build verification:** exact compilation and build commands, whether compilation actually ran, and their outcomes.
8. **Unverified behavior:** each remaining gap and its concrete reason.
9. **Final repository state:** branch, final working-tree status, and confirmation that unrelated pre-existing changes were preserved.

Separate confirmed facts from assumptions. Do not report checks as passed unless their execution and results provide direct evidence.

---

## Assistant

I'll start by inspecting the current repository state and understanding the task scope.

**Tool: bash**

**Tool: bash**

**Tool: glob**

---

## Assistant

**Tool: glob**

**Tool: glob**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: read**

---

## Assistant

Now let me read the current implementation files and relevant resources.

**Tool: read**

**Tool: read**

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: bash**

**Tool: glob**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

Now let me examine the build configuration and test infrastructure.


**Tool: read**

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

The previous session's verification was only a clean build — no executable codec/validation checks exist. Let me check the extracted Minecraft sources and the runtime classpath setup to plan executable verification.


**Tool: bash**

**Tool: bash**

**Tool: bash**

---

## Assistant

The extracted Minecraft sources are available. Let me locate the key classes for registry bootstrap and attribute APIs

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

The bootstrap path is clear: `Bootstrap.bootStrap()` populates the real registries in a plain JVM. Let me check the remaining infrastructure and attachments

**Tool: read**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: bash**

**Tool: bash**

---

## Assistant

The plan is forming. The key technical question is how to build a real biome registry in a plain JVM for `VariantSnapshot.build`. Let me inspect `RegistrySetBuilder` and the worldgen bootstrap.



**Tool: read**

**Tool: bash**

---

## Assistant

**Tool: bash**

**Tool: grep**

---

## Assistant

**Tool: read**

---

## Assistant

`VanillaRegistries.createLookup()` is the standard plain-JVM path for a real biome registry. Now let me check `RegistryAccess` to confirm the exact type that `lookupOrThrow(Registries.BIOME)` returns, since `VariantSnapshot.build` takes `Registry<Biome>`.




**Tool: read**

---

## Assistant

Now let me verify the remaining APIs: `MappedRegistry` registration, `Identifier` factories, `Registry.getOptional`, and `Attributes`.

**Tool: read**

**Tool: bash**

**Tool: bash**

**Tool: bash**

---

## Assistant

Let me check the full `Attributes` class to enumerate real attributes and bounds, and the `ATTACK_DAMAGE` definition

**Tool: read**

---

## Assistant

All vanilla attributes are `RangedAttribute` with explicit bounds (`max_health` [1.0, 1024.0], `attack_damage` [0.0, 2048.0], `movement_speed` [0.0, 1024.0]). Let me check the texture assets and Java environment, then set up my work plan


**Tool: bash**

**Tool: bash**

---

## Assistant

Environment confirmed (Java 25, Gradle 9.7.1). Only `zombie_ice.png` exists — no strong texture, confirming `strong_zombie.json` is a dead example. Let me set up my work plan


**Tool: todowrite**

---

## Assistant

Now I have the full picture. Let me verify a couple more API details (`RegistrationInfo`, `Holder.Reference`) needed for the verification harness


**Tool: bash**

**Tool: bash**

**Tool: bash**

---

## Assistant

All APIs confirmed. Now let me write the executable verification harness. It will use the real production codec, the real `VariantSnapshot.build` validation path, the real `BuiltInRegistries.ATTRIBUTE` (via `Bootstrap.bootStrap()`), and real vanilla biome registry contents (via `VanillaRegistries.createLookup()`).




**Tool: write**

---

## Assistant

Let me fix a type error in the harness (`HolderLookup.RegistryLookup`, not `Holder.Lookup.RegistryLookup`) and verify `Identifier.compareTo` semantics





**Tool: bash**

**Tool: edit**

**Tool: edit**

---

## Assistant

`Identifier.compareTo` compares path first — "ice_zombie" < "strong_zombie" confirmed. Now let me add the Gradle task to run the harness

**Tool: edit**

---

## Assistant

Now let me compile the harness to verify the test source set sees the Minecraft classes




**Tool: bash**

---

## Assistant

The command failed with a process error. Let me retry with a simpler invocation





**Tool: bash**

---

## Assistant

A syntax error in the prepared list. Let me inspect that region

**Tool: read**

---

## Assistant

Line 284 is missing a closing paren for the `unknown_biome` entry. Let me fix it


**Tool: edit**

---

## Assistant

**Tool: bash**

---

## Assistant

The test source set sees Minecraft classes — now fixing type-level issues. Let me check `HolderLookup.RegistryLookup.get` overloads to understand the `get` vs `getOptional` distinction

**Tool: bash**

**Tool: bash**

---

## Assistant

Let me see the rest of `Registry.java` to find the `get(Identifier)` method the production code uses

**Tool: read**

---

## Assistant

`Registry.get(Identifier)` returns `Optional<Holder.Reference<T>>` (line 134) while `getOptional` returns `Optional<T>`. Let me fix the harness accordingly

**Tool: edit**

**Tool: edit**

**Tool: edit**

---

## Assistant

I need to fix the helper calls — `decode` takes only the JSON string. Let me correct those calls and the helpers

**Tool: edit**

**Tool: edit**

**Tool: edit**

---

## Assistant

**Tool: bash**

---

## Assistant

The harness compiles. Now let me run the focused checks






**Tool: bash**

---

## Assistant

The harness runs but `Bootstrap.bootStrap()` needs the game version set first. Let me check `SharedConstants` for the version-detection API





**Tool: bash**

---

## Assistant

`SharedConstants.tryDetectVersion()` is the standard precursor (used by datagen). Let me add it before `Bootstrap.bootStrap()`

**Tool: edit**

---

## Assistant

**Tool: bash**

---

## Assistant

All 50 checks passed with the real registries (65 real biomes, real attribute bounds, real rejection log messages captured). Now let me remove the confirmed-unreachable `strong_zombie.json`




**Tool: bash**

---

## Assistant

The removal is recorded. A `logs/` directory appeared from the verification run — let me check whether it's a build artifact and whether it's git

**Tool: bash**

---

## Assistant

`logs/latest.log` is a runtime artifact generated by the Minecraft logging framework during my verification run (it did not exist in the initial tree). Let me remove it and check how the `test` task behaves now that test classes exist

**Tool: bash**

**Tool: bash**

---

## Assistant

As expected, `test` fails with no JUnit framework present. Gradle's own error message names the minimal remedy: `failOnNoDiscoveredTests = false`. Let me apply it with justification

**Tool: edit**

---

## Assistant

**Tool: bash**

---

## Assistant

Test task passes. Now the fresh compilation requirement — force actual recompilation of the changed sources with `--rerun-t

**Tool: bash**

---

## Assistant

Fresh compilation executed all 5 tasks (none UP-TO-DATE). Now the normal build



**Tool: bash**

---

## Assistant

Build succeeded. Let me re-run the verification against the final state (post-removal) and then perform the final inspection

**Tool: bash**

---

## Assistant

All 50 checks pass against the final state. Let me clean the regenerated log artifact and run the final inspection


**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

The diff is minimal and clean. Let me verify the `AttributeMap` synchronization path to complete the review with source-backed evidence

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

Persistence path confirmed (`output.store("attributes", AttributeInstance.Packed.LIST_CODEC, this.getAttributes().pack())`). Let me finalize the todo list and confirm the untracked files


**Tool: todowrite**

**Tool: bash**

---

## Assistant

Task complete. Final report:

# MobVariantsBRS — Review and Verification Report

## 1. Initial repository state
- Branch: `feat/variant-state-layer` (tracking `origin/feat/variant-state-layer`)
- Initial `git status --short --branch`: **clean working tree** — all S-0002 implementation changes (VariantAttributes.java, VariantSnapshot.java, VariantStateLifecycle.java, strong_zombie.json) were already committed in `9af214f`. No pre-existing uncommitted changes or untracked files existed to preserve.

## 2. Files changed
| File | Change | Reason |
|---|---|---|
| `src/main/resources/data/mob_variants_brs/variants/strong_zombie.json` | **deleted** | Confirmed unreachable example variant (see §3) |
| `src/test/java/.../verify/VariantAttributeVerification.java` | **added** | Executable codec/validation checks against real registries (see §4–5) |
| `build.gradle` | +20 lines | Registers `verifyVariantAttributes` JavaExec task; sets `test.failOnNoDiscoveredTests=false` (repo has no JUnit; Gradle otherwise fails the `test` task when test sources exist) |

No production source files were modified. No dependencies added.

## 3. Example variant — strong_zombie.json removed
Conflict confirmed by executable evidence and source inspection:
- **Ordering**: `VariantDefinitionReloadListener.load` sorts candidates with `Identifier.compareTo` (path-first). Harness check: `ice_zombie < strong_zombie` under that comparator — PASS.
- **Overlap**: harness parsed the real `ice_zombie.json` and confirmed its biome list contains `minecraft:snowy_plains` — PASS. `strong_zombie.json`'s only biome (`snowy_plains`) is a strict subset of ice_zombie's 9 biomes, so **no biome exists where strong_zombie matches but ice_zombie doesn't**.
- **First-match-return**: `VariantStateLifecycle.selectVariant` (lines 88–106) returns immediately after the first matching candidate. Therefore `strong_zombie` is unreachable in every biome.
- **No intentional purpose**: both variants share the identical texture (`zombie_ice.png`); no `zombie_strong.png` asset exists; the file's only unique content (its `attributes`) can never apply.

Removal is the smallest justified change — it changes no gameplay (the data was dead), invents no new requirements, and leaves ice-zombie conditions/texture untouched.

## 4. Codec verification — `.\gradlew.bat verifyVariantAttributes`
All checks exercise the **production** `VariantDefinition.CODEC`. Results: **50/50 PASS**.
- **Decode top-level**: JSON with top-level `attributes` decodes; `entity_type`/`texture`/`conditions` retain values; `attributes` decodes to exactly 3 entries (max_health 40.0, attack_damage 5.0, movement_speed 0.3) — PASS ×8.
- **Encode structure**: encoded output has a top-level `attributes` member that is a flat JSON object containing the three identifiers; **no nested `attributes` key** — PASS ×7.
- **Omitted field**: definition without `attributes` decodes, `attributes()` is empty, other fields intact; encoding produces no `attributes` member — PASS ×4.
- **Round trip**: `decode(encode(decoded))` equals the original definition (record equality); attributes survive — PASS ×2.

## 5. Validation verification — real registries, no mocks
Real registry initialization confirmed executable: `SharedConstants.tryDetectVersion()` + `Bootstrap.bootStrap()` populated the real `BuiltInRegistries.ATTRIBUTE` (harness proves `minecraft:max_health` resolves to the same instance as `Attributes.MAX_HEALTH`, a `RangedAttribute` with real bounds [1.0, 1024.0]); biome registry built from `VanillaRegistries.createLookup()` (65 real biomes, `snowy_plains` present). All cases run through the production `VariantSnapshot.build`; the mod's actual ERROR diagnostics were captured in the run log:
- Unknown identifier `minecraft:not_an_attribute` → rejected ("unknown attribute") — PASS
- `NaN` → rejected ("non-finite value NaN") — PASS
- `+Infinity` (via valid JSON number `1e999`, which Gson parses to ∞ — itself asserted) → rejected — PASS
- `-Infinity` (`-1e999`) → rejected — PASS
- Below bounds (max_health 0.5 < 1.0) → rejected ("outside supported range [1.0, 1024.0]") — PASS
- Above bounds (max_health 2000.0 > 1024.0) → rejected — PASS
- Valid in-range finite values (40.0 / 5.0 / 0.3) → accepted — PASS
- Backward compatibility (no `attributes`) → accepted — PASS; empty `attributes` object → accepted as no-op — PASS
- Real biome condition (`snowy_plains`) → accepted (proves the real biome registry is in use); unknown biome → rejected — PASS
- **Rejection before selection**: `snapshot.variantsFor(minecraft:zombie)` contains exactly the 4 accepted definitions in ascending variant-id order; rejected definitions are absent from `byVariantId` and cannot participate in selection — PASS ×4
- Application API semantics: a real `AttributeInstance` for the real max_health holder starts at the default base value; `setBaseValue(40.0)` yields `getBaseValue()==40.0` and `getValue()==40.0` (absolute, not additive); a second call replaces the previous value — PASS ×3

## 6. Implementation review (source inspection + cited executable evidence)
- **Absolute base values**: `applyAttributes` calls `AttributeInstance.setBaseValue(double)` — sets `this.baseValue` directly (AttributeInstance.java:44–49), not a modifier. Executable check confirms absolute semantics.
- **At most once per selection**: `selectVariant` calls `applyAttributes` exactly once per matching candidate in both branches, then `return`s; the persistent `hasAttached(VARIANT_TEXTURE)` guard makes selection one-shot per entity (not re-selected from disk, respawn, teleport).
- **Server-side only**: selection runs solely in `ServerEntityEvents.ENTITY_LOAD`; loader registered on `PackType.SERVER_DATA` only.
- **Rejection before selection**: validation lives in `VariantSnapshot.build`, executed at snapshot publication (server start / reload) — rejected definitions never enter `byEntityType` (executable evidence above).
- **Failure isolation**: `setAttached(VARIANT_TEXTURE, ...)` executes **before** `applyAttributes` in both branches; per-attribute try/catch with WARN/ERROR logging; missing holder/instance are logged and skipped. Texture assignment cannot be prevented by attribute failures.
- **Bounds consistency**: NaN/±∞ rejected; per-attribute `RangedAttribute.getMinValue()/getMaxValue()` used (all 40 vanilla attributes in 26.1.2 are `RangedAttribute`; a hypothetical non-ranged attribute would get the finite check only — no invented bounds).
- **Preservation**: selection guards, biome-condition logic, attachment persistence (`Identifier.CODEC`), synchronization (`Identifier.STREAM_CODEC`, `AttachmentSyncPredicate.all()`), and rendering paths are untouched.
- **Persistence/sync (source-backed)**: `LivingEntity.addAdditionalSaveData` stores `attributes` via `AttributeInstance.Packed.LIST_CODEC` (includes `baseValue`); restore via `readAdditionalSaveData` → `apply(Packed)`. Sync: `setBaseValue` → `setDirty()` → `AttributeMap.onAttributeModified` adds to `attributesToSync` **iff `isClientSyncable()`** (verified in AttributeMap.java); `ServerEntity` drains `getAttributesToSync()` into `ClientboundUpdateAttributesPacket`. So base values persist and sync for syncable attributes (max_health, movement_speed are syncable; attack_damage is not — vanilla limitation).

## 7. Build verification
- `.\gradlew.bat compileJava compileTestJava --rerun-tasks --console=plain` → **BUILD SUCCESSFUL**, all 5 tasks **executed** (none UP-TO-DATE) — fresh compilation of changed production sources demonstrated.
- `.\gradlew.bat build --console=plain` → **BUILD SUCCESSFUL** (compileJava UP-TO-DATE only because the immediately preceding `--rerun-tasks` compile had already produced fresh outputs; `test` UP-TO-DATE after its first successful execution).
- `.\gradlew.bat verifyVariantAttributes --console=plain` → **BUILD SUCCESSFUL**, 50/50 checks passed (run twice: pre- and post-removal).
- `git diff --check` → exit 0 (no whitespace errors; only a benign LF→CRF warning on build.gradle).

## 8. Unverified behavior
- **Runtime application in a live server** (entity spawn, actual `setBaseValue` on a spawned zombie, client-visible sync): not executed this session — requires a running dedicated server/client. The application path is verified by source inspection plus the executable `AttributeInstance` API check; S-0001/S-0002 previously performed a live runtime render test (see `run/variant_render_test`).
- **Vanilla `ServerEntity` packet drain**: verified up to `AttributeMap.getAttributesToSync()` in the extracted 26.1.2 source this session; the final `ClientboundUpdateAttributesPacket` send was source-verified in the S-0002 session, not re-executed here.
- **Non-`RangedAttribute` attributes**: none exist among vanilla 26.1.2 attributes (all 40 verified `RangedAttribute`), so per-attribute bounds behavior for a hypothetical custom non-ranged attribute is unverified by construction (finite-check-only path).
- Harness side effect: running `verifyVariantAttributes` makes the Minecraft logging framework write `logs/latest.log` (regenerated on each run; removed before final inspection; `logs/` is not gitignored — same class of runtime artifact as the gitignored `run/`).

## 9. Final repository state
- Branch: `feat/variant-state-layer` (no commits or pushes performed)
- `git status --short --branch`:
  ```
   M build.gradle
   D src/main/resources/data/mob_variants_brs/variants/strong_zombie.json
  ?? src/test/java/com/baruc/brs/mobvariants/verify/VariantAttributeVerification.java
  ```
- Unrelated pre-existing changes: none existed in the initial tree (it was clean); all S-0002 committed work is preserved intact in HEAD. No destructive Git commands were used.

---

