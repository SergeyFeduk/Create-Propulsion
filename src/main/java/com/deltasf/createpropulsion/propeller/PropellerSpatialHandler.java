package com.deltasf.createpropulsion.propeller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.joml.primitives.AABBi;

import com.deltasf.createpropulsion.propeller.blades.PropellerBladeItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

//Handles hard and soft obstruction & fluid checks for propeller
public class PropellerSpatialHandler extends BlockEntityBehaviour {
    public static final BehaviourType<PropellerSpatialHandler> TYPE = new BehaviourType<>();

    private static final AABBi HARD_OBSTRUCTION_REGION = new AABBi(-1, -1, 0, 1, 1, 0);

    //Hard obstruction
    private int scanIndex = 0;
    private final Set<BlockPos> obstructedBlocks = new HashSet<>();
    private static final List<BlockPos> SCAN_POSITIONS;
    static {
        SCAN_POSITIONS = StreamSupport.stream(
            BlockPos.betweenClosed(HARD_OBSTRUCTION_REGION.minX, HARD_OBSTRUCTION_REGION.minY, HARD_OBSTRUCTION_REGION.minZ, HARD_OBSTRUCTION_REGION.maxX, HARD_OBSTRUCTION_REGION.maxY, HARD_OBSTRUCTION_REGION.maxZ).spliterator(), false)
            .map(BlockPos::immutable)
            .collect(Collectors.toList());
    }

    public PropellerSpatialHandler(SmartBlockEntity be) {
        super(be);
    }

    public Set<BlockPos> getObstructedBlocks() {
        return obstructedBlocks;
    }

    @Override
    public void tick() {
        super.tick();

        Level level = getWorld();

        if (level.isClientSide()) {
            return;
        }

        if (!(blockEntity instanceof PropellerBlockEntity pbe)) {
            return;
        }

        scanIndex = (scanIndex + 1) % SCAN_POSITIONS.size();
        BlockPos relativePos = SCAN_POSITIONS.get(scanIndex);

        Direction facing = pbe.getBlockState().getValue(PropellerBlock.FACING);
        BlockPos worldCheckPos = getPos().offset(rotate(relativePos, facing));

        boolean isObstructed = isPositionObstructed(worldCheckPos);

        if (isObstructed) {
            obstructedBlocks.add(worldCheckPos);
        } else {
            obstructedBlocks.remove(worldCheckPos);
        }

        applyObstructionConsequences(pbe);
    }

    public void triggerImmediateScan() {
        if (!(blockEntity instanceof PropellerBlockEntity pbe)) return;

        obstructedBlocks.clear();
        Direction facing = pbe.getBlockState().getValue(PropellerBlock.FACING);

        for (BlockPos relativePos : SCAN_POSITIONS) {
            BlockPos worldCheckPos = getPos().offset(rotate(relativePos, facing));
            if (isPositionObstructed(worldCheckPos)) {
                obstructedBlocks.add(worldCheckPos);
            }
        }

        applyObstructionConsequences(pbe);
    }


    private boolean isPositionObstructed(BlockPos worldCheckPos) {
        if (worldCheckPos.equals(getPos())) {
            return false;
        }
        return !getWorld().getBlockState(worldCheckPos).isAir();
    }

    private void applyObstructionConsequences(PropellerBlockEntity pbe) {
        if (!obstructedBlocks.isEmpty() && pbe.getBladeCount() > 0) {
            if (Math.abs(pbe.getSpeed()) > 0) {
                breakBlades(pbe);
            } else {
                dropBlades(pbe);
            }
        }
    }

    private void breakBlades(PropellerBlockEntity pbe) {
        Level level = getWorld();
        BlockPos pos = getPos();
        
        List<ItemStack> removedBlades = new ArrayList<>();
        for (int i = 0; i < pbe.bladeInventory.getSlots(); i++) {
            ItemStack stackInSlot = pbe.bladeInventory.getStackInSlot(i);
            if (!stackInSlot.isEmpty()) {
                removedBlades.add(stackInSlot.copy());
                pbe.bladeInventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }

        for (ItemStack bladeStack : removedBlades) {
            if (bladeStack.getItem() instanceof PropellerBladeItem bladeItem) {
                ItemStack drop = bladeItem.getBreakDrop();
                if (!drop.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
                }
            }
        }
        
        pbe.setChanged();
        pbe.sendData();

        level.playSound(null, pos, SoundEvents.SHIELD_BREAK, SoundSource.BLOCKS, 1.2f, 0.8f);
    }

    private void dropBlades(PropellerBlockEntity pbe) {
        Level level = getWorld();
        BlockPos pos = getPos();

        for (int i = 0; i < pbe.bladeInventory.getSlots(); i++) {
            ItemStack stackInSlot = pbe.bladeInventory.getStackInSlot(i);
            if (!stackInSlot.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stackInSlot);
                pbe.bladeInventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        
        pbe.setChanged();
        pbe.sendData();

        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0f, 1.0f);
    }


    public static BlockPos rotate(BlockPos pos, Direction targetDirection) {
        return switch (targetDirection) {
            case NORTH -> new BlockPos(pos.getX(), pos.getY(), pos.getZ());
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case DOWN -> new BlockPos(pos.getX(), -pos.getZ(), pos.getY());
            case UP -> new BlockPos(pos.getX(), pos.getZ(), -pos.getY());
        };
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
