package com.baruc.brs.mobvariants.gametest;

import java.io.Reader;

import com.baruc.brs.mobvariants.variant.VariantDefinition;
import com.mojang.serialization.JsonOps;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

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
		helper.succeed();
	}
}
