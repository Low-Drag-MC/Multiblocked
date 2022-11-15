package com.lowdragmc.multiblocked.common.capability;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.MbdConfig;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.serde.content.SerializerInteger;
import com.lowdragmc.multiblocked.common.capability.widget.NumberContentWidget;
import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.List;

public class AuraMultiblockCapability extends MultiblockCapability<Integer> {

    public static final AuraMultiblockCapability CAP = new AuraMultiblockCapability();

    private AuraMultiblockCapability() {
        super("natures_aura", 0xFF95EF95, SerializerInteger.INSTANCE);
    }

    @Override
    public Integer defaultContent() {
        return 200;
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull BlockEntity tileEntity) {
        // The specific storage of aura should be in the chunk and not inside te
        return true;
    }

    @Override
    public Integer copyInner(Integer content) {
        return content;
    }

    @Override
    public CapabilityProxy<? extends Integer> createProxy(@Nonnull IO io, @Nonnull BlockEntity tileEntity) {
        return new ManaBotainaCapabilityProxy(tileEntity);
    }

    @Override
    public BlockInfo[] getCandidates() {
        return new BlockInfo[]{
                BlockInfo.fromBlock(Blocks.FURNACE),
                BlockInfo.fromBlock(Blocks.CHEST),
                BlockInfo.fromBlock(Blocks.HOPPER),
                BlockInfo.fromBlock(Blocks.ENDER_CHEST)
        };
    }

    @Override
    public Integer deserialize(JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return jsonElement.getAsInt();
    }

    @Override
    public JsonElement serialize(Integer integer, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(integer);
    }

    @Override
    public ContentWidget<? super Integer> createContentWidget() {
        return new NumberContentWidget().setContentTexture(new TextTexture("NA", color)).setUnit("NaturesAura");
    }

    public static class ManaBotainaCapabilityProxy extends CapabilityProxy<Integer> {

        public ManaBotainaCapabilityProxy(BlockEntity tileEntity) {
            super(AuraMultiblockCapability.CAP, tileEntity);
        }

        @Override
        protected List<Integer> handleRecipeInner(IO io, Recipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
            Level world = getTileEntity().getLevel();
            BlockPos pos = getTileEntity().getBlockPos();

            int sum = left.stream().reduce(0, Integer::sum);
            if (io == IO.IN) {
                if (!simulate) {
                    BlockPos spot = IAuraChunk.getHighestSpot(world, pos, MbdConfig.naturesAura.get(), pos);
                    IAuraChunk.getAuraChunk(world, spot).drainAura(pos, sum);
                }
            } else if (io == IO.OUT) {
                if (!simulate) {
                    BlockPos spot = IAuraChunk.getLowestSpot(world, pos, MbdConfig.naturesAura.get(), pos);
                    IAuraChunk.getAuraChunk(world, spot).storeAura(pos, sum);
                }
            }
            return null;
        }

        int lastMana = Integer.MIN_VALUE;

        @Override
        protected boolean hasInnerChanged() {
            int auraInArea = IAuraChunk.getAuraInArea(getTileEntity().getLevel(), getTileEntity().getBlockPos(), MbdConfig.naturesAura.get());
            if (lastMana == auraInArea) return false;
            lastMana = auraInArea;
            return true;
        }
    }
}
