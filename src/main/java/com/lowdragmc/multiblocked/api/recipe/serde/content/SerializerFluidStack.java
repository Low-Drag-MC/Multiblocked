package com.lowdragmc.multiblocked.api.recipe.serde.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

public class SerializerFluidStack implements IContentSerializer<FluidStack> {
    @Override
    public void toNetwork(FriendlyByteBuf buf, FluidStack content) {
        content.writeToPacket(buf);
    }

    @Override
    public FluidStack fromNetwork(FriendlyByteBuf buf) {
        return FluidStack.readFromPacket(buf);
    }

    @Override
    public FluidStack fromJson(JsonElement json) {
        if (!json.isJsonObject())
            return FluidStack.EMPTY;
        var jObj = json.getAsJsonObject();
        var fluid = new ResourceLocation(jObj.get("fluid").getAsString());
        var amount = jObj.get("amount").getAsInt();
        var fluidStack = new FluidStack(Objects.requireNonNull(ForgeRegistries.FLUIDS.getValue(fluid)), amount);
        if (jObj.has("nbt")) {
            try {
                fluidStack.setTag(TagParser.parseTag(jObj.get("nbt").getAsString()));
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
        }
        return fluidStack;
    }

    @Override
    public JsonElement toJson(FluidStack content) {
        var json = new JsonObject();
        json.addProperty("fluid", Objects.requireNonNull(content.getFluid().getRegistryName()).toString());
        json.addProperty("amount", content.getAmount());
        if (content.hasTag())
            json.addProperty("nbt", content.getTag().toString());
        return json;
    }
}
