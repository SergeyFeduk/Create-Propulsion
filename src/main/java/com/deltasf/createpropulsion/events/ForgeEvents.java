package com.deltasf.createpropulsion.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.balloons.hot_air.BalloonAttachment;
import com.deltasf.createpropulsion.magnet.MagnetForceAttachment;
import com.deltasf.createpropulsion.magnet.MagnetRegistry;
import com.deltasf.createpropulsion.network.PropulsionPackets;
import com.deltasf.createpropulsion.network.SyncThrusterFuelsPacket;
import com.deltasf.createpropulsion.physics_assembler.AssemblyGaugeItem;
import com.deltasf.createpropulsion.registries.PropulsionCommands;
import com.deltasf.createpropulsion.registries.PropulsionFluids;
import com.deltasf.createpropulsion.thruster.ThrusterFuelManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.BlockEvent.FluidPlaceBlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ThrusterFuelManager());
    }

    //Sync thruster fuels for goggles & particles on client side
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player)  {
            PropulsionPackets.sendToPlayer(SyncThrusterFuelsPacket.create(ThrusterFuelManager.getFuelPropertiesMap()), player);
        }
    }

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        PropulsionCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MagnetRegistry.get().reset();
        //TODO: Reset balloon registry
    }

    @SubscribeEvent
	public static void onServerStart(ServerStartedEvent event) {
        Map<ResourceLocation, ServerLevel> levelLookup = new HashMap<>();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            levelLookup.put(level.dimension().location(), level);
        }

        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return; //We are so fucked at this stage

        final String PREFIX = "minecraft:dimension:";

        var allShips = VSGameUtilsKt.getAllShips(overworld);
        for (Ship ship : allShips) {
            if (ship instanceof ServerShip serverShip) {
                String shipDimensionId = serverShip.getChunkClaimDimension();
                if (shipDimensionId != null && shipDimensionId.startsWith(PREFIX)) {
                    String resourceLocationString = shipDimensionId.substring(PREFIX.length());
                    ResourceLocation dimensionKey = new ResourceLocation(resourceLocationString);
                    ServerLevel level = levelLookup.get(dimensionKey);
                    if (level == null) continue; //Wtf

                    //Restore MagnetForceAttachment levels
                    var magnetAttachment  = serverShip.getAttachment(MagnetForceAttachment.class);
                    if (magnetAttachment != null) {
                        magnetAttachment.level = level;
                    }
                    //Restore BalloonAttachment atmosphere data
                    var balloonAttachment = serverShip.getAttachment(BalloonAttachment.class);
                    if (balloonAttachment != null) {
                        BalloonAttachment.updateAtmosphereData(balloonAttachment, level);
                    }
                }
            }
        }
    }

    //Turpentine-lava interaction
    @SubscribeEvent
    public static void onNeighborBlockUpdate(BlockEvent.NeighborNotifyEvent event) {
        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.getFluidState().isEmpty()) {
            return;
        }

        boolean isTurpentine = state.getFluidState().is(PropulsionFluids.TURPENTINE.get());
        boolean isLava = state.getFluidState().is(Fluids.LAVA) || state.getFluidState().is(Fluids.FLOWING_LAVA);

        if (!isTurpentine && !isLava) {
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            FluidState neighborFluid = level.getFluidState(neighborPos);

            if (isTurpentine && (neighborFluid.is(Fluids.LAVA) || neighborFluid.is(Fluids.FLOWING_LAVA))) {
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                return;
            }

            if (isLava && neighborFluid.is(PropulsionFluids.TURPENTINE.get())) {
                level.setBlock(neighborPos, Blocks.STONE.defaultBlockState(), 3);
                return;
            }
        }
    }
}
