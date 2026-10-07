package com.baruc.brs.mobvariants.variant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * One authored variant definition, exactly as written in a
 * {@code data/<namespace>/variants/<name>.json} server data file.
 *
 * <p>Both fields are required and there is no derived field: a definition <em>is</em> its file
 * body. Variant identity is not stored here; it is derived from the file path at load time and
 * lives in {@link VariantSnapshot#byVariantId()}.
 */
public record VariantDefinition(Identifier entityType, Identifier texture) {
	public static final Codec<VariantDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.fieldOf("entity_type").forGetter(VariantDefinition::entityType),
			Identifier.CODEC.fieldOf("texture").forGetter(VariantDefinition::texture)
	).apply(i, VariantDefinition::new));
}