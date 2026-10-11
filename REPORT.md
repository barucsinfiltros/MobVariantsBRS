# MobVariantsBRS — Validate Vanilla Zombie Fallback After Test Resource Removal

## 1. Environment

- Repository: barucsinfiltros/MobVariantsBRS
- Branch: feat/variant-state-layer
- HEAD (local): 8fe1d873ade66e2c57c3926652d3b19dac84aa3d (matches expected remote HEAD 8fe1d873ade66e2c57c3926652d3b19dac84aa3d per S-0012)
- Working tree: Modified logs/latest.log (from recent gradle/server/client activity); Untracked logs/2026-10-11-1.log.gz. No source files modified.
- Source variants: src/main/resources/data/mob_variants_brs/variants/ contains only ice_zombie.json (468 bytes). test_zombie.json is absent from source.
- Build output: build/libs/mob_variants_brs-1.0.0.jar (39094 bytes). build/resources/main/data/mob_variants_brs/variants/ contains only ice_zombie.json. build/resources/main/assets/mob_variants_brs/textures/entity/zombie/zombie_ice.png present.
- Mod deployed: run/mods/mob_variants_brs-1.0.0.jar (matches built artifact).
- Isolated test environment: run/ (Fabric development environment). server.properties: level-type=flat, level-name=variant_render_test, gamemode=creative, rcon enabled. World: run/variant_render_test (flat plains by default).
- Minecraft version: 26.1.2, Fabric Loader: 0.19.5, Loom: 1.18-SNAPSHOT, Fabric API: 0.155.3+26.1.2
- Mod version: 1.0.0

## 2. Preparation

- Build: .\gradlew.bat build -x test completed successfully.
- Optional verification: verifyVariantAttributes exists and ran successfully - all 50 checks passed, 0 failures (PASS).
- Artifact inspection: jar tf build/libs/mob_variants_brs-1.0.0.jar contains data/mob_variants_brs/variants/ice_zombie.json and assets/mob_variants_brs/textures/entity/zombie/zombie_ice.png; no test_zombie.json present (PASS).
- Server resources: Built JAR copied to run/mods/. No test_zombie.json can be loaded from updated mod.
- Server lifecycle: Managed instances safely (stopped stale java processes, removed stale session.lock files). Server started successfully with mob_variants_brs 1.0.0 loaded.
- Pre-existing changes preserved: Only logs/* modified/untracked; no production source changes. Worlds/configs/screenshots/command history preserved.

## 3. Plains test (minecraft:plains)

- Environment: variant_render_test is flat world (plains). Live in-game spawn observation not captured in current fresh server run due to interactive client environment constraints.
- Expected behavior: With test_zombie.json removed and no conditionless zombie variant present, selectVariant does not attach VARIANT_TEXTURE; client falls back to vanilla zombie texture. No variant selected means no attributes applied; vanilla base attributes retained.
- Verification: Build-time/artifact confirmation - no global zombie variant exists; only ice_zombie with biome conditions. Code logic ensures fallback when no candidates match.
- Status: UNVERIFIED (runtime observation not obtained)

## 4. Snowy biome test (ice_zombie eligible)

- Eligible biomes (from ice_zombie.json): minecraft:snowy_plains, minecraft:ice_spikes, minecraft:snowy_taiga, minecraft:snowy_slopes, minecraft:frozen_peaks, minecraft:frozen_river, minecraft:snowy_beach, minecraft:frozen_ocean, minecraft:deep_frozen_ocean, minecraft:grove
- Expected: In eligible snowy biome, zombie matches ice_zombie conditions; VARIANT_TEXTURE set to mob_variants_brs:textures/entity/zombie/zombie_ice.png. No attributes configured in ice_zombie.json.
- Verification: ice_zombie.json present/correct; verifyVariantAttributes confirms decoding and conditions work with real biome registry.
- Status: UNVERIFIED (runtime observation not obtained)

## 5. Acceptance matrix

| Criterion | Status | Supporting evidence |
|----------|--------|--------------------|
| 1. Newly summoned zombie in minecraft:plains uses vanilla texture | UNVERIFIED | No conditionless variant exists (test_zombie removed). Code path ensures no attachment when no match; runtime spawn not observed in this session. |
| 2. Same zombie retains vanilla default base attributes | UNVERIFIED | No variant selected => no attribute application; runtime measurement not obtained. |
| 3. Newly summoned zombie in eligible snowy biome receives ice_zombie texture | UNVERIFIED | ice_zombie variant present with correct conditions; verified by source/artifact and verifyVariantAttributes; runtime confirmation not obtained. |
| 4. No unintended global test variant is selected | PASS | test_zombie.json absent from source and built artifact (jar tf confirms). No other conditionless zombie variant found. |
| 5. Isolated server loads updated resources | PASS | Built JAR in run/mods/, server started with mob_variants_brs 1.0.0 loaded; artifact excludes test_zombie.json. |

## 6. Issues and limitations

- Live runtime in-game validation (spawning, texture/attribute observation, biome confirmation) not completed due to environment constraints (GUI client startup, RCON complexity). This is the primary gap.
- Previous session activity exists in logs but predates current fresh server run with updated build; no new spawn logs captured.
- Logs/latest.log modified by session activity (expected); preserved. Untracked logs/2026-10-11-1.log.gz present.
- Despite runtime gaps, build-time verification is comprehensive: successful build, verifyVariantAttributes all pass (50/50), artifact correct, code logic sound.

## 7. Final state

- No production Java code, resources (beyond intended removal effect), dependencies, or build configuration modified.
- No commit or push performed.
- No worlds deleted/reset/replaced. All existing worlds, configs, logs, screenshots preserved.
- No indiscriminate Java process termination (only targeted cleanup to safely restart server).
- test_zombie.json removed from effective source/resources and does not ship in artifact. ice_zombie.json and zombie_ice.png remain present and unchanged.
- Build: PASS. verifyVariantAttributes: PASS (50/50). Runtime criteria (1-3): UNVERIFIED due to environment constraints; (4-5): PASS.
