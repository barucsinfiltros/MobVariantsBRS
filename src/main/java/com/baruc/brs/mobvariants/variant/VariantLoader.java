package com.baruc.brs.mobvariants.variant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class VariantLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("mob_variants_brs/variant_loader");
    private static final String VARIANTS_FOLDER = "variants";
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mob_variants_brs", "variants");

    private final VariantRegistry registry;

    public VariantLoader(VariantRegistry registry) {
        super(MobVariantsBRS.GSON, VARIANTS_FOLDER);
        this.registry = registry;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        registry.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement element = entry.getValue();

            if (!element.isJsonObject()) {
                LOGGER.warn("Skipping non-object variant file: {}", fileId);
                continue;
            }

            JsonObject object = element.getAsJsonObject();
            try {
                ResourceLocation baseEntity = ResourceLocation.tryParse(object.get("base_entity").getAsString());
                ResourceLocation texture = ResourceLocation.tryParse(object.get("texture").getAsString());

                if (baseEntity == null || texture == null) {
                    LOGGER.warn("Invalid resource location in variant file: {}", fileId);
                    continue;
                }

                String variantPath = fileId.getPath();
                if (variantPath.startsWith(VARIANTS_FOLDER + "/")) {
                    variantPath = variantPath.substring(VARIANTS_FOLDER.length() + 1);
                }
                if (variantPath.endsWith(".json")) {
                    variantPath = variantPath.substring(0, variantPath.length() - 5);
                }
                ResourceLocation variantId = ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), variantPath);

                VariantDefinition definition = new VariantDefinition(baseEntity, texture);
                registry.register(variantId, definition);
                LOGGER.info("Loaded variant: {} -> {}", variantId, definition);
            } catch (Exception e) {
                LOGGER.error("Failed to parse variant file: {}", fileId, e);
            }
        }
        LOGGER.info("Loaded {} variants", registry.getVariantIds().size());
    }

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }

    public static void register(VariantRegistry registry) {
        ResourceManagerHelper.get(ResourceManagerHelper.SERVER_PACK).registerReloadListener(new VariantLoader(registry));
    }
}