# Plan: Safe, Isolated MobVariantsBRS Runtime-Test Environment

## Verified Environment Facts

### Repository State
- Branch: `feat/variant-state-layer` (verified via `git status`)
- HEAD: `74835b3522409a59e26d268c68d3a641c70c5019`
- Working tree: Clean (no uncommitted changes)

### Isolated Client State
- Path: `C:\modding_mc\isolated_test\brs_live_test_client`
- Status: **Empty directory** - no configuration, mods, or world data

### Isolated Server State
- Path: `C:\modding_mc\isolated_test\brs_live_test_server`
- Configuration:
  - `server.properties`: level-name=isolated_live_test, level-type=minecraft:flat, generator-settings={}
  - `start_server.bat`: References main project build artifacts (.gradle\loom-cache and build\loom-cache)
  - `mods/`: **Empty directory**
  - World: `isolated_live_test/` exists with session.lock
  - Logs show mod loaded via development environment classpath (not standalone)

### Main Project Runtime State
- Path: `C:\modding_mc\MobVariantsBRS\run\`
- Mod JAR: `mods/mob_variants_brs-1.0.0.jar` present
- Worlds:
  - `world/`: No session.lock
  - `variant_render_test/`: Has session.lock, configured as flat plains biome (ineligible for ice_zombie)
- Server port: 25565
- Isolated server port: 25566 (from server.properties)

### Variant Definition Evidence
- File: `src/main/resources/data/mob_variants_brs/variants/ice_zombie.json`
- Eligible biomes:
  - minecraft:snowy_plains
  - minecraft:ice_spikes
  - minecraft:snowy_taiga
  - minecraft:snowy_slopes
  - minecraft:frozen_peaks
  - minecraft:frozen_river
  - minecraft:snowy_beach
  - minecraft:frozen_ocean
  - minecraft:deep_frozen_ocean
  - minecraft:grove
- Ineligible biomes: Any biome not in the above list (e.g., minecraft:plains, minecraft:desert)

### Process and Lock State
- Process inspection: **BLOCKED** by tool restrictions (cannot verify running Java/Minecraft processes)
- File locks:
  - `isolated_test/brs_live_test_server/isolated_live_test/session.lock` exists
  - `run/variant_render_test/session.lock` exists
  - Cannot determine if locks indicate active processes or are stale

## Facts vs. Uncertainties

### Verified Facts
1. Repository is on correct branch with clean working tree
2. Isolated client directory exists but is empty
3. Isolated server directory exists but mods/ is empty and depends on main project build
4. Isolated server world is configured as flat with empty generator-settings (defaults to plains)
5. Ice_zombie variant definition requires snowy biomes
6. Main project runtime contains built mod JAR
7. Port isolation exists: main server 25565, isolated server 25566

### Critical Uncertainties
1. Whether isolated client can be configured independently without affecting main project
2. Whether isolated server can run with standalone mod JAR (not development classpath)
3. Whether isolated server world can be configured with eligible biome
4. Whether file locks indicate active processes that would interfere with testing
5. Whether client can connect to isolated server and receive variant data

### Manual Confirmation Requirements
1. Verification that no critical processes are using isolated directories (requires manual check since process inspection blocked)
2. Confirmation that creating new isolated client configuration won't interfere with user's existing setups
3. Approval to modify isolated server world biome configuration (would require world regeneration)

## Proposed Isolated Layout

### Client Game Directory
- **Path**: `C:\modding_mc\isolated_test\brs_live_test_client`
- **Purpose**: Dedicated Fabric client instance for testing
- **Required contents**:
  - `.minecraft` subdirectory (or direct game directory structure)
  - `mods/` containing `mob_variants_brs-1.0.0.jar`
  - `options.txt` for client configuration
  - `servers.dat` pointing to localhost:25566

### Server Runtime Directory
- **Path**: `C:\modding_mc\isolated_test\brs_live_test_server`
- **Purpose**: Dedicated server instance for testing
- **Required contents**:
  - `server.properties` with eligible biome configuration
  - `mods/` containing `mob_variants_brs-1.0.0.jar` (standalone, not dev classpath)
  - `eula.txt` (true)
  - World directory with eligible biome (e.g., `snowy_test_world/`)

### Test World
- **Path**: `C:\modding_mc\isolated_test\brs_live_test_server\snowy_test_world/`
- **Purpose**: World with eligible biome for ice_zombie variant
- **Configuration**: Flat world with snowy biome (e.g., snowy_plains)

### Evidence Storage
- **Logs**: `logs/` directories in both client and server
- **Screenshots**: `screenshots/` in client directory
- **Crash reports**: `crash-reports/` in both directories

## Staged Preparation Plan

### Stage 1: Environment Isolation (Read-only verification)
1. **Verify isolated client directory is safe to use** [Manual confirmation required]
   - Confirm no important user data exists in `C:\modding_mc\isolated_test\brs_live_test_client`
   - Verify directory is truly empty
2. **Verify isolated server directory independence** [Read-only]
   - Confirm `start_server.bat` can be modified to use standalone mod JAR
   - Verify server.properties is isolated from main project
3. **Confirm port isolation** [Read-only]
   - Verify main project uses port 25565
   - Verify isolated server configured for port 25566
   - Verify no other services conflict with these ports

### Stage 2: Client Preparation (Requires manual action - NOT part of this planning session)
1. Create client directory structure
2. Copy `mob_variants_brs-1.0.0.jar` from `run/mods/` to client `mods/`
3. Create basic `options.txt` and `servers.dat` pointing to localhost:25566
4. Verify client can launch independently (future execution step)

### Stage 3: Server Preparation (Requires manual action - NOT part of this planning session)
1. **Backup current isolated server world** [Manual confirmation required]
   - Rename `isolated_live_test/` to `isolated_live_test_backup/` if user approves
2. **Configure eligible biome world** [Manual confirmation required]
   - Modify `server.properties`:
     - `level-name=snowy_test_world`
     - `generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:snow_block","height":1}],"biome":"minecraft:snowy_plains"}`
   - Delete existing world directory to force regeneration
3. **Install standalone mod** [Manual confirmation required]
   - Copy `mob_variants_brs-1.0.0.jar` from `run/mods/` to server `mods/`
   - Remove development classpath references from `start_server.bat`
4. **Verify server can start with new configuration** (future execution step)

### Stage 4: World and Biome Preparation
1. **Eligible biome verification** [Read-only]
   - Confirm snowy_plains is in ice_variant eligible biomes list
2. **Ineligible biome verification** [Read-only]
   - Confirm plains biome is NOT in ice_variant eligible biomes list
3. **World generation approach** [Read-only]
   - Flat world with snowy_plains biome is compatible with Minecraft 26.1.2
   - No invasive commands or APIs required

### Stage 5: Safety Safeguards (Read-only planning)
1. **World protection**
   - Never modify `run/world/` or `run/variant_render_test/` without explicit authorization
   - Always use uniquely named test worlds (e.g., snowy_test_world_)
   - Never launch test against unverified world path
2. **Process safety**
   - Never treat session.lock alone as proof of active process
   - Require manual process verification before testing
   - Never attempt to terminate processes or release locks
3. **Isolation verification**
   - Confirm client and server use separate directories
   - Verify logs show connections to correct ports
   - Ensure no path overlaps with main project runtime

## World and Biome Strategy

### Eligible Biome Source
- **Evidence**: `src/main/resources/data/mob_variants_brs/variants/ice_zombie.json` lines 5-16
- **Selected biome**: `minecraft:snowy_plains` (first in list, verifiably eligible)
- **Confirmation**: Direct file evidence shows this biome is in the conditions.biomes array

### Ineligible Biome Source
- **Evidence**: Same variant definition file
- **Selected biome**: `minecraft:plains` (clearly not in the eligible list)
- **Confirmation**: Direct file evidence shows this biome is absent from conditions.biomes

### World Generation Compatibility
- **Approach**: Flat world generator with specified biome
- **Evidence**: 
  - Main project already uses flat world generator (`level-type=minecraft\:flat`)
  - generator-settings format verified in main project's server.properties
  - snowy_plains is a valid biome identifier in Minecraft 26.1.2
- **Compatibility**: Least invasive approach - uses existing world generation system

### Test Procedure
1. Client connects to server on localhost:25566
2. Player spawns in snowy_test_world (snowy_plains biome)
3. Zombie spawns → should receive ice_zombie variant (eligible biome)
4. Player travels to plains biome area (if exists) or creates plains area
5. Zombie spawns in plains → should receive regular zombie (ineligible biome fallback)

## Acceptance Matrix

| Criterion | Required Evidence | Status | Prerequisite |
|----------|-------------------|--------|--------------|
| **1. Environment isolation and world safety** | Actual runtime directories identified; client/server cannot reuse main project state; existing worlds protected | **BLOCKED** | Isolated client directory empty; isolated server depends on main project build; process/lock status unverified |
| **2. Successful mod loading** | Compatible local versions established; proposed installation arrangement supported; future runtime log can establish successful mod initialization | **BLOCKED** | Isolated client lacks mod; isolated server uses dev classpath; cannot verify mod loads in isolation |
| **3. Variant-definition loading** | Expected definition source identified; future runtime check can distinguish successful loading from missing/invalid definitions | **BLOCKED** | No isolated environment to test loading; cannot verify DefinitionReloadListener logs |
| **4. Biome selection and fallback** | Eligible/ineligible biomes identified from authoritative config; proposed test world/exercise both cases; expected behavior has observable criteria | **READY** | Biome evidence from variant definition; world generation approach verified; awaiting environment setup |
| **5. Client synchronization and texture rendering** | Proposed arrangement supports synchronization; future test can verify variant on client and observe texture; logs/visual evidence capturable separately | **BLOCKED** | No client environment; cannot verify connection or rendering |

## Risks and Limitations

1. **Process interference risk**: Blocked process inspection prevents verification that no conflicting Java/Minecraft processes are using test directories or ports
2. **World corruption risk**: If session.lock files indicate active processes, modifying worlds could corrupt data
3. **Mod version mismatch**: Using mod JAR from main project runtime may not match development state if built artifacts are stale
4. **Examples directory pollution**: Modifying isolated test directories without explicit user approval could interfere with user's workflow
5. **Biome uncertainty**: While snowy_plains is confirmed eligible, actual world generation must verify the biome is present at spawn point

## Exactly One Recommended Next Action

**Read-only verification of isolated directory safety and process state through user confirmation**

**Action**: Request user confirmation that:
1. `C:\modding_mc\isolated_test\brs_live_test_client` can be safely populated with test configuration (currently empty)
2. `C:\modding_mc\isolated_test\brs_live_test_server` can be safely modified for standalone operation (currently depends on main project build)
3. No critical processes are known to be using these directories or ports 25565-25566 that would interfere with testing

**Justification**: 
- This addresses the top three blockers from the acceptance matrix (environment isolation, mod loading, variant definition loading)
- Isolated client being empty is the most fundamental issue preventing any testing
- Isolated server's dependence on main project build prevents true isolation
- Process/lock uncertainties must be resolved before any world modification or server launch
- This is the smallest action that enables progression to environment preparation
- Read-only: only requires user confirmation, no system changes

**Status**: This action requires user input (not read-only execution) but is the necessary prerequisite for any further preparation.