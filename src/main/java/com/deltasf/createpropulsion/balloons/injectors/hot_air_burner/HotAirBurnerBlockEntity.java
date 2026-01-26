package com.deltasf.createpropulsion.balloons.injectors.hot_air_burner;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.atmosphere.DimensionAtmosphereManager;
import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.injectors.AirInjectorObstructionBehaviour;
import com.deltasf.createpropulsion.balloons.injectors.BalloonInfoBehaviour;
import com.deltasf.createpropulsion.balloons.injectors.HotAirInjectorBehaviour;
import com.deltasf.createpropulsion.balloons.injectors.IHotAirInjector;
import com.deltasf.createpropulsion.balloons.particles.BalloonParticleSystem;
import com.deltasf.createpropulsion.balloons.particles.ShipParticleHandler;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.balloons.registries.ClientBalloonRegistry;
import com.deltasf.createpropulsion.utility.burners.BurnerFuelBehaviour;
import com.deltasf.createpropulsion.utility.burners.IBurner;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public class HotAirBurnerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IBurner, IHotAirInjector {
    public static final double TREND_THRESHOLD = 0.001;
    private static final float PARTICLE_SPAWN_MULTIPLIER = 0.1f;

    // Behaviours
    private HotAirInjectorBehaviour injectorBehaviour;
    private BurnerFuelBehaviour fuelInventory;
    private AirInjectorObstructionBehaviour obstructionBehaviour;
    private BalloonInfoBehaviour balloonInfoBehaviour;

    private int burnTime = 0;
    private int leverPosition = 0; // 0-1-2
    private float particleAccumulator = 0;

    public HotAirBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        injectorBehaviour = new HotAirInjectorBehaviour(this);
        behaviours.add(injectorBehaviour);

        fuelInventory = new BurnerFuelBehaviour(this, () -> attemptScan());
        behaviours.add(fuelInventory);

        obstructionBehaviour = new AirInjectorObstructionBehaviour(this);
        behaviours.add(obstructionBehaviour);

        balloonInfoBehaviour = new BalloonInfoBehaviour(this, this::getId);
        behaviours.add(balloonInfoBehaviour);
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
        if (fuelInventory.fuelStack.isEmpty()) return;
        //Ship
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship == null) return; 
        //Balloon
        Balloon balloon = BalloonShipRegistry.forShip(ship.getId(), level).getBalloonOf(getId());
        if (balloon != null) return; 
        //Scan
        injectorBehaviour.performScan();
    }

    @Override
    public double getInjectionAmount() {
        if (burnTime <= 0) return 0; //Not burning = not producing hot air
        //Injection amount due to lever position
        double baseInjection = (leverPosition + 1) / 3.0;
        double efficiency = obstructionBehaviour.getEfficiency();
        return baseInjection * efficiency * PropulsionConfig.HOT_AIR_BURNER_PRODUCTION_MULTIPLIER.get();
    }

    @Override
    public void onBalloonLoaded() { balloonInfoBehaviour.performUpdate(); }

    // Lever and burner logic

    public void cycleLever(boolean isShiftPressed) {
        if (isShiftPressed) {
            leverPosition = Math.max(0, leverPosition - 1);
        } else {
            leverPosition = Math.min(2, leverPosition + 1);
        }

        notifyUpdate();
        attemptScan();
    }

    public int getLeverPosition() { return leverPosition;}

    public ItemStack getFuelStack() { return fuelInventory.fuelStack;}

    @Override
    public void setBurnTime(int burnTime) { this.burnTime = burnTime; }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide()) {
            spawnParticles(level);
            return;
        }

        //Burning logic
        if (burnTime > 0) {
            burnTime--;
        }

        if (burnTime <= 0) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
            Balloon balloon = (ship != null) ? BalloonShipRegistry.forShip(ship.getId()).getBalloonOf(getId()) : null;
            if (!fuelInventory.fuelStack.isEmpty() && balloon != null) {
                if (fuelInventory.tryConsumeFuel()) {
                    notifyUpdate();
                }
            }
        }
        
        updateBlockState();
    }

    private void spawnParticles(Level level) {
        double amount = getInjectionAmount();
        if (amount <= 0) return;

        Player player = Minecraft.getInstance().player;
        if (player == null || player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) > BalloonParticleSystem.SPAWN_RADIUS_SQ) {
            return;
        }
        
        int targetBalloonId = ClientBalloonRegistry.getBalloonIdForHai(getId());
        if (targetBalloonId == -1) return;
        
        particleAccumulator += amount * PARTICLE_SPAWN_MULTIPLIER;
        
        if (particleAccumulator >= 1.0f) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
            if (ship != null) {
                ShipParticleHandler handler = BalloonParticleSystem.getHandler(ship.getId());
                if (handler != null) {
                    
                    final float Y_MIN = 0.6f;
                    final float Y_MAX = 0.9f;
                    final float R_IN = 3.0f / 16.0f;
                    final float R_OUT = 7.0f / 16.0f;

                    float width = R_OUT - R_IN;
                    float wTopBot = 2.0f * R_OUT * width;
                    float wLeftRight = 2.0f * R_IN * width;
                    float totalArea = (2 * wTopBot) + (2 * wLeftRight);

                    double centerX = worldPosition.getX() + 0.5;
                    double centerY = worldPosition.getY();
                    double centerZ = worldPosition.getZ() + 0.5;
                    
                    while (particleAccumulator >= 1.0f) {
                        double localY = Y_MIN + level.random.nextFloat() * (Y_MAX - Y_MIN);
                        double offsetX, offsetZ;
                        float r = level.random.nextFloat() * totalArea;

                        if (r < wTopBot) {
                            // Top rect
                            offsetX = (level.random.nextFloat() * 2 * R_OUT) - R_OUT;
                            offsetZ = -R_OUT + (level.random.nextFloat() * width);
                        } else if (r < 2 * wTopBot) {
                            // Bottom rect
                            offsetX = (level.random.nextFloat() * 2 * R_OUT) - R_OUT;
                            offsetZ = R_IN + (level.random.nextFloat() * width);
                        } else if (r < 2 * wTopBot + wLeftRight) {
                            // Left rect
                            offsetX = -R_OUT + (level.random.nextFloat() * width);
                            offsetZ = (level.random.nextFloat() * 2 * R_IN) - R_IN;
                        } else {
                            // Right rect
                            offsetX = R_IN + (level.random.nextFloat() * width);
                            offsetZ = (level.random.nextFloat() * 2 * R_IN) - R_IN;
                        }
                        
                        handler.spawnStream(
                            centerX + offsetX, 
                            centerY + localY, 
                            centerZ + offsetZ, 
                            0.2f,
                            0.1f,
                            1.0f,
                            0.1f,
                            targetBalloonId
                        );
                        particleAccumulator -= 1.0f;
                    }
                }
            }
            if (particleAccumulator > 5.0f) particleAccumulator = 5.0f;
        }
    }

    @SuppressWarnings("null")
    private void updateBlockState() {
        boolean isBurning = burnTime > 0;
        if (getBlockState().getValue(HotAirBurnerBlock.LIT) != isBurning) {
            level.setBlock(worldPosition, getBlockState().setValue(HotAirBurnerBlock.LIT, isBurning), 3);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean hasFuel = !fuelInventory.fuelStack.isEmpty();
        boolean isBurning = burnTime > 0;
        boolean isAirless = level != null && DimensionAtmosphereManager.getData(level).isAirless();
        boolean isBalloonPresent = balloonInfoBehaviour.isBalloonPresent();

        //Status
        String key = "";
        ChatFormatting color = null;

        //Airless -> Not on ship -> No balloon / Fuel -> Valid statuses
        if (isAirless) {
            key = "gui.goggles.hot_air_burner.status.airless";
            color = ChatFormatting.RED;
        } else if (!VSGameUtilsKt.isBlockInShipyard(getLevel(), getBlockPos())) {
            key = "gui.goggles.hot_air_burner.status.not_shipified";
            color = ChatFormatting.RED;
        } else if (!isBalloonPresent) {
            if (hasFuel) {
                key = "gui.goggles.hot_air_burner.status.no_balloon";
                color = ChatFormatting.DARK_GRAY;
            } else {
                key = "gui.goggles.hot_air_burner.status.no_fuel";
                color = ChatFormatting.GOLD;
            }
        } else if (isBurning) {
            key = "gui.goggles.hot_air_burner.status.on";
            color = ChatFormatting.GREEN;
        } else if (hasFuel) {
            key = "gui.goggles.hot_air_burner.status.ready";
            color = ChatFormatting.GRAY;
        } else {
            key = "gui.goggles.hot_air_burner.status.no_fuel";
            color = ChatFormatting.GOLD;
        }

        CreateLang.builder()
            .add(CreateLang.translate("gui.goggles.hot_air_burner.status"))
            .text(": ")
            .add(CreateLang.translate(key).style(color))
            .forGoggles(tooltip);

        //Fuel info
        ItemStack fuel = fuelInventory.fuelStack;
        if (hasFuel) {
            LangBuilder fuelName = CreateLang.builder().add(fuel.getHoverName()).style(ChatFormatting.GRAY);
            LangBuilder fuelCount = CreateLang.builder().text("x").text(String.valueOf(fuel.getCount())).style(ChatFormatting.GREEN);

            CreateLang.builder().add(fuelName).space().add(fuelCount).forGoggles(tooltip);
        }

        //Balloon section
        if (isAirless) {
            return true;
        }

        if (hasFuel && isBalloonPresent) {
            CreateLang.text("").forGoggles(tooltip);
        }

        balloonInfoBehaviour.addBalloonTooltip(tooltip, isPlayerSneaking);
        obstructionBehaviour.displayObstructionOutline("HotAirBurnerObstruction");
        return true;
    }

    // Caps and nbt

    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return fuelInventory.getCapability(cap);
        return super.getCapability(cap, side);
    }

    @Override
    protected void write(CompoundTag tag, boolean isClient) {
        super.write(tag, isClient);
        tag.putInt("leverPosition", leverPosition);
        tag.putInt("burnTime", burnTime);
    }

    @Override
    protected void read(CompoundTag tag, boolean isClient) {
        super.read(tag, isClient);
        leverPosition = tag.getInt("leverPosition");
        burnTime = tag.getInt("burnTime");
    }
}
