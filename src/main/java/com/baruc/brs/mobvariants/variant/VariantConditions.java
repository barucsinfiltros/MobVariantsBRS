package com.baruc.brs.mobvariants.variant;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * Optional selection conditions for a variant definition.
 *
 * <p>v1 supports only biome restrictions. Conditions are evaluated once at entity load
 * during {@link VariantStateLifecycle#selectVariant(Entity, net.minecraft.server.level.ServerLevel)}
 * and are not persisted.
 *
 * <p>A definition without conditions is eligible for every entity of its declared type.
 * A definition with conditions is eligible only when all present conditions match.
 */
public record VariantConditions(List<Identifier> biomes) {
	public static final Codec<VariantConditions> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.listOf().optionalFieldOf("biomes", List.of()).forGetter(VariantConditions::biomes)
	).apply(i, VariantConditions::new));

	/**
	 * Checks whether this conditions instance is empty (no conditions declared).
	 * An empty conditions instance is considered an authoring error and should be
	 * rejected during definition loading.
	 */
	public boolean isEmpty() {
		return biomes == null || biomes.isEmpty();
	}

	/**
	 * Evaluates these conditions against the given biome holder.
	 *
	 * @param biome the biome the entity is in, never null
	 * @return true if all conditions match, false otherwise
	 */
	public boolean matches(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
		if (biomes == null || biomes.isEmpty()) {
			return true;
		}
		return biomes.stream().anyMatch(biome::is);
	}
}