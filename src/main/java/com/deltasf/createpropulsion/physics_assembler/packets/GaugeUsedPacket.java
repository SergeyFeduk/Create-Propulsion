package com.deltasf.createpropulsion.physics_assembler.packets;

import com.deltasf.createpropulsion.physics_assembler.AssemblyGaugeOverlayRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GaugeUsedPacket {
    private final AABB selection;

    public GaugeUsedPacket(AABB selection) {
        this.selection = selection;
    }

    public GaugeUsedPacket(FriendlyByteBuf buf) {
        this.selection = new AABB(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                                  buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(selection.minX);
        buf.writeDouble(selection.minY);
        buf.writeDouble(selection.minZ);
        buf.writeDouble(selection.maxX);
        buf.writeDouble(selection.maxY);
        buf.writeDouble(selection.maxZ);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        if (FMLEnvironment.dist.isClient()) AssemblyGaugeOverlayRenderer.triggerFlash(this.selection);
        context.get().setPacketHandled(true);
    }
}
