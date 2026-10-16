package com.eotsplus.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class EotsNetwork {
    private EotsNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(FxPacket.TYPE, FxPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> com.eotsplus.client.fx.FxManager.accept(packet)));
    }

    public static void sendNear(ServerLevel level, double x, double y, double z, double range, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, range, packet);
    }
}
