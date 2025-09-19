package com.deltasf.createpropulsion.heat.burners.solid;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class FuelItemHandler extends ItemStackHandler {
    private final FuelInventoryBehaviour behaviour;
    public FuelItemHandler(FuelInventoryBehaviour behaviour) {
        super(1);
        this.behaviour = behaviour;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 8;
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot != 0 || !isItemValid(slot, stack)) {
            return stack;
        }

        if (behaviour.fuelStack.isEmpty()) {
            int amount = Math.min(stack.getCount(), getSlotLimit(slot));
            if (!simulate) {
                behaviour.fuelStack = ItemHandlerHelper.copyStackWithSize(stack, amount);
                behaviour.blockEntity.notifyUpdate();
            }
            return ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - amount);
        } else {
            if (!ItemHandlerHelper.canItemStacksStack(behaviour.fuelStack, stack)) {
                return stack;
            }
            int space = getSlotLimit(slot) - behaviour.fuelStack.getCount();
            int amount = Math.min(stack.getCount(), space);
            if (!simulate) {
                behaviour.fuelStack.grow(amount);
                behaviour.blockEntity.notifyUpdate();
            }
            return ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - amount);
        }
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != 0 || amount <= 0 || behaviour.fuelStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int toExtract = Math.min(amount, behaviour.fuelStack.getCount());
        ItemStack extracted = behaviour.fuelStack.copy();
        extracted.setCount(toExtract);
        if (!simulate) {
            behaviour.fuelStack.shrink(toExtract);
            if (behaviour.fuelStack.isEmpty()) {
                behaviour.fuelStack = ItemStack.EMPTY;
            }
            behaviour.blockEntity.notifyUpdate();
        }
        return extracted;
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? behaviour.fuelStack : ItemStack.EMPTY;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return ForgeHooks.getBurnTime(stack, null) > 0;
    }
}
