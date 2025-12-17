package com.deltasf.createpropulsion.physics_assembler.packets;

import com.deltasf.createpropulsion.physics_assembler.PhysicsAssemblerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AssemblyFailedPacket {
    private final BlockPos pos;

    public AssemblyFailedPacket(BlockPos pos) { this.pos = pos; }
    public AssemblyFailedPacket(FriendlyByteBuf buf) { this.pos = buf.readBlockPos();}

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(context.get()));
        });
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient(NetworkEvent.Context context) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level != null) {
            if (level.getBlockEntity(pos) instanceof PhysicsAssemblerBlockEntity assembler) {
                assembler.spawnEffect(ParticleTypes.SMOKE, 0.2f, 12);
            }
        }
    }
}