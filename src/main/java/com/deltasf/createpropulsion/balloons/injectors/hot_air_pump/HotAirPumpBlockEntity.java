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
import com.deltasf.createpropulsion.registries.PropulsionCapabilities;
import com.deltasf.createpropulsion.utility.math.MathUtility;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
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
    private int scanTally = 0;

    //Visuals
    public float fanAngle = 0;
    public float membraneTime = (float) (Math.PI / 2.0); //Full extent after placment
    public float membraneSpeed = 0;
    public float lastRenderTime = -1;

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
        }
    }

    @Override
    public void tick() {
        super.tick();
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        
        if (scanTally > 0) {
            scanTally--;
        }

        lastHeatConsumed = heatConsumedThisTick;
        heatConsumedThisTick = 0;

        boolean currentlyHot = lastHeatConsumed > 0;
        if (currentlyHot != isAboveHeatThreshold) {
            isAboveHeatThreshold = currentlyHot;
            attemptScan();
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
        if (level == null || level.isClientSide() || scanTally > 0) return;
        scanTally = 5;
        // Ship check
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship == null) return;
        
        // Balloon check
        Balloon balloon = BalloonShipRegistry.forShip(ship.getId(), level).getBalloonOf(getId());
        if (balloon != null) return;
        
        // Perform scan via behaviour
        injectorBehaviour.performScan();
        notifyUpdate();
    }

    @Override
    public double getInjectionAmount() {
        float rpmPercentage = Math.abs(getSpeed()) / MAX_RPM;
        double effectiveHeat = lastHeatConsumed;
        //Fallback, so first tick of injection uses heat from this tick, not previous
        if (effectiveHeat <= MathUtility.epsilon && heatConsumedThisTick > MathUtility.epsilon) {
            effectiveHeat = heatConsumedThisTick;
        }
        double injection = BASE_INJECTION_AMOUNT * rpmPercentage * effectiveHeat;
        double efficiency = obstructionBehaviour.getEfficiency();
        return injection * efficiency;
    }

    @Override
    public void onBalloonLoaded() { balloonInfoBehaviour.performUpdate(); }

    // IHeatConsumer impl

    public float getLastHeatConsumed() {
        return lastHeatConsumed;
    }

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
        boolean hasRPM = Math.abs(getSpeed()) > 0;
        boolean hasHeat = lastHeatConsumed >= OPERATING_THRESHOLD;
        boolean isOnShip = VSGameUtilsKt.isBlockInShipyard(getLevel(), getBlockPos());

        //Status
        String key = "";
        ChatFormatting color = null;

        if (isAirless) {
            key = "gui.goggles.hot_air_pump.status.airless";
            color = ChatFormatting.RED;
        } else if (!isOnShip) {
            key = "gui.goggles.hot_air_burner.status.not_shipified";
            color = ChatFormatting.RED;
        } else if (!isBalloonPresent) {
            //No RPM -> No Heat -> No Balloon
            if (!hasRPM) {
                key = "gui.goggles.hot_air_pump.status.no_rpm";
                color = ChatFormatting.GRAY;
            } else if (!hasHeat) {
                key = "gui.goggles.hot_air_pump.status.no_heat";
                color = ChatFormatting.GOLD;
            } else {
                key = "gui.goggles.hot_air_burner.status.no_balloon";
                color = ChatFormatting.DARK_GRAY;
            }
        } else {
            //Balloon is present
            if (hasRPM && hasHeat) {
                key = "gui.goggles.hot_air_burner.status.on";
                color = ChatFormatting.GREEN;
            } else if (!hasRPM) {
                key = "gui.goggles.hot_air_pump.status.no_rpm";
                color = ChatFormatting.GRAY;
            } else {
                key = "gui.goggles.hot_air_pump.status.no_heat";
                color = ChatFormatting.GOLD;
            }
        }

        CreateLang.builder()
            .add(CreateLang.translate("gui.goggles.hot_air_pump.status"))
            .text(": ")
            .add(CreateLang.translate(key).style(color))
            .forGoggles(tooltip);

        //Injection
        if (!isAirless && isOnShip && isBalloonPresent) {
            double currentInjection = getInjectionAmount();
            int percentage = (int) ((currentInjection / BASE_INJECTION_AMOUNT) * 100);

            LangBuilder injectionBuilder = CreateLang.builder()
                .translate("gui.goggles.hot_air_pump.injection")
                .text(": ")
                .add(CreateLang.text(percentage + "%").style(ChatFormatting.AQUA));

            if (isPlayerSneaking) {
                injectionBuilder.add(CreateLang.text(String.format(" (%.2f)", currentInjection)).style(ChatFormatting.AQUA));
            }
            
            injectionBuilder.forGoggles(tooltip);
        }

        //Balloon
        if (!isAirless && isBalloonPresent) {
            CreateLang.text("").forGoggles(tooltip);
            balloonInfoBehaviour.addBalloonTooltip(tooltip, isPlayerSneaking);
        }

        obstructionBehaviour.displayObstructionOutline("HotAirPumpObstruction");
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
