package com.baruc.brs.mobvariants.attachment;

import com.baruc.brs.mobvariants.MobVariantsBRS;
import com.baruc.brs.mobvariants.variant.VariantSnapshot;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.resources.Identifier;

/**
 * The only attachment declarations in the mod. Both types live in the common source set so that
 * client and server register the same identifiers; the configuration-phase attachment sync
 * handshake requires the synced type to exist on both logical sides.
 */
public final class VariantAttachments {
	private VariantAttachments() {
	}

	/**
	 * The texture {@link Identifier} the server has already resolved for one entity.
	 *
	 * <p>This is resolved render state, not variant identity: nothing about it needs to be looked
	 * up on the client. It is persisted into entity NBT and synchronised to tracking players, so a
	 * disk-restored or late-tracked entity keeps the value it was given at spawn.
	 *
	 * <p>No {@code initializer()}: Fabric's attachment storage is a lazily created map, so an
	 * entity without a variant allocates nothing. No {@code copyOnDeath()}: a variant does not
	 * survive zombie to drowned conversion.
	 *
	 * <p>{@code Identifier.CODEC} and {@code Identifier.STREAM_CODEC} are distinct objects because
	 * persistence and synchronisation take different codec types.
	 */
	public static final AttachmentType<Identifier> VARIANT_TEXTURE = AttachmentRegistry.create(
			MobVariantsBRS.id("variant_texture"),
			builder -> builder
					.persistent(Identifier.CODEC)
					.syncWith(Identifier.STREAM_CODEC, AttachmentSyncPredicate.all()));

	/**
	 * The current definition snapshot, held by the running server's {@code GlobalAttachments} so
	 * its lifetime is bound to the server object.
	 *
	 * <p>Neither persisted nor synchronised: it is derived state rebuilt from the current datapack
	 * set on every reload, and the client never needs it.
	 */
	public static final AttachmentType<VariantSnapshot> SERVER_VARIANT_SNAPSHOT =
			AttachmentRegistry.create(MobVariantsBRS.id("server_variant_snapshot"));
}