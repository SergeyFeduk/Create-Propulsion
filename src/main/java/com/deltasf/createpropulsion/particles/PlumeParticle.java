package com.deltasf.createpropulsion.particles;

import javax.annotation.Nonnull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.world.phys.Vec3;

public class PlumeParticle extends SimpleAnimatedParticle {
    //Plume
    private static final float PLUME_SPREAD = 0.05f;
    private static final float PLUME_BASE_QUAD_SIZE = 2.0f;
    private static final float PLUME_FRICTION = 0.99f;
    private static final float PLUME_SPEED_MULTIPLIER = 0.144f;
    private static final int PLUME_BASE_LIFETIME = 30;
    //Smoke

    //Physics
    private static final float COLLISION_SPEED_RETENTION = 0.9f;
    private static final double COLLISION_DETECTION_EPSILON = 0.001;
    private static final float COLLISION_PERPENDICULAR_DAMPEN = 0.1f;

    private enum ParticleState {
        PLUME, SMOKE
    }

    //State
    /*private final SpriteSet plumeSprites;
    private final SpriteSet smokeSprites;*/

    private ParticleState currentState;
    private float currentSpeedMultiplier;
    private float currentFriction;
    double dx; double dy; double dz;
    float baseSize;

    protected PlumeParticle(ClientLevel level, double x, double y, double z, 
                            double dxSource, double dySource, double dzSource, 
                            SpriteSet plumeSpriteSet, SpriteSet smokeSpriteSet) {
        super(level, x, y, z, smokeSpriteSet, 0);
        /*this.plumeSprites = plumeSpriteSet;
        this.smokeSprites = smokeSpriteSet;*/
        //Initialize plume state
        this.quadSize *= PLUME_BASE_QUAD_SIZE;
        this.baseSize = this.quadSize;
        this.lifetime = PLUME_BASE_LIFETIME + random.nextInt(5);
        this.friction = PLUME_FRICTION;
        this.dx = dxSource + getRandomSpread(); 
        this.dy = dySource + getRandomSpread(); 
        this.dz = dzSource + getRandomSpread();
        this.hasPhysics = true;
        this.currentSpeedMultiplier = PLUME_SPEED_MULTIPLIER;
        this.currentFriction = PLUME_FRICTION;
        this.currentState = ParticleState.PLUME;

        setSpriteFromAge(plumeSpriteSet);
        setColor(0xFFFFFF);
        setAlpha(1);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo /* gurt */ = this.y;
        this.zo = this.z;
        final double COLLISION_IGNORE_DOT_THRESHOLD = -1.0E-5D;
        
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        //Velocity before possible collision
        double intendedMoveX = this.dx * this.currentSpeedMultiplier;
        double intendedMoveY = this.dy * this.currentSpeedMultiplier;
        double intendedMoveZ = this.dz * this.currentSpeedMultiplier;

        double prevX = this.x;
        double prevY = this.y;
        double prevZ = this.z;

        //Actual movement
        this.move(intendedMoveX, intendedMoveY, intendedMoveZ);
        double actualMoveX = this.x - prevX;
        double actualMoveY = this.y - prevY;
        double actualMoveZ = this.z - prevZ;

        //Collision check
        boolean collisionDetected = false;
        Vec3 collisionNormal = null; 

        if (this.currentState == ParticleState.PLUME) {
            //Determine collision and its normal
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

            //We actually collided with something, lets resolve velocity!
            if (collisionDetected && collisionNormal != null) {
                Vec3 incomingVel = new Vec3(this.dx, this.dy, this.dz);
                if (incomingVel.normalize().dot(collisionNormal) > COLLISION_IGNORE_DOT_THRESHOLD) {
                    //Nothing ever happens, we collide backwards here, which should not be resolved
                } else {
                    double incomingSpeedSq = incomingVel.lengthSqr();
                    if (incomingSpeedSq > 1e-7) {
                        Vec3 incomingVelNormalized = incomingVel.normalize();
                        double dot = incomingVelNormalized.dot(collisionNormal);
                        //0 - perpendicular, PI/2 - parallel
                        double angleOfIncidence = Math.acos(org.joml.Math.clamp(Math.abs(dot), 0.0, 1.0));
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
                        Vec3 slideComponent = V_tangential_comp.scale(slideBlendFactor); // For sliding use original tangential component
                        
                        Vec3 desiredTangentialVel = slideComponent.add(spreadComponent);

                        //Combine and apply new velocity
                        Vec3 newVel = desiredNormalVel.add(desiredTangentialVel);
                        double newVelMagnitude = newVel.length();
                        if (newVelMagnitude > 1e-5) {
                            this.dx = (newVel.x / newVelMagnitude) * incomingVel.length() * COLLISION_SPEED_RETENTION;
                            this.dy = (newVel.y / newVelMagnitude) * incomingVel.length() * COLLISION_SPEED_RETENTION;
                            this.dz = (newVel.z / newVelMagnitude) * incomingVel.length() * COLLISION_SPEED_RETENTION;
                        } else { //Fallback
                            this.dx = spreadPlaneDirection.x * incomingVel.length() * COLLISION_SPEED_RETENTION * 0.5;
                            this.dy = spreadPlaneDirection.y * incomingVel.length() * COLLISION_SPEED_RETENTION * 0.5;
                            this.dz = spreadPlaneDirection.z * incomingVel.length() * COLLISION_SPEED_RETENTION * 0.5;
                        }

                    } else { // Incoming speed too low, slow down
                        this.dx *= 0.1; this.dy *= 0.1; this.dz *= 0.1;
                    }
                }
            }
        }
        //Friction
        this.dx *= this.currentFriction;
        this.dy *= this.currentFriction;
        this.dz *= this.currentFriction;
        //Visual update
        setSpriteFromAge(this.sprites);
        float percent = (float)this.age / (float)this.lifetime;
        this.quadSize = this.baseSize + (float)Math.pow(percent, 0.8f) * 2.0f;
        //setAlpha(1);
        setAlpha(1 - percent);
    }

    //Helpers

    float getRandomSpread(){
        return (random.nextFloat() * 2.0f - 1.0f) * PLUME_SPREAD;
    }

    @Nonnull
    public ParticleRenderType getRenderType(){
        //return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    //Factory
    public static class Factory implements ParticleProvider<PlumeParticleData>{
        private final SpriteSet plumeSpriteSet;
        private final SpriteSet smokeSpriteSet;
        public Factory(SpriteSet plumeSpriteSet, SpriteSet smokeSpriteSet) {
            this.plumeSpriteSet = plumeSpriteSet;
            this.smokeSpriteSet = smokeSpriteSet;
        }

        @Override
        public Particle createParticle(@Nonnull PlumeParticleData data, @Nonnull ClientLevel level, 
        double x, double y, double z, double dx, double dy, double dz){
            return new PlumeParticle(level, x, y, z, dx, dy, dz, this.plumeSpriteSet, this.smokeSpriteSet);
        }
    }
}
