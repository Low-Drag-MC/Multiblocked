package com.lowdragmc.multiblocked.network;

import com.lowdragmc.lowdraglib.networking.IPacket;
import com.lowdragmc.lowdraglib.networking.Networking;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.network.s2c.SPacketCommand;
import com.lowdragmc.multiblocked.network.s2c.SPacketRemoveDisabledRendering;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;

/**
 * Author: KilaBash
 * Date: 2022/04/27
 * Description:
 */
public class MultiblockedNetworking {

    private static final Networking network = new Networking(new ResourceLocation(Multiblocked.MODID, "networking"), "0.0.1");

    public static void init() {
        network.registerS2C(SPacketRemoveDisabledRendering.class);
        network.registerS2C(SPacketCommand.class);
    }

    public static void sendToServer(IPacket packet) {
        network.sendToServer(packet);
    }

    public static void sendToAll(IPacket packet) {
        network.sendToAll(packet);
    }

    public static void sendToPlayer(IPacket packet, ServerPlayerEntity player) {
        network.sendToPlayer(packet, player);
    }

}
