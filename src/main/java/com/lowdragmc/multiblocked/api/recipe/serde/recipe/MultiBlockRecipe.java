package com.lowdragmc.multiblocked.api.recipe.serde.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.recipe.Content;
import com.lowdragmc.multiblocked.api.recipe.RecipeCondition;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import com.lowdragmc.multiblocked.api.registry.MbdRecipeConditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiBlockRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final String machineType;
    private final Map<MultiblockCapability<?>, List<Content>> inputs;
    private final Map<MultiblockCapability<?>, List<Content>> outputs;
    private final Map<MultiblockCapability<?>, List<Content>> tickInputs;
    private final Map<MultiblockCapability<?>, List<Content>> tickOutputs;
    private final List<RecipeCondition> conditions;
    private final CompoundTag data;
    private final Component text;
    private final int duration;

    public MultiBlockRecipe(ResourceLocation id, String machineType, Map<MultiblockCapability<?>, List<Content>> inputs, Map<MultiblockCapability<?>, List<Content>> outputs, Map<MultiblockCapability<?>, List<Content>> tickInputs, Map<MultiblockCapability<?>, List<Content>> tickOutputs, List<RecipeCondition> conditions, CompoundTag data, Component text, int duration) {
        this.id = id;
        this.machineType = machineType;
        this.inputs = inputs;
        this.outputs = outputs;
        this.tickInputs = tickInputs;
        this.tickOutputs = tickOutputs;
        this.conditions = conditions;
        this.data = data;
        this.text = text;
        this.duration = duration;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    public String getMachineType() {
        return machineType;
    }

    public com.lowdragmc.multiblocked.api.recipe.Recipe getMBDRecipe() {
        ImmutableMap.Builder<MultiblockCapability<?>, ImmutableList<Content>> tickInputMBD = ImmutableMap.builder();
        ImmutableMap.Builder<MultiblockCapability<?>, ImmutableList<Content>> tickOutputMBD = ImmutableMap.builder();
        ImmutableMap.Builder<MultiblockCapability<?>, ImmutableList<Content>> inputMBD = ImmutableMap.builder();
        ImmutableMap.Builder<MultiblockCapability<?>, ImmutableList<Content>> outputMBD = ImmutableMap.builder();

        inputs.forEach((k, v) -> inputMBD.put(k, ImmutableList.copyOf(v)));
        outputs.forEach((k, v) -> outputMBD.put(k, ImmutableList.copyOf(v)));
        tickInputs.forEach((k, v) -> tickInputMBD.put(k, ImmutableList.copyOf(v)));
        tickOutputs.forEach((k, v) -> tickOutputMBD.put(k, ImmutableList.copyOf(v)));

        return new com.lowdragmc.multiblocked.api.recipe.Recipe(id.toString(),
                inputMBD.build(),
                outputMBD.build(),
                tickInputMBD.build(),
                tickOutputMBD.build(),
                ImmutableList.copyOf(conditions),
                data.copy(),
                text, duration
        );
    }


    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return MBDRecipeType.MULTIBLOCK_RECIPE_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return MBDRecipeType.MULTIBLOCK_RECIPE_TYPE;
    }

    public static class MultiBlockRecipeType implements RecipeType<MultiBlockRecipe> {
        public static final ResourceLocation TYPE_ID = new ResourceLocation(Multiblocked.MODID, "multiblock");
    }

    public static class Serializer implements RecipeSerializer<MultiBlockRecipe> {

        public Map<MultiblockCapability<?>, List<Content>> capabilitiesFromJson(JsonObject json) {
            Map<MultiblockCapability<?>, List<Content>> capabilities = new HashMap<>();
            for (String key : json.keySet()) {
                JsonArray contentsJson = json.getAsJsonArray(key);
                MultiblockCapability<?> capability = MbdCapabilities.get(key);
                List<Content> contents = new ArrayList<>();
                for (JsonElement contentJson : contentsJson) {
                    contents.add(capability.serializer.fromJsonContent(contentJson));
                }
                capabilities.put(capability, contents);
            }
            return capabilities;
        }

        @Override
        public @NotNull MultiBlockRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            String machineType = GsonHelper.getAsString(json, "machine_map");
            int duration = json.has("duration") ? GsonHelper.getAsInt(json, "duration") : 100;
            Component component = json.has("text") ? new TranslatableComponent(GsonHelper.getAsString(json, "text")) : null;
            CompoundTag data = new CompoundTag();
            try {
                if (json.has("data"))
                    data = TagParser.parseTag(GsonHelper.getAsString(json, "data"));
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
            Map<MultiblockCapability<?>, List<Content>> inputs = capabilitiesFromJson(json.has("inputs") ? json.getAsJsonObject("inputs") : new JsonObject());
            Map<MultiblockCapability<?>, List<Content>> tickInputs = capabilitiesFromJson(json.has("tickInputs") ? json.getAsJsonObject("tickInputs") : new JsonObject());
            Map<MultiblockCapability<?>, List<Content>> outputs = capabilitiesFromJson(json.has("outputs") ? json.getAsJsonObject("outputs") : new JsonObject());
            Map<MultiblockCapability<?>, List<Content>> tickOutputs = capabilitiesFromJson(json.has("tickOutputs") ? json.getAsJsonObject("tickOutputs") : new JsonObject());
            List<RecipeCondition> conditions = new ArrayList<>();
            JsonObject conditionsJson = json.has("recipeConditions") ? json.getAsJsonObject("recipeConditions") : new JsonObject();
            for (String conditionKey : conditionsJson.keySet()) {
                RecipeCondition condition = MbdRecipeConditions.getCondition(conditionKey).createTemplate();
                condition.deserialize(conditionsJson.getAsJsonObject(conditionKey));
                conditions.add(condition);
            }

            return new MultiBlockRecipe(id, machineType, inputs, outputs, tickInputs, tickOutputs, conditions, data, component, duration);
        }

        private static Tuple<MultiblockCapability<?>, List<Content>> entryReader(FriendlyByteBuf buf) {
            MultiblockCapability<?> capability = MbdCapabilities.getByIndex(buf.readVarInt());
            List<Content> contents = buf.readList(capability.serializer::fromNetworkContent);
            return new Tuple<>(capability, contents);
        }

        private static void entryWriter(FriendlyByteBuf buf, Map.Entry<MultiblockCapability<?>, List<Content>> entry) {
            MultiblockCapability<?> capability = entry.getKey();
            List<Content> contents = entry.getValue();
            buf.writeVarInt(MbdCapabilities.indexOf(capability));
            buf.writeCollection(contents, capability.serializer::toNetworkContent);
        }

        public static RecipeCondition conditionReader(FriendlyByteBuf buf) {
            RecipeCondition condition = MbdRecipeConditions.getConditionByIndex(buf.readVarInt()).createTemplate();
            return condition.fromNetwork(buf);
        }

        public static void conditionWriter(FriendlyByteBuf buf, RecipeCondition condition) {
            buf.writeVarInt(MbdRecipeConditions.getConditionOrder(condition));
            condition.toNetwork(buf);
        }

        private static Map<MultiblockCapability<?>, List<Content>> tuplesToMap(List<Tuple<MultiblockCapability<?>, List<Content>>> entries) {
            Map<MultiblockCapability<?>, List<Content>> map = new HashMap<>();
            entries.forEach(entry -> map.put(entry.getA(), entry.getB()));
            return map;
        }

        @Nullable
        @Override
        public MultiBlockRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buf) {
            String machineType = buf.readUtf();
            int duration = buf.readInt();
            Map<MultiblockCapability<?>, List<Content>> inputs = tuplesToMap(buf.readCollection(c -> new ArrayList<>(), Serializer::entryReader));
            Map<MultiblockCapability<?>, List<Content>> tickInputs = tuplesToMap(buf.readCollection(c -> new ArrayList<>(), Serializer::entryReader));
            Map<MultiblockCapability<?>, List<Content>> outputs = tuplesToMap(buf.readCollection(c -> new ArrayList<>(), Serializer::entryReader));
            Map<MultiblockCapability<?>, List<Content>> tickOutputs = tuplesToMap(buf.readCollection(c -> new ArrayList<>(), Serializer::entryReader));
            List<RecipeCondition> conditions = buf.readCollection(c -> new ArrayList<>(), Serializer::conditionReader);
            CompoundTag data = buf.readNbt();
            Component component = buf.readComponent();
            return new MultiBlockRecipe(id, machineType, inputs, outputs, tickInputs, tickOutputs, conditions, data, component, duration);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MultiBlockRecipe recipe) {
            buf.writeUtf(recipe.machineType);
            buf.writeInt(recipe.duration);
            buf.writeCollection(recipe.inputs.entrySet(), Serializer::entryWriter);
            buf.writeCollection(recipe.tickInputs.entrySet(), Serializer::entryWriter);
            buf.writeCollection(recipe.outputs.entrySet(), Serializer::entryWriter);
            buf.writeCollection(recipe.tickOutputs.entrySet(), Serializer::entryWriter);
            buf.writeCollection(recipe.conditions, Serializer::conditionWriter);
            buf.writeNbt(recipe.data);
            buf.writeComponent(recipe.text);
        }

        @Nullable
        private ResourceLocation registryName;

        @Override
        public RecipeSerializer<?> setRegistryName(ResourceLocation name) {
            registryName = name;
            return this;
        }

        @Nullable
        @Override
        public ResourceLocation getRegistryName() {
            return registryName;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<RecipeSerializer<?>> getRegistryType() {
            Class<?> clazz = RecipeSerializer.class;
            return (Class<RecipeSerializer<?>>) clazz;
        }
    }

    @Override
    public boolean matches(@NotNull Container pContainer, @NotNull Level pLevel) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container pContainer) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return false;
    }

    @Override
    public @NotNull ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

}
