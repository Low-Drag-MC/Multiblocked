package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.kubejs.event.EventJS;

public class StatusChangedEvent extends EventJS {
    public static final String ID = "mbd.status_changed";
    private final ComponentTileEntity<?> component;
    private String status;

    public StatusChangedEvent(ComponentTileEntity<?> component, String status) {
        this.component = component;
        this.status = status;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean canCancel() {
        return true;
    }
}
