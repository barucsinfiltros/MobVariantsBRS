package com.baruc.brs.mobvariants.variant;

import java.io.Reader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.baruc.brs.mobvariants.MobVariantsBRS;
import com.baruc.brs.mobvariants.attachment.VariantAttachments;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.JsonOps;

import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Loads variant definitions from the server data resource manager and publishes one immutable
 * snapshot per reload.
 *
 * <p>Registered on {@code ResourceLoader.get(PackType.SERVER_DATA)} only. Fabric injects a
 * listener into a reload pipeline exclusively for that pipeline's own {@code PackType}, so this
 * loader can never be driven by client resources and no client-side registry can exist.
 *
 * <p>{@link SimpleReloadListener} is otherwise kept stateless: the only field besides the prepared
 * cache is a single nullable reference to the running server, needed because {@code apply} is
 * handed a {@link PreparableReloadListener.SharedState} that exposes no server.
 */
public final class VariantDefinitionReloadListener
		extends SimpleReloadListener<List<Map.Entry<Identifier, VariantDefinition>>> {

	private static final VariantDefinitionReloadListener INSTANCE = new VariantDefinitionReloadListener();

	/**
	 * {@code ResourceManager.listResources} resolves its argument against
	 * {@code <pack>/data/<pack namespace>}, not {@code <pack>/data}, and returns keys shaped
	 * {@code <namespace>:<startingPath>/<file>}. Verified against Minecraft 26.1.2.
	 */
	private static final String DIRECTORY = "variants";
	private static final String DIRECTORY_PREFIX = DIRECTORY + "/";
	private static final String JSON_EXTENSION = ".json";
	private static final Predicate<Identifier> JSON_FILE = id -> id.getPath().endsWith(JSON_EXTENSION);

	/** Written only by server lifecycle events, read only on the game thread. */
	private @Nullable MinecraftServer server;

	private VariantDefinitionReloadListener() {
	}

	public static VariantDefinitionReloadListener getInstance() {
		return INSTANCE;
	}

	/**
	 * Called from {@code ServerLifecycleEvents.SERVER_STARTING}, and publishes the startup snapshot.
	 *
	 * <p>Minecraft 26.1.2 builds and reloads the pack repository once while <em>constructing</em> the
	 * server, before {@code SERVER_STARTING}, and never reloads it again during {@code initServer};
	 * only {@code /reload} does. Waiting for a reload to publish would therefore leave the snapshot
	 * absent for the whole life of a normal server, so the already-populated server data resource
	 * manager is read once here, on the server thread, before any entity can be loaded.
	 */
	public void attach(MinecraftServer server) {
		this.server = server;

		try {
			server.globalAttachments().setAttached(VariantAttachments.SERVER_VARIANT_SNAPSHOT,
					VariantSnapshot.build(load(server.getResourceManager()), server.registryAccess().lookupOrThrow(Registries.BIOME)));
		} catch (Exception e) {
			MobVariantsBRS.LOGGER.error("Failed to publish the startup variant snapshot; "
					+ "variants stay inactive until the next reload.", e);
		}
	}

	/** Called from {@code ServerLifecycleEvents.SERVER_STOPPED}, after all levels have closed. */
	public void detach() {
		this.server = null;
	}

	/**
	 * Runs off-thread during a reload. Decodes every definition file, skipping and logging any file
	 * that fails, so one bad file cannot abort the reload.
	 */
	@Override
	protected List<Map.Entry<Identifier, VariantDefinition>> prepare(PreparableReloadListener.SharedState state) {
		return load(state.resourceManager());
	}

	private static List<Map.Entry<Identifier, VariantDefinition>> load(ResourceManager resourceManager) {
		Map<Identifier, Resource> resources = resourceManager.listResources(DIRECTORY, JSON_FILE);
		List<Map.Entry<Identifier, VariantDefinition>> prepared = new ArrayList<>(resources.size());

		for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
			Identifier resourceId = entry.getKey();
			Identifier variantId = variantIdOf(resourceId);

			if (variantId == null) {
				continue;
			}

			try (Reader reader = entry.getValue().openAsReader()) {
				JsonElement json = JsonParser.parseReader(reader);
				VariantDefinition definition = VariantDefinition.CODEC
						.parse(JsonOps.INSTANCE, json)
						.getOrThrow(message -> new IllegalArgumentException(message));

				prepared.add(new AbstractMap.SimpleImmutableEntry<>(variantId, definition));
			} catch (Exception e) {
				MobVariantsBRS.LOGGER.error("Skipping unreadable variant definition {}: {}",
						resourceId, e.getMessage());
			}
		}

		prepared.sort((left, right) -> left.getKey().compareTo(right.getKey()));
		MobVariantsBRS.LOGGER.debug("Loaded {} variant definitions from {} candidate files",
				prepared.size(), resources.size());
		return List.copyOf(prepared);
	}

	/**
	 * Runs on the game thread and is the only place that publishes. The whole snapshot is built
	 * before the single reference assignment, so a reader can only ever see a complete snapshot and
	 * never a partially rebuilt one.
	 */
	@Override
	protected void apply(List<Map.Entry<Identifier, VariantDefinition>> prepared,
			PreparableReloadListener.SharedState state) {
		MinecraftServer server = this.server;

		if (server == null) {
			// Expected, not a failure: Minecraft builds and reloads the pack repository once while
			// constructing the server, which runs before ServerLifecycleEvents.SERVER_STARTING.
			// There is no server to publish to yet; attach() publishes the startup snapshot from the
			// same resource manager, and this listener owns every reload after that.
			MobVariantsBRS.LOGGER.debug("Skipping variant snapshot publication: the server has not "
					+ "started yet; the startup snapshot is published by attach().");
			return;
		}

		try {
			server.globalAttachments().setAttached(VariantAttachments.SERVER_VARIANT_SNAPSHOT,
					VariantSnapshot.build(prepared, server.registryAccess().lookupOrThrow(Registries.BIOME)));
		} catch (Exception e) {
			MobVariantsBRS.LOGGER.error("Failed to publish the variant snapshot; "
					+ "the previous snapshot remains in effect.", e);
		}
	}

/**
	 * Variant identity is path-derived: {@code mob_variants_brs:variants/ice_zombie.json} becomes
	 * {@code mob_variants_brs:ice_zombie}. Only this namespace's own definitions are consumed, so a
	 * {@code variants} directory belonging to some other namespace can never claim a variant id.
	 */
	private static @Nullable Identifier variantIdOf(Identifier resourceId) {
		if (!resourceId.getNamespace().equals(MobVariantsBRS.MOD_ID)) {
			MobVariantsBRS.LOGGER.warn("Skipping variant definition {}: only the {} namespace is loaded",
					resourceId, MobVariantsBRS.MOD_ID);
			return null;
		}

		String path = resourceId.getPath();
		int nameStart = path.startsWith(DIRECTORY_PREFIX) ? DIRECTORY_PREFIX.length() : 0;

		return Identifier.fromNamespaceAndPath(MobVariantsBRS.MOD_ID,
				path.substring(nameStart, path.length() - JSON_EXTENSION.length()));
	}
}