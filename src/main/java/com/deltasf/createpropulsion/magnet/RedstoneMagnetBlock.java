package com.deltasf.createpropulsion.magnet;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.particles.magnetite.MagnetiteSparkParticleData;
import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

@SuppressWarnings("deprecation")
public class RedstoneMagnetBlock extends DirectionalBlock implements EntityBlock, IWrenchable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty MAGNETITE = BooleanProperty.create("magnetite");

    public RedstoneMagnetBlock(Properties properties){
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(POWERED, false)
            .setValue(MAGNETITE, false));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, POWERED, MAGNETITE);
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        Direction baseDirection = context.getNearestLookingDirection();
        Direction placeDirection;
        Player player = context.getPlayer();
        if (player != null) {
            placeDirection = !player.isShiftKeyDown() ? baseDirection.getOpposite() : baseDirection;
        } else {
            placeDirection = baseDirection.getOpposite();
        }
        
        return this.defaultBlockState().setValue(FACING, placeDirection);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new RedstoneMagnetBlockEntity(PropulsionBlockEntities.REDSTONE_MAGNET_BLOCK_ENTITY.get(), pos, state);
    }
    
    @Override
    public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide) return;
        if (state.is(oldState.getBlock())) return;

        updatePower(state, level, pos);
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        //Final destination
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RedstoneMagnetBlockEntity rbe) {
                rbe.onBlockBroken(level);
            }
            if (state.getValue(MAGNETITE)) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.NETHERITE_INGOT, 1));
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (heldStack.is(Items.NETHERITE_INGOT) && !state.getValue(MAGNETITE)) {
            if (!PropulsionConfig.REDSTONE_MAGNET_ALLOW_MAGNETITE.get()) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide) {
                if (!player.isCreative()) {
                    heldStack.shrink(1);
                }
                level.setBlock(pos, state.setValue(MAGNETITE, true), 3);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);
                
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof RedstoneMagnetBlockEntity rbe) {
                    rbe.scheduleUpdate();
                }

            } else {
                spawnMagnetiteParticles(level, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (state.getValue(MAGNETITE)) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            Player player = context.getPlayer();

            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(MAGNETITE, false), 3);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.BLOCKS, 0.8F, level.random.nextFloat() * 0.1F + 0.6F); 
                // Try to add to inventory first, if fails -> drop
                if (player == null || !player.isCreative()) {
                    ItemStack drop = new ItemStack(Items.NETHERITE_INGOT, 1);
                    if (player != null && !player.getInventory().add(drop)) {
                        Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drop);
                    } else if (player == null) {
                        Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, drop);
                    }
                }
            } else {
                spawnMagnetiteParticles(level, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return IWrenchable.super.onWrenched(state, context);
    }

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull net.minecraft.world.level.block.Block block, @Nonnull BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;
        updatePower(state, level, pos);
    }

    private void updatePower(BlockState state, Level level, BlockPos pos) {
        int signalStrength = level.getBestNeighborSignal(pos);
        boolean shouldBePowered = signalStrength > 0;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RedstoneMagnetBlockEntity rbe) {
            rbe.setPower(signalStrength / 15.0f);
            rbe.updateBlockstateFromPower();
        }

        if (state.getValue(POWERED) != shouldBePowered) {
            level.setBlock(pos, state.setValue(POWERED, shouldBePowered), 2);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return new SmartBlockEntityTicker<>();
    }

    @Override
    public BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    //TODO: Utils
    private void spawnMagnetiteParticles(Level level, BlockPos pos) {
        RandomSource random = level.random;
        for (Direction direction : Direction.values()) {
            int count = UniformInt.of(3, 5).sample(random);
            for (int i = 0; i < count; i++) {
                double offX = direction.getStepX() == 0 ? random.nextDouble() : (direction.getStepX() > 0 ? 1.05D : -0.05D);
                double offY = direction.getStepY() == 0 ? random.nextDouble() : (direction.getStepY() > 0 ? 1.05D : -0.05D);
                double offZ = direction.getStepZ() == 0 ? random.nextDouble() : (direction.getStepZ() > 0 ? 1.05D : -0.05D);
                double vx = direction.getStepX() == 0 ? random.nextGaussian() * 0.03D : 0.0D;
                double vy = direction.getStepY() == 0 ? random.nextGaussian() * 0.03D : 0.0D;
                double vz = direction.getStepZ() == 0 ? random.nextGaussian() * 0.03D : 0.0D;
                
                level.addParticle(
                    new MagnetiteSparkParticleData(0.15f, 0.15f, 0.15f), 
                    pos.getX() + offX, pos.getY() + offY, pos.getZ() + offZ, 
                    vx, vy, vz
                );
            }
        }
    }

}
