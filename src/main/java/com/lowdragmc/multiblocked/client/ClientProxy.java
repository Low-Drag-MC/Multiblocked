package com.lowdragmc.multiblocked.client;

import com.lowdragmc.multiblocked.CommonProxy;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
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
        e.enqueueWork(MbdComponents::clientLastWork);
    }
}
