package com.baruc.brs.mobvariants.variant;

import java.util.List;
import java.util.Map;

import com.baruc.brs.mobvariants.MobVariantsBRS;
import com.baruc.brs.mobvariants.attachment.VariantAttachments;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/**
 * Server wiring for the variant state layer: the {@code SERVER_DATA} definition loader, the
 * server reference the loader publishes through, and the one-shot per-entity selection.
 *
 * <p>Selection is server-authoritative end to end. The client runs none of this, resolves nothing,
 * and receives the already-resolved texture through Fabric's attachment synchronisation.
 */
public final class VariantStateLifecycle {
	private VariantStateLifecycle() {
	}

	public static void register() {
		VariantDefinitionReloadListener loader = VariantDefinitionReloadListener.getInstance();

		ResourceLoader.get(PackType.SERVER_DATA)
				.registerReloadListener(MobVariantsBRS.id("variants"), loader);

		ServerLifecycleEvents.SERVER_STARTING.register(loader::attach);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> loader.detach());

		ServerEntityEvents.ENTITY_LOAD.register(VariantStateLifecycle::selectVariant);
	}

	/**
	 * Resolves one entity's texture, once. Guard 1 avoids work for entities whose value was just
	 * restored from NBT; guard 2 is the load-bearing one, because the attachment is persistent and
	 * therefore already present on every disk-restored, reloaded, teleported or respawned entity.
	 * Together they make selection idempotent without depending on {@code isLoadedFromDisk()}.
	 *
	 * <p>Applicability is keyed by {@code EntityType<?>}, so no {@code instanceof} filter is
	 * needed: players, items and any other entity simply miss the map.
	 *
	 * <p>Biome conditions are evaluated lazily: if no candidate requires conditions, no biome
	 * lookup is performed. If at least one candidate has conditions, a single biome lookup is
	 * performed and reused for all candidate evaluations.
	 */
	private static void selectVariant(Entity entity, ServerLevel level) {
		if (entity.isLoadedFromDisk()) {
			return;
		}

		if (entity.hasAttached(VariantAttachments.VARIANT_TEXTURE)) {
			return;
		}

		VariantSnapshot snapshot = level.globalAttachments()
				.getAttachedOrElse(VariantAttachments.SERVER_VARIANT_SNAPSHOT, VariantSnapshot.EMPTY);

		List<VariantDefinition> candidates = snapshot.variantsFor(entity.getType());

		if (candidates.isEmpty()) {
			return;
		}

		// Check if any candidate has conditions that require biome evaluation
		boolean needsBiomeCheck = candidates.stream()
				.anyMatch(c -> c.conditions().isPresent() && !c.conditions().get().isEmpty());

		Holder<net.minecraft.world.level.biome.Biome> biome = null;
		if (needsBiomeCheck) {
			BlockPos pos = entity.blockPosition();
			biome = level.getBiome(pos);
		}

		for (VariantDefinition candidate : candidates) {
			if (candidate.conditions().isPresent()) {
				VariantConditions conditions = candidate.conditions().get();
				if (!conditions.isEmpty()) {
					// Candidate has biome conditions, biome must have been looked up
					if (biome != null && conditions.matches(biome)) {
						entity.setAttached(VariantAttachments.VARIANT_TEXTURE, candidate.texture());
						applyAttributes(entity, candidate);
						return;
					}
					// Conditions not met, continue to next candidate
					continue;
				}
			}
			// No conditions or empty conditions (should not happen due to validation) -> eligible
			entity.setAttached(VariantAttachments.VARIANT_TEXTURE, candidate.texture());
			applyAttributes(entity, candidate);
			return;
		}

		// No candidate matched: do not attach a variant texture, allow vanilla fallback
	}

	/**
	 * Applies the configured absolute base attribute values from the selected variant to the entity.
	 *
	 * <p>This is called immediately after a variant is selected, as part of the same server-side
	 * selection operation. Failures to resolve or apply individual attributes are logged but do not
	 * prevent the variant texture from being assigned.
	 *
	 * @param entity the entity receiving the variant, must be a {@link LivingEntity} to have attributes
	 * @param variant the selected variant definition, may have empty or absent attributes
	 */
	private static void applyAttributes(Entity entity, VariantDefinition variant) {
		if (!(entity instanceof LivingEntity livingEntity)) {
			return;
		}

		if (!variant.attributes().isPresent()) {
			return;
		}

		VariantAttributes attributes = variant.attributes().get();
		if (attributes.isEmpty()) {
			return;
		}

		for (Map.Entry<Identifier, Double> entry : attributes.values().entrySet()) {
			Identifier attributeId = entry.getKey();
			Double value = entry.getValue();

			try {
				var attributeHolder = BuiltInRegistries.ATTRIBUTE.get(attributeId);
				if (attributeHolder.isEmpty()) {
					MobVariantsBRS.LOGGER.warn("Failed to apply attribute {} to entity {}: attribute not found in registry",
							attributeId, entity);
					continue;
				}

				Holder<Attribute> holder = attributeHolder.get();
				AttributeInstance instance = livingEntity.getAttribute(holder);
				if (instance == null) {
					MobVariantsBRS.LOGGER.warn("Failed to apply attribute {} to entity {}: entity does not have this attribute",
							attributeId, entity);
					continue;
				}

				instance.setBaseValue(value);
				MobVariantsBRS.LOGGER.debug("Applied base value {} to attribute {} for entity {}",
						value, attributeId, entity);
			} catch (Exception e) {
				MobVariantsBRS.LOGGER.error("Unexpected error applying attribute {} to entity {}",
						attributeId, entity, e);
			}
		}
	}
}