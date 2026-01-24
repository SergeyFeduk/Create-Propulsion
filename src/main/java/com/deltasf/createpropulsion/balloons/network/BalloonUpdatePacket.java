package com.deltasf.createpropulsion.balloons.network;

import java.util.function.Supplier;

import com.deltasf.createpropulsion.balloons.registries.ClientBalloonRegistry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class BalloonUpdatePacket extends BalloonPacket {
    private final float hotAir;

    public BalloonUpdatePacket(long shipId, int balloonId, float hotAir) { super(shipId, balloonId); this.hotAir = hotAir; }

    public static void encode(BalloonUpdatePacket pkt, FriendlyByteBuf buf) { 
        pkt.writeHeader(buf); 
        buf.writeFloat(pkt.hotAir);
    }

    public static BalloonUpdatePacket decode(FriendlyByteBuf buf) { 
        return new BalloonUpdatePacket(buf.readLong(), buf.readInt(), buf.readFloat()); 
    }
    
    public static void handle(BalloonUpdatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientBalloonRegistry.onUpdatePacket(pkt.shipId, pkt.balloonId, pkt.hotAir);
        });
        ctx.get().setPacketHandled(true);
    }
}
