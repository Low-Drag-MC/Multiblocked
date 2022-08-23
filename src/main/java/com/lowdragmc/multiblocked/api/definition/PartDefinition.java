package com.lowdragmc.multiblocked.api.definition;

import com.lowdragmc.multiblocked.api.tile.part.IPartComponent;
import com.lowdragmc.multiblocked.api.tile.part.PartTileEntity;
import net.minecraft.resources.ResourceLocation;
import com.google.gson.JsonObject;
import com.mojang.realmsclient.util.JsonUtils;


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
        canShared = JsonUtils.getBooleanOr("canShared", json, canShared);
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        json = super.toJson(json);
        json.addProperty("canShared", canShared);
        return json;
    }
}
