package com.lowdragmc.multiblocked.events;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Author: KilaBash
 * Date: 2022/04/26
 * Description:
 */
@Mod.EventBusSubscriber(modid = Multiblocked.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientListeners {

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (Minecraft.getInstance().level == event.getWorld()) {
            MultiblockWorldSavedData.clearDisabled();
        }
    }
}
