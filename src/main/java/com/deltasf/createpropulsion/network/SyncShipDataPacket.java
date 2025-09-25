package com.deltasf.createpropulsion.network;

/*
public class SyncShipDataPacket {
    private final long shipId;
    private final double mass;

    public SyncShipDataPacket(long shipId, double mass) {
        this.shipId = shipId;
        this.mass = mass;
    }

    public static void encode(SyncShipDataPacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.shipId);
        buf.writeDouble(packet.mass);
    }

    public static SyncShipDataPacket decode(FriendlyByteBuf buf) {
        return new SyncShipDataPacket(buf.readLong(), buf.readDouble());
    }

    public static void handle(SyncShipDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> {
                //ClientShipDataCache.receiveShipData(packet.shipId, packet.mass);
                return null;
            });
        });

        ctx.get().setPacketHandled(true);
    }
}
*/