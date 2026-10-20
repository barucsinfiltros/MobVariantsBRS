package com.baruc.brs.mobvariants.gametest;

import java.util.AbstractMap;
import java.util.List;
import java.util.Optional;

import com.baruc.brs.mobvariants.MobVariantsBRS;
import com.baruc.brs.mobvariants.attachment.VariantAttachments;
import com.baruc.brs.mobvariants.variant.VariantConditions;
import com.baruc.brs.mobvariants.variant.VariantDefinition;
import com.baruc.brs.mobvariants.variant.VariantDefinitionReloadListener;
import com.baruc.brs.mobvariants.variant.VariantSnapshot;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

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
		try (var reader = resource.get().openAsReader()) {
			com.google.gson.JsonElement json = com.google.gson.JsonParser.parseReader(reader);
			VariantDefinition.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow(message -> new IllegalArgumentException(message));
		} catch (Exception e) {
			helper.fail("Failed to parse variant resource " + resourceId + ": " + e.getMessage());
			return;
		}

		// A - Reload and publication integration via real listener
		try {
			VariantDefinitionReloadListener.getInstance().attach(server);
			var snapshot = server.globalAttachments().getAttachedOrElse(VariantAttachments.SERVER_VARIANT_SNAPSHOT, VariantSnapshot.EMPTY);
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
		// B - Snapshot semantics using already-parsed definitions (in-memory)
		try {
			var biomeRegistry = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);

			// Valid definition constructed in memory (matches ice_zombie semantics)
			Identifier validVariantId = Identifier.fromNamespaceAndPath(MobVariantsBRS.MOD_ID, "ice_zombie");
			Identifier textureId = Identifier.fromNamespaceAndPath(MobVariantsBRS.MOD_ID, "textures/entity/zombie/ice_zombie.png");
			Identifier zombieEntityType = Identifier.withDefaultNamespace("zombie");
			// Use a known valid biome from the registry for conditions
			Identifier validBiomeId = Identifier.withDefaultNamespace("snowy_plains");
			if (biomeRegistry.getOptional(validBiomeId).isEmpty()) {
				// Fallback to first available biome if snowy_plains not present
				var first = biomeRegistry.keySet().iterator();
				if (first.hasNext()) {
					validBiomeId = first.next();
				}
			}
			VariantConditions validConditions = new VariantConditions(List.of(validBiomeId));
			VariantDefinition validDef = new VariantDefinition(zombieEntityType, textureId, Optional.of(validConditions), Optional.empty());

			var validEntry = new AbstractMap.SimpleImmutableEntry<>(validVariantId, validDef);
			var snapshot = VariantSnapshot.build(List.of(validEntry), biomeRegistry);
			if (!snapshot.byVariantId().containsKey(validVariantId)) {
				helper.fail("B: valid definition not accepted");
				return;
			}
			if (snapshot.variantsFor(EntityType.ZOMBIE).isEmpty()) {
				helper.fail("B: valid definition not indexed by entity type");
				return;
			}

			// Genuine negative case: conditions present but empty -> contract violation
			VariantConditions emptyConditions = new VariantConditions(List.of());
			VariantDefinition invalidEmptyConditionsDef = new VariantDefinition(zombieEntityType, textureId, Optional.of(emptyConditions), Optional.empty());
			var invalidEntry = new AbstractMap.SimpleImmutableEntry<>(Identifier.fromNamespaceAndPath(MobVariantsBRS.MOD_ID, "empty_conditions"), invalidEmptyConditionsDef);
			var snapshotInvalid = VariantSnapshot.build(List.of(invalidEntry), biomeRegistry);
			if (snapshotInvalid.byVariantId().containsKey(Identifier.fromNamespaceAndPath(MobVariantsBRS.MOD_ID, "empty_conditions"))) {
				helper.fail("B: definition with empty conditions incorrectly accepted");
				return;
			}
		} catch (Exception e) {
			helper.fail("B: failed: " + e.getMessage());
			return;
		}

		helper.succeed();
	}
}
