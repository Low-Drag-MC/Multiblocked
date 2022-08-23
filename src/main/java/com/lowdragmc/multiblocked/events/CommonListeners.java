package com.lowdragmc.multiblocked.events;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.pattern.BlockPattern;
import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

import static net.minecraft.Util.NIL_UUID;

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

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getWorld();
        if (level.isClientSide) return;
        ItemStack held = event.getPlayer().getItemInHand(event.getHand());
        ControllerDefinition[] definitions = MbdComponents.checkNoNeedController(held);
        if (definitions.length > 0) {
            BlockPos pos = event.getPos();
            Direction face = event.getFace();
            Player player= event.getPlayer();
            Direction[] facings;
            if (face == null || face.getAxis() == Direction.Axis.Y) {
                facings = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            } else {
                facings = new Direction[]{face};
            }

            for (ControllerDefinition definition : definitions) {
                BlockPattern pattern = definition.getBasePattern();
                if (pattern != null && definition.noNeedController) {
                    Set<MultiblockCapability<?>> inputCapabilities = definition.getRecipeMap().inputCapabilities;
                    Set<MultiblockCapability<?>> outputCapabilities = definition.getRecipeMap().outputCapabilities;
                    MultiblockState worldState = new MultiblockState(level, pos);
                    BlockState oldState = level.getBlockState(pos);
                    BlockEntity oldBlockEntity = level.getBlockEntity(pos);
                    if (oldBlockEntity instanceof ControllerTileEntity) {
                        return;
                    }
                    for (Direction facing : facings) {
                        if (pattern.checkPatternAt(worldState, pos, facing, false, inputCapabilities, outputCapabilities)) {
                            CompoundTag oldNbt = null;
                            if (oldBlockEntity != null) {
                                oldNbt = oldBlockEntity.saveWithFullMetadata();
                            }
                            level.setBlockAndUpdate(pos, MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(definition.location).defaultBlockState());
                            BlockEntity newBlockEntity = level.getBlockEntity(pos);
                            if (newBlockEntity instanceof ControllerTileEntity controller) {
                                controller.state = worldState;
                                if (controller.checkCatalystPattern(player, event.getHand(), held)) { // formed
                                    controller.saveOldBlock(oldState, oldNbt);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
