package com.deltasf.createpropulsion.particles.magnetite;

import javax.annotation.Nonnull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;

public class MagnetiteSparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected MagnetiteSparkParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, float r, float g, float b) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        
        this.quadSize = 0.1f * (this.random.nextFloat() * 0.5f + 0.75f);
        this.lifetime = (int)(20.0D / (this.random.nextDouble() * 0.8D + 0.2D));
        this.setSpriteFromAge(sprites);
        
        this.hasPhysics = false; 
        
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.alpha = 0.9f;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.setSpriteFromAge(this.sprites);
        this.move(this.xd, this.yd, this.zd);
        
        this.xd *= 0.9f;
        this.yd *= 0.9f;
        this.zd *= 0.9f;
    }

    @Nonnull
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<MagnetiteSparkParticleData> {
        private final SpriteSet spriteSet;
        public Factory(SpriteSet spriteSet) { this.spriteSet = spriteSet; }
        
        @Override
        public Particle createParticle(@Nonnull MagnetiteSparkParticleData data, @Nonnull ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new MagnetiteSparkParticle(level, x, y, z, dx, dy, dz, this.spriteSet, data.r, data.g, data.b);
        }
    }
}