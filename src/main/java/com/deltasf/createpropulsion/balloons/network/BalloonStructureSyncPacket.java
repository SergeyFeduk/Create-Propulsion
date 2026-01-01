package com.deltasf.createpropulsion.balloons.network;

import java.util.function.Supplier;

import com.deltasf.createpropulsion.balloons.registries.ClientBalloonRegistry;
import com.deltasf.createpropulsion.balloons.serialization.BalloonCompressor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class BalloonStructureSyncPacket extends BalloonPacket {
    private final byte[] compressedVolume;
    private final int volumeSize;
    private final byte[] compressedHoles;
    private final int holesSize;

    public BalloonStructureSyncPacket(long shipId, int balloonId, byte[] compressedVolume, int volumeSize, byte[] compressedHoles, int holesSize) {
        super(shipId, balloonId);
        this.compressedVolume = compressedVolume;
        this.volumeSize = volumeSize;
        this.compressedHoles = compressedHoles;
        this.holesSize = holesSize;
    }

    public static void encode(BalloonStructureSyncPacket pkt, FriendlyByteBuf buf) {
        pkt.writeHeader(buf);
        buf.writeInt(pkt.volumeSize);
        buf.writeByteArray(pkt.compressedVolume);
        buf.writeInt(pkt.holesSize);
        buf.writeByteArray(pkt.compressedHoles);
    }

    public static BalloonStructureSyncPacket decode(FriendlyByteBuf buf) {
        long shipId = buf.readLong();
        int balloonId = buf.readInt();
        int vSize = buf.readInt();
        byte[] vData = buf.readByteArray();
        int hSize = buf.readInt();
        byte[] hData = buf.readByteArray();
        return new BalloonStructureSyncPacket(shipId, balloonId, vData, vSize, hData, hSize);
    }

    public static void handle(BalloonStructureSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            try {
                long[] volume = BalloonCompressor.decompress(pkt.compressedVolume, pkt.volumeSize);
                long[] holes = BalloonCompressor.decompress(pkt.compressedHoles, pkt.holesSize);
                ClientBalloonRegistry.onStructurePacket(pkt.shipId, pkt.balloonId, volume, holes);
            } catch (Exception e) { e.printStackTrace(); }
        });
        ctx.get().setPacketHandled(true);
    }
}