# MobVariantsBRS — Plan de arquitectura de pruebas para variantes (GameTest)

**Objetivo:** Evaluar alternativas técnicamente justificadas para probar recursos de variante en Fabric GameTest sin depender de una API de propiedad JVM de Loom no verificada. Solo análisis y planificación. No se implementan cambios.

**Rama:** `feat/variant-state-layer` · **HEAD actual:** `ffc4b9c2d250e3e45535f7c3bb1c5a3b0e0e2da2` (S-0089) · **Worktree:** limpio · **HEAD anterior (S-0087):** `0b77cae098451b9b8d1d10443a78c17918c1b8fb` (referencia histórica; el HEAD actual es `ffc4b9c...`) · **Histórico S-0082 (`c05e20ff...`):** existe como objeto commit en este repo (VERIFIED) · **HEAD reportado por S-0086:** `f21913275d5794e3d2ac54a1fb50782f0aac9d60` (referencia histórica; el HEAD actual es `ffc4b9c...`) · **HEAD reportado por S-0086 como "actual":** `16ae51f8fb4bfc434b3c78366cfa188ca183905e` (referencia histórica más antigua, no corresponde a ningún HEAD de esta sessión)

---

## 1. Resumen ejecutivo

No existe evidencia verificable de que Fabric Loom (cached `1.18.4`, `gradle.properties` declara `1.18-SNAPSHOT`) exponga un mecanismo soportado para pasar propiedades JVM personalizadas al servidor de `runGameTest`. S-0081 y S-0082 lo dejan BLOCKED/UNRESOLVED: `argFiles/runGameTest` solo tiene `-classpath`; `launch.cfg` solo trae `commonProperties` predeterminadas; el bytecode de `RunGameTask`/`AbstractRunTask`/`ProcessUtil`/`Arguments` no pudo inspeccionarse (`javap`/`jar` bloqueados). Por tanto, cualquier arquitectura que asuma `mob_variants_brs.gametest.variantId` queda descartada por instrucción.

La prueba existente (`VariantResourceGameTest.variantDefinitionFound`) solo hace caso positivo: consulta `ResourceManager` directamente y parsea el JSON con el codec por cuenta propia. **No ejecuta el listener de producción** (`VariantDefinitionReloadListener`), ni valida `VariantSnapshot.build`, ni cubre casos faltante/inválido.

La estrategia de cobertura se organiza en tres categorías independientes (ver §4 y §6):

- **A — Carga, validación y publicación en producción:** ejercitar el camino de producción explícito a través del método público `VariantDefinitionReloadListener.attach(MinecraftServer)` en un entorno GameTest, y afirmar contra el snapshot publicado en los global attachments del servidor. No se requiere cambiar la visibilidad de `load(ResourceManager)` (se mantiene `private static`): `attach()` es público y ejecuta internamente `load()` + `VariantSnapshot.build()` + la publicación.
- **B — Validación semántica de definiciones ya parseadas:** ejercitar `VariantSnapshot.build(List<Map.Entry<Identifier,VariantDefinition>>, Registry<Biome>)` directamente con definiciones ya parseadas, sin ejecutar el parser de JSON ni el listener de producción. No se le atribuye comportamiento de carga de recursos ni de listener.
- **C — JSON mal formado a través del listener de producción:** **PENDING** hasta que se demuestre una estrategia de aislamiento concreta que permita ejercer el parseo y la gestión de errores de `load()` sin introducir cambios API no justificados y sin bloquear A y B.

Recomendación (una sola arquitectura): **Approach B** — un camino de prueba aislado para los casos negativos (faltante, JSON inválido) que no depende de hacer fallar la ejecución normal del servidor GameTest, junto con el caso positivo existente mejorado para afirmar contra el snapshot publicado por el listener de producción. No se introduce propiedad JVM alguna.

---

## 2. Estado actual verificado

| Ítem | Valor | Clasificación |
|------|-------|---------------|
| Raíz del workspace | `C:\modding_mc\MobVariantsBRS` | VERIFIED |
| Rama actual | `feat/variant-state-layer` | VERIFIED |
| HEAD commit | `0b77cae098451b9b8d1d10443a78c17918c1b8fb` (S-0087) | VERIFIED |
| HEAD reportado por S-0086 como "actual" | `f21913275d5794e3d2ac54a1fb50782f0aac9d60` (S-0086; referencia histórica; el HEAD actual es `0b77cae...`) | VERIFIED (como referencia histórica) |
| HEAD reportado por S-0086 como referencia más antigua | `16ae51f8fb4bfc434b3c78366cfa188ca183905e` (referencia histórica; no corresponde a ningún HEAD de esta sessión) | VERIFIED (como referencia histórica) |
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
| S-0086: `ServerLevel.globalAttachments()` delega a `server.globalAttachments()` | Fuente Fabric API ramas `26.1` y `26.2` (`MinecraftServerMixin`, `ServerLevelMixin`); inspección de código fuente, sin ejecución de runtime | VERIFIED (inspección estática) |
| S-0086: alcances de `server` vs `level` `globalAttachments()` | Misma instancia `GlobalAttachments` por servidor; javadoc: ciclo de vida atado a `MinecraftServer` | VERIFIED (inspección estática) |
| S-0086: validación de runtime de attachment scope | No ejecutada; solo inspección de fuente | EXPLÍCITAMENTE NO VALIDADO EN RUNTIME |
| S-0084 / S-0085 reportes | No localizables en disco, árbol de HEAD, refs ni objetos dangling; solo referencias cruzadas en sesiones posteriores | NO DISPONIBLES |

## 2.1. Evidencia S-0086: alcance de attachments (servidor vs nivel)

**Fuente inspeccionada:** Fabric API upstream, ramas `26.1` y `26.2` (alineadas con `fabric_api_version=0.155.3+26.1.2` de `gradle.properties`). No se inspeccionó bytecode de Loom; no se ejecutó Gradle, Minecraft ni GameTests.

**Hallazgos verificados por inspección de fuente:**

1. **`MinecraftServerMixin`** — `MinecraftServer.globalAttachments()` retorna su propio campo `@Unique private GlobalAttachmentsImpl globalAttachments`, asignado una vez en el constructor (`<init>` en `@At("TAIL")`).

2. **`ServerLevelMixin`** — sobrescribe `globalAttachments()` con:
   ```java
   @Override
   public GlobalAttachments globalAttachments() {
       return server.globalAttachments();
   }
   ```
   es decir, **delega a la instancia del `MinecraftServer` propietario**.

3. **Javadoc de `GlobalAttachments`** (rama `26.2`): *"On the server, the lifecycle of this target is bound to the lifecycle of the MinecraftServer."*

**Conclusión soportada (no probada en runtime):** `server.globalAttachments()` y `level.globalAttachments()` resuelven al **mismo objeto `GlobalAttachments`** para todo `ServerLevel` de ese servidor. La escritura del listener en `attach()` a través de `server.globalAttachments()` y la lectura de `selectVariant()` a través de `level.globalAttachments()` acceden al mismo almacén. Esto es consistente por construcción.

**Límites explícitos (no se presentan como hechos de runtime):**
- La prueba es de inspección estática de fuente, no de ejecución de runtime. No se verificó que el entorno GameTest active `MinecraftServerMixin` — aunque dicha mezcla es parte de la dependencia Fabric API, por lo que se aplica en cualquier contexto de servidor incluido GameTest.
- No aborda la cuestión separada de propiedades JVM al servidor GameTest (S-0081/S-0082), que permanece UNRESOLVED/BLOCKED.

---

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

Las pruebas se dividen en **tres categorías independientes de cobertura** (A, B, C). Cada una se ejerce mediante una operación distinta y tiene un alcance distinto; ninguna sustituye a las otras. El mapeo de cada categoría a la operación concreta que la ejerce y al criterio de aceptación asociado está en §8.

### Categoría A — Integración positiva con el listener de producción real

- Invocar el método público `VariantDefinitionReloadListener.attach(MinecraftServer)` en un entorno GameTest con un `MinecraftServer` real (`helper.getLevel().getServer()`).
- Verificar el snapshot **publicado por el propio canal de producción** mediante `server.globalAttachments().getAttachedOrElse(VariantAttachments.SERVER_VARIANT_SNAPSHOT, VariantSnapshot.EMPTY)`.
- **No basta con consultar `ResourceManager`.** La mera existencia del archivo en `ResourceManager` no demuestra que el listener de producción lo haya aceptado ni que el snapshot esté publicado. La aserción final debe leer el attachment del servidor.
- No se requiere cambiar la visibilidad de `load(ResourceManager)` (mantiene su actual `private static`). `attach()` es público y ejecuta internamente `load()` + `VariantSnapshot.build()` + la publicación, por lo que es el único punto de entrada necesario para esta categoría.
- Se afirma que `variantsFor(EntityType.ZOMBIE)` contiene `ice_zombie` (o que `byVariantId` contiene `mob_variants_brs:ice_zombie`).

### Categoría B — Validación semántica aislada

- Ejercitar la validación semántica a través del método público estático `VariantSnapshot.build(List<Map.Entry<Identifier,VariantDefinition>>, Registry<Biome>)`.
- Se construyen y se asercionan explícitamente estas propiedades semánticas:
  - **Aceptación:** una definición válida (`ice_zombie` con entity type conocido, biomas conocidos, condiciones no vacías) aparece en `byVariantId` y en `byEntityType` para `EntityType.ZOMBIE`.
  - **Rechazo por entity type desconocido:** la definición no aparece en `byVariantId`.
  - **Rechazo por condiciones vacías:** la definición no aparece en `byVariantId`.
  - **Rechazo por bioma desconocido:** la definición no aparece en `byVariantId`.
  - **Rechazo por atributo no finito (NaN / ±infinito):** la definición no aparece en `byVariantId`.
  - **Rechazo por atributo desconocido:** la definición no aparece en `byVariantId`.
  - **Rechazo por atributo fuera de rango (por debajo del mínimo o por encima del máximo del `RangedAttribute`):** la definición no aparece en `byVariantId`.
  - **No corrupción:** un identificador faltante (`mob_variants_brs:nonexistent`) no está en `byVariantId` y una definición válida sigue presente.
- Se obtiene el `Registry<Biome>` real sin lanzar Minecraft mediante la secuencia ya demostrada por `VariantAttributeVerification.realBiomeRegistry()`: `Bootstrap.bootStrap()` + `VanillaRegistries.createLookup().lookupOrThrow(Registries.BIOME)` + copia a `MappedRegistry` + `freeze()`.
- Esta categoría **no** ejecuta el parser de JSON (`VariantDefinition.CODEC`) ni el listener de producción; cubre únicamente la validación semántica que `VariantSnapshot.build` realiza sobre definiciones ya parseadas.

### Categoría C — JSON mal formado

- Se ejercita el parseo y la gestión de errores a través del **listener de producción real**, no a través de `VariantSnapshot.build`.
- **`VariantSnapshot.build(...)` no parsea JSON.** Recibe `VariantDefinition` ya deserializadas, por lo que no puede probar ni la omisión de un archivo mal formado ni el logging de producción asociado a `load()`. Cualquier aserción sobre JSON mal formado debe ir dirigida a `load()` (o a `attach()` que lo ejecuta internamente), nunca a `VariantSnapshot.build`.
- **No se añade un archivo JSON mal formado permanente en `src/gametest/resources` sin evaluar su efecto.** Un fixture inválido en `src/gametest/resources/data/mob_variants_brs/variants/` sería cargado por el mismo datapack que `ice_zombie.json` durante la inicialización del listener en `attach()`, y su presencia puede alterar el comportamiento de arranque del listener (skip + log, snapshot parcial). Esta Cobertura **permanece PENDING** porque no se ha demostrado una estrategia de aislamiento que la permita sin bloquear las categorías A y B.
- Si en el futuro se demuestra una estrategia de aislamiento viable (por ejemplo, un recurso temporal inyectado solo para un test concreto, o un camino que ejerza `load()` directamente), la Cobertura C se especificaría en ese momento; por ahora no bloquea A ni B.

---

## 4.1. Evaluación de factibilidad de la estrategia de prueba del listener de producción

Se investigaron las cuatro preguntas decisivas usando únicamente evidencia del repositorio (código fuente, configuración de Gradle, recursos, tests existentes, reportes de sesiones previas S-0074…S-0086). No se ejecutó Gradle, Minecraft, GameTests ni builds. Las conclusiones distinguen hechos verificados, inferencias estáticas, preguntas sin resolver y comportamiento de runtime no probado.

### Pregunta 1 — Ejecutar el parser real y el listener de producción sin reemplazarlos

**Evidencia de repositorio:**

- `VariantDefinitionReloadListener` es un `SimpleReloadListener` público y final, registrado como singleton vía `getInstance()` en `ResourceLoader.get(PackType.SERVER_DATA)` con id `"variants"` (`VariantStateLifecycle.register()`, línea 38-39).
- `attach(MinecraftServer)` es **público**: lee `server.getResourceManager()`, llama al `private static load(ResourceManager)`, construye el snapshot con `VariantSnapshot.build(prepared, server.registryAccess().lookupOrThrow(Registries.BIOME))` y lo publica en `server.globalAttachments().setAttached(VariantAttachments.SERVER_VARIANT_SNAPSHOT, ...)` (líneas 75-85).
- `load(ResourceManager)` es **`private static`**: no invocable directamente desde un test fuera de la clase sin reflexión ni cambio de visibilidad (líneas 101-130).
- `apply(prepared, state)` es **`protected`**: se ejecuta en el hilo de juego, publica solo si `server != null`; si es nulo omite la publicación (líneas 137-159).
- `VariantSnapshot.build(List<Map.Entry<Identifier,VariantDefinition>>, Registry<Biome>)` es **`public static`**: invocable directamente desde cualquier contexto (líneas 72-162).
- `VariantAttributeVerification` (en `src/test/java/…/verify/`) demuestra que `Bootstrap.bootStrap()` + `VanillaRegistries.createLookup().lookupOrThrow(Registries.BIOME)` inicializan registros reales de Minecraft 26.1.2 sin lanzar el servidor (líneas 100-112, 388-403).
- El reporte S-0077 documenta la ejecución exitosa de `.\gradlew.bat runGameTest` con `compileGametestJava` corriendo durante ella, y que el test existente encuentra `mob_variants_brs:variants/ice_zombie.json` vía `server.getResourceManager()`.

**Camino de ejecución concreto:**
`VariantDefinitionReloadListener.getInstance().attach(server)` en un entorno GameTest donde `server` es el `MinecraftServer` real (`helper.getLevel().getServer()`). Esto ejecuta el parser de producción (`VariantDefinition.CODEC`), `VariantSnapshot.build`, y publica en el attachment global del servidor.

**Límites de confianza:**
- `attach()`: **ALTA** — ruta pública, ejecutable en GameTest, ejecuta el listener completo.
- `load()` directamente: **BLOQUEADO** — `private static`; requiere reflexión o cambio de visibilidad de producción (no autorizado por este plan).
- `apply()` directamente: **MEDIA** — `protected`, accesible solo desde el mismo paquete; omite publicación si `server == null`.
- `VariantSnapshot.build()` directamente: **ALTA** — `public static`, ya ejercitado por `VariantAttributeVerification`.
- Lo que la inspección estática establece: el código existe y está cableado. Lo que requiere runtime: confirmar que `attach()` publica correctamente en un servidor GameTest real y que `selectVariant()` lee el mismo almacén.

### Pregunta 2 — Obtener el registro de biomas

**Evidencia de repositorio:**

- `VariantAttributeVerification.realBiomeRegistry()` (líneas 388-403) construye un `Registry<Biome>` real sin Minecraft:
  ```java
  HolderLookup.RegistryLookup<Biome> lookup = VanillaRegistries.createLookup()
          .lookupOrThrow(Registries.BIOME);
  MappedRegistry<Biome> registry = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable(), false);
  lookup.listElements().forEach(element ->
          registry.register(element.key(), element.value(), RegistrationInfo.BUILT_IN));
  registry.freeze();
  ```
- Requiere `Bootstrap.bootStrap()` previo (línea 104).
- La ruta de producción usa `server.registryAccess().lookupOrThrow(Registries.BIOME)` (`attach()` línea 80, `apply()` línea 154).

**Conclusión:** Existe un **mecanismo verificado** en el repositorio para obtener un `Registry<Biome>` válido sin lanzar Minecraft: `Bootstrap.bootStrap()` + `VanillaRegistries.createLookup()` + copia a `MappedRegistry` + `freeze()`. Ya usado por `VariantAttributeVerification`.

**Confianza:** ALTA — código existente en el repositorio, no una invención.

### Pregunta 3 — Verificar recursos y fixtures de GameTest

**Evidencia de repositorio:**

- `src/gametest/resources/data/mob_variants_brs/variants/ice_zombie.json` existe (idéntico al de `src/main/resources/`).
- `src/gametest/resources/fabric.mod.json` declara el entrypoint `fabric-gametest` apuntando a `VariantResourceGameTest`.
- El test existente `variantDefinitionFound` hace `server.getResourceManager().getResource(mob_variants_brs:variants/ice_zombie.json)` y **pasa** (evidencia de ejecución S-0077/S-0078).
- Esto demuestra que `src/gametest/resources` **se carga como datapack del servidor GameTest** en runtime (no solo existencia de archivo estático).
- `ResourceManager.listResources("variants", JSON_FILE)` resuelve contra `<pack>/data/<namespace>` y devuelve claves `namespace:variants/<archivo>` (listener, líneas 102, 166-178).

**Conclusión:** Ruta de verificación concreta con límites explícitos:
- El recurso válido `ice_zombie.json` **está probado como detectable y cargable** en GameTest (evidencia de ejecución S-0077).
- La existencia de un archivo en el repositorio **no es por sí sola prueba** de que Minecraft pueda descubrirlo/cargarlo; se requiere evidencia de ejecución (ya disponible para `ice_zombie.json` vía S-0077).
- Para un fixture de JSON inválido: agregar un archivo mal formado en `src/gametest/resources/data/mob_variants_brs/variants/` sería cargado por el mismo mecanismo (inferencia estática), pero el comportamiento de `load()` saltando y logueando el archivo inválido **no ha sido verificado en runtime**.

**Confianza:** ALTA para recurso válido (evidencia de ejecución); MEDIA/UNRESOLVED para fixture inválido en runtime.

### Pregunta 4 — Identificar la tarea real de GameTest

**Evidencia de repositorio:**

- `build.gradle` líneas 43-49: `fabricApi.configureTests { createSourceSet = true; enableGameTests = true; eula = true; clearRunDirectory = true; }`. No hay bloque `runGameTest` explícito.
- El reporte S-0077 documenta la ejecución exitosa de `.\gradlew.bat runGameTest` y que `compileGametestJava` corrió durante ella.
- El reporte S-0078 confirma que `runGameTest` completó exitosamente con tests pasando.

**Conclusión:** La tarea exacta es **`runGameTest`**, identificada por evidencia de ejecución (reporte S-0077), no solo por convención de nombre. `fabricApi.configureTests` con `enableGameTests = true` y `createSourceSet = true` en `build.gradle` la configura vía el plugin Loom/Fabric API.

**Confianza:** ALTA (evidencia de ejecución histórica); la tarea existe y se ha ejecutado, pero no se ha re-ejecutado en el workspace actual durante esta sesión.

---

## 5. Comparación de alternativas

Criterios aplicados a A, B y C: fiabilidad/determinismo; aislamiento de fallos esperados del resultado de la corrida de pruebas; compatibilidad con Loom 1.18.x / Fabric 0.155.3+26.1.2 / Minecraft 26.1.2 / Java 25 (sin ejecutar builds); complejidad de implementación y mantenimiento; capacidad de ejercer validación y gestión de errores reales de producción; riesgo de falsos positivos / pruebas que saltan la ruta de producción; disponibilidad de la infraestructura requerida.

Las tres coberturas se distinguen por la operación concreta que las ejerce y por el alcance de lo que demuestran:

- **A — Producción real:** invoca `VariantDefinitionReloadListener.attach(MinecraftServer)` en GameTest y afirma contra el snapshot publicado en los global attachments del servidor. Es la única cobertura que ejerce el parser de JSON, `VariantSnapshot.build` y el canal de publicación del listener.
- **B — Validación semántica aislada:** invoca `VariantSnapshot.build(...)` directamente con definiciones ya parseadas. No ejecuta el parser de JSON ni el listener de producción; no demuestra carga de recursos ni comportamiento de listener.
- **C — JSON mal formado:** requiere ejercer `load()` (o `attach()` que lo ejecuta internamente) con un fixture inválido. **PENDING** porque no se ha demostrado una estrategia de aislamiento que la permita sin bloquear A y B.

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

**Coberturas cubiertas por esta arquitectura:**

- **A — Producción real:** el caso positivo mejorado ejerce `VariantDefinitionReloadListener.attach(MinecraftServer)` en GameTest (único punto de entrada necesario; `load(ResourceManager)` se mantiene `private static` y no se expone) y afirma contra el snapshot leído de `server.globalAttachments()`. No se propone cambiar la visibilidad de `load()` ni usar reflexión para A.
- **B — Validación semántica aislada:** el camino de prueba aislado ejerce `VariantSnapshot.build(List<Map.Entry<Identifier,VariantDefinition>>, Registry<Biome>)` directamente con definiciones ya parseadas. No ejecuta el parser de JSON ni el listener de producción; no demuestra carga de recursos ni comportamiento de listener.
- **C — JSON mal formado:** **PENDING.** No se especifica en esta arquitectura. `VariantSnapshot.build(...)` no parsea JSON (recibe `VariantDefinition` ya deserializadas), por lo que no puede probar la omisión de un archivo mal formado ni el logging de producción de `load()`. La única forma de ejercer `load()` es vía `attach()`, y un fixture de JSON mal formado en `src/gametest/resources` sería cargado durante la inicialización del listener, alterando el comportamiento de arranque (Cobertura A). No se ha demostrado una estrategia de aislamiento que permita probar C sin bloquear A y B. No se añade ningún archivo a `src/gametest/resources`.

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
   - Esto ejerce la Cobertura A a través del único punto de entrada público disponible: `VariantDefinitionReloadListener.attach(MinecraftServer)`. No se requiere cambiar la visibilidad de `load(ResourceManager)` (se mantiene `private static`); `attach()` es público y ejecuta internamente `load()` + `VariantSnapshot.build()` + la publicación.
   - *Descubrimiento requerido en implementación:* confirmar accesibilidad de `globalAttachments()` y `VariantAttachments.SERVER_VARIANT_SNAPSHOT` desde el entorno de GameTest, y el `EntityType` de zombie disponible.

2. **Nuevo camino de prueba aislado para negativos (faltante e inválido) — Cobertura B:**
   - Ubicación por descubrir (posiblemente `src/test/java/...` si se añade un framework de pruebas, o una clase `main` vía `JavaExec` como `verifyVariantAttributes`). No se asume JUnit.
   - Invocar directamente `VariantSnapshot.build(prepared, biomeRegistry)` con entradas controladas:
     - variante con `entity_type` desconocido → se salta;
     - variante con bioma desconocido → se salta;
     - `conditions` vacío → se salta;
     - atributo no finito / desconocido / fuera de rango → se salta;
     - entrada válida (`ice_zombie`) → se acepta y aparece en `byVariantId` / `byEntityType`.
   - Afirmar que un identificador faltante (`mob_variants_brs:nonexistent`) **no está en `byVariantId`** y que una entrada válida sigue presente (no corrupción). Esto demuestra únicamente la ausencia del identificador en el snapshot resultante; **no** demuestra que el listener de producción maneje un archivo de recursos faltante, porque `VariantSnapshot.build` recibe definiciones ya parseadas y no consulta `ResourceManager`.
   - *Descubrimiento requerido:* cómo obtener un `Registry<Biome>` válido fuera del servidor (resuelto: `Bootstrap.bootStrap()` + `VanillaRegistries.createLookup()` + `MappedRegistry` + `freeze()`, como demuestra `VariantAttributeVerification.realBiomeRegistry()`). Esta camino no requiere invocar `load(ResourceManager)`, que es `private static`; la Cobertura B se ejerce únicamente sobre `VariantSnapshot.build`, que es `public static`.

3. **Fixtures de prueba para la Cobertura C (JSON mal formado):** **NO se añade de forma permanente.** Un archivo JSON mal formado en `src/gametest/resources/data/mob_variants_brs/variants/` sería cargado por el mismo datapack que `ice_zombie.json` durante la inicialización del listener en `attach()`, y su presencia puede alterar el comportamiento de arranque del listener (skip + log, snapshot parcial). Esta Cobertura **permanece PENDING** porque no se ha demostrado una estrategia de aislamiento que la permita sin bloquear las categorías A y B. No se añade ningún archivo a `src/gametest/resources` en esta fase. Si en el futuro se demuestra una estrategia viable (por ejemplo, un recurso temporal inyectado solo para un test concreto, o un camino que ejerza `load()` directamente), se especificaría en ese momento.

4. **Sin cambios en `build.gradle`** (no añadir `runGameTest`, `jvmArgs` ni `systemProperty`). **Sin introducir `mob_variants_brs.gametest.variantId`.** Sin modificar el comportamiento de producción (`load`, `apply`, `build`, `selectVariant` se mantienen igual).

---

## 8. Criterios de aceptación y validación futura (no ejecutar ahora)

Cada criterio está mapeado a una operación que el test propuesto ejecuta efectivamente. La construcción del snapshot por el test, la existencia de archivos o la inspección estática no constituyen prueba de comportamiento de runtime ajeno a la operación ejecutada.

Las categorías A, B y C de §4 son **independientes**: una Cobertura no cubre la otra, y el éxito de una no implica la de las demás.

| # | Cobertura | Criterio | Operación ejecutada por el test | Evidencia de que la operación es factible |
|---|-----------|----------|----------------------------------|-------------------------------------------|
| 1 | **A** | La prueba positiva pasa **solo si** el listener de producción publicó el snapshot y `ice_zombie` es seleccionable para zombies; falla si solo existe el archivo en `ResourceManager` pero no fue aceptado por producción. | `VariantDefinitionReloadListener.getInstance().attach(server)` en GameTest, luego leer `server.globalAttachments().getAttachedOrElse(SERVER_VARIANT_SNAPSHOT, EMPTY)` y afirmar `variantsFor(EntityType.ZOMBIE)` contiene `ice_zombie`. | `attach()` es público (listener línea 75); `globalAttachments()` probado como mismo almacén que `level.globalAttachments()` (S-0086); `runGameTest` ejecutado con éxito (S-0077). |
| 2 | **B** | La validación semántica rechaza entity type desconocido, condiciones vacías, bioma desconocido, atributo no finito, atributo desconocido y atributo fuera de rango; acepta definiciones válidas; no corrompe entradas no relacionadas. | Invocar `VariantSnapshot.build(prepared, biomeRegistry)` con entradas controladas (mismas categorías que en §4 B) y afirmar presencia/ausencia en `byVariantId`. | `VariantSnapshot.build` es `public static` (línea 72); `Registry<Biome>` obtenible vía `Bootstrap.bootStrap()` + `VanillaRegistries.createLookup()` (líneas 388-403 de `VariantAttributeVerification`). |
| 3 | **B** | Un identificador faltante (`mob_variants_brs:nonexistent`) no está en `byVariantId` y una entrada válida sigue presente (no corrupción). **Esto demuestra únicamente la ausencia del identificador en el snapshot resultante; no demuestra que el listener de producción maneje un archivo de recursos faltante**, porque `VariantSnapshot.build` recibe definiciones ya parseadas y no consulta `ResourceManager`. | Misma invocación a `VariantSnapshot.build` que el criterio 2, con una entrada que excluye el id faltante e incluye `ice_zombie`. | Misma infraestructura que el criterio 2. |
| 4 | **C** | **PENDING** — No hay criterio ejecutable actualmente. La Cobertura C requiere una estrategia de aislamiento demostrada para un fixture de JSON mal formado que no altere la inicialización del listener (ver §4 C). | Ninguna operación especificada hasta que se demuestre la estrategia. | `load()` salta y loguea archivos inválidos (líneas 120-123), pero su comportamiento en runtime con un fixture real en `src/gametest/resources` no se ha verificado. |
| 5 | — | Ninguna prueba asume `mob_variants_brs.gametest.variantId` ni requiere propiedades JVM en `build.gradle` o `argFiles`. | Las pruebas usan identificadores de recursos fijos (`mob_variants_brs:variants/ice_zombie.json`) y llamadas directas a métodos públicos; ningún `System.getProperty` ni `-D`. | `build.gradle` no declara `runGameTest`, `jvmArgs` ni `systemProperty` (líneas 43-49, 78-91); `argFiles/runGameTest` solo tiene `-classpath` (S-0081/S-0082). |
| 6 | — | Se compila con Java 25. | `tasks.withType(JavaCompile).options.release = 25` en `build.gradle` (línea 70). | Verificado estáticamente en `build.gradle`. |
| 7 | — | Comandos de validación futura (sin ejecutar en esta sessión). | Compilación: tarea de Gradle; verificación enfocada: `./gradlew verifyVariantAttributes` (JavaExec sobre `VariantAttributeVerification`); GameTests: `./gradlew runGameTest`. | `runGameTest` identificado por evidencia de ejecución (S-0077), no solo por convención; `verifyVariantAttributes` declarado en `build.gradle` líneas 78-83. |

**Regla de mapeo:** Un criterio se considera cubierto solo si existe una operación de test que lo ejerza. `VariantSnapshot.build()` llamado directamente cubre los criterios 2 y 3 (validación semántica, Cobertura B), pero **no** cubre el parser ni el listener de producción (criterio 1, Cobertura A, requiere `attach()` en GameTest). `VariantSnapshot.build` **no** puede cubrir ningún criterio de Cobertura C porque no parsea JSON (recibe `VariantDefinition` ya deserializadas). Consultar `ResourceManager` sin `attach()` no cubre ningún criterio de aceptación de producción.

---

## 9. Incertidumbres y riesgos pendientes (sin presentar como hechos)

### Resueltos por esta investigación

- **Alcance de attachments (servidor vs nivel):** RESUELTO por S-0086 (inspección de fuente Fabric API `26.1`/`26.2`). `ServerLevelMixin.globalAttachments()` delega a `server.globalAttachments()`; misma instancia por servidor. No se repetirá esta investigación salvo que aparezca evidencia contradictoria.
- **Registro de biomas fuera de servidor:** RESUELTO. `VariantAttributeVerification.realBiomeRegistry()` (líneas 388-403) construye `Registry<Biome>` real vía `Bootstrap.bootStrap()` + `VanillaRegistries.createLookup()` + `MappedRegistry` + `freeze()`, sin lanzar Minecraft.
- **Carga de recursos `src/gametest/resources`:** RESUELTO con límites. S-0077 documenta ejecución de `runGameTest` donde el test existente encuentra `mob_variants_brs:variants/ice_zombie.json` vía `server.getResourceManager()`, probando que el directorio se carga como datapack en GameTest. La existencia de archivo por sí sola no es prueba, pero la evidencia de ejecución sí lo es.
- **Nombre de la tarea de GameTest:** RESUELTO. Es `runGameTest`, verificado por ejecución documentada en S-0077 (no solo convención).

### Pendientes (bloqueadores materiales)

- **Propiedades JVM al servidor GameTest:** UNRESOLVED/BLOCKED (S-0081/S-0082). `build.gradle` no declara `runGameTest`, `jvmArgs` ni `systemProperty`; `argFiles/runGameTest` solo tiene `-classpath`. No se asume `mob_variants_brs.gametest.variantId`.
- **Prueba positiva en GameTest contra el snapshot publicado:** requiere `attach(MinecraftServer)` con un servidor real en el entorno GameTest. No se ha ejecutado en este workspace durante esta sessión (prohibido ejecutar Gradle/GameTests).
- **Fixture de JSON inválido en runtime (Cobertura C):** `load()` salta y loguea archivos inválidos (líneas 120-123), pero este comportamiento no se ha verificado en runtime con un fixture inválido real en `src/gametest/resources`. La Cobertura C permanece PENDING (ver §4 C y §8 criterio 4) porque no se ha demostrado una estrategia de aislamiento que permita probarla sin alterar la inicialización del listener (Cobertura A). No se añade ningún archivo a `src/gametest/resources`.
- **`selectVariant` sin entidad real:** no verificado si puede probarse sin spawn de entidad (depende de `ServerLevel` y `Entity`).

### Aislamiento de `load(ResourceManager)` (no es bloqueador para A ni B)

- `load(ResourceManager)` es `private static` (líneas 101-130) y por tanto **no es invocable directamente** desde un camino de prueba externo sin reflexión ni cambio de visibilidad de producción.
- Esto **no bloquea la Cobertura A**: `attach(MinecraftServer)` es público y ejecuta internamente `load()` + `VariantSnapshot.build()` + la publicación, por lo que es el único punto de entrada necesario para ejercer el listener de producción.
- Esto **no bloquea la Cobertura B**: ella se ejerce exclusivamente sobre `VariantSnapshot.build(List<Map.Entry<Identifier,VariantDefinition>>, Registry<Biome>)`, que es `public static` y recibe definiciones ya parseadas; no requiere invocar `load()`.
- La única cobertura que necesitaría ejercer `load()` directamente es la **C**, la cual permanece PENDING precisamente por la combinación de esta limitación de visibilidad con la ausencia de una estrategia de aislamiento de fixtures (ver §4 C). No se propone cambiar la visibilidad de `load()` ni usar reflexión para habilitar A o B.

---

## 10. Bloqueadores restantes y experimento mínimo necesario

| Bloqueador | Experimento mínimo (no ejecutar ahora) |
|------------|----------------------------------------|
| `load()` es `private static` | **No bloquea A ni B.** Para A, `attach()` es público y ejecuta `load()` internamente. Para B, `VariantSnapshot.build()` es `public static` y recibe definiciones ya parseadas. No se propone exponer `load()` ni usar reflexión. |
| `attach()` en GameTest sin ejecución | Ejecutar `./gradlew runGameTest` y observar si el test positivo contra `server.globalAttachments()` publica y lee el snapshot correctamente. |
| Fixture de JSON inválido en runtime (Cobertura C) | **PENDING — sin experimento definido.** `load()` salta y loguea archivos inválidos (líneas 120-123), pero este comportamiento no se ha verificado en runtime. Un fixture en `src/gametest/resources` afectaría a la inicialización del listener (Cobertura A), por lo que no se añade sin antes demostrar una estrategia de aislamiento. No bloquea A ni B. |
| Propiedades JVM al servidor GameTest | No hay experimento dentro del alcance actual; se descarta por instrucción. Cualquier alternativa debe funcionar sin `-D` ni `build.gradle` modificado. |

---

## 11. Implementación readiness

**Implementación NO autorizada por la evidencia actual.** No se ejecuta Gradle, builds, Minecraft ni GameTests en esta sessión.

La estrategia de prueba del listener de producción es **técnicamente factible en parte**, pero no completamente determinada. El estado se desglosa por Cobertura (ver §4):

### 11.1. Tests suficientemente especificados para futura implementación

| Cobertura | Estado | Fundamento |
|-----------|--------|------------|
| **A** — Integración positiva | **ESPECIFICADO, no ejecutado** | `attach(MinecraftServer)` es público (listener línea 75) y ejecuta `load()` + `VariantSnapshot.build()` + publicación. El snapshot se lee vía `server.globalAttachments().getAttachedOrElse(SERVER_VARIANT_SNAPSHOT, EMPTY)`. S-0086 confirma que `server.globalAttachments()` y `level.globalAttachments()` resuelven al mismo almacén. `runGameTest` es una tarea ejecutable (S-0077). Lo que falta: ejecutar `./gradlew runGameTest` para confirmar que la publicación y lectura funcionan en el servidor GameTest real. |
| **B** — Validación semántica aislada | **ESPECIFICADO, no ejecutado** | `VariantSnapshot.build(...)` es `public static` (línea 72). El `Registry<Biome>` real se obtiene sin lanzar Minecraft mediante la secuencia ya demostrada por `VariantAttributeVerification.realBiomeRegistry()` (líneas 388-403). Las propiedades semánticas a construir y asercionar se listan en §4 B. Lo que falta: escribir y ejecutar el camino de prueba. |

### 11.2. Checks que requieren ejecución y validación real

- **Cobertura A:** `./gradlew runGameTest` con el test modificado para confirmar que `attach()` publica y que la lectura del attachment recupera `ice_zombie` para `EntityType.ZOMBIE`. No se ha ejecutado en este workspace durante esta sessión (prohibido).
- **Cobertura B:** ejecución del camino aislado (JavaExec como `verifyVariantAttributes` o nuevo task) para confirmar que las aserciones de rechazo/aceptación se cumplen contra los registros reales de Minecraft 26.1.2.
- **Accesibilidad en runtime de `globalAttachments()` / `SERVER_VARIANT_SNAPSHOT` / `EntityType.ZOMBIE` desde el entorno GameTest:** por confirmar en la implementación.

### 11.3. Cobertura bloqueada por ausencia de estrategia de aislamiento demostrada

- **Cobertura C — JSON mal formado:** **BLOQUEADA (PENDING).** `VariantSnapshot.build(...)` no parsea JSON (recibe `VariantDefinition` ya deserializadas), por lo que no puede probar la omisión de un archivo mal formado ni el logging de producción de `load()`. La única forma de ejercer `load()` es vía `attach()`, y un fixture de JSON mal formado en `src/gametest/resources` sería cargado durante la inicialización del listener, alterando el comportamiento de arranque (Cobertura A). No se ha demostrado una estrategia de aislamiento que permita probar Cobertura C sin bloquear A y B. **No se añade ningún archivo a `src/gametest/resources`.** Esta Cobertura no bloquea A ni B.

**Decisión:** Implementación de las Coberturas A y B procede cuando un agente de implementación esté autorizado a ejecutar Gradle/GameTests. La Cobertura C permanece PENDING hasta que se demuestre una estrategia de aislamiento. No se requiere cambiar la visibilidad de `load()` (private static) para A ni B: `attach()` es el único punto de entrada necesario.

---

## 12. Siguiente paso concreto

Un agente de implementación debe:

1. **Confirmar accesibilidad de `attach()` desde un test GameTest:** escribir una versión mejorada de `VariantResourceGameTest.variantDefinitionFound` que, Además de verificar el recurso en `Manager`, llame `VariantDefinitionReloadListener.getInstance().attach(server)` (punto de entrada público de Cobertura A) y luego lea `server.globalAttachments().getAttachedOrElse(VariantAttachments.SERVER_VARIANT_SNAPSHOT, VariantSnapshot.EMPTY)` para afirmar que `ice_zombie` está en `byVariantId` / `variantsFor(EntityType.ZOMBIE)`.
2. **Ejecutar `./gradlew runGameTest`** para confirmar que el caso positivo pasa contra el snapshot publicado por producción.
3. **Cobertura B — camino aislado de negativos:** implementar y ejecutar una invocación directa a `VariantSnapshot.build(prepared, biomeRegistry)` con entradas controladas (punto de entrada público estático, sin tocar `load()`), usando `Registry<Biome>` construido vía `Bootstrap.bootStrap()` + `VanillaRegistries.createLookup()` + `MappedRegistry` + `freeze()` (como `VariantAttributeVerification.realBiomeRegistry()`).
4. **Cobertura C:** **PENDING.** No se especifica un camino concreto hasta que se demuestre una estrategia de aislamiento que permita ejercer `load()` (o `attach()` con un fixture inválido) sin bloquear A y B. No se añade ningún fixture a `src/gametest/resources` en esta fase.

No se requiere cambiar la visibilidad de `load()` (se mantiene `private static`): `attach()` es el punto de entrada público para la Cobertura A y `VariantSnapshot.build(...)` es el punto de entrada público para la Cobertura B.

Sin editar `build.gradle`, sin introducir `mob_variants_brs.gametest.variantId`, y sin modificar el comportamiento de producción.

**Plan guardado en:** `.kilo/plans/1792298894695-mobvariants-brs-gametest-alternativas.md`
