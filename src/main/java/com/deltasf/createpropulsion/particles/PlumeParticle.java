package com.deltasf.createpropulsion.particles;

import javax.annotation.Nonnull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;

public class PlumeParticle extends SimpleAnimatedParticle {
    private static final float SPREAD = 0.05f;
    private static final float BASE_QUAD_SIZE = 2.0f;
    private static final float FRICTION = 0.99f;
    private static final float SPEED_MULTIPLIER = 0.144f;
    private static final int BASE_LIFETIME = 40 - 2;

    protected PlumeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprite) {
        super(level, x, y, z,sprite, 0);
        this.quadSize *= BASE_QUAD_SIZE;
        this.baseSize = this.quadSize;
        this.lifetime = BASE_LIFETIME + random.nextInt(5);
        this.friction = FRICTION;
        this.dx = dx + gRng(); this.dy = dy + gRng(); this.dz = dz + gRng();
        hasPhysics = true;

        setSprite(sprite.get(0, this.lifetime));
        setColor(0xFFFFFF);
        setAlpha(1);
    }

    float gRng(){
        return (random.nextFloat() * 2.0f - 1.0f) * SPREAD;
    }

    double dx; double dy; double dz;
    float baseSize;

    @Nonnull
    public ParticleRenderType getRenderType(){
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick(){
        this.move();
        this.setSpriteFromAge(this.sprites);
        //Move and fade
        float percent = (float)this.age / (float)this.lifetime;
        setAlpha(1 - percent);
        this.quadSize = this.baseSize + (float)Math.pow(percent, 0.8f) * 2.0f;
    }
    
    private void move(){
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            double moveX = this.dx * SPEED_MULTIPLIER;
            double moveY = this.dy * SPEED_MULTIPLIER;
            double moveZ = this.dz * SPEED_MULTIPLIER;
            this.move(moveX, moveY, moveZ);
            this.dx *= (double)this.friction;
            this.dy *= (double)this.friction;
            this.dz *= (double)this.friction;
            if (this.onGround) {
                this.dx *= (double)0.7F;
                this.dz *= (double)0.7F;
            }
        }
    }

    //Factory
    public static class Factory implements ParticleProvider<PlumeParticleData>{
        private final SpriteSet spriteSet;
        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(@Nonnull PlumeParticleData data, @Nonnull ClientLevel level, 
        double x, double y, double z, double dx, double dy, double dz){
            return new PlumeParticle(level, x, y, z, dx, dy, dz, this.spriteSet);
        }
    }
}
