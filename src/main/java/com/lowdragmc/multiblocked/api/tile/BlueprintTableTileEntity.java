package com.lowdragmc.multiblocked.api.tile;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.pattern.FactoryBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.Predicates;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.client.renderer.impl.MBDBlockStateRenderer;
import com.lowdragmc.multiblocked.client.renderer.impl.MBDIModelRenderer;
import com.lowdragmc.multiblocked.common.ItemMultiblockCapability;
import net.minecraft.block.Blocks;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;

public class BlueprintTableTileEntity extends ControllerTileEntity{

    public BlueprintTableTileEntity(ControllerDefinition definition) {
        super(definition);
    }

    @Override
    public void updateFormed() {
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        recipeLogic = null;
    }

//    @Override
//    public ModularUI createUI(PlayerEntity entityPlayer) {
//        if (isFormed()) {
//            return new ModularUIBuilder(IGuiTexture.EMPTY, 384, 256)
//                    .widget(new BlueprintTableWidget(this))
//                    .build(this, entityPlayer);
//        } else {
//            TabContainer tabContainer = new TabContainer(0, 0, 200, 232);
//            new StructurePageWidget(this.definition, tabContainer);
//            return new ModularUIBuilder(IGuiTexture.EMPTY, 196, 256)
//                    .widget(tabContainer)
//                    .build(this, entityPlayer);
//        }
//    }

    public final static ControllerDefinition tableDefinition = new ControllerDefinition(new ResourceLocation(Multiblocked.MODID, "blueprint_table"), BlueprintTableTileEntity::new);
    public final static PartDefinition partDefinition = new PartDefinition(new ResourceLocation(Multiblocked.MODID, "blueprint_table_part"));

    public static void registerBlueprintTable() {
        tableDefinition.recipeMap.inputCapabilities.add(ItemMultiblockCapability.CAP);
        tableDefinition.baseRenderer = new MBDIModelRenderer(new ResourceLocation(Multiblocked.MODID,"block/blueprint_table_controller"));
        tableDefinition.formedRenderer = new MBDIModelRenderer(new ResourceLocation(Multiblocked.MODID,"block/blueprint_table_formed"));
        tableDefinition.properties.isOpaque = false;

        partDefinition.baseRenderer = new MBDIModelRenderer(new ResourceLocation(Multiblocked.MODID,"block/blueprint_table"));
        partDefinition.allowRotate = false;
        partDefinition.properties.isOpaque = false;

        tableDefinition.basePattern = FactoryBlockPattern.start()
                .aisle("PPP", "C  ")
                .aisle("PTP", "   ")
                .where(' ', Predicates.any())
                .where('T', Predicates.component(tableDefinition))
                .where('P', Predicates.component(partDefinition).disableRenderFormed())
                .where('C', Predicates.anyCapability(ItemMultiblockCapability.CAP).disableRenderFormed())
                .build();
        MbdComponents.registerComponent(tableDefinition);
        MbdComponents.registerComponent(partDefinition);
    }
}
