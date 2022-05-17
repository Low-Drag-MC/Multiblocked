package com.lowdragmc.multiblocked.api.tile.part;

import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A TileEntity that defies the part of multi.
 *
 * part of the multiblock.
 */
public abstract class PartTileEntity<T extends PartDefinition> extends ComponentTileEntity<T> {

    public Set<BlockPos> controllerPos = new HashSet<>();

    public PartTileEntity(T definition) {
        super(definition);
    }

    @Override
    public boolean isFormed() {
        for (BlockPos blockPos : controllerPos) {
            TileEntity controller = level.getBlockEntity(blockPos);
            if (controller instanceof ControllerTileEntity && ((ControllerTileEntity) controller).isFormed()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IMultiblockedRenderer updateCurrentRenderer() {
//        if (definition.dynamicRenderer != null) {
//
//        }
        if (definition.workingRenderer != null) {
            for (ControllerTileEntity controller : getControllers()) {
                if (controller.isFormed() && controller.getStatus().equals("working")) {
                    return definition.workingRenderer;
                }
            }
        }
        return super.updateCurrentRenderer();
    }

    public boolean canShared() {
        return definition.canShared;
    }
    
    public List<ControllerTileEntity> getControllers() {
        List<ControllerTileEntity> result = new ArrayList<>();
        for (BlockPos blockPos : controllerPos) {
            TileEntity controller = level.getBlockEntity(blockPos);
            if (controller instanceof ControllerTileEntity && ((ControllerTileEntity) controller).isFormed()) {
                result.add((ControllerTileEntity) controller);
            }
        }
        return result;
    }

    public void addedToController(@Nonnull ControllerTileEntity controller){
        if (controllerPos.add(controller.getBlockPos())) {
            writeCustomData(-1, this::writeControllersToBuffer);
//            if (definition.partAddedToMulti != null) {
//            }
            setStatus("idle");
        }
    }

    public void removedFromController(@Nonnull ControllerTileEntity controller){
        if (controllerPos.remove(controller.getBlockPos())) {
            writeCustomData(-1, this::writeControllersToBuffer);
//            if (definition.partRemovedFromMulti != null) {
//            }
            if (getControllers().isEmpty()) {
                setStatus("unformed");
            }
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        writeControllersToBuffer(buf);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        readControllersFromBuffer(buf);
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        if (dataId == -1) {
            readControllersFromBuffer(buf);
        } else {
            super.receiveCustomData(dataId, buf);
        }
    }

    @Override
    public void load(@Nonnull BlockState state, @Nonnull CompoundNBT compound) {
        super.load(state, compound);
        for (MultiblockState multiblockState : MultiblockWorldSavedData.getOrCreate(level).getControllerInChunk(new ChunkPos(getBlockPos()))) {
            if(multiblockState.isPosInCache(getBlockPos())) {
                controllerPos.add(multiblockState.controllerPos);
            }
        }
    }

    private void writeControllersToBuffer(PacketBuffer buffer) {
        buffer.writeVarInt(controllerPos.size());
        for (BlockPos pos : controllerPos) {
            buffer.writeBlockPos(pos);
        }
    }

    private void readControllersFromBuffer(PacketBuffer buffer) {
        int size = buffer.readVarInt();
        controllerPos.clear();
        for (int i = size; i > 0; i--) {
            controllerPos.add(buffer.readBlockPos());
        }
    }

    public static class PartSimpleTileEntity extends PartTileEntity<PartDefinition> {

        public PartSimpleTileEntity(PartDefinition definition) {
            super(definition);
        }
    }

}
