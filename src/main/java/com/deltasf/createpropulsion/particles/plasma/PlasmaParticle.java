package com.deltasf.createpropulsion.particles.plasma;

import javax.annotation.Nonnull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class PlasmaParticle extends SimpleAnimatedParticle {
    
    // Config
    private static final float PLASMA_SPREAD = 0.05f;
    private static final float PLASMA_BASE_QUAD_SIZE = 2.0f;
    private static final float PLASMA_FRICTION = 0.99f;
    private static final float PLASMA_SPEED_MULTIPLIER = 0.144f;
    private static final int PLASMA_BASE_LIFETIME = 40;
    
    // Physics
    private static final float COLLISION_SPEED_RETENTION = 0.9f;
    private static final double COLLISION_DETECTION_EPSILON = 0.001;
    private static final float COLLISION_PERPENDICULAR_DAMPEN = 0.1f;

    // Visuals
    private final SpriteSet spriteSet;
    private static final int PLASMA_SPRITE_COUNT = 9;

    private static final float PLASMA_SHRINK_START = 0.6f;
    private static final float PLASMA_END_SCALE_MULTIPLIER = 3.0f;

    
    private float currentSpeedMultiplier;
    private float baseSize;
    
    double dx; double dy; double dz;

    protected PlasmaParticle(ClientLevel level, double x, double y, double z, 
                            double dxSource, double dySource, double dzSource, 
                            SpriteSet spriteSet) {
        super(level, x, y, z, spriteSet, 0);
        this.spriteSet = spriteSet;
        
        // Initialize plasma state
        this.quadSize *= PLASMA_BASE_QUAD_SIZE;
        this.baseSize = this.quadSize;
        this.lifetime = PLASMA_BASE_LIFETIME;
        this.friction = PLASMA_FRICTION;
        this.dx = dxSource + getRandomSpread(); 
        this.dy = dySource + getRandomSpread(); 
        this.dz = dzSource + getRandomSpread();
        this.hasPhysics = true;
        this.currentSpeedMultiplier = PLASMA_SPEED_MULTIPLIER;
        
        // Calculate spread direction (perpendicular to velocity)
        Vec3 initialVel = new Vec3(this.dx, this.dy, this.dz).normalize();
        Vec3 nonParallel = new Vec3(1, 0, 0);
        if (Math.abs(initialVel.dot(nonParallel)) > 0.99) {
            nonParallel = new Vec3(0, 1, 0);
        }

        setSpriteFromAge(this.spriteSet);
        setColor(0xFFFFFF);
        setAlpha(1);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        final double COLLISION_IGNORE_DOT_THRESHOLD = -1.0E-5D;
        
        //Die young
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Velocity before possible collision
        double intendedMoveX = this.dx * this.currentSpeedMultiplier;
        double intendedMoveY = this.dy * this.currentSpeedMultiplier;
        double intendedMoveZ = this.dz * this.currentSpeedMultiplier;

        double prevX = this.x;
        double prevY = this.y;
        double prevZ = this.z;

        // Actual movement
        this.move(intendedMoveX, intendedMoveY, intendedMoveZ);
        double actualMoveX = this.x - prevX;
        double actualMoveY = this.y - prevY;
        double actualMoveZ = this.z - prevZ;

        // Determine collision and its normal
        boolean collisionDetected = false;
        Vec3 collisionNormal = null; 
        if (this.onGround) {
            collisionDetected = true;
            collisionNormal = new Vec3(0, 1, 0); 
        } else {
            final float COLLISION_DETECTION_FACTOR = 0.95f;
            boolean blockedX = Math.abs(intendedMoveX) > COLLISION_DETECTION_EPSILON && Math.abs(actualMoveX) < Math.abs(intendedMoveX) * COLLISION_DETECTION_FACTOR;
            boolean blockedZ = Math.abs(intendedMoveZ) > COLLISION_DETECTION_EPSILON && Math.abs(actualMoveZ) < Math.abs(intendedMoveZ) * COLLISION_DETECTION_FACTOR;
            boolean blockedYCeiling = Math.abs(intendedMoveY) > COLLISION_DETECTION_EPSILON && intendedMoveY > 0 && Math.abs(actualMoveY) < Math.abs(intendedMoveY) * COLLISION_DETECTION_FACTOR;
            if (blockedYCeiling) {
                collisionDetected = true;
                collisionNormal = new Vec3(0, -1, 0); 
            } else if (blockedX) {
                collisionDetected = true;
                collisionNormal = new Vec3(intendedMoveX < 0 ? 1 : -1, 0, 0);
            } else if (blockedZ) {
                collisionDetected = true;
                collisionNormal = new Vec3(0, 0, intendedMoveZ < 0 ? 1 : -1);
            }
        }

        // We actually collided with something, lets resolve velocity!
        if (collisionDetected && collisionNormal != null) {
            Vec3 incomingVel = new Vec3(this.dx, this.dy, this.dz);
            if (incomingVel.normalize().dot(collisionNormal) > COLLISION_IGNORE_DOT_THRESHOLD) {
                // Nothing ever happens, we collide backwards here, which should not be resolved
            } else {
                double incomingSpeedSq = incomingVel.lengthSqr();
                if (incomingSpeedSq > 1e-7) {
                    Vec3 incomingVelNormalized = incomingVel.normalize();
                    double dot = incomingVelNormalized.dot(collisionNormal);
                    
                    // 0 - perpendicular, PI/2 - parallel
                    // Using org.joml.Math logic from original, or standard Math if preferred
                    double angleOfIncidence = Math.acos(Mth.clamp(Math.abs(dot), 0.0, 1.0));
                    float spreadBlendFactor = (float)Math.cos(angleOfIncidence);
                    float slideBlendFactor = (float)Math.sin(angleOfIncidence);

                    // Velocity decomposition
                    Vec3 V_normal_comp = collisionNormal.scale(incomingVel.dot(collisionNormal));
                    Vec3 V_tangential_comp = incomingVel.subtract(V_normal_comp);

                    // Reflect + dampen
                    Vec3 desiredNormalVel;
                    if (incomingVel.dot(collisionNormal) < 0) { // Moving into the surface
                        desiredNormalVel = V_normal_comp.scale(-COLLISION_PERPENDICULAR_DAMPEN); 
                    } else {
                        desiredNormalVel = V_normal_comp; 
                    }
                    
                    // Calculate spread velocity
                    Vec3 spreadPlaneDirection;
                    double randomAngle = this.random.nextDouble() * Math.PI * 2.0D;
                    
                    // Determine two axes perpendicular to normal
                    Vec3 axis1, axis2;
                    if (Math.abs(collisionNormal.y) > 0.9) { // Ground/Ceiling
                        axis1 = new Vec3(1, 0, 0).normalize();
                        axis2 = collisionNormal.cross(axis1).normalize();
                    } else { // Wall 
                        axis1 = new Vec3(0, 1, 0).normalize();
                        axis2 = collisionNormal.cross(axis1).normalize();
                    }
                    if (axis2.lengthSqr() < 0.1) { // Fallback
                        if(Math.abs(collisionNormal.x) > 0.9) axis1 = new Vec3(0,0,1).normalize();
                        else axis1 = new Vec3(1,0,0).normalize();
                        axis2 = collisionNormal.cross(axis1).normalize();
                    }

                    spreadPlaneDirection = axis1.scale(Math.cos(randomAngle)).add(axis2.scale(Math.sin(randomAngle))).normalize();
                    
                    Vec3 spreadComponent = spreadPlaneDirection.scale(incomingVel.length() * spreadBlendFactor);
                    Vec3 slideComponent = V_tangential_comp.scale(slideBlendFactor);
                    
                    Vec3 desiredTangentialVel = slideComponent.add(spreadComponent);

                    // Combine and apply new velocity
                    Vec3 newVel = desiredNormalVel.add(desiredTangentialVel);
                    double newVelMagnitude = newVel.length();
                    if (newVelMagnitude > 1e-5) {
                        this.dx = (newVel.x / newVelMagnitude) * incomingVel.length() * COLLISION_SPEED_RETENTION;
                        this.dy = (newVel.y / newVelMagnitude) * incomingVel.length() * COLLISION_SPEED_RETENTION;
                        this.dz = (newVel.z / newVelMagnitude) * incomingVel.length() * COLLISION_SPEED_RETENTION;
                    } else { // Fallback
                        this.dx = spreadPlaneDirection.x * incomingVel.length() * COLLISION_SPEED_RETENTION * 0.5;
                        this.dy = spreadPlaneDirection.y * incomingVel.length() * COLLISION_SPEED_RETENTION * 0.5;
                        this.dz = spreadPlaneDirection.z * incomingVel.length() * COLLISION_SPEED_RETENTION * 0.5;
                    }

                } else { // Incoming speed too low, slow down
                    this.dx *= 0.1; this.dy *= 0.1; this.dz *= 0.1;
                }
            }
        }

        // Visual update
        float percent = (float)this.age / (float)this.lifetime;
        
        if (percent < PLASMA_SHRINK_START) {
            this.quadSize = this.baseSize + (float)Math.pow(percent, 0.8f) * 2.0f;
        } else {
            float sizeAtTransition = this.baseSize + (float)Math.pow(PLASMA_SHRINK_START, 0.8f) * 2.0f;
            float sizeAtEnd = this.baseSize * PLASMA_END_SCALE_MULTIPLIER;
            float shrinkProgress = (percent - PLASMA_SHRINK_START) / (1.0f - PLASMA_SHRINK_START);
            this.quadSize = Mth.lerp(shrinkProgress, sizeAtTransition, sizeAtEnd);
        }

        // Friction
        this.dx *= this.friction;
        this.dy *= this.friction;
        this.dz *= this.friction;
        
        this.pickSprite();
    }

    private void pickSprite() {
        int frameIndex = (int) (((float)this.age / (float)this.lifetime) * PLASMA_SPRITE_COUNT);
        frameIndex = Mth.clamp(frameIndex, 0, PLASMA_SPRITE_COUNT - 1);
        this.setSprite(this.spriteSet.get(frameIndex, PLASMA_SPRITE_COUNT));
    }

    float getRandomSpread(){
        return (random.nextFloat() * 2.0f - 1.0f) * PLASMA_SPREAD;
    }

    @Nonnull
    public ParticleRenderType getRenderType(){
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public static class Factory implements ParticleProvider<PlasmaParticleData>{
        private final SpriteSet spriteSet;
        public Factory(SpriteSet plasmaSpriteSet) {
            this.spriteSet = plasmaSpriteSet;
        }

        @Override
        public Particle createParticle(@Nonnull PlasmaParticleData data, @Nonnull ClientLevel level, 
        double x, double y, double z, double dx, double dy, double dz){
            return new PlasmaParticle(level, x, y, z, dx, dy, dz, this.spriteSet);
        }
    }
}