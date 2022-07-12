package com.lowdragmc.multiblocked.common.definition;

import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.common.tile.CreateKineticSourceTileEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * @author KilaBash
 * @date 2022/06/02
 * @implNote CreateStressDefinition
 */
public class CreatePartDefinition extends PartDefinition {

    public boolean isOutput;
    public float stress;

    // used for Gson
    public CreatePartDefinition() {
        this(null);
    }

    public CreatePartDefinition(ResourceLocation location) {
        super(location, CreateKineticSourceTileEntity.class);
        canShared = false;
        stress = 4;
        isOutput = false;
    }
}
