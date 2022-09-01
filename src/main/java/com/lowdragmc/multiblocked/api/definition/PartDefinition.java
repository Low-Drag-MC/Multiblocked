package com.lowdragmc.multiblocked.api.definition;

import com.lowdragmc.multiblocked.api.tile.part.IPartComponent;
import com.lowdragmc.multiblocked.api.tile.part.PartTileEntity;
import net.minecraft.resources.ResourceLocation;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;


public class PartDefinition extends ComponentDefinition {

    public boolean canShared = true;

    public PartDefinition(ResourceLocation location, Class<? extends IPartComponent> clazz) {
        super(location, clazz);
    }

    public PartDefinition(ResourceLocation location) {
        this(location, PartTileEntity.PartSimpleTileEntity.class);
    }

    @Override
    public void fromJson(JsonObject json) {
        super.fromJson(json);
        canShared = GsonHelper.getAsBoolean(json, "canShared", canShared);
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        json = super.toJson(json);
        json.addProperty("canShared", canShared);
        return json;
    }
}
