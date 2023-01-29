package com.lowdragmc.multiblocked.common.capability;

import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapCapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.ContentModifier;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.serde.content.SerializerFloat;
import com.lowdragmc.multiblocked.common.capability.trait.PneumaticMachineTrait;
import com.lowdragmc.multiblocked.common.capability.widget.NumberContentWidget;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.common.core.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author youyihj
 */
public class PneumaticPressureCapability extends MultiblockCapability<Float> {
    public static final PneumaticPressureCapability CAP = new PneumaticPressureCapability();

    protected PneumaticPressureCapability() {
        super("pneumatic_pressure", 0xFFFF3C00, SerializerFloat.INSTANCE);
    }

    @Override
    public Float defaultContent() {
        return 2.0f;
    }

    @Override
    public Float copyInner(Float content) {
        return content;
    }

    @Override
    public Float copyWithModifier(Float content, ContentModifier modifier) {
        return modifier.apply(content).floatValue();
    }

    @Override
    public CapabilityProxy<? extends Float> createProxy(@Nonnull IO io, @Nonnull BlockEntity blockEntity) {
        return new Proxy(blockEntity);
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull BlockEntity blockEntity) {
        return !getCapability(PNCCapabilities.AIR_HANDLER_CAPABILITY, blockEntity).isEmpty();
    }

    @Override
    public BlockInfo[] getCandidates() {
        return new BlockInfo[]{new BlockInfo(ModBlocks.PRESSURE_CHAMBER_WALL.get())};
    }

    @Override
    public boolean hasTrait() {
        return true;
    }

    @Override
    public ContentWidget<? super Float> createContentWidget() {
        return new NumberContentWidget().setContentTexture(new TextTexture("P", color)).setUnit("bar");
    }

    @Override
    public CapabilityTrait createTrait() {
        return new PneumaticMachineTrait();
    }

    private static class Proxy extends CapCapabilityProxy<IAirHandlerMachine, Float> {
        public Proxy(BlockEntity blockEntity) {
            super(PneumaticPressureCapability.CAP, blockEntity, PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY);
        }

        int air;
        int volume;

        @Override
        protected boolean hasInnerChanged() {
            IAirHandlerMachine airHandler = getCapability(null);
            if (airHandler == null) return false;
            if (airHandler.getAir() == air && airHandler.getVolume() == volume) {
                return false;
            }
            air = airHandler.getAir();
            volume = airHandler.getVolume();
            return true;
        }

        @Override
        protected List<Float> handleRecipeInner(IO io, Recipe recipe, List<Float> left, @Nullable String slotName, boolean simulate) {
            IAirHandlerMachine handler = getCapability(slotName);
            float sum = left.stream().reduce(0.0f, Float::sum);
            int consumeAir = (int) (sum * 50);
            if (handler != null && io == IO.IN) {
                int air = handler.getAir();
                if (Math.signum(air) == Math.signum(consumeAir) && Math.abs(air) >= Math.abs(consumeAir) && Math.abs(handler.getPressure()) >= Math.abs(sum)) {
                    if (!simulate) {
                        handler.addAir(-consumeAir);
                    }
                    return null;
                }
            }
            return left;
        }
    }
}

