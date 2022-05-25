package com.lowdragmc.multiblocked.client;

import com.lowdragmc.lowdraglib.utils.CustomResourcePack;
import com.lowdragmc.multiblocked.CommonProxy;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.registry.MbdItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {

    public ClientProxy() {
        Minecraft.getInstance().getResourcePackRepository().addPackFinder(new CustomResourcePack(Multiblocked.location, PackSource.DEFAULT, Multiblocked.MODID, "Multiblocked Extended Resources", 6));
    }

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent e) {
        e.enqueueWork(()->{
            MbdComponents.clientLastWork();
            MbdItems.registerModelsProperties();
        });
    }

    @SubscribeEvent
    public void onRegisterRenderer(EntityRenderersEvent.RegisterRenderers event) {
        MbdComponents.registerBlockEntityRenderer(event);
    }

    @SubscribeEvent
    public void registerTextures(TextureStitchEvent.Pre event) {
        if (event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS)) {
            event.addSprite(new ResourceLocation("multiblocked:void"));
            event.addSprite(new ResourceLocation("multiblocked:blocks/gregtech_base"));
            event.addSprite(new ResourceLocation("multiblocked:blocks/gregtech_front"));
        }
    }
}
