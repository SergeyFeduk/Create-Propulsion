package com.deltasf.createpropulsion.balloons.injectors;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class BalloonInfoBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<BalloonInfoBehaviour> TYPE = new BehaviourType<>();
    public static final double TREND_THRESHOLD = 0.001;

    private final Supplier<UUID> idSupplier;

    //Display data
    private double lastTickBalloonHotAir = -1;
    private int hotAirTrend = 0;
    private boolean isBalloonPresent = false;
    private int balloonHotAir = 0;
    private int balloonMaxHotAir = 0;
    private int balloonHotAirPercentage = 0;

    public BalloonInfoBehaviour(SmartBlockEntity be, Supplier<UUID> idSupplier) {
        super(be);
        this.idSupplier = idSupplier;
    }

    public boolean isBalloonPresent() {
        return isBalloonPresent;
    }

    @Override
    public void tick() {
        super.tick();
        performUpdate();
    }

    public void performUpdate() {
        Level level = getWorld();
        if (level.isClientSide()) return;

        boolean needsSync = false;
        int oldTrend = hotAirTrend;
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, getPos());

        Balloon balloon = (ship != null) ? BalloonShipRegistry.forShip(ship.getId(), level).getBalloonOf(idSupplier.get()) : null;
        
        if (balloon != null) {
            int currentHotAir = (int) balloon.hotAir;
            int currentMaxHotAir = (int) balloon.getVolumeSize();
            double percentage = currentMaxHotAir > 0 ? (balloon.hotAir / currentMaxHotAir) : 0;
            int currentPercentage = (int) (percentage * 100);

            //Update trend
            if (lastTickBalloonHotAir != -1) {
                double delta = balloon.hotAir - lastTickBalloonHotAir;
                if (Math.abs(delta) < TREND_THRESHOLD) hotAirTrend = 0;
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
            blockEntity.notifyUpdate();
        }
    }

    public void addBalloonTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!isBalloonPresent) return;

        CreateLang.builder()
            .translate("gui.goggles.hot_air_burner.balloon.status")
            .text(":")
            .forGoggles(tooltip);

        Component trendSymbol;
        switch (hotAirTrend) {
            case 1 -> trendSymbol = CreateLang.text("▲").style(ChatFormatting.GREEN).component();
            case -1 -> trendSymbol = CreateLang.text("▼").style(ChatFormatting.RED).component();
            default -> trendSymbol = CreateLang.text("■").style(ChatFormatting.DARK_GRAY).component();
        }

        var hotAirBuilder = CreateLang.builder()
            .translate("gui.goggles.hot_air_burner.balloon.hot_air")
            .text(String.format(": %d / %d", balloonHotAir, balloonMaxHotAir));
        
        if (isPlayerSneaking) {
            hotAirBuilder.text(String.format(" (%d%%)", balloonHotAirPercentage));
        }

        hotAirBuilder.text(" ")
            .add(CreateLang.text("[").style(ChatFormatting.DARK_GRAY))
            .add(trendSymbol)
            .add(CreateLang.text("]").style(ChatFormatting.DARK_GRAY))
            .forGoggles(tooltip);
    }

    @Override
    public void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if (clientPacket) {
            tag.putInt("hotAirTrend", hotAirTrend);
            tag.putBoolean("isBalloonPresent", isBalloonPresent);
            tag.putInt("balloonHotAir", balloonHotAir);
            tag.putInt("balloonMaxHotAir", balloonMaxHotAir);
            tag.putInt("balloonHotAirPercentage", balloonHotAirPercentage);
        }
    }

    @Override
    public void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (clientPacket) {
            hotAirTrend = tag.getInt("hotAirTrend");
            isBalloonPresent = tag.getBoolean("isBalloonPresent");
            balloonHotAir = tag.getInt("balloonHotAir");
            balloonMaxHotAir = tag.getInt("balloonMaxHotAir");
            balloonHotAirPercentage = tag.getInt("balloonHotAirPercentage");
        }
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
