package com.lowdragmc.multiblocked.client;

import com.lowdragmc.multiblocked.CommonProxy;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientProxy extends CommonProxy {

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(MbdComponents::clientLastWork);
    }
}
