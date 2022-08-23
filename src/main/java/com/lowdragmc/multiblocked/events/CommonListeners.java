package com.lowdragmc.multiblocked.events;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.pattern.BlockPattern;
import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * Author: KilaBash
 * Date: 2022/04/26
 * Description:
 */
@Mod.EventBusSubscriber(modid = Multiblocked.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonListeners {
    
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        IWorld world = event.getWorld();
        if (!world.isClientSide() && world instanceof ServerWorld) {
            ((ServerWorld) world).getServer().tell(new TickDelayedTask(0, () -> MultiblockWorldSavedData.getOrCreate((World) world)
                    .getControllerInChunk(event.getChunk().getPos())
                    .forEach(MultiblockState::onChunkLoad)));
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        IWorld world = event.getWorld();
        if (!world.isClientSide() && world instanceof ServerWorld) {
            MultiblockWorldSavedData.getOrCreate((World) world)
                    .getControllerInChunk(event.getChunk().getPos())
                    .forEach(MultiblockState::onChunkUnload);
        }
    }

    @SubscribeEvent
    public static void onWorldUnLoad(WorldEvent.Unload event) {
        IWorld world = event.getWorld();
        if (!world.isClientSide() && world instanceof World) {
            MultiblockWorldSavedData.getOrCreate((World) world).releaseExecutorService();
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World level = event.getWorld();
        if (level.isClientSide) return;
        ItemStack held = event.getPlayer().getItemInHand(event.getHand());
        ControllerDefinition[] definitions = MbdComponents.checkNoNeedController(held);
        if (definitions.length > 0) {
            BlockPos pos = event.getPos();
            Direction face = event.getFace();
            PlayerEntity player= event.getPlayer();
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
                    TileEntity oldBlockEntity = level.getBlockEntity(pos);
                    if (oldBlockEntity instanceof ControllerTileEntity) {
                        return;
                    }
                    for (Direction facing : facings) {
                        if (pattern.checkPatternAt(worldState, pos, facing, false, inputCapabilities, outputCapabilities)) {
                            CompoundNBT oldNbt = null;
                            if (oldBlockEntity != null) {
                                oldNbt = oldBlockEntity.serializeNBT();
                            }
                            level.setBlockAndUpdate(pos, MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(definition.location).defaultBlockState());
                            TileEntity newBlockEntity = level.getBlockEntity(pos);
                            if (newBlockEntity instanceof ControllerTileEntity) {
                                ControllerTileEntity controller = (ControllerTileEntity) newBlockEntity;
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
