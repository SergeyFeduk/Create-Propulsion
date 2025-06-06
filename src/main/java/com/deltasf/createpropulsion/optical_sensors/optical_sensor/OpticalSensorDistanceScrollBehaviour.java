package com.deltasf.createpropulsion.optical_sensors.optical_sensor;

import com.deltasf.createpropulsion.Config;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class OpticalSensorDistanceScrollBehaviour extends ScrollValueBehaviour {
    public OpticalSensorDistanceScrollBehaviour(SmartBlockEntity be) {
        super(CreateLang.builder().translate("gui.optical_sensor.distance_behaviour", new Object[0]).component(), be, new OpticalSensorDistanceValueBox());
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> row = ImmutableList.of(CreateLang.builder().text("\u2191").component());
        return new ValueSettingsBoard(label, Config.OPTICAL_SENSOR_MAX_DISTANCE.get(), 8, row, new ValueSettingsFormatter(this::formatValue));
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
        return CreateLang.builder()
            .add(CreateLang.number(Math.max(1, settings.value())))
            .text("\u2191")
            .component();
    }
}
