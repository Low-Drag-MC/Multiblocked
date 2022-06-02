package com.lowdragmc.multiblocked.api.definition;

import com.lowdragmc.multiblocked.api.tile.part.PartTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.function.TriFunction;


public class PartDefinition extends ComponentDefinition {

    public boolean canShared = true;

    // used for Gson
    public PartDefinition() {
        this(null);
    }

    public PartDefinition(ResourceLocation location, TriFunction<PartDefinition, BlockPos, BlockState, BlockEntity> teSupplier) {
        super(location, (d, p, s) -> teSupplier.apply((PartDefinition) d, p, s));
    }

    public PartDefinition(ResourceLocation location) {
        this(location, PartTileEntity.PartSimpleTileEntity::new);
    }

}
