package com.baruc.brs.mobvariants.variant;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class VariantRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("mob_variants_brs/variant_registry");

    private final Map<ResourceLocation, VariantDefinition> variants = new HashMap<>();

    public void register(ResourceLocation id, VariantDefinition definition) {
        if (variants.containsKey(id)) {
            LOGGER.warn("Overriding existing variant: {}", id);
        }
        variants.put(id, definition);
    }

    public void clear() {
        variants.clear();
    }

    public Optional<VariantDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(variants.get(id));
    }

    public boolean contains(ResourceLocation id) {
        return variants.containsKey(id);
    }

    public Set<ResourceLocation> getVariantIds() {
        return ImmutableSet.copyOf(variants.keySet());
    }

    public Map<ResourceLocation, VariantDefinition> getAllVariants() {
        return ImmutableMap.copyOf(variants);
    }

    public ImmutableSet<ResourceLocation> getVariantsForBaseEntity(ResourceLocation baseEntity) {
        ImmutableSet.Builder<ResourceLocation> builder = ImmutableSet.builder();
        for (Map.Entry<ResourceLocation, VariantDefinition> entry : variants.entrySet()) {
            if (entry.getValue().baseEntity().equals(baseEntity)) {
                builder.add(entry.getKey());
            }
        }
        return builder.build();
    }
}