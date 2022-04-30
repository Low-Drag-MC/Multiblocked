package com.lowdragmc.multiblocked.common;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapCapabilityProxy;
import com.lowdragmc.lowdraglib.json.FluidStackTypeAdapter;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FluidMultiblockCapability extends MultiblockCapability<FluidStack> {
    public static final FluidMultiblockCapability CAP = new FluidMultiblockCapability();

    private  FluidMultiblockCapability() {
        super("fluid", new Color(0x3C70EE).getRGB());
    }

    @Override
    public FluidStack defaultContent() {
        return new FluidStack(ForgeMod.MILK.get(), 1000);
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull TileEntity tileEntity) {
        return !getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, tileEntity).isEmpty();
    }

    @Override
    public FluidStack copyInner(FluidStack content) {
        return content.copy();
    }


    @Override
    public FluidCapabilityProxy createProxy(@Nonnull IO io, @Nonnull TileEntity tileEntity) {
        return new FluidCapabilityProxy(tileEntity);
    }

//    @Override
//    public ContentWidget<? super FluidStack> createContentWidget() {
//        return new FluidContentWidget();
//    }
//
//    @Override
//    public boolean hasTrait() {
//        return true;
//    }
//
//    @Override
//    public CapabilityTrait createTrait() {
//        return new FluidCapabilityTrait();
//    }

    @Override
    public BlockInfo[] getCandidates() {
        List<BlockInfo> list = new ArrayList<>();
        TrackedDummyWorld dummyWorld = new TrackedDummyWorld();
        for (Block block : ForgeRegistries.BLOCKS.getValues()) {
            if (block.getRegistryName() != null) {
                String path = block.getRegistryName().getPath();
                if (path.contains("tank") || path.contains("fluid") || path.contains("liquid")) {
                    try {
                        if (block.hasTileEntity(block.defaultBlockState())) {
                            TileEntity tileEntity = block.createTileEntity(block.defaultBlockState(), dummyWorld);
                            if (tileEntity != null  && isBlockHasCapability(IO.BOTH, tileEntity)) {
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
    public FluidStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return FluidStackTypeAdapter.INSTANCE.deserialize(jsonElement, type, jsonDeserializationContext);
    }

    @Override
    public JsonElement serialize(FluidStack fluidStack, Type type, JsonSerializationContext jsonSerializationContext) {
        return FluidStackTypeAdapter.INSTANCE.serialize(fluidStack, type, jsonSerializationContext);
    }

    public static class FluidCapabilityProxy extends CapCapabilityProxy<IFluidHandler, FluidStack> {

        public FluidCapabilityProxy(TileEntity tileEntity) {
            super(FluidMultiblockCapability.CAP, tileEntity, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
        }

        @Override
        protected List<FluidStack> handleRecipeInner(IO io, Recipe recipe, List<FluidStack> left, boolean simulate) {
            IFluidHandler capability = getCapability();
            if (capability == null) return left;
            Iterator<FluidStack> iterator = left.iterator();
            if (io == IO.IN) {
                while (iterator.hasNext()) {
                    FluidStack fluidStack = iterator.next();
                    boolean found = false;
                    for (int i = 0; i < capability.getTanks(); i++) {
                        FluidStack stored = capability.getFluidInTank(i);
                        if (!stored.isFluidEqual(fluidStack)) {
                            continue;
                        }
                        found = true;
                    }
                    if (!found) continue;
                    FluidStack drained = capability.drain(fluidStack.copy(), simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
                    fluidStack.setAmount(fluidStack.getAmount() - drained.getAmount());
                    if (fluidStack.getAmount() <= 0) {
                        iterator.remove();
                    }
                }
            } else if (io == IO.OUT){
                while (iterator.hasNext()) {
                    FluidStack fluidStack = iterator.next();
                    int filled = capability.fill(fluidStack.copy(), simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
                    fluidStack.setAmount(fluidStack.getAmount() - filled);
                    if (fluidStack.getAmount() <= 0) {
                        iterator.remove();
                    }
                }
            }
            return left.isEmpty() ? null : left;
        }

        FluidStack[] lastContents = new FluidStack[0];
        int[] lastCaps = new int[0];

        @Override
        protected boolean hasInnerChanged() {
            IFluidHandler capability = getCapability();
            if (capability == null) return false;
            boolean same = true;
            if (capability.getTanks() == lastContents.length) {
                for (int i = 0; i < capability.getTanks(); i++) {
                    FluidStack content = capability.getFluidInTank(i);
                    FluidStack lastContent = lastContents[i];
                    if (lastContent == null) {
                        same = false;
                        break;
                    } else if (!content.isFluidEqual(lastContent) || content.getAmount() != lastContent.getAmount()) {
                        same = false;
                        break;
                    }
                    int cap = capability.getTankCapacity(i);
                    int lastCap = lastCaps[i];
                    if (cap != lastCap) {
                        same = false;
                        break;
                    }
                }
            } else {
                same = false;
            }

            if (same) {
                return false;
            }
            lastContents = new FluidStack[capability.getTanks()];
            lastCaps = new int[lastContents.length];
            for (int i = 0; i < lastContents.length; i++) {
                FluidStack content = capability.getFluidInTank(i);
                lastContents[i] = content.copy();
                lastCaps[i] = capability.getTankCapacity(i);
            }
            return true;
        }
    }
}
