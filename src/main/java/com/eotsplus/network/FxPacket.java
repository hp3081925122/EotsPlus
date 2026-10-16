package com.eotsplus.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import com.eotsplus.EotsPlus;

public record FxPacket(int kind, double x, double y, double z, float a, float b, float c, float radius, float extra, int duration, int color) implements CustomPacketPayload {
    public static final Type<FxPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EotsPlus.MOD_ID, "fx"));
    public static final StreamCodec<FriendlyByteBuf, FxPacket> STREAM_CODEC = StreamCodec.ofMember(FxPacket::write, FxPacket::decode);

    private void write(FriendlyByteBuf buf) {
        encode(this, buf);
    }

    @Override
    public Type<FxPacket> type() {
        return TYPE;
    }
    public static void encode(FxPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.kind);
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

}
