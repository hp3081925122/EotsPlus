package com.eotsplus.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FxPacket(int type, double x, double y, double z, float a, float b, float c, float radius, float extra, int duration, int color) {
    public static void encode(FxPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.type);
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeFloat(p.a);
        buf.writeFloat(p.b);
        buf.writeFloat(p.c);
        buf.writeFloat(p.radius);
        buf.writeFloat(p.extra);
        buf.writeVarInt(p.duration);
        buf.writeInt(p.color);
    }

    public static FxPacket decode(FriendlyByteBuf buf) {
        return new FxPacket(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readInt());
    }

    public static void handle(FxPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.eotsplus.client.fx.FxManager.accept(p)));
        ctx.get().setPacketHandled(true);
    }
}
