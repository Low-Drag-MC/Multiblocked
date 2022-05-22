package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import dev.latvian.kubejs.event.EventJS;

public class StructureInvalidEvent extends EventJS {
    public static final String ID = "mbd.structure_invalid";
    private final ControllerTileEntity controller;

    public StructureInvalidEvent(ControllerTileEntity controller) {
        this.controller = controller;
    }

    public ControllerTileEntity getController() {
        return controller;
    }

}
