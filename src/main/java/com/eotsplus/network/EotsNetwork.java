package com.eotsplus.network;

import com.eotsplus.EotsPlus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class EotsNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(EotsPlus.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private EotsNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, FxPacket.class, FxPacket::encode, FxPacket::decode, FxPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendNear(ServerLevel level, double x, double y, double z, double range, Object packet) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, range, level.dimension())), packet);
    }
}
