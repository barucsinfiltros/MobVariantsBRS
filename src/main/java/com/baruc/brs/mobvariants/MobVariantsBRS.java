package com.baruc.brs.mobvariants;

import java.util.Objects;

import com.baruc.brs.mobvariants.attachment.VariantAttachments;
import com.baruc.brs.mobvariants.variant.VariantStateLifecycle;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobVariantsBRS implements ModInitializer {
	public static final String MOD_ID = "mob_variants_brs";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// Referencing the attachment types initialises VariantAttachments, which is what registers
		// them on this logical side. This has to happen on the client as well: Fabric's attachment
		// sync handshake advertises only the attachment types the receiving side actually
		// registered, so a client that never initialises this class does not accept
		// `variant_texture` and the server silently skips the sync for every player.
		Objects.requireNonNull(VariantAttachments.VARIANT_TEXTURE);

		VariantStateLifecycle.register();

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
