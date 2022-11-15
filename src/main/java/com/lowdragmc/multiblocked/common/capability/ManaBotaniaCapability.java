package com.lowdragmc.multiblocked.common.capability;

import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapCapabilityProxy;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.serde.content.SerializerInteger;
import com.lowdragmc.multiblocked.common.capability.widget.NumberContentWidget;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.common.block.ModBlocks;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class ManaBotaniaCapability extends MultiblockCapability<Integer> {
    public static final ManaBotaniaCapability CAP = new ManaBotaniaCapability();

    private ManaBotaniaCapability() {
        super("bot_mana", 0xFF06D2D9, SerializerInteger.INSTANCE);
    }

    @Override
    public Integer defaultContent() {
        return 200;
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull BlockEntity tileEntity) {
        return !getCapability(BotaniaForgeCapabilities.MANA_RECEIVER, tileEntity).isEmpty();
    }

    @Override
    public Integer copyInner(Integer content) {
        return content;
    }

    @Override
    public ManaBotainaCapabilityProxy createProxy(@Nonnull IO io, @Nonnull
            BlockEntity tileEntity) {
        return new ManaBotainaCapabilityProxy(tileEntity);
    }

    @Override
    public ContentWidget<? super Integer> createContentWidget() {
        return new NumberContentWidget().setContentTexture(new TextTexture("MN", color)).setUnit("Mana");
    }

    @Override
    public BlockInfo[] getCandidates() {
        return new BlockInfo[]{

                BlockInfo.fromBlock(ModBlocks.manaPool),
                BlockInfo.fromBlock(ModBlocks.manaSpreader),
                BlockInfo.fromBlock(ModBlocks.manaVoid),
                BlockInfo.fromBlock(ModBlocks.terraPlate)
        };
    }

    public static class ManaBotainaCapabilityProxy extends CapCapabilityProxy<IManaReceiver, Integer> {

        public ManaBotainaCapabilityProxy(BlockEntity tileEntity) {
            super(ManaBotaniaCapability.CAP, tileEntity, BotaniaForgeCapabilities.MANA_RECEIVER);
        }

        @Override
        protected List<Integer> handleRecipeInner(IO io, Recipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
            IManaReceiver capability = getCapability(slotName);
            if (capability == null) return left;
            int sum = left.stream().reduce(0, Integer::sum);
            if (io == IO.IN) {
                int cost = Math.min(capability.getCurrentMana(), sum);
                if (!simulate) {
                    capability.receiveMana(-cost);
                }
                sum = sum - cost;
            } else if (io == IO.OUT) {
                if (capability.isFull()) {
                    return left;
                }
                if (!simulate) {
                    capability.receiveMana(sum);
                }
                return null;
            }
            return sum <= 0 ? null : Collections.singletonList(sum);
        }

        int lastMana = -1;

        @Override
        protected boolean hasInnerChanged() {
            IManaReceiver capability = getCapability(null);
            if (capability == null) return false;
            if (lastMana == capability.getCurrentMana()) return false;
            lastMana = capability.getCurrentMana();
            return true;
        }
    }
}
