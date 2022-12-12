package com.lowdragmc.multiblocked.api.capability;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.recipe.serde.content.SerializerDouble;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * @author KilaBash
 * @date 2022/11/29
 * @implNote GuiOnlyCapability
 */
public class GuiOnlyCapability extends MultiblockCapability<Double> {
    Function<MultiblockCapability<?>, CapabilityTrait> supplier;

    public GuiOnlyCapability(String name, Function<MultiblockCapability<?>, CapabilityTrait> supplier) {
        super(name, 0xffafafaf, SerializerDouble.INSTANCE);
        this.supplier = supplier;
    }

    @Override
    public Double defaultContent() {
        return 0d;
    }

    @Override
    public boolean isBlockHasCapability(@NotNull IO io, @NotNull BlockEntity tileEntity) {
        return false;
    }

    @Override
    public Double copyInner(Double content) {
        return 0d;
    }

    @Override
    protected CapabilityProxy<? extends Double> createProxy(@NotNull IO io, @NotNull BlockEntity tileEntity) {
        return null;
    }

    @Override
    public BlockInfo[] getCandidates() {
        return new BlockInfo[0];
    }

    @Override
    public boolean hasTrait() {
        return true;
    }

    @Override
    public CapabilityTrait createTrait() {
        return supplier.apply(this);
    }
}
