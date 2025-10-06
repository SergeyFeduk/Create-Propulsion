package com.deltasf.createpropulsion.propeller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.atmosphere.DimensionAtmosphereManager;
import com.deltasf.createpropulsion.propeller.blades.PropellerBladeItem;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class PropellerBlockEntity extends KineticBlockEntity {
    public static final int MAX_THRUST = 20000;
    public static final int MAX_EFFECTIVE_SPEED = 256;

    protected PropellerData propellerData;
    protected final ItemStackHandler bladeInventory;
    private LazyOptional<IItemHandler> itemHandler;

    public List<Float> targetBladeAngles = new ArrayList<>();
    //Client-side animation state
    @OnlyIn(Dist.CLIENT)
    public List<Float> prevBladeAngles;
    @OnlyIn(Dist.CLIENT)
    public List<Float> renderedBladeAngles;
    @OnlyIn(Dist.CLIENT)
    public long animationStartTime;

    @OnlyIn(Dist.CLIENT)
    public float visualRPM = 0f;
    @OnlyIn(Dist.CLIENT)
    public float visualAngle = 0f;
    @OnlyIn(Dist.CLIENT)
    public long lastRenderTimeNanos = 0;
    @OnlyIn(Dist.CLIENT)
    private boolean hasLoadedClientState = false;

    public PropellerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        propellerData = new PropellerData();
        bladeInventory = createBladeInventory();
        itemHandler = LazyOptional.of(() -> bladeInventory);
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        super.initialize();
        if (level.isClientSide) {
            if (prevBladeAngles == null) prevBladeAngles = new ArrayList<>();
            if (renderedBladeAngles == null) renderedBladeAngles = new ArrayList<>();
        } else {
            BlockState state = getBlockState();

            PropellerAttachment ship = PropellerAttachment.get(level, worldPosition);
            if (ship != null) {
                propellerData.setDirection(VectorConversionsMCKt.toJOMLD(state.getValue(PropellerBlock.FACING).getNormal()));
                propellerData.setThrust(0);
                propellerData.setAtmosphere(DimensionAtmosphereManager.getData(level));
                PropellerForceApplier applier = new PropellerForceApplier(propellerData);
                ship.addApplier(worldPosition, applier);
            }
            updateThrust();
        }
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        if (getSpeed() != prevSpeed) {
            updateThrust();
        }
    }

    public float getTargetRPM() {
        int bladeCount = getBladeCount();
        //Having only one blade makes propeller unbalanced
        if (bladeCount == 1) {
            return 0f;
        }
        //No blades - head rotates
        if (bladeCount == 0) {
            return Math.abs(getSpeed()) * 8.0f;
        }

        ItemStack bladeStack = bladeInventory.getStackInSlot(0);
        if (!bladeStack.isEmpty() && bladeStack.getItem() instanceof PropellerBladeItem bladeItem) {
            return Math.abs(getSpeed()) * bladeItem.getGearRatio();
        }
        return 0f;
    }


    @SuppressWarnings("null")
    public void updateThrust() {
        if (level == null || level.isClientSide) {
            return;
        }

        float speed = Math.abs(getSpeed());
        float thrust = 0;

        if (speed > 0) {
            // Calculate thrust based on speed, up to the max effective speed.
            float speedPercentage = Math.min(speed / (float)MAX_EFFECTIVE_SPEED, 1.0f);
            thrust = MAX_THRUST * speedPercentage;
        }

        propellerData.setThrust(thrust);
    }

    // Blades

    public int getBladeCount() {
        int count = 0;
        for (int i = 0; i < bladeInventory.getSlots(); i++) {
            if (!bladeInventory.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public boolean addBlade(ItemStack bladeStack, Vec3 localHit) {
        if (!(bladeStack.getItem() instanceof PropellerBladeItem bladeItem))
            return false;

        int currentBlades = getBladeCount();
        if (currentBlades >= bladeItem.getMaxBlades())
            return false; // Reached max for this type

        ItemStack firstBlade = ItemStack.EMPTY;
        for (int i = 0; i < bladeInventory.getSlots(); i++) {
            if (!bladeInventory.getStackInSlot(i).isEmpty()) {
                firstBlade = bladeInventory.getStackInSlot(i);
                break;
            }
        }

        if (!firstBlade.isEmpty() && !firstBlade.is(bladeItem))
            return false;

        for (int i = 0; i < bladeInventory.getSlots(); i++) {
            if (bladeInventory.getStackInSlot(i).isEmpty()) {
                ItemStack newBlade = bladeStack.copy();
                newBlade.setCount(1);
                bladeInventory.setStackInSlot(i, newBlade);

                //Animate insertion
                updateTargetAngles(localHit);
                sendData();
                setChanged();
                return true;
            }
        }

        return false;
    }

    public ItemStack removeBlade() {
        for (int i = bladeInventory.getSlots() - 1; i >= 0; i--) {
            ItemStack stackInSlot = bladeInventory.getStackInSlot(i);
            if (!stackInSlot.isEmpty()) {
                ItemStack removedBlade = stackInSlot.copy();
                bladeInventory.setStackInSlot(i, ItemStack.EMPTY);
                //Animate removal
                updateTargetAngles(null);
                sendData();
                setChanged();
                return removedBlade;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStackHandler createBladeInventory() {
        // Max blades for any type is 6
        return new ItemStackHandler(6) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    private void updateTargetAngles(@Nullable Vec3 localHit) {
        int bladeCount = getBladeCount();
        if (bladeCount == 0) {
            targetBladeAngles.clear();
            return;
        }

        List<Float> finalAngles = new ArrayList<>();
        for (int i = 0; i < bladeCount; i++) {
            finalAngles.add(i * 360f / bladeCount);
        }
        targetBladeAngles = finalAngles;
    }

    //NBT and caps

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.put("Blades", bladeInventory.serializeNBT());

        ListTag angleNBT = new ListTag();
        for (Float angle : targetBladeAngles) {
            angleNBT.add(FloatTag.valueOf(angle));
        }
        compound.put("TargetAngles", angleNBT);
    }

    @SuppressWarnings("null")
    @Override
    public void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        bladeInventory.deserializeNBT(compound.getCompound("Blades"));

        ListTag angleNBT = compound.getList("TargetAngles", 5);
        List<Float> newTargetAngles = new ArrayList<>();
        for (int i = 0; i < angleNBT.size(); i++) {
            newTargetAngles.add(angleNBT.getFloat(i));
        }

        if (level != null && level.isClientSide) {
            if (!hasLoadedClientState) {
                this.targetBladeAngles = newTargetAngles;
                if (prevBladeAngles == null) prevBladeAngles = new ArrayList<>();
                if (renderedBladeAngles == null) renderedBladeAngles = new ArrayList<>();
                this.prevBladeAngles.clear();
                this.prevBladeAngles.addAll(newTargetAngles);
                this.renderedBladeAngles.clear();
                this.renderedBladeAngles.addAll(newTargetAngles);
                this.animationStartTime = 0;
                
                if (!newTargetAngles.isEmpty()) {
                    hasLoadedClientState = true;
                }
            } else {
                if (!newTargetAngles.equals(this.targetBladeAngles)) {
                    boolean isRemoval = newTargetAngles.size() < this.targetBladeAngles.size();
                    if (isRemoval) {
                        this.prevBladeAngles = new ArrayList<>(this.targetBladeAngles.subList(0, newTargetAngles.size()));
                    } else {
                        this.prevBladeAngles = new ArrayList<>(this.targetBladeAngles);
                    }
                    this.targetBladeAngles = newTargetAngles;
                    while (this.renderedBladeAngles.size() > this.targetBladeAngles.size()) this.renderedBladeAngles.remove(this.renderedBladeAngles.size() - 1);
                    while (this.renderedBladeAngles.size() < this.targetBladeAngles.size()) this.renderedBladeAngles.add(0f);
                    if (!isRemoval && this.prevBladeAngles.size() < this.targetBladeAngles.size()) {
                        int newBladeIndex = this.targetBladeAngles.size() - 1;
                        this.prevBladeAngles.add(this.targetBladeAngles.get(newBladeIndex));
                    }
                    this.animationStartTime = System.nanoTime();
                }
            }
        } else {
            this.targetBladeAngles = newTargetAngles;
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        itemHandler.invalidate();
    }
}