package com.deltasf.createpropulsion.ponder;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.heat.burners.liquid.LiquidBurnerBlockEntity;
import com.deltasf.createpropulsion.heat.burners.solid.SolidBurnerBlock;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.deltasf.createpropulsion.registries.PropulsionFluids;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;

public class BurnerScenes {
    public static void solidBurner(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("solid_burner", "Generating heat with Solid Burner");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos burnerPos = util.grid().at(2, 1, 2);
        BlockPos enginePos = util.grid().at(2, 2, 2);
        BlockPos leverPos = util.grid().at(1, 1, 2);

        Selection burnerSel = util.select().position(burnerPos);
        Selection engineSel = util.select().position(enginePos);
        Selection leverSel = util.select().position(leverPos);

        scene.world().showSection(burnerSel, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(engineSel, Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(70)
            .text("createpropulsion.ponder.solid_burner.text_1")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(util.vector().blockSurface(burnerPos, Direction.WEST), Pointing.LEFT, 40)
            .rightClick()
            .withItem(new ItemStack(Items.COAL));
        scene.idle(20);

        scene.world().modifyBlock(burnerPos, s -> s.setValue(SolidBurnerBlock.LIT, true), false);
        scene.world().modifyBlock(burnerPos, s -> s.setValue(AbstractBurnerBlock.HEAT, HeatLevel.KINDLED), false);

        scene.effects().emitParticles(util.vector().centerOf(burnerPos), (world, x, y, z) -> {
            BlockPos pos = BlockPos.containing(x, y, z);
            net.minecraft.world.level.block.state.BlockState state = world.getBlockState(pos);
            PropulsionBlocks.SOLID_BURNER.get().animateTick(state, world, pos, world.random);
        }, 1, 1000);
        scene.idle(20);

        scene.world().setKineticSpeed(engineSel, 64);
        scene.effects().indicateSuccess(enginePos);
        scene.idle(10);

        scene.overlay().showText(70)
            .text("createpropulsion.ponder.solid_burner.text_2")
            .pointAt(util.vector().centerOf(enginePos))
            .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(80)
            .colored(PonderPalette.GREEN)
            .text("createpropulsion.ponder.solid_burner.text_3")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(100);

        scene.world().hideSection(engineSel, Direction.UP);
        scene.world().setKineticSpeed(engineSel, 0); 
        scene.idle(25);
        
        scene.world().showSection(leverSel, Direction.EAST);
        scene.idle(20);

        scene.overlay().showText(70)
            .attachKeyFrame()
            .text("createpropulsion.ponder.solid_burner.text_4")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(80)
            .text("createpropulsion.ponder.solid_burner.text_5")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(90);
        
        scene.world().toggleRedstonePower(leverSel);
        scene.effects().indicateRedstone(leverPos);
        scene.idle(20);

        scene.overlay().showText(100)
            .colored(PonderPalette.RED)
            .text("createpropulsion.ponder.solid_burner.text_6")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(110);

        scene.world().modifyBlock(burnerPos, s -> s.setValue(AbstractBurnerBlock.HEAT, HeatLevel.KINDLED), false);
        scene.overlay().showText(60)
            .text("createpropulsion.ponder.solid_burner.text_7") 
            .pointAt(util.vector().topOf(burnerPos))
            .placeNearTarget();
        scene.idle(60);

        scene.world().modifyBlock(burnerPos, s -> s.setValue(AbstractBurnerBlock.HEAT, HeatLevel.SEETHING), false);
        scene.overlay().showText(80)
            .text("createpropulsion.ponder.solid_burner.text_8") 
            .pointAt(util.vector().topOf(burnerPos))
            .placeNearTarget();
        scene.idle(90);
    }

    public static void liquidBurner(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("liquid_burner", "Generating heat with Liquid Burner");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos burnerAPos = util.grid().at(2, 1, 1);
        BlockPos stirlingAPos = util.grid().at(2, 2, 1);
        BlockPos mechPipePos = util.grid().at(1, 1, 3);
        BlockPos powerInputCog = util.grid().at(5, 0, 4);

        Selection burnerASel = util.select().position(burnerAPos);
        Selection stirlingASel = util.select().position(stirlingAPos);
        Selection fluidsGroup = util.select().fromTo(2, 1, 2, 2, 1, 3)
            .add(util.select().position(mechPipePos))
            .add(util.select().fromTo(0, 1, 3, 0, 2, 3));
        Selection kineticsGroup = util.select().fromTo(1, 1, 4, 5, 1, 4)
            .add(util.select().position(powerInputCog));

        scene.world().showSection(burnerASel, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(stirlingASel, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(fluidsGroup, Direction.NORTH);
        scene.idle(5);
        scene.world().showSection(kineticsGroup, Direction.WEST);
        scene.idle(10);

        scene.overlay().showText(70)
            .text("createpropulsion.ponder.liquid_burner.text_1")
            .pointAt(util.vector().blockSurface(burnerAPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(80);

        scene.world().setKineticSpeed(kineticsGroup, 32);
        scene.world().setKineticSpeed(util.select().position(mechPipePos), 32);
        scene.idle(10);

        scene.world().modifyBlockEntityNBT(burnerASel, LiquidBurnerBlockEntity.class, nbt -> {
            CompoundTag tankNbt = new CompoundTag();
            FluidStack fuel = new FluidStack(PropulsionFluids.TURPENTINE.get(), 100); 
            tankNbt.put("Fluid", fuel.writeToNBT(new CompoundTag()));
            nbt.put("Tank", tankNbt);
        });

        scene.effects().emitParticles(util.vector().centerOf(burnerAPos), (world, x, y, z) -> {
            Direction facing = Direction.SOUTH;
            float yRot = -facing.toYRot();
            
            float pipeOffset = 2.5f / 16.0f;
            boolean isLeft = world.getGameTime() % 4 == 0;
            
            Vec3 localOffset = new Vec3(0.6, 0.3, isLeft ? pipeOffset : -pipeOffset);
            Vec3 localVelocity = new Vec3(0.01, 0.05, 0);
            Vec3 offset = VecHelper.rotate(localOffset, yRot, Direction.Axis.Y);
            Vec3 velocity = VecHelper.rotate(localVelocity, yRot, Direction.Axis.Y);
            world.addParticle(ParticleTypes.SMOKE, x + offset.x, y + offset.y, z + offset.z, velocity.x, velocity.y, velocity.z);
        }, 1.0f, 1000);
        scene.world().modifyBlockEntityNBT(burnerASel, LiquidBurnerBlockEntity.class, nbt -> {
            nbt.putInt("burnTime", 1000);
        });
        scene.world().modifyBlock(burnerAPos, s -> s.setValue(AbstractBurnerBlock.HEAT, HeatLevel.KINDLED), false);
        
        scene.idle(20);
        scene.world().setKineticSpeed(stirlingASel, 128);
        scene.effects().indicateSuccess(stirlingAPos);

        scene.overlay().showText(70)
            .text("createpropulsion.ponder.liquid_burner.text_2")
            .pointAt(util.vector().centerOf(stirlingAPos))
            .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(80)
            .colored(PonderPalette.GREEN)
            .text("createpropulsion.ponder.liquid_burner.text_3")
            .pointAt(util.vector().blockSurface(burnerAPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
            .colored(PonderPalette.GREEN)
            .text("createpropulsion.ponder.liquid_burner.text_4")
            .pointAt(util.vector().blockSurface(burnerAPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(100);

        scene.markAsFinished();
    }

    //TODO: Create burner usage (show that burners can be used to power boilers and heated mixers)
}
