package com.hoghunter.client.model;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Samples one opaque patch in one 64x64 frame of a 64x512 authored skin strip. */
final class SkinPatchConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float minU;
    private final float minV;
    private final float spanU;
    private final float spanV;

    SkinPatchConsumer(VertexConsumer delegate, int frame, int x, int y, int width, int height) {
        this.delegate = delegate;
        minU = (x + 0.5F) / 64;
        minV = (frame * 64 + y + 0.5F) / 512;
        spanU = (width - 1) / 64F;
        spanV = (height - 1) / 512F;
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) { delegate.addVertex(x, y, z); return this; }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) { delegate.setColor(r, g, b, a); return this; }
    @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(minU + u * spanU, minV + v * spanV); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
}
