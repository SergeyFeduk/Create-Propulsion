package com.deltasf.createpropulsion.thruster;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.compat.PropulsionCompatibility;
import com.deltasf.createpropulsion.compat.computercraft.ComputerBehaviour;
import com.deltasf.createpropulsion.debug.DebugRenderer;
import com.deltasf.createpropulsion.particles.ParticleTypes;
import com.deltasf.createpropulsion.particles.PlumeParticleData;
import com.deltasf.createpropulsion.utility.GoggleUtils;
import com.deltasf.createpropulsion.utility.MathUtility;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.joml.*;
import org.joml.Math;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.awt.*;
import java.util.List;

import javax.annotation.Nullable;

@SuppressWarnings({"deprecation", "unchecked"})
public abstract class AbstractThrusterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    // Constants
    protected static final int OBSTRUCTION_LENGTH = 10;
    protected static final int TICKS_PER_ENTITY_CHECK = 5;
    protected static final int LOWEST_POWER_THRSHOLD = 5;
    private static final float PARTICLE_VELOCITY = 4;
    private static final double NOZZLE_OFFSET_FROM_CENTER = 0.9;
    private static final double SHIP_VELOCITY_INHERITANCE = 0.5;

    // Common State
    protected ThrusterData thrusterData;
    protected int emptyBlocks;
    protected boolean isThrustDirty = false;
    private final ThrusterDamager damager;

    // Ticking
    private int currentTick = 0;
    private int clientTick = 0;
    private float particleSpawnAccumulator = 0.0f;

    // Particles
    protected ParticleType<PlumeParticleData> particleType;

    // CC Peripheral
    public AbstractComputerBehaviour computerBehaviour;
    public boolean overridePower = false;
    public int overridenPower;

    public AbstractThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        thrusterData = new ThrusterData();
        particleType = (ParticleType<PlumeParticleData>) ParticleTypes.getPlumeType();
        this.damager = new ThrusterDamager(this);
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        super.initialize();
        if (!level.isClientSide) {
            calculateObstruction(level, worldPosition, getBlockState().getValue(AbstractThrusterBlock.FACING));
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }
    }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        if (this.isRemoved()) {
            return;
        }
        //This part should ACTUALLY fix the issue with particle emission 
        if (level.getBlockState(worldPosition).getBlock() != this.getBlockState().getBlock()) {
            this.setRemoved();
            return;
        }

        super.tick();
        BlockState currentBlockState = getBlockState();
        if (level.isClientSide) {
            if (shouldEmitParticles()) {
                emitParticles(level, worldPosition, currentBlockState);
            }
            return;
        }
        currentTick++;
        damager.tick(currentTick);
        int tick_rate = PropulsionConfig.THRUSTER_TICKS_PER_UPDATE.get();

        // Periodically recalculate obstruction
        if (currentTick % (tick_rate * 2) == 0) {
            int previousEmptyBlocks = emptyBlocks;
            calculateObstruction(level, worldPosition, currentBlockState.getValue(AbstractThrusterBlock.FACING));
            if (previousEmptyBlocks != emptyBlocks) {
                isThrustDirty = true;
                setChanged();
                level.sendBlockUpdated(worldPosition, currentBlockState, currentBlockState, Block.UPDATE_CLIENTS);
            }
        }

        // Update thrust periodically or when marked dirty
        if (isThrustDirty || currentTick % tick_rate == 0) {
            updateThrust(currentBlockState);
        }
    }

    public abstract void updateThrust(BlockState currentBlockState);

    protected abstract boolean isWorking();

    protected abstract LangBuilder getGoggleStatus();

    @Nullable
    protected abstract Direction getFluidCapSide();

    public ThrusterData getThrusterData() {
        return thrusterData;
    }

    public int getEmptyBlocks() {
        return emptyBlocks;
    }

    public void dirtyThrust() {
        isThrustDirty = true;
    }

    protected boolean shouldEmitParticles() {
        return isPowered() && isWorking();
    }

    protected boolean shouldDamageEntities() {
        return PropulsionConfig.THRUSTER_DAMAGE_ENTITIES.get() && isPowered() && isWorking();
    }

    protected void addSpecificGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {}

    protected boolean isPowered() {
        return getOverriddenPowerOrState(getBlockState()) > 0;
    }

    protected float calculateObstructionEffect() {
        return (float) emptyBlocks / (float) OBSTRUCTION_LENGTH;
    }
    
    protected int getOverriddenPowerOrState(BlockState currentBlockState) {
        if (PropulsionCompatibility.CC_ACTIVE && overridePower) {
            return overridenPower;
        }
        return currentBlockState.getValue(AbstractThrusterBlock.POWER);
    }

    public void emitParticles(Level level, BlockPos pos, BlockState state) {
        if (emptyBlocks == 0) return;
        int power = getOverriddenPowerOrState(state);
    
        double particleCountMultiplier = org.joml.Math.clamp(0.0, 2.0, PropulsionConfig.THRUSTER_PARTICLE_COUNT_MULTIPLIER.get());
        if (particleCountMultiplier <= 0) return;
    
        clientTick++;
        if (power < LOWEST_POWER_THRSHOLD && clientTick % 2 == 0) {
            clientTick = 0;
            return;
        }
    
        this.particleSpawnAccumulator += particleCountMultiplier;
    
        int particlesToSpawn = (int) this.particleSpawnAccumulator;
        if (particlesToSpawn == 0) return;
    
        this.particleSpawnAccumulator -= particlesToSpawn;
        float powerPercentage = Math.max(power, LOWEST_POWER_THRSHOLD) / 15.0f;
        Direction direction = state.getValue(AbstractThrusterBlock.FACING);
        Direction oppositeDirection = direction.getOpposite();
    
        double currentNozzleOffset = NOZZLE_OFFSET_FROM_CENTER;
        Vector3d additionalVel = new Vector3d();
        ClientShip ship = VSGameUtilsKt.getShipObjectManagingPos((ClientLevel) level, pos);
        if (ship != null) {
            Vector3dc shipWorldVelocityJOML = ship.getVelocity();
            Matrix4dc transform = ship.getRenderTransform().getShipToWorld();
            Matrix4dc invTransform = ship.getRenderTransform().getWorldToShip();
    
            Vector3d shipVelocity = invTransform
                // Rotate velocity with ship transform
                .transformDirection(new Vector3d(shipWorldVelocityJOML));
    
            Vector3d particleEjectionUnitVecJOML = transform
                // Rotate velocity with ship transform
                .transformDirection(VectorConversionsMCKt.toJOMLD(oppositeDirection.getNormal()));
    
            double shipVelComponentAlongRotatedEjection = shipWorldVelocityJOML.dot(particleEjectionUnitVecJOML);
            if (shipVelComponentAlongRotatedEjection > 0.0) {
                Vector3d normalizedVelocity = new Vector3d();
                shipWorldVelocityJOML.normalize(normalizedVelocity);
                double shipVelComponentAlongRotatedEjectionNormalized = normalizedVelocity.dot(particleEjectionUnitVecJOML);
                //Effect is used to smooth transition from no additional offset/vel to full in range [0, 1]
                double effect = org.joml.Math.clamp(0.0, 1, shipVelComponentAlongRotatedEjectionNormalized);
                double additionalOffset = (shipVelComponentAlongRotatedEjection) * PropulsionConfig.THRUSTER_PARTICLE_OFFSET_INCOMING_VEL_MODIFIER.get();
                currentNozzleOffset += additionalOffset * effect;
                additionalVel = new Vector3d(shipVelocity).mul(SHIP_VELOCITY_INHERITANCE * effect);
            }
        }
    
        double particleX = pos.getX() + 0.5 + oppositeDirection.getStepX() * currentNozzleOffset;
        double particleY = pos.getY() + 0.5 + oppositeDirection.getStepY() * currentNozzleOffset;
        double particleZ = pos.getZ() + 0.5 + oppositeDirection.getStepZ() * currentNozzleOffset;
    
        Vector3d particleVelocity = new Vector3d(oppositeDirection.getStepX(), oppositeDirection.getStepY(), oppositeDirection.getStepZ())
            .mul(PARTICLE_VELOCITY * powerPercentage).add(additionalVel);
    
        // Spawn the calculated number of particles.
        for (int i = 0; i < particlesToSpawn; i++) {
            level.addParticle(new PlumeParticleData(particleType), true,
                particleX, particleY, particleZ,
                particleVelocity.x, particleVelocity.y, particleVelocity.z);
        }
    }

    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection){
        //Starting from the block behind and iterate OBSTRUCTION_LENGTH blocks in that direction
        //Can't really use level.clip as we explicitly want to check for obstruction only in ship space
        int oldEmptyBlocks = this.emptyBlocks;
        for (emptyBlocks = 0; emptyBlocks < OBSTRUCTION_LENGTH; emptyBlocks++){
            BlockPos checkPos = pos.relative(forwardDirection.getOpposite(), emptyBlocks + 1);
            BlockState state = level.getBlockState(checkPos);
            if (!(state.isAir() || !state.isSolid())) break;
        }
        if (oldEmptyBlocks != this.emptyBlocks) { // Only set dirty if it actually changed
            isThrustDirty = true;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean wasThrustDirty = isThrustDirty;
        calculateObstruction(getLevel(), worldPosition, getBlockState().getValue(AbstractThrusterBlock.FACING));
        isThrustDirty = wasThrustDirty;

        Lang.translate("gui.goggles.thruster.status", new Object[0]).text(":").space().add(getGoggleStatus()).forGoggles(tooltip);

        float efficiency = 100;
        ChatFormatting tooltipColor = ChatFormatting.GREEN;
        if (emptyBlocks < OBSTRUCTION_LENGTH) {
            efficiency = calculateObstructionEffect() * 100;
            tooltipColor = GoggleUtils.efficiencyColor(efficiency);
            Lang.builder().add(Lang.translate("gui.goggles.thruster.obstructed", new Object[0])).space().add(Lang.text(GoggleUtils.makeObstructionBar(emptyBlocks, OBSTRUCTION_LENGTH))).style(tooltipColor).forGoggles(tooltip);
        }

        Lang.builder().add(Lang.translate("gui.goggles.thruster.efficiency", new Object[0])).space().add(Lang.number(efficiency)).add(Lang.text("%")).style(tooltipColor).forGoggles(tooltip);

        addSpecificGoggleInfo(tooltip, isPlayerSneaking);
        return true;
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putInt("emptyBlocks", emptyBlocks);
        compound.putInt("currentTick", currentTick);
        if (PropulsionCompatibility.CC_ACTIVE) {
            compound.putInt("overridenPower", overridenPower);
            compound.putBoolean("overridePower", overridePower);
        }
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        emptyBlocks = compound.getInt("emptyBlocks");
        currentTick = compound.getInt("currentTick");
        if (PropulsionCompatibility.CC_ACTIVE) {
            overridenPower = compound.getInt("overridenPower");
            overridePower = compound.getBoolean("overridePower");
        }
    }
}
