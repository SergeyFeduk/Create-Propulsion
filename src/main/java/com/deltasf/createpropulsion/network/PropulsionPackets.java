package com.deltasf.createpropulsion.network;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.physics_assembler.packets.AssemblyFailedPacket;
import com.deltasf.createpropulsion.physics_assembler.packets.GaugeInsertionErrorPacket;
import com.deltasf.createpropulsion.physics_assembler.packets.GaugeUsedPacket;
import com.deltasf.createpropulsion.physics_assembler.packets.ResetGaugePacket;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

//Better than PacketHandler
public class PropulsionPackets {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(CreatePropulsion.ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() { return packetId++; }

    public static void register() {
        INSTANCE.messageBuilder(SyncThrusterFuelsPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SyncThrusterFuelsPacket::encode)
            .decoder(SyncThrusterFuelsPacket::decode)
            .consumerMainThread(SyncThrusterFuelsPacket::handle)
            .add();
        
        INSTANCE.messageBuilder(GaugeUsedPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(GaugeUsedPacket::encode)
            .decoder(GaugeUsedPacket::new)
            .consumerMainThread(GaugeUsedPacket::handle)
            .add();

        INSTANCE.messageBuilder(ResetGaugePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .encoder(ResetGaugePacket::encode)
            .decoder(ResetGaugePacket::new)
            .consumerMainThread(ResetGaugePacket::handle)
            .add();
        
        INSTANCE.messageBuilder(GaugeInsertionErrorPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(GaugeInsertionErrorPacket::encode)
            .decoder(GaugeInsertionErrorPacket::new)
            .consumerMainThread(GaugeInsertionErrorPacket::handle)
            .add();

        INSTANCE.messageBuilder(AssemblyFailedPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(AssemblyFailedPacket::encode)
            .decoder(AssemblyFailedPacket::new)
            .consumerMainThread(AssemblyFailedPacket::handle)
            .add();
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAll(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    public static <MSG> void sendToTracking(MSG message, LevelChunk chunk) {
        INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), message);
    }


    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
