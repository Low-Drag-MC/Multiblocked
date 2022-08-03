package com.lowdragmc.multiblocked.api.capability.trait;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.capability.IInnerCapabilityProvider;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public abstract class CapabilityTrait implements IInnerCapabilityProvider {
    public final MultiblockCapability<?> capability;
    protected ComponentTileEntity<?> component;

    protected CapabilityTrait(MultiblockCapability<?> capability) {
        this.capability = capability;
    }

    public void serialize(@Nullable JsonElement jsonElement){

    }

    public JsonElement deserialize(){
        return new JsonObject();
    }

    public void setComponent(ComponentTileEntity<?> component) {
        this.component = component;
    }

    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable Direction facing) {
        return getCapability(capability, facing).isPresent();
    }
    
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        return LazyOptional.empty();
    }

    public boolean hasUpdate() {
        return false;
    }
    
    public void update() {
        
    }

    public void markAsDirty() {
        if (component != null) {
            component.markAsDirty();
        }
    }

    public void readFromNBT(CompoundNBT compound) {
    }

    public void writeToNBT(CompoundNBT compound) {
    }

    public void receiveCustomData(int id, PacketBuffer buffer) {
    }

    public boolean receiveClientEvent(int id, int type) {
        return false;
    }

    public void invalidate() {}

    public void onLoad() {}

    public void onChunkUnload() {}

    public final void writeCustomData(int id, Consumer<PacketBuffer> writer) {
        this.component.writeTraitData(this, id, writer);
    }

    public void createUI(ComponentTileEntity<?> component, WidgetGroup group, PlayerEntity player) {

    }

    public void openConfigurator(WidgetGroup dialog) {
        
    }

    public void onDrops(NonNullList<ItemStack> drops, PlayerEntity player) {

    }
}
