package com.lowdragmc.multiblocked.common.capability.trait;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.capability.trait.SingleCapabilityTrait;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.common.capability.PneumaticPressureCapability;
import com.lowdragmc.multiblocked.common.capability.widget.PressureWidget;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.pressure.PressureTier;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.common.PneumaticCraftAPIHandler;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * @author youyihj
 */
public class PneumaticMachineTrait extends SingleCapabilityTrait {
    private IAirHandlerMachine airHandler;
    public float dangerPressure;
    public float criticalPressure;
    public int volume;

    public PneumaticMachineTrait() {
        super(PneumaticPressureCapability.CAP);
    }

    public IAirHandlerMachine getAirHandler() {
        return airHandler;
    }

    @Override
    public JsonElement deserialize() {
        JsonObject jsonObject = super.deserialize().getAsJsonObject();
        jsonObject.addProperty("dangerPressure", dangerPressure);
        jsonObject.addProperty("criticalPressure", criticalPressure);
        jsonObject.addProperty("volume", volume);
        return jsonObject;
    }

    @Override
    public void serialize(@Nullable JsonElement jsonElement) {
        super.serialize(jsonElement);
        if (jsonElement == null) {
            jsonElement = new JsonObject();
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        dangerPressure = GsonHelper.getAsFloat(jsonObject, "dangerPressure", 5.0f);
        criticalPressure = GsonHelper.getAsFloat(jsonObject, "criticalPressure", 7.0f);
        volume = GsonHelper.getAsInt(jsonObject, "volume", 10000);
        airHandler = PneumaticCraftAPIHandler.getInstance().getAirHandlerMachineFactory().createAirHandler(new PressureTier() {
            @Override
            public float getDangerPressure() {
                return dangerPressure;
            }

            @Override
            public float getCriticalPressure() {
                return criticalPressure;
            }
        }, volume);
    }

    @Override
    public void readFromNBT(CompoundTag compound) {
        if (compound.contains("pnc")) {
            airHandler.deserializeNBT(compound.getCompound("pnc"));
        }
    }

    @Override
    public void writeToNBT(CompoundTag compound) {
        compound.put("pnc", airHandler.serializeNBT());
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @org.jetbrains.annotations.Nullable Direction facing) {
        return PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY.orEmpty(capability, LazyOptional.of(() -> airHandler));
    }

    @Override
    public void update() {
        airHandler.tick(component);
    }

    @Override
    public boolean hasUpdate() {
        return true;
    }

    @Override
    public void createUI(ComponentTileEntity<?> component, WidgetGroup group, Player player) {
        super.createUI(component, group, player);
        group.addWidget(new PressureWidget(x, y, dangerPressure, criticalPressure, airHandler.getPressure(), airHandler.getVolume(), airHandler.getAir(), this));
    }

    @Override
    protected void initSettingDialog(DialogWidget dialog, DraggableWidgetGroup slot) {
        super.initSettingDialog(dialog, slot);

        dialog.addWidget(new TextFieldWidget(60, 25, 100, 15, null, s -> {
            dangerPressure = Float.parseFloat(s);
            updateSettings();
        })
                .setNumbersOnly(0.0f, Float.MAX_VALUE)
                .setCurrentString(dangerPressure + "")
                .setHoverTooltips("multiblocked.gui.trait.pressure.tips.0"));
        dialog.addWidget(new TextFieldWidget(60, 45, 100, 15, null, s -> {
            criticalPressure = Float.parseFloat(s);
            updateSettings();
        })
                .setNumbersOnly(0.0f, Float.MAX_VALUE)
                .setCurrentString(criticalPressure + "")
                .setHoverTooltips("multiblocked.gui.trait.pressure.tips.1"));
        dialog.addWidget(new TextFieldWidget(60, 65, 100, 15, null, s -> {
            volume = Integer.parseInt(s);
            updateSettings();
        })
                .setNumbersOnly(0, Integer.MAX_VALUE)
                .setCurrentString(volume + "")
                .setHoverTooltips("multiblocked.gui.trait.pressure.tips.2"));
    }

//    @Override
//    public void onNeighborChanged() {
//        airHandler.onNeighborChange();
//    }

    public void updateSettings() {
        int air = airHandler.getAir();
        airHandler = PneumaticCraftAPIHandler.getInstance().getAirHandlerMachineFactory().createAirHandler(new PressureTier() {
            @Override
            public float getDangerPressure() {
                return dangerPressure;
            }

            @Override
            public float getCriticalPressure() {
                return criticalPressure;
            }
        }, volume);
        airHandler.addAir(air);
//        airHandler.validate(component);
    }

}
