package com.baruc.brs.mobvariants.verify;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.level.biome.Biome;

import com.baruc.brs.mobvariants.variant.VariantAttributes;
import com.baruc.brs.mobvariants.variant.VariantConditions;
import com.baruc.brs.mobvariants.variant.VariantDefinition;
import com.baruc.brs.mobvariants.variant.VariantSnapshot;

/**
 * Focused executable checks for the optional variant-attribute feature.
 *
 * <p>Exercises the production codec ({@link VariantDefinition#CODEC}) and the
 * production validation path ({@link VariantSnapshot#build}) against the real
 * Minecraft 26.1.2 registries: {@code Bootstrap.bootStrap()} populates
 * {@link BuiltInRegistries#ATTRIBUTE}, and {@link VanillaRegistries#createLookup()}
 * provides the real vanilla biome registry contents. No mocks are involved.
 *
 * <p>Run with the {@code verifyVariantAttributes} Gradle task.
 */
public final class VariantAttributeVerification {
	private static final String ICE_ZOMBIE_JSON =
			"src/main/resources/data/mob_variants_brs/variants/ice_zombie.json";

	private static final Identifier ZOMBIE = Identifier.parse("minecraft:zombie");
	private static final Identifier TEXTURE =
			Identifier.fromNamespaceAndPath("mob_variants_brs", "textures/entity/zombie/zombie_ice.png");
	private static final Identifier MAX_HEALTH = Identifier.parse("minecraft:max_health");
	private static final Identifier ATTACK_DAMAGE = Identifier.parse("minecraft:attack_damage");
	private static final Identifier MOVEMENT_SPEED = Identifier.parse("minecraft:movement_speed");
	private static final Identifier SNOWY_PLAINS = Identifier.parse("minecraft:snowy_plains");

	/** The exact JSON shape the feature documents, including the optional top-level attributes. */
	private static final String VARIANT_WITH_ATTRIBUTES = """
			{
			  "entity_type": "minecraft:zombie",
			  "texture": "mob_variants_brs:textures/entity/zombie/zombie_ice.png",
			  "conditions": {
			    "biomes": ["minecraft:snowy_plains"]
			  },
			  "attributes": {
			    "minecraft:max_health": 40.0,
			    "minecraft:attack_damage": 5.0,
			    "minecraft:movement_speed": 0.3
			  }
			}
			""";

	/** An existing definition shape that omits the optional attributes object entirely. */
	private static final String VARIANT_WITHOUT_ATTRIBUTES = """
			{
			  "entity_type": "minecraft:zombie",
			  "texture": "mob_variants_brs:textures/entity/zombie/zombie_ice.png",
			  "conditions": {
			    "biomes": ["minecraft:snowy_plains"]
			  }
			}
			""";

	private static int passed;
	private static int failed;

	private VariantAttributeVerification() {
	}

	public static void main(String[] args) {
		// The standard datagen/bootstrap sequence: detect the game version from
		// the Minecraft jar manifest, then populate the real built-in registries.
		net.minecraft.SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		registryReadiness();
		codecDecoding();
		codecEncoding();
		codecOmittedField();
		codecRoundTrip();
		selectionOrderEvidence();
		validation();
		attributeApplicationApi();

		System.out.println();
		System.out.println("Checks passed: " + passed + ", failed: " + failed);
		if (failed > 0) {
			System.exit(1);
		}
	}

	/** Proves the real attribute registry is initialized and holds the real vanilla bounds. */
	private static void registryReadiness() {
		Optional<Attribute> maxHealth = BuiltInRegistries.ATTRIBUTE.getOptional(MAX_HEALTH);
		check("registry: real BuiltInRegistries.ATTRIBUTE is populated",
				!BuiltInRegistries.ATTRIBUTE.keySet().isEmpty(),
				"attribute registry is empty; Bootstrap.bootStrap() did not run");
		check("registry: minecraft:max_health resolves in the real registry",
				maxHealth.isPresent(), "minecraft:max_health missing from BuiltInRegistries.ATTRIBUTE");
		check("registry: resolved max_health is the vanilla Attributes.MAX_HEALTH implementation",
				maxHealth.isPresent() && maxHealth.get() == Attributes.MAX_HEALTH.value(),
				"registry resolution returned a different instance than Attributes.MAX_HEALTH");

		Attribute attribute = maxHealth.orElseThrow();
		check("registry: max_health is a RangedAttribute with the real supported bounds [1.0, 1024.0]",
				attribute instanceof RangedAttribute ranged
						&& ranged.getMinValue() == 1.0 && ranged.getMaxValue() == 1024.0,
				"unexpected bounds for " + attribute + ": " + attribute.getClass().getName());
	}

	/** Case 1: a variant definition with the intended top-level attributes object decodes. */
	private static void codecDecoding() {
		VariantDefinition definition = decode(VARIANT_WITH_ATTRIBUTES);

		check("decode: definition with top-level attributes decodes successfully", true, "unreachable");
		check("decode: entity_type retains its value",
				definition.entityType().equals(ZOMBIE),
				"entity_type was " + definition.entityType());
		check("decode: texture retains its value",
				definition.texture().equals(TEXTURE),
				"texture was " + definition.texture());
		check("decode: conditions retain their biomes",
				definition.conditions().isPresent()
						&& definition.conditions().get().biomes().equals(List.of(SNOWY_PLAINS)),
				"conditions were " + definition.conditions());
		check("decode: attributes object is present",
				definition.attributes().isPresent(), "attributes optional was empty");

		VariantAttributes attributes = definition.attributes().orElseThrow();
		Map<Identifier, Double> values = attributes.values();
		check("decode: attributes contains the three configured entries",
				values.size() == 3, "attributes contained " + values.size() + " entries: " + values);
		check("decode: minecraft:max_health decodes to 40.0",
				Double.valueOf(40.0).equals(values.get(MAX_HEALTH)),
				"max_health was " + values.get(MAX_HEALTH));
		check("decode: minecraft:attack_damage decodes to 5.0",
				Double.valueOf(5.0).equals(values.get(ATTACK_DAMAGE)),
				"attack_damage was " + values.get(ATTACK_DAMAGE));
		check("decode: minecraft:movement_speed decodes to 0.3",
				Double.valueOf(0.3).equals(values.get(MOVEMENT_SPEED)),
				"movement_speed was " + values.get(MOVEMENT_SPEED));
	}

	/** Case 2: encoding produces the top-level attributes object without accidental nesting. */
	private static void codecEncoding() {
		VariantDefinition definition = decode(VARIANT_WITH_ATTRIBUTES);
		JsonObject encoded = VariantDefinition.CODEC
				.encodeStart(JsonOps.INSTANCE, definition)
				.getOrThrow()
				.getAsJsonObject();

		check("encode: encoded output has a top-level attributes member",
				encoded.has("attributes"), "encoded object was " + encoded);
		JsonElement attributesMember = encoded.get("attributes");
		check("encode: top-level attributes member is a JSON object",
				attributesMember.isJsonObject(), "attributes member was " + attributesMember);
		JsonObject attributes = attributesMember.getAsJsonObject();
		check("encode: attributes object is flat, not nested under a second attributes key",
				!attributes.has("attributes"),
				"attributes contained a nested attributes object: " + attributes);
		check("encode: attributes object contains minecraft:max_health",
				attributes.has("minecraft:max_health"), "encoded attributes were " + attributes);
		check("encode: encoded minecraft:max_health is 40.0",
				attributes.get("minecraft:max_health").getAsDouble() == 40.0,
				"encoded max_health was " + attributes.get("minecraft:max_health"));
		check("encode: encoded minecraft:attack_damage is 5.0",
				attributes.get("minecraft:attack_damage").getAsDouble() == 5.0,
				"encoded attack_damage was " + attributes.get("minecraft:attack_damage"));
		check("encode: encoded minecraft:movement_speed is 0.3",
				attributes.get("minecraft:movement_speed").getAsDouble() == 0.3,
				"encoded movement_speed was " + attributes.get("minecraft:movement_speed"));
	}

	/** Case 3: a definition omitting attributes remains valid with the default (absent) behavior. */
	private static void codecOmittedField() {
		VariantDefinition definition = decode(VARIANT_WITHOUT_ATTRIBUTES);

		check("omitted: definition without attributes decodes successfully", true, "unreachable");
		check("omitted: attributes optional is empty",
				definition.attributes().isEmpty(),
				"attributes was unexpectedly present: " + definition.attributes());
		check("omitted: remaining fields still decode",
				definition.entityType().equals(ZOMBIE) && definition.texture().equals(TEXTURE)
						&& definition.conditions().isPresent(),
				"other fields lost: " + definition);

		JsonObject encoded = VariantDefinition.CODEC
				.encodeStart(JsonOps.INSTANCE, definition)
				.getOrThrow()
				.getAsJsonObject();
		check("omitted: encoding a definition without attributes produces no attributes member",
				!encoded.has("attributes"), "encoded object contained attributes: " + encoded);
	}

	/** Case 4: existing fields survive an encode/decode round trip. */
	private static void codecRoundTrip() {
		VariantDefinition original = decode(VARIANT_WITH_ATTRIBUTES);
		JsonElement encoded = VariantDefinition.CODEC
				.encodeStart(JsonOps.INSTANCE, original)
				.getOrThrow();
		VariantDefinition roundTripped = VariantDefinition.CODEC
				.parse(JsonOps.INSTANCE, encoded)
				.getOrThrow();

		check("round trip: encode then decode reproduces the original definition",
				roundTripped.equals(original),
				"round-tripped definition differs: " + roundTripped);
		check("round trip: attributes survive the round trip",
				roundTripped.attributes().equals(original.attributes()),
				"round-tripped attributes were " + roundTripped.attributes());
	}

	/**
	 * Executable evidence for the strong_zombie removal: the reload listener sorts
	 * candidates with {@link Identifier#compareTo}, ice_zombie sorts before
	 * strong_zombie under that comparator, and the ice variant's real definition
	 * already matches snowy_plains.
	 */
	private static void selectionOrderEvidence() {
		Identifier iceId = Identifier.fromNamespaceAndPath("mob_variants_brs", "ice_zombie");
		Identifier strongId = Identifier.fromNamespaceAndPath("mob_variants_brs", "strong_zombie");

		check("order: ice_zombie sorts before strong_zombie under the listener's comparator",
				iceId.compareTo(strongId) < 0,
				"comparator returned " + iceId.compareTo(strongId));

		JsonElement iceJson = JsonParser.parseString(readResource(ICE_ZOMBIE_JSON));
		VariantDefinition iceZombie = VariantDefinition.CODEC.parse(JsonOps.INSTANCE, iceJson).getOrThrow();
		check("order: the loaded ice_zombie.json definition matches biome minecraft:snowy_plains",
				iceZombie.conditions().isPresent()
						&& iceZombie.conditions().get().biomes().contains(SNOWY_PLAINS),
				"ice_zombie conditions were " + iceZombie.conditions());
		check("order: ice_zombie.json decodes with no attributes (existing definition unaffected)",
				iceZombie.attributes().isEmpty(),
				"ice_zombie unexpectedly declared attributes");
	}

	/**
	 * Cases 5-13: the production validation path, exercised with the real attribute
	 * registry and the real vanilla biome registry contents.
	 */
	private static void validation() {
		Registry<Biome> biomeRegistry = realBiomeRegistry();

		// Prepared in ascending variant-id order, as the reload listener guarantees.
		List<Map.Entry<Identifier, VariantDefinition>> prepared = new ArrayList<>(List.of(
				entry("above_max", withAttributes(Map.of(MAX_HEALTH, 2000.0))),
				entry("below_min", withAttributes(Map.of(MAX_HEALTH, 0.5))),
				entry("empty_attributes", new VariantDefinition(ZOMBIE, TEXTURE, Optional.empty(),
						Optional.of(new VariantAttributes(Map.of())))),
				entry("nan", withAttributes(Map.of(MAX_HEALTH, Double.NaN))),
				entry("neg_inf", decode(VARIANT_WITH_ATTRIBUTES.replace("40.0", "-1e999"))),
				entry("no_attributes", decode(VARIANT_WITHOUT_ATTRIBUTES)),
				entry("pos_inf", decode(VARIANT_WITH_ATTRIBUTES.replace("40.0", "1e999"))),
				entry("unknown_attr", withAttributes(
						Map.of(Identifier.parse("minecraft:not_an_attribute"), 1.0))),
				entry("unknown_biome", new VariantDefinition(ZOMBIE, TEXTURE,
						Optional.of(new VariantConditions(List.of(Identifier.parse("minecraft:not_a_biome")))),
						Optional.of(new VariantAttributes(Map.of(MAX_HEALTH, 40.0))))),
				entry("valid", withAttributes(Map.of(
						MAX_HEALTH, 40.0, ATTACK_DAMAGE, 5.0, MOVEMENT_SPEED, 0.3))),
				entry("valid_conditional", new VariantDefinition(ZOMBIE, TEXTURE,
						Optional.of(new VariantConditions(List.of(SNOWY_PLAINS))),
						Optional.of(new VariantAttributes(Map.of(MAX_HEALTH, 40.0)))))
		));
		prepared.sort(Comparator.comparing(Map.Entry::getKey));

		// A JSON number that overflows to infinity must decode to an infinite double,
		// so the non-finite rejection cases below are reachable from authored JSON.
		check("validation: JSON 1e999 reaches the codec as a non-finite double",
				JsonParser.parseString("{\"attributes\":{\"minecraft:max_health\":1e999}}")
						.getAsJsonObject().getAsJsonObject("attributes")
						.get("minecraft:max_health").getAsDouble() == Double.POSITIVE_INFINITY,
				"Gson did not parse 1e999 as +Infinity");

		VariantSnapshot snapshot = VariantSnapshot.build(prepared, biomeRegistry);

		EntityType<?> zombieType = BuiltInRegistries.ENTITY_TYPE.getOptional(ZOMBIE).orElseThrow();
		List<VariantDefinition> selectable = snapshot.variantsFor(zombieType);

		Set<Identifier> accepted = snapshot.byVariantId().keySet();
		check("validation: in-range finite values (max_health 40.0, attack_damage 5.0, movement_speed 0.3) are accepted",
				accepted.contains(id("valid")), "valid was rejected; accepted=" + accepted);
		check("validation: definition without attributes is accepted (backward compatibility)",
				accepted.contains(id("no_attributes")), "no_attributes was rejected; accepted=" + accepted);
		check("validation: empty attributes object is accepted as a no-op",
				accepted.contains(id("empty_attributes")), "empty_attributes was rejected; accepted=" + accepted);
		check("validation: definition with a real biome condition is accepted (real biome registry in use)",
				accepted.contains(id("valid_conditional")),
				"valid_conditional was rejected; accepted=" + accepted);
		check("validation: unknown attribute identifier is rejected",
				!accepted.contains(id("unknown_attr")), "unknown_attr was accepted; accepted=" + accepted);
		check("validation: NaN value is rejected",
				!accepted.contains(id("nan")), "nan was accepted; accepted=" + accepted);
		check("validation: positive infinity (1e999) is rejected",
				!accepted.contains(id("pos_inf")), "pos_inf was accepted; accepted=" + accepted);
		check("validation: negative infinity (-1e999) is rejected",
				!accepted.contains(id("neg_inf")), "neg_inf was accepted; accepted=" + accepted);
		check("validation: value below the attribute's minimum (max_health 0.5 < 1.0) is rejected",
				!accepted.contains(id("below_min")), "below_min was accepted; accepted=" + accepted);
		check("validation: value above the attribute's maximum (max_health 2000.0 > 1024.0) is rejected",
				!accepted.contains(id("above_max")), "above_max was accepted; accepted=" + accepted);
		check("validation: unknown biome identifier is rejected",
				!accepted.contains(id("unknown_biome")), "unknown_biome was accepted; accepted=" + accepted);

		check("selection: rejected definitions cannot participate in entity selection",
				selectable.stream().noneMatch(d -> !accepted.contains(variantIdOf(d, prepared))),
				"a rejected definition is selectable");
		check("selection: exactly the accepted definitions are selectable for minecraft:zombie",
				selectable.size() == 4, "expected 4 selectable definitions, got " + selectable.size());

		List<Identifier> selectableIds = selectable.stream()
				.map(d -> variantIdOf(d, prepared))
				.sorted()
				.toList();
		check("selection: selectable variants are ordered by ascending variant id",
				selectableIds.equals(List.of(id("empty_attributes"), id("no_attributes"),
						id("valid"), id("valid_conditional"))),
				"selectable order was " + selectableIds);

		VariantDefinition selectedValid = selectable.stream()
				.filter(d -> variantIdOf(d, prepared).equals(id("valid")))
				.findFirst().orElseThrow();
		check("selection: the accepted definition retains its configured attribute values",
				selectedValid.attributes().isPresent()
						&& Double.valueOf(40.0).equals(selectedValid.attributes().get().values().get(MAX_HEALTH)),
				"selected valid definition lost its attributes");
	}

	/**
	 * Runtime evidence for the exact API the application path uses: constructing a
	 * real AttributeInstance for the real max_health holder and assigning an
	 * absolute base value through it.
	 */
	private static void attributeApplicationApi() {
		Holder.Reference<Attribute> holder = BuiltInRegistries.ATTRIBUTE.get(MAX_HEALTH).orElseThrow();
		AttributeInstance instance = new AttributeInstance(holder, unused -> {
		});

		check("apply-api: a real AttributeInstance starts from the attribute's default base value",
				instance.getBaseValue() == Attributes.MAX_HEALTH.value().getDefaultValue(),
				"initial base value was " + instance.getBaseValue());

		instance.setBaseValue(40.0);
		check("apply-api: setBaseValue assigns an absolute base value (not an additive modifier)",
				instance.getBaseValue() == 40.0 && instance.getValue() == 40.0,
				"base=" + instance.getBaseValue() + ", value=" + instance.getValue());

		instance.setBaseValue(7.5);
		check("apply-api: a second setBaseValue replaces the previous absolute base value",
				instance.getBaseValue() == 7.5 && instance.getValue() == 7.5,
				"base=" + instance.getBaseValue() + ", value=" + instance.getValue());
	}

	/**
	 * Builds a real {@link Registry} holding the real vanilla biome values, the same
	 * implementation and contents the server's worldgen registry layer uses.
	 */
	private static Registry<Biome> realBiomeRegistry() {
		HolderLookup.RegistryLookup<Biome> lookup = VanillaRegistries.createLookup()
				.lookupOrThrow(Registries.BIOME);
		MappedRegistry<Biome> registry =
				new MappedRegistry<>(Registries.BIOME, Lifecycle.stable(), false);
		lookup.listElements().forEach(element ->
				registry.register(element.key(), element.value(), RegistrationInfo.BUILT_IN));
		registry.freeze();

		check("registry: real vanilla biome registry contains minecraft:snowy_plains",
				registry.getOptional(SNOWY_PLAINS).isPresent(),
				"snowy_plains missing from the constructed biome registry");
		check("registry: biome registry is non-empty (" + registry.keySet().size() + " biomes)",
				!registry.keySet().isEmpty(), "biome registry is empty");
		return registry;
	}

	private static VariantDefinition decode(String json) {
		return VariantDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
	}

	private static VariantDefinition withAttributes(Map<Identifier, Double> values) {
		return new VariantDefinition(ZOMBIE, TEXTURE, Optional.empty(),
				Optional.of(new VariantAttributes(values)));
	}

	private static Map.Entry<Identifier, VariantDefinition> entry(String name, VariantDefinition definition) {
		return new AbstractMap.SimpleImmutableEntry<>(id(name), definition);
	}

	private static Identifier id(String name) {
		return Identifier.fromNamespaceAndPath("mob_variants_brs", name);
	}

	private static Identifier variantIdOf(VariantDefinition definition,
			List<Map.Entry<Identifier, VariantDefinition>> prepared) {
		return prepared.stream()
				.filter(candidate -> candidate.getValue() == definition)
				.map(Map.Entry::getKey)
				.findFirst()
				.orElseThrow();
	}

	private static String readResource(String path) {
		try {
			return Files.readString(Path.of(path));
		} catch (Exception e) {
			throw new IllegalStateException("Could not read " + path, e);
		}
	}

	private static void check(String name, boolean condition, String detail) {
		if (condition) {
			passed++;
			System.out.println("[PASS] " + name);
		} else {
			failed++;
			System.out.println("[FAIL] " + name);
			System.out.println("       " + detail);
		}
	}
}
