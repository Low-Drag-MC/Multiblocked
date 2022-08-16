package com.lowdragmc.multiblocked.api.tile;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.gui.controller.IOPageWidget;
import com.lowdragmc.multiblocked.api.gui.controller.RecipePage;
import com.lowdragmc.multiblocked.api.gui.controller.structure.StructurePageWidget;
import com.lowdragmc.multiblocked.api.gui.tester.ControllerScriptWidget;
import com.lowdragmc.multiblocked.api.pattern.FactoryBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.Predicates;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.client.renderer.impl.MBDIModelRenderer;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class ControllerTileTesterEntity extends ControllerTileEntity {
    public final static ControllerDefinition DEFAULT_DEFINITION = new ControllerDefinition(new ResourceLocation("multiblocked:controller_tester"), ControllerTileTesterEntity.class);

    public ControllerTileTesterEntity(ControllerDefinition definition, BlockPos pos, BlockState state) {
        super(definition, pos, state);
    }

    @Override
    public boolean checkPattern() {
        return getDefinition() != DEFAULT_DEFINITION && super.checkPattern();
    }

    @Override
    public void setLevel(@Nonnull Level world) {
        MultiblockWorldSavedData mwsd = MultiblockWorldSavedData.getOrCreate(world);
        if (mwsd.mapping.containsKey(getBlockPos())) {
            mwsd.removeMapping(mwsd.mapping.get(getBlockPos()));
        }
        super.setLevel(world);
    }

    public void setDefinition(ControllerDefinition definition) {
        if (level != null) {
            MultiblockWorldSavedData mwsd = MultiblockWorldSavedData.getOrCreate(level);
            state = null;
            if (worldPosition != null && mwsd.mapping.containsKey(worldPosition)) {
                mwsd.removeMapping(mwsd.mapping.get(worldPosition));
            }
            if (definition == null) {
                this.definition = DEFAULT_DEFINITION;
            } else if (definition != DEFAULT_DEFINITION) {
                this.definition = definition;
                if (isRemote()) {
                    scheduleChunkForRenderUpdate();
                } else {
                    notifyBlockUpdate();
                    if (needAlwaysUpdate()) {
                        MultiblockWorldSavedData.getOrCreate(level).addLoading(this);
                    }
                }
            }
            initTrait();
        }
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        recipeLogic = new RecipeLogic(this);
    }

    @Override
    public ModularUI createComponentUI(Player entityPlayer) {
        if (Multiblocked.isClient() && Multiblocked.isSinglePlayer()) {
            TabContainer tabContainer = new TabContainer(0, 0, 200, 232);
            new ControllerScriptWidget(this, tabContainer);
            if (getDefinition() != DEFAULT_DEFINITION) {
                if (!traits.isEmpty()) initTraitUI(tabContainer, entityPlayer);
                if (isFormed()) {
                    new RecipePage(this, tabContainer);
                    new IOPageWidget(this, tabContainer);
                } else {
                    new StructurePageWidget(this.definition, tabContainer);
                }
            }
            return new ModularUI(196, 256, this, entityPlayer).widget(tabContainer);
        }
        return null;
    }

    public static void registerTestController() {
        DEFAULT_DEFINITION.getBaseStatus().setRenderer(new MBDIModelRenderer(new ResourceLocation(Multiblocked.MODID,"block/controller_tester")));
        DEFAULT_DEFINITION.properties.isOpaque = false;
        DEFAULT_DEFINITION.setBasePattern(FactoryBlockPattern.start()
                .aisle("@")
                .where('@', Predicates.component(DEFAULT_DEFINITION))
                .build());
        MbdComponents.registerComponent(DEFAULT_DEFINITION);
    }
}
