package com.lowdragmc.multiblocked.common.definition;

import com.google.gson.JsonObject;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.common.tile.CreateKineticSourceTileEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

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

    @Override
    public void fromJson(JsonObject json) {
        super.fromJson(json);
        isOutput = GsonHelper.getAsBoolean(json, "isOutput", isOutput);
        stress = GsonHelper.getAsFloat(json, "stress", stress);
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        json = super.toJson(json);
        json.addProperty("isOutput", isOutput);
        json.addProperty("stress", stress);
        return json;
    }
}
