package com.deltasf.createpropulsion.impact_sensor;

import org.joml.Vector3dc;
import org.valkyrienskies.core.api.events.CollisionEvent;
import org.valkyrienskies.core.api.physics.ContactPoint;
import org.valkyrienskies.mod.api.ValkyrienSkies;

public class ImpactSensorSystem {
    public static void register() {
        //TODO: Waiting for collision start events to work
        ValkyrienSkies.api().getCollisionStartEvent().on(ImpactSensorSystem::onGlobalCollision);
    }

    private static void onGlobalCollision(CollisionEvent event) {
        //System.out.println("There was a CCCCC!");
        long idA = event.getShipIdA();
        long idB = event.getShipIdB();

        ImpactSensorAttachment attA = ImpactSensorAttachment.getById(idA);
        ImpactSensorAttachment attB = ImpactSensorAttachment.getById(idB);

        if (attA == null && attB == null) return;

        //System.out.println("There was a collision!");

        for (ContactPoint contact : event.getContactPoints()) {
            Vector3dc normal = contact.getNormal(); 

            if (attA != null) {
                attA.recordCollisionNormal(normal);
            }
            if (attB != null) {
                attB.recordCollisionNormal(normal); 
            }
        }
    }
}
