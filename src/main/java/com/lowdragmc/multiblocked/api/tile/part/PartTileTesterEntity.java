package com.lowdragmc.multiblocked.api.tile.part;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.PartDefinition;
import com.lowdragmc.multiblocked.api.gui.tester.PartScriptWidget;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.client.renderer.impl.MBDIModelRenderer;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockRayTraceResult;

public class PartTileTesterEntity extends PartTileEntity<PartDefinition> {
    public final static PartDefinition DEFAULT_DEFINITION = new PartDefinition(new ResourceLocation("multiblocked:part_tester"), PartTileTesterEntity::new);

    public PartTileTesterEntity(PartDefinition definition) {
        super(definition);
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
    public ActionResultType use(PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        super.use(player, hand, hit);
        return ActionResultType.SUCCESS;
    }

    @Override
    public ModularUI createComponentUI(PlayerEntity entityPlayer) {
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
        DEFAULT_DEFINITION.baseRenderer = new MBDIModelRenderer(new ResourceLocation(Multiblocked.MODID,"block/part_tester"));
        DEFAULT_DEFINITION.properties.isOpaque = false;
        MbdComponents.registerComponent(DEFAULT_DEFINITION);
    }
}
