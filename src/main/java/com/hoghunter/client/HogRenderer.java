package com.hoghunter.client;

import com.hoghunter.client.model.HogModel;
import com.hoghunter.entity.HogEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Use the living renderer for body yaw, walk distance, hurt/death and invisibility. */
public final class HogRenderer<T extends HogEntity> extends MobRenderer<T, HogModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public HogRenderer(EntityRendererProvider.Context context, String id, float scale) {
        super(context, new HogModel<>(context.bakeLayer(HogClient.layer(id)), id), scale * 0.45F);
        texture = ResourceLocation.fromNamespaceAndPath("hoghunter", "textures/entity/hog/" + id + ".png");
        this.scale = scale;
        addLayer(new HogDetailsLayer<>(this));
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) { return texture; }
}
