package com.deltasf.createpropulsion.balloons.injectors;

import java.util.UUID;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.hot_air.BalloonAttachment;
import com.deltasf.createpropulsion.balloons.injectors.hot_air_burner.HotAirBurnerBlockEntity;
import com.deltasf.createpropulsion.balloons.injectors.hot_air_pump.HotAirPumpBlockEntity;
import com.deltasf.createpropulsion.balloons.particles.BalloonParticleSystem;
import com.deltasf.createpropulsion.balloons.particles.ShipParticleHandler;
import com.deltasf.createpropulsion.balloons.particles.effectors.StreamEffector;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.balloons.registries.ClientBalloonRegistry;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public class HotAirInjectorBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<HotAirInjectorBehaviour> TYPE = new BehaviourType<>();

    private UUID haiId;

    private StreamEffector streamEffector;
    private ShipParticleHandler activeHandler;
    private int lastBalloonId = -1;

    public HotAirInjectorBehaviour(SmartBlockEntity be) {
        super(be);
    }

    public UUID getId() {
        if (haiId == null) {
            haiId = UUID.randomUUID();
            blockEntity.setChanged();
        }
        return haiId;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!getWorld().isClientSide) {
            tryRegister();
        } else {
            refreshEffectorGeometry();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        unregister();

        if (getWorld().isClientSide && streamEffector != null && activeHandler != null) {
            activeHandler.effectors.removeStream(streamEffector);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClientSide) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(getWorld(), getPos());
            ShipParticleHandler currentHandler = (ship != null) ? BalloonParticleSystem.getHandler(ship.getId()) : null;

            //Handle changed -> Recreate effector
            if (currentHandler != activeHandler) {
                refreshEffectorGeometry(); 
            }
            
            int currentBalloonId = ClientBalloonRegistry.getBalloonIdForHai(getId());
            if (currentBalloonId != lastBalloonId) {
                lastBalloonId = currentBalloonId;
                refreshEffectorGeometry();
            }

            //Sync Intensity
            if (streamEffector != null) {
                updateEffectorIntensity();
            }
        }

    }

    private void updateEffectorIntensity() {
        float intensity = 0.0f;
        
        if (blockEntity instanceof HotAirBurnerBlockEntity burner) {
            float ratio = (float) (burner.getInjectionAmount() / 1); 
            intensity = Mth.clamp(ratio, 0f, 1f);
        } else if (blockEntity instanceof IHotAirInjector injector) {
            float ratio = (float) (injector.getInjectionAmount() / HotAirPumpBlockEntity.BASE_INJECTION_AMOUNT);
            intensity = Mth.clamp(ratio, 0f, 1f);
        }
        
        streamEffector.intensity = intensity;
    }

    public void onObstructionUpdate(int newHeight) {
        refreshEffectorGeometry();
    }

    public void refreshEffectorGeometry() {
        Level level = getWorld();
        if (level == null) return;
        
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, getPos());
        if (ship == null) {
            cleanupEffector();
            return;
        }
        
        ShipParticleHandler handler = BalloonParticleSystem.getHandler(ship.getId());
        if (handler == null) {
            cleanupEffector();
            return;
        }

        activeHandler = handler;

        //Cleanup old effector from buckets
        if (streamEffector != null) {
            handler.effectors.removeStream(streamEffector);
        }

        //Get height
        int streamHeight = 0;
        AirInjectorObstructionBehaviour obstruction = blockEntity.getBehaviour(AirInjectorObstructionBehaviour.TYPE);
        if (obstruction != null) {
            streamHeight = obstruction.getStreamHeight();
        }
        
        int balloonId = ClientBalloonRegistry.getBalloonIdForHai(getId());
        streamEffector = new StreamEffector(
            getPos(), 
            handler.getAnchorX(), 
            handler.getAnchorY(), 
            handler.getAnchorZ(),
            balloonId
        );
        
        //Register buckets
        handler.effectors.updateStream(streamEffector, streamHeight);
        updateEffectorIntensity();
    }

    private void cleanupEffector() {
        activeHandler = null;
        streamEffector = null;
    }
    
    public void tryRegister() {
        Level level = getWorld();
        BlockPos pos = getPos();
        if (level == null || level.isClientSide) return;

        // Ensure ID exists
        getId(); 

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship != null) {
            BalloonShipRegistry.forShip(ship.getId(), level).registerHai(haiId, level, pos);
            BalloonAttachment.ensureAttachmentExists(level, pos);
        }
    }

    private void unregister() {
        Level level = getWorld();
        if (level == null || level.isClientSide) return;
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, getPos());
        if (ship != null && this.haiId != null) {
            BalloonShipRegistry.forShip(ship.getId(), level).unregisterHai(haiId, level);
        }
    }

    public void performScan() {
        Level level = getWorld();
        BlockPos pos = getPos();
        if (haiId == null || level == null || level.isClientSide()) return;
        
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        if (ship != null) {
            BalloonShipRegistry.forShip(ship.getId(), level).startScanFor(haiId, level, pos);
            BalloonAttachment.ensureAttachmentExists(level, pos);
        }
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if (nbt.hasUUID("id")) {
            this.haiId = nbt.getUUID("id");
        }
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        if (this.haiId != null) {
            nbt.putUUID("id", this.haiId);
        }
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}