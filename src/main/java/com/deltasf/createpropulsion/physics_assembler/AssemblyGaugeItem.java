package com.deltasf.createpropulsion.physics_assembler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.valkyrienskies.core.impl.shadow.nb;

import com.deltasf.createpropulsion.network.PropulsionPackets;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

public class AssemblyGaugeItem extends Item {

    private static final String NBT_KEY_POS1 = "posA";
    private static final String NBT_KEY_POS2 = "posB";
    public static final int MAX_SIZE = 32;

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
        BlockPos targetedPos = getTargetedPosition(context.getClickedPos(), context.getClickedFace());
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
            if (Math.abs(posA.getX() - targetedPos.getX()) > MAX_SIZE
                || Math.abs(posA.getY() - targetedPos.getY()) > MAX_SIZE
                || Math.abs(posA.getZ() - targetedPos.getZ()) > MAX_SIZE) {
                return InteractionResult.FAIL;
            }

            nbt.put(NBT_KEY_POS2, NbtUtils.writeBlockPos(targetedPos));
            AABB selection = new AABB(posA).minmax(new AABB(targetedPos));
            PropulsionPackets.sendToAll(new AssemblyGaugeUsedPacket(selection));
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

    public static BlockPos getTargetedPosition(BlockPos pos, net.minecraft.core.Direction face) {
        return pos.relative(face);
    }

    @SuppressWarnings("null")
    public static boolean handleLeftClick(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AssemblyGaugeItem)) {
            return false;
        }
    
        BlockPos posA = getPosA(stack);
        BlockPos posB = getPosB(stack);
        boolean shouldReset = false;
    
        if (posA != null && posB == null) {
            shouldReset = true;
        }
        else if (posA != null && posB != null) {
            AABB selectionBox = new AABB(posA).minmax(new AABB(posB));
            Vec3 eyePos = player.getEyePosition();
            Attribute reachAttr = ForgeMod.BLOCK_REACH.get();
            if (reachAttr == null) {
                return false;
            }
            double reach = player.getAttribute(reachAttr).getValue();
            Vec3 lookVec = player.getViewVector(1.0F);
            Vec3 endPos = eyePos.add(lookVec.scale(reach));
    
            if (selectionBox.inflate(0.05).clip(eyePos, endPos).isPresent()) {
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
