package me.ht9.rose.feature.module.modules.dupe;

import me.ht9.rose.event.bus.annotation.SubscribeEvent;
import me.ht9.rose.event.events.UpdateEvent; // Assuming you have a standard update event
import me.ht9.rose.feature.module.Module;
import me.ht9.rose.feature.module.annotation.Description;
import net.minecraft.src.*;
import org.lwjgl.input.Keyboard;

@Description("Breaks chests while keeping the GUI open for 0-stack/negative-stack duping.")
public final class ChestDupe extends Module {
    private static final ChestDupe instance = new ChestDupe();

    public ChestDupe() {
        // Module constructor
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null || mc.objectMouseOver == null) return;

        // Check if we are looking at a chest
        if (mc.objectMouseOver.typeOfHit == EnumMovingObjectType.TILE) {
            int x = mc.objectMouseOver.blockX;
            int y = mc.objectMouseOver.blockY;
            int z = mc.objectMouseOver.blockZ;
            int id = mc.theWorld.getBlockId(x, y, z);

            if (id == Block.chest.blockID) {
                // 1. Send the break packets immediately (Start and Stop)
                // This breaks the chest server-side without closing your client GUI
                mc.getSendQueue().addToSendQueue(new Packet14BlockDig(0, x, y, z, mc.objectMouseOver.sideHit));
                mc.getSendQueue().addToSendQueue(new Packet14BlockDig(2, x, y, z, mc.objectMouseOver.sideHit));
                
                mc.thePlayer.addChatMessage("Chest broken. GUI Locked. Get that -1 stack!");
            }
        }
    }

    // This is the "Glue": It prevents the GUI from closing when the server says the block is gone
    @SubscribeEvent
    public void onPacketReceive(PacketEvent event) { // Replace with your actual Packet Event
        if (!this.isEnabled()) return;

        // Block the server-side Close Window packet
        if (event.getPacket() instanceof Packet101CloseWindow) {
            event.setCancelled(true);
        }
        
        // Block the Packet53 (Block Change) if it tries to tell the client the chest is now Air
        if (event.getPacket() instanceof Packet53BlockChange) {
            Packet53BlockChange p = (Packet53BlockChange) event.getPacket();
            if (p.type == 0) { // If it's turning into air
                 event.setCancelled(true);
            }
        }
    }

    public static ChestDupe instance() { 
        return instance; 
    }
}
