package com.lowdragmc.multiblocked.api.item;
;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.pattern.BlockPattern;
import com.lowdragmc.multiblocked.api.pattern.JsonBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.api.tile.IControllerComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class ItemMultiblockBuilder extends Item {

    public ItemMultiblockBuilder() {
        super(new Item.Properties().tab(Multiblocked.TAB_ITEMS));
        setRegistryName(Multiblocked.MODID, "multiblock_builder");
    }

    public static boolean isItemMultiblockBuilder(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemMultiblockBuilder;
    }

    public static boolean isRaw(ItemStack stack) {
        return stack.getDamageValue() == 0;
    }

    public static boolean setPattern(ItemStack stack) {
        if (isItemMultiblockBuilder(stack)) {
            stack.setDamageValue(1);
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null) {
            if (!player.level.isClientSide) {
                BlockEntity tileEntity = player.level.getBlockEntity(context.getClickedPos());
                ItemStack hold = player.getItemInHand(context.getHand());
                if (isItemMultiblockBuilder(hold) && tileEntity instanceof IControllerComponent) {
                    if (isRaw(hold)) {
                        BlockPattern pattern = ((IControllerComponent) tileEntity).getPattern();
                        if (pattern != null) {
                            pattern.autoBuild(player, new MultiblockState(player.level, context.getClickedPos()));
                        }
                        return InteractionResult.SUCCESS;
                    } else {
                        String json = hold.getOrCreateTagElement("pattern").getString("json");
                        String controller = hold.getOrCreateTagElement("pattern").getString("controller");
                        if (!json.isEmpty() && !controller.isEmpty()) {
                            if (controller.equals(((IControllerComponent) tileEntity).getDefinition().location.toString())) {
                                JsonBlockPattern jsonBlockPattern = Multiblocked.GSON.fromJson(json, JsonBlockPattern.class);
                                jsonBlockPattern.build().autoBuild(player, new MultiblockState(player.level, context.getClickedPos()));
                                return InteractionResult.SUCCESS;
                            }
                        }
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (isItemMultiblockBuilder(stack)) {
            if (isRaw(stack)) {
                tooltip.add(new TextComponent("auto build"));
            } else {
                ResourceLocation location = new ResourceLocation(stack.getOrCreateTagElement("pattern").getString("controller"));
                tooltip.add(new TextComponent("pattern build"));
                tooltip.add(new TextComponent(String.format("Controller: %s", I18n.get(location.getPath() + ".name"))));
            }
        }
    }

//    public static class BuilderRecipeLogic extends ShapelessRecipes {
//        private static final ItemStack resultStack;
//        private static final Ingredient builder;
//        private static final Ingredient blueprint;
//
//        static {
//            resultStack = new ItemStack(MbdItems.BUILDER);
//            ItemMultiblockBuilder.setPattern(resultStack);
//            ItemStack stack = new ItemStack(MbdItems.BLUEPRINT);
//            ItemBlueprint.setPattern(stack);
//            blueprint = Ingredient.fromStacks(stack);
//            stack = new ItemStack(MbdItems.BUILDER);
//            ItemMultiblockBuilder.setPattern(stack);
//            builder = Ingredient.fromStacks(new ItemStack(MbdItems.BUILDER), stack);
//        }
//
//        public BuilderRecipeLogic() {
//            super("builder", resultStack, NonNullList.from(Ingredient.EMPTY, builder, blueprint));
//        }
//
//        @Override
//        public boolean matches(@Nonnull InventoryCrafting inv, @Nonnull World worldIn) {
//            ItemStack a = null;
//            ItemStack b = null;
//            for (int i = 0; i < inv.getSizeInventory(); i++) {
//                ItemStack itemStack = inv.getStackInSlot(i);
//                if (itemStack.isEmpty()) continue;
//                if (a == null) {
//                    a = itemStack;
//                } else if (b == null) {
//                    b = itemStack;
//                } else {
//                    return false;
//                }
//            }
//            if (a== null || b == null) return false;
//            return (builder.test(a) && blueprint.test(b)) || (builder.test(b) && blueprint.test(a));
//        }
//
//        @Nonnull
//        @Override
//        public ItemStack getCraftingResult(@Nonnull InventoryCrafting inv) {
//            ItemStack a = null;
//            ItemStack b = null;
//            for (int i = 0; i < inv.getSizeInventory(); i++) {
//                ItemStack itemStack = inv.getStackInSlot(i);
//                if (itemStack.isEmpty()) continue;
//                if (a == null) {
//                    a = itemStack;
//                } else if (b == null) {
//                    b = itemStack;
//                } else {
//                    return ItemStack.EMPTY;
//                }
//            }
//            if (a== null || b == null) return ItemStack.EMPTY;
//            ItemStack builder = new ItemStack(MbdItems.BUILDER);
//            ItemMultiblockBuilder.setPattern(builder);
//            if (ItemBlueprint.isItemBlueprint(a) && !ItemBlueprint.isRaw(a)) {
//                builder.getOrCreateSubCompound("pattern").setString("json", a.getOrCreateSubCompound("pattern").getString("json"));
//                builder.getOrCreateSubCompound("pattern").setString("controller", a.getOrCreateSubCompound("pattern").getString("controller"));
//            } else if (ItemBlueprint.isItemBlueprint(b) && !ItemBlueprint.isRaw(b)) {
//                builder.getOrCreateSubCompound("pattern").setString("json", b.getOrCreateSubCompound("pattern").getString("json"));
//                builder.getOrCreateSubCompound("pattern").setString("controller", b.getOrCreateSubCompound("pattern").getString("controller"));
//            }
//            return builder;
//        }
//
//        @Nonnull
//        @Override
//        public NonNullList<ItemStack> getRemainingItems(@Nonnull InventoryCrafting inv) {
//            NonNullList<ItemStack> ret = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
//
//            ItemStack blueprint = null;
//            ItemStack builder = null;
//            int index = 0;
//            for (int i = 0; i < inv.getSizeInventory(); i++) {
//                ItemStack itemStack = inv.getStackInSlot(i).copy();
//                if (ItemMultiblockBuilder.isItemMultiblockBuilder(itemStack) && !ItemMultiblockBuilder.isRaw(itemStack)) {
//                    index = i;
//                    builder = itemStack;
//                } else if (ItemBlueprint.isItemBlueprint(itemStack)) {
//                    blueprint = itemStack;
//                }
//            }
//
//            if (builder != null && blueprint != null) {
//                ret.set(index, blueprint);
//                blueprint.getOrCreateSubCompound("pattern").setString("json", builder.getOrCreateSubCompound("pattern").getString("json"));
//                blueprint.getOrCreateSubCompound("pattern").setString("controller", builder.getOrCreateSubCompound("pattern").getString("controller"));
//            }
//
//            return ret;
//        }
//
//        @Override
//        public boolean canFit(int width, int height) {
//            return width * height >= 2;
//        }
//
//        @Override
//        public boolean isDynamic() {
//            return true;
//        }
//
//    }
}
