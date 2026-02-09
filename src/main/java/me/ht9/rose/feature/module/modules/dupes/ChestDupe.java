package me.ht9.rose.feature.module.modules.dupes;

import me.ht9.rose.event.bus.annotation.SubscribeEvent;
import me.ht9.rose.event.events.PacketEvent;
import me.ht9.rose.event.events.TickEvent;
import me.ht9.rose.feature.module.Module;
import me.ht9.rose.feature.module.annotation.Description;
import net.minecraft.src.*;

@Description("Breaks chest while GUI is open. Left-click for 0-stack, Right-click for -1.")
public final class ChestDupe extends Module {
    private static final ChestDupe instance = new ChestDupe();

    @Override
    public void onEnable() {
        if (mc.thePlayer == null || mc.objectMouseOver == null) {
            this.toggle();
            return;
        }

        MovingObjectPosition mop = mc.objectMouseOver;
        
        if (mop.typeOfHit == EnumMovingObjectType.TILE && mc.currentScreen instanceof GuiChest) {
            int x = mop.blockX;
            int y = mop.blockY;
            int z = mop.blockZ;
            
            if (mc.theWorld.getBlockId(x, y, z) == Block.chest.blockID) {
                mc.getSendQueue().addToSendQueue(new Packet14BlockDig(0, x, y, z, mop.sideHit));
                mc.getSendQueue().addToSendQueue(new Packet14BlockDig(2, x, y, z, mop.sideHit));
                mc.thePlayer.addChatMessage("Chest broken. GUI Locked.");
            }
        } else {
            mc.thePlayer.addChatMessage("Open the chest first!");
            this.toggle();
        }
    }

    @SubscribeEvent
    public void onPacket(PacketEvent event) {
        // Use your packet() getter from the PacketEvent class
        if (!event.serverBound() && event.packet() instanceof Packet101CloseWindow) {
            // Check if module is enabled using the field 'enabled'
            if (this.enabled) { 
                event.setCancelled(true);
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        // Check if module is enabled using the field 'enabled'
        if (mc.currentScreen == null && this.enabled) {
            this.toggle();
        }
    }

    public static ChestDupe instance() { return instance; }
}
