package com.baruc.brs.mobvariants.variant;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * One authored variant definition, exactly as written in a
 * {@code data/<namespace>/variants/<name>.json} server data file.
 *
 * <p>Variant identity is not stored here; it is derived from the file path at load time and
 * lives in {@link VariantSnapshot#byVariantId()}.
 *
 * <p>Conditions are optional. When absent, the candidate is eligible for every entity of its
 * declared {@code entity_type}. When present, {@code conditions.biomes} restricts eligibility
 * to the listed biomes.
 *
 * <p>Attributes are optional. When present, they declare absolute base attribute values
 * to be applied once when the variant is selected on the server.
 */
public record VariantDefinition(Identifier entityType, Identifier texture, Optional<VariantConditions> conditions, Optional<VariantAttributes> attributes) {
	public static final Codec<VariantDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.fieldOf("entity_type").forGetter(VariantDefinition::entityType),
			Identifier.CODEC.fieldOf("texture").forGetter(VariantDefinition::texture),
			VariantConditions.CODEC.optionalFieldOf("conditions").forGetter(VariantDefinition::conditions),
			VariantAttributes.CODEC.optionalFieldOf("attributes").forGetter(VariantDefinition::attributes)
	).apply(i, VariantDefinition::new));
}