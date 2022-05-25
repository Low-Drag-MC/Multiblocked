package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.network.FriendlyByteBuf;

public class WriteInitialDataEvent extends EventJS {
    public static final String ID = "mbd.write_initial_data";
    private final ComponentTileEntity<?> component;
    private final FriendlyByteBuf packetBuffer;

    public WriteInitialDataEvent(ComponentTileEntity<?> component, FriendlyByteBuf packetBuffer) {
        this.component = component;
        this.packetBuffer = packetBuffer;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public FriendlyByteBuf getPacketBuffer() {
        return packetBuffer;
    }

}
