package com.lowdragmc.multiblocked.client;

import com.lowdragmc.multiblocked.CommonProxy;
import com.lowdragmc.multiblocked.RegistryHandler;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientProxy extends CommonProxy {

    public ClientProxy() {
        super();
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::clientSetup);
    }

    private void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            RenderTypeLookup.setRenderLayer(RegistryHandler.TEST_BLOCK.get(), renderType -> true);
        });
    }
}
