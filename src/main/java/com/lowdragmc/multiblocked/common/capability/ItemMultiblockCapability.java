package com.lowdragmc.multiblocked.common.capability;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapCapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.kubejs.MultiblockedJSPlugin;
import com.lowdragmc.multiblocked.api.recipe.ItemsIngredient;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.common.capability.trait.ItemCapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.widget.ItemsContentWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

public class ItemMultiblockCapability extends MultiblockCapability<ItemsIngredient> {
    public static final ItemMultiblockCapability CAP = new ItemMultiblockCapability();

    private ItemMultiblockCapability() {
        super("item", 0xFFD96106);
    }

    @Override
    public ItemsIngredient defaultContent() {
        return new ItemsIngredient(Ingredient.of(Items.IRON_INGOT), 1);
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull
    BlockEntity tileEntity) {
        return !getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, tileEntity).isEmpty();
    }

    @Override
    public ItemsIngredient copyInner(ItemsIngredient content) {
        return content.copy();
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
    public ContentWidget<? super ItemsIngredient> createContentWidget() {
        return new ItemsContentWidget();
    }

    @Override
    public BlockInfo[] getCandidates() {
        return new BlockInfo[] {
                BlockInfo.fromBlockState(Blocks.CHEST.defaultBlockState()),
                BlockInfo.fromBlockState(Blocks.WHITE_SHULKER_BOX.defaultBlockState()),
                BlockInfo.fromBlockState(Blocks.TRAPPED_CHEST.defaultBlockState()),
                BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "item_input"))),
                BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "item_output")))
        };
    }

    @Override
    public ItemsIngredient of(Object o) {
        if (o instanceof ItemsIngredient) {
            return ((ItemsIngredient) o).copy();
        }
        return new ItemsIngredient(ItemStack.EMPTY);
    }

    @Override
    public ItemsIngredient deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (jsonObject.has("tag")) {
            return new ItemsIngredient(jsonObject.get("tag").getAsString(), jsonObject.get("amount").getAsInt());
        } else {
            return new ItemsIngredient(Ingredient.fromJson(jsonObject.get("matches")), jsonObject.get("amount").getAsInt());
        }
    }

    @Override
    public JsonElement serialize(ItemsIngredient itemsIngredient, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("amount", itemsIngredient.getAmount());
        if (itemsIngredient.isTag()) {
            jsonObject.addProperty("tag", itemsIngredient.getTag());
        } else {
            jsonObject.add("matches", itemsIngredient.ingredient.toJson());
        }
        return jsonObject;
    }

    public static class ItemCapabilityProxy extends CapCapabilityProxy<IItemHandler, ItemsIngredient> {

        public ItemCapabilityProxy(BlockEntity tileEntity) {
            super(ItemMultiblockCapability.CAP, tileEntity, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
        }

        @Override
        protected List<ItemsIngredient> handleRecipeInner(IO io, Recipe recipe, List<ItemsIngredient> left, boolean simulate) {
            IItemHandler capability = getCapability();
            if (capability == null) return left;
            Iterator<ItemsIngredient> iterator = left.iterator();
            if (io == IO.IN) {
                while (iterator.hasNext()) {
                    ItemsIngredient ingredient = iterator.next();
                    for (int i = 0; i < capability.getSlots(); i++) {
                        ItemStack itemStack = capability.getStackInSlot(i);
                        if (ingredient.ingredient.test(itemStack)) {
                            ItemStack extracted = capability.extractItem(i, ingredient.getAmount(), simulate);
                            ingredient.setAmount(ingredient.getAmount() - extracted.getCount());
                            if (ingredient.getAmount() <= 0) {
                                iterator.remove();
                                break;
                            }
                        }
                    }
                }
            } else if (io == IO.OUT){
                while (iterator.hasNext()) {
                    ItemsIngredient ingredient = iterator.next();
                    ItemStack output = ingredient.getOutputStack();
                    for (int i = 0; i < capability.getSlots(); i++) {
                        output = capability.insertItem(i, output.copy(), simulate);
                        if (output.isEmpty()) break;
                    }
                    if (output.isEmpty()) iterator.remove();
                    else ingredient.setAmount(output.getCount());
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
