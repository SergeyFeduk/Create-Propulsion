package com.deltasf.createpropulsion.physics_assembler;

import com.deltasf.createpropulsion.physics_assembler.AssemblyGaugeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ResetGaugePacket {

    public ResetGaugePacket() {}

    public ResetGaugePacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.getItem() instanceof AssemblyGaugeItem) {
                AssemblyGaugeItem.resetPositions(stack, player);
            }
        });
        return true;
    }
}
