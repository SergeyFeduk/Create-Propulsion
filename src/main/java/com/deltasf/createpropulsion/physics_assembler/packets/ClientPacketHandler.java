package com.deltasf.createpropulsion.physics_assembler.packets;

import com.deltasf.createpropulsion.physics_assembler.PhysicsAssemblerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class ClientPacketHandler {
    public static void handleAssemblyFailedClient(AssemblyFailedPacket pkt) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level != null) {
            if (level.getBlockEntity(pkt.getPos()) instanceof PhysicsAssemblerBlockEntity assembler) {
                assembler.spawnEffect(ParticleTypes.SMOKE, 0.2f, 12);
            }
        }
    }

    public static void handleGaugeInsertionErrorClient(GaugeInsertionErrorPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui instanceof ForgeGui gui) {
            gui.setOverlayMessage(pkt.getMessage(), false);
        }
    }

}
