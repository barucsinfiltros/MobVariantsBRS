package com.baruc.brs.mobvariants.variant;

import java.util.List;

import com.baruc.brs.mobvariants.MobVariantsBRS;
import com.baruc.brs.mobvariants.attachment.VariantAttachments;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.Entity;

/**
 * Server wiring for the variant state layer: the {@code SERVER_DATA} definition loader, the
 * server reference the loader publishes through, and the one-shot per-entity selection.
 *
 * <p>Selection is server-authoritative end to end. The client runs none of this, resolves nothing,
 * and receives the already-resolved texture through Fabric's attachment synchronisation.
 */
public final class VariantStateLifecycle {
	private VariantStateLifecycle() {
	}

	public static void register() {
		VariantDefinitionReloadListener loader = VariantDefinitionReloadListener.getInstance();

		ResourceLoader.get(PackType.SERVER_DATA)
				.registerReloadListener(MobVariantsBRS.id("variants"), loader);

		ServerLifecycleEvents.SERVER_STARTING.register(loader::attach);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> loader.detach());

		ServerEntityEvents.ENTITY_LOAD.register(VariantStateLifecycle::selectVariant);
	}

	/**
	 * Resolves one entity's texture, once. Guard 1 avoids work for entities whose value was just
	 * restored from NBT; guard 2 is the load-bearing one, because the attachment is persistent and
	 * therefore already present on every disk-restored, reloaded, teleported or respawned entity.
	 * Together they make selection idempotent without depending on {@code isLoadedFromDisk()}.
	 *
	 * <p>Applicability is keyed by {@code EntityType<?>}, so no {@code instanceof} filter is
	 * needed: players, items and any other entity simply miss the map.
	 */
	private static void selectVariant(Entity entity, ServerLevel level) {
		if (entity.isLoadedFromDisk()) {
			return;
		}

		if (entity.hasAttached(VariantAttachments.VARIANT_TEXTURE)) {
			return;
		}

		VariantSnapshot snapshot = level.globalAttachments()
				.getAttachedOrElse(VariantAttachments.SERVER_VARIANT_SNAPSHOT, VariantSnapshot.EMPTY);

		List<VariantDefinition> candidates = snapshot.variantsFor(entity.getType());

		if (candidates.isEmpty()) {
			return;
		}

		entity.setAttached(VariantAttachments.VARIANT_TEXTURE, candidates.getFirst().texture());
	}
}