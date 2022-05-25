package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import dev.latvian.mods.kubejs.event.EventJS;

public class StructureFormedEvent extends EventJS {
    public static final String ID = "mbd.structure_formed";
    private final ControllerTileEntity controller;

    public StructureFormedEvent(ControllerTileEntity controller) {
        this.controller = controller;
    }

    public ControllerTileEntity getController() {
        return controller;
    }

}
