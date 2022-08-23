package com.lowdragmc.multiblocked.common.capability.trait;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.trait.MultiCapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.GuiUtils;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.common.capability.ItemMultiblockCapability;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ItemCapabilityTrait extends MultiCapabilityTrait {
    private ItemStackHandler handler;
    private ItemStack[][] validItems;
    public ItemCapabilityTrait() {
        super(ItemMultiblockCapability.CAP);
    }

    @Override
    public void serialize(@Nullable JsonElement jsonElement) {
        super.serialize(jsonElement);
        if (jsonElement == null) {
            jsonElement = new JsonArray();
        }
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        int size = jsonArray.size();
        validItems = new ItemStack[size][];
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            if (jsonObject.has("valid")) {
                validItems[i] = new ItemStack[0];
                for (JsonElement item : jsonObject.get("valid").getAsJsonArray()) {
                    validItems[i] = ArrayUtils.add(validItems[i], Multiblocked.GSON.fromJson(item.getAsString(), ItemStack.class));
                }
            }
        }
        handler = new ItemStackHandler(size) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                if (validItems[slot] != null) {
                    for (ItemStack itemStack : validItems[slot]) {
                        if (ItemStack.isSame(stack, itemStack) && ItemStack.tagMatches(stack, itemStack)) {
                            return true;
                        }
                    }
                    return false;
                }
                return super.isItemValid(slot, stack);
            }
        };
    }

    @Override
    public JsonElement deserialize() {
        JsonArray jsonArray = super.deserialize().getAsJsonArray();
        for (int i = 0; i < capabilityIO.length; i++) {
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            if (validItems[i] != null) {
                JsonArray items = new JsonArray();
                for (ItemStack itemStack : validItems[i]) {
                    items.add(Multiblocked.GSON.toJson(itemStack));
                }
                jsonObject.add("valid", items);
            }
        }
        return jsonArray;
    }

    @Override
    public void onDrops(NonNullList<ItemStack> drops, PlayerEntity player) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                drops.add(handler.getStackInSlot(i));
            }
        }
    }

    @Override
    public void readFromNBT(CompoundNBT compound) {
        super.readFromNBT(compound);
        ListNBT tagList = compound.getCompound("_").getList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundNBT itemTags = tagList.getCompound(i);
            int slot = itemTags.getInt("Slot");
            if (slot >= 0 && slot < handler.getSlots()) {
                handler.setStackInSlot(slot, ItemStack.of(itemTags));
            }
        }
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
            for (int i = 0; i < handler.getSlots(); i++) {
                group.addWidget(new SlotWidget(new ProxyItemHandler(handler, guiIO, false), i, x[i], y[i], true, true));
            }
        }
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
                    ItemStack already = this.handler.getStackInSlot(i);
                    int need = this.handler.getSlotLimit(i) - already.getCount();
                    if (need > 0) {
                        for (Direction facing : getIOFacing()) {
                            TileEntity te = component.getLevel().getBlockEntity(component.getBlockPos().relative(facing));
                            if (te != null) {
                                IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite()).orElse(null);
                                if (handler != null) {
                                    for (int j = 0; j < handler.getSlots(); j++) {
                                        ItemStack extracted = handler.extractItem(j, need, true);
                                        if (extracted.isEmpty()) continue;
                                        if (already.isEmpty() || ItemStack.matches(extracted, already)) {
                                            this.handler.insertItem(i, handler.extractItem(j, need, false).copy(), false);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (capabilityIO[i] == IO.OUT){
                    ItemStack already = this.handler.getStackInSlot(i);
                    if (!already.isEmpty()) {
                        for (Direction facing : getIOFacing()) {
                            TileEntity te = component.getLevel().getBlockEntity(component.getBlockPos().relative(facing));
                            if (te != null) {
                                IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite()).orElse(null);
                                if (handler != null) {
                                    for (int j = 0; j < handler.getSlots(); j++) {
                                        ItemStack left = handler.insertItem(j, already.copy(), false);
                                        if (left.getCount() == already.getCount()) continue;
                                        this.handler.extractItem(i, already.getCount() - left.getCount(), false);
                                        return;
                                    }
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
    protected void addSlot() {
        super.addSlot();
        validItems = ArrayUtils.add(validItems, null);
    }

    @Override
    protected void removeSlot(int index) {
        super.removeSlot(index);
        validItems = ArrayUtils.remove(validItems, index);
    }

    @Override
    protected void initSettingDialog(DialogWidget dialog, DraggableWidgetGroup slot, final int index) {
        super.initSettingDialog(dialog, slot, index);
        WidgetGroup widget = new WidgetGroup(5, 73, 200, 200);
        dialog.addWidget(widget);
        dialog.addWidget(GuiUtils.createBoolSwitch(5, 60, "Item Filter", "", validItems[index] != null, result->{
            if (result) {
                validItems[index] = new ItemStack[0];
                widget.addWidget(GuiUtils.createItemStackSelector(0,0, "Valid Items", Arrays.stream(validItems[index]).collect(Collectors.toList()), list -> validItems[index] = list.toArray(new ItemStack[0])));
            } else {
                widget.clearAllWidgets();
                validItems[index] = null;
            }
        }));
        if (validItems[index] != null) {
            widget.addWidget(GuiUtils.createItemStackSelector(0,0, "Valid Items", Arrays.stream(validItems[index]).collect(Collectors.toList()), list -> validItems[index] = list.toArray(new ItemStack[0])));
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.orEmpty(capability, LazyOptional.of(() -> new ProxyItemHandler(handler, capabilityIO, false)));
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getInnerCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.orEmpty(capability, LazyOptional.of(() -> new ProxyItemHandler(handler, capabilityIO, true)));
    }

    private class ProxyItemHandler implements IItemHandler, IItemHandlerModifiable {
        public ItemStackHandler proxy;
        public IO[] ios;
        public boolean inner;

        public ProxyItemHandler(ItemStackHandler proxy, IO[] ios, boolean inner) {
            this.proxy = proxy;
            this.ios = ios;
            this.inner = inner;
        }

        @Override
        public int getSlots() {
            return proxy.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return proxy.getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            IO io = ios[slot];
            if (io == IO.BOTH || (inner ? io == IO.OUT : io == IO.IN)) {
                if (!simulate) markAsDirty();
                return proxy.insertItem(slot, stack, simulate);
            }
            return stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IO io = ios[slot];
            if (io == IO.BOTH || (inner ? io == IO.IN : io == IO.OUT)) {
                if (!simulate) markAsDirty();
                return proxy.extractItem(slot, amount, simulate);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return proxy.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return proxy.isItemValid(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
            proxy.setStackInSlot(slot, stack);
        }
    }

}
