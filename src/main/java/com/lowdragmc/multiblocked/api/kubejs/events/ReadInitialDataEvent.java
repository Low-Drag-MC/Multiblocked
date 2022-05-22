package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.kubejs.event.EventJS;
import net.minecraft.network.PacketBuffer;

public class ReadInitialDataEvent extends EventJS {
    public static final String ID = "mbd.read_initial_data";
    private final ComponentTileEntity<?> component;
    private final PacketBuffer packetBuffer;

    public ReadInitialDataEvent(ComponentTileEntity<?> component, PacketBuffer packetBuffer) {
        this.component = component;
        this.packetBuffer = packetBuffer;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public PacketBuffer getPacketBuffer() {
        return packetBuffer;
    }

}
