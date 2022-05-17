package com.lowdragmc.multiblocked.api.tile;

import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib.LDLMod;
import com.lowdragmc.lowdraglib.gui.factory.TileEntityUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.TabButton;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IInnerCapabilityProvider;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
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
public abstract class ComponentTileEntity<T extends ComponentDefinition> extends TileEntity implements IInnerCapabilityProvider, IUIHolder {
    // is good to write down all CT code here? or move them to @ZenExpansion.
    protected T definition;

    protected IMultiblockedRenderer currentRenderer;

    public Object rendererObject; // used for renderer

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
        return getLocation().getPath() + ".name";
    }
    
    public String getLocalizedName() {
        return I18n.get(getUnlocalizedName());
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
//        if (definition.updateTick != null) {
//        }
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
            if (!this.status.equals(status)) {
//                if (definition.statusChanged != null) {
//                }
                this.status = status;
                writeCustomData(1, buffer->buffer.writeUtf(this.status));
            }
        }
    }

//    public List<AxisAlignedBB> getCollisionBoundingBox() {
//        return definition.getAABB(isFormed(), frontFacing);
//    }

    public Direction getFrontFacing() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    public void setFrontFacing(Direction facing) {
        if (level != null && !level.isClientSide) {
            if (!isValidFrontFacing(facing)) return;
            if (getBlockState().getValue(BlockStateProperties.FACING) == facing) return;
            level.setBlock(getBlockPos(), getBlockState().setValue(BlockStateProperties.FACING, facing), 3);
        }
    }

    @Override
    public void rotate(@Nonnull Rotation rotationIn) {
        setFrontFacing(rotationIn.rotate(getFrontFacing()));
    }

    @Override
    public void mirror(@Nonnull Mirror mirrorIn) {
        rotate(mirrorIn.getRotation(getFrontFacing()));
    }

    public IMultiblockedRenderer updateCurrentRenderer() {
//        if (definition.dynamicRenderer != null) {
//        }
        if (isFormed()) {
            return definition.formedRenderer == null ? definition.baseRenderer : definition.formedRenderer;
        }
        return definition.baseRenderer;
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

    public boolean isValidFrontFacing(Direction facing) {
        return definition.allowRotate;
    }

//    public boolean canConnectRedstone(Direction facing) {
//        return definition.getOutputRedstoneSignal != null;
//    }

    public int getOutputRedstoneSignal(Direction facing) {
//        if (definition.getOutputRedstoneSignal != null) {
//
//        }
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

    //************* TESR *************//

    public boolean hasTESRRenderer() {
        IMultiblockedRenderer renderer = getRenderer();
        return renderer != null && renderer.hasTESR(this);
    }

    //************* events *************//

    public void onDrops(NonNullList<ItemStack> drops, PlayerEntity player) {
//        if (definition.onDrops != null) {
//        }
        for (CapabilityTrait trait : traits.values()) {
            trait.onDrops(drops, player);
        }
        drops.add(definition.getStackForm());
    }

    public ActionResultType use(PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
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
//        if (definition.onNeighborChanged != null) {
//     
//        }
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

    public boolean canConnectRedstone(Direction direction) {
        return false;
    }

    @Override
    public ModularUI createUI(PlayerEntity PlayerEntity) {
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
        for (CapabilityTrait trait : traits.values()) {
            trait.createUI(this, group, PlayerEntity);
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
//        if (definition.writeInitialData != null) { // ct
//
//        } else {
//            buf.writeBoolean(false);
//        }
    }

    public void receiveInitialSyncData(PacketBuffer buf) {
        status = buf.readUtf();
//        if (buf.readBoolean()) { // ct
//            CompoundNBT nbt = buf.readNbt();
//        }
    }

    public void receiveCustomData(int dataId, PacketBuffer buf) {
        if (dataId == 1) {
            status = buf.readUtf();
            scheduleChunkForRenderUpdate();
        } else if (dataId == 2) {
//            int id = buf.readVarInt();
//            try {
//                CompoundNBT nbt = buf.readNbt();
//                if (nbt != null && definition.receiveCustomData != null) {
//                }
//            } catch (IOException e) {
//                Multiblocked.LOGGER.error("handling ct custom data error id:{}", id);
//            }
        } else if (dataId == 3) {
            MultiblockCapability<?> capability = MbdCapabilities.get(buf.readUtf());
            if (traits.containsKey(capability)) {
                traits.get(capability).receiveCustomData(buf.readVarInt(), buf);
            }
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
}
