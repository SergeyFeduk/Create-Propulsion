package com.deltasf.createpropulsion.propeller;

import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Vector4fc;

import java.util.ArrayList;
import java.util.List;

public class PropellerModels {
    private static final Material BLUR_MATERIAL = SimpleMaterial.builder()
        .texture(InventoryMenu.BLOCK_ATLAS)
        .depthTest(DepthTest.LEQUAL)
        .transparency(Transparency.TRANSLUCENT)
        .writeMask(WriteMask.COLOR)
        .backfaceCulling(true)
        .build();

    private static final RendererReloadCache<PartialModel, Model> BLURRED_MODELS =
        new RendererReloadCache<>(partial -> new MaterialOverridenModel(Models.partial(partial), BLUR_MATERIAL));

    public static Model getBlurred(PartialModel partial) {
        return BLURRED_MODELS.get(partial);
    }

    private static class MaterialOverridenModel implements Model {
        private final Model wrapped;
        private final Material override;

        public MaterialOverridenModel(Model wrapped, Material override) {
            this.wrapped = wrapped;
            this.override = override;
        }

        @Override
        public List<ConfiguredMesh> meshes() {
            List<ConfiguredMesh> original = wrapped.meshes();
            List<ConfiguredMesh> replaced = new ArrayList<>(original.size());

            for (ConfiguredMesh mesh : original) {
                replaced.add(new ConfiguredMesh(override, mesh.mesh()));
            }
            return replaced;
        }

        @Override
        public Vector4fc boundingSphere() {
            return wrapped.boundingSphere();
        }
    }
}