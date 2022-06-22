package com.lowdragmc.multiblocked;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * @author KilaBash
 * @date 2022/06/22
 * @implNote MbdConfig
 */
public class MbdConfig {
    public static ForgeConfigSpec.IntValue naturesAura;

    public static void registerConfig(){
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        registerCommonConfig(commonBuilder);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, commonBuilder.build());
    }

    private static void registerCommonConfig(ForgeConfigSpec.Builder builder){
        naturesAura = builder.comment(
                        "set the radius of aura value consumption.",
                        "Default: 20")
                .defineInRange("Natures Aura Radius",20,1,64);

    }
}
