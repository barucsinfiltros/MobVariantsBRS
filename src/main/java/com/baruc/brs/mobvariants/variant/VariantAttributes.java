package com.baruc.brs.mobvariants.variant;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * Optional attribute declarations for a variant definition.
 *
 * <p>Represents absolute base attribute values to be applied once when a variant
 * is selected on the server. Values are stored as validated numeric entries keyed
 * by attribute identifier.
 */
public record VariantAttributes(Map<Identifier, Double> values) {
	public static final Codec<VariantAttributes> CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.DOUBLE)
			.xmap(VariantAttributes::new, VariantAttributes::values);

	/**
	 * @return true if no attributes are declared
	 */
	public boolean isEmpty() {
		return values == null || values.isEmpty();
	}

	public VariantAttributes {
		if (values == null || values.isEmpty()) {
			values = Map.of();
		} else {
			Map<Identifier, Double> copied = new LinkedHashMap<>(values.size());
			for (Map.Entry<Identifier, Double> entry : values.entrySet()) {
				copied.put(entry.getKey(), entry.getValue());
			}
			values = Collections.unmodifiableMap(copied);
		}
	}
}
