package com.hoghunter.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** A readable, deliberately chunky quadruped silhouette shared by the hog roster. */
public final class HogModel<T extends Entity> extends EntityModel<T> {
    public static final float BODY_SCALE = 1.0f;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart[] legs;

    public HogModel(ModelPart root) {
        body = root.getChild("body");
        head = root.getChild("head");
        snout = head.getChild("snout");
        legs = new ModelPart[]{body.getChild("leg_front_left"), body.getChild("leg_front_right"), body.getChild("leg_back_left"), body.getChild("leg_back_right")};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-6, -5, -10, 12, 10, 20), PartPose.offset(0, 12, 0));
        body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 30).addBox(-5, -5, -5, 10, 10, 8), PartPose.offset(0, -2, -9));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -5, -6, 10, 10, 10)
                .texOffs(0, 20).addBox(-6, -3, -7, 12, 6, 5), PartPose.offset(0, 7, -13));
        head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(40, 48).addBox(-4, -2, -3, 8, 5, 5), PartPose.offset(0, 1, -6));
        body.addOrReplaceChild("leg_front_left", leg(36, 0), PartPose.offset(4, 4, -6));
        body.addOrReplaceChild("leg_front_right", leg(36, 14), PartPose.offset(-4, 4, -6));
        body.addOrReplaceChild("leg_back_left", leg(52, 0), PartPose.offset(4, 4, 6));
        body.addOrReplaceChild("leg_back_right", leg(52, 14), PartPose.offset(-4, 4, 6));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder leg(int u, int v) {
        return CubeListBuilder.create().texOffs(u, v).addBox(-2, 0, -2, 4, 10, 4, new CubeDeformation(0.05f));
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        head.xRot = headPitch * ((float) Math.PI / 180F) + ((float) Math.sin(ageInTicks * 0.16F) * 0.035F);
        snout.xRot = head.xRot * 0.25F;
        legs[0].xRot = (float) Math.cos(limbSwing * 0.6662F) * limbSwingAmount;
        legs[1].xRot = (float) Math.cos(limbSwing * 0.6662F + Math.PI) * limbSwingAmount;
        legs[2].xRot = legs[1].xRot;
        legs[3].xRot = legs[0].xRot;
        body.xRot = 0;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int packedColor) {
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, packedColor);
        head.render(poseStack, vertexConsumer, packedLight, packedOverlay, packedColor);
    }
}
