# MobVariantsBRS — Variant Texture Rendering Architecture (read-only investigation)

**Status:** architectural design only. Nothing in this document has been implemented.
**Target stack (verified from `gradle.properties`):** MC `26.1.2`, Fabric Loader `0.19.5`, Fabric API `0.155.3+26.1.2`, Java `25`, Loom `1.18-SNAPSHOT`, official Mojang mappings.

---

## 1. Current State

### 1.1 Repository reality (VERIFIED)

The working tree is an **unmodified Fabric template**. There is no attachment, definition,
selection, or rendering code on disk.

| File | Content |
|---|---|
| `build.gradle` | stock Loom template; `implementation net.fabricmc.fabric-api:fabric-api:${fabric_version}`; no MixinExtras; no test source set |
| `gradle.properties` | versions above; `maven_group=com.baruc`, `archives_base_name=mob_variants_brs` |
| `src/main/java/com/baruc/brs/mobvariants/MobVariantsBRS.java` | `ModInitializer` registering one `ResourceManagerHelper` listener |
| `src/client/java/com/baruc/brs/mobvariants/client/MobVariantsBRSClient.java` | `ClientModInitializer`, empty `onInitializeClient` |
| `src/main/resources/mob_variants_brs.mixins.json` | `ExampleMixin` (inert template) |
| `src/client/resources/mob_variants_brs.client.mixins.json` | `ExampleClientMixin` (inert template) |
| `docs/ARCHITECTURE.md`, `docs/DECISIONS.md` | design only; D-006…D-018 are *proposed*, not implemented |
| `.kilo/plans/1790903055742-mob-variants-brs-architecture.md` | the approved prior plan |

**Consequence:** every "existing architectural decision" named in the task brief (attachment,
definitions, selection, sync, persistence) exists only as a *documented decision*. This
investigation is therefore free to pick any transport it can justify, as long as it does not
contradict those decisions.

### 1.2 The invalidated proposal (VERIFIED)

Prior plan §6.2: *"exactly one client Mixin … the vanilla method on the shared living/mob
renderer base class that resolves an entity's texture. In 1.21.x mojmap this was
`LivingEntityRenderer#getTextureLocation(LivingEntity)`."*

Both halves are now falsified for 26.1.2:

* the parameter is a **RenderState**, not the entity (§2.1);
* the method is **`abstract`**, so a base-class inject cannot supply a universal
  implementation (§2.3).

Decision D-015 ("exactly one client Mixin") and D-016 ("target name UNVERIFIED") are therefore
superseded. See §11.

---

## 2. Verified API and Mapping Findings

### 2.0 Evidence base and its strength

| Source | What it is | Strength |
|---|---|---|
| `aldak0.ru/javadoc/26.1.x/…` | NeoForge `26.1.2-26.1.2.109` javadoc of MC 26.1.2 | **Signatures/overrides: strong.** Method *bodies*: none. NeoForge-patched, so field/method sets may include NeoForge additions. |
| `github.com/FabricMC/fabric` branch `26.1` | Fabric API source for the 26.1 line | **Mixin target descriptors: strongest.** Compiled against the 26.1 line. |
| `maven.fabricmc.net/docs/fabric-api-0.155.3+26.1.2/` | exact-version javadoc of the installed Fabric API | **Fabric API surface: exact.** |
| `.gradle/loom-cache/source_mappings/bc646172….tiny` | local Loom intermediary→official tiny | **Package layout corroboration.** |
| local MC jars under `.gradle/loom-cache/minecraftMaven/` and `~/.gradle/caches/fabric-loom/26.1.2/` | remapped 26.1.2 jars | **Could not be read.** No `javap`, no decompiler and no archive-extraction tool is permitted in this environment; jar entry contents are DEFLATE-compressed. |

Two important environment facts:

* **VERIFIED** — from the `mappings.dev` index page: *"On 29th October 2025, Mojang has announced
  that they will stop obfuscating Minecraft: Java Edition builds."* 26.1.2 therefore ships with
  readable names, and "official Mojang mappings" in this project are the *shipped* names. This
  removes remapping risk but does **not** create an API-stability guarantee (§9.7).
* **VERIFIED** — no jar extraction/decompilation was possible. Consequently **every claim below
  about a method *body* is UNVERIFIED** and is listed as such in §13.

### 2.1 `LivingEntityRenderer` — verified

```
net.minecraft.client.renderer.entity.LivingEntityRenderer<T extends LivingEntity,
                                                          S extends LivingEntityRenderState,
                                                          M extends EntityModel<? super S>>
    extends net.minecraft.client.renderer.entity.EntityRenderer<T, S>
    implements net.minecraft.client.renderer.entity.layers.RenderLayerParent<S, M>

  fields:   protected M model
            protected final ItemModelResolver itemModelResolver
            protected final List<RenderLayer<S, M>> layers

  public final boolean addLayer(RenderLayer<S, M> layer)
  public M getModel()
  public abstract Identifier getTextureLocation(S state)                      <-- ABSTRACT
  protected @Nullable RenderType getRenderType(S state,
                                               boolean isBodyVisible,
                                               boolean forceTransparent,
                                               boolean appearGlowing)
  public void extractRenderState(T entity, S state, float partialTicks)
  public void submit(S state, PoseStack poseStack, SubmitNodeCollector collector,
                     CameraRenderState camera)
  protected int getModelTint(S state)
  protected boolean shouldRenderLayers(S state)
  protected boolean isBodyVisible(S state)
  protected AABB getBoundingBoxForCulling(T entity)
  protected float getShadowRadius(S state)
  public static int getOverlayCoords(LivingEntityRenderState state, float whiteOverlayProgress)

  direct known subclasses: ArmorStandRenderer, AvatarRenderer, MobRenderer
```

The exact base-class descriptor is independently confirmed by Fabric API's own mixin
(`fabric-rendering-v1`, branch 26.1, `LivingEntityRendererMixin`):

```java
@WrapOperation(
    method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;"
           + "Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
    at = @At(value = "INVOKE",
             target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;"
                    + "shouldRender(Lnet/minecraft/world/item/ItemStack;"
                    + "Lnet/minecraft/world/entity/EquipmentSlot;)Z"))
```

`Identifier` is `net.minecraft.resources.Identifier`. `PoseStack` is
`com.mojang.blaze3d.vertex.PoseStack`. `SubmitNodeCollector` is
`net.minecraft.client.renderer.SubmitNodeCollector`. `CameraRenderState` is
`net.minecraft.client.renderer.state.level.CameraRenderState`.

### 2.2 `EntityRenderer` / render states — verified

```
net.minecraft.client.renderer.entity.EntityRenderer<T extends Entity, S extends EntityRenderState>
  public abstract S createRenderState()
  public final    S createRenderState(T entity, float partialTicks)
  public void     extractRenderState(T entity, S state, float partialTicks)
  protected void  finalizeRenderState(T entity, S state)
  public void     submit(S state, PoseStack, SubmitNodeCollector, CameraRenderState)
  public boolean  shouldRender(T entity, Frustum, double, double, double)
  protected AABB  getBoundingBoxForCulling(T entity)

net.minecraft.client.renderer.entity.state.EntityRenderState
  public fields: entityType, x/y/z, ageInTicks, boundingBoxWidth/Height, eyeHeight,
                 distanceToCameraSq, isInvisible, isDiscrete, displayFireAnimation,
                 lightCoords, outlineColor, passengerOffset, nameTag, scoreText,
                 nameTagAttachment, leashStates, shadowRadius, shadowPieces, partialTick
  boolean appearsGlowing(); void fillCrashReportCategory(CrashReportCategory)
  nested: static class LeashState, static final record ShadowPiece

net.minecraft.client.renderer.entity.state.LivingEntityRenderState extends EntityRenderState
  public fields: bodyRot, yRot, xRot, deathTime, walkAnimationPos/Speed, scale, ageScale,
                 ticksSinceKineticHitFeedback, isUpsideDown, isFullyFrozen, isBaby, isInWater,
                 isAutoSpinAttack, hasRedOverlay, isInvisibleToPlayer, bedOrientation, pose,
                 headItem, wornHeadAnimationPos, wornHeadType, wornHeadProfile
  boolean hasPose(Pose pose)
```

`EntityRenderState` and `LivingEntityRenderState` also inherit `getRenderData`,
`setRenderData`, `resetRenderData` from `net.neoforged.neoforge.client.renderstate.BaseRenderState`
and implement `IRenderStateExtension`. **That class is a NeoForge patch artifact and does not
exist on Fabric.** It is cited only as independent evidence that Mojang's render-state objects
are designed to carry out-of-band extension data.

`EntityRenderState` has a **`reset()`** method — established from Fabric API's
`renderstate.LevelRenderStateMixin`, which injects `clearExtraData()` at `@At("TAIL")` of
`LevelRenderState#reset`. `EntityRenderState#reset()` exists too; its body is UNVERIFIED.

### 2.3 Texture selection is implemented per renderer family — verified

`MobRenderer` (the root of every mob renderer) implements **neither**
`getTextureLocation` nor `getRenderType`. Concrete facts:

| Renderer | Declares `getTextureLocation` | Declares `getRenderType` | Declares `extractRenderState` | Declares `submit` |
|---|---|---|---|---|
| `MobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>>` | no (still abstract) | no | no | no |
| `ZombieRenderer` | **no** | no | no | no |
| `AbstractZombieRenderer` | **yes** (`ZOMBIE_LOCATION`, `BABY_ZOMBIE_LOCATION`) | no | **yes** (overrides `HumanoidMobRenderer`) | no |
| `HumanoidMobRenderer` | no | no | no (`extractHumanoidRenderState`) | no |
| `AgeableMobRenderer` | no | no | no | **yes** |
| `ArmorStandRenderer` | **yes** | **yes** | — | — |
| `CodRenderer`, `BlazeRenderer` | **yes** | no | — | — |
| `ZombieVillagerRenderer` (26.2 javadoc, same design) | **yes** | no | **yes** | — |

Verified zombie hierarchy:

```
ZombieRenderer<Zombie, ZombieRenderState, ZombieModel<ZombieRenderState>>
  extends AbstractZombieRenderer  (getTextureLocation, extractRenderState, isShaking, getArmPose)
  extends HumanoidMobRenderer    (extractHumanoidRenderState)
  extends AgeableMobRenderer     (submit)
  extends MobRenderer
  extends LivingEntityRenderer   (getTextureLocation abstract, getRenderType, submit, model field)
  extends EntityRenderer
```

Two consequences that decide this design:

1. **`getTextureLocation` cannot be intercepted generically.** It is abstract on
   `LivingEntityRenderer` and concretely overridden in each renderer family. There is no base
   body to redirect, and there is no base body to reimplement.
2. **`getRenderType` is the first *shared, non-abstract* code on the texture path.** It is
   implemented once on `LivingEntityRenderer` and inherited by `MobRenderer` and the entire mob
   subtree. It is overridden by at least one renderer (`ArmorStandRenderer`, verified).

### 2.4 Texture → RenderType → GPU — verified

```
net.minecraft.client.model.EntityModel<T extends EntityRenderState> extends Model<T>
  protected EntityModel(ModelPart root)
  protected EntityModel(ModelPart root, Function<Identifier, RenderType> renderType)

net.minecraft.client.model.Model
  protected final Function<Identifier, RenderType> renderType   // factory field
  public final  Function<Identifier, RenderType> renderType()
  public final  RenderType renderType(Identifier texture)       // Identifier -> RenderType
```

```
net.minecraft.client.renderer.OrderedSubmitNodeCollector
  <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack,
                       RenderType renderType, int lightCoords, int overlayCoords,
                       int outlineColor, @Nullable ModelFeatureRenderer.CrumblingOverlay)
  default <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack,
                               Identifier texture, int lightCoords, int overlayCoords,
                               int outlineColor, @Nullable …CrumblingOverlay)
  (plus submitModelPart / submitItem / submitShadow / submitFlame / submitLeash / …)

net.minecraft.client.renderer.SubmitNodeCollector extends OrderedSubmitNodeCollector
```

`net.minecraft.client.renderer.rendertype.RenderType` exists with
`create(String, RenderSetup)` (corroborated by the local Loom tiny). **`RenderType` is an
opaque value: this plan never attempts to rewrite the texture *inside* an existing
`RenderType`.** Substitution happens strictly on the `Identifier`, *before* the
`Identifier → RenderType` conversion. That is the only mechanism whose behaviour is fully
determined by verified signatures.

### 2.5 Dispatcher — verified

```
net.minecraft.client.renderer.entity.EntityRenderDispatcher
  private Map<EntityType<?>, EntityRenderer<?, ?>> renderers
  public  <E extends Entity> EntityRenderState extractEntity(E entity, float partialTicks)
  public  <S extends EntityRenderState> void submit(S renderState, CameraRenderState camera,
                                                   double x, double y, double z,
                                                   PoseStack poseStack, SubmitNodeCollector collector)
  public  <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T entity)
  public  <S extends EntityRenderState> EntityRenderer<?, ? super S> getRenderer(S renderState)
  public  void prepare(Camera, Entity)
  public  boolean shouldRender(E, Frustum, double, double, double)
```

There is **no field holding a pooled/reused render state**, and `extractEntity` returns a fresh
`EntityRenderState`. Combined with the absence of an `EntityRenderStateMixin` in Fabric API's
`fabric-rendering-v1.mixins.json` (§2.6) — i.e. `clearExtraData()` is never called for entity
render states — this is strong evidence that **entity render states are allocated per entity per
frame**. The bodies of `extractEntity` / `createRenderState(T, float)` are still UNVERIFIED.

### 2.6 Fabric API `0.155.3+26.1.2` — verified

`net.fabricmc.fabric.api.client.rendering.v1` (exact-version javadoc) contains:

| Type | Notes |
|---|---|
| `FabricRenderState` | `<T> @Nullable T getData(RenderStateDataKey<T>)`, `<T> T getDataOrDefault(RenderStateDataKey<T>, T)`, `<T> void setData(RenderStateDataKey<T>, T)`, `void clearExtraData()` |
| `RenderStateDataKey<T>` | `static <T> RenderStateDataKey<T> create(Supplier<String> debugName)`, `static <T> RenderStateDataKey<T> create()` |
| `LivingEntityRenderLayerRegistrationCallback` | `registerLayers(EntityType, LivingEntityRenderer, RegistrationHelper, Context)`; `RegistrationHelper#register(RenderLayer)` — **additive layers only** |
| `EntityRendererRegistry` | **`@Deprecated`** — "Replaced with transitive access wideners in Fabric Transitive Access Wideners (v1)" |
| `ArmorRenderer` | armor *items* only |
| `LivingEntityFeatureRenderEvents` | per-feature allow/deny |
| `FabricModel<S>` | `getChildPart(String)`, `copyTransforms(Model<?>)` — **no texture/renderType hook** (confirmed by reading `ModelMixin`, which only adds a child-part map) |

Storage implementation — `fabric-rendering-v1 … renderstate/RenderStateMixin.java`, branch
26.1 (verbatim behaviour):

```java
@Mixin({ …, EntityRenderState.class, EntityRenderState.LeashState.class, … })
abstract class RenderStateMixin implements FabricRenderState {
    @Unique @Nullable private Map<RenderStateDataKey<?>, Object> renderStateData;

    public <T> @Nullable T getData(RenderStateDataKey<T> key) {
        return renderStateData == null ? null : (T) renderStateData.get(key);
    }
    public <T> T getDataOrDefault(RenderStateDataKey<T> key, T def) {
        return renderStateData == null ? def : (T) renderStateData.getOrDefault(key, def);
    }
    public <T> void setData(RenderStateDataKey<T> key, T value) {
        if (renderStateData == null) renderStateData = new Reference2ObjectOpenHashMap<>();
        renderStateData.put(key, value);
    }
    public void clearExtraData() { if (renderStateData != null) renderStateData.clear(); }
}
```

* `EntityRenderState` is in the `@Mixin` list, so every subclass — including
  `LivingEntityRenderState` — implements `FabricRenderState`. **VERIFIED.**
* Storage is **one lazily created `Reference2ObjectOpenHashMap` per render-state instance**,
  holding `Object` references. Keys are compared **by reference** (`Reference2ObjectOpenHashMap`).
* `setData` allocates a map on the first call **even when the value is `null`**.
* `clearExtraData` clears but does **not** null the field.
* Full `fabric-rendering-v1` client mixin list (26.1) contains
  `renderstate.{BlockModelRenderStateMixin, GuiRenderStateMixin,
  ItemStackRenderStateLayerRenderStateMixin, ItemStackRenderStateMixin, LevelRenderStateMixin,
  ParticlesRenderStateMixin, RenderStateMixin, SkyRenderStateMixin, WeatherRenderStateMixin,
  WorldBorderRenderStateMixin}` — **no `EntityRenderStateMixin`**. Only the long-lived,
  `reset()`-based states get automatic clearing.

### 2.7 MixinExtras availability

Fabric API's 26.1 rendering module uses `com.llamalad7.mixinextras` (`@WrapOperation`,
`@Local`) in shipped code, so it is on the runtime classpath. **INFERENCE (high confidence):**
Fabric Loader bundles MixinExtras. The project does **not** declare it, so compile-time
availability is **UNVERIFIED**. Per the no-new-dependencies constraint, this plan is written to
use **only plain Mixin injectors** (`@Inject`, `@Redirect`, `@Shadow`, `@Unique`,
`CallbackInfoReturnable`), which need no additional artifact.

---

## 3. Candidate Strategies

### Strategy A — `extractRenderState` + `FabricRenderState` + `getRenderType` substitution

| Sub-step | Verdict |
|---|---|
| Read the attachment in `extractRenderState` | **Viable.** Method, descriptor, and `(T entity, S state, float)` shape all verified (§2.1). |
| Store via `FabricRenderState.setData` | **Viable but rejected on correctness + performance.** Requires an *unconditional* write to guarantee no stale texture leaks from a previous entity (§6.2). `setData` allocates a `Reference2ObjectOpenHashMap` on first call **even for a `null` value** (§2.6), so an unconditional write allocates one fastutil map **per rendered living entity per frame**. `clearExtraData()` is never invoked for `EntityRenderState` by Fabric API (§2.5/§2.6), so a conditional write would be safe only under the *unverified* assumption that states are never reused. |
| Substitute in `getRenderType` | **Viable, with a verified coverage gap.** `getRenderType` is the shared non-abstract code on the texture path, but `ArmorStandRenderer` overrides it (§2.3). It is `@Nullable`, so re-implementing it in an inject would also forfeit vanilla translucency/outline handling unless the vanilla body is reproduced verbatim. |
| `EntityModel#renderType(Identifier)` | **Verified** as the `Identifier → RenderType` factory. Substitution *before* this call is the only fully-determined mechanism (§2.4). |

### Strategy B — RenderState extension via Mixin/interface

* **Exact target:** `net.minecraft.client.renderer.entity.state.LivingEntityRenderState`
  (not `EntityRenderState` — narrower, and sufficient because only `LivingEntityRenderer` is
  targeted).
* **Shared or per-renderer?** The *class* is shared by every living renderer; the *instance* is
  per entity per frame (strong evidence, §2.5). Adding one `@Unique @Nullable Identifier` field
  therefore adds 8 bytes per state instance and no indirection.
* **Written** in the `extractRenderState` head injection; **read** in the `getRenderType` redirect.
* **Injection stability:** the class name and package are confirmed by two independent sources
  (Fabric API source + javadoc) and corroborated by the local Loom tiny. `LivingEntityRenderState`
  is not a `@Unique`/synthetic class.
* **Coupling:** one Mixin class + one accessor interface. No coupling to *any* method body.
* **Allocation:** zero. An unconditional write of `null` or an `Identifier` is a single
  reference store. This is the decisive property (§6).
* **Compatibility:** a `@Unique` field is invisible to other mods; it cannot collide with
  another mod's field. It is a single extra injection target on a public Mojang class.
* **Not created in this phase**, per the task constraints.

### Strategy C — Renderer-specific Mixins

Refuted on verified evidence, not on taste:

* `MobRenderer` leaves `getTextureLocation` abstract (§2.3), so **essentially every concrete mob
  renderer declares its own** `getTextureLocation` (verified for `AbstractZombieRenderer`,
  `CodRenderer`, `BlazeRenderer`, `ArmorStandRenderer`, `ZombieVillagerRenderer`). A per-renderer
  mixin is therefore required **per mob type**, not per family — the exact cost R-004/§7 of the
  prior plan tried to avoid, and worse: each concrete renderer is at a *different* depth of a
  different hierarchy, so a single mixin cannot even be shared across two renderers.
* `getRenderType` overrides (e.g. `ArmorStandRenderer`) add a second, independent mixin axis.
* `submit` overrides (e.g. `AgeableMobRenderer`) add a third.
* No single mixin target is universal → 3 × N mixins for N mob types, with N new breakage points
  per Minecraft release.

### Strategy D — Fabric API mechanism without Mixin

Explicitly checked, all against the installed API (exact-version javadoc + source):

| Mechanism | Verdict |
|---|---|
| `LivingEntityRenderLayerRegistrationCallback` | Additive only (`RegistrationHelper#register(RenderLayer)`). Cannot replace the body texture. |
| `FabricRenderState` | Transport only. It carries data; it does not intercept vanilla calls. |
| `EntityRendererRegistry` | `@Deprecated` in `0.155.3+26.1.2`, and per-`EntityType` anyway (R-001). |
| `FabricModel<S>` | `getChildPart` / `copyTransforms` only. No `renderType` hook (confirmed in `ModelMixin`). |
| `ArmorRenderer` registry | Armor *items*, not mob body textures. |
| `LivingEntityFeatureRenderEvents` | `ALLOW_CAPE_RENDER` and similar boolean gates; no texture control. |

**No official Fabric API mechanism exists that replaces the texture of an existing
`LivingEntityRenderer` without a Mixin.** A Mixin is required for the substitution half. This
statement is evidence-backed, not an assumption.

---

## 4. Renderer Lifecycle

Numbered per the brief. Every step names a class/method that appears in §2.

| # | Phase | Verified code | Data available |
|---|---|---|---|
| 1 | Where the entity exists | `EntityRenderDispatcher#shouldRender(E, Frustum, double,double,double)`; entity from `LevelRenderer` | full `LivingEntity` instance |
| 2 | Where the render state is created | `EntityRenderDispatcher#extractEntity(E, float)` → `EntityRenderer#createRenderState(T entity, float partialTicks)` (final) → `EntityRenderer#createRenderState()` (abstract) | entity **and** state at the same time |
| 3 | Where `extractRenderState` executes | `EntityRenderer#extractRenderState(T, S, float)`, overridden by `HumanoidMobRenderer`/`AbstractZombieRenderer` etc., bottoming out in `LivingEntityRenderer#extractRenderState(T, S, float)` | entity + state |
| 4 | Where variant data can be read | **Step 2 and step 3.** Attachment read: `Entity#getAttached(AttachmentType<Identifier>)` (Fabric `fabric-data-attachment-api-v1`; the `AttachmentType<Identifier>` decision is unchanged) | — |
| 5 | Where texture information is obtained | `LivingEntityRenderer#getTextureLocation(S)` — **abstract**, concretely declared per renderer family (§2.3) | state only |
| 6 | Where `RenderType` is determined | `LivingEntityRenderer#getRenderType(S, boolean, boolean, boolean)` → `@Nullable RenderType`; **inherited by `MobRenderer` and the whole mob subtree**, overridden by e.g. `ArmorStandRenderer` | state + the three vanilla flags |
| 7 | Where `EntityModel#renderType(Identifier)` is involved | `Model#renderType(Identifier)` / the `Function<Identifier, RenderType>` field; i.e. the `Identifier → RenderType` step performed by `getRenderType` | `Identifier` |
| 8 | Where the model is submitted | `LivingEntityRenderer#submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)` (overridden by `AgeableMobRenderer`) → `SubmitNodeCollector#submitModel(Model<? super S>, S, PoseStack, RenderType, int, int, int, @Nullable CrumblingOverlay)` | state, `RenderType` |
| 9 | Where the final texture is consumed | inside the `RenderType`'s `RenderSetup`; the model is drawn by the submit pipeline, not by the renderer | — |

**INFERENCE (strong, not verified):** the body of `LivingEntityRenderer#getRenderType` calls
`this.getTextureLocation(state)` (directly, or via `this.model.renderType(...)`) and derives the
cutout / translucent / emissive / outline `RenderType` from it. Steps 5→6→7 then form a single
call chain inside one shared base method, which is precisely what makes step 6 a usable single
interception point.

**UNVERIFIED:** the bodies of `getRenderType`, `submit`, `extractRenderState`, and
`createRenderState(T, float)`; specifically the exact `INVOKE` instruction targeted by the
redirect proposed in §7. Confirming this is task #1 of the implementation phase (§12).

**Also UNVERIFIED:** whether every concrete renderer calls `super.extractRenderState(...)`.
Verified for the zombie chain (`AbstractZombieRenderer` "Overrides: `extractRenderState` in class
`HumanoidMobRenderer`"). A subclass that skipped `super` would simply never receive a variant
texture — a silent no-op, not a crash (safe degradation, §10).

---

## 5. Technical Comparison

Purely factual. No scores, no rankings.

| # | Dimension | A: FabricRenderState transport | B: Mixin-extended render state | C: Renderer-specific Mixins | D: No-Mixin Fabric API |
|---|---|---|---|---|---|
| 1 | MC 26.1.2 compatibility | Compatible: `FabricRenderState` is implemented on `EntityRenderState` in the installed API | Compatible: `LivingEntityRenderState` verified to exist and be the base of all living render states | Compatible, but must track ~3 × N renderer classes | **Not applicable — no such API exists (§3-D)** |
| 2 | Fabric API 0.155.3+26.1.2 compatibility | Full; documented, public, versioned | Full; no Fabric dependency at all | Full | n/a |
| 3 | Client/server compatibility | Client-only classes; identical for both | Identical | Identical | n/a |
| 4 | Dedicated-server safety | Safe if all classes live under `src/client` and the mixin config is `"client"`-only | Identical | Identical | n/a |
| 5 | Mojang-mappings compatibility | Only Mojang class names used | Only Mojang class names used | ~3 × N Mojang class names used | n/a |
| 6 | Injection-point stability | 1 Mojang method (`extractRenderState`) + 1 Fabric interface | 1 Mojang method + 1 Mojang class | ~3 × N Mojang methods/classes | n/a |
| 7 | Dependence on implementation internals | Transport: none. Substitution: depends on a vanilla **call site** inside `getRenderType` | Transport: none. Substitution: identical dependence to A | Depends on every concrete implementation | n/a |
| 8 | Performance | One `Reference2ObjectOpenHashMap` allocated per rendered living entity per frame when written unconditionally (§2.6) | One reference store per rendered living entity per frame | Identical runtime cost to A/B; cost is maintenance, not CPU | n/a |
| 9 | Memory allocations | Non-zero, per-frame, per-entity | **Zero** | Zero (same as B) | n/a |
| 10 | Attachment access cost | 1 attachment lookup per rendered living entity per frame | Identical | Identical | n/a |
| 11 | Texture-resolution cost | 1 immutable-map lookup by variant id, or **0** if the resolved `Identifier` is precomputed at resource reload and stored on the attachment value (R-007) | Identical | Identical | n/a |
| 12 | RenderState interaction | External side map, reference-compared keys, never auto-cleared for entity states | A real field on the state; unconditional write is natural and stale-free | Identical to A/B | n/a |
| 13 | Scalability across mob renderers | One write mixin + one read mixin | Identical | Linear in mob count, ×3 axes | n/a |
| 14 | Scalability across variants | O(1) lookup; unaffected | Identical | Identical | n/a |
| 15 | Maintainability | 1 Fabric key + 1 constant | 1 field + 1 accessor interface | 3 × N files | n/a |
| 16 | Complexity | Low; but the null-write forces a per-entity map allocation | Low; 3 small client Mixins | High and growing with every mob | n/a |
| 17 | Future MC-version impact | Breaks if `LivingEntityRenderer#extractRenderState` or `FabricRenderState`'s presence on `EntityRenderState` changes | Breaks if `LivingEntityRenderState` or `extractRenderState` changes; **no dependence on Fabric internals** | Breaks on every new mob renderer and on every moved override | n/a |
| 18 | Interaction with other mods | Mixin conflict only at the shared call site in `getRenderType`; identical for B | Identical conflict surface | Fewer conflicts per site, but far more sites | n/a |

---

## 6. Recommended Architecture

Selection against the stated criteria, in order (Performance, Compatibility, Correctness,
Stability, Modularity, Maintainability, Scalability). The transport choice is decided by
Performance **and** Correctness together, which agree.

### 6.1 Selected shape

A **two-stage client pipeline: one Mixin-extended render state as transport, one shared
base-class call-site substitution as the read point.**

```
  attachment (entity)                 RenderState                    submission
  ───────────────────                 ───────────                    ──────────
  extractRenderState HEAD  ──write──▶  LivingEntityRenderState
  @Inject                            +@Unique Identifier          getRenderType body
    entity.getAttached(VARIANT)         mobvariants$texture            │
    → definition lookup                 (null when no variant)        │ calls
    → precomputed texture Identifier                                   ▼
                                                          getTextureLocation(S)  [redirected]
                                                                     │
                                                                     ▼
                                                        Model#renderType(Identifier)
                                                                     │
                                                                     ▼
                                        SubmitNodeCollector#submitModel(…, RenderType, …)
```

### 6.2 Why transport B rather than `FabricRenderState`

Both are viable. `FabricRenderState` is rejected as the transport for one concrete reason that
is a **correctness** requirement plus a **performance** fact:

* **Correctness requirement.** The write must be **unconditional** — every rendered living
  entity must write its own value, including `null` — otherwise a texture left over from a
  previously rendered entity of the same type would be shown on the wrong mob. Skipping the
  write is only safe if render states are provably never reused. That is strong evidence
  (§2.5) but not proof (bodies UNVERIFIED).
* **Performance fact** (`RenderStateMixin`, verbatim, §2.6). `setData` allocates a
  `Reference2ObjectOpenHashMap` on the first call **even when the value is `null`**. An
  unconditional write therefore allocates one map per rendered living entity per frame, and
  Fabric API never calls `clearExtraData()` on `EntityRenderState` instances. A `@Unique`
  reference field costs one store and zero allocation.
* Modifiers, not eliminators: if state reuse *were* proven impossible, a *conditional*
  `FabricRenderState` write would allocate only for varianted entities. That is the variant this
  plan would switch to, and it is a **transport-only** change — the substitution mixin and the
  read site are unaffected. The switch condition is stated in §12.4.

`FabricRenderState` therefore remains documented as the **fallback transport** (§13), not as
dead design work: it is the only officially supported extension point and is the correct choice
if the project ever adds a policy of "no Mixin on Mojang classes".

### 6.3 Why the substitution point is `getRenderType`, not `getTextureLocation`

* `getTextureLocation` is **abstract** (§2.1). There is no shared body, and every family
  declares its own (§2.3) — this is Strategy C, refuted above.
* `getRenderType` is the **first shared, non-abstract code on the texture path**
  (§2.4 steps 5→6→7). It is inherited by `MobRenderer` and therefore by every mob renderer that
  does not override it, which is the overwhelming majority.
* Substitution happens on the **`Identifier`, before** `Model#renderType(Identifier)` converts
  it to a `RenderType` (§2.4). `RenderType` is never touched, so every vanilla concern carried
  by `getRenderType` — invisibility, translucency, emissive, outline — is preserved
  automatically. The `Redirect` changes only the argument the vanilla body feeds in.
* Non-variant entities are unaffected by construction: the redirect returns
  `original.call(state)` unless the state field is non-null.

### 6.4 Preserved architecture

Attachments remain the sole source of variant state; persistence, synchronisation, server
authority, one-time selection, datapack definitions, and the precomputed-texture decision (R-007)
are untouched. Nothing in this plan reads or writes the attachment on the server, and nothing
introduces per-tick work. All new code is client-only.

---

## 7. Exact Integration Points

Nothing below is created in this phase. Signatures are given as verified in §2; where a byte-code
target could not be verified it is explicitly marked.

### 7.1 Transport (Strategy B)

* **Target class:** `net.minecraft.client.renderer.entity.state.LivingEntityRenderState`
  (§2.2, verified in Fabric API source, javadoc, and the local Loom tiny).
* **Injection type:** `Mixin` class adding one `@Unique @Nullable net.minecraft.resources.Identifier`
  field, plus an accessor interface implemented by the state
  (`implements MobVariantsRenderState { Identifier mobvariants$variantTexture(); void …(Identifier); }`).
  Plain Mixin only; no MixinExtras.
* **Lifecycle phase:** none (data carrier).
* **Data transported:** one `Identifier` — the resolved variant texture, or `null`.
* **Allocation:** zero.
* **Why this class and not `EntityRenderState`:** narrower blast radius; every renderer we touch
  extends `LivingEntityRenderer` whose state parameter is bounded by `LivingEntityRenderState`.

### 7.2 Write point

* **Target class:** `net.minecraft.client.renderer.entity.LivingEntityRenderer`
* **Target method:**
  `extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V`
  — **descriptor VERIFIED** (Fabric API's own `@WrapOperation` target, §2.1).
* **Injection type:** `@Inject(method = "extractRenderState(...)", at = @At("HEAD"))`.
  `cancellable` is **not** used: vanilla extraction must run unchanged.
* **Lifecycle phase:** render-state extraction, per entity per frame. At this instant both the
  entity (attachment source) and the state (transport target) are in scope — the only point in
  the lifecycle where both exist (§4 steps 2–3).
* **Attachment read:** `entity.getAttached(VariantAttachments.VARIANT)`.
* **Texture resolution:** only when the attachment is non-null — one lookup in the immutable
  variant registry, returning a texture `Identifier` that was resolved once at resource reload
  (R-007). No string concatenation, no `Identifier` construction, no parsing on this path.
* **Write:** always, including `null`.
* **Vanilla preservation:** the injector returns `void` and touches nothing but the added field.

### 7.3 Read / substitution point

* **Target class:** `net.minecraft.client.renderer.entity.LivingEntityRenderer`
* **Target method:** `getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;`
  (`protected @Nullable`, §2.1).
* **Injection type:** `@Redirect` with `@At(value = "INVOKE", target = …getTextureLocation…)`,
  returning the variant `Identifier` when the state field is non-null, else
  `cir.call(state)`.
* **Exact `target` string: UNVERIFIED.** The method body was not readable in this environment
  (§2.0). It must be read from decompiled 26.1.2 sources before the mixin is written
  (§12.1). The candidate form is
  `Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getTextureLocation(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Lnet/minecraft/resources/Identifier;`
  if the call is on the base class; if the body reaches the texture through
  `this.model.renderType(this.getTextureLocation(state))`, the redirect target stays the
  `getTextureLocation` call and the surrounding call is left alone.
* **Lifecycle phase:** submission, after extraction, before the `Identifier → RenderType`
  conversion.
* **Texture consumed:** inside the `RenderType` produced from the returned `Identifier`, then in
  `SubmitNodeCollector#submitModel(…, RenderType, …)`.
* **Vanilla preservation:** the redirect replaces only the returned `Identifier`. Every vanilla
  decision derived from it — `@Nullable` body suppression, translucency, emissive, outline — is
  still computed by the unmodified vanilla body.
* **Non-variant entities:** the state's variant field is `null` (written by §7.2) and the
  redirect returns `original.call(state)` unchanged.
* **Generic across `LivingEntityRenderer`:** yes for all renderers that inherit
  `LivingEntityRenderer#getRenderType` — that is `MobRenderer` and its entire subtree, verified,
  including the whole zombie family. Known gap: renderers that override `getRenderType`
  (`ArmorStandRenderer`, verified). Complete override set: UNVERIFIED (§13).
* **Fallback rule if the redirect target is absent:** do **not** re-implement `getRenderType` in
  an `@Inject` — that would forfeit the vanilla flag logic. Instead apply §7.3′.
* **§7.3′ documented alternative:** redirect the same call inside
  `LivingEntityRenderer#submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V`
  — verified to exist (§2.1) but **overridden by `AgeableMobRenderer`**, so coverage is narrower
  than §7.3. Select §7.3′ only if §7.3 is structurally impossible.

### 7.4 Client wiring

* `MobVariantsBRSClient#onInitializeClient` — unchanged. No Fabric rendering event is used, so
  no registration is required.
* `mob_variants_brs.client.mixins.json` — `"client"` array gains the new entries; the existing
  `"client": ["ExampleClientMixin"]` entry is **not** removed in this phase.
* `mob_variants_brs.mixins.json` (common) — untouched; the new mixins are client-only, which is
  what guarantees dedicated-server safety.

---

## 8. Performance Analysis

Scope: cost **per rendered living entity, per frame**. Nothing here is per-tick.

| Operation | Frequency | Cost | Notes |
|---|---|---|---|
| `entity.getAttached(VARIANT)` | 1 per rendered living entity per frame | One interface/virtual call + one map lookup inside Fabric's attachment impl | **Attachment-impl body UNVERIFIED** (not readable here). Treat as O(1); it is the same lookup the server-side selection already performs once. |
| Variant definition lookup | 1 per varianted entity per frame, **0** otherwise | One `Map.get` on an immutable registry | Registry is rebuilt on resource reload, never per frame. |
| Texture identifier resolution | **0 per frame** | — | The `Identifier` is resolved once at reload and carried in the attachment value (R-007 preserved). No `Identifier` construction, no string building, no namespace/path parsing on the render path. |
| Write to render state | 1 per rendered living entity per frame | 1 reference store (B) or 1 `put` + possibly 1 map allocation (A) | This is the only point where the two transports differ. |
| Read in `getRenderType` | 1 per rendered living entity per frame | 1 null check | |
| `getTextureLocation` | 1 per rendered living entity per frame | Unchanged from vanilla | |
| `Model#renderType(Identifier)` / `RenderType` construction | 1 per rendered living entity per frame | **Vanilla cost, unchanged** | We only change which `Identifier` is fed in; vanilla already builds a `RenderType` per call. |
| `submitModel` | 1 per rendered living entity per frame | Unchanged | |

Load sizing. Mob counts per frame are bounded by the render distance, frustum culling, and
`EntityRenderDispatcher#shouldRender`; variant count is bounded by the datapack and affects only
map size, not lookup cost. The only input that scales the new cost is *rendered living entity
count*.

**Quantified A-vs-B difference.** The only allocation A introduces is one
`Reference2ObjectOpenHashMap` per rendered living entity per frame, because the write is
unconditional (§6.2). For N rendered living entities at F frames per second the additional
allocation rate is `N × F` maps/second. With a fastutil open-hash map at default load factor the
per-instance footprint is on the order of a few hundred bytes; at N = 100 and F = 60 that is
6 000 maps/second, i.e. order-of-magnitude megabytes per second of young-gen garbage. This is
the concrete basis for selecting B on the *Performance* criterion. It is a projection from the
verified `RenderStateMixin` source, not a measured figure — §12.3 makes measurement a
deliverable.

**No speculative micro-optimisations are proposed.** In particular: no per-variant caches, no
thread-local lookaside, no pre-warmed `RenderType` objects (they are vanilla-owned), and no
attachment-lookup elision — every one of those is unmeasurable without the §12.3 profile, and
the two decisions that actually matter (zero-allocation transport, zero per-frame identifier
resolution) are already taken.

---

## 9. Compatibility Analysis

1. **Dedicated server.** All new classes belong under `src/client/java/.../client/` and are
   registered only in the `"client"` array of `mob_variants_brs.client.mixins.json`. A dedicated
   server loads neither the client source set nor the client mixin config. **Safe by
   construction**, provided that constraint is honoured in the implementation phase.
2. **Integrated server.** Client and server run in one JVM. The rendering path touches only
   client-side renderer classes and the client-side attachment value, which the existing sync
   design already maintains. No cross-thread access: `extractRenderState` and `submit` both run
   on the render thread.
3. **Client/server separation.** The attachment read is a *read*. No write path, no
   re-synchronisation, and no variant re-selection is introduced, so server authority is
   structurally preserved.
4. **Multiplayer / join / rejoin.** The texture is derived from the already-synchronised
   attachment at render time; nothing is cached across sessions on the client except the
   datapack-derived texture map, which is rebuilt on every resource reload.
5. **Entity lifecycle.** Render-state field lifetime is one entity per frame (§2.5), and the
   write is unconditional, so a despawning or re-spawning entity cannot leak a stale texture
   into another entity.
6. **Renderer reuse.** Renderer instances are long-lived but hold no per-entity data in this
   design. The variant lives on the render state, which is per entity. **No renderer state is
   introduced**, so renderer reuse is a non-issue.
7. **Other vanilla mob renderers.** §7.3 covers `MobRenderer` and its whole subtree that does
   not override `getRenderType`. Renderers that override it (`ArmorStandRenderer`, verified) are
   out of coverage by design; the practical consequence is that a mob variant defined for such
   a type renders with the vanilla texture. No crash, no wrong texture on a *different* mob.
8. **Other rendering mods.** No compatibility claim is made. Both A and B occupy the same single
   call site inside `getRenderType`, so any mod that also redirects or injects there (entity
   texture replacement, emissive-layer, or custom-skin mods) creates an ordinary Mixin-priority
   conflict. The design does nothing to reduce that surface, and no evidence was available to
   assert interoperability with any specific mod.
9. **Future Minecraft changes.** 26.1+ ships unobfuscated (§2.0), so there is no
   obfuscation-remapping risk. That is **not** an API-stability guarantee: `extractRenderState`,
   `getRenderType` and `LivingEntityRenderState` are ordinary internal classes and can be renamed
   or reshaped. Mitigation is D-017 (per-injector `require = 0` plus a single one-time log)
   — note the project configs use `"defaultRequire": 1`, so `require = 0` must be set
   explicitly on each vanilla-targeting injector.
10. **Fabric API compatibility.** The design consumes no Fabric API at all once Strategy B is
    chosen, so a Fabric API upgrade cannot break it. `EntityRendererRegistry` is
    `@Deprecated` in the installed version and is not used. `FabricRenderState` is retained only
    as the documented fallback transport (§6.2, §13).
11. **MixinExtras.** Not required (§2.7). Only plain-Mixin injectors are specified, so no new
    dependency is introduced.

---

## 10. Risks

| ID | Risk | Severity basis | Mitigation / status |
|---|---|---|---|
| RK-1 | **The `@Redirect` target inside `getRenderType` is unverified.** The method body was unreadable in this environment. | The entire substitution half rests on it. | Blocking task §12.1: read the decompiled body first. Two documented fallbacks (§7.3′), then a bounded per-renderer list as last resort. |
| RK-2 | Renderers that override `getRenderType` are not covered (`ArmorStandRenderer` verified). | Silent no-op: vanilla texture. | Accepted for the initial scope (mob variants). Coverage list to be produced by a test matrix in §12.3. |
| RK-3 | A concrete renderer that does not call `super.extractRenderState(...)` would never receive the variant. | Silent no-op. | Detectable by the same test matrix. Not observed in the verified zombie chain. |
| RK-4 | Render states are assumed **not** to be reused. If they are, the field is still correct because the write is unconditional. | The design is deliberately robust to this. | Mitigated by construction. |
| RK-5 | Mixin conflict at the `getRenderType` call site with texture-replacement mods. | Ordinary Mixin priority conflict; no evidence gathered about any specific mod. | `require = 0` + one-time warn; documented, not solved. |
| RK-6 | `extractRenderState` / `getRenderType` renamed or re-signatured in a future MC release. | Breakage. | Name-based mixins are the only mechanism available; D-017. |
| RK-7 | The vanilla body of `getRenderType` may call `getTextureLocation` **more than once**; a `@Redirect` requires an `ordinal` or a single unambiguous target. | Redirect failure at runtime (mixin apply error). | Determine the call count in §12.1 and set `ordinal`/`require` accordingly. |
| RK-8 | Variant texture missing on the client (e.g. a server-only datapack) renders the missing-texture checkerboard rather than failing. | Visual only. | Pre-existing data-pack concern; not changed by this design. |
| RK-9 | NeoForge javadoc is a patched 26.1.2 build. A `BaseRenderState` superclass exists there and **not** on Fabric. | The `EntityRenderState` method/field set may differ slightly between the javadoc and the Fabric runtime. | Only §2.1/§2.2 members corroborated by Fabric API source or the local Loom tiny are relied upon; §12.1 re-checks the real body. |
| RK-10 | Two client Mixins now target Mojang classes (one method, one class) instead of one. | More breakage points than the previous plan's single mixin. | Accepted: the extra target is a *class* field addition, not a behavioural hook, and it is what removes the per-frame allocation (§6.2). |

---

## 11. Required Architecture Changes

Scoped strictly to the rendering integration. All other decisions in
`1790903055742-mob-variants-brs-architecture.md` stand.

| # | Location | From | To |
|---|---|---|---|
| 1 | Plan §6.2 | "exactly one client Mixin" targeting `LivingEntityRenderer#getTextureLocation(LivingEntity)` | **Two-phase client pipeline**: one `LivingEntityRenderState` field-mixin (transport) + one `LivingEntityRenderer` mixin carrying the write inject and the read redirect. |
| 2 | Plan §6.2 | "read the entity attachment from `getTextureLocation`" | "read the attachment in the `extractRenderState` head injection; substitute the `Identifier` on the shared `getRenderType` path" |
| 3 | Plan §7 file tree | `client/mixin/MobRendererMixin.java` | `client/render/` package: the state mixin + accessor interface, the renderer mixin, and (if §12.1 requires it) the `submit`-based alternative. `render/` is created now — the prior "no `render/` directory" note is superseded. |
| 4 | Decision D-015 | "exactly one client Mixin" | "two client Mixins (one data carrier, one behavioural)" |
| 5 | Decision D-016 | "exact 26.1.2 signature UNVERIFIED" | **Resolved**: `getTextureLocation(S)` is abstract; the shared interception point is `getRenderType(S, boolean, boolean, boolean)`. The remaining unverified item is the in-body call target, not the method. |
| 6 | Decision D-017 | failure mode | Unchanged (per-injector `require = 0` + one-time log), now applied to two injectors. |
| 7 | Plan §5.2 open item | "The exact 26.1.2 vanilla signature of the renderer texture lookup (§6)" | **Resolved** (§2.1, §2.3). |
| 8 | Plan §10 risk on render coverage | single-mixin coverage | Replaced by RK-1…RK-10 (§10). |
| 9 | Plan §11 step 10 | "Client: loader registration, then `MobRendererMixin`" | "…then the state mixin and the renderer mixin". |
| 10 | `docs/DECISIONS.md` D-006/D-007/D-010, Plan §9 | Fabric API based architecture | Unchanged — this design consumes **no** Fabric rendering API. |

Explicitly **not** changed: attachment design and persistence, synchronisation, server
authority, one-time selection, variant definition format, datapack layout, precomputed texture
`Identifier` (R-007), Gradle, dependencies, mixin *configs'* existing entries, the inert
`ExampleMixin`/`ExampleClientMixin` entries, and all documentation prose outside the rows above.

---

## 12. Implementation Boundary

### 12.1 Must be done first (blocking)

Read the decompiled 26.1.2 bodies of `LivingEntityRenderer#getRenderType`,
`LivingEntityRenderer#submit` and `EntityRenderer#createRenderState(T, float)` — the project has
Loom, so `./gradlew genSources` yields them locally. Establish, as facts:

1. Does `getRenderType`'s base body call `getTextureLocation(state)`? On `this` or via
   `this.model`?
2. How many times, and is the call site unambiguous (no `ordinal` needed)?
3. Does it call `this.model.renderType(identifier)`, or `RenderTypes.*(identifier)` directly?
4. Does `createRenderState(T, float)` call `extractRenderState`, or does the dispatcher?
5. Does `submit` call `getRenderType`, or does `getRenderType` happen elsewhere?

Only then write the redirect. This is read-only inspection; it is not implementation.

### 12.2 What the next (Code) phase implements

* `src/client/java/.../client/render/MobVariantsRenderState.java` — accessor interface
  (getter + setter for one `Identifier`).
* `src/client/java/.../client/mixin/LivingEntityRenderStateMixin.java` — `@Unique @Nullable
  Identifier` field + `implements MobVariantsRenderState`.
* `src/client/java/.../client/mixin/LivingEntityRendererMixin.java` — the `@Inject` at
  `extractRenderState` HEAD and the `@Redirect` in `getRenderType`.
* The two new entries in `mob_variants_brs.client.mixins.json`.
* The variant-texture lookup that maps a variant id to its precomputed texture `Identifier`,
  read from the existing (not-yet-implemented) definition registry.

### 12.3 Validation that must accompany it

* `./gradlew build` compiles — this is the primary check that the descriptors in §2.1 are right.
* Runtime smoke test: one varianted mob and one plain mob of the **same** type visible together
  in the same frame, proving the per-entity write does not leak.
* Invisibility, translucency, glow/outline and name-plate behaviour unchanged for a varianted
  mob (proves the `RenderType` chain was not bypassed).
* A **coverage matrix** over at least: `Zombie` (family chain), one non-zombie `MobRenderer`
  descendant, and `ArmorStand` (a verified `getRenderType` override) to confirm RK-2's predicted
  no-op rather than a crash.
* An allocation/GC observation with and without the feature, over a mob-dense scene, to confirm
  the §8 projection and to supply the switch condition in §12.4.
* Dedicated-server start with the mod installed (no client classes loaded).

### 12.4 What must remain unchanged

Attachments, persistence, synchronisation, server authority, selection timing, definition
format, datapack layout, precomputed texture `Identifier`, Gradle, dependency list, the
`"client"` vs `"mixins"` split of the two mixin configs, and the inert `Example*Mixin` entries.

### 12.5 What must not be implemented yet

* No new `EntityType`; no custom renderer; no render-layer registration.
* No `FabricRenderState` usage (kept only as the documented fallback in §13).
* No MixinExtras; no new dependency; no Gradle change; no documentation edit.
* No per-`EntityType` renderer wrapping (R-001 stands).
* No handling for `getRenderType`-overriding renderers beyond documenting the gap.

### 12.6 Transport switch condition (pre-agreed, mechanical)

If §12.3's allocation measurement shows `LivingEntityRenderState` instances **are** reused
across entities, or if a future project policy forbids Mixins on Mojang classes, replace the
field mixin with `FabricRenderState#setData(RenderStateDataKey<Identifier>, …)` and restore the
conditional write. The write mixin and the read redirect are unchanged by that switch.

---

## 13. Verification Status / Unverified Items

### 13.1 Verified facts relied upon

| Fact | Source |
|---|---|
| `LivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>`, with `getTextureLocation(S)` **abstract**, `getRenderType(S, boolean, boolean, boolean)` `@Nullable`, `submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)`, `protected M model` | 26.1.2 javadoc §2.1 |
| `extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V` | Fabric API 26.1 `LivingEntityRendererMixin` |
| `MobRenderer` implements neither `getTextureLocation` nor `getRenderType`; `ZombieRenderer` implements only `createRenderState`; `AbstractZombieRenderer` implements `getTextureLocation` + `extractRenderState`; `AgeableMobRenderer` implements `submit`; `ArmorStandRenderer` implements `getTextureLocation` + `getRenderType` | 26.1.2 javadoc §2.3 |
| `EntityRenderer#createRenderState()` abstract, `createRenderState(T, float)` final, `extractRenderState(T, S, float)`, `finalizeRenderState(T, S)` | 26.1.2 javadoc §2.2 |
| `Model#renderType(Identifier)` is `final`; `EntityModel` has a `Function<Identifier, RenderType>` constructor | 26.1.2 javadoc §2.4 |
| `OrderedSubmitNodeCollector#submitModel(…, RenderType, …)` and an `Identifier` overload | 26.1.2 javadoc §2.4 |
| `EntityRenderDispatcher#extractEntity(E, float)`, `submit(S, CameraRenderState, …)`, no pooled-state field | 26.1.2 javadoc §2.5 |
| `FabricRenderState` / `RenderStateDataKey` API for `0.155.3+26.1.2` | exact-version javadoc §2.6 |
| `RenderStateMixin` storage: lazy `Reference2ObjectOpenHashMap`, allocation on first `setData` **including `null`**, `clear()` retains the field, `EntityRenderState` in the `@Mixin` list | Fabric API 26.1 source §2.6 |
| `fabric-rendering-v1` 26.1 has **no** `EntityRenderStateMixin`; `LevelRenderState#reset` receives `clearExtraData()` | Fabric API 26.1 source §2.6 |
| `EntityRendererRegistry` is `@Deprecated`; `FabricModel` exposes no texture hook; `LivingEntityRenderLayerRegistrationCallback` only adds layers | exact-version javadoc + source §2.6 |
| 26.1+ ships unobfuscated | `mappings.dev` index page |
| Repository is an unmodified Fabric template; `git status` clean | local inspection |

### 13.2 Unverified items (must not be treated as facts)

| # | Unverified item | How to resolve |
|---|---|---|
| U-1 | The body of `LivingEntityRenderer#getRenderType` — whether and how it calls `getTextureLocation`, and the call count. | §12.1 |
| U-2 | The exact `@At(target = …)` string for the redirect. | §12.1 |
| U-3 | The body of `LivingEntityRenderer#submit` and whether `AgeableMobRenderer`'s override re-enters the base path. | §12.1 |
| U-4 | Whether `EntityRenderer#createRenderState(T, float)` invokes `extractRenderState`, or the dispatcher does. | §12.1 |
| U-5 | Whether `EntityRenderState` instances are ever reused across entities/frames (strong evidence against, §2.5). | §12.3 measurement; note the design is already robust either way |
| U-6 | The complete set of renderers overriding `getRenderType` (only `ArmorStandRenderer` is confirmed). | §12.3 coverage matrix |
| U-7 | Whether every concrete renderer calls `super.extractRenderState(...)`. | §12.3 coverage matrix |
| U-8 | `FabricRenderState` allocation cost in practice (the `RenderStateMixin` source makes the mechanism certain; the numeric footprint is a projection). | §12.3 measurement |
| U-9 | MixinExtras compile-time availability (runtime presence inferred from Fabric API's own use). | Avoided by design (§2.7) |
| U-10 | The cost of `Entity#getAttached` in the installed Fabric API. | Profile; treat as O(1) |
| U-11 | Whether `RenderTypes.*(Identifier)` caches per-`Identifier` (affects nothing functionally; would only mean the variant texture is cached too). | Profile if ever relevant |
| U-12 | `ResourceLocation`→`Identifier` rename status: 26.1.2 uses `net.minecraft.resources.Identifier`; a `ResourceLocation` alias may also exist. | Verified name is `Identifier`; the alias's presence is not checked |
| U-13 | Method bodies of any Mojang class in this document (javadoc exposes signatures only). | §12.1 |
| U-14 | NeoForge javadoc ↔ Fabric runtime differences (RK-9). | §12.1 |

### 13.3 Environment limitation that shaped this investigation

The installed Minecraft 26.1.2 jars could not be introspected. `javap`, decompilers (`cfr`,
`vineflower`, `stitch` are all present in the Gradle cache) and archive-extraction tools are not
permitted by the active command rules, and `ripgrep` is not installed; jar entry *contents* are
DEFLATE-compressed regardless. `mcsrc.dev` was checked and is a client-side SPA that serves no
HTML to a fetch. This is why every body-level question is marked UNVERIFIED rather than
guessed, and why §12.1 is a blocking first step rather than a nicety.
