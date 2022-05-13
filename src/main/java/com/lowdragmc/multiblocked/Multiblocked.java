package com.lowdragmc.multiblocked;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib.ItemGroup.LDItemGroup;
import com.lowdragmc.lowdraglib.json.BlockTypeAdapterFactory;
import com.lowdragmc.lowdraglib.json.FluidStackTypeAdapter;
import com.lowdragmc.lowdraglib.json.IBlockStateTypeAdapterFactory;
import com.lowdragmc.lowdraglib.json.ItemStackTypeAdapter;
import com.lowdragmc.multiblocked.api.json.IMultiblockedRendererTypeAdapterFactory;
import com.lowdragmc.multiblocked.api.json.RecipeMapTypeAdapter;
import com.lowdragmc.multiblocked.api.json.RecipeTypeAdapter;
import com.lowdragmc.multiblocked.api.json.SimplePredicateFactory;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.tile.BlueprintTableTileEntity;
import com.lowdragmc.multiblocked.client.ClientProxy;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Random;

@Mod(Multiblocked.MODID)
public class Multiblocked {
    public static final String MODID = "multiblocked";
    public static final String MODID_CT = "crafttweaker";
    public static final String MODID_JEI = "jei";
    public static final String MODID_BOT = "botania";
    public static final String MODID_QMD = "qmd";
    public static final String MODID_TC6 = "thaumcraft";
    public static final String MODID_THAUMJEI = "thaumicjei";
    public static final String MODID_MEK = "mekanism";
    public static final String MODID_GEO = "geckolib3";
    public static final String MODID_GTCE = "gregtech";
    public static final String MODID_LC = "lightningcraft";
    public static final String MODID_TOP = "theoneprobe";
    public static final String MODID_PRODIGY = "prodigytech";
    public static final String MODID_NA = "naturesaura";
    public static final String MODID_EU2 = "extrautils2";
    public static final String MODID_PE = "projecte";
    public static final String MODNAME = "Multiblocked";
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);
    public static final ItemGroup TAB_ITEMS = new LDItemGroup("multiblocked", "all", BlueprintTableTileEntity.tableDefinition::getStackForm);
    public static final Random RNG = new Random();
    public static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(IBlockStateTypeAdapterFactory.INSTANCE)
            .registerTypeAdapterFactory(IMultiblockedRendererTypeAdapterFactory.INSTANCE)
            .registerTypeAdapterFactory(BlockTypeAdapterFactory.INSTANCE)
            .registerTypeAdapterFactory(SimplePredicateFactory.INSTANCE)
            .registerTypeAdapter(ItemStack.class, ItemStackTypeAdapter.INSTANCE)
            .registerTypeAdapter(FluidStack.class, FluidStackTypeAdapter.INSTANCE)
            .registerTypeAdapter(Recipe.class, RecipeTypeAdapter.INSTANCE)
            .registerTypeAdapter(RecipeMap.class, RecipeMapTypeAdapter.INSTANCE)
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .create();
    public static File location;
    
    
    public Multiblocked() {
        location = new File(FMLPaths.GAMEDIR.get().toFile(), "multiblocked");
        location.mkdir();
        DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    }

    public static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    public static boolean isModLoaded(String mod) {
        return ModList.get().isLoaded(mod);
    }

    public static String prettyJson(String uglyJson) {
        return GSON_PRETTY.toJson(new JsonParser().parse(uglyJson));
    }
}
