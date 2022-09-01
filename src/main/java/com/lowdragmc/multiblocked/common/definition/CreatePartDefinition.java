package com.lowdragmc.multiblocked.common.definition;

import com.google.gson.JsonObject;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.common.tile.CreateKineticSourceTileEntity;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;

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
        super(location, partDefinition -> new CreateKineticSourceTileEntity((CreatePartDefinition) partDefinition));
        canShared = false;
        stress = 4;
        isOutput = false;
    }

    @Override
    public void fromJson(JsonObject json) {
        super.fromJson(json);
        isOutput = JSONUtils.getAsBoolean(json, "isOutput", isOutput);
        stress = JSONUtils.getAsFloat(json, "stress", stress);
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        json = super.toJson(json);
        json.addProperty("isOutput", isOutput);
        json.addProperty("stress", stress);
        return json;
    }
}
