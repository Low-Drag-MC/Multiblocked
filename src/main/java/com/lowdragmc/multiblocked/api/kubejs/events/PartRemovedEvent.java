package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import dev.latvian.mods.kubejs.event.EventJS;

public class PartRemovedEvent extends EventJS {
    public static final String ID = "mbd.part_removed";
    private final ControllerTileEntity controller;

    public PartRemovedEvent(ControllerTileEntity controller) {
        this.controller = controller;
    }

    public ControllerTileEntity getController() {
        return controller;
    }

}
