package com.deltasf.createpropulsion.balloons.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.deltasf.createpropulsion.balloons.registries.ClientBalloonRegistry;
import com.deltasf.createpropulsion.balloons.serialization.BalloonCompressor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class BalloonDeltaPacket extends BalloonPacket {
    private final byte[] addedB, removedB, addedH, removedH;
    private final int sAB, sRB, sAH, sRH; // Sizes
    private final UUID[] addedHais, removedHais;
    //TODO: Just send an aabb

    public BalloonDeltaPacket(long shipId, int balloonId, 
                              byte[] addedB, int sAB, byte[] removedB, int sRB,
                              byte[] addedH, int sAH, byte[] removedH, int sRH,
                              UUID[] addedHais, UUID[] removedHais) {
        super(shipId, balloonId);
        this.addedB = addedB; this.sAB = sAB;
        this.removedB = removedB; this.sRB = sRB;
        this.addedH = addedH; this.sAH = sAH;
        this.removedH = removedH; this.sRH = sRH;
        this.addedHais = addedHais;
        this.removedHais = removedHais;
    }

    public static void encode(BalloonDeltaPacket pkt, FriendlyByteBuf buf) {
        pkt.writeHeader(buf);
        writeSection(buf, pkt.addedB, pkt.sAB);
        writeSection(buf, pkt.removedB, pkt.sRB);
        writeSection(buf, pkt.addedH, pkt.sAH);
        writeSection(buf, pkt.removedH, pkt.sRH);
        writeUUIDs(buf, pkt.addedHais);
        writeUUIDs(buf, pkt.removedHais);
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
        var aHais = readUUIDs(buf); var rHais = readUUIDs(buf);
        return new BalloonDeltaPacket(shipId, balloonId, ab.data, ab.size, rb.data, rb.size, ah.data, ah.size, rh.data, rh.size, aHais, rHais);
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
                ClientBalloonRegistry.onDeltaPacket(pkt.shipId, pkt.balloonId, ab, rb, ah, rh, pkt.addedHais, pkt.removedHais);
            } catch (Exception e) { e.printStackTrace(); }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void writeUUIDs(FriendlyByteBuf buf, UUID[] uuids) {
        if (uuids == null) {
            buf.writeInt(0);
        } else {
            buf.writeInt(uuids.length);
            for(UUID id : uuids) buf.writeUUID(id);
        }
    }

    private static UUID[] readUUIDs(FriendlyByteBuf buf) {
        int count = buf.readInt();
        UUID[] uuids = new UUID[count];
        for(int i=0; i<count; i++) uuids[i] = buf.readUUID();
        return uuids;
    }
}
