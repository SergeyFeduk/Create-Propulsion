package com.deltasf.createpropulsion.network;

/*
public class RequestShipDataPacket {
    private final long shipId;

    public RequestShipDataPacket(long shipId) {
        this.shipId = shipId;
    }

    public static void encode(RequestShipDataPacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.shipId);
    }

    public static RequestShipDataPacket decode(FriendlyByteBuf buf) {
        return new RequestShipDataPacket(buf.readLong());
    }

    public static void handle(RequestShipDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ServerShip ship = (ServerShip)VSGameUtilsKt.getShipWorldNullable(player.level()).getLoadedShips().getById(packet.shipId);
            
            if (ship != null) {
                double mass = ship.getInertiaData().getMass();

                SyncShipDataPacket response = new SyncShipDataPacket(packet.shipId, mass);
                PacketHandler.sendToPlayer(player, response);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
*/