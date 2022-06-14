package com.lowdragmc.multiblocked.common.capability;

import com.google.gson.*;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapCapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.common.capability.trait.ChemicalCapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.widget.ChemicalStackWidget;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.*;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class ChemicalMekanismCapability<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> extends MultiblockCapability<STACK> {
    public static final ChemicalMekanismCapability<Gas, GasStack> CAP_GAS =
            new ChemicalMekanismCapability<>("mek_gas", 0xff85909E,
                    () -> Capabilities.GAS_HANDLER_CAPABILITY,
                    MekanismAPI.EMPTY_GAS, MekanismAPI::gasRegistry,
                    GasStack::new,
                    ChemicalTankBuilder.GAS,
                    GasStack::readFromPacket,
                    () -> new BlockInfo[] {
                            BlockInfo.fromBlock(MekanismBlocks.BASIC_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.ADVANCED_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.CREATIVE_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.ELITE_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.ULTIMATE_CHEMICAL_TANK.getBlock()),
                    });

    public static final ChemicalMekanismCapability<InfuseType, InfusionStack> CAP_INFUSE =
            new ChemicalMekanismCapability<>("mek_infuse", 0xff5c9e90,
                    () -> Capabilities.INFUSION_HANDLER_CAPABILITY,
                    MekanismAPI.EMPTY_INFUSE_TYPE, MekanismAPI::infuseTypeRegistry,
                    InfusionStack::new,
                    ChemicalTankBuilder.INFUSION,
                    InfusionStack::readFromPacket,
                    () -> new BlockInfo[] {
                            BlockInfo.fromBlock(MekanismBlocks.BASIC_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.ADVANCED_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.CREATIVE_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.ELITE_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.ULTIMATE_CHEMICAL_TANK.getBlock()),
                    });

    public static final ChemicalMekanismCapability<Pigment, PigmentStack> CAP_PIGMENT =
            new ChemicalMekanismCapability<>("mek_pigment", 0xff9e2768,
                    () -> Capabilities.PIGMENT_HANDLER_CAPABILITY,
                    MekanismAPI.EMPTY_PIGMENT, MekanismAPI::pigmentRegistry,
                    PigmentStack::new,
                    ChemicalTankBuilder.PIGMENT,
                    PigmentStack::readFromPacket,
                    () -> new BlockInfo[] {
                            BlockInfo.fromBlock(MekanismBlocks.BASIC_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.ADVANCED_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.CREATIVE_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.ELITE_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.ULTIMATE_CHEMICAL_TANK.getBlock()),
                    });

    public static final ChemicalMekanismCapability<Slurry, SlurryStack> CAP_SLURRY =
            new ChemicalMekanismCapability<>("mek_slurry", 0xff9e5a11,
                    () -> Capabilities.SLURRY_HANDLER_CAPABILITY,
                    MekanismAPI.EMPTY_SLURRY, MekanismAPI::slurryRegistry,
                    SlurryStack::new,
                    ChemicalTankBuilder.SLURRY,
                    SlurryStack::readFromPacket,
                    () -> new BlockInfo[] {
                            BlockInfo.fromBlock(MekanismBlocks.BASIC_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.ADVANCED_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.CREATIVE_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.ELITE_CHEMICAL_TANK.getBlock()),
                            BlockInfo.fromBlock(MekanismBlocks.ULTIMATE_CHEMICAL_TANK.getBlock()),
                    });


    public final Supplier<Capability<IChemicalHandler<CHEMICAL, STACK>>> capability;
    public final CHEMICAL empty;
    public final Supplier<IForgeRegistry<CHEMICAL>> registry;
    public final BiFunction<CHEMICAL, Long, STACK> createStack;
    public final Supplier<BlockInfo[]> candidates;
    public final ChemicalTankBuilder<CHEMICAL, STACK, ? extends IChemicalTank<CHEMICAL, STACK>> tankBuilder;
    public final Function<FriendlyByteBuf, STACK> readFromBuffer;

    private ChemicalMekanismCapability(String key, int color,
                                       Supplier<Capability<? extends IChemicalHandler<CHEMICAL, STACK>>> capability,
                                       CHEMICAL empty,
                                       Supplier<IForgeRegistry<CHEMICAL>> registry,
                                       BiFunction<CHEMICAL, Long, STACK> createStack,
                                       ChemicalTankBuilder<CHEMICAL, STACK, ? extends IChemicalTank<CHEMICAL, STACK>> tankBuilder,
                                       Function<FriendlyByteBuf, STACK> readFromBuffer,
                                       Supplier<BlockInfo[]> candidates) {
        super(key, color);
        this.capability = (Supplier<Capability<IChemicalHandler<CHEMICAL, STACK>>>)(Object)capability;
        this.empty = empty;
        this.registry = registry;
        this.createStack = createStack;
        this.candidates = candidates;
        this.tankBuilder = tankBuilder;
        this.readFromBuffer = readFromBuffer;
    }

    @Override
    public STACK defaultContent() {
        return createStack.apply(registry.get().getValues().stream().findFirst().get(), 1000L);
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull
    BlockEntity tileEntity) {
        return !getCapability(Capabilities.GAS_HANDLER_CAPABILITY, tileEntity).isEmpty();
    }

    @Override
    public STACK copyInner(STACK content) {
        return (STACK) content.copy();
    }

    @Override
    public ChemicalMekanismCapabilityProxy<CHEMICAL, STACK> createProxy(@Nonnull IO io, @Nonnull
    BlockEntity tileEntity) {
        return new ChemicalMekanismCapabilityProxy<>(this, tileEntity);
    }

    @Override
    public boolean hasTrait() {
        return true;
    }

    @Override
    public CapabilityTrait createTrait() {
        return new ChemicalCapabilityTrait<>(this, tankBuilder);
    }

    @Override
    public ContentWidget<? super STACK> createContentWidget() {
        return new ChemicalStackWidget<>(this);
    }

    @Override
    public BlockInfo[] getCandidates() {
        return candidates.get();
    }

    @Override
    public STACK deserialize(JsonElement jsonElement, Type jsonType, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        ResourceLocation
                type = new ResourceLocation(jsonElement.getAsJsonObject().get("type").getAsString());
        long amount = jsonElement.getAsJsonObject().get("amount").getAsLong();
        CHEMICAL chemical = ChemicalUtils.readChemicalFromRegistry(type, empty, registry.get());
        return createStack.apply(chemical, amount);
    }

    @Override
    public JsonElement serialize(STACK chemicalStack, Type jsonType, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("type", chemicalStack.getType().getRegistryName().toString());
        jsonObj.addProperty("amount", chemicalStack.getAmount());
        return jsonObj;
    }

    public STACK of(Object o) {
        if (o instanceof ChemicalStack<?> && ((ChemicalStack<?>) o).getType().getClass() == empty.getClass()) {
            return (STACK) ((ChemicalStack<?>) o).copy();
        } else if (o instanceof CharSequence) {
            String s = o.toString();
            if (!s.isEmpty() && !s.equals("-") && !s.equals("empty") && !s.equals("minecraft:empty")) {
                String[] s1 = s.split(" ", 2);
                CHEMICAL chemical = registry.get().getValue(new ResourceLocation(s1[0]));
                long amount = s1.length == 2 ? NumberUtils.toLong(s1[1], 1) : 1;
                return createStack.apply(chemical, amount);
            }
        }
        return (STACK) empty.getStack(0);
    }

    public static class ChemicalMekanismCapabilityProxy<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> extends CapCapabilityProxy<IChemicalHandler<CHEMICAL, STACK>, STACK> {

        public ChemicalMekanismCapabilityProxy(ChemicalMekanismCapability<CHEMICAL, STACK> cap, BlockEntity tileEntity) {
            super(cap, tileEntity, cap.capability.get());
        }

        @Override
        protected List<STACK> handleRecipeInner(IO io, Recipe recipe, List<STACK> left, boolean simulate) {
            IChemicalHandler<CHEMICAL, STACK> capability = getCapability();
            if (capability == null) return left;
            Iterator<STACK> iterator = left.iterator();
            if (io == IO.IN) {
                while (iterator.hasNext()) {
                    STACK chemicalStack = iterator.next();
                    if (chemicalStack.isEmpty()) {
                        iterator.remove();
                        continue;
                    }
                    STACK extracted = capability.extractChemical((STACK) chemicalStack.copy(), simulate ? Action.SIMULATE : Action.EXECUTE);
                    chemicalStack.setAmount(chemicalStack.getAmount() - extracted.getAmount());
                    if (chemicalStack.isEmpty()) {
                        iterator.remove();
                    }
                }
            } else if (io == IO.OUT){
                while (iterator.hasNext()) {
                    STACK chemicalStack = iterator.next();
                    if (chemicalStack.isEmpty()) {
                        iterator.remove();
                        continue;
                    }
                    STACK leftStack = capability.insertChemical((STACK) chemicalStack.copy(), simulate ? Action.SIMULATE : Action.EXECUTE);
                    chemicalStack.setAmount(leftStack.getAmount());
                    if (chemicalStack.isEmpty()) {
                        iterator.remove();
                    }
                }
            }
            return left.isEmpty() ? null : left;
        }

        List<STACK> lastStacks = new ArrayList<>(0);
        long[] lastCapability = new long[0];

        @Override
        protected boolean hasInnerChanged() {
            IChemicalHandler<CHEMICAL, STACK> capability = getCapability();
            if (capability == null) return false;
            boolean same = true;
            if (lastStacks.size() == capability.getTanks()) {
                for (int i = 0; i < capability.getTanks(); i++) {
                    STACK content = capability.getChemicalInTank(i);
                    STACK lastContent = lastStacks.get(i);
                    if (lastContent == null) {
                        same = false;
                        break;
                    } else if (!content.isStackIdentical(lastContent)) {
                        same = false;
                        break;
                    }
                    long cap = capability.getTankCapacity(i);
                    long lastCap = lastCapability[i];
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
            lastStacks = new ArrayList<>(capability.getTanks());
            lastCapability = new long[capability.getTanks()];
            for (int i = 0; i < capability.getTanks(); i++) {
                STACK gas = capability.getChemicalInTank(i);
                lastStacks.add((STACK) gas.copy());
                lastCapability[i] = capability.getTankCapacity(i);
            }
            return true;
        }
    }
}
