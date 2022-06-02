package com.lowdragmc.multiblocked.api.registry;

import com.google.common.collect.Maps;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.common.capability.ChemicalMekanismCapability;
import com.lowdragmc.multiblocked.common.capability.CreateStressCapacityCapability;
import com.lowdragmc.multiblocked.common.capability.FEMultiblockCapability;
import com.lowdragmc.multiblocked.common.capability.FluidMultiblockCapability;
import com.lowdragmc.multiblocked.common.capability.HeatMekanismCapability;
import com.lowdragmc.multiblocked.common.capability.ItemMultiblockCapability;
import com.lowdragmc.multiblocked.common.capability.ManaBotaniaCapability;
import net.minecraft.util.ResourceLocation;

import java.util.Map;

public class MbdCapabilities {

    public static final Map<String, MultiblockCapability<?>> CAPABILITY_REGISTRY = Maps.newHashMap();

    public static void registerCapability(MultiblockCapability<?> capability) {
        CAPABILITY_REGISTRY.put(capability.name, capability);
    }

    public static void registerCapabilities() {
        registerCapability(FEMultiblockCapability.CAP);
        registerCapability(ItemMultiblockCapability.CAP);
        registerCapability(FluidMultiblockCapability.CAP);
        if (Multiblocked.isMekLoaded()) {
            registerCapability(ChemicalMekanismCapability.CAP_GAS);
            registerCapability(ChemicalMekanismCapability.CAP_SLURRY);
            registerCapability(ChemicalMekanismCapability.CAP_INFUSE);
            registerCapability(ChemicalMekanismCapability.CAP_PIGMENT);
            registerCapability(HeatMekanismCapability.CAP);
        }
        if (Multiblocked.isBotLoaded()) {
            registerCapability(ManaBotaniaCapability.CAP);
        }
        if (Multiblocked.isCreateLoaded()) {
            registerCapability(CreateStressCapacityCapability.CAP);
        }
    }

    public static MultiblockCapability<?> get(String s) {
        return CAPABILITY_REGISTRY.get(s);
    }

    public static void registerAnyCapabilityBlocks() {
        for (MultiblockCapability<?> capability : MbdCapabilities.CAPABILITY_REGISTRY.values()) {
            ComponentDefinition definition = new PartDefinition(new ResourceLocation(Multiblocked.MODID, capability.name + ".any"));
            definition.properties.isOpaque = false;
            definition.properties.tabGroup = null;
            definition.allowRotate = false;
            definition.showInJei = false;
            MbdComponents.registerComponent(definition);
        }
    }
}
