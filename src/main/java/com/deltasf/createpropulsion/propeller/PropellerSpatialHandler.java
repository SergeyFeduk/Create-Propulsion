package com.deltasf.createpropulsion.propeller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.primitives.AABBi;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import com.deltasf.createpropulsion.propeller.blades.PropellerBladeItem;
import com.deltasf.createpropulsion.utility.MathUtility;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

//Handles hard and soft obstruction & fluid checks for propeller
public class PropellerSpatialHandler extends BlockEntityBehaviour {
    public static final BehaviourType<PropellerSpatialHandler> TYPE = new BehaviourType<>();

    private static final float FLUID_SMOOTHING_DECAY = 5.0f;
    private static final AABBi HARD_OBSTRUCTION_REGION = new AABBi(-1, -1, 0, 1, 1, 0);

    private static final List<BlockPos> SCAN_POSITIONS;
    private static final List<Vector3f> FLUID_SAMPLE_POINTS;
    static {
        SCAN_POSITIONS = StreamSupport.stream(
            BlockPos.betweenClosed(HARD_OBSTRUCTION_REGION.minX, HARD_OBSTRUCTION_REGION.minY, HARD_OBSTRUCTION_REGION.minZ, HARD_OBSTRUCTION_REGION.maxX, HARD_OBSTRUCTION_REGION.maxY, HARD_OBSTRUCTION_REGION.maxZ).spliterator(), false)
            .map(BlockPos::immutable)
            .collect(Collectors.toList());

        FLUID_SAMPLE_POINTS = SCAN_POSITIONS.stream()
            .map(pos -> { 
                Vec3 center = pos.getCenter();
                //Even I don't know why
                return new Vector3f((float)center.x()-0.5f, (float)center.y()-0.5f,(float)center.z()); 
            }).collect(Collectors.toList());
    }

    private int scanIndex = 0;
    private final Set<BlockPos> obstructedBlocks = new HashSet<>();
    private final boolean[] isSampleInFluid;
    private int fluidSampleCount = 0;
    private float smoothFluidSample = 0f;

    public PropellerSpatialHandler(SmartBlockEntity be) {
        super(be);
        this.isSampleInFluid = new boolean[FLUID_SAMPLE_POINTS.size()];
    }

    public Set<BlockPos> getObstructedBlocks() {
        return obstructedBlocks;
    }

    public float getSmoothFluidSample() {
        return smoothFluidSample;
    }

    @Override
    public void tick() {
        super.tick();

        Level level = getWorld();

        if (level.isClientSide()) {
            return;
        }

        float dt = 1.0f / 20.0f;
        float fluidSample = (float) fluidSampleCount / (float)FLUID_SAMPLE_POINTS.size();
        smoothFluidSample = MathUtility.expDecay(smoothFluidSample, fluidSample, FLUID_SMOOTHING_DECAY, dt);

        if (!(blockEntity instanceof PropellerBlockEntity pbe)) {
            return;
        }

        scanIndex = (scanIndex + 1) % SCAN_POSITIONS.size();
        BlockPos relativePos = SCAN_POSITIONS.get(scanIndex);

        Direction facing = pbe.getBlockState().getValue(PropellerBlock.FACING);
        BlockPos worldCheckPos = getPos().offset(rotate(relativePos, facing));

        handleObstruction(worldCheckPos, pbe);

        if (!FLUID_SAMPLE_POINTS.isEmpty()) {
            Vector3f relativeSamplePoint = FLUID_SAMPLE_POINTS.get(scanIndex);
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, getPos());
            updateFluidState(pbe, ship, relativeSamplePoint);
        }
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

    private void updateFluidState(PropellerBlockEntity pbe, Ship ship, Vector3f relativeSamplePoint) {
        Direction facing = pbe.getBlockState().getValue(PropellerBlock.FACING);
        BlockPos origin = getPos();
        Vector3d propellerCenter = new Vector3d(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        Vector3f rotatedOffset = rotate(relativeSamplePoint, facing);
        Vector3d worldPosVec;

        if (ship == null) {
            worldPosVec = propellerCenter.add(rotatedOffset.x, rotatedOffset.y, rotatedOffset.z, new Vector3d());
        } else {
            Vector3d samplePointInShipSpace = propellerCenter.add(rotatedOffset.x, rotatedOffset.y, rotatedOffset.z, new Vector3d());
            worldPosVec = ship.getTransform().getShipToWorld().transformPosition(samplePointInShipSpace, new Vector3d());
        }

        BlockPos currentWorldPos = new BlockPos((int)Math.floor(worldPosVec.x), (int)Math.floor(worldPosVec.y), (int)Math.floor(worldPosVec.z));
        //DebugRenderer.drawBox(String.valueOf(pbe.hashCode() + relativeSamplePoint.hashCode()), VectorConversionsMCKt.toMinecraft(worldPosVec), new Vec3(0.1,0.1,0.1),20);
        //DebugRenderer.drawBox(String.valueOf(pbe.hashCode() + relativeSamplePoint.hashCode()), currentWorldPos, 20);

        //Update the state
        boolean wasInFluid = isSampleInFluid[scanIndex];
        boolean isInFluid = !getWorld().getFluidState(currentWorldPos).isEmpty();
        if (isInFluid && !wasInFluid) {
            fluidSampleCount++;
        } else if (!isInFluid && wasInFluid) {
            fluidSampleCount--;
        }

        isSampleInFluid[scanIndex] = isInFluid;
    }

    private void handleObstruction(BlockPos worldCheckPos, PropellerBlockEntity pbe) {
        boolean isObstructed = isPositionObstructed(worldCheckPos);

        if (isObstructed) {
            obstructedBlocks.add(worldCheckPos);
        } else {
            obstructedBlocks.remove(worldCheckPos);
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

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        if (!clientPacket)
            return;

        compound.putFloat("smoothFluidSample", this.smoothFluidSample);

        ListTag obstructedList = new ListTag();
        for (BlockPos pos : obstructedBlocks) {
            obstructedList.add(NbtUtils.writeBlockPos(pos));
        }
        compound.put("obstructed", obstructedList);
    }

    @Override
    public void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if (!clientPacket)
            return;

        this.smoothFluidSample = compound.getFloat("smoothFluidSample");

        obstructedBlocks.clear();
        ListTag obstructedList = compound.getList("obstructed", Tag.TAG_COMPOUND);
        for (Tag tag : obstructedList) {
            obstructedBlocks.add(NbtUtils.readBlockPos((CompoundTag) tag));
        }
    }

    //Helpers

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

    public static Vector3f rotate(Vector3f vec, Direction targetDirection) {
    return switch (targetDirection) {
        case NORTH -> new Vector3f(-vec.x, -vec.y, -vec.z);
        case SOUTH -> new Vector3f(vec.x, -vec.y, vec.z);
        case WEST -> new Vector3f(-vec.z, vec.y, -vec.x);
        case EAST -> new Vector3f(vec.z, -vec.y, -vec.x);
        case DOWN -> new Vector3f(vec.x, -vec.z, vec.y);
        case UP -> new Vector3f(vec.x, vec.z, -vec.y);
    };
}

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
