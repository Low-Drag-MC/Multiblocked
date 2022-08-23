package com.lowdragmc.multiblocked.api.tile.part;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.gui.tester.PartScriptWidget;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.client.renderer.impl.MBDIModelRenderer;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class PartTileTesterEntity extends PartTileEntity<PartDefinition> {
    public final static PartDefinition DEFAULT_DEFINITION = new PartDefinition(new ResourceLocation("multiblocked:part_tester"), PartTileTesterEntity.class);

    public PartTileTesterEntity(PartDefinition definition, BlockPos pos, BlockState state) {
        super(definition, pos, state);
    }

    public void setDefinition(PartDefinition definition) {
        if (level != null) {
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
    public InteractionResult use(Player player, InteractionHand hand, BlockHitResult hit) {
        super.use(player, hand, hit);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ModularUI createComponentUI(Player entityPlayer) {
        if (Multiblocked.isClient() && Multiblocked.isSinglePlayer()) {
            TabContainer tabContainer = new TabContainer(0, 0, 200, 232);
            new PartScriptWidget(this, tabContainer);
            if (getDefinition() != DEFAULT_DEFINITION) {
                if (!traits.isEmpty()) initTraitUI(tabContainer, entityPlayer);
            }
            return new ModularUI(196, 256, this, entityPlayer)
                    .widget(tabContainer);
        }
        return null;
    }


    public static void registerTestPart() {
        DEFAULT_DEFINITION.getBaseStatus().setRenderer(new MBDIModelRenderer(new ResourceLocation(Multiblocked.MODID,"block/part_tester")));
        DEFAULT_DEFINITION.properties.isOpaque = false;
        MbdComponents.registerComponent(DEFAULT_DEFINITION);
    }
}
