package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.mods.kubejs.event.EventJS;

public class InitTraitUIEvent extends EventJS {
    public static final String ID = "mbd.trait_ui";
    private final ComponentTileEntity<?> component;

    private final WidgetGroup widgetGroup;

    public InitTraitUIEvent(ComponentTileEntity<?> component, WidgetGroup widgetGroup) {
        this.component = component;
        this.widgetGroup = widgetGroup;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public WidgetGroup getWidgetGroup() {
        return widgetGroup;
    }
}
