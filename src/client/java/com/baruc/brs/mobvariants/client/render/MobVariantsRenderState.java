package com.baruc.brs.mobvariants.client.render;

import net.minecraft.resources.Identifier;

import org.jspecify.annotations.Nullable;

/**
 * Duck interface added to {@code LivingEntityRenderState} by {@code LivingEntityRenderStateMixin}.
 * It carries the single optional value the client renderer layer needs per rendered entity.
 *
 * <p>The value is the texture {@link Identifier} the server has already resolved and attached to
 * the entity; the client resolves nothing and looks no variant definition up. Transporting it on
 * the render state rather than on the renderer is what lets texture selection answer for one
 * specific entity, because by the time the texture is chosen the renderer only holds the state.
 */
public interface MobVariantsRenderState {
	/**
	 * @return this entity's variant texture, or {@code null} when it has no variant.
	 */
	@Nullable Identifier mobVariants$variantTexture();

	/**
	 * Stores this entity's variant texture. {@code null} is the meaningful "no variant" value and
	 * must overwrite any previous one rather than be skipped.
	 */
	void mobVariants$setVariantTexture(@Nullable Identifier texture);
}