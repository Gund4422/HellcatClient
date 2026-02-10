package me.ht9.rose.feature.module.modules.movement;

import me.ht9.rose.event.bus.annotation.SubscribeEvent;
import me.ht9.rose.event.events.PlayerMoveEvent;
import me.ht9.rose.feature.module.Module;
import me.ht9.rose.feature.module.annotation.Description;
import me.ht9.rose.util.module.Movement;

@Description("Automated TAS-style diagonal movement vectoring.")
public final class Strafe extends Module {
    private static final Strafe instance = new Strafe();

    private Strafe() {
        setArrayListInfo(() -> "Full");
    }

    @SubscribeEvent
    public void onMove(PlayerMoveEvent event) {
        if (mc.thePlayer == null) return;

        // Get current inputs
        float forward = mc.thePlayer.movementInput.moveForward;
        float side = mc.thePlayer.movementInput.moveStrafe;
        float yaw = mc.thePlayer.rotationYaw;

        // If we aren't trying to move, don't do anything
        if (forward == 0 && side == 0) return;

        // TAS Logic: If holding only W, we 'fake' a diagonal input 
        // to gain that sqrt(2) speed multiplier without normalizing.
        if (forward != 0 && side == 0) {
            yaw += (forward > 0) ? 45.0f : -45.0f; // Lean into the diagonal
        } else if (forward != 0 && side != 0) {
            // Already diagonal? Just optimize the yaw to hit exactly 45 degrees
            yaw += (forward > 0) ? (side > 0 ? -45.0f : 45.0f) : (side > 0 ? 45.0f : -45.0f);
        }

        // Calculate new movement vectors based on the optimized yaw
        double speed = Movement.getSpeed(); 
        
        // Beta 1.7.3 Trig (Degrees to Radians)
        double radians = Math.toRadians(yaw);
        double nx = -Math.sin(radians) * speed;
        double nz = Math.cos(radians) * speed;

        // Update your event values - Hose now controls the movement!
        event.setX(nx);
        event.setZ(nz);
    }

    public static Strafe instance() {
        return instance;
    }
}
