package com.lowdragmc.multiblocked.common.capability;

import com.google.gson.*;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.trait.RecipeProgressTrait;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;

/**
 * @author KilaBash
 * @date 2022/11/15
 * @implNote RecipeProgressCapability
 */
public class RecipeProgressCapability extends MultiblockCapability<Double> {

    public static final RecipeProgressCapability CAP = new RecipeProgressCapability();

    private RecipeProgressCapability() {
        super("recipe_progress", 0xffafafaf);
    }

    @Override
    public Double defaultContent() {
        return 0d;
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull TileEntity tileEntity) {
        return false;
    }

    @Override
    public Double copyInner(Double content) {
        return 0d;
    }

    @Override
    protected CapabilityProxy<? extends Double> createProxy(@Nonnull IO io, @Nonnull TileEntity tileEntity) {
        return null;
    }

    @Override
    public BlockInfo[] getCandidates() {
        return new BlockInfo[0];
    }

    @Override
    public Double of(Object o) {
        return 0d;
    }

    @Override
    public Double deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return 0d;
    }

    @Override
    public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src);
    }

    @Override
    public boolean hasTrait() {
        return true;
    }

    @Override
    public CapabilityTrait createTrait() {
        return new RecipeProgressTrait();
    }

}
