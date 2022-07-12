package com.lowdragmc.multiblocked.api.definition;

import com.lowdragmc.multiblocked.api.tile.part.IPartComponent;
import com.lowdragmc.multiblocked.api.tile.part.PartTileEntity;
import net.minecraft.resources.ResourceLocation;


public class PartDefinition extends ComponentDefinition {

    public boolean canShared = true;

    // used for Gson
    public PartDefinition() {
        this(null);
    }

    public PartDefinition(ResourceLocation location, Class<? extends IPartComponent> clazz) {
        super(location, clazz);
    }

    public PartDefinition(ResourceLocation location) {
        this(location, PartTileEntity.PartSimpleTileEntity.class);
    }

}
