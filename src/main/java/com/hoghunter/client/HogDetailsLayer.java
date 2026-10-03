package com.hoghunter.client;

import com.hoghunter.client.model.HogModel;
import com.hoghunter.entity.HogEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Distinct physical tusks, eyes, iron plates and spore sacs, using native materials. */
final class HogDetailsLayer<T extends HogEntity> extends RenderLayer<T, HogModel<T>> {
    private static final ResourceLocation BONE = texture("bone_block_side");
    private static final ResourceLocation IRON = texture("iron_block");
    private static final ResourceLocation SPORE = texture("slime_block");
    private static final ResourceLocation EYES = texture("red_concrete");

    HogDetailsLayer(RenderLayerParent<T, HogModel<T>> parent) { super(parent); }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.withDefaultNamespace("textures/block/" + name + ".png");
    }

    @Override
    public void render(PoseStack poses, MultiBufferSource buffers, int light, T entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0);
        HogModel<T> model = getParentModel();
        model.renderTusks(poses, buffers.getBuffer(RenderType.entityCutoutNoCull(BONE)), light, overlay);
        model.renderPlates(poses, buffers.getBuffer(RenderType.entityCutoutNoCull(IRON)), light, overlay);
        model.renderSpores(poses, buffers.getBuffer(RenderType.entityCutoutNoCull(SPORE)), light, overlay);
        model.renderEyes(poses, buffers.getBuffer(RenderType.eyes(EYES)), 0xF000F0);
    }
}
