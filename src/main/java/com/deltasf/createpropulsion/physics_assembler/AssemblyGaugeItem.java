package com.deltasf.createpropulsion.physics_assembler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.network.PropulsionPackets;
import com.deltasf.createpropulsion.physics_assembler.packets.GaugeUsedPacket;
import com.deltasf.createpropulsion.physics_assembler.packets.ResetGaugePacket;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class AssemblyGaugeItem extends Item {

    private static final String NBT_KEY_POS1 = "posA";
    private static final String NBT_KEY_POS2 = "posB";

    public AssemblyGaugeItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean canAttackBlock(@Nonnull BlockState pState, @Nonnull Level pLevel, @Nonnull BlockPos pPos, @Nonnull Player pPlayer) {
        return false;
    }

    @Override
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos targetedPos = AssemblyUtility.getTargetedPosition(context.getClickedPos(), context.getClickedFace());
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player == null) {
            return InteractionResult.PASS;
        }

        CompoundTag nbt = stack.getOrCreateTag();
        boolean hasPosA = nbt.contains(NBT_KEY_POS1);
        boolean hasPosB = nbt.contains(NBT_KEY_POS2);

        if (player.isShiftKeyDown()) {
            if (hasPosA && !hasPosB) {
                resetPositions(stack, null);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        if (hasPosA && hasPosB) {
            resetPositions(stack, null);
        }

        if (!nbt.contains(NBT_KEY_POS1)) {
            nbt.put(NBT_KEY_POS1, NbtUtils.writeBlockPos(targetedPos));
        } else {
            BlockPos posA = NbtUtils.readBlockPos(nbt.getCompound(NBT_KEY_POS1));
            if (Math.abs(posA.getX() - targetedPos.getX()) > AssemblyUtility.MAX_ASSEMBLY_SIZE
                || Math.abs(posA.getY() - targetedPos.getY()) > AssemblyUtility.MAX_ASSEMBLY_SIZE
                || Math.abs(posA.getZ() - targetedPos.getZ()) > AssemblyUtility.MAX_ASSEMBLY_SIZE) {
                return InteractionResult.FAIL;
            }

            nbt.put(NBT_KEY_POS2, NbtUtils.writeBlockPos(targetedPos));
            AABB selection = AssemblyUtility.fromBlockVolumes(posA, targetedPos);
            PropulsionPackets.sendToAll(new GaugeUsedPacket(selection));
        }

        return InteractionResult.CONSUME;
    }

    @Nullable
    public static BlockPos getPosA(ItemStack stack) {
        return getPos(stack, NBT_KEY_POS1);
    }

    @Nullable
    public static BlockPos getPosB(ItemStack stack) {
        return getPos(stack, NBT_KEY_POS2);
    }

    @Nullable
    private static BlockPos getPos(ItemStack stack, String key) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains(key)) {
            return null;
        }
        return NbtUtils.readBlockPos(nbt.getCompound(key));
    }

    public static void resetPositions(ItemStack stack, @Nullable Player player) {
        if (!(stack.getItem() instanceof AssemblyGaugeItem) || !stack.hasTag()) {
            return;
        }
        CompoundTag nbt = stack.getTag();
        if (nbt == null) return;
        nbt.remove(NBT_KEY_POS1);
        nbt.remove(NBT_KEY_POS2);
    }

    public static boolean handleLeftClick(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!AssemblyUtility.isAssemblyGauge(stack)) {
            return false;
        }
    
        BlockPos posA = getPosA(stack);
        BlockPos posB = getPosB(stack);
        boolean shouldReset = false;
    
        if (posA != null && posB == null) {
            shouldReset = true;
        }
        else if (posA != null && posB != null) {
            AABB selectionBox = AssemblyUtility.fromBlockVolumes(posA, posB);
            if (AssemblyUtility.isPlayerLookingAtAABB(player, selectionBox, 1.0f, 0.0, 0.05)) {
                shouldReset = true;
            }
        }
    
        if (shouldReset) {
            PropulsionPackets.sendToServer(new ResetGaugePacket());
            return true;
        }
    
        return false;
    }
}