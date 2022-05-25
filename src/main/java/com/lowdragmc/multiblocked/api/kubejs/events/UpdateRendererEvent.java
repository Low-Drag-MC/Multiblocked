package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import dev.latvian.mods.kubejs.event.EventJS;

public class UpdateRendererEvent extends EventJS {
    public static final String ID = "mbd.update_renderer";
    private final ComponentTileEntity<?> component;
    public IMultiblockedRenderer renderer;

    public UpdateRendererEvent(ComponentTileEntity<?> component, IMultiblockedRenderer renderer) {
        this.component = component;
        this.renderer = renderer;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public IMultiblockedRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(IMultiblockedRenderer renderer) {
        this.renderer = renderer;
    }
}
