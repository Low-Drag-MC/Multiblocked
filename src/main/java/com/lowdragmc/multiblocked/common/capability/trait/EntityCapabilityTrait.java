package com.lowdragmc.multiblocked.common.capability.trait;

import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.EntityMultiblockCapability;

/**
 * @author KilaBash
 * @date 2022/8/3
 * @implNote EntityCapabilityTrait
 */
public class EntityCapabilityTrait extends CapabilityTrait {

    public EntityCapabilityTrait() {
        super(EntityMultiblockCapability.CAP);
    }

}
