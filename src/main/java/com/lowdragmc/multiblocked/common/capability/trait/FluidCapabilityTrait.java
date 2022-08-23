package com.lowdragmc.multiblocked.common.capability.trait;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.trait.MultiCapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.GuiUtils;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.common.capability.FluidMultiblockCapability;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.JSONUtils;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class FluidCapabilityTrait extends MultiCapabilityTrait {
    private FluidTankList handler;
    private int[] tankCapability;

    private FluidStack[][] validFluids;
    public FluidCapabilityTrait() {
        super(FluidMultiblockCapability.CAP);
    }

    @Override
    public void serialize(@Nullable JsonElement jsonElement) {
        super.serialize(jsonElement);
        if (jsonElement == null) {
            jsonElement = new JsonArray();
        }
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        int size = jsonArray.size();
        tankCapability = new int[size];
        validFluids =  new FluidStack[size][];
        int i = 0;
        for (JsonElement element : jsonArray) {
            JsonObject jsonObject = element.getAsJsonObject();
            tankCapability[i] = JSONUtils.getAsInt(jsonObject, "tC", 1000);
            if (jsonObject.has("valid")) {
                validFluids[i] = new FluidStack[0];
                for (JsonElement fluid : jsonObject.get("valid").getAsJsonArray()) {
                    validFluids[i] = ArrayUtils.add(validFluids[i], Multiblocked.GSON.fromJson(fluid.getAsString(), FluidStack.class));
                }
            }
            i++;
        }
        FluidTank[] fluidTanks = Arrays.stream(tankCapability).mapToObj(FluidTank::new).toArray(FluidTank[]::new);
        for (int j = 0; j < fluidTanks.length; j++) {
            if (validFluids[j] != null) {
                final FluidStack[] fluids = validFluids[j];
                fluidTanks[j].setValidator(stack -> {
                    for (FluidStack fluidStack : fluids) {
                        if (fluidStack.isFluidEqual(stack)) return true;
                    }
                    return false;
                });
            }
        }
        handler = new FluidTankList(capabilityIO, fluidTanks);
    }

    @Override
    public JsonElement deserialize() {
        JsonArray jsonArray = super.deserialize().getAsJsonArray();
        for (int i = 0; i < capabilityIO.length; i++) {
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            jsonObject.addProperty("tC", tankCapability[i]);
            if (validFluids[i] != null) {
                JsonArray fluids = new JsonArray();
                for (FluidStack fluid : validFluids[i]) {
                    fluids.add(Multiblocked.GSON.toJson(fluid));
                }
                jsonObject.add("valid", fluids);
            }
        }
        return jsonArray;
    }

    @Override
    public boolean hasUpdate() {
        return ArrayUtils.contains(autoIO, true);
    }

    @Override
    public void update() {
        for (int i = 0; i < autoIO.length; i++) {
            if (autoIO[i]) {
                if (capabilityIO[i] == IO.IN) {
                    FluidTank already = this.handler.getTankAt(i);
                    FluidStack fluidStack = already.getFluid();
                    int need = already.getCapacity() - already.getFluidAmount();
                    if (need > 0) {
                        for (Direction facing : getIOFacing()) {
                            TileEntity te = component.getLevel().getBlockEntity(component.getBlockPos().relative(facing));
                            if (te != null) {
                                AtomicBoolean r = new AtomicBoolean(false);
                                te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite()).ifPresent(handler -> {
                                    if (already.fill(handler.drain(new FluidStack(fluidStack.getFluid(), need), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE) > 0) {
                                        r.set(true);
                                    }
                                });
                                if (r.get()) {
                                    return;
                                }
                            }
                        }
                    }
                } else if (capabilityIO[i] == IO.OUT){
                    FluidTank already = this.handler.getTankAt(i);
                    FluidStack fluidStack = already.getFluid();
                    if (already.getFluidAmount() > 0) {
                        for (Direction facing : getIOFacing()) {
                            TileEntity te = component.getLevel().getBlockEntity(component.getBlockPos().relative(facing));
                            if (te != null) {
                                AtomicBoolean r = new AtomicBoolean(false);
                                te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite()).ifPresent(handler -> {
                                    if (!already.drain(handler.fill(fluidStack.copy(), IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE).isEmpty()) {
                                        r.set(true);
                                    } 
                                });
                                if (r.get()) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
        super.update();
    }

    @Override
    protected void initSettingDialog(DialogWidget dialog, DraggableWidgetGroup slot, final int index) {
        super.initSettingDialog(dialog, slot, index);
        dialog.addWidget(new LabelWidget(5, 60, "multiblocked.gui.label.tank_capability"));
        dialog.addWidget(new TextFieldWidget(5, 70, 100, 15, null, s -> tankCapability[index] = Integer.parseInt(s))
                .setNumbersOnly(1, Integer.MAX_VALUE)
                .setCurrentString(tankCapability[index] + ""));
        WidgetGroup widget = new WidgetGroup(5, 103, 200, 200);
        dialog.addWidget(widget);
        dialog.addWidget(GuiUtils.createBoolSwitch(5, 90, "Fluid Filter", "", validFluids[index] != null, result->{
            if (result) {
                validFluids[index] = new FluidStack[0];
                widget.addWidget(GuiUtils.createFluidStackSelector(0,0, "Valid Fluids", Arrays.stream(validFluids[index]).collect(Collectors.toList()), list -> validFluids[index] = list.toArray(new FluidStack[0])));
            } else {
                widget.clearAllWidgets();
                validFluids[index] = null;
            }
        }));
        if (validFluids[index] != null) {
            widget.addWidget(GuiUtils.createFluidStackSelector(0,0, "Valid Fluids", Arrays.stream(validFluids[index]).collect(Collectors.toList()), list -> validFluids[index] = list.toArray(new FluidStack[0])));
        }
    }

    @Override
    protected void addSlot() {
        super.addSlot();
        tankCapability = ArrayUtils.add(tankCapability, 1000);
        validFluids = ArrayUtils.add(validFluids, null);
    }

    @Override
    protected void removeSlot(int index) {
        super.removeSlot(index);
        tankCapability = ArrayUtils.remove(tankCapability, index);
        validFluids = ArrayUtils.remove(validFluids, index);
    }

    @Override
    public void readFromNBT(CompoundNBT compound) {
        super.readFromNBT(compound);
        handler.deserializeNBT(compound.getCompound("_"));
    }

    @Override
    public void writeToNBT(CompoundNBT compound) {
        super.writeToNBT(compound);
        compound.put("_", handler.serializeNBT());
    }

    @Override
    public void createUI(ComponentTileEntity<?> component, WidgetGroup group, PlayerEntity player) {
        super.createUI(component, group, player);
        if (handler != null) {
            for (int i = 0; i < guiIO.length; i++) {
                group.addWidget(new TankWidget(new ProxyFluidHandler(handler.getTankAt(i), guiIO[i]), x[i], y[i], true, true));
            }
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.orEmpty(capability, LazyOptional.of(() -> handler));
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getInnerCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.orEmpty(capability, LazyOptional.of(() -> handler.inner()));
    }

    private class ProxyFluidHandler implements IFluidTank, IFluidHandler {
        public FluidTank proxy;
        public IO io;

        public ProxyFluidHandler(FluidTank proxy, IO io) {
            this.proxy = proxy;
            this.io = io;
        }

        @Nonnull
        @Override
        public FluidStack getFluid() {
            return proxy.getFluid();
        }

        @Override
        public int getFluidAmount() {
            return proxy.getFluidAmount();
        }

        @Override
        public int getCapacity() {
            return proxy.getCapacity();
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return true;
        }

        @Override
        public int getTanks() {
            return proxy.getTanks();
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return proxy.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return proxy.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return proxy.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (io == IO.OUT) {
                return 0;
            }
            markAsDirty();
            return proxy.fill(resource, action);
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction doDrain) {
            return proxy.drain(resource, doDrain);
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction doDrain) {
            if (io == IO.IN) {
                return FluidStack.EMPTY;
            }
            markAsDirty();
            return proxy.drain(maxDrain, doDrain);
        }
    }

    public class FluidTankList implements IFluidHandler, INBTSerializable<CompoundNBT> {
        public IO[] cIOs;
        protected final List<FluidTank> fluidTanks;
        private boolean inner;

        public FluidTankList(IO[] cIOs, FluidTank... fluidTanks) {
            this.fluidTanks = Arrays.asList(fluidTanks);
            this.cIOs = cIOs;
        }

        private FluidTankList(IO[] cIOs, final List<FluidTank> fluidTanks, boolean inner) {
            this.cIOs = cIOs;
            this.fluidTanks = fluidTanks;
            this.inner = inner;
        }

        public FluidTankList inner(){
            return new FluidTankList(cIOs, fluidTanks, true);
        }

        public int getTanks() {
            return fluidTanks.size();
        }

        public FluidTank getTankAt(int index) {
            return fluidTanks.get(index);
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return fluidTanks.get(tank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return fluidTanks.get(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return fluidTanks.get(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction doFill) {
            if (resource == null || resource.getAmount() <= 0) {
                return 0;
            }
            if (doFill.execute()) markAsDirty();
            return fillTanksImpl(resource.copy(), doFill);
        }

        private int fillTanksImpl(FluidStack resource, FluidAction doFill) {
            int totalFilled = 0;
            for (int i = 0; i < fluidTanks.size(); i++) {
                IO io = cIOs[i];
                if ((inner ? io == IO.IN : io == IO.OUT)) {
                    continue;
                }
                FluidTank handler = fluidTanks.get(i);
                if (resource.isFluidEqual(handler.getFluid())) {
                    int filledAmount = handler.fill(resource, doFill);
                    totalFilled += filledAmount;
                    resource.setAmount(resource.getAmount() - filledAmount);
                    if (resource.getAmount() == 0)
                        return totalFilled;
                }
            }
            for (int i = 0; i < fluidTanks.size(); i++) {
                IO io = cIOs[i];
                if ((inner ? io == IO.IN : io == IO.OUT)) {
                    continue;
                }
                FluidTank handler = fluidTanks.get(i);
                if (handler.getFluidAmount() == 0) {
                    int filledAmount = handler.fill(resource, doFill);
                    totalFilled += filledAmount;
                    resource.setAmount(resource.getAmount() - filledAmount);
                    if (resource.getAmount() == 0)
                        return totalFilled;
                }
            }
            return totalFilled;
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction doDrain) {
            if (resource == null || resource.getAmount() <= 0) {
                return FluidStack.EMPTY;
            }
            if (doDrain.execute()) markAsDirty();
            resource = resource.copy();
            FluidStack totalDrained = null;
            for (int i = 0; i < fluidTanks.size(); i++) {
                IO io = cIOs[i];
                if ((inner ? io == IO.OUT : io == IO.IN)) {
                    continue;
                }
                FluidTank handler = fluidTanks.get(i);
                if (!resource.isFluidEqual(handler.getFluid())) {
                    continue;
                }
                FluidStack drain = handler.drain(resource.getAmount(), doDrain);
                if (drain.isEmpty()) {
                    continue;
                }
                if (totalDrained == null) {
                    totalDrained = drain;
                } else totalDrained.setAmount(totalDrained.getAmount() + drain.getAmount());
                resource.setAmount(resource.getAmount() - drain.getAmount());
                if (resource.getAmount() == 0) break;
            }
            return totalDrained == null ? FluidStack.EMPTY : totalDrained;
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction doDrain) {
            if (maxDrain == 0) {
                return FluidStack.EMPTY;
            }
            if (doDrain.execute()) markAsDirty();
            FluidStack totalDrained = null;
            for (int i = 0; i < fluidTanks.size(); i++) {
                IO io = cIOs[i];
                if ((inner ? io == IO.OUT : io == IO.IN)) {
                    continue;
                }
                FluidTank handler = fluidTanks.get(i);
                if (totalDrained == null) {
                    totalDrained = handler.drain(maxDrain, doDrain);
                    if (!totalDrained.isEmpty())
                        maxDrain -= totalDrained.getAmount();
                } else {
                    FluidStack copy = totalDrained.copy();
                    copy.setAmount(maxDrain);
                    if (!copy.isFluidEqual(handler.getFluid())) continue;
                    FluidStack drain = handler.drain(copy.getAmount(), doDrain);
                    if (!drain.isEmpty()) {
                        totalDrained.setAmount(totalDrained.getAmount() + drain.getAmount());
                        maxDrain -= drain.getAmount();
                    }
                }
                if (maxDrain <= 0) break;
            }
            return totalDrained == null ? FluidStack.EMPTY : totalDrained;
        }

        @Override
        public CompoundNBT serializeNBT() {
            CompoundNBT fluidInventory = new CompoundNBT();
            fluidInventory.putInt("TankAmount", this.getTanks());

            ListNBT tanks = new ListNBT();
            for (int i = 0; i < this.getTanks(); i++) {
                CompoundNBT writeTag;
                FluidTank fluidTank = fluidTanks.get(i);
                if (fluidTank != null) {
                    writeTag = fluidTank.writeToNBT(new CompoundNBT());
                } else writeTag = new CompoundNBT();

                tanks.add(writeTag);
            }
            fluidInventory.put("Tanks", tanks);
            return fluidInventory;
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            ListNBT tanks = nbt.getList("Tanks", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < Math.min(fluidTanks.size(), nbt.getInt("TankAmount")); i++) {
                INBT nbtTag = tanks.get(i);
                FluidTank fluidTank = fluidTanks.get(i);
                if (fluidTank != null) {
                    fluidTank.readFromNBT((CompoundNBT) nbtTag);
                }
            }
        }

    }

}
