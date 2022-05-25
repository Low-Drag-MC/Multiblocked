package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.mods.kubejs.event.EventJS;

public class NeighborChangedEvent extends EventJS {
    public static final String ID = "mbd.neighbor_changed";
    private final ComponentTileEntity<?> component;

    public NeighborChangedEvent(ComponentTileEntity<?> component) {
        this.component = component;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

}
