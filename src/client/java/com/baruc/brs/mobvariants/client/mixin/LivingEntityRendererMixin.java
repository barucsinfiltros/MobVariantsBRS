package com.baruc.brs.mobvariants.client.mixin;

import com.baruc.brs.mobvariants.attachment.VariantAttachments;
import com.baruc.brs.mobvariants.client.render.MobVariantsRenderState;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries the server-resolved variant texture from the entity onto its render state, then
 * substitutes it for the renderer's own texture at the one place the body texture is chosen.
 *
 * <p>Both halves run only while a render state is extracted or submitted, so there is no per-tick
 * work, no client-side variant registry and no definition lookup. The value read here is the
 * already-resolved {@link Identifier} the server attached to the entity, which makes an absent
 * attachment — the single no-variant case — fall through to vanilla untouched.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	/**
	 * Declared so the fallback branch can reach the renderer's own texture, which is abstract here
	 * and implemented per renderer family.
	 */
	@Shadow
	public abstract Identifier getTextureLocation(LivingEntityRenderState state);

	/**
	 * Written unconditionally, including {@code null}: {@code extractRenderState} is the only point
	 * in the render cycle where both the entity and its render state are in scope, and clearing on
	 * every pass makes a stale texture impossible if render states are ever reused.
	 */
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("HEAD"))
	private void mobVariants$extractRenderState(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		((MobVariantsRenderState) state).mobVariants$setVariantTexture(
				entity.getAttached(VariantAttachments.VARIANT_TEXTURE));
	}

	/**
	 * Substitutes only the {@link Identifier}. {@code getRenderType}'s own body is untouched, so
	 * translucency, the emissive outline and the invisible-body {@code null} return stay exactly as
	 * vanilla decides them.
	 *
	 * <p>{@code renderer} is the instance the replaced {@code this.getTextureLocation(state)} call
	 * was made on and is required by the redirect signature; the body invokes {@code this}, which is
	 * that same instance.
	 */
	@Redirect(
			method = "getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getTextureLocation(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Lnet/minecraft/resources/Identifier;"))
	private Identifier mobVariants$getTextureLocation(LivingEntityRenderer<?, ?, ?> renderer, LivingEntityRenderState state) {
		Identifier variantTexture = ((MobVariantsRenderState) state).mobVariants$variantTexture();

		return variantTexture != null ? variantTexture : this.getTextureLocation(state);
	}
}