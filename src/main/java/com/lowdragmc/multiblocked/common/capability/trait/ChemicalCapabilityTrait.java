package com.lowdragmc.multiblocked.common.capability.trait;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.trait.MultiCapabilityTrait;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.common.capability.ChemicalMekanismCapability;
import com.lowdragmc.multiblocked.common.capability.widget.ChemicalStackWidget;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unchecked")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ChemicalCapabilityTrait<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>, TANK extends IChemicalTank<CHEMICAL, STACK>> extends MultiCapabilityTrait {
    private final ChemicalTankBuilder<CHEMICAL, STACK, TANK> tankBuilder;
    private List<TANK> handlers;
    private long[] tankCapability;

    public ChemicalCapabilityTrait(ChemicalMekanismCapability<CHEMICAL, STACK> cap, ChemicalTankBuilder<CHEMICAL, STACK, TANK> tankBuilder) {
        super(cap);
        this.tankBuilder = tankBuilder;
    }

    @Override
    public void serialize(@Nullable JsonElement jsonElement) {
        super.serialize(jsonElement);
        if (jsonElement == null) {
            jsonElement = new JsonArray();
        }
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        int size = jsonArray.size();
        tankCapability = new long[size];
        handlers = new ArrayList<>(size);
        for (JsonElement element : jsonArray) {
            JsonObject jsonObject = element.getAsJsonObject();
            tankCapability[handlers.size()] = GsonHelper.getAsLong(jsonObject, "tankCapability", 1000L);
            handlers.add(tankBuilder.createAllValid(tankCapability[handlers.size()], this::markAsDirty));
        }
    }

    @Override
    public JsonElement deserialize() {
        JsonArray jsonArray = super.deserialize().getAsJsonArray();
        for (int i = 0; i < capabilityIO.length; i++) {
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            jsonObject.addProperty("tankCapability", tankCapability[i]);
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
                TANK already = handlers.get(i);
                STACK stack = already.getStack();
                if (capabilityIO[i] == IO.IN) {
                    long need = already.getCapacity() - already.getStored();
                    if (need > 0) {
                        for (Direction facing : getIOFacing()) {
                            BlockEntity te = component.getLevel().getBlockEntity(component.getBlockPos().relative(facing));
                            if (te != null) {
                                AtomicBoolean r = new AtomicBoolean(false);
                                te.getCapability(getCap().capability, facing.getOpposite()).ifPresent(handler -> {
                                    if (!already.insert(handler.extractChemical(getCap().createStack.apply(stack.getRaw(), need), Action.EXECUTE), Action.EXECUTE, AutomationType.INTERNAL).isEmpty()) {
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
                    if (already.getStored() > 0) {
                        for (Direction facing : getIOFacing()) {
                            BlockEntity te = component.getLevel().getBlockEntity(component.getBlockPos().relative(facing));
                            if (te != null) {
                                AtomicBoolean r = new AtomicBoolean(false);
                                te.getCapability(getCap().capability, facing.getOpposite()).ifPresent(handler -> {
                                    if (!already.extract(handler.insertChemical((STACK) stack.copy(), Action.EXECUTE).getAmount(), Action.EXECUTE, AutomationType.INTERNAL).isEmpty()) {
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
    protected void initSettingDialog(DialogWidget dialog, DraggableWidgetGroup slot, int index) {
        super.initSettingDialog(dialog, slot, index);
        dialog.addWidget(new LabelWidget(5, 60, "multiblocked.gui.label.tank_capability"));
        dialog.addWidget(new TextFieldWidget(5, 70, 100, 15, null, s -> tankCapability[index] = Integer.parseInt(s))
                .setNumbersOnly(1, Integer.MAX_VALUE)
                .setCurrentString(tankCapability[index] + ""));
    }

    @Override
    protected void addSlot() {
        super.addSlot();
        tankCapability = ArrayUtils.add(tankCapability, 1000);
    }

    @Override
    protected void removeSlot(int index) {
        super.removeSlot(index);
        tankCapability = ArrayUtils.remove(tankCapability, index);
    }

    @Override
    public void readFromNBT(CompoundTag compound) {
        super.readFromNBT(compound);
        ListTag listNBT = compound.getList("_", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(listNBT.size(), handlers.size()); i++) {
            handlers.get(i).deserializeNBT(listNBT.getCompound(i));
        }
    }

    @Override
    public void writeToNBT(CompoundTag compound) {
        super.writeToNBT(compound);
        ListTag listNBT = new ListTag();
        for (TANK handler : handlers) {
            listNBT.add(handler.serializeNBT());
        }
        compound.put("_", listNBT);
    }

    @Override
    public void createUI(ComponentTileEntity<?> component, WidgetGroup group, Player player) {
        super.createUI(component, group, player);
        if (handlers != null) {
            for (int i = 0; i < handlers.size(); i++) {
                group.addWidget(new ChemicalStackWidget<>(getCap(), new ProxyChemicalHandler(guiIO, false), i, x[i], y[i]));
            }
        }
    }

    private ChemicalMekanismCapability<CHEMICAL, STACK> getCap() {
        return (ChemicalMekanismCapability<CHEMICAL, STACK>) this.capability;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        return getCap().capability.orEmpty(capability, LazyOptional.of(() -> new ProxyChemicalHandler(capabilityIO, false)));
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getInnerCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        return getCap().capability.orEmpty(capability, LazyOptional.of(() -> new ProxyChemicalHandler(capabilityIO, true)));
    }

    public class ProxyChemicalHandler implements IChemicalHandler<CHEMICAL, STACK> {
        public boolean inner;
        public IO[] ios;

        public ProxyChemicalHandler(IO[] ios, boolean inner) {
            this.inner = inner;
            this.ios = ios;
        }


        @Override
        public int getTanks() {
            return handlers.size();
        }

        @Override
        public STACK getChemicalInTank(int i) {
            return i < handlers.size() ? handlers.get(i).getStack() : getEmptyStack();
        }

        @Override
        public void setChemicalInTank(int i, STACK stack) {
            if (i < handlers.size()) {
                handlers.get(i).setStack(stack);
            }
        }

        @Override
        public long getTankCapacity(int i) {
            return i < tankCapability.length ? tankCapability[i] : 0;
        }

        @Override
        public boolean isValid(int i, STACK stack) {
            return i < handlers.size() && handlers.get(i).isValid(stack);
        }

        @Override
        public STACK insertChemical(int i, STACK stack, Action action) {
            IO io = ios[i];
            if (io == IO.BOTH || (inner ? io == IO.OUT : io == IO.IN)) {
                return i < handlers.size() ? handlers.get(i).insert(stack, action, AutomationType.EXTERNAL) : stack;
            }
            return stack;
        }

        @Override
        public STACK extractChemical(int i, long l, Action action) {
            IO io = ios[i];
            if (io == IO.BOTH || (inner ? io == IO.IN : io == IO.OUT)) {
                return i < handlers.size() ? handlers.get(i).extract(l, action, AutomationType.EXTERNAL) : this.getEmptyStack();

            }
            return getEmptyStack();
        }

        @Override
        public STACK insertChemical(STACK stack, Action action) {
            return IChemicalHandler.super.insertChemical(stack, action);
        }

        @Override
        public STACK extractChemical(long amount, Action action) {
            return IChemicalHandler.super.extractChemical(amount, action);
        }

        @Override
        public STACK extractChemical(STACK stack, Action action) {
            return IChemicalHandler.super.extractChemical(stack, action);
        }

        @Nonnull
        @Override
        public STACK getEmptyStack() {
            return (STACK) getCap().empty.getStack(0);
        }
    }

}
