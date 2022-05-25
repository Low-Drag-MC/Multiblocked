package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import dev.latvian.mods.kubejs.event.EventJS;

public class UpdateFormedEvent extends EventJS {
    public static final String ID = "mbd.update_formed";
    private final ControllerTileEntity controller;

    public UpdateFormedEvent(ControllerTileEntity controller) {
        this.controller = controller;
    }

    public ControllerTileEntity getController() {
        return controller;
    }

}
