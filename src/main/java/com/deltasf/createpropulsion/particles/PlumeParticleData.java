package com.deltasf.createpropulsion.particles;

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

@SuppressWarnings("deprecation") //STFU deserializer
public class PlumeParticleData implements ParticleOptions, ICustomParticleDataWithSprite<PlumeParticleData> {
    //Serialization of exactly nothing
    public static final ParticleOptions.Deserializer<PlumeParticleData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        public PlumeParticleData fromCommand(@Nonnull ParticleType<PlumeParticleData> particleTypeIn, @Nonnull StringReader reader) 
        throws CommandSyntaxException {
            return new PlumeParticleData(particleTypeIn);
        } 

        public PlumeParticleData fromNetwork(@Nonnull ParticleType<PlumeParticleData> particleTypeIn, @Nonnull FriendlyByteBuf buffer) {
            return new PlumeParticleData(particleTypeIn);
        }
    };
    private final ParticleType<PlumeParticleData> type;

    public PlumeParticleData() {
        this.type = null; 
    }

    public PlumeParticleData(ParticleType<PlumeParticleData> type) {
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
            return "createpropulsion:plume_default"; // Fallback
       }
       return key.toString();
	}

    @Override
	public Deserializer<PlumeParticleData> getDeserializer() {
		return DESERIALIZER;
	}

    @Override
	public Codec<PlumeParticleData> getCodec(ParticleType<PlumeParticleData> type) {
		return Codec.unit(() -> new PlumeParticleData(type));
	}

    @Override
	@OnlyIn(Dist.CLIENT)
	public SpriteParticleRegistration<PlumeParticleData> getMetaFactory() {
        return PlumeParticle.Factory::new;
	}
}
