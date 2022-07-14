package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.kubejs.event.EventJS;

public class CreateUIEvent extends EventJS {
    public static final String ID = "mbd.create_ui";
    private final ComponentTileEntity<?> component;

    private ModularUI modularUI;

    public CreateUIEvent(ComponentTileEntity<?> component, ModularUI modularUI) {
        this.component = component;
        this.modularUI = modularUI;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public ModularUI getModularUI() {
        return modularUI;
    }

    public void setModularUI(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

    @Override
    public boolean canCancel() {
        return true;
    }
}
