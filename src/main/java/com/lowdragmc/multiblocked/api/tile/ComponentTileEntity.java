package com.lowdragmc.multiblocked.api.tile;

import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib.LDLMod;
import com.lowdragmc.lowdraglib.gui.factory.TileEntityUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.TabButton;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IInnerCapabilityProvider;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.kubejs.events.*;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import dev.latvian.kubejs.script.ScriptType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A TileEntity that defies everything a TileEntity represents.
 *
 * This isn't going to be in-world.
 */
public abstract class ComponentTileEntity<T extends ComponentDefinition> extends TileEntity implements IInnerCapabilityProvider, IUIHolder, IComponent {
    // is good to write down all CT code here? or move them to @ZenExpansion.
    protected T definition;

    protected IMultiblockedRenderer currentRenderer;

    public Object rendererObject; // used for renderer

    public INBT persistedData; // used for renderer

    private UUID owner;

    private final int offset = Multiblocked.RNG.nextInt();

    private int timer = offset;

    protected String status = "unformed";

    protected Map<MultiblockCapability<?>, CapabilityTrait> traits = new HashMap<>();

    public ComponentTileEntity(T definition) {
        super(definition.getTileType());
        this.definition = definition;
        initTrait();
    }

    protected void initTrait() {
        for (Map.Entry<String, JsonElement> entry : this.definition.traits.entrySet()) {
            MultiblockCapability<?> capability = MbdCapabilities.get(entry.getKey());
            if (capability != null && capability.hasTrait()) {
                CapabilityTrait trait = capability.createTrait();
                trait.serialize(entry.getValue());
                trait.setComponent(this);
                traits.put(capability, trait);
            }
        }
    }

    public boolean needAlwaysUpdate() {
        return level != null && !isRemote() && (definition.needUpdateTick() || traits.values().stream().anyMatch(CapabilityTrait::hasUpdate));
    }

    public boolean hasTrait(MultiblockCapability<?> capability) {
        return traits.get(capability) != null;
    }

    public CapabilityTrait getTrait(MultiblockCapability<?> capability) {
        return traits.get(capability);
    }

    public T getDefinition() {
        return definition;
    }

    public ResourceLocation getLocation() {
        return definition.location;
    }
    
    public String getUnlocalizedName() {
        return "block." + definition.getID();
    }
    
    public String getLocalizedName() {
        return I18n.get(getUnlocalizedName());
    }

    public String getSubID() {
        return definition.getID();
    }

    public abstract boolean isFormed();

    public int getOffset() {
        return offset;
    }

    public int getTimer() {
        return timer;
    }

    public UUID getOwnerUUID() {
        return owner;
    }

    @Nullable
    public PlayerEntity getOwner() {
        return (owner == null || this.level == null) ? null : this.level.getPlayerByUUID(owner);
    }

    public void setOwner(UUID player) {
        this.owner = player;
    }

    public void setOwner(PlayerEntity player) {
        this.owner = player.getUUID();
    }

    public void update(){
        timer++;
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            new UpdateTickEvent(this).post(ScriptType.of(level), UpdateTickEvent.ID, getSubID());
        }
        if (!traits.isEmpty()) {
            for (CapabilityTrait trait : traits.values()) {
                trait.update();
            }
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (!isRemote()) {
            if (Multiblocked.isKubeJSLoaded() && level != null) {
                StatusChangedEvent event = new StatusChangedEvent(this, status);
                if (event.post(ScriptType.of(level), StatusChangedEvent.ID, getSubID())) {
                    return;
                }
                status = event.getStatus();
            }
            if (!this.status.equals(status)) {
                this.status = status;
                writeCustomData(1, buffer->buffer.writeUtf(this.status));
            }
        }
    }

//    public List<AxisAlignedBB> getCollisionBoundingBox() {
//        return definition.getAABB(isFormed(), frontFacing);
//    }

    @Override
    public void mirror(@Nonnull Mirror mirrorIn) {
        rotateTo(mirrorIn.getRotation(getFrontFacing()));
    }

    public IMultiblockedRenderer updateCurrentRenderer() {
        IMultiblockedRenderer renderer;
        if (isFormed()) {
            renderer = definition.formedRenderer == null ? definition.baseRenderer : definition.formedRenderer;
        } else {
            renderer = definition.baseRenderer;
        }
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            UpdateRendererEvent event = new UpdateRendererEvent(this, renderer);
            event.post(ScriptType.of(level), UpdateRendererEvent.ID, getSubID());
            renderer = event.getRenderer();
        }
        return renderer;
    }

    public IMultiblockedRenderer getRenderer() {
        IMultiblockedRenderer lastRenderer = currentRenderer;
        currentRenderer = updateCurrentRenderer();
        if (lastRenderer != currentRenderer) {
            if (lastRenderer != null) {
                lastRenderer.onPostAccess(this);
            }
            if (currentRenderer != null) {
                currentRenderer.onPreAccess(this);
            }
        }
        return currentRenderer;
    }

//    public boolean canConnectRedstone(Direction facing) {
//        return definition.getOutputRedstoneSignal != null;
//    }

    public int getOutputRedstoneSignal(Direction facing) {
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            OutputRedstoneEvent event = new OutputRedstoneEvent(this, facing);
            event.post(ScriptType.of(level), OutputRedstoneEvent.ID, getSubID());
            return event.redstone;
        }
        return 0;
    }

    public void scheduleChunkForRenderUpdate() {
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Constants.BlockFlags.RERENDER_MAIN_THREAD);
        }
    }

    public void notifyBlockUpdate() {
        if (level != null) {
            level.updateNeighborsAt(getBlockPos(), level.getBlockState(getBlockPos()).getBlock());
        }
    }

    public void markAsDirty() {
        super.setChanged();
    }

    //************* events *************//

    public void onDrops(NonNullList<ItemStack> drops, PlayerEntity player) {
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            if (new DropEvent(this, drops, player).post(ScriptType.of(level), DropEvent.ID, getSubID())) {
                return;
            }
        }
        for (CapabilityTrait trait : traits.values()) {
            trait.onDrops(drops, player);
        }
        drops.add(definition.getStackForm());
    }

    public ActionResultType use(PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            RightClickEvent event = new RightClickEvent(this, player, hand, hit);
            if (event.post(ScriptType.of(level), RightClickEvent.ID, getSubID())) {
                return ActionResultType.SUCCESS;
            }
        }
        if (!player.isCrouching() && hand == Hand.MAIN_HAND) {
            if (player instanceof ServerPlayerEntity) {
                return TileEntityUIFactory.INSTANCE.openUI(this, (ServerPlayerEntity) player) ? ActionResultType.SUCCESS : ActionResultType.PASS;
            } else {
                return traits.isEmpty() ? ActionResultType.PASS : ActionResultType.SUCCESS;
            }
        }
        return ActionResultType.PASS;
    }
    
    public void onNeighborChange() {
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            new NeighborChangedEvent(this).post(ScriptType.of(level), NeighborChangedEvent.ID, getSubID());
        }
    }
    //************* capability *************//


    @Override
    @Nonnull
    public <K> LazyOptional<K> getCapability(@Nonnull Capability<K> capability, @Nullable Direction facing) {
        for (CapabilityTrait trait : traits.values()) {
            LazyOptional<K> result = trait.getCapability(capability, facing);
            if (result.isPresent()) {
                return result;
            }
        }
        return super.getCapability(capability, facing);
    }

    @Nullable
    @Override
    public  <K> LazyOptional<K> getInnerCapability(@Nonnull Capability<K> capability, @Nullable Direction facing) {
        for (CapabilityTrait trait : traits.values()) {
            LazyOptional<K> result = trait.getInnerCapability(capability, facing);
            if (result.isPresent()) {
                return result;
            }
        }
        return getCapability(capability, facing);
    }

    //************* gui *************//

    public final boolean isRemote() {
        return level == null ? LDLMod.isRemote() : level.isClientSide;
    }

    @Override
    public ModularUI createUI(PlayerEntity entityPlayer) {
        ModularUI modularUI = createComponentUI(entityPlayer);
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            CreateUIEvent event = new CreateUIEvent(this, modularUI);
            if (event.post(ScriptType.of(level), CreateUIEvent.ID, getSubID())) {
                return null;
            }
            modularUI = event.getModularUI();
        }
        return modularUI;
    }

    public ModularUI createComponentUI(PlayerEntity PlayerEntity) {
        if (traits.isEmpty()) return null;
        TabContainer tabContainer = new TabContainer(0, 0, 200, 232);
        initTraitUI(tabContainer, PlayerEntity);
        return new ModularUI(196, 256,this, PlayerEntity).widget(tabContainer);
    }

    @Override
    public boolean isInvalid() {
        return isRemoved();
    }

    protected void initTraitUI(TabContainer tabContainer, PlayerEntity PlayerEntity) {
        WidgetGroup group = new WidgetGroup(20, 0, 176, 256);
        tabContainer.addTab(new TabButton(0, tabContainer.containerGroup.widgets.size() * 20, 20, 20)
                        .setTexture(new ResourceTexture("multiblocked:textures/gui/custom_gui_tab_button.png").getSubTexture(0, 0, 1, 0.5),
                                new ResourceTexture("multiblocked:textures/gui/custom_gui_tab_button.png").getSubTexture(0, 0.5, 1, 0.5)), group);
        group.addWidget(new ImageWidget(0, 0, 176, 256, new ResourceTexture(JSONUtils.getAsString(definition.traits, "background", "multiblocked:textures/gui/custom_gui.png"))));
        if (traits.size() > 0 ) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    group.addWidget(new SlotWidget(PlayerEntity.inventory, col + (row + 1) * 9, 7 + col * 18, 173 + row * 18).setLocationInfo(true, false));
                }
            }
            for (int slot = 0; slot < 9; slot++) {
                group.addWidget(new SlotWidget(PlayerEntity.inventory, slot, 7 + slot * 18, 231).setLocationInfo(true, true));
            }
        }
        for (CapabilityTrait trait : traits.values()) {
            trait.createUI(this, group, PlayerEntity);
        }
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            new InitTraitUIEvent(this, group).post(ScriptType.of(level), InitTraitUIEvent.ID, getSubID());
        }
    }

    public final void writeTraitData(CapabilityTrait trait, int internalId, Consumer<PacketBuffer> dataWriter) {
        this.writeCustomData(3, (buffer) -> {
            buffer.writeUtf(trait.capability.name);
            buffer.writeVarInt(internalId);
            dataWriter.accept(buffer);
        });
    }

    //************* data sync *************//

    private static class UpdateEntry {
        private final int discriminator;
        private final byte[] updateData;

        public UpdateEntry(int discriminator, byte[] updateData) {
            this.discriminator = discriminator;
            this.updateData = updateData;
        }
    }

    protected final List<UpdateEntry> updateEntries = new ArrayList<>();

    public void writeInitialSyncData(PacketBuffer buf) {
        buf.writeUtf(status);
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            new WriteInitialDataEvent(this, buf).post(ScriptType.of(level), WriteInitialDataEvent.ID, getSubID());
        }
    }

    public void receiveInitialSyncData(PacketBuffer buf) {
        status = buf.readUtf();
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            new ReadInitialDataEvent(this, buf).post(ScriptType.of(level), ReadInitialDataEvent.ID, getSubID());
        }
    }

    public void receiveCustomData(int dataId, PacketBuffer buf) {
        if (dataId == 1) {
            status = buf.readUtf();
            scheduleChunkForRenderUpdate();
        } else if (dataId == 3) {
            MultiblockCapability<?> capability = MbdCapabilities.get(buf.readUtf());
            if (traits.containsKey(capability)) {
                traits.get(capability).receiveCustomData(buf.readVarInt(), buf);
            }
        } else if (Multiblocked.isKubeJSLoaded() && level != null) {
            new ReceiveCustomDataEvent(this, dataId, buf).post(ScriptType.of(level), ReceiveCustomDataEvent.ID, getSubID());
        }
    }

    
    @Override
    public final void deserializeNBT(@Nonnull CompoundNBT nbt) {
        super.deserializeNBT(nbt);
    }

    @Override
    public final CompoundNBT serializeNBT() {
        return super.serializeNBT();
    }

    @Override
    public void setLevelAndPosition(@Nonnull World world, @Nonnull BlockPos pos) {
        super.setLevelAndPosition(world, pos);
        if (needAlwaysUpdate()) {
            MultiblockWorldSavedData.getOrCreate(level).addLoading(this);
        }
    }

    @Override
    public void load(@Nonnull BlockState blockState, @Nonnull CompoundNBT compound) {
        super.load(blockState, compound);
        if (needAlwaysUpdate()) {
            MultiblockWorldSavedData.getOrCreate(level).addLoading(this);
        }
        if (compound.contains("owner")) {
            this.owner = compound.getUUID("owner");
        }
        CompoundNBT traitTag = compound.getCompound("trait");
        for (Map.Entry<MultiblockCapability<?>, CapabilityTrait> entry : traits.entrySet()) {
            entry.getValue().readFromNBT(traitTag.getCompound(entry.getKey().name));
        }
        if (compound.contains("persisted")) {
            persistedData = compound.get("persisted");
        }
    }

    @Nonnull
    @Override
    public CompoundNBT save(@Nonnull CompoundNBT compound) {
        super.save(compound);
        compound.putString("loc", definition.location.toString());
        if (this.owner != null) {
            compound.putUUID("owner", this.owner);
        }
        compound.putString("mbd_def", definition.location.toString());
        CompoundNBT traitTag = new CompoundNBT();
        for (Map.Entry<MultiblockCapability<?>, CapabilityTrait> entry : traits.entrySet()) {
            CompoundNBT tag = new CompoundNBT();
            entry.getValue().writeToNBT(tag);
            traitTag.put(entry.getKey().name, tag);
        }
        compound.put("trait", traitTag);
        if (persistedData != null) {
            compound.put("persisted", persistedData);
        }
        return compound;
    }

    public void writeCustomData(int discriminator, Consumer<PacketBuffer> dataWriter) {
        ByteBuf backedBuffer = Unpooled.buffer();
        dataWriter.accept(new PacketBuffer(backedBuffer));
        byte[] updateData = Arrays.copyOfRange(backedBuffer.array(), 0, backedBuffer.writerIndex());
        updateEntries.add(new UpdateEntry(discriminator, updateData));
        BlockState state = getBlockState();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
        }
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT updateTag = new CompoundNBT();
        ListNBT tagList = new ListNBT();
        for (UpdateEntry updateEntry : updateEntries) {
            CompoundNBT entryTag = new CompoundNBT();
            entryTag.putInt("i", updateEntry.discriminator);
            entryTag.putByteArray("d", updateEntry.updateData);
            tagList.add(entryTag);
        }
        this.updateEntries.clear();
        updateTag.put("d", tagList);
        return new SUpdateTileEntityPacket(getBlockPos(), 0, updateTag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        CompoundNBT updateTag = pkt.getTag();
        ListNBT tagList = updateTag.getList("d", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundNBT entryTag = tagList.getCompound(i);
            int discriminator = entryTag.getInt("i");
            byte[] updateData = entryTag.getByteArray("d");
            ByteBuf backedBuffer = Unpooled.copiedBuffer(updateData);
            receiveCustomData(discriminator, new PacketBuffer(backedBuffer));
        }
    }

    @Nonnull
    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT updateTag = super.getUpdateTag();
        ByteBuf backedBuffer = Unpooled.buffer();
        writeInitialSyncData(new PacketBuffer(backedBuffer));
        byte[] updateData = Arrays.copyOfRange(backedBuffer.array(), 0, backedBuffer.writerIndex());
        updateTag.putByteArray("d", updateData);
        return updateTag;
    }

    @Override
    public void handleUpdateTag(BlockState state, @Nonnull CompoundNBT tag) {
        super.handleUpdateTag(state, tag);
        byte[] updateData = tag.getByteArray("d");
        ByteBuf backedBuffer = Unpooled.copiedBuffer(updateData);
        receiveInitialSyncData(new PacketBuffer(backedBuffer));
    }

    @Override
    @Nonnull
    public AxisAlignedBB getRenderBoundingBox() {
        return getRenderer().isGlobalRenderer(this) ? INFINITE_EXTENT_AABB : super.getRenderBoundingBox();
    }

    @Override
    public VoxelShape getDynamicShape() {
        VoxelShape shape = IComponent.super.getDynamicShape();
        if (Multiblocked.isKubeJSLoaded() && level != null) {
            CustomShapeEvent event= new CustomShapeEvent(this, shape);
            event.post(ScriptType.of(level), CustomShapeEvent.ID, getSubID());
            shape = event.getShape();
        }
        return shape;
    }

    @Override
    public void setRendererObject(Object o) {
        rendererObject = o;
    }

    @Override
    public Object getRendererObject() {
        return rendererObject;
    }
}
