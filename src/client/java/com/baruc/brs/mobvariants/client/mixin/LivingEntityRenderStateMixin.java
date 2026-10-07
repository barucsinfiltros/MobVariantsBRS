package com.baruc.brs.mobvariants.client.mixin;

import com.baruc.brs.mobvariants.client.render.MobVariantsRenderState;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Gives {@code LivingEntityRenderState} the one optional texture override the client renderer
 * layer needs, exposed through {@link MobVariantsRenderState}.
 *
 * <p>A dedicated field rather than Fabric's {@code FabricRenderState} extra-data map: that map
 * allocates on its first write and entity render states are never cleared by their owner, so every
 * rendered living entity would pay for the map on every frame even without a variant.
 */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements MobVariantsRenderState {
	@Unique
	@Nullable
	private Identifier mobVariants$variantTexture;

	@Override
	@Nullable
	public Identifier mobVariants$variantTexture() {
		return this.mobVariants$variantTexture;
	}

	@Override
	public void mobVariants$setVariantTexture(@Nullable Identifier texture) {
		this.mobVariants$variantTexture = texture;
	}
}