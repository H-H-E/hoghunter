package com.hoghunter.client.model;

import com.hoghunter.entity.HogEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Chunky quadrupeds with species-specific silhouettes and server-synchronized tells.
 * The committed skins are eight 64px illustration frames, not conventional unfolded
 * cube atlases. Each body surface samples their opaque color/marking patches; tusks,
 * eyes, armor and spores are separate native-material parts. No face samples empty
 * pixels and no model UV rectangle extends beyond the logical 64px frame.
 */
public final class HogModel<T extends HogEntity> extends EntityModel<T> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart tusks;
    private final ModelPart eyes;
    private final ModelPart plates;
    private final ModelPart spores;
    private final ModelPart[] legs;
    private final String species;
    private int frame;

    public HogModel(ModelPart root, String species) {
        this.root = root;
        this.species = species;
        body = root.getChild("body");
        head = root.getChild("head");
        snout = root.getChild("snout");
        tusks = root.getChild("tusks");
        eyes = root.getChild("eyes");
        plates = root.getChild("plates");
        spores = root.getChild("spores");
        legs = new ModelPart[]{body.getChild("leg_front_left"), body.getChild("leg_front_right"),
                body.getChild("leg_back_left"), body.getChild("leg_back_right")};
    }

    public static LayerDefinition createBodyLayer(String species) {
        boolean tall = species.equals("hook_hog") || species.equals("screecher_hog");
        boolean iron = species.equals("ironback_hog");
        boolean boss = species.equals("rootmother");
        boolean mire = species.equals("mire_hog");
        float bodyY = tall ? 10 : mire ? 14 : 12;
        float headY = tall ? 8 : mire ? 14 : 11;
        float width = iron ? 14 : species.equals("screecher_hog") ? 8 : 12;
        float headWidth = boss ? 10 : 8;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", cube(-width / 2, -4, -8, width, 8, 16), PartPose.offset(0, bodyY, 2));
        float legHeight = 24 - bodyY - 3;
        body.addOrReplaceChild("leg_front_left", cube(-1.5F, 0, -1.5F, 3, legHeight, 3), PartPose.offset(width / 2 - 2, 3, -5));
        body.addOrReplaceChild("leg_front_right", cube(-1.5F, 0, -1.5F, 3, legHeight, 3), PartPose.offset(-width / 2 + 2, 3, -5));
        body.addOrReplaceChild("leg_back_left", cube(-1.5F, 0, -1.5F, 3, legHeight, 3), PartPose.offset(width / 2 - 2, 3, 5));
        body.addOrReplaceChild("leg_back_right", cube(-1.5F, 0, -1.5F, 3, legHeight, 3), PartPose.offset(-width / 2 + 2, 3, 5));
        PartDefinition head = root.addOrReplaceChild("head", cube(-headWidth / 2, -4, -6, headWidth, 8, 8), PartPose.offset(0, headY, -8));
        head.addOrReplaceChild("ear_left", cube(-1, -3, -1, 3, 4, 1), PartPose.offsetAndRotation(3, -4, -1, 0, 0, -0.3F));
        head.addOrReplaceChild("ear_right", cube(-2, -3, -1, 3, 4, 1), PartPose.offsetAndRotation(-3, -4, -1, 0, 0, 0.3F));
        root.addOrReplaceChild("snout", cube(-3, 0, -9, 6, 4, 4), PartPose.offset(0, headY, -8));
        PartDefinition tusks = root.addOrReplaceChild("tusks", CubeListBuilder.create(), PartPose.offset(0, headY, -8));
        tusks.addOrReplaceChild("left", cube(-0.75F, -4, -0.75F, 1.5F, 5, 1.5F), PartPose.offsetAndRotation(3.5F, 3, -7, -0.2F, 0, 0.3F));
        tusks.addOrReplaceChild("right", cube(-0.75F, -4, -0.75F, 1.5F, 5, 1.5F), PartPose.offsetAndRotation(-3.5F, 3, -7, -0.2F, 0, -0.3F));
        if (species.equals("hook_hog")) {
            tusks.addOrReplaceChild("hook_left", cube(-0.6F, 0, -0.6F, 1.2F, 7, 1.2F), PartPose.offsetAndRotation(2, 1, -9, -0.2F, 0, -0.15F));
            tusks.addOrReplaceChild("hook_right", cube(-0.6F, 0, -0.6F, 1.2F, 7, 1.2F), PartPose.offsetAndRotation(-2, 1, -9, -0.2F, 0, 0.15F));
        }
        if (boss) {
            tusks.addOrReplaceChild("antler_left", cube(-1, -9, -1, 2, 9, 2), PartPose.offsetAndRotation(4, -3, 0, 0.15F, 0, 0.45F));
            tusks.addOrReplaceChild("antler_right", cube(-1, -9, -1, 2, 9, 2), PartPose.offsetAndRotation(-4, -3, 0, 0.15F, 0, -0.45F));
        }
        root.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(0, 0)
                .addBox(headWidth / 2 - 0.4F, -2.5F, -5, 0.6F, 1.2F, 1.5F)
                .addBox(-headWidth / 2 - 0.2F, -2.5F, -5, 0.6F, 1.2F, 1.5F), PartPose.offset(0, headY, -8));
        PartDefinition plates = root.addOrReplaceChild("plates", CubeListBuilder.create(), PartPose.offset(0, bodyY, 2));
        if (iron) {
            for (int i = 0; i < 3; i++) plates.addOrReplaceChild("plate_" + i,
                    cube(-7.5F, -5.5F, -2, 15, 2, 4), PartPose.offset(0, 0, -5 + i * 5));
        }
        PartDefinition spores = root.addOrReplaceChild("spores", CubeListBuilder.create(), PartPose.offset(0, bodyY, 2));
        if (species.equals("spore_hog")) {
            spores.addOrReplaceChild("sac_left", cube(-2, -4, -2, 4, 4, 4), PartPose.offset(3, -4, -3));
            spores.addOrReplaceChild("sac_right", cube(-2, -5, -2, 4, 5, 4), PartPose.offset(-3, -4, 0));
            spores.addOrReplaceChild("sac_back", cube(-2, -3, -2, 4, 3, 4), PartPose.offset(1, -4, 5));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder cube(float x, float y, float z, float dx, float dy, float dz) {
        return CubeListBuilder.create().texOffs(0, 0).addBox(x, y, z, dx, dy, dz);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        frame = Math.floorMod((int) ageInTicks / 2, 8);
        float stride = Math.min(1, limbSwingAmount) * (entity.isCharging() ? 1.5F : 0.9F);
        legs[0].xRot = Mth.cos(limbSwing * 0.6662F) * stride;
        legs[1].xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * stride;
        legs[2].xRot = legs[1].xRot;
        legs[3].xRot = legs[0].xRot;
        head.yRot = Mth.clamp(netHeadYaw, -55, 55) * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD + Mth.sin(ageInTicks * 0.12F) * 0.025F;
        if (entity.getAbilityState() == HogEntity.AbilityState.WINDUP) {
            body.xRot = 0.07F;
            head.xRot += species.equals("screecher_hog") ? -0.4F : 0.28F;
            head.y += 0.5F;
        } else if (entity.getAbilityState() == HogEntity.AbilityState.ACTIVE) {
            head.xRot += species.equals("screecher_hog") ? -0.7F : -0.12F;
        } else if (entity.getAbilityState() == HogEntity.AbilityState.RECOVERY) {
            head.xRot += 0.12F;
        }
        snout.copyFrom(head);
        tusks.copyFrom(head);
        eyes.copyFrom(head);
        plates.copyFrom(body);
        spores.copyFrom(body);
        if (entity.getAbilityState() == HogEntity.AbilityState.WINDUP) {
            spores.xScale = 1.12F;
            spores.yScale = 1.12F;
            spores.zScale = 1.12F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int light, int overlay, int color) {
        // These bounded rectangles are opaque in all eight committed species frames.
        VertexConsumer hide = new SkinPatchConsumer(vertices, frame, 26, 24, 21, 7);
        body.render(poses, hide, light, overlay, color);
        head.render(poses, hide, light, overlay, color);
        snout.render(poses, new SkinPatchConsumer(vertices, frame, 14, 9, 4, 2), light, overlay, color);
    }

    public void renderTusks(PoseStack poses, VertexConsumer vertices, int light, int overlay) {
        tusks.render(poses, vertices, light, overlay, 0xFFFFFFFF);
    }

    public void renderEyes(PoseStack poses, VertexConsumer vertices, int light) {
        eyes.render(poses, vertices, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }

    public void renderPlates(PoseStack poses, VertexConsumer vertices, int light, int overlay) {
        plates.render(poses, vertices, light, overlay, 0xFFFFFFFF);
    }

    public void renderSpores(PoseStack poses, VertexConsumer vertices, int light, int overlay) {
        spores.render(poses, vertices, light, overlay, 0xFFFFFFFF);
    }
}
