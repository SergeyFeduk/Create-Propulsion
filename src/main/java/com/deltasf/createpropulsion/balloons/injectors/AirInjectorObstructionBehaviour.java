package com.deltasf.createpropulsion.balloons.injectors;

import java.util.HashSet;
import java.util.Set;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.physics_assembler.AssemblyUtility;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

public class AirInjectorObstructionBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<AirInjectorObstructionBehaviour> TYPE = new BehaviourType<>();

    private static final int SCAN_COOLDOWN_TICKS = 20; //TODO: Config
    private static final int SCAN_DISTANCE = 8;

    private int scanCooldown;
    private final Set<BlockPos> obstructedBlocks = new HashSet<>();
    private int streamHeight = 0;
    
    private boolean clientDataDirty = false;

    public AirInjectorObstructionBehaviour(SmartBlockEntity be) {
        super(be);
        this.scanCooldown = 0;
    }

    public Set<BlockPos> getObstructedBlocks() {
        return obstructedBlocks;
    }
    
    public int getStreamHeight() {
        return streamHeight;
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

    public void displayObstructionOutline(String key) {
        if (!getObstructedBlocks().isEmpty()) {
            Outline.OutlineParams outline = Outliner.getInstance().showCluster(key, getObstructedBlocks());
            outline.colored(AssemblyUtility.CANCEL_COLOR);
            outline.lineWidth(1/16f);
            outline.withFaceTexture(AllSpecialTextures.CHECKERED);
            outline.disableLineNormals();
        }
    }

    @Override
    public void tick() {
        super.tick();
        Level level = getWorld();
        if (level.isClientSide()) {
            if (clientDataDirty) {
                clientDataDirty = false;
                HotAirInjectorBehaviour injector = blockEntity.getBehaviour(HotAirInjectorBehaviour.TYPE);
                if (injector != null) {
                    injector.onObstructionUpdate(streamHeight);
                }
            }
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

        //Check ship
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship == null) {
            clearObstructions();
            return;
        }

        if (!(blockEntity instanceof IHotAirInjector injector)) {
            clearObstructions();
            return;
        }

        BalloonRegistry registry = BalloonShipRegistry.forShip(ship.getId(), level);
        Balloon balloon = registry.getBalloonOf(injector.getId());
        if (balloon == null) {
            clearObstructions();
            return;
        }

        Set<BlockPos> newObstructions = new HashSet<>();
        int newStreamHeight = 0;
        
        boolean hitBalloon = false; 
        boolean hitHab= false;

        for (int y = 1; y <= SCAN_DISTANCE; y++) {
            BlockPos currentPos = pos.above(y);

            boolean isBalloonVolume = balloon.contains(currentPos);
            boolean isHab = HaiGroup.isHab(currentPos, level);

            //Track pre-balloon obstructions
            if (!hitBalloon) {
                if (isBalloonVolume) {
                    hitBalloon = true;
                }
                if (!hitBalloon && isHab) {
                    newObstructions.add(currentPos);
                }
            }
            //Track hab hit
            if (!hitHab && isHab) {
                hitHab = true;
                newStreamHeight = y - 1;
            }
        }

        boolean changed = false;
        if (!obstructedBlocks.equals(newObstructions)) {
            obstructedBlocks.clear();
            obstructedBlocks.addAll(newObstructions);
            changed = true;
        }
        
        if (streamHeight != newStreamHeight) {
            streamHeight = newStreamHeight;
            changed = true;
        }

        if (changed) {
            blockEntity.notifyUpdate();
        }
    }

    private void clearObstructions() {
        boolean changed = !obstructedBlocks.isEmpty() || streamHeight != 0;
        if (changed) {
            obstructedBlocks.clear();
            streamHeight = 0;
            blockEntity.notifyUpdate();
        }
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        if (!clientPacket) return;
        
        ListTag obstructedList = new ListTag();
        for (BlockPos pos : obstructedBlocks) {
            obstructedList.add(NbtUtils.writeBlockPos(pos));
        }
        compound.put("obstructedList", obstructedList);
        compound.putInt("streamHeight", streamHeight);
    }

    @Override
    public void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if (!clientPacket) return;
        
        Set<BlockPos> oldSet = new HashSet<>(obstructedBlocks);
        int oldHeight = streamHeight;
        
        obstructedBlocks.clear();
        ListTag obstructedList = compound.getList("obstructedList", Tag.TAG_COMPOUND);
        for (Tag tag : obstructedList) {
            obstructedBlocks.add(NbtUtils.readBlockPos((CompoundTag) tag));
        }
        streamHeight = compound.getInt("streamHeight");
        
        if (!oldSet.equals(obstructedBlocks) || oldHeight != streamHeight) {
            clientDataDirty = true;
        }
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}