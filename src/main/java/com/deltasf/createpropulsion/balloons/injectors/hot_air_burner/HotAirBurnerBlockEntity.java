package com.deltasf.createpropulsion.balloons.injectors.hot_air_burner;

import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.atmosphere.DimensionAtmosphereManager;
import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.injectors.AbstractHotAirInjectorBlockEntity;
import com.deltasf.createpropulsion.balloons.injectors.AirInjectorObstructionBehaviour;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.physics_assembler.AssemblyUtility;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.outliner.Outline.OutlineParams;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public class HotAirBurnerBlockEntity extends AbstractHotAirInjectorBlockEntity implements IHaveGoggleInformation {
    public static final double TREND_THRESHOLD = 0.001;

    private HotAirBurnerFuelBehaviour fuelInventory;
    private AirInjectorObstructionBehaviour obstructionBehaviour;
    private int burnTime = 0;
    private int leverPosition = 0; // 0-1-2

    //Goggle display data
    private double lastTickBalloonHotAir = -1;
    private int hotAirTrend = 0;
    private boolean isBalloonPresent = false;
    private int balloonHotAir = 0;
    private int balloonMaxHotAir = 0;
    private int balloonHotAirPercentage = 0;

    public HotAirBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        fuelInventory = new HotAirBurnerFuelBehaviour(this);
        behaviours.add(fuelInventory);
        obstructionBehaviour = new AirInjectorObstructionBehaviour(this);
        behaviours.add(obstructionBehaviour);
    }

    public void cycleLever(boolean isShiftPressed) {
        if (isShiftPressed) {
            leverPosition = Math.max(0, leverPosition - 1);
        } else {
            leverPosition = Math.min(2, leverPosition + 1);
        }

        notifyUpdate();
        attemptScan();
    }

    public int getLeverPosition() {
        return leverPosition;
    }

    public ItemStack getFuelStack() {
        return fuelInventory.fuelStack;
    }

    @Override
    public double getInjectionAmount() {
        if (burnTime <= 0) return 0; //Not burning = not producing hot air
        //Injection amount due to lever position
        double baseInjection = (leverPosition + 1) / 3.0;

        //Injection penalty due to obstruction
        int obstructions = obstructionBehaviour.getObstructedBlocks().size();
        double efficiency;
        switch (obstructions) {
            case 0:  efficiency = 1.0; break;
            case 1:  efficiency = 2.0 / 3.0; break;
            case 2:  efficiency = 1.0 / 3.0; break;
            default: efficiency = 0.0; break;
        }
        return baseInjection * efficiency;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    @SuppressWarnings("null")
    public void attemptScan() {
        if (level == null || level.isClientSide()) return;
        if (fuelInventory.fuelStack.isEmpty()) return; //No fuel - no need to perform scan
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship == null) return; //No ship - no balloon possible

        Balloon balloon = BalloonShipRegistry.forShip(ship.getId(), level).getBalloonOf(this.haiId);
        if (balloon != null) return; //There is a balloon - no need to rescan

        scan();
    }

    @Override
    public void onBalloonLoaded() {
        updateGoggleData();
    }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide()) return;

        updateGoggleData();

        //Burning logic
        if (burnTime > 0) {
            burnTime--;
        }

        if (burnTime <= 0) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
            Balloon balloon = (ship != null) ? BalloonShipRegistry.forShip(ship.getId()).getBalloonOf(this.haiId) : null;
            if (!fuelInventory.fuelStack.isEmpty() && balloon != null) {
                if (fuelInventory.tryConsumeFuel()) {
                    notifyUpdate();
                }
            }
        }
        
        updateBlockState();
    }

    private void updateGoggleData() {
        boolean needsSync = false;
        //Trend logic
        int oldTrend = hotAirTrend;
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);

        Balloon balloon = (ship != null) ? BalloonShipRegistry.forShip(ship.getId()).getBalloonOf(this.haiId) : null;
        if (balloon != null) {
            int currentHotAir = (int) balloon.hotAir;
            int currentMaxHotAir = (int) balloon.getVolumeSize();
            double percentage = currentMaxHotAir > 0 ? (balloon.hotAir / currentMaxHotAir) : 0;
            int currentPercentage = (int) (percentage * 100);

            //Update trend
            if (lastTickBalloonHotAir != -1) {
                double delta = balloon.hotAir - lastTickBalloonHotAir;
                if (Math.abs(delta) < HotAirBurnerBlockEntity.TREND_THRESHOLD) hotAirTrend = 0;
                else if (delta > 0) hotAirTrend = 1;
                else hotAirTrend = -1;
            } else {
                hotAirTrend = 0;
            }
            lastTickBalloonHotAir = balloon.hotAir;

            //Check if something changed
            if (!isBalloonPresent || balloonHotAir != currentHotAir || balloonMaxHotAir != currentMaxHotAir 
                || balloonHotAirPercentage != currentPercentage || hotAirTrend != oldTrend) {
                needsSync = true;
            }

            //Update state
            isBalloonPresent = true;
            balloonHotAir = currentHotAir;
            balloonMaxHotAir = currentMaxHotAir;
            balloonHotAirPercentage = currentPercentage;
        } else {
            if (isBalloonPresent) {
                needsSync = true;
            }

            isBalloonPresent = false;
            lastTickBalloonHotAir = -1;
            hotAirTrend = 0;
        }

        if (needsSync) {
            notifyUpdate();
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

        //Status
        String key = "";
        ChatFormatting color = null;

        if (isAirless) {
            key = "gui.goggles.hot_air_burner.status.airless";
            color = ChatFormatting.RED;
        } else if (!isBalloonPresent) {
            key = "gui.goggles.hot_air_burner.status.no_balloon";
            color = ChatFormatting.DARK_GRAY;
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

        Lang.builder()
            .add(Lang.translate("gui.goggles.hot_air_burner.status"))
            .text(": ")
            .add(Lang.translate(key).style(color))
            .forGoggles(tooltip);

        //Fuel info
        ItemStack fuel = fuelInventory.fuelStack;
        if (hasFuel) {
            LangBuilder fuelName = Lang.builder().add(fuel.getHoverName()).style(ChatFormatting.GRAY);
            LangBuilder fuelCount = Lang.builder().text("x").text(String.valueOf(fuel.getCount())).style(ChatFormatting.GREEN);

            Lang.builder().add(fuelName).space().add(fuelCount).forGoggles(tooltip);
        }

        //Balloon section
        if (!isBalloonPresent || isAirless) {
            return true;
        }

        if (hasFuel) {
            Lang.text("").forGoggles(tooltip);
        }
        
        Lang.builder()
            .translate("gui.goggles.hot_air_burner.balloon.status")
            .forGoggles(tooltip);

        Component trendSymbol;
        switch (hotAirTrend) {
            case 1 -> trendSymbol = Lang.text("▲").style(ChatFormatting.GREEN).component();
            case -1 -> trendSymbol = Lang.text("▼").style(ChatFormatting.RED).component();
            default -> trendSymbol = Lang.text("■").style(ChatFormatting.DARK_GRAY).component();
        }

        var hotAirBuilder = Lang.builder()
            .translate("gui.goggles.hot_air_burner.balloon.hot_air")
            .text(String.format(": %d / %d", balloonHotAir, balloonMaxHotAir));
        
        if (isPlayerSneaking) {
            hotAirBuilder.text(String.format(" (%d%%)", balloonHotAirPercentage));
        }

        hotAirBuilder.text(" ")
            .add(Lang.text("[").style(ChatFormatting.DARK_GRAY))
            .add(trendSymbol)
            .add(Lang.text("]").style(ChatFormatting.DARK_GRAY))
            .forGoggles(tooltip);

        //Obstruction overlay
        OutlineParams outline = CreateClient.OUTLINER.showCluster("HotAirBurnerObstruction", obstructionBehaviour.getObstructedBlocks());
        outline.colored(AssemblyUtility.CANCEL_COLOR);
        outline.lineWidth(1/16f);
        outline.withFaceTexture(AllSpecialTextures.CHECKERED);
        outline.disableLineNormals();

        return true;
    }

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

        if (isClient) {
            tag.putInt("hotAirTrend", hotAirTrend);
            tag.putBoolean("isBalloonPresent", isBalloonPresent);
            tag.putInt("balloonHotAir", balloonHotAir);
            tag.putInt("balloonMaxHotAir", balloonMaxHotAir);
            tag.putInt("balloonHotAirPercentage", balloonHotAirPercentage);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean isClient) {
        super.read(tag, isClient);
        leverPosition = tag.getInt("leverPosition");
        burnTime = tag.getInt("burnTime");

        if (isClient) {
            hotAirTrend = tag.getInt("hotAirTrend");
            isBalloonPresent = tag.getBoolean("isBalloonPresent");
            balloonHotAir = tag.getInt("balloonHotAir");
            balloonMaxHotAir = tag.getInt("balloonMaxHotAir");
            balloonHotAirPercentage = tag.getInt("balloonHotAirPercentage");
        }
    }
}
