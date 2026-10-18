# MobVariantsBRS — Plan de arquitectura de pruebas para variantes (GameTest)

**Objetivo:** Evaluar alternativas técnicamente justificadas para probar recursos de variante en Fabric GameTest sin depender de una API de propiedad JVM de Loom no verificada. Solo análisis y planificación. No se implementan cambios.

**Rama:** `feat/variant-state-layer` · **HEAD:** `02e3b59e306e8b7c7eb66b5ff20c113dc3a2c714` · **Worktree:** limpio · **Histórico S-0082 (`c05e20ff...`):** existe como objeto commit en este repo (VERIFIED)

---

## 1. Resumen ejecutivo

No existe evidencia verificable de que Fabric Loom (cached `1.18.4`, `gradle.properties` declara `1.18-SNAPSHOT`) exponga un mecanismo soportado para pasar propiedades JVM personalizadas al servidor de `runGameTest`. S-0081 y S-0082 lo dejan BLOCKED/UNRESOLVED: `argFiles/runGameTest` solo tiene `-classpath`; `launch.cfg` solo trae `commonProperties` predeterminadas; el bytecode de `RunGameTask`/`AbstractRunTask`/`ProcessUtil`/`Arguments` no pudo inspeccionarse (`javap`/`jar` bloqueados). Por tanto, cualquier arquitectura que asuma `mob_variants_brs.gametest.variantId` queda descartada por instrucción.

La prueba existente (`VariantResourceGameTest.variantDefinitionFound`) solo hace caso positivo: consulta `ResourceManager` directamente y parsea el JSON con el codec por cuenta propia. **No ejecuta el listener de producción** (`VariantDefinitionReloadListener`), ni valida `VariantSnapshot.build`, ni cubre casos faltante/inválido.

Recomendación (una sola arquitectura): **Approach B** — un camino de prueba aislado para los casos negativos (faltante, JSON inválido) que no depende de hacer fallar la ejecución normal del servidor GameTest, junto con el caso positivo existente mejorado para afirmar contra el snapshot publicado por el listener de producción. No se introduce propiedad JVM alguna.

---

## 2. Estado actual verificado

| Ítem | Valor | Clasificación |
|------|-------|---------------|
| Raíz del workspace | `C:\modding_mc\MobVariantsBRS` | VERIFIED |
| Rama actual | `feat/variant-state-layer` | VERIFIED |
| HEAD commit | `02e3b59e306e8b7c7eb66b5ff20c113dc3a2c714` | VERIFIED |
| Estado del worktree | limpio (`git status --porcelain=v1` sin salida) | VERIFIED |
| Remoto origin | `https://barucsinfiltros@github.com/barucsinfiltros/MobVariantsBRS.git` | VERIFIED |
| Histórico HEAD S-0082 `c05e20ff5a81a4480f24ad933d8fed459e11b278` | Existe como objeto commit en este repo | VERIFIED |
| Versiones (`gradle.properties`) | minecraft 26.1.2, loader 0.19.5, loom 1.18-SNAPSHOT, fabric-api 0.155.3+26.1.2, Java 25 | VERIFIED |
| JAR Loom en caché | `.../fabric-loom/1.18.4/.../fabric-loom-1.18.4.jar`, 1.564.439 bytes | VERIFIED (S-0081/S-0082) |
| Propiedad `mob_variants_brs.gametest.variantId` | Ausente del código (sólo propuesta en sesiones previas S-0078/S-0079/S-0080) | VERIFIED (ausente) |
| `runGameTest` / `jvmArgs` / `systemProperty` en Gradle | Ausentes (`build.gradle` lines 43-49, 78-91) | VERIFIED |
| `argFiles/runGameTest` | Solo `-classpath` + classpath; sin `-D` | VERIFIED (S-0081/S-0082) |
| `launch.cfg` | `commonProperties` con propiedades predeterminadas de Loom/Fabric; sin sección GameTest/servidor | VERIFIED (S-0081/S-0082) |
| Propagación de propiedades JVM al servidor GameTest | No demostrada ni negada | UNRESOLVED / BLOCKED |
| Ejecución de Gradle, builds, Minecraft, GameTests, javap | Ninguna | NOT RUN |

## 3. Flujo de producción y pruebas existentes

### Producción (verificada por inspección de código)

1. Registro y cableado — `src/main/java/com/baruc/brs/mobvariants/variant/VariantStateLifecycle.java:35-45`
   - `VariantStateLifecycle.register()` registra `VariantDefinitionReloadListener` en `ResourceLoader.get(PackType.SERVER_DATA)` con id `"variants"` (línea 38-39).
   - `ServerLifecycleEvents.SERVER_STARTING.register(loader::attach)` (línea 41); `SERVER_STOPPED` → `detach` (línea 42); `ENTITY_LOAD` → `selectVariant` (línea 44).

2. Publicación del snapshot al iniciar — `VariantDefinitionReloadListener.attach(MinecraftServer)` (líneas 75-85)
   - Lee `server.getResourceManager()` una vez, llama `load(rm)`, construye snapshot con `VariantSnapshot.build(..., server.registryAccess().lookupOrThrow(Registries.BIOME))` y lo publica en `VariantAttachments.SERVER_VARIANT_SNAPSHOT` de los global attachments del servidor. Excepciones: se loguean y las variantes quedan inactivas hasta el próximo reload.

3. Carga y parseo (off-thread en reload) — `prepare` → `load(ResourceManager)` (líneas 97-130)
   - `resourceManager.listResources("variants", JSON_FILE)` resuelve contra `<pack>/data/<namespace>` y devuelve claves `namespace:variants/<archivo>`.
   - `variantIdOf(resourceId)` (líneas 166-178): solo acepta namespace `MobVariantsBRS.MOD_ID` (`mob_variants_brs`); deriva `mob_variants_brs:<nombre>` de la ruta `variants/<nombre>.json`. Otros namespaces: warn + `null` (se salta).
   - Cada archivo: parsea con `VariantDefinition.CODEC` (líneas 114-117). Cualquier fallo se salta y se loguea como error; **un archivo corrupto no aborta el reload** (líneas 120-123).
   - Resultado: lista ordenada de `(variantId, VariantDefinition)`.

4. Publicación en reload (game thread) — `apply` (líneas 138-159)
   - Si `server == null` se omite (esperado durante la construcción inicial del servidor; `attach()` ya publicó el snapshot de arranque).
   - Publica `VariantSnapshot.build(prepared, biomeRegistry)`. Excepción: se loguea y **el snapshot anterior permanece vigente** (líneas 155-158).

5. Validación de definición — `VariantSnapshot.build(List<Map.Entry<Identifier,VariantDefinition>>, Registry<Biome>)` (líneas 72-161), público estático
   - Salta si `entity_type` no está en `BuiltInRegistries.ENTITY_TYPE` (usa `getOptional`, no `getValue`, por `DefaultedRegistry`).
   - Salta si `conditions` presente pero vacío, o si algún bioma es desconocido en el registro de biomas.
   - Salta si `attributes` tiene valor no finito, atributo desconocido, o valor fuera de rango (`RangedAttribute`).
   - Construye `byVariantId` y `byEntityType` (orden ascendente por variant id). `EMPTY` snapshot cuando nada carga.

6. Selección por entidad — `VariantStateLifecycle.selectVariant(Entity, ServerLevel)` (líneas 60-109)
   - Guarda 1 (`isLoadedFromDisk()`) y guarda 2 (`hasAttached(VARIANT_TEXTURE)`) hacen la selección idempotente.
   - Toma `SERVER_VARIANT_SNAPSHOT` (o `EMPTY`), `variantsFor(entity.getType())`, evalúa condiciones de bioma perezosamente, asigna `VARIANT_TEXTURE` + atributos al primer candidato que coincida; si ninguno coincide, deja caída vanilla sin attachar.

### Recursos JSON

- `src/main/resources/data/mob_variants_brs/variants/ice_zombie.json` — válido: `entity_type: minecraft:zombie`, textura de hielo, condiciones de biomas nevados.
- `src/gametest/resources/data/mob_variants_brs/variants/ice_zombie.json` — idéntico al de arriba (fixture del GameTest).

### Prueba existente

- `src/gametest/java/com/baruc/brs/mobvariants/gametest/VariantResourceGameTest.java:20-37`
  - Un único `@GameTest variantDefinitionFound(GameTestHelper)`.
  - Obtiene el servidor, consulta `server.getResourceManager().getResource(mob_variants_brs:variants/ice_zombie.json)`; si está vacío, `helper.fail`.
  - Abre el reader y parsea **por cuenta propia** con `VariantDefinition.CODEC.parse(JsonOps.INSTANCE, json)` (líneas 29-31).
  - `helper.succeed()` si todo sale bien.
  - **No llama a `VariantDefinitionReloadListener` ni a `VariantSnapshot.build`**; no publica ni verifica el snapshot del servidor. Por tanto, consultar `ResourceManager` no valida el listener de producción.

### Infraestructura de pruebas

- `src/gapmetest/java/` contiene únicamente `VariantResourceGameTest.java` (directorio `src/gametest/java/com/baruc/brs/mobvariants/gametest/`).
- `src/test/java/com/baruc/brs/mobvariants/verify/VariantAttributeVerification.java` — clase `main`, no JUnit.
- `build.gradle` (líneas 78-91): sin dependencia JUnit; `test` con `failOnNoDiscoveredTests = false`; las verificaciones corren como `JavaExec` (`verifyVariantAttributes`) en el classpath de test.
- `build.gradle` `fabricApi.configureTests` (líneas 43-49): `createSourceSet = true`, `enableGameTests = true`, `eula = true`, `clearRunDirectory = true`. Sin bloque `runGameTest`.

---

## 4. Comportamientos que deben demostrarse

Categoría 1 — Recurso válido:
- El recurso `mob_variants_brs:variants/ice_zombie.json` es detectable.
- Su JSON es válido y cumple las reglas de producción (`VariantDefinition.CODEC` + `VariantSnapshot.build`: entity type conocido, biomas conocidos, condiciones no vacías).
- El canal de producción lo acepta y realiza la esperada actualización de estado (snapshot publicado en `SERVER_VARIANT_SNAPSHOT` y, si procede, variante seleccionada en la entidad).

Categoría 2 — Recurso faltante:
- El identificador previsto no se resuelve (`listResources` no lo devuelve / `byVariantId` no lo contiene).
- El canal de producción sigue su comportamiento documentado de recurso faltante (no lanza, no aborta el reload).
- Recursos no relacionados y el estado válido existente no se corrompen silenciosamente.

Categoría 3 — JSON inválido:
- El parseo o la validación falla en el límite correcto (`load` salta el archivo y loguea error; `VariantSnapshot.build` salta definiciones con entity type/bioma/atributo inválido).
- Se observa el comportamiento de gestión de errores de producción (log de error, skip, continuación).
- La falla no invalida recursos ni estado no relacionados.

Categoría 4 — Aceptación/rechazo de producción:
- Las pruebas distinguen "el recurso existe" de "se parseó y aceptó la lógica de producción".
- No se afirma que consultar `ResourceManager` valide el listener de producción.
- Se verifica el contrato real de comportamiento y gestión de errores antes de definir aserciones (no se asumen semánticas de rechazo/fallback/logging; se derivan de la implementación y se documentan los contratos faltantes).

---

## 5. Comparación de alternativas

Criterios aplicados a A, B y C: fiabilidad/determinismo; aislamiento de fallos esperados del resultado de la corrida de pruebas; compatibilidad con Loom 1.18.x / Fabric 0.155.3+26.1.2 / Minecraft 26.1.2 / Java 25 (sin ejecutar builds); complejidad de implementación y mantenimiento; capacidad de ejercer validación y gestión de errores reales de producción; riesgo de falsos positivos / pruebas que saltan la ruta de producción; disponibilidad de la infraestructura requerida.

### A. GameTest positivo con identificador de recurso fijo y verificado, más pruebas negativas separadas

- Fiabilidad/determinismo: medio. El positivo es determinista con el recurso fijo `ice_zombie.json`. Los negativos requieren un recurso ausente o inválido presente en la corrida de GameTest; no hay forma limpia de enviar un identificador "inexistente" al servidor sin modificar datos o sin propiedades JVM.
- Aislamiento de fallos esperados: bajo. Hacer que una corrida normal de GameTest falle para cubrir un caso negativo contamina el resultado de la corrida de pruebas a menos que el framework de GameTest soporte expectativas de fallo (no verificado).
- Compatibilidad: alta (usa solo `@GameTest` y recursos existentes), pero el mecanismo de "hacer fallar a propósito" depende de semánticas de GameTest no verificadas.
- Complejidad: baja para el positivo; alta/incierta para los negativos sin propiedades JVM.
- Ejercicio de la ruta de producción: bajo si el positivo solo consulta `ResourceManager` (como hoy); medio si afirma contra el snapshot publicado.
- Riesgo de falso positivo / evitar la ruta de producción: alto en el estado actual (la prueba actual no toca el listener).
- Infraestructura existente: sí, `VariantResourceGameTest` y el recurso `ice_zombie.json`.

### B. Validar casos negativos mediante un camino de prueba aislado que no depende de hacer fallar la ejecución normal del servidor GameTest

- Fiabilidad/determinismo: alto. Los casos negativos se inyectan como datos/llamadas directas, no como fallos de infraestructura de servidor.
- Aislamiento de fallos esperados: alto. Los fallos esperados se asercionan dentro del camino aislado; no se confunden con el resultado `pass/fail` de una corrida de GameTest.
- Compatibilidad: alta. No requiere propiedades JVM ni modificar `build.gradle`. Depende de accesibilidad de métodos de producción (`VariantSnapshot.build` es público estático; `load` es `private static`; `attach` necesita `MinecraftServer` — por confirmar en implementación).
- Complejidad: medio. Requiere nuevo camino/fixture y, posiblemente, ajustar visibilidad o usar reflection/package-private test.
- Ejercicio de la ruta de producción: alto si invoca `VariantSnapshot.build` y/o `attach`/`load` del listener real.
- Riesgo de falso positivo / evitar la ruta de producción: bajo, si se invoca el listener real y no un mock.
- Infraestructura existente: parcial. `VariantSnapshot.build` y `VariantAttachments.SERVER_VARIANT_SNAPSHOT` son públicos; `load`/`attach` requieren descubrimiento de accesibilidad.

### C. Usar solo mecanismos de configuración de prueba cuya compatibilidad se demuestre desde el código checkoutado, artefactos locales o documentación local

- Fiabilidad/determinismo: no determinable (mecanismo no verificado).
- Aislamiento: no determinable.
- Compatibilidad: UNRESOLVED. La única evidencia local es la ausencia de configuración en `build.gradle` y `argFiles/runGameTest` sin `-D`, lo cual no demuestra ni niega soporte de Loom (S-0081/S-0082 BLOCKED).
- Complejidad: alta, porque primero habría que resolver el bytecode de Loom (fuera de alcance de este plan).
- Ejercicio de ruta de producción: indirecto e incierto.
- Riesgo de falso positivo: alto (se basaría en una suposición no verificada).
- Infraestructura existente: no verificable.

### Resumen comparativo

| Criterio | A | B | C |
|----------|---|---|---|
| Fiabilidad/determinismo | Medio | Alto | No determinable |
| Aislamiento de fallos esperados | Bajo | Alto | No determinable |
| Compatibilidad verificada | Alta (parcial) | Alta | UNRESOLVED |
| Complejidad | Baja/Media | Media | Alta |
| Ejercicio ruta de producción real | Medio (si se mejora) | Alto | Indirecto/incierto |
| Riesgo falso positivo / evitar producción | Alto | Bajo | Alto |
| Infraestructura existente | Sí | Parcial (pendiente descubrimiento) | No verificable |

**Veredicto:** C queda descartado por no ser verificable. A por sí sola no cubre los negativos sin asumir propiedades JVM o semánticas de fallo de GameTest no verificadas. B es la única que satisface todos los requisitos sin dependencias no verificadas.

---

## 6. Arquitectura recomendada (una sola): Approach B

**Arquitectura:** Camino de prueba aislado para los casos negativos (faltante e inválido), más el caso positivo existente mejorado para afirmar contra el snapshot publicado por el listener de producción. No se introduce ni se asume `mob_variants_brs.gametest.variantId`.

**Rationale:**
1. No depende del mecanismo de propiedades JVM de Loom, cuyo estado es UNRESOLVED/BLOCKED (S-0081/S-0082).
2. Los fallos esperados (faltante, inválido) se asercionan en el camino aislado, nunca haciendo fallar una corrida normal de GameTest, así no contaminan el resultado de la corrida de pruebas.
3. Ejercita la ruta de producción real: `VariantDefinitionReloadListener.load`/`apply` y `VariantSnapshot.build` (válidación de entity type, biomas, condiciones, atributos), no una duplicación del parseo en la prueba.
4. Distingue existencia del recurso de aceptación por producción (la prueba positiva afirma contra el snapshot del servidor, no solo contra `ResourceManager`).
5. Complejidad contenida: reusa el listener y `VariantSnapshot.build` existentes; no requiere `build.gradle` ni nuevas dependencias.

**Contrato de producción derivado de la implementación (a respetar, no inventar):**
- `load` salta y loguea cualquier archivo no legible/inválido sin abortar el reload (líneas 120-123 del listener).
- `apply` ante excepción deja el snapshot anterior vigente y loguea error (líneas 155-158).
- `VariantSnapshot.build` salta definiciones con entity type desconocido, condiciones vacías, bioma desconocido, o atributo no finito/desconocido/fuera de rango (líneas 83-155).
- `variantIdOf` ignora namespaces distintos a `mob_variants_brs` (líneas 167-171).

---

## 7. Cambios mínimos previstos (para futura implementación; no se aplican ahora)

1. **Prueba positiva existente (`VariantResourceGameTest.variantDefinitionFound`):**
   - Cambiar la aserción final para que valide el snapshot publicado por producción: obtener `server.globalAttachments().getAttachedOrElse(VariantAttachments.SERVER_VARIANT_SNAPSHOT, VariantSnapshot.EMPTY)` y afirmar que `variantsFor(EntityType.ZOMBIE)` contiene `ice_zombie` (o que `byVariantId` contiene `mob_variants_brs:ice_zombie`). Mantener la comprobación de existencia del recurso como precondición, pero no como única validación.
   - *Descubrimiento requerido en implementación:* confirmar accesibilidad de `globalAttachments()` y `VariantAttachments.SERVER_VARIANT_SNAPSHOT` desde el entorno de GameTest, y el `EntityType` de zombie disponible.

2. **Nuevo camino de prueba aislado para negativos (faltante e inválido):**
   - Ubicación por descubrir (posiblemente `src/test/java/...` si se añade un framework de pruebas, o una clase `main` vía `JavaExec` como `verifyVariantAttributes`). No se asume JUnit.
   - Invocar directamente `VariantSnapshot.build(prepared, biomeRegistry)` con entradas controladas:
     - variante con `entity_type` desconocido → se salta;
     - variante con bioma desconocido → se salta;
     - `conditions` vacío → se salta;
     - atributo no finito / desconocido / fuera de rango → se salta;
     - entrada válida (`ice_zombie`) → se acepta y aparece en `byVariantId` / `byEntityType`.
   - Afirmar que un identificador faltante (`mob_variants_brs:nonexistent`) no está en `byVariantId` y que una entrada válida sigue presente (no corrupción).
   - *Descubrimiento requerido:* cómo obtener un `Registry<Biome>` válido fuera del servidor (posiblemente `BuiltInRegistries.BIOME` o un registro falso), y si `load(ResourceManager)` puede invocarse (es `private static`; posible necesidad de cambiar visibilidad o usar reflection).

3. **Fixtures de prueba solo para el caso inválido (si se confirma que los recursos de `src/gametest/resources` son cargados por el servidor de GameTest):**
   - Añadir un archivo JSON intencionalmente inválido en `src/gametest/resources/data/mob_variants_brs/variants/` y afirmar que su variant id no aparece en el snapshot mientras `ice_zombie` sigue presente.
   - *Descubrimiento requerido:* confirmar si Loom/Fabric GameTest carga los recursos de `src/gametest/resources` como datapack del servidor de prueba.

4. **Sin cambios en `build.gradle`** (no añadir `runGameTest`, `jvmArgs` ni `systemProperty`). **Sin introducir `mob_variants_brs.gametest.variantId`.** Sin modificar el comportamiento de producción (`load`, `apply`, `build`, `selectVariant` se mantienen igual).

---

## 8. Criterios de aceptación y validación futura (no ejecutar ahora)

1. La prueba positiva pasa **solo si** el listener de producción publicó el snapshot y `ice_zombie` es seleccionable para zombies; falla si solo existe el archivo en `ResourceManager` pero no fue aceptado por producción.
2. El caso faltante pasa si el identificador inexistente no está en `byVariantId` y el estado válido (`ice_zombie`) permanece.
3. El caso inválido pasa si la definición mal formada es saltada y logueada por producción, y las definiciones válidas no relacionadas siguen presentes (no corrupción).
4. Ninguna prueba asume `mob_variants_brs.gametest.variantId` ni requiere propiedades JVM en `build.gradle` o `argFiles`.
5. Se compila con Java 25 (`tasks.withType(JavaCompile).options.release = 25`).
6. Comandos/procedimientos de validación futura (pendientes de confirmar nombres exactos de tareas de Loom):
   - Compilación: tarea de compilación de Gradle (compilar main, gametest y test).
   - Verificación enfocada existente: `./gradlew verifyVariantAttributes` (JavaExec sobre `VariantAttributeVerification`).
   - GameTests: tarea de GameTest de Loom (convención `runGameTests`; confirmar nombre real en implementación), sin modificar `build.gradle`.
   - No ejecutar en esta sesión.

---

## 9. Incertidumbres y riesgos pendientes (sin presentar como hechos)

- **UNRESOLVED:** Soporte de Loom para pasar propiedades JVM personalizadas al servidor de `runGameTest` (S-0081/S-0082 BLOCKED). No se asume.
- **Descubrimiento requerido:** si `src/gametest/resources` se carga como datapack del servidor GameTest; si no, el fixture de JSON inválido en gametest resources no ejercitaría `load`.
- **Descubrimiento requerido:** accesibilidad de `VariantDefinitionReloadListener.load` (`private static`) y `attach` desde un camino de prueba aislado; posible ajuste de visibilidad o reflection.
- **Descubrimiento requerido:** cómo obtener un `Registry<Biome>` válido fuera de un servidor en ejecución para `VariantSnapshot.build`.
- **Descubrimiento requerido:** nombre exacto de la tarea de Gradle de Loom para ejecutar GameTests y si el entorno de este repositorio puede ejecutarla (no se verificó).
- No se verificó si `VariantStateLifecycle.selectVariant` puede probarse sin spawn de entidad real (depende de `ServerLevel` y `Entity`).

---

## 10. Siguiente paso concreto

Un agente de implementación debe tomar este plan, confirmar los puntos de descubrimiento de la sección 7 (especialmente accesibilidad de `load`/`attach`, disponibilidad de `Registry<Biome>`, carga de recursos gametest y nombre de la tarea GameTest de Loom), y entonces implementar los cambios mínimos de la sección 7 siguiendo los criterios de aceptación de la sección 8, sin editar `build.gradle`, sin introducir `mob_variants_brs.gametest.variantId` y sin modificar el comportamiento de producción.

**Plan guardado en:** `.kilo/plans/1792298894695-mobvariants-brs-gametest-alternativas.md`
