package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import dev.latvian.kubejs.event.EventJS;

public class PartAddedEvent extends EventJS {
    public static final String ID = "mbd.part_added";
    private final ControllerTileEntity controller;

    public PartAddedEvent(ControllerTileEntity controller) {
        this.controller = controller;
    }

    public ControllerTileEntity getController() {
        return controller;
    }
}
