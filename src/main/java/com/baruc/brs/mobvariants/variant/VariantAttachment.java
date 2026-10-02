package com.baruc.brs.mobvariants.variant;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class VariantAttachment {
    public static final AttachmentType<ResourceLocation> VARIANT_ID = AttachmentRegistry
            .createPersistentAndCopyOnDeath(ResourceLocation.class)
            .synced()
            .build();

    private VariantAttachment() {
    }

    public static void register() {
        // Static initializer triggers registration
    }

    public static void setVariant(Entity entity, ResourceLocation variantId) {
        entity.setData(VARIANT_ID, variantId);
    }

    public static ResourceLocation getVariant(Entity entity) {
        return entity.getData(VARIANT_ID);
    }

    public static boolean hasVariant(Entity entity) {
        return entity.getData(VARIANT_ID) != null;
    }

    public static void clearVariant(Entity entity) {
        entity.setData(VARIANT_ID, null);
    }
}