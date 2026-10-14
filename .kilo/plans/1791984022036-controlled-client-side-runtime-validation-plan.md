# Controlled Client-Side Runtime Validation Plan for MobVariantsBRS

## Current Environment

### Confirmed Facts
- **Repository**: `C:\modding_mc\MobVariantsBRS`
- **Current Branch**: `feat/variant-state-layer` (verified via `git branch --list`)
- **HEAD Commit**: `0e607b0b7e68bd2ef289b3bd33214ebd98e9a553` (verified via `git rev-parse HEAD`)
- **Working Tree Status**: Clean (no uncommitted changes, verified via `git status --porcelain`)
- **Mod ID**: `mob_variants_brs` (from `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java`)
- **Minecraft Version**: 26.1.2 (confirmed from logs)
- **Fabric Loader Version**: 0.19.5 (confirmed from logs)
- **Mod Loading Status**: Successfully loads (confirmed by "Hello Fabric world!" logs in both client and server logs)
- **Variant Definitions**: Exactly one variant definition exists: `ice_zombie.json` (in `src/main/resources/data/mob_variants_brs/variants/`)
- **Ice Zombie Biome Eligibility**: Eligible in these biomes:
  - `minecraft:snowy_plains`
  - `minecraft:ice_spikes`
  - `minecraft:snowy_taiga`
  - `minecraft:snowy_slopes`
  - `minecraft:frozen_peaks`
  - `minecraft:frozen_river`
  - `minecraft:snowy_beach`
  - `minecraft:frozen_ocean`
  - `minecraft:deep_frozen_ocean`
  - `minecraft:grove`
  (verified from `src/main/resources/data/mob_variants_brs/variants/ice_zombie.json`)
- **Ice Zombie Texture**: Uses `mob_variants_brs:textures/entity/zombie/zombie_ice.png` (file verified to exist)
- **Attribute Modifications**: Ice zombie variant defines NO custom attributes (no "attributes" section in variant JSON)
- **Variant Selection Mechanics**: 
  - Server-authoritative selection during entity load (`VariantStateLifecycle.selectVariant`)
  - Variant texture synchronized to clients via `VariantAttachments.VARIANT_TEXTURE`
  - Vanilla fallback when no variant matches
  - Attributes applied immediately after variant selection (`VariantStateLifecycle.applyAttributes`)
- **Rendering Approach**: No client-side mixins; relies on texture synchronization for rendering
- **Test Environments Available**:
  - Test world: `run/variant_render_test/`
  - Main world: `run/world/`
  - Evidence of recent client-server session: `run/logs/latest.log` shows player login at [17:02:32] [Server thread/INFO]

### Availability Evidence
- **Minecraft Client**: Available (evidenced by client logs in `run/logs/latest.log`)
- **Minecraft Server**: Available (evidenced by server logs in `run/logs/latest.log` showing player login, though recent startup attempts show file lock issues)
- **Mod Build**: Available (mod JAR present in `run/mods/` as evidenced by debug log showing classpath addition)
- **Required Dependencies**: Fabric Loader 0.19.5 and Minecraft 26.1.2 confirmed available
- **Variant Resources**: Present in source (`src/main/resources/data/mob_variants_brs/variants/ice_zombie.json`)

### Blockers
1. **File Lock Issues**: Recent server startup attempts fail due to file lock errors:
   ```
   java.io.IOException: El proceso no tiene acceso al archivo porque otro proceso tiene bloqueada una parte del archivo
   ```
   This suggests another process may be holding locks on critical files (likely log files or world data).

2. **No Variant Loading Evidence**: Despite checking available logs (`latest.log`, `debug.log`, `server_stdout.log`, `server_stderr.log`), there are no debug messages indicating:
   - Variant definitions loaded successfully
   - Variant selection occurring
   - Texture synchronization happening

3. **Unclear Test Environment State**: 
   - Test world (`run/variant_render_test/`) and main world (`run/world/`) do not contain variant definitions in their data directories
   - Variant definitions are loaded from mod resources via SERVER_DATA resource manager, not world data
   - Unclear if test worlds were created with the mod active

## Concise Validation Plan

### Scenario A: Vanilla Zombie Fallback
**Objective**: Verify that zombies spawn with vanilla fallback in biomes ineligible for ice_zombie variant.

**Initial Conditions**:
1. Located in a biome confirmed NOT in the ice_zombie eligibility list (e.g., `minecraft:plains`, `minecraft:desert`)
2. Ensure no zombie variant is attached to any entities (fresh spawns)

**Reproducible Steps**:
1. Navigate to or create a location in a non-eligible biome (verify biome via F3 debug screen)
2. Spawn a zombie using a reproducible method (e.g., zombie spawn egg, `/summon minecraft:zombie`, natural spawning in darkness)
3. Immediately after spawn, observe and record:
   - Server-side entity state (variant attachment)
   - Client-side entity state (variant attachment)
   - Rendered texture
   - Effective attributes

**Server-Side Observations Required**:
- Check that `VariantAttachments.VARIANT_TEXTURE` is NOT attached to the zombie entity
- Verify no variant definition was selected (check server logs for variant selection debug messages if available)

**Client-Side Observations Required**:
- Check that `VariantAttachments.VARIANT_TEXTURE` is NOT attached to the zombie entity
- Verify rendered texture matches vanilla zombie texture
- Confirm no custom variant texture is being used

**Evidence Sources**:
- **Server-side**: Entity attachment inspection (if debugging tools available), server logs with variant selection traces
- **Client-side**: Entity attachment inspection, visual texture confirmation, screenshot comparison
- **Attributes**: Attribute value comparison against vanilla zombie baseline (max health, attack damage, etc.)
- **Fallback Distinction**: Genuine vanilla fallback confirmed by:
  - No variant texture attached
  - Correct vanilla texture rendered
  - Attributes matching vanilla baseline
  - (vs. missing/unresolved state: would show no texture attachment but might have incorrect rendering or attributes)

**Expected Results**:
- Zombie spawns successfully
- No variant texture attached server-side or client-side
- Vanilla zombie texture rendered
- Attributes match vanilla zombie baseline for Minecraft 26.1.2
- Clear distinction from missing state: all systems functioning normally, just no variant applicable

**Failure Indicators**:
- Variant texture attached when it shouldn't be (incorrect selection)
- Wrong texture rendered
- Attributes deviate from vanilla baseline without explanation
- Entity fails to spawn or behaves abnormally

**Limitations**:
- Requires ability to inspect entity attachments (may need debugging tools or custom commands)
- Biomeme identification requires F3 screen or equivalent
- Attribute measurement requires precise tools or commands

### Scenario B: Snowy-Biome Ice Zombie
**Objective**: Verify that zombies spawn with ice_zombie variant in biomes eligible for ice_zombie.

**Initial Conditions**:
1. Located in a biome CONFIRMED by current implementation to satisfy ice_zombie eligibility (from list above)
2. Establish controlled location in verified eligible biome
3. Ensure clean state (no pre-existing zombie variants interfering)

**Reproducible Steps**:
1. Navigate to or create location in verified eligible biome (e.g., `minecraft:snowy_plains`)
2. Confirm biome eligibility via current implementation (check that biome ID is in ice_zombie.json conditions)
3. Spawn a zombie using reproducible method
4. Immediately after spawn, observe and record:
   - Selected variant ID
   - Server-side and client-side variant state
   - Rendered texture
   - Effective attributes

**Server-Side Observations Required**:
- Confirm `VariantAttachments.VARIANT_TEXTURE` is attached with value `mob_variants_brs:textures/entity/zombie/zombie_ice.png`
- Verify variant selection logic chose ice_zombie definition
- Check that no attributes were applied (since variant has no attribute modifiers)

**Client-Side Observations Required**:
- Confirm `VariantAttachments.VARIANT_TEXTURE` is attached with value `mob_variants_brs:textures/entity/zombie/zombie_ice.png`
- Verify rendered texture matches ice_zombie texture
- Confirm client received correct texture identifier from server

**Evidence Sources**:
- **Variant Selection**: Server logs showing variant selection process, entity attachment inspection
- **Client Synchronization**: Comparison of server/client attachment values, network traffic inspection
- **Texture Rendering**: Screenshot comparison with expected ice_zombie texture, visual confirmation
- **Attributes**: Measurement of base attributes (should match vanilla zombie since no modifiers defined)
- **Incorrect Selection Detection**: 
  - Wrong variant ID selected
  - Missing or incorrect texture synchronization
  - Wrong texture rendered
- **Attribute Change Detection**: Comparison against vanilla zombie baseline

**Expected Results**:
- Zombie spawns successfully in eligible biome
- Server selects ice_zombie variant and attaches correct texture identifier
- Client receives and applies same texture identifier
- Ice_zombie texture rendered correctly
- Attributes match vanilla zombie baseline (no expected changes)
- Clear detection of failures:
  - Incorrect selection: wrong variant or no variant in eligible biome
  - Sync failure: client missing texture or wrong texture
  - Rendering failure: wrong texture visible despite correct data
  - Attribute changes: unexpected deviation from vanilla baseline

**Limitations**:
- Requires biome verification per implementation (not just biome name)
- Needs ability to capture entity state at exact moment of spawn
- Attribute measurement precision may be limited by available tools

## Acceptance Matrix

| Criterion | Required Evidence | PASS | FAIL | UNVERIFIED |
|---|---|---|---|---|
| Variant Selection | Authoritative server-side entity state showing attached variant texture identifier | Confirmed expected variant texture attached (`mob_variants_brs:textures/entity/zombie/zombie_ice.png` for ice_zombie biome, none for vanilla fallback) | Confirmed incorrect variant texture attached or missing when expected | State unavailable or ambiguous (cannot inspect entity attachments) |
| Client Synchronization | Matching server/client variant texture attachment values | Client receives exactly the same texture identifier as server | Confirmed missing, incorrect, or inconsistent state | Insufficient observations to compare server and client states |
| Texture Rendering | In-game screenshot plus variant/resource correlation | Correct texture visibly rendered matching expected variant | Wrong texture rendered or reproducible rendering failure | No usable runtime observation (cannot capture or verify visuals) |
| Vanilla Fallback | Entity state and texture evidence | Expected fallback state (no variant attached) and vanilla texture rendered | Confirmed incorrect fallback (variant attached when shouldn't be or wrong texture) | Fallback cannot be established (cannot confirm absence of variant attachment or verify texture) |
| Attribute Behavior | Comparable measurements and version-appropriate baseline | Values match expected effective attributes (vanilla zombie baseline for both scenarios since ice_zombie has no modifiers) | Confirmed discrepancy between measured and expected values | Baseline or measurements insufficient (cannot measure attributes or establish baseline) |
| Environment Readiness | Current configuration and availability evidence | Required components available and compatible (mod loads, biome data accessible, entity spawning functional) | A required component confirmed unusable (mod fails to load, critical functionality broken) | Availability or compatibility unknown (cannot verify component status) |

**Matrix Notes**:
- Source inspection alone (e.g., viewing variant JSON) never qualifies a runtime criterion as PASS
- PASS requires direct runtime evidence meeting the criteria
- FAIL requires contradictory runtime evidence
- UNVERIFIED when evidence cannot be obtained safely or reliably
- For Attribute Behavior: Since ice_zombie defines no attributes, expected values are identical to vanilla zombie baseline for Minecraft 26.1.2

## Evidence Gaps and Risks

### Source-Level Findings (Confirmed via Inspection)
- ✅ Ice zombie variant definition exists with correct biome conditions
- ✅ Ice zombie texture resource exists at specified path
- ✅ Ice zombie variant defines no attribute modifications
- ✅ Variant selection logic implemented in `VariantStateLifecycle.selectVariant`
- ✅ Texture synchronization implemented via `VariantAttachments.VARIANT_TEXTURE`
- ✅ Vanilla fallback behavior implied by lack of variant attachment
- ✅ Attribute application logic exists but will apply no changes for ice_zombie (empty attributes)

### Runtime Observations (Not Yet Verified)
- ❌ Variant definitions loading successfully (no debug logs showing "Loaded X variant definitions")
- ❌ Variant selection occurring for ice_zombie in eligible biomes
- ❌ Variant selection resulting in vanilla fallback in ineligible biomes
- ❌ Client receiving correct variant texture synchronization
- ❌ Correct texture rendering client-side
- ❌ Attribute values remaining at vanilla baseline
- ❌ Proper zombie spawning in test environments

### Key Risks
1. **File Lock Issues**: Preventing server startup and corrupting logs, making runtime observation impossible
2. **Logging Level Issues**: Variant selection debugging may not be enabled, hiding runtime behavior
3. **Test Environment Misconfiguration**: Worlds may not have been created with mod active or may lack necessary biome generation
4. **Synchronization Failures**: Texture attachment may not be properly synchronized despite correct implementation
5. **Attribute Application Errors**: Potential bugs in attribute application logic despite empty attribute definitions

## Recommended Next Action

**Choose the smallest safe action that advances validation**: Read the most recent server debug log to check for variant definition loading messages.

**Rationale**: 
- This is a read-only operation that preserves all existing data
- It requires no world modification, entity spawning, or server/client interaction
- It directly addresses the most fundamental evidence gap: whether variant definitions are loading at all
- If definitions aren't loading, no amount of world testing will show variant behavior
- If definitions are loading, we can then investigate why selection isn't occurring
- The action is safe: only reading an existing log file, no risk of data corruption or unintended changes

**Specific Action**: 
Examine `run/logs/debug.log` for messages from `VariantDefinitionReloadListener` indicating successful variant definition loading, particularly looking for:
- "Loaded {} variant definitions from {} candidate files"
- Any error messages about skipping definitions or failing to publish snapshots

This action will confirm whether the variant data loading pipeline is functional, which is a prerequisite for all runtime validation.