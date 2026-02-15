package com.deltasf.createpropulsion.physics_assembler.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class AssemblyFailedPacket {
    private final BlockPos pos;

    public AssemblyFailedPacket(BlockPos pos) { this.pos = pos; }
    public AssemblyFailedPacket(FriendlyByteBuf buf) { this.pos = buf.readBlockPos();}

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean handle(Supplier<NetworkEvent.Context> context) {
        if (FMLEnvironment.dist.isClient()) ClientPacketHandler.handleAssemblyFailedClient(this);
        return true;
    }
}