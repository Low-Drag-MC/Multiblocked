package com.lowdragmc.multiblocked.api.tile;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.gui.blueprint_table.BlueprintTableWidget;
import com.lowdragmc.multiblocked.api.gui.controller.structure.StructurePageWidget;
import com.lowdragmc.multiblocked.api.pattern.FactoryBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.Predicates;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.client.renderer.impl.MBDIModelRenderer;
import com.lowdragmc.multiblocked.common.capability.ItemMultiblockCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class BlueprintTableTileEntity extends ControllerTileEntity{

    public BlueprintTableTileEntity(ControllerDefinition definition, BlockPos pos, BlockState state) {
        super(definition, pos, state);
    }

    @Override
    public void updateFormed() {
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        recipeLogic = null;
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        if (isFormed()) {
            return new ModularUI(384, 256, this, entityPlayer).widget(new BlueprintTableWidget(this));
        } else {
            TabContainer tabContainer = new TabContainer(0, 0, 200, 232);
            new StructurePageWidget(this.definition, tabContainer);
            return new ModularUI(196, 256, this, entityPlayer).widget(tabContainer);
        }
    }

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
