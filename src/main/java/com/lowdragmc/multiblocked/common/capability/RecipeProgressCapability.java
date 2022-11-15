package com.lowdragmc.multiblocked.common.capability;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.recipe.serde.content.SerializerDouble;
import com.lowdragmc.multiblocked.common.capability.trait.RecipeProgressTrait;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * @author KilaBash
 * @date 2022/11/15
 * @implNote RecipeProgressCapability
 */
public class RecipeProgressCapability extends MultiblockCapability<Double> {

    public static final RecipeProgressCapability CAP = new RecipeProgressCapability();

    private RecipeProgressCapability() {
        super("recipe_progress", 0xffafafaf, SerializerDouble.INSTANCE);
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
        return new RecipeProgressTrait();
    }
}
