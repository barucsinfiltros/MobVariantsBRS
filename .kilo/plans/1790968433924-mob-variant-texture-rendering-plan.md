# MobVariantsBRS — Variant Texture Rendering Architecture

**Status:** architecture only. This document contains **no implementation**; no Java file is
created or modified by the task that produced it.

**Replaces** the previous contents of this file. The prior revision's `@Redirect` sketch
(`CallbackInfoReturnable<Identifier>` handler and `cir.call(state)`) was wrong: `@Redirect`
handlers take neither, and `cir.call(...)` belongs to MixinExtras `@WrapOperation`, which this
project does not declare. That error is corrected in §4.

**Verified stack** (`gradle.properties`, `build.gradle`): MC `26.1.2`, Fabric Loader `0.19.5`,
Fabric API `0.155.3+26.1.2`, Java `25`, Loom `1.18-SNAPSHOT`, official Mojang mappings.

---

## 1. Scope

**In scope** — how a server-selected, precomputed variant texture reaches the body texture of a
vanilla living-entity renderer, using two client-only Mixins and one field on the vanilla render
state.

**Out of scope** — the attachment/definition/snapshot/selection layers (owned by
`1790996850927-variant-state-layer-architecture.md`, not yet implemented), Gradle, dependencies,
`EntityType`s, mob classes, custom renderers, render layers, `FabricRenderState` transport,
networking, persistence, attribute application.

**Dependency on other phase — the contract, as finalized.** The state-layer design is
`1790996850927-variant-state-layer-architecture.md`; its §15.1 is the authoritative statement of
what the render layer may consume. That contract supersedes the earlier
"attachment → registry → texture" sketch this plan previously carried:

```
Identifier texture = entity.getAttached(VariantAttachments.VARIANT_TEXTURE);   // AttachmentType<Identifier>
```

The attachment holds the **already-resolved texture `Identifier`** the server chose for that entity.
Nothing else from the state layer is used:

- there is **no** client-side `VariantRegistry`, no definition lookup, and no id → texture step;
- `null` (no attachment) is the only no-variant case;
- the resolved value is self-contained, so a removed or changed definition cannot strand a mob.

If that attachment read does not exist yet, this phase must wait for it; it does not re-implement it.

---

## 2. Verified interception point (Minecraft 26.1.2)

`LivingEntityRenderer#getRenderType` (verified body, task brief / generated 26.1.2 sources):

```java
protected @Nullable RenderType getRenderType(
        final S state,
        final boolean isBodyVisible,
        final boolean forceTransparent,
        final boolean appearGlowing) {

    Identifier texture = this.getTextureLocation(state);

    if (forceTransparent) {
        return RenderTypes.entityTranslucentCullItemTarget(texture);
    } else if (isBodyVisible) {
        return this.model.renderType(texture);
    } else {
        return appearGlowing ? RenderTypes.outline(texture) : null;
    }
}
```

The single interception target, exactly as specified:

| Property | Value |
|---|---|
| Owner | `net.minecraft.client.renderer.entity.LivingEntityRenderer` |
| Method | `getTextureLocation` |
| Descriptor | `(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Lnet/minecraft/resources/Identifier;` |
| Opcode | `INVOKEVIRTUAL` (call is `this.getTextureLocation(state)`) |
| Occurrences in `getRenderType` | 1, unambiguous → **no `ordinal`** |
| Method selector | `getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;` |

**Why this is the right point.** The `Identifier` is produced once and then consumed by
`RenderTypes.*(texture)` / `this.model.renderType(texture)`. Substituting the `Identifier` — never
the `RenderType` — leaves every vanilla decision (null body → invisible, translucency, emissive
outline) computed by the unmodified vanilla body. `getTextureLocation` itself is `abstract` on
`LivingEntityRenderer` and concretely overridden per renderer family, so it has no shared body to
intercept; `getRenderType` is the first shared, non-abstract code on the texture path.

**Why no ordinal.** `BeforeInvoke.find(...)` only filters by ordinal when `At#ordinal` is set;
the default `-1` matches every occurrence, and there is exactly one occurrence here.

---

## 3. Verified dependency and API names

Confirmed against Fabric API source branch `26.1.2` (the exact version installed here,
`0.155.3+26.1.2`) and the local Gradle cache:

| Name | Evidence |
|---|---|
| `net.minecraft.client.renderer.entity.LivingEntityRenderer` | Fabric API `LivingEntityRendererMixin` `@Mixin` target |
| `net.minecraft.client.renderer.entity.state.LivingEntityRenderState` | Fabric API `LivingEntityRendererMixin` target descriptor; `CapeLayerMixin` imports `…entity.state.AvatarRenderState` |
| `extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V` | Fabric API `LivingEntityRendererMixin` `method = "…"` string (verbatim) |
| `net.minecraft.client.renderer.rendertype.RenderType` | Fabric API `ModelMixin` imports |
| `net.minecraft.resources.Identifier` | Fabric API `ModelMixin` imports |
| `net.minecraft.world.entity.LivingEntity` | Fabric API `LivingEntityRendererMixin` imports |
| `Entity#getAttached(AttachmentType<A>)` → `@Nullable A` | Fabric API `fabric-data-attachment-api-v1` `AttachmentTarget` |
| Mixin = `net.fabricmc:sponge-mixin:0.17.4+mixin.0.8.7` (FabricMC fork of Mixin 0.8.7) | `~/.gradle/caches/modules-2/files-2.1/net.fabricmc/sponge-mixin/0.17.4+mixin.0.8.7/` |
| `org.spongepowered.asm.mixin.*` is on the compile classpath | `build.gradle` → `fabric-loader`; already used by `src/client/java/…/client/mixin/ExampleClientMixin.java` |

Fabric API's own `fabric-rendering-v1.mixins.json` (branch `26.1.2`) contains **no**
`EntityRenderStateMixin` and no Mixin that touches `LivingEntityRenderer#getRenderType`, so the
interception point is currently unoccupied by Fabric API. `EntityRenderDispatcherMixin` only
injects into `onResourceManagerReload` and does not interfere with extraction.

**MixinExtras is not used.** Fabric API ships `com.llamalad7.mixinextras` code, so it is present
at runtime, but this project declares no dependency on it (`build.gradle` lists only
`minecraft`, `fabric-loader`, `fabric-api`). Adopting `@WrapOperation` would require a new
dependency, which is forbidden. Everything below is plain Mixin.

---

## 4. Verified interception mechanism and handler contract

**Mechanism: plain `@Redirect`** (`org.spongepowered.asm.mixin.injection.Redirect`) with
`@At(value = "INVOKE", target = …)`, plus `@Shadow` to reach the original method. Confirmed by
reading `RedirectInjector` and `Injector` from FabricMC/Mixin branch `fabric-0.8` (the source of
the installed `sponge-mixin 0.17.4+mixin.0.8.7`).

Contract, as implemented by `RedirectedInvokeData` + `Injector#validateParams`:

1. `RedirectedInvokeData` derives from the *redirected* instruction: `returnType =
   Type.getReturnType(node.desc)`, `targetArgs = Type.getArgumentTypes(node.desc)`, and for a
   non-`INVOKESTATIC` call `handlerArgs = [owner, …targetArgs]`.
2. For this target: return type `Identifier`, required handler args
   `[LivingEntityRenderer, LivingEntityRenderState]`.
3. Handler args are matched **positionally**. Extra handler args consume the *enclosing* method's
   arguments; **fewer handler args than required are accepted** (validation only throws when the
   handler has *more* args than can be satisfied). No `@Coerce` is needed — every erased type
   matches exactly.
4. `checkTargetForNode` requires handler staticness to equal the target's: `getRenderType` is an
   instance method, so the handler is a non-static (instance) method.
5. `CallbackInfo`, `CallbackInfoReturnable`, and any `Operation`/`cir.call(...)` are **not part of
   this contract**. They belong to `@Inject`/`@ModifyReturnValue` (Mixin) and `@WrapOperation`
   (MixinExtras) respectively.
6. To call the original method inside the handler, shadow it. `@Shadow` on an abstract target
   method is supported — Mixin's own javadoc uses `@Shadow abstract …` as the canonical form, and
   shipped Fabric API `0.155.3+26.1.2` code does exactly this (`ModelMixin`:
   `@Shadow public abstract ModelPart root();` against `net.minecraft.client.model.Model`).

### 4.1 Exact handler signature

Target class: `net.minecraft.client.renderer.entity.LivingEntityRenderer`.

Handler contract (signature only — the implementation phase supplies the body):

```java
@org.spongepowered.asm.mixin.Shadow
public abstract net.minecraft.resources.Identifier getTextureLocation(
        net.minecraft.client.renderer.entity.state.LivingEntityRenderState state);

@org.spongepowered.asm.mixin.injection.Redirect(
    method = "getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;",
    at = @org.spongepowered.asm.mixin.injection.At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getTextureLocation(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Lnet/minecraft/resources/Identifier;"),
    require = 0)
private net.minecraft.resources.Identifier mobVariants$getTextureLocation(
        net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?, ?> renderer,
        net.minecraft.client.renderer.entity.state.LivingEntityRenderState state);
```

Body contract: read the variant texture from the render state; return it when non-null, otherwise
return the renderer's own texture as `renderer.getTextureLocation(state)` — the handler's first
parameter is the very instance the original `this.getTextureLocation(state)` call would have been
made on. Two branches, no logging, no allocation.

Declaring the mixin class **non-generic** is deliberate: every parameter and shadow parameter then
matches the target's erased descriptor directly, with no generic-signature resolution involved.

### 4.2 Write point contract

```java
@org.spongepowered.asm.mixin.injection.Inject(
    method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
    at = @org.spongepowered.asm.mixin.injection.At("HEAD"),
    require = 0)
private void mobVariants$extractRenderState(
        net.minecraft.world.entity.LivingEntity entity,
        net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
        org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci);
```

`CallbackInfo` **is** correct here: this is an `@Inject` handler, whose args are a prefix of the
target method's args followed by `CallbackInfo`. Body contract: resolve the variant texture from
the entity and store it on the state, **unconditionally, including `null`**.

Verified lifecycle position — `extractRenderState` runs with the entity and its render state both
in scope (only point in the cycle where that is true):

```
EntityRenderDispatcher#extractEntity → EntityRenderer#createRenderState(T,float) → createRenderState()
  → extractRenderState(entity,state,partialTicks) → LivingEntityRenderer#extractRenderState(…)
  → finalizeRenderState(…) → renderer submit → LivingEntityRenderer#getRenderType(…)
  → getTextureLocation(state)   ← intercepted
```

---

## 5. Architecture

Preserved unchanged:

```
Entity attachment (already-resolved texture Identifier)
  → LivingEntityRenderState custom field
  → LivingEntityRenderer#getRenderType
  → variant texture OR the renderer's original texture
```

Client-only, server-authoritative, zero per-tick work, no custom `EntityType`, no mob classes, no
new dependency, no `FabricRenderState` side-channel, no per-frame allocation, no datapack lookup
during rendering, no attachment lookup during rendering.

### 5.1 Design requirements and where each is satisfied

| # | Requirement | Decision |
|---|---|---|
| 1 | Render-state field representation | One `@Unique private @Nullable Identifier mobVariants$variantTexture;` added to `LivingEntityRenderState` by a Mixin, exposed through a duck interface `MobVariantsRenderState` (getter + setter) that the same Mixin `implements`. Not `FabricRenderState`: its `setData` allocates a fastutil map on first write **even for `null`** (Fabric API `renderstate/RenderStateMixin`), which would allocate per rendered living entity per frame, and Fabric never calls `clearExtraData()` for entity render states. |
| 2 | How server-authoritative selection reaches the client state | The already-synced attachment is read **once** at extraction (§5.3). Nothing is written client-side. |
| 3 | Where the `Identifier` is resolved | **On the server, once per spawn**, inside the state layer's `ENTITY_LOAD` callback (`variantId → VariantDefinition → texture`, per `1790996850927` §5.2/§9.2). The client receives the resolved value through the synced attachment. Zero string work, zero `Identifier` construction, and zero registry reads on the render path. |
| 4 | Where the field is assigned | The `@Inject` at `extractRenderState` HEAD, unconditionally, including `null` — so a stale value is impossible even if render-state allocation ever changes. |
| 5 | How the renderer obtains variant-or-original | The `@Redirect` returns the stored variant when non-null, else the shadowed `getTextureLocation(state)`. |
| 6 | Exact bytecode interception point | §2. |
| 7 | Exact Mixin mechanism + handler contract | §4. |
| 8 | Fallback when no variant exists | Field is `null` → redirect returns the renderer's own texture. Byte-for-byte vanilla behaviour. |
| 9 | Entity without a MobVariantsBRS attachment | `getAttached` returns `null` → same fallback as #8. This is the **only** no-variant case: the state layer resolves the texture on the server, so the client never receives an unresolvable value. |
| 10 | Null variant texture values | An absent attachment resolves to `null` and is stored unchanged → vanilla fallback (the outcome D-020 asks for, reached structurally). A texture-less definition cannot reach the client: the state layer rejects it at load (`1790996850927` §6.3, R-011), so no second null path exists. |
| 11 | Client/server separation | All new files under `src/client/java/…/client/` and registered only in the `"client"` array of `mob_variants_brs.client.mixins.json`; the common mixin config is untouched. Dedicated servers never load them. |

### 5.2 Per-frame cost (one rendered living entity)

| Step | Cost |
|---|---|
| `entity.getAttached(VARIANT_TEXTURE)` (extraction only) | **1 attachment lookup — the only lookup on this path** |
| Store to render-state field | 1 reference store |
| Redirect read | 1 null check |
| Anything during `submit` / `getRenderType` beyond that | **nothing** |

The `VariantRegistry.get(id)` row this table used to carry is **deleted**: there is no id, no
registry, and nothing to resolve — the attachment value *is* the texture.

No allocation is introduced: the stored value is either `null` or an already-interned
`Identifier`; the redirect returns either the stored reference or the renderer's own.

---

## 6. Files

Create (client source set, `com.baruc.brs.mobvariants`):

```
src/client/java/com/baruc/brs/mobvariants/client/render/MobVariantsRenderState.java
        duck interface on LivingEntityRenderState: @Nullable Identifier mobVariants$variantTexture();
                                       void mobVariants$setVariantTexture(@Nullable Identifier);

src/client/java/com/baruc/brs/mobvariants/client/mixin/LivingEntityRenderStateMixin.java
        @Mixin(LivingEntityRenderState.class), @Unique @Nullable Identifier field,
        implements MobVariantsRenderState;

src/client/java/com/baruc/brs/mobvariants/client/mixin/LivingEntityRendererMixin.java
        @Shadow getTextureLocation; @Inject extractRenderState HEAD (write);
        @Redirect getRenderType → getTextureLocation (read/substitute);
        the resolve helper from §1 — a single attachment read of the already-resolved
        texture Identifier (no registry, no definition, no second lookup).
```

Modify:

```
src/client/resources/mob_variants_brs.client.mixins.json
        add "LivingEntityRenderStateMixin" and "LivingEntityRendererMixin" to "client".
        Keep "ExampleClientMixin"; keep package, compatibilityLevel, injectors and overwrites as-is.
```

Package choice follows the existing repository convention: mixins live in
`com.baruc.brs.mobvariants.client.mixin` (already the client config's `package`), while the
non-mixin helper lives in `…client.render`. No `src/main` change, no resource change, no
`build.gradle` change.

---

## 7. Implementation task list (for the Code phase)

1. Implement `MobVariantsRenderState` (duck interface) under `client/render/`.
2. Implement `LivingEntityRenderStateMixin` (`@Unique` field + `implements`).
3. Implement `LivingEntityRendererMixin` with exactly the two injectors and the one `@Shadow`
   specified in §4 — no third injection point, no logger, no flags.
4. Register both mixins in `mob_variants_brs.client.mixins.json`.
5. Wire the resolve helper to the state-layer contract in §1 — **one attachment read** that yields
   the already-resolved texture `Identifier`. Do not re-read datapacks, do not look up a variant id,
   do not add a client-side registry; there is nothing left to resolve.
6. Update `docs/DECISIONS.md` / `docs/ARCHITECTURE.md` / `docs/ROADMAP.md` / `docs/TESTING.md`
   rows that state "exactly one client mixin" or "target method UNVERIFIED" (D-015, D-016,
   ROADMAP P-1, ARCHITECTURE §15.3/§15.5).

---

## 8. Behaviour matrix

| Situation | Field | Redirect returns | Result |
|---|---|---|---|
| Entity has a MobVariantsBRS variant with a resolved texture | variant `Identifier` (as received from the attachment) | variant `Identifier` | variant texture; translucency/outline/invisibility unchanged |
| Entity has no MobVariantsBRS attachment | `null` | renderer's own texture | vanilla; no crash; no log |
| Non-living entity / non-living renderer | n/a | n/a | untouched — the Mixins target living renderers only |
| Mixin fails to apply on a future MC version | `null` or absent | renderer's own texture | silent degradation; **no custom diagnostic added** (§10) |

The former "variant whose texture is absent or unresolvable" row is **deleted**: it is unreachable.
The texture is resolved on the server before it is ever attached, and a texture-less definition is
rejected at load, so "no attachment" is the only no-variant path.

`require = 0` is retained on both injectors per D-017, so a rename degrades to vanilla instead of
hard-failing a client launch. Because that failure is silent, §13 requires the client test to
assert that the texture *changed*, not merely that the game launched. No logging, sentinel,
`AtomicBoolean`, startup probe, or extra injection point is introduced.

---

## 9. Known coverage gap: `ArmorStandRenderer`

`ArmorStandRenderer` is the only client renderer that overrides `getRenderType`. Its normal and
non-marker path delegates to `super.getRenderType(...)`, so the `@Redirect` — which lives in
`LivingEntityRenderer#getRenderType`'s own bytecode — still applies. **Marker** armor stands use
their own body-rendering path and never execute the base `getRenderType`, so they keep vanilla
behaviour. This is a benign, documented gap: vanilla behaviour, no crash, no wrong texture on
another entity. No compensating mixin is added.

`AgeableMobRenderer#submit` calls `super`, so it does not bypass the interception.

---

## 10. Open decision gate: vanilla renderer-specific texture selection — MUST be resolved

Several vanilla renderers choose the body texture themselves inside their own `getTextureLocation`
(cats, cows, pigs, chickens, axolotls, pandas, llamas, parrots, mooshrooms, tropical fish, …).
With this mechanism, a MobVariantsBRS variant on such an entity **replaces** that renderer-specific
choice wholesale. **No repository decision establishes a policy** (`docs/DECISIONS.md` has no
entry), so none is chosen here. Options for the owner to pick:

* **A — MobVariantsBRS wins.** Server-authoritative variant overrides the renderer's choice.
  Simplest; breaks the per-entity vanilla variety for those types.
* **B — Exclude those types in v1.** Variant definitions for those `EntityType`s are rejected or
  ignored (field stays `null` → vanilla). Preserves vanilla behaviour everywhere; smaller v1
  surface.
* **C — Override only the default.** Substitute only when the renderer's own choice equals its
  default texture; otherwise keep the renderer's choice. Preserves vanilla variety; needs a
  comparison against a renderer-specific constant per type.

**This decision does not block the mechanism.** It belongs in the **write point** (§5.1 #4): the
policy decides whether a variant `Identifier` is stored at all. The read point (§5.1 #5) stays a
pure substitution either way. Implementation may proceed up to and including the `@Redirect`;
only the gate in the write point is deferred. Note that options B/C require per-`EntityType`
knowledge that does not exist in the repository yet — budget for that.

---

## 11. Client/server separation

All new code is client-only: `src/client/java`, listed only under the client mixin config's
`"client"` array, touching only client renderer classes and the client-side attachment value that
Fabric already syncs. No write path, no re-selection, no networking. A dedicated server loads
neither the client source set nor the client mixin config.

---

## 12. Performance invariants (must survive review)

No per-tick work; **exactly one attachment read per rendered living entity per frame**, at
extraction, and **zero registry reads** (no client registry exists); zero attachment reads and zero
datapack reads during submission; zero per-frame allocation; no per-renderer global state; no
`RenderType` construction beyond vanilla (only the `Identifier` handed to vanilla differs).

---

## 13. Validation (Code phase — not performed here)

1. `./gradlew build` — the compile-time proof that every Mojang descriptor in §2/§3 and the Mixin
   handler signatures in §4 are correct.
2. `./gradlew runClient`: a varianted mob and a plain mob **of the same type** in one frame →
   only the varianted one changes texture (proves the per-entity write does not leak).
3. Same client: translucency, glow/outline, invisibility and name plate unchanged for the
   varianted mob (proves the `RenderType` chain was not bypassed).
4. Negative test: an entity whose variant has no texture → vanilla texture, no crash.
5. Armor stand: normal armor stand renders the variant; marker armor stand renders vanilla (§9).
6. `./gradlew runServer` with the mod installed: starts with no client classes loaded.
7. Coverage matrix over at least one zombie-family renderer and one non-zombie `MobRenderer`
   descendant, confirming that `super.extractRenderState(...)` is reached.

---

## 14. Repository decision updates

| ID | Current | New |
|---|---|---|
| D-011 | Attachment holds the canonical variant `Identifier`; client resolves it to a texture | **Amended** (by `1790996850927` §5): the attachment holds the **already-resolved texture** `Identifier`. This plan consumes it with one read and performs no resolution. |
| D-015 | "Zero server-side mixins; **exactly one** client-side mixin" | Still zero server-side mixins; **two** client mixins — one transport field on `LivingEntityRenderState`, one behavioural mixin on `LivingEntityRenderer` |
| D-016 | "Client mixin target method UNVERIFIED for 26.1.2" | **Resolved**: interception is `INVOKEVIRTUAL LivingEntityRenderer.getTextureLocation(LivingEntityRenderState)Identifier` inside `getRenderType`; the transport write is at `extractRenderState` HEAD |
| D-017 | `require = 0` + one warning, "open choice" | `require = 0` **retained** on both injectors; the "one warning" half is dropped — no diagnostic mechanism was found that is both verified and off the hot path, so none is invented |
| D-020 | Unknown variant id → vanilla | **Re-based**: the outcome is unchanged and now reached structurally. An unknown id never reaches the client (the server resolves it); the only no-variant case is an absent attachment, which resolves to `null` at extraction and stores `null`. |
| R-005 / R-007 | No second attachment; precomputed `Identifier` | R-005 **still holds** — there is still exactly one attachment, and the resolved texture is that attachment's payload rather than a second one. R-007 (precomputed `Identifier`) **honoured**: the value is computed once per spawn on the server, so the render path does no string or `Identifier` work. |

---

## 15. Verification status of this plan

**Verified in this session**

* Mixin dependency `net.fabricmc:sponge-mixin:0.17.4+mixin.0.8.7` (local Gradle cache) and the
  `@Redirect` / handler / `@Shadow` contracts in §4, read from FabricMC/Mixin `fabric-0.8` sources:
  `injection/Redirect.java`, `injection/code/Injector.java`, `injection/invoke/RedirectInjector.java`,
  `injection/points/BeforeInvoke.java`, `Shadow.java`.
* All Mojang FQNs and the `extractRenderState` descriptor in §3, from Fabric API branch `26.1.2`
  (= the installed `0.155.3+26.1.2`): `LivingEntityRendererMixin.java`, `ModelMixin.java`,
  `CapeLayerMixin.java`, `fabric-rendering-v1.mixins.json`.
* `Entity#getAttached(AttachmentType<A>)` exists (`fabric-data-attachment-api-v1`, branch `26.1`).
* Project conventions on disk: both mixin configs, `ExampleClientMixin` (plain `@Inject` +
  `CallbackInfo`), `MobVariantsBRSClient`, build/Gradle files.
* Repository state: working tree clean; `1790968433924-…-plan.md` was last committed in
  `8dcaa46` and had **no** uncommitted modification before this rewrite.

**Not verified here (accepted inputs / open risks)**

* The `getRenderType` body in §2 is taken as given (verified against the generated 26.1.2 sources
  by the task brief). It could not be re-read in this session: the decompiled sources exist at
  `.gradle/loom-cache/…-sources.jar` and `~/.gradle/caches/fabric-loom/decompile/v1.zip`, but
  `unzip`/`tar`/`mkdir` are blocked by the active command rules and `rg`/`grep`/`head` are not
  installed on this machine. Every Mixin-level claim in §4 *was* verified from source.
* Nothing was compiled, launched, or profiled. No build, runtime, or gameplay validation has
  occurred; §13 is the validation owed by the implementation phase.
* Whether every concrete living renderer calls `super.extractRenderState(...)` — verified for the
  zombie chain, covered by §13.7 otherwise.
* The complete set of renderers overriding `getRenderType` — `ArmorStandRenderer` is the only one
  found.
* The vanilla-variant interaction policy (§10) is **not decided**; it is a gate, not a default.

---

## 16. Explicitly not done here

No Java implemented or modified; no mixin config, resource, Gradle, or `docs/` file changed; no
dependency added; no diagnostic infrastructure invented; no unrelated class renamed or refactored;
no build run. The only artefact produced by that task is this plan file.

**Later amendment.** A subsequent documentation-only task rewired §1, §5, §5.1 #3/#9/#10, §5.2, §6,
§7 task 5, §8, §12 and §14 onto the finalized state-layer contract of
`1790996850927-variant-state-layer-architecture.md` (resolved texture in the attachment; no
client-side registry). It changed plan text only. Nothing in §2, §4, §5.1 #1, §9, §10, §13 or §15
was touched, so every verified mechanism, open gate and validation item below stands as written.