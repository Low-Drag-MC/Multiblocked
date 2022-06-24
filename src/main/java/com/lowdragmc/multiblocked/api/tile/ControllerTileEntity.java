package com.lowdragmc.multiblocked.api.tile;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.lowdragmc.lowdraglib.gui.factory.TileEntityUIFactory;
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
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.lowdragmc.multiblocked.client.renderer.MultiblockPreviewRenderer;
import com.lowdragmc.multiblocked.persistence.IAsyncThreadUpdate;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import dev.latvian.kubejs.script.ScriptType;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.minecraft.util.Util.NIL_UUID;

/**
 * A TileEntity that defies all controller machines.
 *
 * Head of the multiblock.
 */
public class ControllerTileEntity extends ComponentTileEntity<ControllerDefinition> implements ICapabilityProxyHolder, IAsyncThreadUpdate, IControllerComponent {
    public MultiblockState state;
    public boolean asyncRecipeSearching = true;
    protected Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> capabilities;
    private Map<Long, Map<MultiblockCapability<?>, Tuple<IO, Direction>>> settings;
    protected LongOpenHashSet parts;
    protected RecipeLogic recipeLogic;
    protected AxisAlignedBB renderBox;

    public ControllerTileEntity(ControllerDefinition definition) {
        super(definition);
    }

    @Nullable
    public BlockPattern getPattern() {
        if (Multiblocked.isKubeJSLoaded()) {
            DynamicPatternEvent event = new DynamicPatternEvent(this, definition.basePattern);
            if (event.post(ScriptType.SERVER, DynamicPatternEvent.ID, getSubID())) {
                return null;
            }
            return event.pattern;
        }
        return definition.basePattern;
    }
    public RecipeLogic getRecipeLogic() {
        return recipeLogic;
    }

    public boolean checkPattern() {
        if (state == null) return false;
        BlockPattern pattern = getPattern();
        return pattern != null && pattern.checkPatternAt(state, false);
    }

    @Override
    public boolean isValidFrontFacing(Direction facing) {
        return definition.allowRotate && facing.getAxis() != Direction.Axis.Y;
    }

    public boolean isFormed() {
        return state != null && state.isFormed();
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
        if (Multiblocked.isKubeJSLoaded()) {
            new UpdateFormedEvent(this).post(ScriptType.SERVER, UpdateFormedEvent.ID, getSubID());
        }
    }

    @Override
    public IMultiblockedRenderer updateCurrentRenderer() {
        IMultiblockedRenderer renderer;
        if (definition.workingRenderer != null && isFormed() && (status.equals("working") || status.equals("suspend"))) {
            renderer = definition.workingRenderer;
            if (Multiblocked.isKubeJSLoaded()) {
                UpdateRendererEvent event = new UpdateRendererEvent(this, renderer);
                event.post(ScriptType.SERVER, UpdateRendererEvent.ID, getSubID());
                renderer = event.getRenderer();
            }
        } else {
            renderer = super.updateCurrentRenderer();
        }
        return renderer;
    }

    public Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> getCapabilitiesProxy() {
        return capabilities;
    }

    /**
     * Called when its formed, server side only.
     */
    public void onStructureFormed() {
        if (recipeLogic == null) {
            recipeLogic = new RecipeLogic(this);
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
                TileEntity tileEntity = level.getBlockEntity(BlockPos.of(entry.getKey()));
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
                                    CapabilityProxy<?> proxy = capability.createProxy(io, tileEntity);
                                    proxy.facing = facing;
                                    proxy.slots = slotsMap == null ? null : slotsMap.get(tileEntity.getBlockPos().asLong());
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
                                    CapabilityProxy<?> proxy = capability.createProxy(io, tileEntity);
                                    proxy.slots = slotsMap == null ? null : slotsMap.get(tileEntity.getBlockPos().asLong());
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
                TileEntity tileEntity = level.getBlockEntity(BlockPos.of(pos));
                if (tileEntity instanceof IPartComponent) {
                    ((IPartComponent) tileEntity).addedToController(this);
                }
            }
        }

        writeCustomData(-1, this::writeState);
        if (Multiblocked.isKubeJSLoaded()) {
            new StructureFormedEvent(this).post(ScriptType.SERVER, StructureFormedEvent.ID, getSubID());
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
                TileEntity tileEntity = level.getBlockEntity(BlockPos.of(pos));
                if (tileEntity instanceof IPartComponent) {
                    ((IPartComponent) tileEntity).removedFromController(this);
                }
            }
            parts = null;
        }
        capabilities = null;

        writeCustomData(-1, this::writeState);
        if (Multiblocked.isKubeJSLoaded()) {
            new StructureInvalidEvent(this).post(ScriptType.SERVER, StructureInvalidEvent.ID, getSubID());
        }
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        if (dataId == -1) {
            readState(buf);
            scheduleChunkForRenderUpdate();
        } else {
            super.receiveCustomData(dataId, buf);
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        writeState(buf);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        readState(buf);
        scheduleChunkForRenderUpdate();
    }

    protected void writeState(PacketBuffer buffer) {
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
        }
    }

    protected void readState(PacketBuffer buffer) {
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
            renderBox = new AxisAlignedBB(buffer.readBlockPos(), buffer.readBlockPos());
        } else {
            if (state != null) {
                MultiblockWorldSavedData.removeDisableModel(state.controllerPos);
            }
            state = null;
            renderBox = null;
        }
    }

    @Override
    public void setLevelAndPosition(@Nonnull World world, @Nonnull BlockPos pos) {
        super.setLevelAndPosition(world, pos);
        state = MultiblockWorldSavedData.getOrCreate(level).mapping.get(worldPosition);
    }

    @Override
    public void load(@Nonnull BlockState blockState, @Nonnull CompoundNBT compound) {
        try {
            super.load(blockState, compound);
        } catch (Exception e) {
            if (definition == null) {
                MultiblockWorldSavedData mwsd = MultiblockWorldSavedData.getOrCreate(level);
                if (worldPosition != null && mwsd.mapping.containsKey(worldPosition)) {
                    mwsd.removeMapping(mwsd.mapping.get(worldPosition));
                }
                return;
            }
        }
        if (compound.contains("ars")) {
            asyncRecipeSearching = compound.getBoolean("ars");
        }
        if (compound.contains("recipeLogic")) {
            recipeLogic = new RecipeLogic(this);
            recipeLogic.readFromNBT(compound.getCompound("recipeLogic"));
            status = recipeLogic.getStatus().name;
        }
        if (compound.contains("capabilities")) {
            ListNBT tagList = compound.getList("capabilities", Constants.NBT.TAG_COMPOUND);
            settings = new HashMap<>();
            for (INBT base : tagList) {
                CompoundNBT tag = (CompoundNBT) base;
                settings.computeIfAbsent(tag.getLong("pos"), l->new HashMap<>())
                        .put(MbdCapabilities.get(tag.getString("cap")), new Tuple<>(IO.VALUES[tag.getInt("io")], Direction.values()[tag.getInt("facing")]));
            }
        }
    }

    @Nonnull
    @Override
    public CompoundNBT save(@Nonnull CompoundNBT compound) {
        super.save(compound);
        if (!asyncRecipeSearching) {
            compound.putBoolean("ars", false);
        }
        if (recipeLogic != null) compound.put("recipeLogic", recipeLogic.writeToNBT(new CompoundNBT()));
        if (capabilities != null) {
            ListNBT tagList = new ListNBT();
            for (Table.Cell<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> cell : capabilities.cellSet()) {
                IO io = cell.getRowKey();
                MultiblockCapability<?> cap = cell.getColumnKey();
                Long2ObjectOpenHashMap<CapabilityProxy<?>> value = cell.getValue();
                if (io != null && cap != null && value != null) {
                    for (Map.Entry<Long, CapabilityProxy<?>> entry : value.entrySet()) {
                        CompoundNBT tag = new CompoundNBT();
                        tag.putInt("io", io.ordinal());
                        tag.putInt("facing", entry.getValue().facing.ordinal());
                        tag.putString("cap", cap.name);
                        tag.putLong("pos", entry.getKey());
                        tagList.add(tag);
                    }
                }
            }
            compound.put("capabilities", tagList);
        }
        return compound;
    }

    @Override
    public ActionResultType use(PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (Multiblocked.isKubeJSLoaded()) {
            RightClickEvent event = new RightClickEvent(this, player, hand, hit);
            if (event.post(ScriptType.SERVER, RightClickEvent.ID, getSubID())) {
                return ActionResultType.SUCCESS;
            }
        }

        if (isRemote() && !this.isFormed() && player.isCrouching() && player.getItemInHand(hand).isEmpty()) {
            MultiblockPreviewRenderer.renderMultiBlockPreview(this, 60000);
            return ActionResultType.SUCCESS;
        }

        if (!isRemote()) {
            if (!isFormed() && definition.catalyst != null) {
                if (state == null) state = new MultiblockState(level, getBlockPos());
                ItemStack held = player.getItemInHand(hand);
                if (definition.catalyst.isEmpty() || ItemStack.isSame(held, definition.catalyst)) {
                    if (checkPattern()) { // formed
                        player.swing(hand);
                        ITextComponent formedMsg = new TranslationTextComponent(getUnlocalizedName()).append(new TranslationTextComponent("multiblocked.multiblock.formed"));
                        player.sendMessage(formedMsg, NIL_UUID);
                        if (!player.isCreative() && !definition.catalyst.isEmpty()) {
                            held.shrink(1);
                        }
                        MultiblockWorldSavedData.getOrCreate(level).addMapping(state);
                        if (!needAlwaysUpdate()) {
                            MultiblockWorldSavedData.getOrCreate(level).addLoading(this);
                        }
                        onStructureFormed();
                        return ActionResultType.SUCCESS;
                    }
                }
            }
            if (!player.isCrouching()) {
                if (!isRemote() && player instanceof ServerPlayerEntity) {
                    TileEntityUIFactory.INSTANCE.openUI(this, (ServerPlayerEntity) player);
                }
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public ModularUI createUI(PlayerEntity entityPlayer) {
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

    @Nonnull
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getRenderer().isGlobalRenderer(this) ? INFINITE_EXTENT_AABB : renderBox == null ? super.getRenderBoundingBox() : renderBox;
    }

    @Override
    public void asyncThreadLogic(long periodID) {
        if (!isFormed() && getDefinition().catalyst == null && (getOffset() + periodID) % 4 == 0) {
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
}
