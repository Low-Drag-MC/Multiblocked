package com.lowdragmc.multiblocked.api.tile;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.SelectorWidget;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;

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

    @Override
    public ModularUI createUI(PlayerEntity entityPlayer) {
        if (isFormed()) {
            return new ModularUI(384, 256, this, entityPlayer).widget(new BlueprintTableWidget(this))
                    .widget(new SelectorWidget(50, 150, 100, 20, Arrays.asList("1123","2123","3123","4123","1235","1236"), -1)
                            .setButtonBackground(new ColorRectTexture(0xffff0000)));
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
