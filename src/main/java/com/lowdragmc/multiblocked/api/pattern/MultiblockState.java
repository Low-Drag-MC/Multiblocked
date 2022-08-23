package com.lowdragmc.multiblocked.api.pattern;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.pattern.error.PatternError;
import com.lowdragmc.multiblocked.api.pattern.error.PatternStringError;
import com.lowdragmc.multiblocked.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.multiblocked.api.pattern.util.PatternMatchContext;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.lowdragmc.multiblocked.network.MultiblockedNetworking;
import com.lowdragmc.multiblocked.network.s2c.SPacketRemoveDisabledRendering;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MultiblockState {
    public final static PatternError UNLOAD_ERROR = new PatternStringError("multiblocked.pattern.error.chunk");
    public final static PatternError UNINIT_ERROR = new PatternStringError("multiblocked.pattern.error.init");

    public BlockPos pos;
    public BlockState state;
    public BlockEntity tileEntity;
    public boolean tileEntityInitialized;
    public PatternMatchContext matchContext;
    public Map<SimplePredicate, Integer> globalCount;
    public TraceabilityPredicate predicate;
    public IO io;
    public PatternError error;
    public final Level world;
    public final BlockPos controllerPos;
    public ControllerTileEntity lastController;

    // persist
    public LongOpenHashSet cache;

    public MultiblockState(Level world, BlockPos controllerPos) {
        this.world = world;
        this.controllerPos = controllerPos;
        this.error = UNINIT_ERROR;
    }

    public void clean() {
        this.matchContext = new PatternMatchContext();
        this.globalCount = new HashMap<>();
        cache = new LongOpenHashSet();
    }

    public boolean update(BlockPos posIn, TraceabilityPredicate predicate) {
        this.pos = posIn;
        this.state = null;
        this.tileEntity = null;
        this.tileEntityInitialized = false;
        this.predicate = predicate;
        this.error = null;
        if (!world.isLoaded(posIn)) {
            error = UNLOAD_ERROR;
            return false;
        }
        return true;
    }

    public ControllerTileEntity getController() {
        if (world.isLoaded(controllerPos)) {
            BlockEntity tileEntity = world.getBlockEntity(controllerPos);
            if (tileEntity instanceof ControllerTileEntity) {
                return lastController = (ControllerTileEntity) tileEntity;
            }
        } else {
            error = UNLOAD_ERROR;
        }
        return null;
    }

    public boolean isFormed() {
        return error == null;
    }

    public void setError(PatternError error) {
        this.error = error;
        if (error != null) {
            error.setWorldState(this);
        }
    }

    public PatternMatchContext getMatchContext() {
        return matchContext;
    }

    public BlockState getBlockState() {
        if (this.state == null) {
            this.state = this.world.getBlockState(this.pos);
        }

        return this.state;
    }

    @Nullable
    public BlockEntity getTileEntity() {
        if (this.tileEntity == null && !this.tileEntityInitialized) {
            this.tileEntity = this.world.getBlockEntity(this.pos);
            this.tileEntityInitialized = true;
        }

        return this.tileEntity;
    }

    public BlockPos getPos() {
        return this.pos.immutable();
    }

    public BlockState getOffsetState(Direction face) {
        if (pos instanceof BlockPos.MutableBlockPos) {
            ((BlockPos.MutableBlockPos) pos).move(face);
            BlockState blockState = world.getBlockState(pos);
            ((BlockPos.MutableBlockPos) pos).move(face.getOpposite());
            return blockState;
        }
        return world.getBlockState(this.pos.relative(face));
    }

    public Level getWorld() {
        return world;
    }

    public void addPosCache(BlockPos pos) {
        cache.add(pos.asLong());
    }

    public boolean isPosInCache(BlockPos pos) {
        return cache.contains(pos.asLong());
    }

    public Collection<BlockPos> getCache() {
        return cache.stream().map(BlockPos::of).collect(Collectors.toList());
    }

    public void onBlockStateChanged(BlockPos pos) {
        if (pos.equals(controllerPos)) {
            if (this.getMatchContext().containsKey("renderMask")) {
                MultiblockedNetworking.sendToAll(new SPacketRemoveDisabledRendering(controllerPos));
            }
            if (lastController != null) {
                lastController.onStructureInvalid();
                if (lastController.hasOldBlock()) {
                    lastController.resetOldBlock(world, controllerPos);
                }
            }
            MultiblockWorldSavedData mbds = MultiblockWorldSavedData.getOrCreate(world);
            mbds.removeMapping(this);
            mbds.removeLoading(controllerPos);
        } else if (error != UNLOAD_ERROR) {
            ControllerTileEntity controller = getController();
            boolean hasRenderMask = getMatchContext().containsKey("renderMask");
            if (controller != null && !controller.checkPattern()) {
                controller.onStructureInvalid();
                if (controller.hasOldBlock()) {
                    if (hasRenderMask) {
                        MultiblockedNetworking.sendToAll(new SPacketRemoveDisabledRendering(controllerPos));
                    }
                    MultiblockWorldSavedData.getOrCreate(world).removeLoading(controllerPos);
                    controller.resetOldBlock(world, controllerPos);
                }
                MultiblockWorldSavedData.getOrCreate(world).removeMapping(this);
            } else if (controller != null){
                controller.onStructureFormed();
            }
        }
    }

    public void onChunkLoad() {
        try {
            ControllerTileEntity controller = getController();
            if (controller != null) {
                if (controller.checkPattern()) {
                    if (!controller.needAlwaysUpdate()) {
                        MultiblockWorldSavedData.getOrCreate(world).addLoading(controller);
                    }
                    if (controller.getCapabilitiesProxy() == null) {
                        controller.onStructureFormed();
                    }
                } else {
                    error = UNLOAD_ERROR;
                }
            }
        } catch (Throwable e) { // if controller loading failed.
            MultiblockWorldSavedData.getOrCreate(world).removeMapping(this);
            Multiblocked.LOGGER.error("An error while loading the controller world: {} pos: {}, {}", world.dimensionType(), controllerPos, e);
        }
    }

    public void onChunkUnload() {
        ControllerTileEntity controller = getController();
        if (controller != null) {
            error = UNLOAD_ERROR;
            if (!controller.needAlwaysUpdate()) {
                MultiblockWorldSavedData.getOrCreate(world).removeLoading(controllerPos);
            }
        } else {
            MultiblockWorldSavedData.getOrCreate(world).removeLoading(controllerPos);
        }
    }

    public void deserialize(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        cache = new LongOpenHashSet();
        for (int i = 0; i < size; i++) {
            cache.add(buffer.readVarLong());
        }
    }

    public void serialize(FriendlyByteBuf buffer) {
        buffer.writeVarInt(cache.size());
        for (Long aLong : cache) {
            buffer.writeVarLong(aLong);
        }
    }
}
