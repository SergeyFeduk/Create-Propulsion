package com.deltasf.createpropulsion.particles.plasma;

import javax.annotation.Nonnull;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;

import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("deprecation")
public class PlasmaParticleData implements ParticleOptions, ICustomParticleDataWithSprite<PlasmaParticleData> {

    public static final ParticleOptions.Deserializer<PlasmaParticleData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        public PlasmaParticleData fromCommand(@Nonnull ParticleType<PlasmaParticleData> particleTypeIn, @Nonnull StringReader reader) 
        throws CommandSyntaxException {
            return new PlasmaParticleData(particleTypeIn);
        } 

        public PlasmaParticleData fromNetwork(@Nonnull ParticleType<PlasmaParticleData> particleTypeIn, @Nonnull FriendlyByteBuf buffer) {
            return new PlasmaParticleData(particleTypeIn);
        }
    };
    private final ParticleType<PlasmaParticleData> type;

    public PlasmaParticleData() {
        this.type = null; 
    }

    public PlasmaParticleData(ParticleType<PlasmaParticleData> type) {
        this.type = type;
    }

    @Override
    public ParticleType<?> getType(){
        return this.type;
    }

    @Override
    public void writeToNetwork(@Nonnull FriendlyByteBuf buffer){}

    @Override
    public String writeToString() {
        ResourceLocation key = ForgeRegistries.PARTICLE_TYPES.getKey(this.getType());
        if (key == null) {
            return "createpropulsion:plasma_default";
       }
       return key.toString();
    }

    @Override
    public Deserializer<PlasmaParticleData> getDeserializer() {
        return DESERIALIZER;
    }

    @Override
    public Codec<PlasmaParticleData> getCodec(ParticleType<PlasmaParticleData> type) {
        return Codec.unit(() -> new PlasmaParticleData(type));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public SpriteParticleRegistration<PlasmaParticleData> getMetaFactory() {
        return PlasmaParticle.Factory::new;
    }
}
