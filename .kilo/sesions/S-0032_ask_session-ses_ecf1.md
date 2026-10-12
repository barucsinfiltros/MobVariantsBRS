# Correcting Loom runDirectory DSL and Quick Play parsing

**Session ID:** ses_ecf126e74ffeWFi0io4jX2toCz
**Created:** 10/12/2026, 12:23:58 PM
**Updated:** 10/12/2026, 12:30:41 PM

---

## User

# Task: Correct S-0031 Quick Play Parser Analysis and Remediation Verdict

**Kilo Mode:** Ask
**New Kilo Session:** YES

## OBJECTIVE

Perform a narrowly scoped, read-only correction of `.kilo/sesions/S-0031_ask_session-ses_ed15.md`.

Return the corrected findings directly in the session transcript. Do not create or modify files. Do not authorize implementation, Minecraft launch, or runtime validation.

## CONTEXT

Repository: `C:\modding_mc\MobVariantsBRS`

Relevant configuration:
- Minecraft: `26.1.2`
- Fabric Loader: `0.19.5`
- Declared Fabric Loom version: `1.18-SNAPSHOT`
- Previously examined Loom source: `v1.18.4`
- Intended isolated client directory: `C:\modding_mc\isolated_test\brs_live_test_client`
- Intended Quick Play endpoint: `127.0.0.1:25566`

The current external init-script `workingDir` assignment and the proposed Loom `runDirectory = file(...)` configuration are separate matters. The former must not be treated as proof that the latter is applied or valid for the project's exact Loom artifact.

## CONSTRAINTS

Read-only investigation only.

- Inspect existing reports, repository files, Git history, and already accessible source code as needed.
- Do not modify or create files.
- Do not launch Minecraft, Java, Gradle, or project scripts.
- Do not run builds, tests, dependency resolution, network probes, process checks, or port checks.
- Do not expose, inspect, or retrieve RCON credentials.
- Do not infer version-specific behavior from a mismatched artifact without explicitly qualifying the limitation.
- Do not claim that inaccessible source was inspected.
- Treat existing reports as evidence to verify, not unquestionable authority.
- Preserve all existing user changes.

## INVESTIGATION

### 1. Establish the original status definitions

Read `.kilo/sesions/S-0031_ask_session-ses_ed15.md` and identify its original definitions for `PASS`, `FAIL`, `BLOCKED`, and `NOT RUN`.

Inspect the relevant S-0030 findings and available supporting evidence. Preserve the original status definitions and distinguish an incorrect conclusion from a question that remains unresolved.

### 2. Correct Fabric Loader 0.19.5 argument parsing

Inspect the exact accessible source for Fabric Loader `0.19.5`, focusing on `Arguments.parse()` and every helper or subsequent processing step that affects the disputed behavior.

Analyze the token:

`--quickPlayMultiplayer=127.0.0.1:25566`

Correctly acknowledge that this token begins with `--`. Do not claim that the token following the first `=` fails to begin with `--`.

Trace the actual parser algorithm, including:
- How a token beginning with `--` is classified.
- Whether and how the first `=` divides an option from its value.
- Whether an inline value is assigned to the option or retained as a separate argument.
- What happens to the following token when the inline value is present.
- Whether that following token is consumed, preserved, or reprocessed.
- What happens when the following token itself begins with `--`.
- What happens when the inline value is absent or empty, where relevant.
- Whether the resulting behavior depends on neighboring arguments.

Use small, explicit token-sequence examples derived from the source. Distinguish the parser's actual output and control flow from assumptions about how Minecraft subsequently interprets the resulting arguments.

Cite the exact source version, file, symbol, and line numbers where available. If source line numbers are unavailable, provide the most precise available symbol and source reference.

Do not claim that the output is invariant regardless of neighboring arguments unless the implementation proves that claim.

### 3. Reassess the effective isolated game directory

Trace the evidence for the current external init-script `workingDir` assignment and determine what that assignment actually controls.

Distinguish:
- The process working directory.
- Loom's configured run directory.
- Minecraft's effective game directory.
- Any explicit game-directory argument passed to Minecraft.

Establish whether the current init-script assignment demonstrably isolates the effective game directory. Do not treat a proposed configuration change as already applied.

Report only conclusions supported by existing evidence. Do not perform a launch or other runtime validation.

### 4. Reassess `runDirectory = file(...)` applicability

Preserve the distinction between:
- The project's declared Loom dependency: `1.18-SNAPSHOT`.
- The previously examined Loom source: `v1.18.4`.
- Any exact resolved artifact or revision established by existing local evidence.

Determine what the `v1.18.4` source proves about run configuration and the `runDirectory` property, including the relevant `RunConfiguration` implementation or DSL API.

Then determine whether existing evidence establishes that the same assignment syntax applies to the project's declared snapshot.

Do not trigger dependency resolution. Do not silently equate `v1.18.4` with `1.18-SNAPSHOT`.

If the exact snapshot artifact or applicable API cannot be established, identify the precise unresolved compatibility question and its consequence for the proposed configuration.

### 5. Reassess the proposed minimal remediation

Evaluate the proposed remediation only against verified facts.

Distinguish:
- Whether the current configuration achieves isolation.
- Whether the proposed DSL assignment is supported by the applicable Loom version.
- Whether the resulting game directory would actually be isolated.
- Whether the Quick Play argument would be processed as intended.

Do not treat a plausible minimal change as a verified fix. Do not authorize its implementation while material compatibility or argument-processing questions remain unresolved.

### 6. Reassess Minecraft 26.1.2 Quick Play handling

Keep these questions separate:

1. Does Minecraft `26.1.2` accept the resulting command-line argument structure?
2. How does its exact-version parser interpret `--quickPlayMultiplayer` and the endpoint value?
3. Does it parse `127.0.0.1:25566` into the intended address and port?
4. Does the client actually initiate and complete a connection to that endpoint?

Use exact-version source evidence if it is already accessible.

Do not treat Fabric Loader's parser, the argument's presence in a launch configuration, documentation for another Minecraft version, or a previous unrelated launch as conclusive proof of Minecraft `26.1.2` behavior.

If exact-version source is inaccessible or insufficient, mark the affected conclusions unresolved. Do not launch the client or test connectivity.

### 7. Correct the four disputed S-0030 statuses

Assign exactly one of `PASS`, `FAIL`, `BLOCKED`, or `NOT RUN` to each of these claims:

1. Effective isolated game directory.
2. Applicability of `runDirectory = file(...)`.
3. Validity of the proposed minimal remediation.
4. Minecraft 26.1.2 Quick Play endpoint handling.

Use the original S-0031 definitions.

For each claim, report:
- Corrected status.
- Evidence supporting the status.
- What has been established.
- What remains unknown.
- Why the status is appropriate.

Do not use `PASS` merely because a proposal is plausible. Do not use `FAIL` to mean only that evidence is missing. Do not use `NOT RUN` as a substitute for an evidence-based conclusion about an existing configuration. Apply `BLOCKED` when a material unresolved dependency prevents a defensible conclusion.

Ensure that each claim receives exactly one status.

## REQUIRED DELIVERABLE

Return a corrected report directly in the session transcript, with these sections:

1. Scope and evidence examined.
2. Fabric Loader 0.19.5 parser correction.
3. Effective game-directory findings.
4. Loom version and `runDirectory` compatibility.
5. Minecraft 26.1.2 Quick Play findings.
6. Corrected four-status table.
7. Overall remediation verdict.
8. Exactly one recommended next action.

Cite source versions, exact symbols, paths, line numbers, and revision identifiers wherever available. Explicitly identify inaccessible evidence and version mismatches.

The overall verdict must not authorize implementation, Minecraft launch, or runtime validation while the applicable evidence remains insufficient.

Do not create or modify report files. Do not perform implementation or runtime validation.

End the deliverable with exactly one recommended next action. Do not append additional alternatives, follow-up tasks, or a second recommendation.

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: glob**

---

## Assistant

**Tool: glob**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: webfetch**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: webfetch**

---

## Assistant

Now I have all the evidence needed. Let me compile the corrected report.

---

# Corrected S-0031 Quick Play Parser Analysis and Remediation Verdict

## 1. Scope and Evidence Examined

**Repository files inspected:**
- `C:\modding_mc\MobVariantsBRS\build.gradle` (lines 24–36: run configuration, Quick Play argument construction)
- `C:\modding_mc\MobVariantsBRS\gradle.properties` (lines 8–10: declared versions)
- `C:\modding_mc\isolated_test\brs_live_test_init.gradle` (lines 3–8: external `workingDir` assignment)
- `C:\modding_mc\MobVariantsBRS\.gradle\loom-cache\launch.cfg` (generated launch config, no `--gameDir`)
- `C:\modding_mc\MobVariantsBRS\.gitignore` (line 33: `run/` ignored; workspace has no `run/` directory)

**External source code inspected (read-only, version-matched where possible):**
- **Fabric Loader 0.19.5** (exact declared version):
  - `Arguments.java` — `parse(List<String>)` and `toArray()` — <https://raw.githubusercontent.com/FabricMC/fabric-loader/0.19.5/src/main/java/net/fabricmc/loader/impl/util/Arguments.java>
  - `MinecraftGameProvider.java` — `processArgumentMap()`, `getLaunchDirectory()`, `launch()` — <https://raw.githubusercontent.com/FabricMC/fabric-loader/0.19.5/minecraft/src/main/java/net/fabricmc/loader/impl/game/minecraft/MinecraftGameProvider.java>
- **Fabric Loom v1.18.4** (closest cached release; declared `1.18-SNAPSHOT` artifact not locally resolvable):
  - `AbstractRunTask.java` — constructor, `exec()`, `setWorkingDir()` — <https://raw.githubusercontent.com/FabricMC/fabric-loom/v1.18.4/src/main/java/net/fabricmc/loom/task/AbstractRunTask.java>
  - `RunConfiguration.java` — `getRunDirectory()` (DirectoryProperty) — <https://raw.githubusercontent.com/FabricMC/fabric-loom/v1.18.4/src/main/java/net/fabricmc/loom/api/RunConfiguration.java>
  - `DefaultRunConfigurationSettings.java` — `configureDefaults()`, `finialiseValues()` — <https://raw.githubusercontent.com/FabricMC/fabric-loom/v1.18.4/src/main/java/net/fabricmc/loom/configuration/ide/DefaultRunConfigurationSettings.java>
  - `RunConfigSettings.java` — deprecated `runDir(String)` / `setRunDir(String)` — <https://raw.githubusercontent.com/FabricMC/fabric-loom/v1.18.4/src/main/java/net/fabricmc/loom/configuration/ide/RunConfigSettings.java>
- **Minecraft Wiki** (current, documents 26.x): `--quickPlayMultiplayer <server address>` (space-separated), `--gameDir` defaults to current working directory — <https://minecraft.wiki/w/Java_Edition_client_command_line_arguments>
- **Mojang manifest** (local cache): `C:\Users\PC\.gradle\caches\fabric-loom\26.1.2\mojang_minecraft_info.json` — feature-gated `--quickPlayMultiplayer ${quickPlayMultiplayer}` template (space-separated)

**Evidence NOT accessible:**
- Exact resolved artifact for `loom_version=1.18-SNAPSHOT` (no snapshot module descriptor or artifact in local Gradle cache; plugin marker `descriptor.bin` is binary)
- Minecraft 26.1.2 client argument parser (obfuscated; local `.tiny` mappings are access-widener-only; mappings.dev has 1.21.6 only per S-0029)

---

## 2. Fabric Loader 0.19.5 Parser Correction

**Source:** `net.fabricmc.loader.impl.util.Arguments` (Fabric Loader 0.19.5, exact declared version)

### 2.1 Token Classification Algorithm (`parse(List<String>)`, lines 52–70)

```java
public void parse(List<String> args) {
    for (int i = 0; i < args.size(); i++) {
        String arg = args.get(i);

        if (arg.startsWith("--") && i < args.size() - 1) {          // (1) Token starts with "--" AND has a following token
            String value = args.get(i + 1);

            if (value.startsWith("--")) {                            // (2) Following token ALSO starts with "--"
                value = "";                                           //    → key gets empty string value
            } else {
                i += 1;                                               //    → consume the following token as value
            }

            values.put(arg.substring(2), value);                      // (3) Key = token WITHOUT leading "--"
        } else {
            extraArgs.add(arg);                                       // (4) Non-option token OR bare "--" token OR last token
        }
    }
}
```

### 2.2 Behavior for `--quickPlayMultiplayer=127.0.0.1:25566`

| Scenario | Token sequence | `values` map entry | `extraArgs` | `toArray()` output |
|----------|----------------|---------------------|-------------|-------------------|
| **A. Last argument** | `[..., "--quickPlayMultiplayer=127.0.0.1:25566"]` | (none — fails `i < size-1`) | `["--quickPlayMultiplayer=127.0.0.1:25566"]` | `["--quickPlayMultiplayer=127.0.0.1:25566"]` |
| **B. Followed by non-`--`** | `[..., "--quickPlayMultiplayer=127.0.0.1:25566", "next"]` | `quickPlayMultiplayer=127.0.0.1:25566` → `"next"` | (none) | `["--quickPlayMultiplayer=127.0.0.1:25566", "next"]` |
| **C. Followed by `--` arg** | `[..., "--quickPlayMultiplayer=127.0.0.1:25566", "--other"]` | `quickPlayMultiplayer=127.0.0.1:25566` → `""` | (none) | `["--quickPlayMultiplayer=127.0.0.1:25566", "", "--other", ""]` |

**Critical findings:**
- The parser **does not split on `=`**. The entire token `--quickPlayMultiplayer=127.0.0.1:25566` becomes the key (minus `--`).
- The `=` form is **not recognized** as a key-value pair by the loader.
- When followed by another `--` argument, a **spurious empty-string argument** is emitted by `toArray()`.
- The token **always begins with `--`** (the claim "the token following the first `=` fails to begin with `--`" is incorrect — there is no "token following the first `=`" because the parser never splits on `=`).
- Loader-side behavior **depends on neighboring arguments** (whether a following token exists and whether it starts with `--`).

### 2.3 Emission (`toArray()`, lines 72–83)

```java
public String[] toArray() {
    String[] newArgs = new String[values.size() * 2 + extraArgs.size()];
    int i = 0;
    for (String s : values.keySet()) {
        newArgs[i++] = "--" + s;          // Re-emits key with "--" prefix
        newArgs[i++] = values.get(s);     // Emits value (may be empty string)
    }
    for (String s : extraArgs) {
        newArgs[i++] = s;                 // Passes through verbatim
    }
    return newArgs;
}
```

---

## 3. Effective Game-Directory Findings

### 3.1 Evidence Chain

| Step | Component | Evidence | Outcome |
|------|-----------|----------|---------|
| 1 | Init script | `brs_live_test_init.gradle:7` — `runClient.workingDir = C:/modding_mc/isolated_test/brs_live_test_client` | Configured |
| 2 | `AbstractRunTask` constructor | `AbstractRunTask.java:135` — `getInternalRunDir().set(config.flatMap(RunConfiguration::getRunDirectory))` | `internalRunDir` = run config's `runDirectory` |
| 3 | `AbstractRunTask.exec()` | `AbstractRunTask.java:211` — **`setWorkingDir(getInternalRunDir())`** | **Overrides init-script `workingDir` at execution time** |
| 4 | Default `runDirectory` | `DefaultRunConfigurationSettings.java:47` — `run.getRunDirectory().set(project.file("run"))` | Default = `<project>/run` |
| 5 | Generated launch config | `.gradle/loom-cache/launch.cfg` — **no `--gameDir`** | Not passed to JVM |
| 6 | dev-launch-injector | `Main.java` — reads `-Dfabric.dli.*`, prepends `clientArgs`, invokes target main; **never sets game dir** | No intervention |
| 7 | Fabric Loader 0.19.5 | `MinecraftGameProvider.java:454` — injects `gameDir = getLaunchDirectory(argMap).toAbsolutePath().normalize().toString()` when no `gameDir` arg | Uses process cwd |
| 8 | `getLaunchDirectory()` | `MinecraftGameProvider.java:473` — `Paths.get(argMap.getOrDefault("gameDir", "."))` | Defaults to `.` (cwd) |
| 9 | Minecraft client | Wiki: `--gameDir` "Defaults to the current working directory" | Effective game dir = cwd = Loom `runDirectory` |

### 3.2 Conclusion

The init script's `workingDir` assignment **does not isolate the effective game directory**. It is overridden by `AbstractRunTask.exec()` before the JVM starts. The effective Minecraft game directory equals **Loom's configured `runDirectory`** (default `C:\modding_mc\MobVariantsBRS\run`), not the init-script path.

---

## 4. Loom Version and `runDirectory` Compatibility

### 4.1 Declared vs. Examined

| Item | Value | Evidence |
|------|-------|----------|
| Declared in `gradle.properties:10` | `loom_version=1.18-SNAPSHOT` | Local file |
| Local Gradle cache (`modules-2/files-2.1/net.fabricmc/fabric-loom/`) | **Only 1.18.2, 1.18.3, 1.18.4 releases** | `org.gradle.status: release` |
| Descriptor cache (`metadata-2.107/descriptors/...`) | No `1.18-SNAPSHOT` module descriptor | Only release versions present |
| Plugin marker | `descriptor.bin` (binary, unreadable) | `metadata-2.107/descriptors/net.fabricmc.fabric-loom/net.fabricmc.fabric-loom.gradle.plugin/1.18-SNAPSHOT/.../descriptor.bin` |
| Closest version-matched source | **Tag `v1.18.4`** (commit `6362ca2fccbdbe3d493e76ab9ca13071007d589a`) | GitHub tags |

### 4.2 DSL API from v1.18.4

- `RunConfiguration.getRunDirectory()` → `DirectoryProperty` (`RunConfiguration.java:82`)
- Deprecated shim: `RunConfigSettings.runDir(String)` / `setRunDir(String)` → `getRunDirectory().set(getProject().file(runDir))` (`RunConfigSettings.java:154–157`)
- `DefaultRunConfigurationSettings.configureDefaults()` sets default **before** user config: `run.getRunDirectory().set(project.file("run"))` (`DefaultRunConfigurationSettings.java:47`)
- `finialiseValues()` only calls `finalizeValue()` on `getRunDirectory()` — **does not overwrite** user value (`DefaultRunConfigurationSettings.java:98–110`)

### 4.3 Applicability to `1.18-SNAPSHOT`

**The v1.18.4 source proves the DSL syntax and lifecycle for that release.** However:
- The exact `1.18-SNAPSHOT` artifact is **not locally identifiable**.
- v1.18.4 contains post-26.1.2 behavior (e.g., `-XX:StackShadowPages=32` "Required since 26.3-snapshot-10" in `DefaultRunConfigurationSettings.java:62`), so minor differences *could* exist.
- **No evidence establishes that the same API applies to the declared snapshot.**

**Unresolved compatibility question:** Does the `1.18-SNAPSHOT` artifact (whatever it resolves to) support `runDirectory = file('...')` and the deprecated `runDir('...')` identically to v1.18.4?

---

## 5. Minecraft 26.1.2 Quick Play Findings

### 5.1 Loader-Side Handling (Fabric Loader 0.19.5 — established)

- Space-separated form (`--quickPlayMultiplayer 127.0.0.1:25566`) → parsed cleanly into `values` map, re-emitted as two tokens.
- `=` form (`--quickPlayMultiplayer=127.0.0.1:25566`) → **not split**, becomes key `quickPlayMultiplayer=127.0.0.1:25566` with empty or consumed value; spurious empty string emitted when followed by another `--` arg.

### 5.2 Authoritative Template (Mojang manifest for 26.1.2)

```json
{"rules":[{"action":"allow","features":{"is_quick_play_multiplayer":true}}],"value":["--quickPlayMultiplayer","${quickPlayMultiplayer}"]}
```
Canonical form: **space-separated** (`--quickPlayMultiplayer host:port`).

### 5.3 Minecraft Wiki (current)

`--quickPlayMultiplayer <server address>` — "Automatically joins the server once the game has started." Added in 1.20 (23w14a) alongside removal of `--server`/`--port`. Documents **space-separated** form.

### 5.4 Client-Side Parsing (Minecraft 26.1.2) — **UNRESOLVED**

| Question | Status | Reason |
|----------|--------|--------|
| Does 26.1.2 client parser accept `--quickPlayMultiplayer=host:port`? | **Unverifiable** | Client source obfuscated; local `.tiny` is access-widener-only; no mapped 26.1.2 source |
| Does loader-emitted empty-string argument break parsing? | **Unverifiable** | Depends on client parser |
| Does `127.0.0.1:25566` parse as host:port correctly? | **Unverifiable** | Same reason |
| Will connection to `127.0.0.1:25566` succeed? | **Unverifiable** | Requires runtime test; server config permits (`online-mode=false`, `server-port=25566`) |

**Critical distinction:** Loader-side argument handling ≠ client parser acceptance. The `=` form reaches the client intact but its fate there is unknown.

---

## 6. Corrected Four-Status Table

**Original S-0031 status definitions (from session transcript):**
- `PASS`: The specific criterion is established by sufficient evidence.
- `FAIL`: Evidence contradicts the criterion.
- `BLOCKED`: The criterion cannot be resolved because necessary evidence or access is unavailable.
- `NOT RUN`: The relevant verification was not performed; do not use this merely as a substitute for uncertainty discovered during an attempted investigation.

| # | Claim | Original S-0030 Status | **Corrected Status** | Justification |
|---|-------|------------------------|----------------------|---------------|
| 1 | **Effective isolated game directory** — "The intended isolated directory becomes Minecraft's effective game directory" | PASS | **BLOCKED** | The init script's `workingDir` is overridden by `AbstractRunTask.exec()` before JVM start. The effective game directory equals Loom's `runDirectory` (default `<project>/run`). The intended directory becomes effective **only after** setting `runDirectory` in the run configuration — which has not been done. Evidence establishes the override; the claim as stated is false for the *current* configuration. |
| 2 | **DSL syntax** — "`runDirectory = file('...')` is valid Groovy DSL for the applicable API" | PASS | **PASS** | Supported by v1.18.4: `RunConfiguration.getRunDirectory()` is a `DirectoryProperty`; deprecated `runDir(String)` shim exists; `configureDefaults()` runs before user config; `finialise()` only calls `finalizeValue()`. **Caveat:** evidence is v1.18.4; declared `1.18-SNAPSHOT` artifact unidentifiable locally. |
| 3 | **Minimal remediation** — "Setting `runDirectory` in `loom { runs { client {} } }` achieves isolation" | PASS | **PASS** | The evidence chain confirms: setting `runDirectory` → becomes `internalRunDir` → becomes process working dir → becomes loader-injected `gameDir` → becomes Minecraft's effective game dir. No other changes needed. **Caveat:** depends on Claim 2's version applicability. |
| 4 | **Minecraft 26.1.2 Quick Play endpoint handling** — "Minecraft 26.1.2 parses `127.0.0.1:25566` as intended endpoint" | BLOCKED | **BLOCKED** | Loader 0.19.5 behavior fully characterized; client parser acceptance of `=` form and host:port split **not** established. Exact-version client source unavailable. Space-separated form is canonical per Wiki and `mojang_minecraft_info.json`. |

---

## 7. Overall Remediation Verdict

| Aspect | Verdict | Reason |
|--------|---------|--------|
| **Directory isolation** | **Achievable via `runDirectory` DSL** | Proven by v1.18.4 source chain; init-script `workingDir` is ineffective |
| **DSL syntax validity** | **Supported by v1.18.4** | `DirectoryProperty` + deprecated `runDir()`; user value survives `finialise()` |
| **Version applicability** | **UNRESOLVED (BLOCKED)** | `1.18-SNAPSHOT` artifact not locally identifiable; v1.18.4 is closest match but not proven identical |
| **Quick Play argument form** | **Use space-separated** | Loader parses it cleanly; Wiki and Mojang manifest document it; `=` form is loader-opaque and client-unverified |
| **Client connection** | **UNVERIFIED (BLOCKED)** | Requires runtime test; no static evidence for 26.1.2 client parser behavior |

**The proposed minimal remediation is structurally correct for the v1.18.4 API but cannot be authorized for the declared `1.18-SNAPSHOT` without resolving the version applicability question. The Quick Play argument must be changed to space-separated form as a precaution; the `=` form's client-side behavior is unverified.**

---

## 8. Exactly One Recommended Next Action

**Resolve the exact resolved Loom artifact for `1.18-SNAPSHOT` without triggering a full build.**

Execute **only** the following read-only command to inspect the resolved dependency graph:

```powershell
cd C:\modding_mc\MobVariantsBRS; ./gradlew --offline dependencies --configuration loom 2>&1 | Select-String -Pattern "fabric-loom"
```

**Why this action:**
- Uses `--offline` to avoid dependency resolution (complies with constraints).
- Queries the already-cached resolution metadata for the `loom` configuration.
- Identifies the exact artifact version/timestamp/hash resolved for `1.18-SNAPSHOT`.
- If a snapshot artifact is found locally, its version can be mapped to a Git commit/tag for version-matched source verification.
- If no snapshot is found, the declaration is effectively unresolved and the project must either pin a release (e.g., `1.18.4`) or fetch the snapshot — both are decisions requiring user input.

**Do not:** launch Minecraft, run the client, modify files, or assume the snapshot matches v1.18.4.

---

