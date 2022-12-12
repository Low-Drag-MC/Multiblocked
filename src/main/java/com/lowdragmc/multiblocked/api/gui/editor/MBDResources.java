package com.lowdragmc.multiblocked.api.gui.editor;

import com.lowdragmc.lowdraglib.gui.editor.data.Resources;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import net.minecraft.nbt.CompoundTag;

/**
 * @author KilaBash
 * @date 2022/12/11
 * @implNote MBDResources
 */
public class MBDResources extends Resources {
    protected MBDResources() {
        resources.put(TraitResource.RESOURCE_NAME, new TraitResource());
        resources.put(RecipeResource.RESOURCE_NAME, new RecipeResource());
    }

    public static MBDResources fromNBT(CompoundTag tag) {
        var resource = new MBDResources();
        resource.deserializeNBT(tag);
        return resource;
    }

    public static MBDResources defaultResource() { // default
        MBDResources resources = new MBDResources();
        resources.resources.values().forEach(Resource::buildDefault);
        return resources;
    }

}
