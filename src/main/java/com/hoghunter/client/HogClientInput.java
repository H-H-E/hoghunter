package com.hoghunter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Mirrors the server's sprint lock before local movement prediction runs. */
final class HogClientInput {
    private static LocalPlayer tickingPlayer;
    private static boolean restoreSprintKey;
    private static boolean sprintKeyWasDown;

    private HogClientInput() {}

    static void beforePlayerTick(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player != minecraft.player) return;
        reset();
        if (minecraft.isPaused() || !player.isAlive() || player.isCreative() || player.isSpectator()
                || !HogClientState.received()
                || (HogClientState.sprintLockTicks() <= 0 && HogClientState.fracture() < 2)) return;

        LocalPlayer local = minecraft.player;
        if (local == null || local.input == null) return;
        local.setSprinting(false);
        // Player.aiStep updates this cached speed only AFTER travel. Refresh it now
        // so the first locked tick cannot retain the previous tick's sprint speed.
        local.setSpeed((float) local.getAttributeValue(Attributes.MOVEMENT_SPEED));

        // Exact 1.21.1 ordering: LocalPlayer.aiStep samples the PREVIOUS forward
        // impulse for its double-tap latch, then input.tick reads the real controls,
        // then sprint conditions run, then super.aiStep performs travel. A true
        // previous impulse suppresses only the double-tap transition. The normal
        // input refresh restores the player's actual impulse before movement;
        // no forward/strafe magnitude is reduced, including while swimming.
        local.input.forwardImpulse = 1.0F;
        sprintKeyWasDown = minecraft.options.keySprint.isDown();
        setSprintKeyDown(false);
        tickingPlayer = local;
        restoreSprintKey = true;
    }

    static void afterPlayerTick(Player player) {
        // PlayerTick.Post runs before LocalPlayer.sendPosition, so the outgoing
        // sprint state remains false while the actual key state is restored.
        if (player == tickingPlayer) reset();
    }

    static void reset() {
        if (restoreSprintKey) setSprintKeyDown(sprintKeyWasDown);
        restoreSprintKey = false;
        tickingPlayer = null;
    }

    private static void setSprintKeyDown(boolean down) {
        var options = Minecraft.getInstance().options;
        // ToggleKeyMapping ignores false and toggles on true. Use its hold-mode
        // setter for this brief state write, restoring the preference immediately.
        // In 1.21.1 this option's value-change callback is empty: no settings save
        // occurs, and neither another tick nor rendering sees a changed mode.
        boolean toggle = options.toggleSprint().get();
        if (toggle) options.toggleSprint().set(false);
        try {
            options.keySprint.setDown(down);
        } finally {
            if (toggle) options.toggleSprint().set(true);
        }
    }
}
