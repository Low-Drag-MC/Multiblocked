package com.lowdragmc.multiblocked.persistence;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lowdragmc.lowdraglib.utils.DummyWorld;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MultiblockWorldSavedData extends WorldSavedData {

    // ********************************* dummy ********************************* //

    private static final MultiblockWorldSavedData DUMMY = new MultiblockWorldSavedData("dummy"){
        @Override
        public void addMapping(MultiblockState state) {
        }

        @Override
        public void addLoading(ComponentTileEntity<?> tileEntity) {
        }

        @Override
        public void createExecutorService() {
        }
    };

    @OnlyIn(Dist.CLIENT)
    public static Set<BlockPos> modelDisabled;
    @OnlyIn(Dist.CLIENT)
    public static Map<BlockPos, Collection<BlockPos>> multiDisabled;

    public final static ThreadLocal<Boolean> isBuildingChunk = ThreadLocal.withInitial(()-> Boolean.FALSE);

    static {
        if (Multiblocked.isClient()) {
            modelDisabled = new HashSet<>();
            multiDisabled = new HashMap<>();
        }
    }

    private static WeakReference<World> worldRef;

    public static MultiblockWorldSavedData getOrCreate(World world) {
        if (world == null || world instanceof DummyWorld) {
            return DUMMY;
        }
        if (world instanceof ServerWorld) {
            worldRef = new WeakReference<>(world);
            MultiblockWorldSavedData mbwsd = ((ServerWorld) world).getDataStorage().computeIfAbsent(() -> new MultiblockWorldSavedData(Multiblocked.MODNAME), Multiblocked.MODNAME);
            worldRef = null;
            return mbwsd;
        }
        return DUMMY;
    }

    public final Map<BlockPos, MultiblockState> mapping;
    public final Map<ChunkPos, Set<MultiblockState>> chunkPosMapping;
    public final Map<BlockPos, ComponentTileEntity<?>> loading;

    public MultiblockWorldSavedData(String name) { // Also constructed Reflectively by MapStorage
        super(name);
        this.mapping = new Object2ObjectOpenHashMap<>();
        this.chunkPosMapping = new HashMap<>();
        this.loading = new Object2ObjectOpenHashMap<>();
    }

    public static void clearDisabled() {
        modelDisabled.clear();
        multiDisabled.clear();
    }

    public Collection<MultiblockState> getControllerInChunk(ChunkPos chunkPos) {
        return new ArrayList<>(chunkPosMapping.getOrDefault(chunkPos, Collections.emptySet()));
    }

    public Collection<ComponentTileEntity<?>> getLoadings() {
        return loading.values();
    }

    public void addMapping(MultiblockState state) {
        this.mapping.put(state.controllerPos, state);
        for (BlockPos blockPos : state.getCache()) {
            chunkPosMapping.computeIfAbsent(new ChunkPos(blockPos), c->new HashSet<>()).add(state);
        }
        setDirty(true);
    }

    public void removeMapping(MultiblockState state) {
        this.mapping.remove(state.controllerPos);
        for (Set<MultiblockState> set : chunkPosMapping.values()) {
            set.remove(state);
        }
        setDirty(true);
    }

    public void addLoading(ComponentTileEntity<?> tileEntity) {
        ComponentTileEntity<?> last = loading.put(tileEntity.getBlockPos(), tileEntity);
        if (last != tileEntity) {
            if (last instanceof IAsyncThreadUpdate) {
                asyncComponents.remove(last);
                if (asyncComponents.isEmpty()) {
                    releaseExecutorService();
                }
            }
            if (tileEntity instanceof IAsyncThreadUpdate) {
                asyncComponents.add((IAsyncThreadUpdate) tileEntity);
                createExecutorService();
            }
        }
    }

    public void removeLoading(BlockPos componentPos) {
        ComponentTileEntity<?> component = loading.remove(componentPos);
        if (component instanceof IAsyncThreadUpdate) {
            asyncComponents.remove(component);
            if (asyncComponents.isEmpty()) {
                releaseExecutorService();
            }
        }
    }

     @OnlyIn(Dist.CLIENT)
    public static void removeDisableModel(BlockPos controllerPos) {
        Collection<BlockPos> poses = multiDisabled.remove(controllerPos);
        if (poses == null) return;
        modelDisabled.clear();
        multiDisabled.values().forEach(modelDisabled::addAll);
        updateRenderChunk(poses);
    }

     @OnlyIn(Dist.CLIENT)
    private static void updateRenderChunk(Collection<BlockPos> poses) {
         ClientWorld world = Minecraft.getInstance().level;
        if (world != null) {
            for (BlockPos pos : poses) {
                BlockState state = world.getBlockState(pos);
                world.sendBlockUpdated(pos, state, state, Constants.BlockFlags.RERENDER_MAIN_THREAD);
            }
        }
    }

     @OnlyIn(Dist.CLIENT)
    public static void addDisableModel(BlockPos controllerPos, Collection<BlockPos> poses) {
        multiDisabled.put(controllerPos, poses);
        modelDisabled.addAll(poses);
        updateRenderChunk(poses);
    }

     @OnlyIn(Dist.CLIENT)
    public static boolean isModelDisabled(BlockPos pos) {
        if (isBuildingChunk.get()) {
            return modelDisabled.contains(pos);
        }
        return false;
    }

    @Override
    public void load(CompoundNBT nbt) {
        for (String key : nbt.getAllKeys()) {
            BlockPos pos = BlockPos.of(Long.parseLong(key));
            MultiblockState state = new MultiblockState(worldRef.get(), pos);
            state.deserialize(new PacketBuffer(Unpooled.copiedBuffer(nbt.getByteArray(key))));
            this.mapping.put(pos, state);
            for (BlockPos blockPos : state.getCache()) {
                chunkPosMapping.computeIfAbsent(new ChunkPos(blockPos), c->new HashSet<>()).add(state);
            }
        }
    }

    @Nonnull
    @Override
    public CompoundNBT save(@Nonnull CompoundNBT compound) {
        this.mapping.forEach((pos, state) -> {
            ByteBuf byteBuf = Unpooled.buffer();
            state.serialize(new PacketBuffer(byteBuf));
            compound.putByteArray(String.valueOf(pos.asLong()), Arrays.copyOfRange(byteBuf.array(), 0, byteBuf.writerIndex()));
        });
        return compound;
    }

    // ********************************* thread for searching ********************************* //
    private final CopyOnWriteArrayList<IAsyncThreadUpdate> asyncComponents = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService executorService;
    private final static ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("Multiblocked Async Thread-%d")
            .setDaemon(true)
            .build();
    private static final ThreadLocal<Boolean> IN_SERVICE = ThreadLocal.withInitial(()->false);
    private long periodID = Long.MIN_VALUE;

    public void createExecutorService() {
        if (executorService != null && !executorService.isShutdown()) return;
        executorService = Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
        executorService.scheduleAtFixedRate(this::searchingTask, 0, 250, TimeUnit.MILLISECONDS);
    }

    private void searchingTask() {
        try {
            IN_SERVICE.set(true);
            for (IAsyncThreadUpdate asyncComponent : asyncComponents) {
                asyncComponent.asyncThreadLogic(periodID);
            }
            IN_SERVICE.set(false);
        } catch (Throwable e) {
            Multiblocked.LOGGER.error("asyncThreadLogic error: {}", e.getMessage());
        }
        periodID++;
    }

    public static boolean isThreadService() {
        return IN_SERVICE.get();
    }

    public void releaseExecutorService() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        executorService = null;
    }

    public long getPeriodID() {
        return periodID;
    }

}
