package com.baruc.brs.mobvariants;

import com.baruc.brs.mobvariants.variant.VariantAttachment;
import com.baruc.brs.mobvariants.variant.VariantLoader;
import com.baruc.brs.mobvariants.variant.VariantRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobVariantsBRS implements ModInitializer {
    public static final String MOD_ID = "mob_variants_brs";
    public static final Gson GSON = new GsonBuilder().create();

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final VariantRegistry VARIANT_REGISTRY = new VariantRegistry();

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        VariantAttachment.register();
        VariantLoader.register(VARIANT_REGISTRY);

        LOGGER.info("Hello Fabric world!");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
