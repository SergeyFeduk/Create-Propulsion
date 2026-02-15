package com.deltasf.createpropulsion.physics_assembler.packets;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

public class GaugeInsertionErrorPacket {

    private final Component message;

    public GaugeInsertionErrorPacket(Component message) {
        this.message = message;
    }

    public GaugeInsertionErrorPacket(FriendlyByteBuf buf) {
        this.message = buf.readComponent();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeComponent(message);
    }

    public Component getMessage() {
        return message;
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        if (FMLEnvironment.dist.isClient()) ClientPacketHandler.handleGaugeInsertionErrorClient(this);
    }
}
