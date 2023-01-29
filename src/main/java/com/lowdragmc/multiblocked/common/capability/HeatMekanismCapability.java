package com.lowdragmc.multiblocked.common.capability;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapCapabilityProxy;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.ContentModifier;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.serde.content.SerializerDouble;
import com.lowdragmc.multiblocked.common.capability.widget.NumberContentWidget;
import mekanism.api.heat.IHeatHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.List;

public class HeatMekanismCapability extends MultiblockCapability<Double> {
    public static final HeatMekanismCapability CAP = new HeatMekanismCapability();

    private HeatMekanismCapability() {
        super("mek_heat", 0xFFD9068D, SerializerDouble.INSTANCE);
    }

    @Override
    public Double defaultContent() {
        return 100d;
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull BlockEntity tileEntity) {
        return !getCapability(Capabilities.HEAT_HANDLER_CAPABILITY, tileEntity).isEmpty();
    }

    @Override
    public Double copyInner(Double content) {
        return content;
    }

    @Override
    public Double copyWithModifier(Double content, ContentModifier modifier) {
        return modifier.apply(content).doubleValue();
    }

    @Override
    public HeatMekanismCapabilityProxy createProxy(@Nonnull IO io, @Nonnull
            BlockEntity tileEntity) {
        return new HeatMekanismCapabilityProxy(tileEntity);
    }

    @Override
    public ContentWidget<? super Double> createContentWidget() {
        return new NumberContentWidget().setContentTexture(new TextTexture("HE", color)).setUnit("Heat");
    }

    @Override
    public BlockInfo[] getCandidates() {
        return new BlockInfo[]{
                BlockInfo.fromBlock(MekanismBlocks.FUELWOOD_HEATER.getBlock()),
                BlockInfo.fromBlock(MekanismBlocks.RESISTIVE_HEATER.getBlock()),
        };
    }

    @Override
    public Double deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return jsonElement.getAsDouble();
    }

    @Override
    public JsonElement serialize(Double aDouble, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(aDouble);
    }

    public static class HeatMekanismCapabilityProxy extends CapCapabilityProxy<IHeatHandler, Double> {

        public HeatMekanismCapabilityProxy(BlockEntity tileEntity) {
            super(HeatMekanismCapability.CAP, tileEntity, Capabilities.HEAT_HANDLER_CAPABILITY);
        }

        @Override
        protected List<Double> handleRecipeInner(IO io, Recipe recipe, List<Double> left, @Nullable String slotName, boolean simulate) {
            IHeatHandler capability = getCapability(slotName);
            if (capability == null || capability.getTotalTemperature() <= 0) return left;
            double sum = left.stream().reduce(0d, Double::sum);
            if (io == IO.IN) {
                if (!simulate) {
                    capability.handleHeat(-sum);
                }
            } else if (io == IO.OUT) {
                if (!simulate) {
                    capability.handleHeat(sum);
                }
            }
            return null;
        }

        double lastTemp = -Double.MAX_VALUE;

        @Override
        protected boolean hasInnerChanged() {
            IHeatHandler capability = getCapability(null);
            if (capability == null || capability.getTotalTemperature() <= 0) return false;
            if (lastTemp == capability.getTotalTemperature()) return false;
            lastTemp = capability.getTotalTemperature();
            return true;
        }
    }
}
