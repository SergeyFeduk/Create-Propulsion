package com.deltasf.createpropulsion.balloons.injectors;

import java.util.HashSet;
import java.util.Set;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.balloons.utils.BalloonScanner;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AirInjectorObstructionBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<AirInjectorObstructionBehaviour> TYPE = new BehaviourType<>();

    private static final int SCAN_COOLDOWN_TICKS = 20; //TODO: Config

    private int scanCooldown;
    private final Set<BlockPos> obstructedBlocks = new HashSet<>();

    public AirInjectorObstructionBehaviour(SmartBlockEntity be) {
        super(be);
        this.scanCooldown = 0;
    }

    public Set<BlockPos> getObstructedBlocks() {
        return obstructedBlocks;
    }

    public double getEfficiency() {
        int count = obstructedBlocks.size();
        switch (count) {
            case 0: return 1.0;
            case 1: return 2.0 / 3.0;
            case 2: return 1.0 / 3.0;
            default: return 0.0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        Level level = getWorld();
        if (level.isClientSide()) {
            return;
        }

        scanCooldown--;
        if (scanCooldown <= 0) {
            scanCooldown = SCAN_COOLDOWN_TICKS;
            performScan();
        }
    }

    private void performScan() {
        Level level = getWorld();
        BlockPos pos = getPos();

        // Check ship
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship == null) {
            clearObstructions();
            return;
        }

        // Check implementation
        if (!(blockEntity instanceof IHotAirInjector injector)) {
            clearObstructions();
            return;
        }

        // Check balloon
        BalloonRegistry registry = BalloonShipRegistry.forShip(ship.getId(), level);
        Balloon balloon = registry.getBalloonOf(injector.getId());
        if (balloon == null) {
            clearObstructions();
            return;
        }

        // Perform scan
        Set<BlockPos> newObstructions = new HashSet<>();
        for (int i = 1; i <= BalloonScanner.VERTICAL_ANOMALY_SCAN_DISTANCE; i++) {
            BlockPos currentPos = pos.above(i);

            BlockState state = level.getBlockState(currentPos);
            if (!state.getCollisionShape(level, currentPos).isEmpty() && !HaiGroup.isHab(currentPos, level)) {
                newObstructions.add(currentPos.immutable());
            }

            if (balloon.contains(currentPos)) {
                break;
            }
        }

        // Update
        if (!obstructedBlocks.equals(newObstructions)) {
            obstructedBlocks.clear();
            obstructedBlocks.addAll(newObstructions);
            blockEntity.notifyUpdate();
        }
    }

    private void clearObstructions() {
        if (!obstructedBlocks.isEmpty()) {
            obstructedBlocks.clear();
            blockEntity.notifyUpdate();
        }
    }

    //NBT

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        if (!clientPacket) return;
        ListTag obstructedList = new ListTag();
        for (BlockPos pos : obstructedBlocks) {
            obstructedList.add(NbtUtils.writeBlockPos(pos));
        }
        compound.put("ObstructedBlocks", obstructedList);
    }

    @Override
    public void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if (!clientPacket) return;
        obstructedBlocks.clear();
        ListTag obstructedList = compound.getList("ObstructedBlocks", Tag.TAG_COMPOUND);
        for (Tag tag : obstructedList) {
            obstructedBlocks.add(NbtUtils.readBlockPos((CompoundTag) tag));
        }
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
