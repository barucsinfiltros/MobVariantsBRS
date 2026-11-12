# Investigating vanilla freezing mechanics for cold_zombie variant

**Session ID:** ses_e32043c79ffewuoF5VtoHiZcUf
**Created:** 11/12/2026, 12:19:51 AM
**Updated:** 11/12/2026, 12:23:33 AM

---

## User

# MOB VARIANTS BRS — NATIVE FREEZING MECHANICS INVESTIGATION

## KILO CODE SESSION

- **Mode:** Ask
- **New Kilo Session:** YES
- **Project:** MobVariantsBRS
- **Repository:** https://github.com/barucsinfiltros/MobVariantsBRS
- **Target branch:** `feat/variant-state-layer`
- **Minecraft:** Java Edition 26.1.2
- **Fabric Loader:** 0.19.5
- **Fabric API:** 0.155.3+26.1.2
- **Java:** 25

## OBJECTIVE

Perform a strictly read-only technical investigation to determine whether the existing Minecraft freezing system can implement the simplified `cold_zombie` freezing mechanic without introducing an independent duration-tracking system.

The previous S-0101 design proposed a persistent attachment for accumulated freezing duration. That design is explicitly rejected.

The investigation must establish whether vanilla `Entity.ticksFrozen` and the existing freezing lifecycle can support cumulative freezing, periodic damage, natural recovery, and powder snow compatibility.

Do not implement anything.

## 1. REQUIRED INITIAL INSPECTION

1. Identify and verify the actual workspace root.
2. Verify the current Git branch, HEAD commit, and working tree status.
3. Inspect the previous S-0101 investigation and relevant project documentation if available.
4. Inspect the actual Minecraft 26.1.2 mapped sources and project dependencies.
5. Verify the actual Fabric API version and relevant available hooks.
6. Identify existing `cold_zombie` implementation files and related Mixins, without modifying them.

Do not assume historical reports accurately represent the current source code.

Prioritize actual Minecraft and Fabric source code over secondary documentation.

If a source, mapping, dependency, or API cannot be accessed, explicitly report that limitation.

## 2. REQUIRED GAMEPLAY BEHAVIOR

### 2.1 Freezing Touch

Every successful melee hit by a `cold_zombie` must:

**Slowness**
- Apply Slowness I.
- Duration: 120 ticks.
- Repeated hits refresh the duration.
- Duration must never accumulate or exceed 120 ticks.

**Freezing**
- Add 40 ticks of freezing per successful hit.
- Repeated hits must accumulate.
- Freezing must continue after the victim leaves powder snow.
- Preserve vanilla freezing visuals and periodic damage.
- Allow natural recovery after the accumulated duration expires.
- Do not introduce an arbitrary gameplay duration cap.

### 2.2 Cryogenic Body

The `cold_zombie` must:
- Be immune to natural powder snow freezing.
- Never accumulate freezing from powder snow.
- Never receive vanilla freezing damage.
- Remain vulnerable to unrelated damage sources.

Verify whether overriding or intercepting `canFreeze()` is sufficient for all these requirements.

Do not assume that preventing freezing automatically prevents every possible freezing-related damage path.

### 2.3 Isolation

Other entities must retain their normal vanilla behavior.

Do not globally modify freezing mechanics in a way that changes unrelated entities.

## 3. NATIVE FREEZING LIFECYCLE

Reconstruct the exact Minecraft 26.1.2 freezing lifecycle.

Inspect the relevant implementations and call order, including:

- `Entity.ticksFrozen`.
- `Entity.getTicksFrozen()`.
- `Entity.setTicksFrozen()`.
- `Entity.isFullyFrozen()`.
- `Entity.canFreeze()`.
- `Entity.baseTick()` or the actual equivalent.
- `LivingEntity.baseTick()` or the actual equivalent.
- Powder snow collision and freezing logic.
- Vanilla freezing damage processing.
- Frozen-state synchronization and visual behavior.

Verify these historical observations instead of accepting them without evidence:

- `BASE_TICKS_REQUIRED_TO_FREEZE = 140`.
- `FREEZE_HURT_FREQUENCY = 40`.
- Powder snow increments `ticksFrozen` toward 140.
- Outside powder snow, vanilla recovery decreases freezing by 2 ticks per tick.
- Vanilla freezing damage requires `isFullyFrozen()` and `canFreeze()`.

For every relevant method, document:

1. Exact signature.
2. Source file or mapped class.
3. Relevant code path.
4. Execution order.
5. Whether the value is clamped, bounded, or allowed to exceed 140.
6. Whether the method is invoked on both client and server.

Do not infer behavior from constant names alone.

## 4. CRITICAL ACCUMULATION INVESTIGATION

Determine whether native `ticksFrozen` can represent additional freezing duration.

Investigate at least these possibilities:

### A. Values above 140

- Can `ticksFrozen` exceed 140?
- Does `setTicksFrozen()` clamp its input?
- Does any subsequent vanilla method clamp or overwrite the value?
- Can values above 140 represent additional freezing duration?
- Are those values preserved through synchronization?

### B. Repeated freezing applications

Investigate whether adding 40 ticks to the existing native counter is sufficient.

Analyze at least these scenarios:

1. First successful hit against an unfrozen victim.
2. Second hit before the first freezing period expires.
3. Repeated hits while the victim remains fully frozen.
4. Repeated hits after leaving powder snow.
5. A hit immediately before vanilla recovery.
6. A hit immediately after vanilla recovery.
7. A victim entering powder snow while accumulated freezing is active.
8. A victim leaving powder snow with accumulated freezing still active.

### C. Periodic damage

This is a critical requirement.

Determine whether vanilla freezing damage continues periodically after accumulated freezing is extended.

Specifically investigate the ordering between:

- Native freezing recovery.
- The fully-frozen check.
- `canFreeze()`.
- The freezing damage timer or damage-frequency check.
- Actual freezing damage execution.

The previous investigation identified a potential problem: native recovery may occur before the fully-frozen damage check.

Determine whether this makes the intended behavior impossible using only the native counter.

Do not assume that setting `ticksFrozen` to 140 repeatedly preserves periodic damage.

If a native value above 140 can preserve duration but breaks damage behavior, explain precisely why.

## 5. CRYOGENIC BODY INVESTIGATION

Verify whether `canFreeze()` interception alone guarantees all Cryogenic Body requirements.

Analyze:

- Powder snow accumulation.
- Natural freezing progression.
- Fully frozen state.
- Vanilla freezing damage.
- Existing frozen state when entering powder snow.
- Interaction with unrelated damage.

Identify the smallest correct interception point and whether additional narrowly scoped behavior is required.

Do not propose broad changes to global freezing behavior.

## 6. SUCCESSFUL MELEE DETECTION

Investigate the previously identified hook:

`Mob.doHurtTarget(ServerLevel, Entity)`

Verify:

- Exact Minecraft 26.1.2 signature.
- Return value semantics.
- Whether `true` reliably indicates a successful melee attack.
- Whether the method is called for projectiles or unrelated damage.
- Whether a TAIL injection can safely use the boolean return value.
- Whether the target can be safely cast to `LivingEntity`.
- Whether server-side execution is guaranteed or needs explicit protection.

Determine whether a TAIL injection is the best integration point.

Only successful melee attacks from `cold_zombie` may trigger Freezing Touch.

## 7. POWDER SNOW AND RECOVERY

Investigate how native freezing interacts with:

- Natural powder snow accumulation.
- Accumulated freezing from Freezing Touch.
- Powder snow entry and exit.
- Natural recovery.
- Repeated melee hits.
- Vanilla visual synchronization.

Determine whether native state can distinguish accumulated freezing from powder snow freezing without additional tracking.

If it cannot, explain the exact conflict.

## 8. ARCHITECTURAL CONSTRAINTS

The final recommendation must respect these restrictions:

- No independent freezing counter.
- No custom expiration timer.
- No expiration timestamp.
- No victim-side freezing attachment.
- No global UUID maps.
- No world-wide entity scans.
- No per-tick biome checks.
- No custom damage implementation unless vanilla damage is proven unusable.
- No duplicate freezing damage.
- Preserve server-authoritative behavior.
- Preserve normal behavior for unrelated entities.
- Do not modify unrelated variant mechanics.

If native Minecraft mechanics cannot satisfy the requirements, propose the smallest viable alternative conceptually, but do not implement it.

Clearly distinguish between:
- A native-only solution.
- A minimal alternative requiring additional state.
- An approach that is technically possible but violates the stated design constraints.

## 9. COMPARISON AND FINAL RECOMMENDATION

Compare the technically viable approaches.

Use the following criteria:

| Criterion | Required |
|---|---|
| Cumulative 40-tick applications | Yes |
| Freezing persists outside powder snow | Yes |
| Vanilla periodic damage | Yes |
| Vanilla freezing visuals | Yes |
| Natural recovery | Yes |
| No arbitrary duration cap | Yes |
| Powder snow compatibility | Yes |
| No independent duration tracking | Yes |
| Server authoritative | Yes |
| Unrelated entities unaffected | Yes |

Select one definitive recommendation.

Do not recommend a solution that satisfies accumulation but silently breaks vanilla periodic damage.

If no approach satisfies every requirement, explicitly state that native-only implementation is not feasible.

## 10. STRICT READ-ONLY RESTRICTIONS

This session is investigation only.

DO NOT:

- Modify Java source files.
- Create attachments.
- Register Mixins.
- Modify Mixin configuration.
- Change JSON resources.
- Modify Gradle files.
- Add dependencies.
- Change project configuration.
- Implement gameplay mechanics.
- Create implementation scaffolding.
- Commit or push changes.
- Reset, revert, or discard existing work.

Preserve the complete working tree.

If Git reports pre-existing modifications, do not overwrite or interfere with them.

## 11. REQUIRED REPORT

Produce a complementary technical investigation report containing:

1. Verified repository root, branch, HEAD, and working tree status.
2. Exact Minecraft 26.1.2 freezing lifecycle.
3. Verified `ticksFrozen` behavior and numerical limits.
4. Whether native accumulation is technically possible.
5. Whether vanilla periodic damage survives accumulated freezing.
6. Exact method execution order and relevant injection points.
7. Cryogenic Body interception requirements.
8. Successful melee detection and exact method signature.
9. Powder snow interaction and natural recovery.
10. Comparison of viable approaches.
11. Definitive recommended architecture.
12. Remaining limitations and implementation readiness.

For every important technical conclusion, provide:
- Relevant source file and class.
- Exact method or field.
- Relevant code behavior.
- Evidence supporting the conclusion.

Distinguish verified facts from assumptions and unresolved questions.

Do not claim source inspection or validation that was not actually performed.

## 12. FINAL DECISION

Conclude with exactly one of these outcomes:

### READY FOR CODE

Use only if native Minecraft freezing mechanics satisfy every required gameplay and architectural constraint without independent duration tracking.

### NOT READY FOR CODE

Use if native mechanics cannot satisfy the requirements.

Explain the exact technical limitation and propose the smallest viable alternative without implementing it.

**The session must remain strictly read-only regardless of the final outcome.**

---

## Assistant

**Tool: bash**

**Tool: bash**

**Tool: bash**

**Tool: bash**

**Tool: bash**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: read**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: bash**

**Tool: bash**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: glob**

**Tool: glob**

**Tool: glob**

**Tool: grep**

**Tool: grep**

---

## Assistant

**Tool: read**

**Tool: read**

**Tool: bash**

**Tool: bash**

---

## Assistant

**Tool: read**

**Tool: bash**

**Tool: bash**

**Tool: read**

**Tool: read**

---

## Assistant

# Investigación técnica (solo lectura): congelación nativa para `cold_zombie` — MobVariantsBRS

## 1. Estado verificado del repositorio y entorno

| Elemento | Evidencia verificada |
|---|---|
| Raíz del workspace | `C:\modding_mc\MobVariantsBRS` (directorio de trabajo y raíz del proyecto) |
| Rama Git | `feat/variant-state-layer` (`git rev-parse --abbrev-ref HEAD`) |
| HEAD | `252b7b0dee170cc0df9cc21c2b8a47285773067d` (`git rev-parse HEAD`, `git log -5 --oneline`) |
| Seguimiento remoto | `origin/feat/variant-state-layer` al día (`git status --short --branch` → `## feat/variant-state-layer...origin/feat/variant-state-layer`) |
| Working tree | Limpio, sin cambios staged/unstaged/untracked (`git status` / `git status --short --branch` sin entradas de modificación) |
| Versiones declaradas del proyecto | `gradle.properties`: `minecraft_version=26.1.2`, `loader_version=0.19.5`, `loom_version=1.18-SNAPSHOT`, `fabric_api_version=0.155.3+26.1.2`, Java 25 (`build.gradle` → `tasks.withType(JavaCompile).configureEach { options.release = 25 }`, `sourceCompatibility/targetCompatibility = VERSION_25`) |
| Dependencias declaradas | `build.gradle`: `minecraft`, `net.fabricmc:fabric-loader`, `net.fabricmc.fabric-api:fabric-api` (sin dependencias adicionales) |
| Configuración de Mixins actual | `src/main/resources/mob_variants_brs.mixins.json`: `"mixins": []`, paquete `com.baruc.brs.mobvariants.mixin`, `compatibilityLevel: JAVA_25` |
| Variante/estado existente inspeccionado | `src/main/resources/data/mob_variants_brs/variants/cold_zombie.json` (texture `mob_variants_brs:textures/entity/zombie/zombie_cold.png`, biomas fríos), `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java`, `src/main/java/com/baruc/brs/mobvariants/attachment/VariantAttachments.java`, `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/TESTING.md`, `.kilo/plans/1794447703349-cold-zombie-mechanics-plan.md` |
| Investigación previa S-0101 | `.kilo/sesions/S-0101_ask_session-ses_e322.md` (presente; contiene reconstrucción detallada del ciclo de congelación, firmas y análisis de Approach C con attachment de duración restante — diseño explícitamente rechazado para esta investigación) |

**Implementación actual de `cold_zombie`:** solo definición de variante JSON y textura (`src/main/resources/.../cold_zombie.json`, `src/main/resources/assets/.../zombie_cold.png`). No hay Mixins registrados ni lógica de mecánicas de congelación/ataque en `src/main/java` (búsquedas `doHurtTarget|ticksFrozen|canFreeze|powder|Slowness|cold` en `src/` sin resultados de código Java de mecánica).

## 2. Fuentes inspeccionadas, mapeos y limitaciones

| Fuente | Estado | Nota |
|---|---|---|
| Código del proyecto (repo) | **Inspeccionado** | Archivos listados arriba leídos directamente. |
| Fuentes mapeadas de MC 26.1.2 en caché local / `build/generated/sources` | **No disponibles localmente** | No se encontraron `Entity.java`/`LivingEntity.java` en `.gradle/caches` ni en `build` mediante búsquedas de archivos; `build/generated/sources/headers/...` vacío en inspección. No se ejecutó `genSources` (sesión read-only y sujeto a restricciones de ejecución). |
| Fuentes MC 26.1.2 usadas como evidencia secundaria durante la sesión | **Inspeccionadas por referencia externa** | Se consultaron fuentes mapeadas (mojmap) de 26.1.2 para `net.minecraft.world.entity.Entity`, `LivingEntity`, `Mob`, `InsideBlockEffectType`, `PowderSnowBlock`, `DamageSources` (espejo público `ma4z-sys/Minecraft-26.1.2`). Las firmas/comportamientos citados abajo coinciden con la reconstrucción documentada en `.kilo/sesions/S-0101_ask_session-ses_e322.md`. |
| Fabric API 0.155.3+26.1.2 | **Parcialmente verificada** | Versión declarada en `gradle.properties`. Inspección documental de `ServerLivingEntityEvents` y ausencia de evento de tick por entidad en la familia de lifecycle events; artefactos locales de Fabric presentes en caché pero no decompilados/extraídos en esta sesión por restricciones. |
| Ejecución/validación runtime o build | **No realizada** | Sesión estrictamente read-only. |

**Distinción:** las conclusiones sobre el ciclo vanilla se consideran **altamente fundamentadas** por las fuentes mapeadas 26.1.2 inspeccionadas/referenciadas y por S-0101, pero la paridad exacta con los artefactos *localmente* generados por este entorno Loom no fue re-verificada con `genSources` local. No se afirman resultados de compilación, tests o ejecución en juego.

## 3. Ciclo de congelación vanilla reconstruido (MC 26.1.2, mojmap)

### 3.1 Constantes y estado

| Elemento | Clase / archivo fuente (mojmap 26.1.2) | Firma / valor | Comportamiento relevante |
|---|---|---|---|
| Umbral de congelación completa | `net.minecraft.world.entity.Entity` | `public static final int BASE_TICKS_REQUIRED_TO_FREEZE = 140` (constante); `public int getTicksRequiredToFreeze()` → `140` | Límite usado por `isFullyFrozen()` y por el tope de incremento en polvo de nieve. |
| Frecuencia de daño por congelación | `net.minecraft.world.entity.Entity` | `public static final int FREEZE_HURT_FREQUENCY = 40` | Usada en el chequeo de daño periódico. |
| Estado sincronizado | `net.minecraft.world.entity.Entity` | `DATA_TICKS_FROZEN` (`EntityDataAccessor<Integer>`, synched data) | `getTicksFrozen()`/`setTicksFrozen(int)` leen/escriben este dato; se sincroniza a clientes (overlay/visuales). |
| Lectura/escritura | `Entity` | `public int getTicksFrozen()` / `public void setTicksFrozen(int ticks)` | Setter asigna el valor al synched data (sin clamp explícito en el setter según fuente inspeccionada). |
| Congelado completo | `Entity` | `public boolean isFullyFrozen()` → `getTicksFrozen() >= getTicksRequiredToFreeze()` | Condición booleana (≥ 140). |
| Porcentaje visual | `Entity` | `public float getPercentFrozen()` → `min(getTicksFrozen(), getTicksRequiredToFreeze()) / 140.0F` | Visualmente saturado en 1.0 al llegar a 140. |
| Elegibilidad | `Entity` / `net.minecraft.world.entity.LivingEntity` | `Entity.canFreeze()` → `!is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)`; `LivingEntity.canFreeze()` → `false` si espectador o armadura inmune, si no `super.canFreeze()` | Gate de acumulación (vía efecto de bloque) y de daño periódico. Invocado en servidor y cliente según contexto. |
| Persistencia NBT | `Entity` | `addAdditionalSaveData(...)` guarda `"TicksFrozen"` si `> 0`; `readAdditionalSaveData(...)` lee `getIntOr("TicksFrozen", 0)` y llama `setTicksFrozen` | Estado persiste con la entidad. |

### 3.2 Orden de ejecución relevante por tick (entidad viva, servidor)

Reconstrucción a partir de `LivingEntity.baseTick()` y `Entity.baseTick()` / efectos de bloque (26.1.2):

1. **`Entity.baseTick()`**: reinicia `isInPowderSnow = false` (entre otra lógica base).
2. **`applyEffectsFromBlocks()` / `InsideBlockEffectApplier`**: al estar dentro de polvo de nieve, `PowderSnowBlock.entityInside(...)` aplica `InsideBlockEffectType.FREEZE`.
   - `InsideBlockEffectType.FREEZE.effect()`: `entity.setIsInPowderSnow(true); if (entity.canFreeze()) entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze(), entity.getTicksFrozen() + 1));`
   - Incremento **+1/tick**, **acotado a `getTicksRequiredToFreeze()` (140)**, condicionado a `canFreeze()`.
3. **Bloque de congelación en `LivingEntity.baseTick()` (lado servidor)**:
   - **Recuperación:** `if (!this.isInPowderSnow || !this.canFreeze()) this.setTicksFrozen(Math.max(0, this.getTicksFrozen() - 2));`
   - Actualización visual/atributo de escarcha (`removeFrost()` / `tryAddFrost()`; modificador de velocidad cuando `ticksFrozen > 0`).
   - **Daño periódico:** `if (this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) this.hurtServer(serverLevel, this.damageSources().freeze(), 1.0F);`

**Fuente de daño:** `net.minecraft.world.damagesource.DamageSources.freeze()` → tipo de daño de congelación (`DamageTypes.FREEZE`). `LivingEntity.hurtServer(...)` aplica reglas generales de invulnerabilidad/bloqueo; el multiplicador extra por tipo congelante aplica según tags, pero la emisión periódica vanilla depende del bloque anterior.

### 3.3 Verificación de observaciones históricas

| Observación histórica | Verificación |
|---|---|
| `BASE_TICKS_REQUIRED_TO_FREEZE = 140` | **Confirmada** (`Entity`). |
| `FREEZE_HURT_FREQUENCY = 40` | **Confirmada** (`Entity` / uso en `LivingEntity.baseTick`). |
| Polvo de nieve incrementa hacia 140 | **Confirmada** (`InsideBlockEffectType.FREEZE`: `min(getTicksRequiredToFreeze(), getTicksFrozen()+1)` si `canFreeze()`). |
| Fuera de polvo de nieve, recuperación −2 ticks/tick | **Confirmada** (`LivingEntity.baseTick`: si `!isInPowderSnow || !canFreeze()` → `max(0, ticksFrozen-2)`). |
| Daño vanilla requiere `isFullyFrozen()` y `canFreeze()` | **Confirmada** (condición conjunta en `LivingEntity.baseTick`). |

### 3.4 Límites numéricos y semántica de `ticksFrozen`

| Aspecto | Conclusión (evidencia) |
|---|---|
| ¿`setTicksFrozen(int)` acota la entrada? | **No se observa clamp en el setter** (`Entity.setTicksFrozen` asigna al synched data). |
| ¿Puede `ticksFrozen` superar 140? | **Posible a nivel de almacenamiento/sincronización** si se escribe directamente, **pero** no es un estado producido por el camino vanilla de polvo de nieve (este lo limita a 140). |
| ¿Algún camino vanilla lo acota/reescribe? | **Sí, contextualmente:** `InsideBlockEffectType.FREEZE` fuerza `min(140, current+1)` al aplicar; la recuperación lo reduce en −2/tick fuera de polvo de nieve o si `!canFreeze()`. No hay tope superior global observado aparte de estos comportamientos. |
| ¿Valores > 140 preservan "congelación completa"? | `isFullyFrozen()` es `>= 140` → **sí** mientras el valor permanezca ≥ 140. `getPercentFrozen()` satura en 1.0. |
| ¿Valores > 140 sobreviven sincronización/persistencia? | El valor viaja por synched data/NBT como entero; **en principio sí**, sujeto a los ticks posteriores que lo modifiquen (recuperación, efectos de bloque). |
| ¿`canFreeze()` false impide daño/acumulación vanilla? | **Acumulación por polvo:** sí, el incremento está gated por `canFreeze()`. **Daño periódico:** sí, gated por `canFreeze() && isFullyFrozen()`. **Recuperación:** la condición `!canFreeze()` también provoca decrecimiento (−2), lo que tiende a descongelar. |

## 4. Investigación crítica de acumulación (requisito "sin contador independiente")

### 4.1 Escenarios analizados

| Escenario | Análisis con contador nativo únicamente (`ticksFrozen`) |
|---|---|
| 1. Primer golpe a víctima no congelada (+40) | Se puede hacer `setTicksFrozen(min/según diseño, getTicksFrozen()+40)` → 40 (<140), no `isFullyFrozen()`, **no hay daño periódico** aún; se requerirían más golpes o esperar polvo de nieve para llegar a 140. Esto difiere de "acumular duración de congelación completa" salvo que se fuerce ≥140. |
| 2. Segundo golpe antes de expirar el primero | Sumar +40 es posible numéricamente, pero fuera de polvo de nieve la recuperación resta −2 cada tick; sin estado extra no hay forma de distinguir "duración pendiente" de "valor actual". |
| 3. Golpes repetidos estando totalmente congelado (≥140) | Mantener/elevar el valor puede sostener `isFullyFrozen()`, **pero** entrar en polvo de nieve aplica `min(140, current+1)` y **perdería cualquier exceso >140** (conflicto directo). |
| 4. Golpes tras salir de polvo de nieve | El valor decae −2/tick; para que persista N ticks adicionales habría que fijar un valor inicial alto y dejar que decaiga, pero no se puede "refrescar/acumular" de forma fiable sin rastrear cuánto tiempo queda más allá del propio contador que ya está decayendo. |
| 5. Golpe inmediatamente antes de la recuperación | La recuperación ocurre cada tick (antes del chequeo de daño) cuando corresponde; un valor justo en el borde puede cruzar 140→138 en el mismo tick y **romper `isFullyFrozen()`/daño en ese tick**. |
| 6. Golpe inmediatamente después de la recuperación | Similar sensibilidad al orden; el daño depende de `tickCount % 40 == 0` y del estado tras recuperación, no de un temporizador independiente de duración acumulada. |
| 7. Víctima entra en polvo de nieve con congelación acumulada activa | **Conflicto crítico:** `FREEZE` hace `min(getTicksRequiredToFreeze(), getTicksFrozen()+1)` → si había acumulación modelada como >140, se **clampa a 140** y se pierde la duración extra; si se modelara como "mantener en 140", el polvo de nieve no distingue fuentes pero la "duración acumulada" ya no existe en el contador nativo. |
| 8. Víctima sale de polvo de nieve con acumulación activa | Sin estado adicional no hay memoria de duración restante: el contador simplemente decae −2/tick (o se comporta según reglas vanilla), imposibilitando garantizar "continúa tras salir de polvo de nieve hasta agotar lo acumulado". |

### 4.2 Daño periódico y orden de ejecución (crítico)

**Orden verificado (servidor, `LivingEntity.baseTick`):** (a) recuperación/ajuste de `ticksFrozen` (`-2` si aplica) → (b) frost/visuales → (c) si `tickCount % 40 == 0 && isFullyFrozen() && canFreeze()` → `hurtServer(... freeze ..., 1.0F)`.

Consecuencias:
- El daño **sí** puede seguir ocurriendo mientras la entidad permanezca `isFullyFrozen()` en el instante del chequeo y `canFreeze()` sea true (comportamiento vanilla preservado si se sostiene ≥140 en esos ticks de evaluación).
- **Pero** sostener ≥140 "a la fuerza" cada tick desde lógica externa equivale a introducir un control de duración externo (contradice "sin contador/temporizador independiente"), y además interactúa con la recuperación previa: si se corrige después, se altera el flujo natural; si no se corrige, el decaimiento rompe la condición.
- Un valor >140 que decae naturalmente **no** garantiza intervalos de daño estables al cruzar el umbral por el `-2` previo al chequeo (p.ej. 141 → 139 antes del daño). Por tanto, **representar duración extra solo con `ticksFrozen` > 140 no preserva de forma robusta el daño periódico** en todos los cruces.
- Reaplicar `setTicksFrozen(140)` repetidamente preservaría `isFullyFrozen()` pero **no** codifica por sí mismo "cuántos ticks quedan" sin un estado auxiliar; además el disparo de daño sigue ligado a `tickCount % 40`, no a "40 ticks por golpe".

**Conclusión de acumulación:** la acumulación de "duración pendiente" por golpes (+40, persistente fuera de polvo de nieve, sin tope arbitrario, compatible con reingresos a polvo de nieve) **no es representable fielmente solo con `ticksFrozen`** dado: cap/aplastamiento a 140 en `InsideBlockEffectType.FREEZE`, recuperación automática −2/tick, ausencia de distinción entre fuentes (golpe vs. polvo de nieve) y dependencia del umbral/orden para daño. **La periodicidad del daño vanilla puede preservarse** mientras el estado sea plenamente congelado en los ticks de chequeo, pero **no** puede garantizarse para "duración acumulada extendida" sin un mecanismo que gobierne esa duración (lo cual sería estado/temporizador independiente, rechazado por las restricciones).

## 5. Cuerpo criogénico (`Cryogenic Body`) y punto de intercepción

**Requisito:** inmune a congelación natural por polvo de nieve, sin acumular congelación ni recibir daño vanilla de congelación por esa vía, pero vulnerable a otras fuentes de daño.

| Camino | Efecto de interceptar `canFreeze()` → `false` para `cold_zombie` |
|---|---|
| Acumulación en polvo de nieve | **Bloqueada** (`InsideBlockEffectType.FREEZE` incrementa solo si `canFreeze()`). |
| Daño periódico de congelación vanilla | **Bloqueado** (chequeo `isFullyFrozen() && canFreeze()`). |
| Recuperación en `LivingEntity.baseTick()` | La rama `!canFreeze()` **también fuerza decaimiento** (`-2`); para un cold zombie que nunca debería congelarse por polvo esto es coherente (tiende a 0), pero impide usar `canFreeze()==false` como "inmunidad" mientras simultáneamente se quisiera mantener un estado congelado vanilla por otra causa en la misma entidad (no aplica aquí: el atacante es el cold zombie). |
| Estado ya congelado al entrar en polvo de nieve | Si por alguna razón tuviera `ticksFrozen > 0`, el decaimiento por `!canFreeze()` lo reduciría; el polvo no lo incrementaría. |
| Daño no relacionado | `canFreeze()` no gatea `hurtServer` en general; **otros daños permanecen** (correcto), salvo que alguna lógica específica consulte congelación — no observado como bloqueo global de daño. |
| Daño por fuente `FREEZE` fuera del tick periódico | El tick periódico es la emisión vanilla principal; `hurtServer` procesa la fuente, pero sin estar `isFullyFrozen()` por acumulación natural y con `canFreeze()` false el camino periódico no dispara. (No se identificó otro emisor vanilla periódico independiente en el flujo inspeccionado). |

**Punto mínimo correcto (aislado a la variante):** override/intercept de `LivingEntity.canFreeze()` (o equivalente efectivo en la jerarquía usada en runtime) devolviendo `false` **solo** cuando la entidad sea `cold_zombie` (p.ej. comparando `VariantAttachments.VARIANT_TEXTURE` con el id de textura del cold zombie, patrón ya existente). Esto es **suficiente** para los requisitos de inmunidad al polvo de nieve y ausencia de daño periódico vanilla por esa vía, siempre que la identificación sea inequívoca y server-authoritative. No se requiere cambio global para otras entidades. **Caveat:** si en el futuro se necesitara que la misma entidad cold zombie pueda estar "congelada" por un sistema que dependa de `canFreeze()==true`, habría conflicto semántico; con los requisitos actuales (cold zombie inmune, víctima es otra entidad) no se observa.

## 6. Detección de golpe cuerpo a cuerpo exitoso

| Elemento | Evidencia / firma (MC 26.1.2, mojmap) |
|---|---|
| Implementación relevante | `net.minecraft.world.entity.Mob.doHurtTarget(ServerLevel level, Entity target)` → `boolean` (`Mob.java`) |
| Stub no utilizable | `net.minecraft.world.entity.LivingEntity.doHurtTarget(...)` devuelve `false` (sin lógica de ataque real). Zombies extienden `Mob`. |
| Semántica de retorno | Calcula daño/knockback/encantamientos, llama `target.hurtServer(level, damageSource, dmg)`; `wasHurt` refleja éxito real; retorna `wasHurt`. |
| Éxito fiable | **Sí, para melee de `Mob`**: `true` solo cuando el daño se aplicó según `hurtServer` (respeta invulnerabilidad, bloqueo efectivo, etc.); fallos/misses del flujo de daño no retornan éxito. |
| ¿Proyectiles u otros daños? | `doHurtTarget` es el camino de ataque cuerpo a cuerpo del mob; no cubre proyectiles. |
| Seguridad de TAIL | Inyección en `RETURN`/TAIL puede leer el valor de retorno (`wasHurt`) y el contexto (`this` atacante, `target`, `level`); debe ejecutarse en servidor (`!level.isClientSide()`) y filtrar `target instanceof LivingEntity`, vivo y atacante `cold_zombie`. |
| Recomendación | **TAIL en `Mob.doHurtTarget(ServerLevel, Entity)`** condicionado a `retval == true` es el punto más estrecho y fiable para "golpe melee exitoso" del atacante mob. No usar eventos globales de daño para inferir éxito melee sin ambigüedad de fuente. |

## 7. Polvo de nieve, recuperación e interacción

- **Acumulación natural:** +1/tick hasta 140, gated por `canFreeze()`.
- **Acumulación por Freezing Touch (requerida):** +40 por golpe, acumulativa, persistente fuera de polvo de nieve, sin cap arbitrario → **exige memoria de duración restante**.
- **Distinción de fuentes:** `ticksFrozen` es un único contador entero sin procedencia; **no puede distinguir** acumulación por golpe vs. por polvo de nieve. Cualquier mezcla (entrar/salir de polvo de nieve mientras hay "duración acumulada") genera ambigüedad irresoluble sin estado adicional.
- **Recuperación natural:** −2/tick cuando `!isInPowderSnow || !canFreeze()`. Debe poder actuar al expirar la duración acumulada para restaurar comportamiento vanilla.
- **Sincronización visual:** `DATA_TICKS_FROZEN` synched; mantener ≥140 produce estado totalmente congelado/overlay mientras se sostenga, pero sostenerlo requiere controlar cuándo dejar de hacerlo (otra forma de tracking temporal).

## 8. Comparación de enfoques frente a criterios

| Criterio | Nativo-only (`ticksFrozen` sin estado auxiliar) | Alternativa mínima (estado de duración restante, p.ej. attachment en víctima + coordinación con `ticksFrozen`) | Nota |
|---|---:|---:|---|
| Aplicaciones acumulativas de 40 ticks | **No (fiable)** | **Sí** | Nativo no preserva "pendiente" ante cap 140 del polvo y decaimiento −2. |
| Congelación persiste fuera de polvo de nieve | **No (garantizado)** | **Sí** | Nativo decae sin memoria de duración extra. |
| Daño periódico vanilla | **Parcial/condicional** | **Sí** (manteniendo `isFullyFrozen()` y dejando que vanilla dispare) | Nativo solo mientras ≥140 en ticks de chequeo; cruces/gestión rompen acumulación. |
| Visuales vanilla de congelación | **Sí** (si ≥0/≥140 según caso) | **Sí** | Ambos pueden usar `ticksFrozen` synched. |
| Recuperación natural | **Sí** | **Sí** (al expirar, soltar control → −2/tick) | Requiere no forzar indefinidamente. |
| Sin tope arbitrario de duración | **No práctico** | **Sí** (sujeto a límites de tipo, p.ej. saturación segura) | Nativo acotado por interacciones (140 en polvo) y semántica. |
| Compatibilidad con polvo de nieve | **No** | **Sí** (si la duración restante es independiente) | `min(140, current+1)` destruye exceso. |
| Sin tracking independiente de duración | **Sí** | **No** | Requisito explícito incumplido por la alternativa. |
| Server-authoritative | **Posible** | **Sí** | Ambos factibles si se opera en servidor. |
| Entidades no relacionadas sin cambios | **Sí** (si hooks acotados) | **Sí** (si hooks acotados) | Requiere Mixins/guards por variante. |

## 9. Restricciones arquitectónicas (evaluación)

- **Nativo-only** cumple "sin contador/temporizador/attachment de expiración", pero **falla** requisitos funcionales clave (acumulación real, persistencia fuera de polvo de nieve, compatibilidad con polvo de nieve, duración sin tope efectivo en presencia de mecánicas vanilla).
- **Alternativa mínima viable** (conceptual, no implementada): almacenar **duración restante acumulada** en la víctima (p.ej. attachment persistente server-side, `Codec.INT`/entero, sin sync de cliente si se conduce `ticksFrozen`), incrementar en +40 en `Mob.doHurtTarget` TAIL exitoso del cold zombie, decrementar cada tick en un hook por entidad viva (p.ej. `LivingEntity.baseTick` TAIL, early-out), y coordinar `ticksFrozen` para reflejar estado congelado mientras quede duración, dejando recuperación vanilla al expirar. Esto **viola** la restricción "sin contador/attachment/timer independiente" pero es la mínima coherente con los requisitos de gameplay y reuse de daño/visuales vanilla.
- Cualquier enfoque que fuerce `ticksFrozen` repetidamente para "simular" duración sin estado propio introduce de facto un control temporal externo y no resuelve la compatibilidad con el clamp a 140 del polvo de nieve.

## 10. Puntos de integración e impacto en código existente (inspección)

- **Ataque:** `Mob.doHurtTarget(ServerLevel, Entity)` (MC) — no hay hook actual en proyecto (`mob_variants_brs.mixins.json` vacío).
- **Inmunidad del cold zombie:** `LivingEntity.canFreeze()` — candidato de interceptación acotada por identidad de variante (`VARIANT_TEXTURE`).
- **Ciclo por tick:** `LivingEntity.baseTick()` — candidato si se necesitara coordinación temporal (solo en alternativa con estado; no requerido para nativo-only puro, que a su vez no satisface requisitos).
- **Identidad de variante:** ya resuelta por `VariantAttachments.VARIANT_TEXTURE` (`Identifier` sincronizado, server-authoritative) y selección en `VariantStateLifecycle`.
- **Aislamiento:** toda lógica debe condicionar por atacante/víctima `cold_zombie` y no alterar entidades sin esa variante.

## 11. Recomendación definitiva

- **Técnicamente posible con solo mecánicas nativas y sin tracking independiente:** **no** para el conjunto completo de requisitos.
- **Limitación exacta:** `ticksFrozen` es un contador único, acotado a 140 por la acumulación de polvo de nieve (`InsideBlockEffectType.FREEZE`), sujeto a recuperación automática de −2 ticks/tick y sin semántica de "duración pendiente acumulada por golpes" ni distinción de origen. Por ello no puede garantizar simultáneamente: acumulación aditiva de +40 por golpe, persistencia fuera de polvo de nieve hasta agotar lo acumulado, compatibilidad con entrada/salida de polvo de nieve sin perder duración, y ausencia de tope arbitrario, **todo ello sin** introducir estado/temporizador independiente. El daño periódico vanilla es reutilizable cuando `isFullyFrozen() && canFreeze()` en los ticks de evaluación, pero gobernar esa condición durante una duración acumulada arbitraria implica tracking externo.
- **Alternativa mínima viable (conceptual):** attachment/estado persistente en la víctima con "ticks restantes de congelación aplicada", decremento server-side por tick, suma saturada de +40 en golpe melee exitoso del `cold_zombie`, inmunidad del `cold_zombie` al polvo vía `canFreeze()` acotado a la variante, y coordinación de `ticksFrozen` para preservar visuales/daño vanilla sin daño duplicado; recuperación vanilla al expirar. **No implementada** (y no permitida en esta sesión).

## 12. Decisión final

**NOT READY FOR CODE**

**Razón técnica concreta:** las mecánicas nativas de congelación de Minecraft 26.1.2 no satisfacen simultáneamente los requisitos obligatorios (acumulación acumulativa de +40, persistencia fuera de polvo de nieve, daño periódico y visuales vanilla preservados durante toda la duración acumulada, recuperación natural al expirar, compatibilidad con polvo de nieve y ausencia de tope arbitrario de gameplay) **sin** introducir un sistema independiente de seguimiento de duración (contador/temporizador/attachment/timestamp/estado equivalente). La restricción de diseño "sin tracking independiente de duración" es incompatible con la semántica requerida; la alternativa viable más pequeña requiere dicho estado mínimo en la víctima y coordinación acotada con `ticksFrozen`. La sesión se mantuvo estrictamente en modo lectura: no se modificó ningún archivo del proyecto.

---

