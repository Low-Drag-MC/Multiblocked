package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.IComponent;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import dev.latvian.mods.kubejs.event.EventJS;

public class UpdateRendererEvent extends EventJS {
    public static final String ID = "mbd.update_renderer";
    private final IComponent component;
    public IMultiblockedRenderer renderer;

    public UpdateRendererEvent(IComponent component, IMultiblockedRenderer renderer) {
        this.component = component;
        this.renderer = renderer;
    }

    public IComponent getComponent() {
        return component;
    }

    public IMultiblockedRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(IMultiblockedRenderer renderer) {
        this.renderer = renderer;
    }
}
