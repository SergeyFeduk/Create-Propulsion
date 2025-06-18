package com.deltasf.createpropulsion.network;

import java.util.Optional;

import com.deltasf.createpropulsion.CreatePropulsion;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/*public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(CreatePropulsion.ID, "main"), 
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int id = 0;
    private static int nextID() {
        return id++;
    }

    public static void register() {
        //CTS ship data
        INSTANCE.registerMessage(nextID(), 
            RequestShipDataPacket.class, 
            RequestShipDataPacket::encode,
            RequestShipDataPacket::decode,
            RequestShipDataPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        //STC ship data
        INSTANCE.registerMessage(nextID(), 
            SyncShipDataPacket.class, 
            SyncShipDataPacket::encode,
            SyncShipDataPacket::decode,
            SyncShipDataPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(ServerPlayer player, MSG message) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
*/