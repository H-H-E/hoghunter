package com.hoghunter.client;

import com.hoghunter.client.model.HogModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class HogRenderer<T extends Entity> extends EntityRenderer<T> {
    private final HogModel<T> model;
    private final ResourceLocation texture;
    private final float scale;

    public HogRenderer(EntityRendererProvider.Context context, String id, float scale) {
        super(context);
        this.model = new HogModel<>(context.bakeLayer(HogClient.layer(id)));
        this.texture = ResourceLocation.fromNamespaceAndPath("hoghunter", "textures/entity/hog/" + id + ".png");
        this.scale = scale;
        this.shadowRadius = Math.max(0.25f, scale * 0.45f);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        float limbSwing = (entity.tickCount + partialTick) * 0.45f;
        model.setupAnim(entity, limbSwing, 0.65f, entity.tickCount + partialTick, entity.getYRot(), entity.getXRot());
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) { return texture; }
}
