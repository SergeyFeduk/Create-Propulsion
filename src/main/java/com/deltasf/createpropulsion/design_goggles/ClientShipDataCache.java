package com.deltasf.createpropulsion.design_goggles;

/*public class ClientShipDataCache {
    public record ShipData(double mass) {}

    private static final Cache<Long, ShipData> SHIP_DATA_CACHE = CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build();

    //Antispam literally valkyrien golem
    private static final Cache<Long, Boolean> PENDING_REQUESTS = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build();
    
    public static ShipData getShipData(long shipId) {
        ShipData data = SHIP_DATA_CACHE.getIfPresent(shipId);
        if (data != null) return data;

        if (PENDING_REQUESTS.getIfPresent(shipId) == null) {
            PENDING_REQUESTS.put(shipId, true);
            PacketHandler.sendToServer(new RequestShipDataPacket(shipId));
        }

        //We will get ship data soon
        return null;
    }

    public static void receiveShipData(long shipId, double mass) {
        ShipData data = new ShipData(mass);
        SHIP_DATA_CACHE.put(shipId, data);
        PENDING_REQUESTS.invalidate(shipId);
    }
}
*/