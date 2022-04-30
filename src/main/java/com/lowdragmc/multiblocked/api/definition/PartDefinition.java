package com.lowdragmc.multiblocked.api.definition;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.api.tile.part.PartTileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.function.Function;


public class PartDefinition extends ComponentDefinition {
    
    public boolean canShared = true;

    // used for Gson
    public PartDefinition() {
        this(null);
    }

    public PartDefinition(ResourceLocation location, Function<PartDefinition, ? extends ComponentTileEntity<?>> teSupplier) {
        super(location, d -> teSupplier.apply((PartDefinition) d));
    }

    public PartDefinition(ResourceLocation location) {
        this(location, PartTileEntity.PartSimpleTileEntity::new);
    }

}
