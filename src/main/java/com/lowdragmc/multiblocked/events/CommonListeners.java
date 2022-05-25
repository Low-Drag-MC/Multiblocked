package com.lowdragmc.multiblocked.events;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Author: KilaBash
 * Date: 2022/04/26
 * Description:
 */
@Mod.EventBusSubscriber(modid = Multiblocked.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonListeners {
    
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        LevelAccessor world = event.getWorld();
        if (!world.isClientSide() && world instanceof ServerLevel) {
            ((ServerLevel) world).getServer().tell(new TickTask(0, () -> MultiblockWorldSavedData.getOrCreate((Level) world)
                    .getControllerInChunk(event.getChunk().getPos())
                    .forEach(MultiblockState::onChunkLoad)));
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        LevelAccessor world = event.getWorld();
        if (!world.isClientSide() && world instanceof ServerLevel) {
            MultiblockWorldSavedData.getOrCreate((Level) world)
                    .getControllerInChunk(event.getChunk().getPos())
                    .forEach(MultiblockState::onChunkUnload);
        }
    }

    @SubscribeEvent
    public static void onWorldUnLoad(WorldEvent.Unload event) {
        LevelAccessor world = event.getWorld();
        if (!world.isClientSide() && world instanceof Level) {
            MultiblockWorldSavedData.getOrCreate((Level) world).releaseExecutorService();
        }
    }
}
