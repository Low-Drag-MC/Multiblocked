package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.network.FriendlyByteBuf;

public class ReceiveCustomDataEvent extends EventJS {
    public static final String ID = "mbd.receive_custom_data";
    private final ComponentTileEntity<?> component;
    private final FriendlyByteBuf packetBuffer;
    private final int dataId;

    public ReceiveCustomDataEvent(ComponentTileEntity<?> component, int dataId, FriendlyByteBuf packetBuffer) {
        this.component = component;
        this.packetBuffer = packetBuffer;
        this.dataId = dataId;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public FriendlyByteBuf getPacketBuffer() {
        return packetBuffer;
    }

    public int getDataId() {
        return dataId;
    }
}
