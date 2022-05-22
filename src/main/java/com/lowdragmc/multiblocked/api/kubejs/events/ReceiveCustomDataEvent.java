package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.kubejs.event.EventJS;
import net.minecraft.network.PacketBuffer;

public class ReceiveCustomDataEvent extends EventJS {
    public static final String ID = "mbd.receive_custom_data";
    private final ComponentTileEntity<?> component;
    private final PacketBuffer packetBuffer;
    private final int dataId;

    public ReceiveCustomDataEvent(ComponentTileEntity<?> component, int dataId, PacketBuffer packetBuffer) {
        this.component = component;
        this.packetBuffer = packetBuffer;
        this.dataId = dataId;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public PacketBuffer getPacketBuffer() {
        return packetBuffer;
    }

    public int getDataId() {
        return dataId;
    }
}
