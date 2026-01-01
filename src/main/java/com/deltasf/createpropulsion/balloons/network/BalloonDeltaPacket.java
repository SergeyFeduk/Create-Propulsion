package com.deltasf.createpropulsion.balloons.network;

import java.util.function.Supplier;

import com.deltasf.createpropulsion.balloons.registries.ClientBalloonRegistry;
import com.deltasf.createpropulsion.balloons.serialization.BalloonCompressor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class BalloonDeltaPacket extends BalloonPacket {
    // Stores 4 compressed arrays
    private final byte[] addedB, removedB, addedH, removedH;
    private final int sAB, sRB, sAH, sRH; // Sizes

    public BalloonDeltaPacket(long shipId, int balloonId, 
                              byte[] addedB, int sAB, byte[] removedB, int sRB,
                              byte[] addedH, int sAH, byte[] removedH, int sRH) {
        super(shipId, balloonId);
        this.addedB = addedB; this.sAB = sAB;
        this.removedB = removedB; this.sRB = sRB;
        this.addedH = addedH; this.sAH = sAH;
        this.removedH = removedH; this.sRH = sRH;
    }

    public static void encode(BalloonDeltaPacket pkt, FriendlyByteBuf buf) {
        pkt.writeHeader(buf);
        writeSection(buf, pkt.addedB, pkt.sAB);
        writeSection(buf, pkt.removedB, pkt.sRB);
        writeSection(buf, pkt.addedH, pkt.sAH);
        writeSection(buf, pkt.removedH, pkt.sRH);
    }
    
    private static void writeSection(FriendlyByteBuf buf, byte[] data, int size) {
        buf.writeInt(size);
        if (size > 0) buf.writeByteArray(data);
    }

    public static BalloonDeltaPacket decode(FriendlyByteBuf buf) {
        long shipId = buf.readLong();
        int balloonId = buf.readInt();
        var ab = readSection(buf); var rb = readSection(buf);
        var ah = readSection(buf); var rh = readSection(buf);
        return new BalloonDeltaPacket(shipId, balloonId, ab.data, ab.size, rb.data, rb.size, ah.data, ah.size, rh.data, rh.size);
    }
    
    private record Section(byte[] data, int size) {}
    private static Section readSection(FriendlyByteBuf buf) {
        int size = buf.readInt();
        byte[] data = size > 0 ? buf.readByteArray() : new byte[0];
        return new Section(data, size);
    }

    public static void handle(BalloonDeltaPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            try {
                long[] ab = BalloonCompressor.decompress(pkt.addedB, pkt.sAB);
                long[] rb = BalloonCompressor.decompress(pkt.removedB, pkt.sRB);
                long[] ah = BalloonCompressor.decompress(pkt.addedH, pkt.sAH);
                long[] rh = BalloonCompressor.decompress(pkt.removedH, pkt.sRH);
                ClientBalloonRegistry.onDeltaPacket(pkt.shipId, pkt.balloonId, ab, rb, ah, rh);
            } catch (Exception e) { e.printStackTrace(); }
        });
        ctx.get().setPacketHandled(true);
    }
}
