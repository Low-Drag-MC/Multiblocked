package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.kubejs.event.EventJS;

public class UpdateTickEvent extends EventJS {
    public static final String ID = "mbd.update_tick";
    private final ComponentTileEntity<?> component;

    public UpdateTickEvent(ComponentTileEntity<?> component) {
        this.component = component;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

}
