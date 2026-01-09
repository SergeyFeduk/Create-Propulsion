package com.deltasf.createpropulsion.balloons.injectors.hot_air_pump;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.atmosphere.DimensionAtmosphereManager;
import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.injectors.AirInjectorObstructionBehaviour;
import com.deltasf.createpropulsion.balloons.injectors.BalloonInfoBehaviour;
import com.deltasf.createpropulsion.balloons.injectors.HotAirInjectorBehaviour;
import com.deltasf.createpropulsion.balloons.injectors.IHotAirInjector;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.heat.IHeatConsumer;
import com.deltasf.createpropulsion.physics_assembler.AssemblyUtility;
import com.deltasf.createpropulsion.registries.PropulsionCapabilities;
import com.deltasf.createpropulsion.utility.math.MathUtility;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class HotAirPumpBlockEntity extends KineticBlockEntity implements IHotAirInjector, IHeatConsumer {
    public static final float MAX_RPM = 256.0f;
    public static final float MAX_HEAT_CONSUMPTION = 2.0f;

    public static final float OPERATING_THRESHOLD = 0.3f; 
    public static final float BASE_INJECTION_AMOUNT = 6.0f; //TODO: Config

    //Behaviours
    private HotAirInjectorBehaviour injectorBehaviour;
    private AirInjectorObstructionBehaviour obstructionBehaviour;
    private BalloonInfoBehaviour balloonInfoBehaviour;

    private final LazyOptional<IHeatConsumer> heatConsumerCap;

    //State
    private float heatConsumedThisTick = 0;
    private float lastHeatConsumed = 0;
    private boolean isAboveHeatThreshold = false;

    public HotAirPumpBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.heatConsumerCap = LazyOptional.of(() -> this);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        injectorBehaviour = new HotAirInjectorBehaviour(this);
        behaviours.add(injectorBehaviour);

        obstructionBehaviour = new AirInjectorObstructionBehaviour(this);
        behaviours.add(obstructionBehaviour);

        balloonInfoBehaviour = new BalloonInfoBehaviour(this, this::getId);
        behaviours.add(balloonInfoBehaviour);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (Math.abs(previousSpeed - getSpeed()) > MathUtility.epsilon) {
            attemptScan();
            notifyUpdate();
        }
    }

    @Override
    public void tick() {
        super.tick();
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;

        lastHeatConsumed = heatConsumedThisTick;
        heatConsumedThisTick = 0;

        boolean currentlyHot = lastHeatConsumed > 0;
        if (currentlyHot != isAboveHeatThreshold) {
            isAboveHeatThreshold = currentlyHot;
            attemptScan();
            notifyUpdate();
        }
    }

    // Hot air injector impl

    @Override
    public UUID getId() {
        return injectorBehaviour.getId();
    }

    @Override
    public void attemptScan() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        
        // Ship check
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship == null) return;
        
        // Balloon check
        Balloon balloon = BalloonShipRegistry.forShip(ship.getId(), level).getBalloonOf(getId());
        if (balloon != null) return;
        
        // Perform scan via behaviour
        injectorBehaviour.performScan();
    }

    @Override
    public double getInjectionAmount() {
        float rpmPercentage = Math.abs(getSpeed()) / MAX_RPM;
        double injection = BASE_INJECTION_AMOUNT * rpmPercentage * lastHeatConsumed;
        double efficiency = obstructionBehaviour.getEfficiency();
        return injection * efficiency;
    }

    @Override
    public void onBalloonLoaded() { balloonInfoBehaviour.performUpdate(); }

    // IHeatConsumer impl

    @Override
    public boolean isActive() {
        return Math.abs(getSpeed()) > 0; //Not rotating -> Do not waste fuel
        //return true;
    }

    @Override
    public float getOperatingThreshold() { return OPERATING_THRESHOLD; }

    @Override
    public float consumeHeat(float maxAvailable, float expectedHeatOutput, boolean simulate) {
        if (!isActive()) return 0;

        float limit = Math.min(MAX_HEAT_CONSUMPTION, expectedHeatOutput);
        float toConsume = Math.min(limit, maxAvailable);

        //We are in a simulation?!
        if (!simulate) {
            heatConsumedThisTick += toConsume;
        }
        return toConsume;
    }

    //Goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean isAirless = level != null && DimensionAtmosphereManager.getData(level).isAirless();
        boolean isBalloonPresent = balloonInfoBehaviour.isBalloonPresent();
        
        LangBuilder status = CreateLang.builder().add(CreateLang.translate("gui.goggles.hot_air_burner.status")).text(": ");
        
        if (isAirless) {
            status.add(CreateLang.translate("gui.goggles.hot_air_burner.status.airless").style(ChatFormatting.RED));
        } else if (Math.abs(getSpeed()) == 0) {
             status.add(Component.translatable("create.tooltip.speedRequirement.stopped").withStyle(ChatFormatting.GRAY));
        } else if (lastHeatConsumed < 0.1f) {
             status.add(Component.literal("Cold").withStyle(ChatFormatting.BLUE));
        } else {
             status.add(Component.literal("Pumping").withStyle(ChatFormatting.GREEN));
        }
        status.forGoggles(tooltip);

        // Heat Info
        CreateLang.builder()
            .add(Component.literal("Heat Intake: ").withStyle(ChatFormatting.GRAY))
            .add(Component.literal(String.format("%.1f / %.1f HU", lastHeatConsumed, MAX_HEAT_CONSUMPTION)).withStyle(ChatFormatting.GOLD))
            .forGoggles(tooltip);

        // Injection Info
        double injection = getInjectionAmount();
        if (injection > 0) {
            CreateLang.builder()
                .add(Component.literal("Injection: ").withStyle(ChatFormatting.GRAY))
                .add(Component.literal(String.format("%.2f", injection)).withStyle(ChatFormatting.AQUA))
                .forGoggles(tooltip);
        }

        if (!isAirless && isBalloonPresent) {
            CreateLang.text("").forGoggles(tooltip);
        }

        if (!isAirless) {
            balloonInfoBehaviour.addBalloonTooltip(tooltip, isPlayerSneaking);
        }

        // Obstruction Overlay
        if (!obstructionBehaviour.getObstructedBlocks().isEmpty()) {
            Outline.OutlineParams outline = Outliner.getInstance().showCluster("HotAirPumpObstruction", obstructionBehaviour.getObstructedBlocks());
            outline.colored(AssemblyUtility.CANCEL_COLOR);
            outline.lineWidth(1/16f);
            outline.withFaceTexture(AllSpecialTextures.CHECKERED);
            outline.disableLineNormals();
            
            CreateLang.builder()
                .add(Component.literal("Obstruction Detected!").withStyle(ChatFormatting.RED))
                .forGoggles(tooltip);
        }

        return true;
    }

    //Caps

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PropulsionCapabilities.HEAT_CONSUMER && side == Direction.DOWN) {
            return heatConsumerCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        heatConsumerCap.invalidate();
    }

    //Nbt (took some mental effort to not call this section "NBT slop")

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putFloat("LastHeatConsumed", lastHeatConsumed);
        compound.putBoolean("IsAboveThreshold", isAboveHeatThreshold);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        lastHeatConsumed = compound.getFloat("LastHeatConsumed");
        isAboveHeatThreshold = compound.getBoolean("IsAboveThreshold");
    }
}
