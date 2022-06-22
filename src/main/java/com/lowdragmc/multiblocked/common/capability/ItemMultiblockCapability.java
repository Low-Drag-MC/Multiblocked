package com.lowdragmc.multiblocked.common.capability;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapCapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.ingredient.SizedIngredient;
import com.lowdragmc.multiblocked.api.recipe.serde.content.SerializerIngredient;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.common.capability.trait.ItemCapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.widget.ItemsContentWidget;
import com.lowdragmc.multiblocked.core.mixins.NBTIngredientMixin;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.crafting.NBTIngredient;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;

public class ItemMultiblockCapability extends MultiblockCapability<Ingredient> {
    public static final ItemMultiblockCapability CAP = new ItemMultiblockCapability();

    private ItemMultiblockCapability() {
        super("item", 0xFFD96106, SerializerIngredient.INSTANCE);
    }

    @Override
    public Ingredient defaultContent() {
        return new SizedIngredient(Ingredient.of(Items.IRON_INGOT), 1);
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull
            BlockEntity tileEntity) {
        return !getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, tileEntity).isEmpty();
    }

    @Override
    public Ingredient copyInner(Ingredient content) {
        //Use network serialization to reduce overhead
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        content.toNetwork(buf);
        return Ingredient.fromNetwork(buf);
    }

    @Override
    public ItemCapabilityProxy createProxy(@Nonnull IO io, @Nonnull
            BlockEntity tileEntity) {
        return new ItemCapabilityProxy(tileEntity);
    }

    @Override
    public boolean hasTrait() {
        return true;
    }

    public CapabilityTrait createTrait() {
        return new ItemCapabilityTrait();
    }

    @Override
    public ContentWidget<? super Ingredient> createContentWidget() {
        return new ItemsContentWidget();
    }

    @Override
    public BlockInfo[] getCandidates() {
        return new BlockInfo[]{
                BlockInfo.fromBlockState(Blocks.CHEST.defaultBlockState()),
                BlockInfo.fromBlockState(Blocks.WHITE_SHULKER_BOX.defaultBlockState()),
                BlockInfo.fromBlockState(Blocks.TRAPPED_CHEST.defaultBlockState()),
                BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "item_input"))),
                BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "item_output")))
        };
    }

    public static class ItemCapabilityProxy extends CapCapabilityProxy<IItemHandler, Ingredient> {

        public ItemCapabilityProxy(BlockEntity tileEntity) {
            super(ItemMultiblockCapability.CAP, tileEntity, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
        }

        @Override
        protected List<Ingredient> handleRecipeInner(IO io, Recipe recipe, List<Ingredient> left, boolean simulate) {
            IItemHandler capability = getCapability();
            if (capability == null) return left;
            Iterator<Ingredient> iterator = left.iterator();
            if (io == IO.IN) {
                while (iterator.hasNext()) {
                    Ingredient ingredient = iterator.next();
                    SLOT_LOOKUP:
                    for (int i = 0; i < capability.getSlots(); i++) {
                        ItemStack itemStack = capability.getStackInSlot(i);
                        //Does not look like a good implementation, but I think it's at least equal to vanilla Ingredient::test
                        if (!ingredient.test(itemStack))
                            continue;
                        ItemStack[] ingredientStacks = ingredient.getItems();
                        for (ItemStack ingredientStack : ingredientStacks) {
                            if (ingredientStack.is(itemStack.getItem())) {
                                ItemStack extracted = capability.extractItem(i, ingredientStack.getCount(), simulate);
                                if (extracted.getCount() >= ingredientStack.getCount()) {
                                    iterator.remove();
                                    break SLOT_LOOKUP;
                                }
                            }
                        }
                    }
                }
            } else if (io == IO.OUT) {
                while (iterator.hasNext()) {
                    Ingredient ingredient = iterator.next();
                    ItemStack output = ingredient instanceof NBTIngredient nbtIngredient ? ((NBTIngredientMixin) nbtIngredient).getStack().copy() : ingredient.getItems()[0];
                    for (int i = 0; i < capability.getSlots(); i++) {
                        output = capability.insertItem(i, output.copy(), simulate);
                        if (output.isEmpty()) break;
                    }
                    if (output.isEmpty()) iterator.remove();
                }
            }
            return left.isEmpty() ? null : left;
        }

        ItemStack[] lastStacks = new ItemStack[0];
        int[] limits = new int[0];

        @Override
        protected boolean hasInnerChanged() {
            IItemHandler capability = getCapability();
            if (capability == null) return false;
            boolean same = true;
            if (lastStacks.length == capability.getSlots()) {
                for (int i = 0; i < capability.getSlots(); i++) {
                    ItemStack content = capability.getStackInSlot(i);
                    ItemStack lastContent = lastStacks[i];
                    if (lastContent == null) {
                        same = false;
                        break;
                    } else if (lastContent.isEmpty() && content.isEmpty()) {

                    } else if (!content.equals(lastContent, true)) {
                        same = false;
                        break;
                    }
                    int cap = capability.getSlotLimit(i);
                    int lastCap = limits[i];
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
            lastStacks = new ItemStack[capability.getSlots()];
            limits = new int[capability.getSlots()];
            for (int i = 0; i < capability.getSlots(); i++) {
                lastStacks[i] = capability.getStackInSlot(i).copy();
                limits[i] = capability.getSlotLimit(i);
            }
            return true;
        }
    }
}
