package com.lowdragmc.multiblocked.common.capability.trait;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.multiblocked.api.block.CustomProperties;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.trait.ProgressCapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.FEMultiblockCapability;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.JSONUtils;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FECapabilityTrait extends ProgressCapabilityTrait {
    private EnergyStorage handler;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public FECapabilityTrait() {
        super(FEMultiblockCapability.CAP);
    }

    @Override
    public void serialize(@Nullable JsonElement jsonElement) {
        super.serialize(jsonElement);
        if (jsonElement == null) {
            jsonElement = new JsonObject();
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        capacity = JSONUtils.getAsInt(jsonObject, "capacity", 10000);
        maxReceive = JSONUtils.getAsInt(jsonObject, "maxReceive", 500);
        maxExtract = JSONUtils.getAsInt(jsonObject, "maxExtract", 500);
        handler = new EnergyStorage(capacity, maxReceive, maxExtract);
    }

    @Override
    public JsonElement deserialize() {
        JsonObject jsonObject = super.deserialize().getAsJsonObject();
        jsonObject.addProperty("capacity", capacity);
        jsonObject.addProperty("maxReceive", maxReceive);
        jsonObject.addProperty("maxExtract", maxExtract);
        return jsonObject;
    }

    @Override
    protected String dynamicHoverTips(double progress) {
        return LocalizationUtils.format("multiblocked.gui.trait.fe.progress", (int)(handler.getMaxEnergyStored() * progress), handler.getMaxEnergyStored());
    }

    @Override
    protected double getProgress() {
        return handler.getEnergyStored() * 1f / handler.getMaxEnergyStored();
    }

    @Override
    public void readFromNBT(CompoundNBT compound) {
        super.readFromNBT(compound);
        if (compound.contains("_")) {
            handler = new EnergyStorage(capacity, maxReceive, maxExtract, Math.min(compound.getInt("_"), capacity));
        }
    }

    @Override
    public void writeToNBT(CompoundNBT compound) {
        super.writeToNBT(compound);
        compound.putInt("_", handler.getEnergyStored());
    }

    @Override
    protected void initSettingDialog(DialogWidget dialog, DraggableWidgetGroup slot) {
        super.initSettingDialog(dialog, slot);
        dialog.addWidget(new TextFieldWidget(60, 5, 100, 15, null, s -> capacity = Integer.parseInt(s))
                .setNumbersOnly(1, Integer.MAX_VALUE)
                .setCurrentString(capacity + "")
                .setHoverTooltips("multiblocked.gui.trait.fe.tips.0"));

        dialog.addWidget(new TextFieldWidget(60, 25, 100, 15, null, s -> maxReceive = Integer.parseInt(s))
                .setNumbersOnly(1, Integer.MAX_VALUE)
                .setCurrentString(maxReceive + "")
                .setHoverTooltips("multiblocked.gui.trait.fe.tips.1"));

        dialog.addWidget(new TextFieldWidget(60, 45, 100, 15, null, s -> maxExtract = Integer.parseInt(s))
                .setNumbersOnly(1, Integer.MAX_VALUE)
                .setCurrentString(maxExtract + "")
                .setHoverTooltips("multiblocked.gui.trait.fe.tips.2"));
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        return CapabilityEnergy.ENERGY.orEmpty(capability, LazyOptional.of(() -> new ProxyEnergyStorage(handler, capabilityIO)));
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getInnerCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        return CapabilityEnergy.ENERGY.orEmpty(capability, LazyOptional.of(() -> new ProxyEnergyStorage(handler, guiIO)));
    }

    @Override
    public boolean hasUpdate() {
        return capabilityIO == IO.OUT;
    }

    @Override
    public void update() {
        int left = Math.min(maxExtract, handler.getEnergyStored());
        if (left > 0) {
            for (Direction facing : getIOFacing()) {
                TileEntity te = component.getLevel().getBlockEntity(component.getBlockPos().relative(facing));
                if (te != null) {
                    IEnergyStorage handler = te.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite()).orElse(null);
                    if (handler != null) {
                        int accepted = handler.receiveEnergy(left, false);
                        left -= this.handler.extractEnergy(accepted, false);
                        if (left <= 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public Direction[] getIOFacing() {
        if (component.getDefinition().properties.rotationState != CustomProperties.RotationState.NONE) {
            return new Direction[]{component.getFrontFacing()};
        }
        return Direction.values();
    }

    private class ProxyEnergyStorage implements IEnergyStorage {
        public EnergyStorage proxy;
        public IO io;

        public ProxyEnergyStorage(EnergyStorage proxy, IO io) {
            this.proxy = proxy;
            this.io = io;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (io == IO.BOTH || io == IO.IN) {
                if (!simulate) markAsDirty();
                return proxy.receiveEnergy(maxReceive, simulate);
            }
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (io == IO.BOTH || io == IO.OUT) {
                if (!simulate) markAsDirty();
                return proxy.extractEnergy(maxExtract, simulate);
            }
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return proxy.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return proxy.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            if (io == IO.BOTH || io == IO.OUT) {
                return proxy.canExtract();
            }
            return false;
        }

        @Override
        public boolean canReceive() {
            if (io == IO.BOTH || io == IO.IN) {
                return proxy.canReceive();
            }
            return false;
        }
    }

}
