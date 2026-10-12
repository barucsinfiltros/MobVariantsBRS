# Minimal Security Remediation Plan — Isolated Minecraft Test (26.1.2)

**Plan ID:** 1791776861582-minimal-security-remediation-plan
**Primary audit:** S-0025 (`.kilo/sesions/S-0025_ask_session-ses_ed1d.md`)
**Scope:** Read-only planning. No changes, no execution, no launch.

---

## 1. Evidence Status

### Verified (directly supported by files + source code)

| Finding | Evidence | Source |
|---|---|---|
| Minecraft version 26.1.2 | `gradle.properties:8`, `fabric.mod.json:34`, `latest.log:1` | Three independent sources |
| `online-mode=false` | `server.properties:47` | Static config |
| `enable-rcon=true` | `server.properties:14` | Static config |
| `rcon.port=25576` | `server.properties:55` | Static config |
| `enable-query=false` | `server.properties:13` | Static config |
| `query.port=25566` | `server.properties:52` | Static config (inactive — Query disabled) |
| `server-ip=` (empty) | `server.properties:62` | Static config |
| `server-port=25566` | `server.properties:63` | Static config |
| `broadcast-rcon-to-ops=true` | `server.properties:7` | Static config |
| `white-list=false` | `server.properties:72` | Static config |
| `eula=true` | `eula.txt:3` | Static config |
| `rcon.bind` does not exist | Minecraft 26.1.2 source (`RconThread.java`); Wiki | S-0025 lines 601–610 |
| `server-ip` controls RCON binding | Minecraft 26.1.2 source (`RconThread.create()` uses `serverInterface.getServerIp()`) | S-0025 lines 526–537; Correction 3 |
| `enable-query` is the Query gate (not `query.port`) | Minecraft 26.1.2 source (`DedicatedServer.initServer()`: `if (properties.enableQuery)`) | S-0025 lines 539–554 |
| RCON password was disclosed in S-0024 report | S-0024 session transcript, line 282 | S-0024 line 282 |
| Init script sets `runClient.workingDir` | `brs_live_test_init.gradle:6–8` | Static config |
| `build.gradle` has no `workingDir` override | S-0025 grep result, lines 571–575 | Static config |
| Client directory is empty | `ls` returns no entries (`brs_live_test_client`) | Static config |
| `--init-script` applies to same invocation | Gradle CLI docs | S-0025 lines 565–570 |
| `autoConnect` → `--quickPlayMultiplayer` | `build.gradle:32–34` | Static config |
| Historical server was running | `latest.log:58` — `Starting minecraft server on *:25566` | Historical log |
| Historical RCON was on all interfaces | `latest.log:70` — `RCON running on 0.0.0.0:25576` | Historical log |

### Contradicted

| Claim (from earlier reports) | Correction | Evidence |
|---|---|---|
| S-0023: "RCON always binds to 0.0.0.0; `server-ip` controls game port only" | S-0025: `server-ip` DOES control RCON binding (source code proof) | S-0025 Correction 3; `RconThread.java` source |
| S-0024: "Query enabled on same port" | Query is disabled (`enable-query=false`); `query.port` is inert when disabled | S-0025 Correction 1; server.properties:13; source code |
| S-0024: `rcon.bind=127.0.0.1` is a valid option | No such property exists in vanilla Minecraft 26.1.2 | S-0025 Correction 2; source code + Wiki |
| S-0024/S-0023: "unauthenticated RCON" | RCON requires its own password independent of `online-mode`; phrase is contradictory | S-0025 Correction 4 |

### Unverified

| Finding | Reason |
|---|---|
| Server is currently running or stopped | No live process/port checks performed (task constraint); `latest.log` is historical (00:15–00:25 on 2026-10-11, clean shutdown at 00:25:09) |
| Network is actually reachable on ports 25566/25576 | Firewall rules not inspected; no network probes performed |
| Client game directory equals `workingDir` at runtime | Depends on Loom's internal `workingDir` → `gameDir` mapping; cannot verify without launching |
| All game data (saves, logs, config) is isolated at runtime | Requires client launch to verify |
| Host firewall blocks non-loopback inbound | Not inspected (static config only) |

### Not found / inaccessible

The repository-level `run/server.properties` referenced by S-0023 was not found via glob (`**/run/server.properties` — no matches). S-0023's table references it but no file evidence was located.

---

## 2. Proposed Configuration

### Recommended target values for `server.properties`

| Property | Current value | Recommended target | Change type | Rationale |
|---|---|---|---|---|
| `server-ip` | *(empty)* | `127.0.0.1` | **Recommended** | Binds both game server (25566) and RCON (25576) to loopback only. Source code confirms `RconThread.create()` uses `serverInterface.getServerIp()` (S-0025 lines 526–537). This is the only vanilla configuration-level method to restrict RCON to loopback — no `rcon.bind` exists. |
| `online-mode` | `false` | `true` | **Recommended** | Verifies player identities against Mojang. Offline mode allows arbitrary username impersonation. Must be set to `true` unless explicit conditions in §4 are accepted. |
| `enable-rcon` | `true` | `false` | **Recommended** | RCON is unnecessary for the basic Quick Play auto-connect test (see §3.2). Disabling eliminates the exposed credential entirely. |
| `rcon.port` | `25576` | *(no action if RCON disabled)* | **Conditional** | No change needed when `enable-rcon=false`. The RCON listener is not created at all (`DedicatedServer.initServer()`: `if (properties.enableRcon)` — S-0025 lines 548–553). |
| `enable-query` | `false` | `false` | **No change** | Already disabled. No action required. |
| `query.port` | `25566` | *(no action — Query disabled)* | **No action** | Irrelevant when `enable-query=false`. Source code confirms Query starts only when `enableQuery` is true (S-0025 lines 539–547). |
| `white-list` | `false` | `true` | **Recommended** | Ensures only explicitly listed players can join. Currently allows anyone with the address to connect (in combination with `online-mode=false`, this is especially risky). |
| `broadcast-rcon-to-ops` | `true` | `false` | **Recommended** | RCON output should not be broadcast to ops. (Only relevant if RCON is re-enabled after approval.) |

### Values requiring explicit approval before changing

| Property | Reason for approval gate |
|---|---|
| `online-mode` → `true` | May prevent test client connection if the client's Fabric installation is not configured for online mode. Must verify client has valid Mojang credentials. |
| `enable-rcon` → `false` | May break RCON-based test procedures (e.g., `rcon_test.py` / `rcon_test.ps1` mentioned in TESTING.md:224). Requires explicit decision on whether those procedures are needed. |
| `server-ip=127.0.0.1` | Will prevent remote access entirely. Must be confirmed acceptable for the test scope. |
| `rcon.password` rotation | Requires updating only authorized credential storage locations and verifying old credential is rejected. See §3.3. |

### Values confirmed as correct (no action)

| Property | Current value | Confirmed correct |
|---|---|---|
| `server-port` | `25566` | Matches `autoConnect` destination in `build.gradle:32–34` |
| `enable-query` | `false` | S-0025 Correction 1; no `query.port` change needed |
| `level-name` | `isolated_live_test` | Distinct from repository run server's world |
| `gamemode` | `creative` | Appropriate for testing |
| `difficulty` | `easy` | Appropriate for testing |

---

## 3. Risk Assessment

### 3.1 RCON

**Why disable RCON is preferable to relying on an exposed password:**

1. **Password is already compromised.** S-0024 reproduced the RCON password (`server.properties:54`) in its session report (S-0024 line 282). The credential must be treated as compromised regardless of any current access controls.
2. **No `rcon.bind` exists.** S-0023 and S-0024 both explored binding options. Neither `rcon.bind` nor `rcon.host` is a valid Minecraft property (confirmed by source code and Wiki). The only vanilla option is `server-ip`, which affects both game and RCON.
3. **RCON has no encryption.** The Minecraft Wiki RCON page (S-0025 line 715) warns: "RCON is not encrypted." An exposed password over the network is trivially intercepted.
4. **Disabling eliminates all RCON risk.** With `enable-rcon=false`, no RCON listener is created (`DedicatedServer.initServer()`: `if (properties.enableRcon)` — S-0025 lines 548–553). There is nothing to bind, nothing to probe, and no credential to expose.

**When `rcon.port` needs changing:** No change is needed when `enable-rcon=false`. The port property is only consulted when RCON is enabled. Changing `rcon.port` while RCON is disabled is meaningless.

**Test procedures that would actually require RCON:**
- TESTING.md:224 mentions `rcon_test.py` / `rcon_test.ps1` for issuing test commands without joining the game.
- The basic Quick Play auto-connect test (`build.gradle:32–34` → `--quickPlayMultiplayer=localhost:25566`) does **not** require RCON — it connects the client directly to the server.
- If the broader test plan requires automated server command issuance without a connected client, RCON is needed. This is a **test-specific decision** requiring explicit approval.

**Additional approval before re-enabling RCON:**
- Rotate the compromised RCON password (see §3.3).
- Set `server-ip=127.0.0.1` to bind RCON to loopback only.
- Confirm host firewall blocks inbound 25576 from non-loopback.
- Document explicit justification for why RCON is necessary for the specific test.
- Obtain written authorization for RCON re-enablement.

### 3.2 `online-mode=false`

**Assessment (separate from RCON authentication):**

`online-mode=false` disables Mojang account verification for player logins. This is a **completely separate** authentication dimension from RCON password authentication:

- **`online-mode=false`** → Any client can join with any username. No identity verification. Server log warns: "The server will make no attempt to authenticate usernames" (`latest.log:60`).
- **RCON password** → Independent password-based auth for the RCON admin interface (`rcon.password`). Required even when `online-mode=false`.

The two are not equivalent. Disabling `online-mode` does not bypass RCON password authentication. RCON requires its password regardless of the `online-mode` setting.

**Prefer `online-mode=true`** unless the actual local development test requires offline-mode operation.

**Conditions required to accept `online-mode=false` risk:**
1. The server is restricted to localhost (`server-ip=127.0.0.1`).
2. External access is independently blocked (host firewall verified).
3. Only trusted local processes and users can access the test.
4. The environment contains no valuable or production world data (only `isolated_live_test` world, disposable).
5. The identity-spoofing risk is explicitly accepted for this disposable test.

If all five conditions are met and documented, `online-mode=false` may be retained temporarily. This is a **risk acceptance decision**, not a configuration change.

### 3.3 Credential disclosure

S-0024 reproduced the RCON password value in its session report (S-0024 line 282), expanding exposure beyond `server.properties:54`. The credential (`server.properties:54`) must be treated as compromised.

**Remediation procedure (NOT to be executed in this task):**

1. **Generate a replacement.** Use a cryptographically random secret (≥32 bytes, base64 or hex). Do not reuse any previously used password.
2. **Store the replacement only in authorized locations.** Update `server.properties:54` (`rcon.password`) as the sole credential store if RCON is to be re-enabled. Remove the old password from `server.properties` — do not leave it in comments, logs, or history files.
3. **Avoid disclosure.** Never print, quote, copy, embed, or paste the old or replacement secret into reports, commands, session transcripts, or evidence files. Use a `REDACTED` placeholder when documenting the field's presence.
4. **Verify the old credential is rejected.** After rotation, attempt an RCON connection using the old password — confirm it is rejected. Do not disclose the result's authentication details.
5. **Verify the replacement remains secret.** Confirm the new password does not appear in any log file, config file, or transcript. Confirm no backup or history file retains the old value.

**Do not rotate or verify credentials in this task.** This procedure is documented for a future authorized phase.

---

## 4. Host Binding vs. Firewall Protection

### Binding the Minecraft server socket to loopback

Setting `server-ip=127.0.0.1` in `server.properties` restricts the game server's main listener to the loopback interface only. Per Minecraft 26.1.2 source code, this **also** restricts RCON binding (since `RconThread.create()` uses `serverInterface.getServerIp()` — S-0025 lines 526–537). This is the preferred configuration-level defense.

### Firewall rules restricting inbound traffic

Firewall rules provide OS-level defense-in-depth but are **separate** from the server's bind configuration. A firewall can block inbound traffic to ports 25566 and 25576 from non-loopback sources even if the server binds to all interfaces. However:
- No firewall rules were inspected (task constraint).
- Firewall presence or configuration is **UNVERIFIED**.
- Firewall rules alone do not eliminate the risk of credential exposure — they merely reduce reachability.

### RCON and Query listeners (separate from main gameplay listener)

- **RCON**: Separate listener on `rcon.port` (25576). Only created when `enable-rcon=true`. Uses `serverInterface.getServerIp()` for binding (S-0025 Correction 3).
- **Query**: Separate listener on `query.port` (25566). Only created when `enable-query=true`. Uses `serverInterface.getServerIp()` for binding (S-0025 Correction 1). Currently disabled — no listener created.
- Both are gated by their respective enable flags in `DedicatedServer.initServer()` (S-0025 lines 539–553).

### If RCON is disabled, is its listener binding still relevant?

No. When `enable-rcon=false`, the RCON listener is never created. The `RconThread.create()` method is never called. `rcon.port` and the (non-existent) binding behavior are irrelevant. Disabling RCON is the cleanest configuration — it removes the listener, the binding question, and the credential exposure all at once.

**Do not claim** that firewall protection or external inaccessibility has been verified without evidence. No firewall inspection was performed.

---

## 5. Client Directory Isolation

### Current static evidence

- `brs_live_test_init.gradle:6–8`: Sets `runClient.workingDir = new File('C:/modding_mc/isolated_test/brs_live_test_client')`.
- `build.gradle:24–36`: No `workingDir` override in the `loom.runs.client` block.
- Grep for `workingDir|runDir|gameDir|game.directory` across all `.gradle` files: no matches (S-0025 line 575).
- `brs_live_test_client` directory: confirmed empty.

### Classification: UNVERIFIED

Loom's `workingDir` setter configures the process working directory for the `runClient` task. However, **whether Minecraft's effective game directory** (where `saves/`, `logs/`, `options.txt`, `config/` are written) **equals** the `workingDir` at runtime depends on:

1. Loom's internal mapping of `workingDir` → `runDir` → `gameDir`.
2. Whether the Fabric Loom version in use (`loom` plugin in `build.gradle`) treats `workingDir` as the game directory or only as the process working directory.
3. Whether any Fabric mod or Mixin redirects game file I/O to an alternate location.

A Gradle working directory, an init script, or a declared launch argument alone does **not** prove effective game-directory isolation.

### Smallest additional static evidence needed

- Read the Fabric Loom plugin source or documentation for the Loom version used in this project, specifically how `workingDir` maps to the Minecraft game directory.
- Check `build.gradle` for the Loom plugin version and inspect `gradle/libs.versions.toml` or `gradle.properties` for the Loom version if available.
- Review `loom.runs.client` block in `build.gradle:24–36` for any `runDir` or `gameDir` property that overrides the default.

### Runtime evidence (separately authorized future step)

If static evidence is insufficient, a future authorization could:
- Launch the client and inspect the actual game directory for `saves/`, `logs/latest.log`, `options.txt`, and `config/`.
- Verify no files are written outside the isolated client directory.
- Confirm the `isolated_live_test` world is not accessed by the client.

**This is a separately authorized future verification step and is NOT part of this plan.**

---

## 6. Required Approvals

The following decisions must be made explicitly before implementation or launch:

| # | Decision | Default recommendation |
|---|---|---|
| 1 | Set `server-ip=127.0.0.1`? | **Yes** — binds both game and RCON to loopback. |
| 2 | Set `online-mode=true` or accept `false`? | **True** — unless explicit risk-acceptance conditions in §3.2 are documented. |
| 3 | Keep, disable, or re-enable RCON? | **Disable** (`enable-rcon=false`) — unnecessary for basic Quick Play test. |
| 4 | Rotate compromised RCON password? | **Yes** — if RCON is ever re-enabled, rotate before re-enabling. |
| 5 | Set `white-list=true`? | **Yes** — minimizes access to trusted clients only. |
| 6 | Set `broadcast-rcon-to-ops=false`? | **Yes** — if RCON is re-enabled. |
| 7 | Verify host firewall rules? | **Required** — confirm no inbound access to 25566/25576 from non-loopback. |
| 8 | Verify client game-directory isolation? | **Required before client launch** — separate authorization. |
| 9 | Authorize any process launch or build execution? | **None authorized by this task.** |

---

## 7. Future Validation Plan (non-executed, ordered)

1. **Confirm intended server properties.** Read `server.properties` and verify: `server-ip=127.0.0.1`, `enable-rcon=false`, `enable-query=false`, `online-mode=<decided>`. Status: `NOT RUN`.
2. **Verify interface binding.** Confirm server binds only to loopback (static: `server-ip=127.0.0.1`; if needed in future authorized phase: process inspection of `latest.log` for `Starting minecraft server on 127.0.0.1:25566`). Status: `NOT RUN`.
3. **Confirm RCON and Query are disabled.** Read `server.properties`: `enable-rcon=false`, `enable-query=false`. Status: `NOT RUN`.
4. **Verify firewall protections.** Inspect host firewall rules for ports 25566 and 25576 — confirm inbound blocked except from loopback. Status: `NOT RUN`.
5. **Confirm credential rotation before RCON reactivation.** If RCON is re-enabled: rotate password, update only `server.properties`, verify old credential rejected, verify new secret not in logs. Status: `NOT RUN`.
6. **Record explicit `online-mode` decision.** Document whether `online-mode=true` or whether offline-mode risk-acceptance conditions are met. Status: `NOT RUN`.
7. **Verify effective client game directory.** Inspect actual game files, logs, worlds, and configuration location — either via static Loom source inspection or via separately authorized runtime check. Status: `UNVERIFIED`.
8. **Confirm launch procedure uses only the intended isolated environment.** Verify `--init-script` applied to `runClient`, `workingDir` isolated, `-PautoConnect=localhost:25566` matches server port. Status: `NOT RUN`.
9. **Obtain explicit authorization.** Before any change, runtime check, build, or launch. Status: `NOT RUN`.

---

## 8. Acceptance Criteria

| # | Criterion | Status | Evidence / Notes |
|---|---|---|---|
| 1 | Server properties set to localhost-only and RCON/Query disabled unless explicitly justified | `NOT RUN` | Target: `server-ip=127.0.0.1`; `enable-rcon=false`; `enable-query=false` (already). Current: `server-ip=` empty, `enable-rcon=true`. |
| 2 | Server binds only to intended interface and firewall protections exist | `NOT RUN` | Static target: `server-ip=127.0.0.1`. Firewall: not inspected. |
| 3 | RCON credential rotated before any future reactivation | `NOT RUN` | Old password exposed in S-0024 report (line 282). Rotation requires future authorization. |
| 4 | Explicit decision on `online-mode` recorded | `NOT RUN` | Current: `false`. Decision pending. |
| 5 | Effective client game directory verified | `UNVERIFIED` | Init script sets `workingDir` (line 6–8); no override in `build.gradle`. Runtime mapping UNVERIFIED. |
| 6 | Launch procedure uses only intended isolated environment | `NOT RUN` | Init script + `--init-script` flag + `-PautoConnect=localhost:25566` proposed. Not yet authorized. |
| 7 | Read-only compliance: no files/processes modified | `PASS` | Only `read`, `grep`, `glob`, `ls` tools used. No edits, writes, or executions in this task. |

---

## 9. Recommended Next Action

**Do not implement, launch, or execute anything.** The single recommended next step is:

> **Read the Fabric Loom plugin documentation for the project's Loom version** (check `build.gradle` for the `fabric` plugin version and inspect `gradle/libs.versions.toml` or `gradle.properties` if present) to establish, from static version-appropriate documentation, whether Loom's `runClient.workingDir` maps to Minecraft's effective game directory. This resolves the primary UNVERIFIED item (client game-directory isolation) without requiring any execution.

This is a read-only information-gathering step that directly closes the largest remaining evidence gap. No changes, no launches, and no network probes are required.

---

*Plan file: `C:\modding_mc\MobVariantsBRS\.kilo\plans\1791776861582-minimal-security-remediation-plan.md`*
