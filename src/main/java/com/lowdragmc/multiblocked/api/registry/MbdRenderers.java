package com.lowdragmc.multiblocked.api.registry;


import com.google.common.collect.Maps;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.lowdragmc.multiblocked.client.renderer.impl.MBDBlockStateRenderer;
import com.lowdragmc.multiblocked.client.renderer.impl.MBDIModelRenderer;

import java.util.Map;

public class MbdRenderers {
    public static final Map<String, IMultiblockedRenderer> RENDERER_REGISTRY = Maps.newHashMap();

    public static void registerRenderer(IMultiblockedRenderer renderer) {
        RENDERER_REGISTRY.put(renderer.getType().toLowerCase(), renderer);
    }

    public static IMultiblockedRenderer getRenderer(String type) {
        return RENDERER_REGISTRY.get(type.toLowerCase());
    }

    public static void registerRenderers() {
        registerRenderer(MBDIModelRenderer.INSTANCE);
        registerRenderer(MBDBlockStateRenderer.INSTANCE);
//        registerRenderer(B3DRenderer.INSTANCE);
//        registerRenderer(OBJRenderer.INSTANCE);
//        registerRenderer(TextureParticleRenderer.INSTANCE);
//        registerRenderer(GTRenderer.INSTANCE);
        if (Multiblocked.isModLoaded(Multiblocked.MODID_GEO)) {
//            registerRenderer(GeoComponentRenderer.INSTANCE);
        }
    }
}
