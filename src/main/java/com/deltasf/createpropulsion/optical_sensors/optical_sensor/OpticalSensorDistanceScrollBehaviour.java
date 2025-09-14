package com.deltasf.createpropulsion.optical_sensors.optical_sensor;

import org.joml.Math;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.optical_sensors.AbstractOpticalSensorBlockEntity;
import com.deltasf.createpropulsion.registries.PropulsionItems;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class OpticalSensorDistanceScrollBehaviour extends ScrollValueBehaviour {
    public OpticalSensorDistanceScrollBehaviour(SmartBlockEntity be) {
        super(Lang.builder().translate("gui.optical_sensor.distance_behaviour", new Object[0]).component(), be, new OpticalSensorDistanceValueBox());
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> row = ImmutableList.of(Lang.builder().text("\u2191").component());
        var be = (AbstractOpticalSensorBlockEntity)getWorld().getBlockEntity(getPos());
        int maxDistance;
        if (be != null) {
            boolean isFocused = be.hasLens(PropulsionItems.FOCUS_LENS.get());
            boolean isUnfocused = be.hasLens(PropulsionItems.UNFINISHED_LENS.get());
            maxDistance = (int)Math.floor(PropulsionConfig.OPTICAL_SENSOR_MAX_DISTANCE.get() * (isFocused ? 2 : 1) * (isUnfocused ? 0.5 : 1));
        } else {
            maxDistance = PropulsionConfig.OPTICAL_SENSOR_MAX_DISTANCE.get();
        }
        return new ValueSettingsBoard(label, maxDistance, 8, row, new ValueSettingsFormatter(this::formatValue));
    }

    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlHeld) {
        int value = Math.max(1, valueSetting.value());
        if (!valueSetting.equals(getValueSettings()))
				playFeedbackSound(this);
        setValue(value);
    }

    @Override 
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, value);
    }

    public MutableComponent formatValue(ValueSettings settings) {
        return Lang.builder()
            .add(Lang.number(Math.max(1, settings.value())))
            .text("\u2191")
            .component();
    }
}
