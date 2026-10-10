package com.baruc.brs.mobvariants.variant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.baruc.brs.mobvariants.MobVariantsBRS;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.level.biome.Biome;

/**
 * Immutable, complete index of the variant definitions currently loaded by the server.
 *
 * <p>A snapshot is published as a single reference on the running server's
 * {@code GlobalAttachments}, so a reader only ever observes a fully built snapshot and the
 * structure must never be mutated afterwards. The canonical constructor defensively copies both
 * maps and every candidate list, so immutability holds even if a caller passes a mutable map.
 *
 * @param byVariantId path-derived variant id ({@code mob_variants_brs:<name>}) to definition
 * @param byEntityType entity type to its definitions, in ascending variant id order
 */
public record VariantSnapshot(
		Map<Identifier, VariantDefinition> byVariantId,
		Map<EntityType<?>, List<VariantDefinition>> byEntityType) {

	/** Snapshot published before any datapack has been read; every entity misses it. */
	public static final VariantSnapshot EMPTY = new VariantSnapshot(Map.of(), Map.of());

	public VariantSnapshot {
		byVariantId = Map.copyOf(byVariantId);
		Map<EntityType<?>, List<VariantDefinition>> copied = new LinkedHashMap<>();

		for (Map.Entry<EntityType<?>, List<VariantDefinition>> entry : byEntityType.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				copied.put(entry.getKey(), List.copyOf(entry.getValue()));
			}
		}

		byEntityType = Map.copyOf(copied);
	}

	/** Never null and never allocating: the shared empty list is returned for unknown types. */
	public List<VariantDefinition> variantsFor(EntityType<?> type) {
		return byEntityType.getOrDefault(type, List.of());
	}

	/**
	 * Validates the parsed definitions and builds an ordered snapshot.
	 *
	 * <p>Must be called on the game thread: it reads {@link BuiltInRegistries#ENTITY_TYPE}, which
	 * is not safe to read from a reload preparation thread.
	 *
	 * <p>A definition whose {@code entity_type} is absent from the registry is logged and skipped
	 * rather than defaulted. {@code BuiltInRegistries.ENTITY_TYPE} is a {@code DefaultedRegistry},
	 * whose {@code getValue} silently substitutes the default entity for an unknown id, so the
	 * non-defaulting {@code getOptional} lookup is required here.
	 *
	 * <p>A definition whose {@code conditions.biomes} contains an unknown biome identifier is
	 * logged and skipped. An empty {@code conditions} object or an empty {@code biomes} list is
	 * treated as an authoring error and the definition is skipped.
	 *
	 * @param prepared parsed entries in ascending variant id order, as produced by the reload listener
	 * @param biomeRegistry the biome registry for validating biome identifiers
	 */
	public static VariantSnapshot build(List<Map.Entry<Identifier, VariantDefinition>> prepared, Registry<Biome> biomeRegistry) {
		Map<Identifier, VariantDefinition> byVariantId = new LinkedHashMap<>();
		Map<EntityType<?>, List<VariantDefinition>> byEntityType = new LinkedHashMap<>();

		for (Map.Entry<Identifier, VariantDefinition> entry : prepared) {
			Identifier variantId = entry.getKey();
			VariantDefinition definition = entry.getValue();

			EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(definition.entityType())
					.orElse(null);

			if (entityType == null) {
				MobVariantsBRS.LOGGER.error("Skipping variant definition {}: unknown entity type {}",
						variantId, definition.entityType());
				continue;
			}

// Validate conditions if present
		if (definition.conditions().isPresent()) {
			VariantConditions conditions = definition.conditions().get();
			if (conditions.isEmpty()) {
				MobVariantsBRS.LOGGER.error("Skipping variant definition {}: empty conditions are not allowed",
						variantId);
				continue;
			}

			boolean hasInvalidBiome = false;
			for (Identifier biomeId : conditions.biomes()) {
				if (biomeRegistry.getOptional(biomeId).isEmpty()) {
					MobVariantsBRS.LOGGER.error("Skipping variant definition {}: unknown biome {}",
							variantId, biomeId);
					hasInvalidBiome = true;
					break;
				}
			}
			if (hasInvalidBiome) {
				continue;
			}
		}

		// Validate attributes if present
		if (definition.attributes().isPresent()) {
			VariantAttributes attributes = definition.attributes().get();
			if (!attributes.isEmpty()) {
				boolean hasInvalidAttribute = false;
				for (Map.Entry<Identifier, Double> attrEntry : attributes.values().entrySet()) {
					Identifier attributeId = attrEntry.getKey();
					Double value = attrEntry.getValue();

					// Check for non-finite values
					if (value == null || value.isNaN() || value.isInfinite()) {
						MobVariantsBRS.LOGGER.error("Skipping variant definition {}: attribute {} has non-finite value {}",
								variantId, attributeId, value);
						hasInvalidAttribute = true;
						break;
					}

					// Resolve attribute from registry
					var attributeHolder = BuiltInRegistries.ATTRIBUTE.get(attributeId);
					if (attributeHolder.isEmpty()) {
						MobVariantsBRS.LOGGER.error("Skipping variant definition {}: unknown attribute {}",
								variantId, attributeId);
						hasInvalidAttribute = true;
						break;
					}

					// Validate against attribute bounds if it's a RangedAttribute
					Attribute attribute = attributeHolder.get().value();
					if (attribute instanceof RangedAttribute rangedAttribute) {
						double minValue = rangedAttribute.getMinValue();
						double maxValue = rangedAttribute.getMaxValue();
						if (value < minValue || value > maxValue) {
							MobVariantsBRS.LOGGER.error("Skipping variant definition {}: attribute {} value {} outside supported range [{}, {}]",
									variantId, attributeId, value, minValue, maxValue);
							hasInvalidAttribute = true;
							break;
						}
					}
				}
				if (hasInvalidAttribute) {
					continue;
				}
			}
		}

		byVariantId.put(variantId, definition);
		byEntityType.computeIfAbsent(entityType, unused -> new ArrayList<>()).add(definition);
	}

	return new VariantSnapshot(byVariantId, byEntityType);
	}
}