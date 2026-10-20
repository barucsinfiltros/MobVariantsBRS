package com.baruc.brs.mobvariants.gametest;

import java.io.Reader;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import com.baruc.brs.mobvariants.MobVariantsBRS;
import com.baruc.brs.mobvariants.attachment.VariantAttachments;
import com.baruc.brs.mobvariants.variant.VariantDefinition;
import com.baruc.brs.mobvariants.variant.VariantDefinitionReloadListener;
import com.baruc.brs.mobvariants.variant.VariantSnapshot;
import com.mojang.serialization.JsonOps;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;

public final class VariantResourceGameTest {
	public VariantResourceGameTest() {
	}

	@GameTest
	public void variantDefinitionFound(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		var resourceId = Identifier.fromNamespaceAndPath("mob_variants_brs", "variants/ice_zombie.json");
		var resource = server.getResourceManager().getResource(resourceId);
		if (resource.isEmpty()) {
			helper.fail("Expected variant resource not found: " + resourceId);
			return;
		}
		try (Reader reader = resource.get().openAsReader()) {
			JsonElement json = JsonParser.parseReader(reader);
			VariantDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(message -> new IllegalArgumentException(message));
		} catch (Exception e) {
			helper.fail("Failed to parse variant resource " + resourceId + ": " + e.getMessage());
			return;
		}

		// A - Reload and publication integration via real listener
		try {
			VariantDefinitionReloadListener.getInstance().attach(server);
			var snapshot = server.globalAttachments().getAttachedOrElse(VariantAttachments.SERVER_VARIANT_SNAPSHOT, VariantSnapshot.EMPTY);
			var zombieVariants = snapshot.variantsFor(EntityType.ZOMBIE);
			boolean foundIceZombie = false;
			for (var def : zombieVariants) {
				// variantId is path-derived; just ensure it's present in snapshot for zombie
			}
			// Check byVariantId contains ice_zombie
			if (!snapshot.byVariantId().containsKey(Identifier.fromNamespaceAndPath(MobVariantsBRS.MOD_ID, "ice_zombie"))) {
				helper.fail("A: ice_zombie not published in server snapshot via attach()");
				return;
			}
		} catch (Exception e) {
			helper.fail("A: attach() or snapshot validation failed: " + e.getMessage());
			return;
		}

		helper.succeed();
	}

	@GameTest
	public void variantSnapshotBuildSemantics(GameTestHelper helper) {
		// B - Snapshot semantics using already-parsed definitions
		try {
			var biomeRegistry = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
			// Build a valid definition for ice_zombie
			var resourceId = Identifier.fromNamespaceAndPath("mob_variants_brs", "variants/ice_zombie.json");
			var resource = helper.getLevel().getServer().getResourceManager().getResource(resourceId);
			if (resource.isEmpty()) {
				helper.fail("B: missing fixture ice_zombie.json");
				return;
			}
			VariantDefinition validDef;
			try (Reader reader = resource.get().openAsReader()) {
				JsonElement json = JsonParser.parseReader(reader);
				validDef = VariantDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(message -> new IllegalArgumentException(message));
			}

			var validEntry = new AbstractMap.SimpleImmutableEntry<>(Identifier.fromNamespaceAndPath(MobVariantsBRS.MOD_ID, "ice_zombie"), validDef);
			var snapshot = VariantSnapshot.build(List.of(validEntry), biomeRegistry);
			if (!snapshot.byVariantId().containsKey(Identifier.fromNamespaceAndPath(MobVariantsBRS.MOD_ID, "ice_zombie"))) {
				helper.fail("B: valid definition not accepted");
				return;
			}
			if (snapshot.variantsFor(EntityType.ZOMBIE).isEmpty()) {
				helper.fail("B: valid definition not indexed by entity type");
				return;
			}

			// Non-existent id not present and valid still present
			var snapshot2 = VariantSnapshot.build(List.of(validEntry), biomeRegistry);
			if (snapshot2.byVariantId().containsKey(Identifier.fromNamespaceAndPath(MobVariantsBRS.MOD_ID, "nonexistent"))) {
				helper.fail("B: nonexistent id incorrectly present");
				return;
			}
			if (!snapshot2.byVariantId().containsKey(Identifier.fromNamespaceAndPath(MobVariantsBRS.MOD_ID, "ice_zombie"))) {
				helper.fail("B: valid definition lost");
				return;
			}
		} catch (Exception e) {
			helper.fail("B: failed: " + e.getMessage());
			return;
		}

		helper.succeed();
	}
}
