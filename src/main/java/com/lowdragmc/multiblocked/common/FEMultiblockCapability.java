package com.lowdragmc.multiblocked.common;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapCapabilityProxy;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FEMultiblockCapability extends MultiblockCapability<Integer> {
    public static final FEMultiblockCapability CAP = new FEMultiblockCapability();

    private FEMultiblockCapability() {
        super("forge_energy", new Color(0xCB0000).getRGB());
    }

    @Override
    public Integer defaultContent() {
        return 500;
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull TileEntity tileEntity) {
        return !getCapability(CapabilityEnergy.ENERGY, tileEntity).isEmpty();
    }

    @Override
    public Integer copyInner(Integer content) {
        return content;
    }

    @Override
    public FECapabilityProxy createProxy(@Nonnull IO io, @Nonnull TileEntity tileEntity) {
        return new FECapabilityProxy(tileEntity);
    }

//    @Override
//    public ContentWidget<? super Integer> createContentWidget() {
//        return new NumberContentWidget().setContentTexture(new TextTexture("FE", color)).setUnit("FE");
//    }
//
//    @Override
//    public boolean hasTrait() {
//        return true;
//    }
//
//    @Override
//    public CapabilityTrait createTrait() {
//        return new FECapabilityTrait();
//    }

    @Override
    public BlockInfo[] getCandidates() {
        List<BlockInfo> list = new ArrayList<>();
        TrackedDummyWorld dummyWorld = new TrackedDummyWorld();
        for (Block block : ForgeRegistries.BLOCKS.getValues()) {
            if (block.getRegistryName() != null) {
                String path = block.getRegistryName().getPath();
                if (path.contains("energy") || path.contains("rf")) {
                    try {
                        if (block.hasTileEntity(block.defaultBlockState())) {
                            TileEntity tileEntity = block.createTileEntity(block.defaultBlockState(), dummyWorld);
                            if (tileEntity != null && isBlockHasCapability(IO.BOTH, tileEntity)) {
                                list.add(new BlockInfo(block.defaultBlockState(), tileEntity));
                            }
                        }
                    } catch (Throwable ignored) { }
                }
            }
        }
        return list.toArray(new BlockInfo[0]);
    }

    @Override
    public Integer deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return jsonElement.getAsInt();
    }

    @Override
    public JsonElement serialize(Integer integer, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(integer);
    }

    public static class FECapabilityProxy extends CapCapabilityProxy<IEnergyStorage, Integer> {

        public FECapabilityProxy(TileEntity tileEntity) {
            super(FEMultiblockCapability.CAP, tileEntity, CapabilityEnergy.ENERGY);
        }

        @Override
        protected List<Integer> handleRecipeInner(IO io, Recipe recipe, List<Integer> left, boolean simulate) {
            IEnergyStorage capability = getCapability();
            if (capability == null) return left;
            int sum = left.stream().reduce(0, Integer::sum);
            if (io == IO.IN) {
                sum = sum - capability.extractEnergy(sum, simulate);
            } else if (io == IO.OUT) {
                sum = sum - capability.receiveEnergy(sum, simulate);
            }
            return sum <= 0 ? null : Collections.singletonList(sum);
        }

        int stored = -1;
        boolean canExtract = false;
        boolean canReceive = false;

        @Override
        protected boolean hasInnerChanged() {
            IEnergyStorage capability = getCapability();
            if (capability == null) return false;
            if (stored == capability.getEnergyStored() && canExtract == capability.canExtract() && canReceive == capability.canReceive()) {
                return false;
            }
            canExtract = capability.canExtract();
            canReceive = capability.canReceive();
            stored = capability.getEnergyStored();
            return true;
        }
    }
}
