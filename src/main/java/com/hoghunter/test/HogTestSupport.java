package com.hoghunter.test;

import com.hoghunter.entity.HogEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Controlled server-side fixtures. These players use vanilla ServerPlayer combat,
 * inventory, attachments and collision; outbound packets are intentionally dropped.
 * They do not simulate login negotiation, client input, network delivery, or a human.
 * Minecraft places the template one block above its structure block: its floor is
 * helper-relative Y=1, and standing entities belong at Y=2.
 */
final class HogTestSupport {
    private HogTestSupport() {}

    static ServerPlayer player(GameTestHelper helper, double x, double y, double z) {
        var level = helper.getLevel();
        var profile = new GameProfile(UUID.randomUUID(), "HHTest");
        var player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), connection, player,
                CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(Packet<?> packet) {}
            @Override public void send(Packet<?> packet, PacketSendListener listener) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        player.setNoGravity(true);
        position(helper, player, x, y, z);
        helper.assertTrue(level.noCollision(player), "player fixture overlaps solid geometry at " + player.position());
        // ServerPlayer has a short post-spawn invulnerability interval. Advance it
        // through its normal tick entry point before asserting any combat outcome.
        for (int tick = 0; tick < 65; tick++) player.tick();
        player.setHealth(player.getMaxHealth());
        level.addNewPlayer(player);
        return player;
    }

    static <T extends HogEntity> T hog(GameTestHelper helper, EntityType<T> type, double x, double y, double z) {
        T hog = helper.spawn(type, new Vec3(x, y, z));
        helper.assertTrue(helper.getLevel().noCollision(hog), "hog fixture overlaps solid geometry at " + hog.position());
        hog.setNoAi(true);
        hog.setNoGravity(true);
        hog.setPersistenceRequired();
        return hog;
    }

    static void position(GameTestHelper helper, net.minecraft.world.entity.Entity entity, double x, double y, double z) {
        Vec3 position = helper.absoluteVec(new Vec3(x, y, z));
        entity.setPos(position.x, position.y, position.z);
        entity.setDeltaMovement(Vec3.ZERO);
    }

    /**
     * Advance actual entity logic while excluding pathfinding and incidental drift.
     * World time is intentionally not advanced: world-time expiry gets separate live
     * tests or direct data-timer tests, never inferred from these controlled steps.
     */
    static void stationaryTicks(HogEntity hog, int count) {
        Vec3 anchor = hog.position();
        for (int tick = 0; tick < count; tick++) {
            hog.setPos(anchor.x, anchor.y, anchor.z);
            hog.setDeltaMovement(Vec3.ZERO);
            hog.tick();
        }
        hog.setPos(anchor.x, anchor.y, anchor.z);
        hog.setDeltaMovement(Vec3.ZERO);
    }
}
