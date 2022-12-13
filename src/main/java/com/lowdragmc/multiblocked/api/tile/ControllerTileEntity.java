package com.lowdragmc.multiblocked.api.tile;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.lowdragmc.lowdraglib.gui.factory.BlockEntityUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.ICapabilityProxyHolder;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.gui.controller.IOPageWidget;
import com.lowdragmc.multiblocked.api.gui.controller.RecipePage;
import com.lowdragmc.multiblocked.api.gui.controller.structure.StructurePageWidget;
import com.lowdragmc.multiblocked.api.kubejs.events.*;
import com.lowdragmc.multiblocked.api.pattern.BlockPattern;
import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import com.lowdragmc.multiblocked.api.tile.part.IPartComponent;
import com.lowdragmc.multiblocked.client.renderer.MultiblockPreviewRenderer;
import com.lowdragmc.multiblocked.persistence.IAsyncThreadUpdate;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import dev.latvian.mods.kubejs.script.ScriptType;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * A TileEntity that defies all controller machines.
 *
 * Head of the multiblock.
 */
public class ControllerTileEntity extends ComponentTileEntity<ControllerDefinition> implements ICapabilityProxyHolder, IAsyncThreadUpdate, IControllerComponent {
    public MultiblockState state;
    protected Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> capabilities;
    private Map<Long, Map<MultiblockCapability<?>, Tuple<IO, Direction>>> settings;
    protected LongOpenHashSet parts;
    protected RecipeLogic recipeLogic;
    protected AABB renderBox;
    protected BlockState oldState;
    protected CompoundTag oldNbt;

    public ControllerTileEntity(ControllerDefinition definition, BlockPos pos, BlockState state) {
        super(definition, pos, state);
    }

    @Nullable
    public BlockPattern getPattern() {
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            DynamicPatternEvent event = new DynamicPatternEvent(this, definition.getBasePattern());
            if (event.post(ScriptType.of(level), DynamicPatternEvent.ID, getSubID())) {
                return null;
            }
            return event.pattern;
        }
        return definition.getBasePattern();
    }
    public RecipeLogic getRecipeLogic() {
        return recipeLogic;
    }

    public RecipeLogic createRecipeLogic() {
        return new RecipeLogic(this);
    }

    @Override
    public void update() {
        super.update();
        if (isFormed()) {
            updateFormed();
        }
    }

    public void updateFormed() {
        if (recipeLogic != null) {
            recipeLogic.update();
        }
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            new UpdateFormedEvent(this).post(ScriptType.of(level), UpdateFormedEvent.ID, getSubID());
        }
    }

    public Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> getCapabilitiesProxy() {
        return capabilities;
    }

    /**
     * Called when its formed, server side only.
     */
    public void onStructureFormed() {
        if (recipeLogic == null) {
            recipeLogic = createRecipeLogic();
        }
        if (status.equals("unformed")) {
            setStatus("idle");
        }
        // init capabilities
        Map<Long, EnumMap<IO, Set<MultiblockCapability<?>>>> capabilityMap = state.getMatchContext().get("capabilities");
        Map<Long, Set<String>> slotsMap = state.getMatchContext().get("slots");
        if (capabilityMap != null) {
            capabilities = Tables.newCustomTable(new EnumMap<>(IO.class), Object2ObjectOpenHashMap::new);
            for (Map.Entry<Long, EnumMap<IO, Set<MultiblockCapability<?>>>> entry : capabilityMap.entrySet()) {
                BlockEntity tileEntity = level.getBlockEntity(BlockPos.of(entry.getKey()));
                if (tileEntity != null) {
                    if (settings != null) {
                        Map<MultiblockCapability<?>, Tuple<IO, Direction>> caps = settings.get(entry.getKey());
                        if (caps != null) {
                            for (Map.Entry<MultiblockCapability<?>, Tuple<IO, Direction>> ioEntry : caps.entrySet()) {
                                MultiblockCapability<?> capability = ioEntry.getKey();
                                Tuple<IO, Direction> tuple = ioEntry.getValue();
                                if (tuple == null || capability == null) continue;
                                IO io = tuple.getA();
                                Direction facing = tuple.getB();
                                if (capability.isBlockHasCapability(io, tileEntity)) {
                                    if (!capabilities.contains(io, capability)) {
                                        capabilities.put(io, capability, new Long2ObjectOpenHashMap<>());
                                    }
                                    CapabilityProxy<?> proxy = capability.createProxy(io, tileEntity, facing, slotsMap);
                                    capabilities.get(io, capability).put(entry.getKey().longValue(), proxy);
                                }
                            }
                        }
                    } else {
                        entry.getValue().forEach((io,set)->{
                            for (MultiblockCapability<?> capability : set) {
                                if (capability.isBlockHasCapability(io, tileEntity)) {
                                    if (!capabilities.contains(io, capability)) {
                                        capabilities.put(io, capability, new Long2ObjectOpenHashMap<>());
                                    }
                                    CapabilityProxy<?> proxy = capability.createProxy(io, tileEntity, Direction.UP, slotsMap);
                                    capabilities.get(io, capability).put(entry.getKey().longValue(), proxy);
                                }
                            }
                        });
                    }
                }
            }
        }

        settings = null;

        // init parts
        parts = state.getMatchContext().get("parts");
        if (parts != null) {
            for (Long pos : parts) {
                BlockEntity tileEntity = level.getBlockEntity(BlockPos.of(pos));
                if (tileEntity instanceof IPartComponent) {
                    ((IPartComponent) tileEntity).addedToController(this);
                }
            }
        }

        writeCustomData(-1, this::writeState);
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            new StructureFormedEvent(this).post(ScriptType.of(level), StructureFormedEvent.ID, getSubID());
        }
    }

    public void onStructureInvalid() {
        if (recipeLogic != null) {
            recipeLogic.inValid();
        }
        recipeLogic = null;
        setStatus("unformed");
        // invalid parts
        if (parts != null) {
            for (Long pos : parts) {
                BlockEntity tileEntity = level.getBlockEntity(BlockPos.of(pos));
                if (tileEntity instanceof IPartComponent) {
                    ((IPartComponent) tileEntity).removedFromController(this);
                }
            }
            parts = null;
        }
        capabilities = null;

        writeCustomData(-1, this::writeState);
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            new StructureInvalidEvent(this).post(ScriptType.of(level), StructureInvalidEvent.ID, getSubID());
        }
    }

    @Override
    public MultiblockState getMultiblockState() {
        return state;
    }

    @Override
    public void setMultiblockState(MultiblockState multiblockState) {
        this.state = multiblockState;
    }

    public boolean hasOldBlock() {
        return getDefinition().noNeedController && oldState != null && this.level != null;
    }

    public void resetOldBlock(Level level, BlockPos pos) {
        level.setBlockAndUpdate(pos, oldState);
        if (oldNbt != null) {
            BlockEntity blockEntity = BlockEntity.loadStatic(pos, oldState, oldNbt);
            if (blockEntity != null) {
                this.level.setBlockEntity(blockEntity);
            }
        }
    }

    @Override
    public void receiveCustomData(int dataId, FriendlyByteBuf buf) {
        if (dataId == -1) {
            readState(buf);
            scheduleChunkForRenderUpdate();
        } else {
            super.receiveCustomData(dataId, buf);
        }
    }

    @Override
    public void writeInitialSyncData(FriendlyByteBuf buf) {
        super.writeInitialSyncData(buf);
        writeState(buf);
    }

    @Override
    public void receiveInitialSyncData(FriendlyByteBuf buf) {
        super.receiveInitialSyncData(buf);
        readState(buf);
        scheduleChunkForRenderUpdate();
    }

    protected void writeState(FriendlyByteBuf buffer) {
        buffer.writeBoolean(isFormed());
        if (isFormed()) {
            LongSet disabled = state.getMatchContext().getOrDefault("renderMask", LongSets.EMPTY_SET);
            buffer.writeVarInt(disabled.size());
            for (long blockPos : disabled) {
                buffer.writeLong(blockPos);
            }
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos pos : state.getCache()) {
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());

                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }
            buffer.writeBlockPos(new BlockPos(minX, minY, minZ));
            buffer.writeBlockPos(new BlockPos(maxX + 1, maxY + 1, maxZ + 1));

            var tag = new CompoundTag();

            // ser capabilities
            var list = saveCapabilities();
            if (list != null && state != null) {
                tag.put("capabilities", list);
                //ser slotsMap
                Map<Long, Set<String>> slotsMap = state.getMatchContext().get("slots");
                if (slotsMap != null) {
                    var slotList = new ListTag();
                    slotsMap.forEach((l, set) -> {
                        var slot = new CompoundTag();
                        ListTag names = new ListTag();
                        set.forEach(n -> names.add(StringTag.valueOf(n)));
                        slot.put("names", names);
                        slot.putLong("pos", l);
                        slotList.add(slot);
                    });
                    tag.put("slotList", slotList);
                }
            }
            buffer.writeNbt(tag);
        }
    }

    protected void readState(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            state = new MultiblockState(level, worldPosition);
            state.setError(null);
            int size = buffer.readVarInt();
            if (size > 0) {
                ImmutableList.Builder<BlockPos> listBuilder = new ImmutableList.Builder<>();
                for (int i = size; i > 0; i--) {
                    listBuilder.add(BlockPos.of(buffer.readLong()));
                }
                MultiblockWorldSavedData.addDisableModel(state.controllerPos, listBuilder.build());
            }
            renderBox = new AABB(buffer.readBlockPos(), buffer.readBlockPos());
            var tag = buffer.readNbt();
            if (tag != null && tag.contains("capabilities")) {
                var tagList = tag.getList("capabilities", Tag.TAG_COMPOUND);
                loadCapabilities(tagList);

                Map<Long, Set<String>> slotsMap = new HashMap<>();
                var slotList = tag.getList("slotList", Tag.TAG_COMPOUND);
                for (Tag t : slotList) {
                    var slot = (CompoundTag)t;
                    var pos = slot.getLong("pos");
                    ListTag names = slot.getList("names", Tag.TAG_STRING);
                    for (Tag name : names) {
                        slotsMap.computeIfAbsent(pos, p -> new HashSet<>()).add(name.getAsString());
                    }
                }

                if (!settings.isEmpty()) {
                    capabilities = Tables.newCustomTable(new EnumMap<>(IO.class), Object2ObjectOpenHashMap::new);
                    for (Map.Entry<Long, Map<MultiblockCapability<?>, Tuple<IO, Direction>>> entry : settings.entrySet()) {
                        BlockPos pos = BlockPos.of(entry.getKey());
                        var tileEntity = level.getBlockEntity(pos);
                        if (tileEntity != null) {
                            for (Map.Entry<MultiblockCapability<?>, Tuple<IO, Direction>> tupleEntry : entry.getValue().entrySet()) {
                                var capability = tupleEntry.getKey();
                                var io = tupleEntry.getValue().getA();
                                var facing = tupleEntry.getValue().getB();

                                if (capability.isBlockHasCapability(io, tileEntity)) {
                                    if (!capabilities.contains(io, capability)) {
                                        capabilities.put(io, capability, new Long2ObjectOpenHashMap<>());
                                    }
                                    CapabilityProxy<?> proxy = capability.createProxy(io, tileEntity, facing, slotsMap);
                                    capabilities.get(io, capability).put(entry.getKey().longValue(), proxy);
                                }
                            }
                        }
                    }
                }
                settings = null;
            }
        } else {
            if (state != null) {
                MultiblockWorldSavedData.removeDisableModel(state.controllerPos);
            }
            settings = null;
            state = null;
            renderBox = null;
            capabilities = null;
        }
    }

    @Override
    public void setLevel(@Nonnull Level world) {
        super.setLevel(world);
        state = MultiblockWorldSavedData.getOrCreate(level).mapping.get(worldPosition);
    }

    @Override
    public void load(@Nonnull CompoundTag compound) {
        try {
            super.load(compound);
        } catch (Exception e) {
            if (definition == null) {
                MultiblockWorldSavedData mwsd = MultiblockWorldSavedData.getOrCreate(level);
                if (worldPosition != null && mwsd.mapping.containsKey(worldPosition)) {
                    mwsd.removeMapping(mwsd.mapping.get(worldPosition));
                }
                return;
            }
        }
        if (compound.contains("recipeLogic")) {
            recipeLogic = createRecipeLogic();
            recipeLogic.readFromNBT(compound.getCompound("recipeLogic"));
            status = recipeLogic.getStatus().name;
        }
        if (compound.contains("capabilities")) {
            ListTag tagList = compound.getList("capabilities", Tag.TAG_COMPOUND);
            loadCapabilities(tagList);
        }
        if (getDefinition().noNeedController && compound.contains("oldState")) {
            this.oldState = NbtUtils.readBlockState(compound.getCompound("oldState"));
            if (compound.contains("oldNbt")) {
                this.oldNbt = compound.getCompound("oldNbt");
            }
        }
    }

    @Nullable
    private ListTag saveCapabilities() {
        if (capabilities != null) {
            ListTag tagList = new ListTag();
            for (Table.Cell<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> cell : capabilities.cellSet()) {
                IO io = cell.getRowKey();
                MultiblockCapability<?> cap = cell.getColumnKey();
                Long2ObjectOpenHashMap<CapabilityProxy<?>> value = cell.getValue();
                if (io != null && cap != null && value != null) {
                    for (Map.Entry<Long, CapabilityProxy<?>> entry : value.entrySet()) {
                        CompoundTag tag = new CompoundTag();
                        tag.putInt("io", io.ordinal());
                        tag.putInt("facing", entry.getValue().facing.ordinal());
                        tag.putString("cap", cap.name);
                        tag.putLong("pos", entry.getKey());
                        tagList.add(tag);
                    }
                }
            }
            return tagList;
        }
        return null;
    }

    private void loadCapabilities(ListTag tagList) {
        settings = new HashMap<>();
        for (Tag base : tagList) {
            CompoundTag nbt = (CompoundTag) base;
            settings.computeIfAbsent(nbt.getLong("pos"), l->new HashMap<>())
                    .put(MbdCapabilities.get(nbt.getString("cap")), new Tuple<>(IO.VALUES[nbt.getInt("io")], Direction.values()[nbt.getInt("facing")]));
        }
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag compound) {
        super.saveAdditional(compound);
        if (recipeLogic != null) compound.put("recipeLogic", recipeLogic.writeToNBT(new CompoundTag()));
        var list = saveCapabilities();
        if (list != null) {
            compound.put("capabilities", list);
        }
        if (getDefinition().noNeedController && oldState != null) {
            compound.put("oldState", NbtUtils.writeBlockState(oldState));
            if (oldNbt != null) {
                compound.put("oldNbt", oldNbt);
            }
        }
    }

    @Override
    public InteractionResult use(Player player, InteractionHand hand, BlockHitResult hit) {
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            RightClickEvent event = new RightClickEvent(this, player, hand, hit);
            if (event.post(ScriptType.of(level), RightClickEvent.ID, getSubID())) {
                return InteractionResult.SUCCESS;
            }
        }

        if (isRemote() && !this.isFormed() && player.isCrouching() && player.getItemInHand(hand).isEmpty()) {
            MultiblockPreviewRenderer.renderMultiBlockPreview(this, 60000);
            return InteractionResult.SUCCESS;
        }

        if (!isRemote()) {
            if (!isFormed() && definition.getCatalyst() != null) {
                if (state == null) state = new MultiblockState(level, getBlockPos());
                ItemStack held = player.getItemInHand(hand);
                if (definition.getCatalyst().isEmpty() || ItemStack.isSameIgnoreDurability(held, definition.getCatalyst())) {
                    if (checkCatalystPattern(player, hand, held)) {
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            if (!player.isCrouching()) {
                if (!isRemote() && player instanceof ServerPlayer) {
                    BlockEntityUIFactory.INSTANCE.openUI(this, (ServerPlayer) player);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ModularUI createComponentUI(Player entityPlayer) {
        TabContainer tabContainer = new TabContainer(0, 0, 200, 232);
        if (!traits.isEmpty()) initTraitUI(tabContainer, entityPlayer);
        if (isFormed()) {
            new RecipePage(this, tabContainer);
            new IOPageWidget(this, tabContainer);
        } else {
            new StructurePageWidget(this.definition, tabContainer);
        }
        return new ModularUI(196, 256, this, entityPlayer).widget(tabContainer);
    }

    @Override
    public void handleMbdUI(ModularUI modularUI) {
        var capabilitiesProxy = getCapabilitiesProxy();
        if (capabilitiesProxy != null) {
            for (Long2ObjectOpenHashMap<CapabilityProxy<?>> map : capabilitiesProxy.values()) {
                for (CapabilityProxy<?> proxy : map.values()) {
                    proxy.handleProxyMbdUI(modularUI);
                }
            }
        }
        super.handleMbdUI(modularUI);
    }

    @NotNull
    @Override
    public AABB getRenderBoundingBox() {
        return getRenderer().isGlobalRenderer(this) ? INFINITE_EXTENT_AABB : renderBox == null ? super.getRenderBoundingBox() : renderBox;
    }

    @Override
    public void asyncThreadLogic(long periodID) {
        if (!isFormed() && getDefinition().getCatalyst() == null && (getOffset() + periodID) % 4 == 0) {
            BlockPattern pattern = getPattern();
            if (pattern != null && pattern.checkPatternAt(new MultiblockState(level, worldPosition), false)) {
                ServerLifecycleHooks.getCurrentServer().execute(() -> {
                    if (state == null) state = new MultiblockState(level, worldPosition);
                    if (checkPattern()) { // formed
                        MultiblockWorldSavedData.getOrCreate(level).addMapping(state);
                        onStructureFormed();
                    }
                });
            }
        }
        try {
            if (hasProxies()) {
                // should i add locks for proxies?
                for (Long2ObjectOpenHashMap<CapabilityProxy<?>> map : getCapabilitiesProxy().values()) {
                    if (map != null) {
                        for (CapabilityProxy<?> proxy : map.values()) {
                            if (proxy != null) {
                                proxy.updateChangedState(periodID);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Multiblocked.LOGGER.error("something run while checking proxy changes");
        }
    }

    public void saveOldBlock(BlockState oldState, CompoundTag oldNbt) {
        this.oldState = oldState;
        if (oldNbt != null) {
            this.oldNbt = oldNbt;
        }
        markAsDirty();
    }
}
