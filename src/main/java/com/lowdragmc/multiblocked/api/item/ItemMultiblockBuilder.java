package com.lowdragmc.multiblocked.api.item;
;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.pattern.BlockPattern;
import com.lowdragmc.multiblocked.api.pattern.JsonBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
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
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player != null) {
            if (!player.level.isClientSide) {
                TileEntity tileEntity = player.level.getBlockEntity(context.getClickedPos());
                ItemStack hold = player.getItemInHand(context.getHand());
                if (isItemMultiblockBuilder(hold) && tileEntity instanceof ControllerTileEntity) {
                    if (isRaw(hold)) {
                        BlockPattern pattern = ((ControllerTileEntity) tileEntity).getPattern();
                        if (pattern != null) {
                            pattern.autoBuild(player, new MultiblockState(player.level, context.getClickedPos()));
                        }
                        return ActionResultType.SUCCESS;
                    } else {
                        String json = hold.getOrCreateTagElement("pattern").getString("json");
                        String controller = hold.getOrCreateTagElement("pattern").getString("controller");
                        if (!json.isEmpty() && !controller.isEmpty()) {
                            if (controller.equals(((ControllerTileEntity) tileEntity).getDefinition().location.toString())) {
                                JsonBlockPattern jsonBlockPattern = Multiblocked.GSON.fromJson(json, JsonBlockPattern.class);
                                jsonBlockPattern.build().autoBuild(player, new MultiblockState(player.level, context.getClickedPos()));
                                return ActionResultType.SUCCESS;
                            }
                        }
                    }
                }
            }
        }
        return ActionResultType.PASS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (isItemMultiblockBuilder(stack)) {
            if (isRaw(stack)) {
                tooltip.add(new StringTextComponent("auto build"));
            } else {
                ResourceLocation location = new ResourceLocation(stack.getOrCreateTagElement("pattern").getString("controller"));
                tooltip.add(new StringTextComponent("pattern build"));
                tooltip.add(new StringTextComponent(String.format("Controller: %s", I18n.get(location.getPath() + ".name"))));
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
