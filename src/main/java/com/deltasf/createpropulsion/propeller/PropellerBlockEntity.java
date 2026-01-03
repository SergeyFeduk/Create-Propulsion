package com.deltasf.createpropulsion.propeller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.atmosphere.DimensionAtmosphereManager;
import com.deltasf.createpropulsion.physics_assembler.AssemblyUtility;
import com.deltasf.createpropulsion.propeller.blades.PropellerBladeItem;
import com.deltasf.createpropulsion.propeller.rendering.PropellerRenderer;
import com.deltasf.createpropulsion.utility.math.MathUtility;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class PropellerBlockEntity extends KineticBlockEntity {
    public static final int MAX_THRUST = 20000;
    public static final int MAX_TORQUE = 4000; //This is quite high, but I want it to be noticeable
    public static final int MAX_EFFECTIVE_SPEED = 256;

    protected PropellerData propellerData;
    protected final ItemStackHandler bladeInventory;
    private LazyOptional<IItemHandler> itemHandler;
    private PropellerSpatialHandler spatialHandler;

    public List<Float> targetBladeAngles = new ArrayList<>();
    protected boolean isClockwise = true;
    protected float lastFluidSample = 0;

    //Server-side rpm sim
    public float internalRPM = 0f;

    //Client-side animation state
    public List<Float> prevBladeAngles;
    public List<Float> renderedBladeAngles;
    public float animationStartTime;

    public float visualRPM = 0f;
    public float visualAngle = 0f;
    public float lastRenderTimeSeconds = 0;
    private boolean hasLoadedClientState = false;

    public PropellerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        propellerData = new PropellerData();
        bladeInventory = createBladeInventory();
        itemHandler = LazyOptional.of(() -> bladeInventory);

        prevBladeAngles = new ArrayList<>();
        renderedBladeAngles = new ArrayList<>();
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        super.initialize();
        internalRPM = getTargetRPM();

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

    public boolean IsClockwise() {
        return isClockwise;
    }

    @Override
    public float calculateStressApplied() {
        float stress = 8;
        if (getBlade().isEmpty()) {
            this.lastStressApplied = stress;
            return stress;
        }
        PropellerBladeItem blade = getBlade().get(); 
        float bladeStressPercent = getBladeCount() / (float)blade.getMaxBlades();

        stress = getBlade().get().getStressImpact() * bladeStressPercent;
        this.lastStressApplied = stress;
        return stress;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(new PropellerDamager(this));
        spatialHandler = new PropellerSpatialHandler(this);
        behaviours.add(spatialHandler);
    }

    public PropellerSpatialHandler getSpatialHandler() {
        return spatialHandler;
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
            return getSpeed() * 8.0f;
        }

        ItemStack bladeStack = bladeInventory.getStackInSlot(0);
        if (!bladeStack.isEmpty() && bladeStack.getItem() instanceof PropellerBladeItem bladeItem) {
            return getSpeed() * bladeItem.getGearRatio();
        }
        return 0f;
    }

    public float getInternalRPM() {
        return internalRPM;
    }

    public float getPowerPercent() {
        Optional<PropellerBladeItem> bladeOptional = getBlade();
        if (bladeOptional.isEmpty()) return 0f;
        
        float maxTargetRPM = MAX_EFFECTIVE_SPEED * bladeOptional.get().getGearRatio();
        if (maxTargetRPM == 0) return 0f;

        return Mth.clamp(Math.abs(this.internalRPM) / maxTargetRPM, 0f, 1f);
    }

    @SuppressWarnings("null")
    public void updateThrust() {
        if (level == null || level.isClientSide) {
            return;
        }

        float speed = Math.abs(getSpeed());
        Optional<PropellerBladeItem> bladeOptional = getBlade();
        if (!bladeOptional.isPresent()) {
            propellerData.setThrust(0);
            propellerData.setTorque(0);
            return;
        }
        boolean invertDirection = (getSpeed() < 0) ^ isClockwise;
        float thrust = 0;
        float torque = 0;

        if (speed > 0) {
            PropellerBladeItem blade = bladeOptional.get();
            // Calculate thrust based on speed, up to the max effective speed.
            float speedPercentage = Math.min(speed / (float)MAX_EFFECTIVE_SPEED, 1.0f);
            float bladeCountModifier = (float)getBladeCount() / (float)blade.getMaxBlades();
            float fluidSample = getSpatialHandler().getSmoothFluidSample();

            //Technically when PROPELLER_WATER_POWER_MULTIPLIER is larger than 1.0 we can get more than 100% efficiency, but who cares :P
            double fluidEfficiency = fluidSample * blade.getFluidEfficiency() * PropulsionConfig.PROPELLER_WATER_POWER_MULTIPLIER.get();
            float airEfficiency = (1 - fluidSample) * blade.getAirEfficiency();
            float substanceEfficiency = (float)fluidEfficiency + airEfficiency;

            float baseWorkMultiplier = speedPercentage * bladeCountModifier * substanceEfficiency;

            //Thrust
            thrust = MAX_THRUST * baseWorkMultiplier;

            //Torque
            float torqueFactor = blade.getTorqueFactor();
            if (torqueFactor > MathUtility.epsilon) {
                //double torqueEffectMultiplier = ;
                float torqueMagnitude = MAX_TORQUE * baseWorkMultiplier * torqueFactor;
                torque = Math.signum(getSpeed()) * torqueMagnitude;
            }
        }

        propellerData.setThrust(thrust);
        propellerData.setTorque(torque);
        propellerData.setInvertDirection(invertDirection);
    }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;

        //RPM sim
        float deltaTimeSeconds = 0.05f; 
        float targetRPM = getTargetRPM();
        float diff = targetRPM - internalRPM;

        if (Math.abs(diff) > MathUtility.epsilon) {
            float proportionalAccel = diff * PropellerRenderer.SMOOTHING_FACTOR;
            float clampedAcceleration = Mth.clamp(Math.abs(proportionalAccel), PropellerRenderer.RPM_MIN_ACCELERATION, PropellerRenderer.RPM_MAX_ACCELERATION);
            float deltaRPM = Math.signum(diff) * clampedAcceleration * deltaTimeSeconds;

            if (Math.abs(deltaRPM) > Math.abs(diff)) {
                internalRPM = targetRPM;
            } else {
                internalRPM += deltaRPM;
            }
        }
        
        //Update thrust only if fluid sample changed
        float fluidSample = getSpatialHandler().getSmoothFluidSample();
        if (Math.abs(lastFluidSample - fluidSample) > 0.01) {
            lastFluidSample = fluidSample;
            updateThrust();
        }
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

    public Optional<PropellerBladeItem> getBlade() {
        ItemStack bladeStack = bladeInventory.getStackInSlot(0);
        if (!bladeStack.isEmpty() && bladeStack.getItem() instanceof PropellerBladeItem bladeItem) {
            return Optional.of(bladeItem);
        }
        return Optional.empty();
    }

    public void flipBladeDirection() {
        if (getBladeCount() > 0) {
            this.isClockwise = !this.isClockwise;
            setChanged();
            sendData();
        }
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

        if (currentBlades == 0) {
            isClockwise = bladeItem.isBladeInverted();
        }

        for (int i = 0; i < bladeInventory.getSlots(); i++) {
            if (bladeInventory.getStackInSlot(i).isEmpty()) {
                ItemStack newBlade = bladeStack.copy();
                newBlade.setCount(1);
                bladeInventory.setStackInSlot(i, newBlade);

                //Animate insertion
                sendData();
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

                if (getBladeCount() == 0) {
                    this.isClockwise = true;
                }
                //Animate removal
                sendData();
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
                updateTargetAngles();
                updateThrust();
                setChanged();
            }
        };
    }

    private void updateTargetAngles() {
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

    //Goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        spatialHandler.triggerImmediateScan();

        Outline.OutlineParams outline = Outliner.getInstance().showCluster("PropellerObstruction", spatialHandler.getObstructedBlocks());
        outline.colored(AssemblyUtility.CANCEL_COLOR);
        outline.lineWidth(1/16f);
        outline.withFaceTexture(AllSpecialTextures.CHECKERED);
        outline.disableLineNormals();

        return true;
    }

    //NBT and caps

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.put("blades", bladeInventory.serializeNBT());
        compound.putBoolean("isClockwise", isClockwise);

        if (!clientPacket) {
            compound.putFloat("internalRPM", internalRPM);
        }

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
        bladeInventory.deserializeNBT(compound.getCompound("blades"));
        isClockwise = compound.contains("isClockwise") ? compound.getBoolean("isClockwise") : true;

        if (!clientPacket) {
            internalRPM = compound.getFloat("internalRPM");
        }

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

                this.visualRPM = getBladeCount() > 0 ? this.getTargetRPM() : 0f;
                this.visualAngle = (worldPosition.hashCode() * 31) % 360f;
                
                hasLoadedClientState = true;
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
                    this.animationStartTime = AnimationTickHolder.getRenderTime(level) / 20.0f;
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
