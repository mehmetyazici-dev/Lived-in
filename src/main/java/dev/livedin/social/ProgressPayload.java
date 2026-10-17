package dev.livedin.social;

import dev.livedin.LivedIn;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Sent only to the viewer; other players' relationship maps never leave the server. */
public record ProgressPayload(int entityId, UUID npc, int count) implements CustomPacketPayload {
    public static final Type<ProgressPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LivedIn.MOD_ID, "relationship"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ProgressPayload decode(RegistryFriendlyByteBuf buf) { return new ProgressPayload(buf.readVarInt(), buf.readUUID(), buf.readVarInt()); }
        @Override public void encode(RegistryFriendlyByteBuf buf, ProgressPayload data) { buf.writeVarInt(data.entityId); buf.writeUUID(data.npc); buf.writeVarInt(data.count); }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
