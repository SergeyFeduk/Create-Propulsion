package com.deltasf.createpropulsion.utility.burners;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public class BurnerFuelBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<BurnerFuelBehaviour> TYPE = new BehaviourType<>();

    private BurnerFuelItemHandler itemHandler;
    private LazyOptional<IItemHandler> capability;
    public ItemStack fuelStack = ItemStack.EMPTY;
    private final Runnable onInsertion;

    public BurnerFuelBehaviour(SmartBlockEntity be, Runnable onInsertion) {
        super(be);
        this.itemHandler = new BurnerFuelItemHandler(this);
        this.capability = LazyOptional.of(() -> this.itemHandler);
        this.onInsertion = onInsertion;
    }

    public <T> LazyOptional<T> getCapability(Capability<T> cap) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return this.capability.cast();
        }
        return LazyOptional.empty();
    }

    public boolean tryConsumeFuel() {
        if (fuelStack.isEmpty()) return false;
        
        int burnTime = ForgeHooks.getBurnTime(fuelStack, RecipeType.SMELTING);
        if (burnTime > 0 && blockEntity instanceof IBurner burner) {
            burner.setBurnTime(burnTime);
            fuelStack.shrink(1);
            if (fuelStack.isEmpty()) {
                fuelStack = ItemStack.EMPTY;
            }
            return true;
        }
        return false;
    }


    public boolean handlePlayerInteraction(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(blockEntity instanceof IBurner)) return false;

        if (stack.isEmpty() && !fuelStack.isEmpty()) {
            player.getInventory().placeItemBackInInventory(itemHandler.extractItem(0, 64, false));
            blockEntity.notifyUpdate();
            return true;
        }

        if (!stack.isEmpty() && itemHandler.isItemValid(0, stack)) {
            ItemStack remainder = itemHandler.insertItem(0, stack, false);
            player.setItemInHand(hand, remainder);
            if (remainder.getCount() != stack.getCount()) {
                blockEntity.notifyUpdate();
                if (onInsertion != null) onInsertion.run();
                return true;
            }
        }

        return false;
    }

    //NBT

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        nbt.put("Fuel", fuelStack.save(new CompoundTag()));
        super.write(nbt, clientPacket);
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        fuelStack = ItemStack.of(nbt.getCompound("Fuel"));
        super.read(nbt, clientPacket);
    }

    @Override
    public void unload() {
        super.unload();
        capability.invalidate();
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
