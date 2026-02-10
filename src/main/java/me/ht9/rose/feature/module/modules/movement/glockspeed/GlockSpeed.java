package me.ht9.rose.feature.module.modules.movement.glockspeed;

import me.ht9.rose.event.bus.annotation.SubscribeEvent;
import me.ht9.rose.event.events.TickEvent;
import me.ht9.rose.feature.module.Module;
import me.ht9.rose.feature.module.annotation.Description;
import me.ht9.rose.feature.module.setting.Setting;
import me.ht9.rose.util.module.Movement;
import me.ht9.rose.util.module.Timer;
import net.minecraft.src.Packet11PlayerPosition;

@Description("Check the rhythm, I’m steady as a rock, Time is ticking but I never watch the clock.")
public final class GlockSpeed extends Module {
    private static final GlockSpeed instance = new GlockSpeed();

    public final Setting<Double> power = new Setting<>("Power", 0.2, 0.8, 2.0, 2);
    public final Setting<Integer> flow = new Setting<>("Flow", 2, 4, 10); // Interval between 'rounds'
    public final Setting<Boolean> reset = new Setting<>("Reset", true); // Force ground state

    private int tickCount = 0;
    private final Timer timer = new Timer();

    private GlockSpeed() {
        setArrayListInfo(() -> "Glock");
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        tickCount++;

        // The "Steady" Logic:
        // We move in cycles. Most ticks are spent recharging the bank (Line 118).
        // Every 'flow' ticks, we 'fire' a high-velocity packet.
        
        if (tickCount % flow.value() == 0) {
            // FIRE: Bullet tick. We consume the horizontalBuffer accumulated.
            Movement.setSpeed(power.value());
            
            if (reset.value()) {
                // Send a packet that triggers the 'toOnGround' check (Line 95)
                // This forces NoCheat to update its safe location mid-burst.
                mc.getSendQueue().addToSendQueue(new Packet11PlayerPosition(
                    mc.thePlayer.posX, 
                    mc.thePlayer.posY, 
                    mc.thePlayer.posY, 
                    mc.thePlayer.posZ, 
                    true // Spoofing 'On Ground'
                ));
            }
        } else {
            // RECHARGE: Steady ticks.
            // Moving at ~0.22 ensures distanceAboveLimit (Line 105) is negative,
            // which feeds the horizontalBuffer at Line 118.
            Movement.setSpeed(0.23);
        }
    }

    public static GlockSpeed instance() {
        return instance;
    }
}
