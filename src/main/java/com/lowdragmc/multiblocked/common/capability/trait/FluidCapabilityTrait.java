package com.lowdragmc.multiblocked.common.capability.trait;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.JsonUtil;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.trait.MultiCapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.GuiUtils;
import com.lowdragmc.multiblocked.api.gui.dialogs.ResourceTextureWidget;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.common.capability.FluidMultiblockCapability;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
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
    private static final String EMPTY_TEX = "multiblocked:textures/void.png";
    private FluidTankList handler;
    private int[] tankCapability;
    private FluidStack[][] validFluids;
    protected int[] width;
    protected int[] height;
    protected String[] texture;
    protected ProgressTexture.FillDirection[] fillDirection;

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
        width = new int[size];
        height = new int[size];
        texture = new String[size];
        fillDirection = new ProgressTexture.FillDirection[size];
        validFluids =  new FluidStack[size][];
        int i = 0;
        for (JsonElement element : jsonArray) {
            JsonObject jsonObject = element.getAsJsonObject();
            tankCapability[i] = GsonHelper.getAsInt(jsonObject, "tC", 1000);
            width[i] = GsonHelper.getAsInt(jsonObject, "w", 18);
            height[i] = GsonHelper.getAsInt(jsonObject, "h", 18);
            texture[i] = GsonHelper.getAsString(jsonObject, "tex", EMPTY_TEX);
            fillDirection[i] = JsonUtil.getEnumOr(jsonObject, "fillDir", ProgressTexture.FillDirection.class, ProgressTexture.FillDirection.ALWAYS_FULL);
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
        handler = new FluidTankList(capabilityIO, Arrays.asList(fluidTanks), this.slotName, null);
    }

    @Override
    public JsonElement deserialize() {
        JsonArray jsonArray = super.deserialize().getAsJsonArray();
        for (int i = 0; i < capabilityIO.length; i++) {
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            jsonObject.addProperty("tC", tankCapability[i]);
            jsonObject.addProperty("w", width[i]);
            jsonObject.addProperty("h", height[i]);
            if (!texture[i].equals(EMPTY_TEX)) jsonObject.addProperty("tex", texture[i]);
            jsonObject.addProperty("fillDir", fillDirection[i].name());
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
                            BlockEntity te = component.getLevel().getBlockEntity(component.getBlockPos().relative(facing));
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
                            BlockEntity te = component.getLevel().getBlockEntity(component.getBlockPos().relative(facing));
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
    protected void refreshSlots(DraggableScrollableWidgetGroup dragGroup) {
        dragGroup.widgets.forEach(dragGroup::waitToRemoved);
        for (int i = 0; i < guiIO.length; i++) {
            int finalI = i;
            ButtonWidget setting = (ButtonWidget) new ButtonWidget(width[finalI] - 8, 0, 8, 8, new ResourceTexture("multiblocked:textures/gui/option.png"), null).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.settings");
            ImageWidget imageWidget = new ImageWidget(0, 0,  width[finalI], height[finalI], new GuiTextureGroup(createAutoProgressTexture(finalI), new ColorBorderTexture(1, getColorByIO(capabilityIO[finalI]))));
            setting.setVisible(false);
            DraggableWidgetGroup slot = new DraggableWidgetGroup(x[finalI], y[finalI], width[finalI], height[finalI]);
            slot.setOnSelected(w -> setting.setVisible(true));
            slot.setOnUnSelected(w -> setting.setVisible(false));
            slot.addWidget(imageWidget);
            slot.addWidget(setting);
            slot.setOnEndDrag(b -> {
                x[finalI] = b.getSelfPosition().x;
                y[finalI] = b.getSelfPosition().y;
            });
            dragGroup.addWidget(slot);

            setting.setOnPressCallback(cd2 -> {
                DialogWidget dialog = new DialogWidget(dragGroup, true);
                dialog.addWidget(new ImageWidget(0, 0, 176, 256, new ColorRectTexture(0xaf000000)));
                dialog.addWidget(new ButtonWidget(5, 5, 85, 20, new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("multiblocked.gui.trait.remove_slot")), cd3 -> {
                    removeSlot(finalI);
                    refreshSlots(dragGroup);
                    dialog.close();
                }).setHoverBorderTexture(1, -1));
                initSettingDialog(dialog, slot, finalI);
            });
        }
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
                widget.addWidget(GuiUtils.createFluidStackSelector(0,0, "Valid Fluids", Arrays.stream(validFluids[index]).toList(), list -> validFluids[index] = list.toArray(FluidStack[]::new)));
            } else {
                widget.clearAllWidgets();
                validFluids[index] = null;
            }
        }));
        if (validFluids[index] != null) {
            widget.addWidget(GuiUtils.createFluidStackSelector(0,0, "Valid Fluids", Arrays.stream(validFluids[index]).toList(), list -> validFluids[index] = list.toArray(FluidStack[]::new)));
        }

        // progress bar
        WidgetGroup group = new WidgetGroup(0, 180, 50, 50);
        dialog.addWidget(group);
        ImageWidget imageWidget = (ImageWidget) slot.widgets.get(0);
        ButtonWidget setting = (ButtonWidget) slot.widgets.get(1);
        ButtonWidget imageSelector = (ButtonWidget) new ButtonWidget(60, 45, width[index] , height[index] , new GuiTextureGroup(new ColorBorderTexture(1, -1), createAutoProgressTexture(index)), null)
                .setHoverTooltips("multiblocked.gui.tips.select_image");
        group.addWidget(new TextFieldWidget(5, 25, 50, 15, null, s -> {
            width[index] = Integer.parseInt(s);
            Size size = new Size(width[index], height[index]);
            slot.setSize(size);
            imageWidget.setSize(size);
            imageSelector.setSize(size);
            setting.setSelfPosition(new Position(width[index] - 8, 0));
        }).setCurrentString(width[index] + "").setNumbersOnly(1, 180).setHoverTooltips("multiblocked.gui.trait.set_width"));
        group.addWidget(new TextFieldWidget(5, 45, 50, 15, null, s -> {
            height[index]  = Integer.parseInt(s);
            Size size = new Size(width[index], height[index]);
            slot.setSize(size);
            imageWidget.setSize(size);
            imageSelector.setSize(size);
            setting.setSelfPosition(new Position(width[index] - 8, 0));
        }).setCurrentString(height[index] + "").setNumbersOnly(1, 180).setHoverTooltips("multiblocked.gui.trait.set_height"));

        group.addWidget(imageSelector);
        group.addWidget(new SelectorWidget(60, 25, 90, 15, Arrays.stream(ProgressTexture.FillDirection.values()).map(Enum::name).collect(Collectors.toList()), -1)
                .setIsUp(true)
                .setValue(fillDirection[index].name())
                .setOnChanged(io -> {
                    fillDirection[index] = ProgressTexture.FillDirection.valueOf(io);
                    ResourceTexture autoProgressTexture = createAutoProgressTexture(index);
                    imageSelector.setButtonTexture(new GuiTextureGroup(new ColorBorderTexture(1, -1), autoProgressTexture));
                    imageWidget.setImage(new GuiTextureGroup(autoProgressTexture, new ColorBorderTexture(1, getColorByIO(capabilityIO[index]))));
                })
                .setButtonBackground(ResourceBorderTexture.BUTTON_COMMON)
                .setBackground(new ColorRectTexture(0xffaaaaaa))
                .setHoverTooltips("multiblocked.gui.trait.fill_direction"));
        imageSelector.setOnPressCallback(cd -> new ResourceTextureWidget(dialog.getParent().getGui().mainGroup, texture1 -> {
            if (texture1 != null) {
                texture[index] = texture1.imageLocation.toString();
                ResourceTexture autoProgressTexture = createAutoProgressTexture(index);
                imageSelector.setButtonTexture(new GuiTextureGroup(new ColorBorderTexture(1, -1), autoProgressTexture));
                imageWidget.setImage(new GuiTextureGroup(autoProgressTexture, new ColorBorderTexture(1, getColorByIO(capabilityIO[index]))));
            }
        }));
    }

    @Override
    protected void updateImageWidget(ImageWidget imageWidget, int index) {
        imageWidget.setImage(new GuiTextureGroup(createAutoProgressTexture(index), new ColorBorderTexture(1, getColorByIO(capabilityIO[index]))));
    }

    private ResourceTexture createAutoProgressTexture(int index) {
        return new ResourceTexture(this.texture[index]);
    }

    @Override
    protected void addSlot() {
        super.addSlot();
        tankCapability = ArrayUtils.add(tankCapability, 1000);
        validFluids = ArrayUtils.add(validFluids, null);
        width = ArrayUtils.add(width, 18);
        height = ArrayUtils.add(height, 18);
        texture = ArrayUtils.add(texture, EMPTY_TEX);
        fillDirection = ArrayUtils.add(fillDirection, ProgressTexture.FillDirection.ALWAYS_FULL);
    }

    @Override
    protected void removeSlot(int index) {
        super.removeSlot(index);
        tankCapability = ArrayUtils.remove(tankCapability, index);
        validFluids = ArrayUtils.remove(validFluids, index);
        width = ArrayUtils.remove(width, index);
        height = ArrayUtils.remove(height, index);
        texture = ArrayUtils.remove(texture, index);
        fillDirection = ArrayUtils.remove(fillDirection, index);
    }

    @Override
    public void readFromNBT(CompoundTag compound) {
        super.readFromNBT(compound);
        handler.deserializeNBT(compound.getCompound("_"));
    }

    @Override
    public void writeToNBT(CompoundTag compound) {
        super.writeToNBT(compound);
        compound.put("_", handler.serializeNBT());
    }

    @Override
    public void handleMbdUI(ModularUI modularUI) {
        for (int i = 0; i < slotName.length; i++) {
            String name = slotName[i];
            if (name != null && !name.isEmpty()) {
                for (Widget widget : modularUI.getWidgetsById("^%s$".formatted(name))) {
                    if (widget instanceof TankWidget tankWidget) {
                        tankWidget.setFluidTank(new ProxyFluidHandler(handler.getTankAt(i), guiIO[i]));
                    }
                }
            }
        }
    }

    @Override
    public void createUI(ComponentTileEntity<?> component, WidgetGroup group, Player player) {
        super.createUI(component, group, player);
        if (handler != null) {
            for (int i = 0; i < guiIO.length; i++) {
                group.addWidget(new TankWidget(new ProxyFluidHandler(handler.getTankAt(i), guiIO[i]), x[i], y[i], width[i], height[i], true, true).setOverlay(new ResourceTexture(texture[i])).setFillDirection(fillDirection[i]).setShowAmount(fillDirection[i] == ProgressTexture.FillDirection.ALWAYS_FULL));
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
    public <T> LazyOptional<T> getInnerCapability(@Nonnull Capability<T> capability, @Nullable Direction facing, @Nullable String slotName) {
        return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.orEmpty(capability, LazyOptional.of(() -> new FluidTankList(getRealMbdIO(), handler.fluidTanks, this.slotName, slotName)));
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

    public class FluidTankList implements IFluidHandler, INBTSerializable<CompoundTag> {
        public IO[] cIOs;
        public String[] slotNames;
        public String slotName;
        protected final List<FluidTank> fluidTanks;

        private FluidTankList(IO[] cIOs, final List<FluidTank> fluidTanks, String[] slotNames, @Nullable String slotName) {
            this.cIOs = cIOs;
            this.fluidTanks = fluidTanks;
            this.slotNames = slotNames;
            this.slotName = slotName;
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
                if (io == IO.OUT) {
                    continue;
                }
                if (slotName != null && !slotNames[i].equals(slotName)) {
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
                if (io == IO.OUT) {
                    continue;
                }
                if (slotName != null && !slotNames[i].equals(slotName)) {
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
                if (io == IO.IN) {
                    continue;
                }
                if (slotName != null && !slotNames[i].equals(slotName)) {
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
                if (io == IO.IN) {
                    continue;
                }
                if (slotName != null && !slotNames[i].equals(slotName)) {
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
        public CompoundTag serializeNBT() {
            CompoundTag fluidInventory = new CompoundTag();
            fluidInventory.putInt("TankAmount", this.getTanks());

            ListTag tanks = new ListTag();
            for (int i = 0; i < this.getTanks(); i++) {
                CompoundTag writeTag;
                FluidTank fluidTank = fluidTanks.get(i);
                if (fluidTank != null) {
                    writeTag = fluidTank.writeToNBT(new CompoundTag());
                } else writeTag = new CompoundTag();

                tanks.add(writeTag);
            }
            fluidInventory.put("Tanks", tanks);
            return fluidInventory;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            ListTag tanks = nbt.getList("Tanks", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(fluidTanks.size(), nbt.getInt("TankAmount")); i++) {
                Tag nbtTag = tanks.get(i);
                FluidTank fluidTank = fluidTanks.get(i);
                if (fluidTank != null) {
                    fluidTank.readFromNBT((CompoundTag) nbtTag);
                }
            }
        }

    }

}
