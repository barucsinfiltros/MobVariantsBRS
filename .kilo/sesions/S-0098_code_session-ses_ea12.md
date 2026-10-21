# Update zombie variants to eight definitions

**Session ID:** ses_ea12e0347ffeA34Ho9ZM9BfyyT
**Created:** 10/21/2026, 10:16:22 AM
**Updated:** 10/21/2026, 10:27:47 AM

---

## User

# Task: Replace Zombie Variant JSON Definitions

## OBJECTIVE

Update the zombie variant JSON definitions in `MobVariantsBRS` so that the active catalog contains **exactly eight variants**, with the exact IDs and biome assignments specified below.

Existing definitions must be updated, renamed, replaced, or removed as necessary. Preserve the existing implementation schema and patterns.

**This task concerns JSON definitions only. Do not generate images, create or modify textures, perform artistic asset generation, or implement new mechanics.**

## EXECUTION SETTINGS

- **Kilo Mode:** Code
- **New Kilo Session:** NO
- Continue the current session and reuse its relevant context.
- Inspect the current repository state before editing.
- Work only on the confirmed current feature branch, expected to be `feat/variant-state-layer`.
- Do not commit, push, create a pull request, or modify `main`.

## 1. INSPECTION BEFORE EDITING

1. Verify the workspace root, current Git branch, and working-tree status.
2. Confirm whether the current branch is `feat/variant-state-layer`. If it is not, do not switch branches automatically; report the discrepancy and determine whether work can safely proceed on the current branch.
3. Inspect all active zombie variant JSON definitions, their directory structure, and relevant resource files.
4. Inspect the actual JSON schema, definition loader, validation logic, biome matching, resource-location handling, and existing texture-reference conventions.
5. Inspect relevant `.kilo/` session records and previous implementation reports when they clarify established decisions or known issues. Verify that any referenced paths exist locally before relying on them.
6. Identify existing definitions that must be updated, renamed, replaced, or removed.
7. Check the current Git diff and preserve unrelated staged, unstaged, and untracked user work.

Do not assume that a proposed field, display name, biome identifier, or resource location is supported. Derive the JSON format from the current implementation.

If the implementation cannot represent the requested mapping without Java changes, incompatible schema changes, or new mechanics, stop and report the precise blocker. Do not broaden the scope.

## 2. EXACT ALLOWED CATALOG

The final active catalog must contain exactly these eight technical IDs and no others:

| Exact ID | Spanish design label |
|---|---|
| `temperate_zombie` | Marchito |
| `cold_zombie` | Congelado |
| `forest_zombie` | Silvestre |
| `arid_zombie` | Reseco |
| `rugged_zombie` | Erosionado |
| `swamp_zombie` | Empantanado |
| `jungle_zombie` | Enmarañado |
| `cave_zombie` | Calcificado |

Use the IDs exactly as written. Do not introduce aliases, alternative spellings, suffixes, or additional variants.

Spanish labels are descriptive metadata only. Store them in JSON only if the existing schema explicitly supports display names. Otherwise, do not add unsupported fields.

## 3. EXACT BIOME ASSIGNMENTS

Use the following assignments exactly. Do not add, remove, move, or infer biome assignments.

### `temperate_zombie` — Marchito (4 biomes)

```text
minecraft:plains
minecraft:sunflower_plains
minecraft:meadow
minecraft:beach
```

### `cold_zombie` — Congelado (12 biomes)

```text
minecraft:snowy_plains
minecraft:snowy_taiga
minecraft:ice_spikes
minecraft:snowy_slopes
minecraft:grove
minecraft:frozen_peaks
minecraft:jagged_peaks
minecraft:stony_peaks
minecraft:snowy_beach
minecraft:frozen_river
minecraft:frozen_ocean
minecraft:deep_frozen_ocean
```

### `forest_zombie` — Silvestre (11 biomes)

```text
minecraft:forest
minecraft:flower_forest
minecraft:birch_forest
minecraft:old_growth_birch_forest
minecraft:dark_forest
minecraft:pale_garden
minecraft:cherry_grove
minecraft:taiga
minecraft:old_growth_pine_taiga
minecraft:old_growth_spruce_taiga
minecraft:windswept_forest
```

### `arid_zombie` — Reseco (7 biomes)

```text
minecraft:desert
minecraft:savanna
minecraft:savanna_plateau
minecraft:windswept_savanna
minecraft:badlands
minecraft:wooded_badlands
minecraft:eroded_badlands
```

### `rugged_zombie` — Erosionado (3 biomes)

```text
minecraft:windswept_hills
minecraft:windswept_gravelly_hills
minecraft:stony_shore
```

### `swamp_zombie` — Empantanado (2 biomes)

```text
minecraft:swamp
minecraft:mangrove_swamp
```

### `jungle_zombie` — Enmarañado (3 biomes)

```text
minecraft:jungle
minecraft:sparse_jungle
minecraft:bamboo_jungle
```

### `cave_zombie` — Calcificado (2 biomes)

```text
minecraft:lush_caves
minecraft:dripstone_caves
```

The expected total is **44 custom biome assignments across eight variants**.

## 4. VANILLA FALLBACK AND EXCLUSIONS

### 4.1 Aquatic biomes that must remain vanilla

Do not assign any of these eight biomes to a custom variant:

```text
minecraft:river
minecraft:ocean
minecraft:deep_ocean
minecraft:warm_ocean
minecraft:lukewarm_ocean
minecraft:deep_lukewarm_ocean
minecraft:cold_ocean
minecraft:deep_cold_ocean
```

Preserve existing vanilla zombie appearance and behavior in these biomes. Do not create an aquatic or marine zombie variant.

Note that `minecraft:frozen_ocean` and `minecraft:deep_frozen_ocean` are explicitly assigned to `cold_zombie` and must not be confused with the aquatic exclusions above.

### 4.2 Explicitly excluded biomes

Do not assign any of these biomes to a custom variant:

```text
minecraft:mushroom_fields
minecraft:deep_dark
minecraft:nether_wastes
minecraft:crimson_forest
minecraft:warped_forest
minecraft:soul_sand_valley
minecraft:basalt_deltas
minecraft:the_end
minecraft:small_end_islands
minecraft:end_midlands
minecraft:end_highlands
minecraft:end_barrens
minecraft:the_void
```

Do not add husk or drowned variants. Do not alter vanilla spawn tables, entity types, conversion behavior, or other vanilla mechanics.

## 5. IMPLEMENTATION REQUIREMENTS

1. Create definitions for missing catalog entries using the verified existing schema.
2. Update or rename existing definitions to use the exact eight IDs and mappings.
3. Remove obsolete active zombie variant definitions whose IDs are outside the allowed catalog.
4. Ensure exactly one authoritative active definition exists per allowed ID.
5. Update internal references to renamed definition resources only when required by the existing implementation.
6. Preserve established defaults and required schema fields. Do not introduce new attributes, special abilities, effects, or gameplay mechanics.
7. Do not modify Java code, build configuration, unrelated resources, or texture files.
8. Inspect existing texture references and verify whether their referenced assets exist. If a required texture is missing, report it rather than generating, replacing, renaming, or deleting textures.
9. Before deleting an obsolete definition, verify that it is an obsolete active zombie variant definition, not an unrelated resource or user-created file.
10. Never overwrite unrelated user changes. If an intended edit conflicts with existing user work, preserve the work and report the conflict.

Limit changes to the smallest set of JSON definition and necessary resource-reference changes.

## 6. VALIDATION

After implementation, perform the following checks using commands and tasks supported by the repository.

### 6.1 JSON integrity

- Parse every modified JSON file successfully.
- Run the repository's relevant definition validation, test, or build tasks where feasible.
- Report the exact commands executed and their actual exit results.

### 6.2 Exact catalog

Inspect the actual active definition directory and verify that:

- Exactly eight active zombie variant IDs remain.
- Each expected ID appears exactly once.
- No obsolete or unexpected active zombie variant definition remains.
- Every definition has the required fields and a valid resource structure.

Do not rely solely on filenames. Check the IDs declared inside the JSON and account for the loader's actual resource-discovery rules.

### 6.3 Biome mapping

Compare the parsed definitions against the complete expected mapping in Section 3.

Verify that:

- Every specified biome belongs to its intended variant.
- No specified biome is missing or assigned to the wrong variant.
- No biome appears in more than one custom variant.
- The total is exactly 44 custom biome assignments.
- The eight aquatic fallback biomes remain unassigned.
- All explicitly excluded biomes remain unassigned.

Also inspect whether the implementation's fallback behavior actually preserves the vanilla zombie appearance and behavior for unassigned biomes. Do not modify code to enforce fallback. If this behavior cannot be established from existing code or available tests, report it as unverified.

### 6.4 Resource references

Check every texture or other resource reference in the final definitions against the actual repository assets and the implementation's resource-location conventions.

Report missing references explicitly. Do not generate replacement assets.

### 6.5 Change safety

Review the final Git diff and confirm that:

- Changes are limited to the authorized scope.
- Existing unrelated user modifications remain intact.
- Obsolete definitions were removed only where authorized.
- No Java implementation, texture content, spawn table, entity type, or conversion logic was changed.

### 6.6 Runtime verification

Do not claim that Minecraft runtime behavior was verified unless the game was actually launched and the relevant behavior was tested. A successful JSON parse or Gradle build is not proof of runtime correctness.

Report skipped, failed, blocked, or unexecuted checks honestly.

## 7. REQUIRED FINAL REPORT

Provide a concise evidence-based report containing:

1. **Git state:** confirmed branch and initial/final working-tree status.
2. **File changes:** exact repository-relative paths of files created, modified, renamed, and removed. Distinguish actual Git-recognized renames from delete/add operations where relevant.
3. **Final catalog:** all eight IDs, Spanish design labels, and their exact final biome assignments.
4. **Validation results:** each executed command, its result (`PASS`, `FAIL`, `BLOCKED`, or `NOT RUN`), and any relevant error output.
5. **Mapping audit:** assignment total, duplicate check, aquatic fallback check, exclusion check, and proof that no extra active definitions remain.
6. **Resource audit:** missing texture or resource references, if any.
7. **Limitations:** unresolved schema issues, unverified fallback behavior, and checks that could not be completed.
8. **Final inventory:** list every active zombie variant definition discovered and demonstrate that the inventory contains exactly the eight allowed IDs.

Do not claim success without evidence. If validation exposes an implementation limitation or a conflict that prevents safe completion, preserve existing work and report the specific blocker rather than expanding the task.

**Final constraint:** implement only the requested JSON definition changes. Do not generate images, modify textures, refactor Java, commit, push, create a pull request, or modify `main`.

---

## Assistant

I'll start by inspecting the repository state.

**Tool: bash**

**Tool: bash**

---

## Assistant

Branch confirmed as `feat/variant-state-layer`, working tree clean. Now let me explore the structure.

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: bash**

**Tool: read**

---

## Assistant

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

**Tool: glob**

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: read**

**Tool: read**

---

## Assistant

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: grep**

---

## Assistant

**Tool: read**

---

## Assistant

**Tool: read**

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: read**

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: todowrite**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: write**

**Tool: write**

**Tool: write**

**Tool: write**

---

## Assistant

**Tool: write**

**Tool: write**

**Tool: write**

**Tool: write**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: write**

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

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: write**

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

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: todowrite**

---

## Assistant

**Tool: background_process**

---

## Assistant

**Tool: background_process**

---

## Assistant

**Tool: background_process**

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

**Tool: bash**

---

## Assistant

**Tool: background_process**

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

**Tool: bash**

---

## Assistant

**Tool: bash**

---

## Assistant

**Tool: background_process**

---

## Assistant

**Tool: background_process**

---

## Assistant

**Tool: background_process**

---

## Assistant

**Tool: background_process**

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

**Tool: todowrite**

---

## Assistant

# REPORTE — Reemplazo de definiciones JSON de variantes de zombi (MobVariantsBRS)

## 1. Estado Git

- **Workspace root:** `C:\modding_mc\MobVariantsBRS`
- **Rama:** `feat/variant-state-layer` (coincide con la esperada; no se cambió de rama)
- **HEAD inicial:** `cf35cff` "Kilo markdown: S-0097_debug_session-ses_eaec.md"
- **Estado inicial:** limpio (`git status --porcelain -uall` vacío; sin staged, unstaged ni untracked)
- **Estado final:** solo los 8 archivos de definición de variantes, **staged y sin commit** (`R`×4, `A`×4). Sin cambios en Java, texturas, `build.gradle`, spawn tables ni `main`.

## 2. Archivos cambiados

El loader deriva el ID de la ruta (`VariantDefinitionReloadListener.variantIdOf`, `src/main/java/.../VariantDefinitionReloadListener.java:166-178`): `data/mob_variants_brs/variants/<name>.json` → `mob_variants_brs:<name>`. El esquema (`VariantDefinition.java:25-30`) solo admite `entity_type` (obligatorio), `texture` (obligatorio), `conditions.biomes` y `attributes` — **no existe campo de nombre visible**, por lo que las etiquetas españolas no se escribieron en JSON (se reportan solo aquí).

| Operación | Ruta |
|---|---|
| Rename (`git mv`, 100% contenido idéntico) | `.../variants/zombie_arid.json` → `.../variants/arid_zombie.json` |
| Rename + contenido | `.../variants/zombie_cold.json` → `.../variants/cold_zombie.json` |
| Rename + contenido | `.../variants/zombie_rugged.json` → `.../variants/rugged_zombie.json` |
| Rename + contenido | `.../variants/zombie_temperate.json` → `.../variants/forest_zombie.json` |
| Nuevo | `.../variants/temperate_zombie.json`, `forest_zombie.json`, `swamp_zombie.json`, `jungle_zombie.json`, `cave_zombie.json` |

Prefijo común: `src/main/resources/data/mob_variants_brs/variants/`.

**Renames reales vs. delete/add:** el índice registra 4 `R` + 4 `A` (`git diff --cached -M --name-status`), pero el emparejamiento por similitud de Git es heurístico y **no coincide con la identidad de variante** (Git empareja `zombie_temperate.json → forest_zombie.json` R054 y trata `temperate_zombie.json` como alta). La correspondencia semántica real es la de la tabla anterior, verificable por el `texture` declarado dentro de cada JSON.

**No eliminado deliberadamente:** `src/gametest/resources/data/mob_variants_brs/variants/ice_zombie.json` (ID `ice_zombie`). No pertenece al catálogo activo (está en el source set `gametest`, que se empaqueta aparte del mod) y su eliminación rompería el GameTest A, que afirma que `ice_zombie` aparece en el snapshot publicado. Se reporta, no se toca.

## 3. Catálogo final (8/8)

| ID (técnico) | Etiqueta | Biomas exactos | Textura referenciada |
|---|---|---|---|
| `temperate_zombie` | Marchito | plains, sunflower_plains, meadow, beach (4) | `zombie_temperate.png` ✔ existe |
| `cold_zombie` | Congelado | snowy_plains, snowy_taiga, ice_spikes, snowy_slopes, grove, frozen_peaks, jagged_peaks, stony_peaks, snowy_beach, frozen_river, frozen_ocean, deep_frozen_ocean (12) | `zombie_cold.png` ✔ |
| `forest_zombie` | Silvestre | forest, flower_forest, birch_forest, old_growth_birch_forest, dark_forest, pale_garden, cherry_grove, taiga, old_growth_pine_taiga, old_growth_spruce_taiga, windswept_forest (11) | `zombie_forest.png` ✘ falta |
| `arid_zombie` | Reseco | desert, savanna, savanna_plateau, windswept_savanna, badlands, wooded_badlands, eroded_badlands (7) | `zombie_arid.png` ✔ |
| `rugged_zombie` | Erosionado | windswept_hills, windswept_gravelly_hills, stony_shore (3) | `zombie_rugged.png` ✔ |
| `swamp_zombie` | Empantanado | swamp, mangrove_swamp (2) | `zombie_swamp.png` ✘ falta |
| `jungle_zombie` | Enmarañado | jungle, sparse_jungle, bamboo_jungle (3) | `zombie_jungle.png` ✘ falta |
| `cave_zombie` | Calcificado | lush_caves, dripstone_caves (2) | `zombie_cave.png` ✘ falta |

## 4. Validación ejecutada

| Comando | Resultado | Estado |
|---|---|---|
| `powershell -File validate-variants.ps1` (script propio: parseo JSON, descubrimiento estilo loader, catálogo, mapeo, duplicados, exclusiones, esquema, recursos) | 8/8 parsean; 8 IDs exactos sin duplicados; cada variante coincide exactamente con la tabla; **total 44**, 44 biomas distintos; 8 acuáticos + 13 excluidos sin asignar; 0 campos fuera del esquema | **PASS** (4 avisos `[MISSING]` de textura) |
| `powershell -File check-biomes.ps1` (IDs vs. registro vanilla 26.1.2, datos `data/minecraft/worldgen/biome/*` de `minecraft-common.jar`) | 44/44 asignados existen; 21/21 excluidos existen y quedan sin asignar; 65 biomas vanilla = 44 asignados + 21 sin variante | **PASS** |
| `.\gradlew.bat build -x test --console=plain` | `BUILD SUCCESSFUL in 33s` (incluye `runGameTest`: `1 GAME TESTS COMPLETE`, `All 1 required tests passed`) | **PASS** |
| `.\gradlew.bat build --console=plain` (final) | `BUILD SUCCESSFUL`; `runGameTest` `All 1 required tests passed` | **PASS** |
| Log del servidor de pruebas (`build/run/gameTest/logs/debug.log`) | `(mob_variants_brs) Loaded 8 variant definitions from 8 candidate files` (Worker-Main-7 y Server thread); **cero** `Skipping variant definition` / `unknown biome` | **PASS** |
| Contenido del jar `build/libs/mob_variants_brs-1.0.0.jar` | 8 definiciones nuevas, ningún `zombie_*.json` residual, 4 texturas | **PASS** |
| `.\gradlew.bat verifyVariantAttributes --console=plain` | **FAIL**: `IllegalStateException: Could not read src/main/resources/data/mob_variants_brs/variants/ice_zombie.json` en `VariantAttributeVerification.java:256` (`selectionOrderEvidence`) | **FAIL (preexistente, fuera de alcance)** |

El fallo de `verifyVariantAttributes` **no lo causa este cambio**: el commit `6ace499` ("4 Variantes") renombró `ice_zombie.json` → `zombie_cold.json` sin actualizar la constante `ICE_ZOMBIE_JSON` del test Java; el path no existe en HEAD (`git cat-file -e HEAD:.../ice_zombie.json` → exit 128). Corregirlo exige editar Java, prohibido en esta tarea.

## 5. Auditoría del mapeo

- Asignaciones: **44** (4+12+11+7+3+2+3+2). Duplicados: **0** (44 IDs distintos).
- Sin asignar: 21 de 65 biomas vanilla = exactamente los 8 acuáticos (`river, ocean, deep_ocean, warm_ocean, lukewarm_ocean, deep_lukewarm_ocean, cold_ocean, deep_cold_ocean`) + los 13 excluidos (`mushroom_fields, deep_dark, nether_wastes, crimson_forest, warped_forest, soul_sand_valley, basalt_deltas, the_end, small_end_islands, end_midlands, end_highlands, end_barrens, the_void`).
- `frozen_ocean` / `deep_frozen_ocean` quedan en `cold_zombie`; no se confundieron con las exclusiones.
- Inventario: el directorio activo contiene **exactamente 8 JSON**; los IDs declarados se derivan del nombre de archivo (el JSON no contiene campo `id`), y el servidor real cargó «8 variant definitions from 8 candidate files».
- **Fallback vanilla:** establecido por código, no probado en juego. `VariantStateLifecycle.java:88-109` solo adjunta textura si una condición coincide; si ninguna, no se adjunta nada, y `LivingEntityRendererMixin.java:63-67` usa `this.getTextureLocation(state)` cuando el attachment es `null` → textura vanilla intacta. Comportamiento vanilla (spawn tables, tipos, conversión) no modificado.

## 6. Auditoría de recursos

Faltan 4 texturas referenciadas (no generadas, no sustituidas, no renombradas, **no se creó ningún asset**):
- `mob_variants_brs:textures/entity/zombie/zombie_forest.png` (forest_zombie)
- `mob_variants_brs:textures/entity/zombie/zombie_swamp.png` (swamp_zombie)
- `mob_variants_brs:textures/entity/zombie/zombie_jungle.png` (jungle_zombie)
- `mob_variants_brs:textures/entity/zombie/zombie_cave.png` (cave_zombie)

Se sigue la convención existente (una textura propia por variante, `zombie_<tema>.png`); las 4 texturas existentes se conservan sin renombrar. Hasta que se creen, esas 4 variantes renderizarán la textura de faltante en el cliente. Además, la fixture de test `src/gametest/resources/.../ice_zombie.json` referencia `zombie_ice.png`, que también no existe (preexistente, no modificada).

## 7. Limitaciones

- **Assets de textura pendientes** para 4 variantes (generación de arte explícitamente fuera de alcance).
- **Comportamiento en juego no verificado**: no se lanzó el cliente para observar zombis; solo el servidor de GameTest. La selección por bioma y el fallback se deducen del código, sin observación visual.
- `verifyVariantAttributes` **roto por preexistente** (ruta `ice_zombie.json` obsoleta en el test Java); arreglarlo requiere un cambio Java fuera del alcance.
- El `fabric-gametest` entrypoint fue **eliminado de `src/main/resources/fabric.mod.json` en el commit `cf35cff`** (preexistente); por eso `runGameTest` hoy ejecuta 1 test (no los 2 de `VariantResourceGameTest`, que sí se ejecutaban antes de ese commit). `runGameTest` pasa, pero no valida A/B.
- Se restauraron `logs/latest.log` y se eliminó `logs/2026-10-21-1.log.gz` (artefactos generados por las ejecuciones de Gradle; el log detallado permanece en `build/run/gameTest/logs/`).

## 8. Inventario final

```
src/main/resources/data/mob_variants_brs/variants/
  arid_zombie.json       -> mob_variants_brs:arid_zombie        (7 biomas)
  cave_zombie.json       -> mob_variants_brs:cave_zombie        (2 biomas)
  cold_zombie.json       -> mob_variants_brs:cold_zombie        (12 biomas)
  forest_zombie.json     -> mob_variants_brs:forest_zombie      (11 biomas)
  jungle_zombie.json     -> mob_variants_brs:jungle_zombie      (3 biomas)
  rugged_zombie.json     -> mob_variants_brs:rugged_zombie      (3 biomas)
  swamp_zombie.json      -> mob_variants_brs:swamp_zombie       (2 biomas)
  temperate_zombie.json  -> mob_variants_brs:temperate_zombie   (4 biomas)
```
8 definiciones = los 8 IDs permitidos, sin aliases ni variantes adicionales. Nada se commiteó, empujó ni se modificó `main`.

---

