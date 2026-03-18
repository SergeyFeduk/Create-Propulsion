package com.deltasf.createpropulsion.particles.magnetite;

import java.util.Locale;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.particles.ParticleTypes;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;

import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("deprecation")
public class MagnetiteSparkParticleData implements ParticleOptions, ICustomParticleDataWithSprite<MagnetiteSparkParticleData> {

    public static final ParticleOptions.Deserializer<MagnetiteSparkParticleData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        public MagnetiteSparkParticleData fromCommand(@Nonnull ParticleType<MagnetiteSparkParticleData> type, @Nonnull StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float r = reader.readFloat();
            reader.expect(' ');
            float g = reader.readFloat();
            reader.expect(' ');
            float b = reader.readFloat();
            return new MagnetiteSparkParticleData(type, r, g, b);
        }

        public MagnetiteSparkParticleData fromNetwork(@Nonnull ParticleType<MagnetiteSparkParticleData> type, @Nonnull FriendlyByteBuf buffer) {
            return new MagnetiteSparkParticleData(type, buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        }
    };

    private final ParticleType<MagnetiteSparkParticleData> type;
    public final float r, g, b;

    public MagnetiteSparkParticleData() {
        this.type = null;
        this.r = 0.15f;
        this.g = 0.15f;
        this.b = 0.15f;
    }

    public MagnetiteSparkParticleData(float r, float g, float b) {
        this(ParticleTypes.MAGNETITE_SPARK.get(), r, g, b);
    }

    @SuppressWarnings("unchecked")
    public MagnetiteSparkParticleData(ParticleType<?> type, float r, float g, float b) {
        this.type = (ParticleType<MagnetiteSparkParticleData>) type;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    // Default constructor for registry
    public MagnetiteSparkParticleData(ParticleType<MagnetiteSparkParticleData> type) {
        this(type, 0.15f, 0.15f, 0.15f);
    }

    @Override 
    public ParticleType<?> getType() { 
        return this.type; 
    }

    @Override 
    public void writeToNetwork(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeFloat(r);
        buffer.writeFloat(g);
        buffer.writeFloat(b);
    }
    
    @Override
    public String writeToString() {
        ResourceLocation key = ForgeRegistries.PARTICLE_TYPES.getKey(this.getType());
        return (key == null ? "createpropulsion:magnetite_spark_default" : key.toString()) + String.format(Locale.ROOT, " %.2f %.2f %.2f", r, g, b);
    }

    @Override 
    public Deserializer<MagnetiteSparkParticleData> getDeserializer() { 
        return DESERIALIZER; 
    }

    @Override 
    public Codec<MagnetiteSparkParticleData> getCodec(ParticleType<MagnetiteSparkParticleData> type) { 
        return RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("r").forGetter(d -> d.r),
            Codec.FLOAT.fieldOf("g").forGetter(d -> d.g),
            Codec.FLOAT.fieldOf("b").forGetter(d -> d.b)
        ).apply(instance, (r, g, b) -> new MagnetiteSparkParticleData(type, r, g, b)));
    }

    @Override 
    public SpriteParticleRegistration<MagnetiteSparkParticleData> getMetaFactory() { 
        return MagnetiteSparkParticle.Factory::new; 
    }
}