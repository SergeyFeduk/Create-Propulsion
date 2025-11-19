package com.deltasf.createpropulsion.utility.value_boxes;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

public class DualRowValueBehaviour extends BlockEntityBehaviour implements ValueSettingsBehaviour {
	public static final BehaviourType<DualRowValueBehaviour> TYPE = new BehaviourType<>();

	ValueBoxTransform slotPositioning;
	public Component label;
	public int value;

	private Consumer<Integer> callback;
	private Supplier<Boolean> isActive;
	private boolean needsWrench;
	private Function<Integer, String> formatter;

	private static final int MAX_ABSOLUTE_VALUE = 256;

	public DualRowValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
		super(be);
		this.label = label;
		this.slotPositioning = slot;
		this.callback = i -> {};
		this.isActive = () -> true;
		this.formatter = i -> Integer.toString(i);
		this.value = 1;
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}
	
	@Override
	public boolean isSafeNBT() {
		return true;
	}

	@Override
	public void write(CompoundTag nbt, boolean clientPacket) {
		nbt.putInt("DualRowValue", value);
		super.write(nbt, clientPacket);
	}

	@Override
	public void read(CompoundTag nbt, boolean clientPacket) {
		value = nbt.getInt("DualRowValue");
		super.read(nbt, clientPacket);
	}
	
	public DualRowValueBehaviour withCallback(Consumer<Integer> valueCallback) {
		this.callback = valueCallback;
		return this;
	}

	public DualRowValueBehaviour requiresWrench() {
		this.needsWrench = true;
		return this;
	}

	public DualRowValueBehaviour withFormatter(Function<Integer, String> formatter) {
		this.formatter = formatter;
		return this;
	}

	public DualRowValueBehaviour onlyActiveWhen(Supplier<Boolean> condition) {
		this.isActive = condition;
		return this;
	}
	
	public void setValue(int newValue) {
		if (newValue == 0)
			newValue = 1;
		
		newValue = Mth.clamp(newValue, -MAX_ABSOLUTE_VALUE, MAX_ABSOLUTE_VALUE);
		
		if (newValue == this.value)
			return;
			
		this.value = newValue;
		callback.accept(value);
		blockEntity.setChanged();
		blockEntity.sendData();
	}

	public int getValue() {
		return value;
	}
	
	public String formatValue() {
		return formatter.apply(value);
	}

	@Override
	public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
		List<Component> rowLabels = ImmutableList.of(Components.literal("-"), Components.literal("+"));

		ValueSettingsFormatter boardFormatter = new ValueSettingsFormatter((settings) -> {
			if (settings.row() == 0) {
				return Lang.number(-(settings.value() + 1)).component();
			}
			return Lang.number(settings.value() + 1).component();
		});

		return new ValueSettingsBoard(label, MAX_ABSOLUTE_VALUE, 1, rowLabels, boardFormatter);
	}

	@Override
	public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlDown) {
		int newValue;
		if (valueSetting.row() == 0) {
			newValue = -(valueSetting.value() + 1);
		} else {
			newValue = valueSetting.value() + 1;
		}

		if (this.value == newValue)
			return;

		setValue(newValue);
		playFeedbackSound(this);
	}

	@Override
	public ValueSettings getValueSettings() {
		if (value < 0) {
			int index = -value - 1;
			return new ValueSettings(0, index);
		}
		
		int index = value - 1;
		return new ValueSettings(1, index);
	}

	@Override
	public boolean isActive() {
		return isActive.get();
	}
	
	@Override
	public boolean onlyVisibleWithWrench() {
		return needsWrench;
	}

	@Override
	public ValueBoxTransform getSlotPositioning() {
		return slotPositioning;
	}

	@Override
	public boolean testHit(Vec3 hit) {
		BlockState state = blockEntity.getBlockState();
		Vec3 localHit = hit.subtract(Vec3.atLowerCornerOf(blockEntity.getBlockPos()));
		return slotPositioning.testHit(state, localHit);
	}

	@Override
	public void onShortInteract(Player player, InteractionHand hand, Direction side) {
		if (player instanceof FakePlayer)
			blockEntity.getBlockState()
				.use(getWorld(), player, hand,
					new BlockHitResult(VecHelper.getCenterOf(getPos()), side, getPos(), true));
	}
}
