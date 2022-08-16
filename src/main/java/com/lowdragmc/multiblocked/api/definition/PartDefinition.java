package com.lowdragmc.multiblocked.api.definition;

import com.google.gson.JsonObject;
import com.lowdragmc.multiblocked.api.tile.part.PartTileEntity;
import com.mojang.realmsclient.util.JsonUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.util.function.Function;


public class PartDefinition extends ComponentDefinition {
    
    public boolean canShared = true;

    // used for Gson
    public PartDefinition() {
        this(null);
    }

    public PartDefinition(ResourceLocation location, Function<PartDefinition, TileEntity> teSupplier) {
        super(location, d -> teSupplier.apply((PartDefinition) d));
    }

    public PartDefinition(ResourceLocation location) {
        this(location, PartTileEntity.PartSimpleTileEntity::new);
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
