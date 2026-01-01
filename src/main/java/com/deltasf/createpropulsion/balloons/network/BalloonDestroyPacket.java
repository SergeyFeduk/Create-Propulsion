package com.deltasf.createpropulsion.balloons.network;

import java.util.function.Supplier;

import com.deltasf.createpropulsion.balloons.registries.ClientBalloonRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class BalloonDestroyPacket extends BalloonPacket {
    public BalloonDestroyPacket(long shipId, int balloonId) { super(shipId, balloonId); }

    public static void encode(BalloonDestroyPacket pkt, FriendlyByteBuf buf) { pkt.writeHeader(buf); }
    public static BalloonDestroyPacket decode(FriendlyByteBuf buf) { 
        return new BalloonDestroyPacket(buf.readLong(), buf.readInt()); 
    }
    public static void handle(BalloonDestroyPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientBalloonRegistry.onDestroyPacket(pkt.shipId, pkt.balloonId);
        });
        ctx.get().setPacketHandled(true);
    }
}
