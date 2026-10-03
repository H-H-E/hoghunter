package com.hoghunter.client;

import com.hoghunter.HogHunterMod;
import com.hoghunter.content.HogItems;
import com.hoghunter.core.HogHunterConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Scaled GUI coordinates, numeric values and words keep survival feedback legible. */
final class HogHud {
    private static final int TEXT = 0xFFE8E3D8;
    private static final int MUTED = 0xFFB2ABA0;
    private static final int SAFE = 0xFFAFCCA0;
    private static final int WARNING = 0xFFE3B976;
    private static final int DANGER = 0xFFFF8F89;
    private static final ResourceLocation VIGNETTE = HogHunterMod.id("textures/gui/hog_hunter_vignette.png");
    private static final ResourceLocation BLOOD = HogHunterMod.id("textures/gui/hog_hunter_blood_edges.png");

    private HogHud() {}

    private static boolean visible(Minecraft minecraft) {
        return minecraft.player != null && minecraft.level != null && minecraft.player.isAlive()
                && !minecraft.player.isSpectator() && !minecraft.options.hideGui && HogClientState.received();
    }

    static void renderStatus(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!visible(minecraft) || minecraft.screen != null || minecraft.getDebugOverlay().showDebugScreen()) return;
        Font font = minecraft.font;
        int width = Math.min(158, graphics.guiWidth() - 12);
        if (width < 100 || graphics.guiHeight() < 110) return;
        int injuryRows = (HogClientState.wounds() > 0 ? 1 : 0) + (HogClientState.fracture() > 0 ? 1 : 0);
        int lockRows = HogClientState.sprintLockTicks() > 0 ? 1 : 0;
        int height = 96 + (injuryRows + lockRows) * 12;
        int x = 6, y = 6;
        graphics.fill(x, y, x + width, y + height, 0xBC101318);
        graphics.fill(x, y, x + 2, y + height, heartColor());
        int left = x + 8, right = x + width - 7, inner = right - left;
        pair(graphics, font, tr("title"), tr("tier", HogClientState.unlockedTier()), left, right, y + 7, MUTED, MUTED);
        pair(graphics, font, tr("heart", HogClientState.heartRate()), tr(heartLabel()), left, right, y + 21, TEXT, heartColor());
        bar(graphics, left, y + 32, inner, (HogClientState.heartRate() - 45) / 135F, heartColor());
        pair(graphics, font, tr("noise"), Component.literal(Integer.toString(Math.round(HogClientState.noise()))),
                left, right, y + 41, MUTED, HogClientState.noise() >= 50 ? WARNING : TEXT);
        bar(graphics, left, y + 52, inner, HogClientState.noise() / 100F, HogClientState.noise() >= 50 ? WARNING : SAFE);
        boolean held = minecraft.player.isHolding(HogItems.FIELD_LANTERN.get());
        String lantern = HogClientState.blackoutTicks() > 0 ? "blackout" : HogClientState.oil() <= 0 ? "lantern_empty"
                : HogClientState.lanternLit() && held ? "lantern_on" : "lantern_off";
        Component fuel = tr("oil_value", Math.round(HogClientState.oil())).append(" ").append(tr(lantern));
        pair(graphics, font, tr("oil"), fuel, left, right, y + 61, MUTED, HogClientState.oil() <= 25 ? WARNING : TEXT);
        bar(graphics, left, y + 72, inner, HogClientState.oil() / 100F, HogClientState.oil() <= 25 ? WARNING : SAFE);
        pair(graphics, font, tr("ammo"), Component.literal(Integer.toString(HogClientState.ammo())),
                left, right, y + 81, MUTED, HogClientState.ammo() == 0 ? WARNING : TEXT);
        int rowY = y + 96;
        if (HogClientState.wounds() > 0) {
            line(graphics, font, tr("wounds", HogClientState.wounds()), left, rowY, inner, DANGER);
            rowY += 12;
        }
        if (HogClientState.fracture() > 0) {
            line(graphics, font, tr("fracture", HogClientState.fracture()), left, rowY, inner, DANGER);
            rowY += 12;
        }
        if (HogClientState.sprintLockTicks() > 0)
            line(graphics, font, tr("sprint_locked", (HogClientState.sprintLockTicks() + 19) / 20), left, rowY, inner, WARNING);
    }

    static void renderEffects(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!visible(minecraft) || minecraft.player.isCreative()) return;
        float intensity = HogHunterConfig.HORROR_VISUAL_INTENSITY.get().floatValue()
                * minecraft.options.screenEffectScale().get().floatValue();
        if (intensity <= 0) return;
        float stress = Mth.clamp((HogClientState.heartRate() - 110) / 70F, 0, 1);
        float sanity = HogHunterConfig.SANITY_EFFECTS_ENABLED.get()
                ? Mth.clamp((40 - HogClientState.sanity()) / 40F, 0, 1) : 0;
        overlay(graphics, VIGNETTE, intensity * Math.max(stress * 0.35F, sanity * 0.2F));
        overlay(graphics, BLOOD, intensity * HogClientState.wounds() * 0.08F);
        if (HogClientState.blackoutTicks() > 0) {
            int alpha = (int) (Math.min(1, HogClientState.blackoutTicks() / 10F) * 95 * intensity);
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24);
        }
    }

    private static void overlay(GuiGraphics graphics, ResourceLocation texture, float opacity) {
        if (opacity <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1, 1, 1, opacity);
        try {
            graphics.blit(texture, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0F, 0F, 256, 256, 256, 256);
        } finally {
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
    }

    private static void bar(GuiGraphics graphics, int x, int y, int width, float value, int color) {
        graphics.fill(x, y, x + width, y + 3, 0xFF393C40);
        int fill = Math.round(width * Mth.clamp(value, 0, 1));
        if (fill > 0) graphics.fill(x, y, x + fill, y + 3, color);
    }

    private static void pair(GuiGraphics graphics, Font font, Component label, Component value,
                             int left, int right, int y, int labelColor, int valueColor) {
        String valueText = font.plainSubstrByWidth(value.getString(), right - left);
        int valueWidth = font.width(valueText);
        line(graphics, font, label, left, y, Math.max(0, right - left - valueWidth - 6), labelColor);
        graphics.drawString(font, valueText, right - valueWidth, y, valueColor, false);
    }

    private static void line(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), width), x, y, color, false);
    }

    private static net.minecraft.network.chat.MutableComponent tr(String key, Object... args) {
        return Component.translatable("gui.hoghunter." + key, args);
    }

    private static int heartColor() {
        return HogClientState.heartRate() >= 140 ? DANGER : HogClientState.heartRate() >= 90 ? WARNING : SAFE;
    }

    private static String heartLabel() {
        return HogClientState.heartRate() >= 160 ? "panic" : HogClientState.heartRate() >= 120 ? "strained"
                : HogClientState.heartRate() >= 90 ? "alert" : "calm";
    }
}
