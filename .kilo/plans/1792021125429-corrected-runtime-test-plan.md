# Corrected Runtime-Test Plan for MobVariantsBRS

## 1. Verified Current State

### Repository State
- **Branch**: `feat/variant-state-layer` (confirmed via `git status`)
- **HEAD**: `2a16099` ("Correcion de Kilo MarkDown")
- **Working tree**: Clean (no uncommitted changes)
- **Discrepancy with prior reports**: Previous plan (1791998410131) reported HEAD `74835b3`; actual HEAD is `2a16099` — 3 commits ahead.

### Project Configuration (Verified from Source)
| Component | Version | Evidence |
|-----------|---------|----------|
| Minecraft | 26.1.2 | `gradle.properties:8`, `fabric.mod.json:34`, runtime logs |
| Fabric Loader | 0.19.5 | `gradle.properties:9`, `fabric.mod.json:33`, runtime logs |
| Fabric API | 0.155.3+26.1.2 | `gradle.properties:17`, runtime logs |
| Java Runtime | 25.0.2 (Temurin) | `java -version`, `build.gradle:64` |
| Java Toolchain | Release 25 | `build.gradle:64, 93-94` |
| Mod Version | 1.0.0 | `gradle.properties:13`, `fabric.mod.json:4`, JAR manifest |
| Mod ID | `mob_variants_brs` | `fabric.mod.json:3`, `MobVariantsBRS.java:16` |

### Built Artifacts
| Artifact | Path | Size | Notes |
|----------|------|------|-------|
| Main JAR | `run/mods/mob_variants_brs-1.0.0.jar` | 39,094 bytes | Loaded in both client/server runtime logs |
| Sources JAR | `build/libs/mob_variants_brs-1.0.0-sources.jar` | 27,753 bytes | |
| Built JAR | `build/libs/mob_variants_brs-1.0.0.jar` | 39,094 bytes | Identical to run/mods copy |

### Variant Definition (Single Source of Truth)
**File**: `src/main/resources/data/mob_variants_brs/variants/ice_zombie.json`
```json
{
  "entity_type": "minecraft:zombie",
  "texture": "mob_variants_brs:textures/entity/zombie/zombie_ice.png",
  "conditions": {
    "biomes": [
      "minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga",
      "minecraft:snowy_slopes", "minecraft:frozen_peaks", "minecraft:frozen_river",
      "minecraft:snowy_beach", "minecraft:frozen_ocean", "minecraft:deep_frozen_ocean",
      "minecraft:grove"
    ]
  }
}
```
- **Eligible biomes**: 10 snowy/cold biomes (explicitly listed)
- **Ineligible biomes**: Any biome not in list (e.g., `minecraft:plains`, `minecraft:desert`)
- **Attributes**: None defined in this variant
- **Texture asset**: `src/main/resources/assets/mob_variants_brs/textures/entity/zombie/zombie_ice.png` (598 bytes, exists)

### Isolated Environment State
| Environment | Path | Status | Critical Issues |
|-------------|------|--------|-----------------|
| **Client** | `C:\modding_mc\isolated_test\brs_live_test_client` | **Empty directory** | No `.minecraft`, no `mods/`, no config, no world |
| **Server** | `C:\modding_mc\isolated_test\brs_live_test_server` | Configured but **not standalone** | `mods/` empty; `start_server.bat` uses dev classpath (`.gradle/loom-cache`, `build/loom-cache`) |
| **Server World** | `.../isolated_live_test/` | Exists, has `session.lock` | Flat world, `generator-settings={}` → defaults to `minecraft:plains` (ineligible) |
| **Server Logs** | `.../logs/` | Exist, show successful runs | Mod loads, variant definitions load (2 candidate files), attributes applied to zombies |

### Main Project Runtime (Reference)
| Component | Path | Status |
|-----------|------|--------|
| Mod JAR | `run/mods/mob_variants_brs-1.0.0.jar` | Present, loaded |
| World `world` | `run/world/` | No `session.lock`, not flat |
| World `variant_render_test` | `run/variant_render_test/` | Has `session.lock`, flat plains biome |
| Server Port | 25565 | From `run/server.properties:61` |
| RCON Port | 25575 | From `run/server.properties:53` |

### Runtime Evidence (from Isolated Server `debug.log`)
- **Variant loading**: `[00:15:49] Loaded 2 variant definitions from 2 candidate files` (prepare + apply phases)
- **Variant selection**: Attributes applied to spawned zombies (`max_health=40.0`, `movement_speed=0.3`)
- **Biome context**: World is flat plains → ice_zombie should NOT match (conditions require snowy biomes)
- **Implication**: A second variant definition (without biome conditions) exists and is being selected as fallback. Source of second definition unverified (only `ice_zombie.json` found in source/build).

### Process & Lock State
- **Process inspection**: BLOCKED (tool restrictions prevent `tasklist`/`Get-Process`)
- **`session.lock` files**: Present in both `isolated_live_test/` and `variant_render_test/` (contain only `☃`)
- **Lock status**: UNKNOWN — cannot distinguish stale vs. active without process inspection

---

## 2. Technical Decisions

### 2.1 JAR Provenance Method
**Decision**: Use **content-addressable verification** (SHA-256) of the JAR against the current source tree.
- **Procedure**:
  1. Compute SHA-256 of `build/libs/mob_variants_brs-1.0.0.jar` (built artifact)
  2. Compute SHA-256 of `run/mods/mob_variants_brs-1.0.0.jar` (runtime copy) — **must match**
  3. Verify JAR manifest contains `Fabric-Minecraft-Version: 26.1.2`, `Fabric-Loader-Version: 0.19.5`
  4. Verify JAR contains `data/mob_variants_brs/variants/ice_zombie.json` with expected content
  5. Record commit hash (`git rev-parse HEAD`) alongside JAR hash
- **Limitation**: Identical hashes prove byte equality, not that the JAR was built *from* the current source without contamination. If exact correspondence cannot be proven, a future authorized build phase must produce a new artifact with recorded provenance.

### 2.2 Standalone Dedicated Server Architecture
**Decision**: Convert isolated server to **true standalone Fabric server** (no dev classpath).
- **Required Components**:
  - **Fabric Loader**: 0.19.5 (server JAR: `fabric-loader-0.19.5-26.1.2-server.jar`)
  - **Java**: 25.0.2 (Temurin), same as development
  - **Fabric API**: 0.155.3+26.1.2 (JAR in `mods/`)
  - **MobVariantsBRS**: `mob_variants_brs-1.0.0.jar` in `mods/`
  - **Server Config**: `server.properties` with eligible biome, `eula.txt=true`
- **Launch Command** (replaces `start_server.bat`):
  ```bat
  java -Xmx2G -jar fabric-loader-0.19.5-26.1.2-server.jar nogui
  ```
- **Directory Structure**:
  ```
  C:\modding_mc\isolated_test\brs_live_test_server\
  ├── fabric-loader-0.19.5-26.1.2-server.jar
  ├── mods/
  │   ├── fabric-api-0.155.3+26.1.2.jar
  │   └── mob_variants_brs-1.0.0.jar
  ├── server.properties
  ├── eula.txt
  ├── snowy_test_world/          (new, uniquely named)
  ├── logs/
  └── start_server.bat           (rewritten)
  ```
- **Dependency Sources**: All JARs must be copied from verified local artifacts (`run/mods/`, Gradle cache, or Maven local). No Gradle execution during test phase.

### 2.3 Isolated Client Architecture
**Decision**: Create **standalone Fabric client** using official launcher profile or manual `.minecraft` structure.
- **Required Components**:
  - **Fabric Loader**: 0.19.5 for Minecraft 26.1.2
  - **Java**: 25.0.2 (Temurin)
  - **Fabric API**: 0.155.3+26.1.2
  - **MobVariantsBRS**: `mob_variants_brs-1.0.0.jar`
- **Directory Structure**:
  ```
  C:\modding_mc\isolated_test\brs_live_test_client\
  ├── .minecraft/
  │   ├── mods/
  │   │   ├── fabric-api-0.155.3+26.1.2.jar
  │   │   └── mob_variants_brs-1.0.0.jar
  │   ├── options.txt
  │   ├── servers.dat (localhost:25566)
  │   └── logs/
  └── launcher_profiles.json (or equivalent)
  ```
- **Launch Mechanism**: Use `--quickPlayMultiplayer=localhost:25566` (per `build.gradle:30-34`) or manual server entry in `servers.dat`.

### 2.4 Biome Testing Strategy
**Decision**: Use **flat world with explicit `generator-settings`** for both biome cases, creating **two separate worlds** in the isolated server directory.

| Test Case | Biome | World Name | Generator Settings |
|-----------|-------|------------|-------------------|
| **Eligible** | `minecraft:snowy_plains` | `snowy_test_world` | `{"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:snow_block","height":1}],"biome":"minecraft:snowy_plains"}` |
| **Ineligible** | `minecraft:plains` | `plains_test_world` | `{"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}` |

- **Compatibility Evidence**: Main project `run/server.properties:21` uses identical flat-world `generator-settings` format (escaped JSON) for `minecraft:plains` on Minecraft 26.1.2.
- **Verification**: Before test, confirm biome at spawn via `/debug` or `/execute in minecraft:overworld run data get block <pos> biome` (requires operator).
- **No World Regeneration**: Each world created once; server `level-name` switched via `server.properties` edit + server restart. Existing worlds never deleted.

### 2.5 World Isolation Strategy
**Decision**: **Uniquely named test worlds** in isolated server directory; **preflight verification** before any server start.

**Preflight Checks (All Must Pass)**:
1. **Effective directories**: Confirm `server.properties:level-name` → `worlds/<level-name>/` resolves inside isolated server directory only.
2. **Name collision**: Verify `snowy_test_world/` and `plains_test_world/` do **not** exist.
3. **Process ownership**: Manual confirmation that no Java/Minecraft process uses isolated server directory or ports 25566/25576.
4. **Lock files**: `session.lock` presence recorded but **not** treated as conclusive; manual verification required.
5. **Port availability**: Confirm 25566 (server), 25576 (RCON) not in use by other services.

**Stop Condition**: If any check fails → `BLOCKED`. No automatic cleanup, regeneration, or reuse of existing worlds.

---

## 3. Prerequisites

### 3.1 Repository-Relative Paths (Must Exist)
| Path | Purpose |
|------|---------|
| `src/main/resources/data/mob_variants_brs/variants/ice_zombie.json` | Variant definition (eligible biomes) |
| `src/main/resources/assets/mob_variants_brs/textures/entity/zombie/zombie_ice.png` | Variant texture |
| `build/libs/mob_variants_brs-1.0.0.jar` | Built mod artifact (provenance source) |
| `run/mods/mob_variants_brs-1.0.0.jar` | Runtime mod artifact (must match built) |
| `gradle.properties` | Version declarations |
| `fabric.mod.json` | Mod metadata, dependencies |
| `build.gradle` | Launch arguments, toolchain |

### 3.2 External Resources (Must Be Available Locally)
| Resource | Version | Source | Verification |
|----------|---------|--------|--------------|
| Fabric Loader Server JAR | 0.19.5 for 26.1.2 | `~/.gradle/caches/...` or manual download | SHA-256 match |
| Fabric API JAR | 0.155.3+26.1.2 | `run/mods/` or Gradle cache | Present in runtime logs |
| Java Runtime | 25.0.2 Temurin | `C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot\` | `java -version` |

### 3.3 Environmental Facts (Must Be Confirmed)
- No active process holds `C:\modding_mc\isolated_test\brs_live_test_server\` or ports 25566/25576
- No active process holds `C:\modding_mc\isolated_test\brs_live_test_client\`
- User authorizes population of empty client directory
- User authorizes rewrite of `start_server.bat` and `server.properties` in isolated server

---

## 4. Staged Future Procedure

### Stage 0: Preflight Verification (Read-Only, Current Session)
- [ ] Verify repository state (branch, HEAD, clean tree) — **DONE**
- [ ] Verify JAR provenance (SHA-256 match, manifest, commit hash) — **PENDING**
- [ ] Verify isolated directories empty/safe — **PARTIAL** (client empty, server not standalone)
- [ ] Verify no process conflicts — **BLOCKED** (requires manual confirmation)
- [ ] Verify port availability — **BLOCKED** (requires manual confirmation)

### Stage 1: Environment Preparation (Requires Authorization)
1. **Client Setup**:
   - Create `.minecraft/` structure in isolated client directory
   - Copy `fabric-api-0.155.3+26.1.2.jar` + `mob_variants_brs-1.0.0.jar` to `.minecraft/mods/`
   - Create minimal `options.txt`, `servers.dat` (localhost:25566)
   - **Gate**: User confirms client directory safe to populate

2. **Server Conversion**:
   - Obtain Fabric Loader server JAR (0.19.5, 26.1.2)
   - Copy Fabric API JAR + mod JAR to `mods/`
   - Rewrite `start_server.bat` to standalone launch command
   - Create `eula.txt` with `eula=true`
   - **Gate**: User confirms server directory safe to modify

3. **World Creation**:
   - Create `snowy_test_world/` via server start with eligible `generator-settings`
   - Create `plains_test_world/` via server start with ineligible `generator-settings`
   - **Gate**: Preflight checks pass; worlds do not exist

### Stage 2: Launch Readiness (Read-Only Verification)
- [ ] Client launches, shows Fabric mods loaded, connects to localhost:25566
- [ ] Server starts, loads mods, shows variant definitions loaded
- [ ] Both logs show no errors for mod initialization

### Stage 3: Runtime Checks (Execution Phase)
| Check | Method | Expected Evidence |
|-------|--------|-------------------|
| **1. Mod Initialization** | Server/client logs | `mob_variants_brs 1.0.0` loaded, entrypoints registered |
| **2. Variant Definition Loading** | Server `debug.log` | `Loaded N variant definitions from N candidate files`; `ice_zombie` variant ID present |
| **3. Eligible Biome Selection** | Spawn zombie in `snowy_test_world` | Variant texture attached (`zombie_ice.png`), attributes applied (if any) |
| **4. Ineligible Biome Fallback** | Spawn zombie in `plains_test_world` | No variant texture attached (vanilla texture), no variant attributes |
| **5. No-Match Behavior** | Spawn zombie in third biome (e.g., desert) | Same as ineligible — vanilla fallback |
| **6. Server-Authoritative Sync** | Client observes zombie texture | Client renderer uses `zombie_ice.png` in eligible biome; vanilla in ineligible |
| **7. Texture Rendering** | Screenshot/video capture | Visual confirmation of ice texture on zombie in snowy biome |
| **8. Attribute Behavior** | Server log `/attribute` command | `max_health=40.0`, `movement_speed=0.3` applied only when variant selected |

### Stage 4: Shutdown & Evidence Collection
- Stop server gracefully (`/stop`)
- Archive logs from both client and server
- Archive screenshots
- **No world deletion or modification**

---

## 5. Validation Matrix

| Criterion | Type | Required Evidence | Status | Notes |
|-----------|------|-------------------|--------|-------|
| **Source/Config Checks** (Static) |
| Repository on correct branch | Config | `git status` output | PASS | `feat/variant-state-layer`, clean |
| JAR provenance verifiable | Config | SHA-256 match, manifest, commit hash | NOT RUN | Requires computation |
| Variant definition correct | Config | `ice_zombie.json` biomes list | PASS | 10 snowy biomes, no attributes |
| Texture asset exists | Config | File exists, non-zero | PASS | `zombie_ice.png` (598 bytes) |
| Dependency versions declared | Config | `gradle.properties`, `fabric.mod.json` | PASS | All versions match runtime logs |
| **Runtime Checks** (Require Execution) |
| Mod initializes in isolated env | Runtime | Logs show `mob_variants_brs 1.0.0` loaded | NOT RUN | Requires standalone env |
| Variant definitions load | Runtime | `Loaded N variant definitions` log | NOT RUN | Requires isolated server |
| Eligible biome selects variant | Runtime | Variant texture + attributes on spawn | NOT RUN | Requires `snowy_test_world` |
| Ineligible biome falls back | Runtime | Vanilla texture, no variant attributes | NOT RUN | Requires `plains_test_world` |
| Server-client sync works | Runtime | Client renderer shows variant texture | NOT RUN | Requires connected client |
| Texture renders correctly | Runtime | Screenshot shows ice zombie | NOT RUN | Requires client + eligible biome |
| Attributes applied correctly | Runtime | Server log shows attribute values | NOT RUN | Requires variant with attributes |

---

## 6. Known Limitations & Unresolved Blockers

| Blocker | Why Unresolved | Evidence Needed to Resolve |
|---------|----------------|----------------------------|
| **Process/lock ownership unknown** | Tool restrictions prevent `tasklist`/`Get-Process`; `session.lock` files present but inconclusive | Manual user confirmation that no process uses isolated directories or ports 25566/25576 |
| **Second variant definition source unknown** | Logs show "2 variant definitions loaded" but only `ice_zombie.json` found in source/build | Inspect JAR contents for additional `data/mob_variants_brs/variants/*.json`; check if Fabric API or another mod contributes |
| **Standalone Fabric server JAR not verified locally** | Only dev-environment launch tested (`start_server.bat` uses loom-cache) | Locate `fabric-loader-0.19.5-26.1.2-server.jar` in Gradle cache or download; verify SHA-256 |
| **Flat world `generator-settings` schema for 26.1.2** | Main project uses escaped JSON format; proposed format unverified for snowy biome | Test generation in isolated server (requires Stage 1); or consult Minecraft 26.1.2 wiki/vanilla docs |
| **Client launch mechanism** | `--quickPlayMultiplayer` argument works in Loom; standalone client profile untested | Create minimal launcher profile or `servers.dat`; verify connection |
| **JAR provenance gap** | Existing JARs match each other but build not reproduced in this session | Authorized rebuild with recorded commit hash + JAR hash; or accept current JAR with documented limitation |

---

## 7. Exactly One Recommended Next Action

**Action**: **Request user confirmation on three safety prerequisites (read-only, no system changes):**

1. `C:\modding_mc\isolated_test\brs_live_test_client` can be safely populated with a standalone Fabric client configuration (currently empty).
2. `C:\modding_mc\isolated_test\brs_live_test_server` can be safely modified for standalone operation: replace `start_server.bat` with standard Fabric server launch using `mods/mob_variants_brs-1.0.0.jar`, update `server.properties` for snowy biome, and create new world directories `snowy_test_world/` and `plains_test_world/`.
3. No critical processes are known to be using the isolated directories or ports 25566 (server) / 25576 (RCON) that would interfere with testing.

**Justification**:
- Addresses top three `BLOCKED`/`NOT RUN` criteria: environment isolation, mod loading in isolation, variant-definition loading in isolation.
- Isolated client being empty is the most fundamental blocker — no client-side testing possible.
- Isolated server's dev-classpath dependency prevents true isolation validation.
- Process/lock uncertainties must be resolved before any world creation or server launch.
- This is the smallest read-only action that unblocks Stage 1 preparation.

**Status**: Awaiting user confirmation. No files, worlds, configurations, or processes were modified during this planning phase.