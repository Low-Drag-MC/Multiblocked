package com.lowdragmc.multiblocked.api.item;

import com.lowdragmc.multiblocked.Multiblocked;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemBlueprint extends Item {

    public ItemBlueprint() {
        super(new Item.Properties().tab(Multiblocked.TAB_ITEMS));
        setRegistryName(Multiblocked.MODID, "blueprint");
    }

    public static boolean isItemBlueprint(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemBlueprint;
    }

    public static BlockPos[] getPos(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTagElement("blueprint");
        if (!tag.contains("minX")) return null;
        return new BlockPos[]{
                new BlockPos(tag.getInt("minX"), tag.getInt("minY"), tag.getInt("minZ")),
                new BlockPos(tag.getInt("maxX"), tag.getInt("maxY"), tag.getInt("maxZ"))
        };
    }


    public static void addPos(ItemStack stack, BlockPos pos) {
        CompoundNBT tag = stack.getOrCreateTagElement("blueprint");
        if (!tag.contains("minX") || tag.getInt("minX") > pos.getX()) {
            tag.putInt("minX", pos.getX());
        }
        if (!tag.contains("maxX") || tag.getInt("maxX") < pos.getX()) {
            tag.putInt("maxX", pos.getX());
        }

        if (!tag.contains("minY") || tag.getInt("minY") > pos.getY()) {
            tag.putInt("minY", pos.getY());
        }
        if (!tag.contains("maxY") || tag.getInt("maxY") < pos.getY()) {
            tag.putInt("maxY", pos.getY());
        }

        if (!tag.contains("minZ") || tag.getInt("minZ") > pos.getZ()) {
            tag.putInt("minZ", pos.getZ());
        }
        if (!tag.contains("maxZ") || tag.getInt("maxZ") < pos.getZ()) {
            tag.putInt("maxZ", pos.getZ());
        }
    }

    public static void removePos(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTagElement("blueprint");
        tag.remove("minX");
        tag.remove("maxX");
        tag.remove("minY");
        tag.remove("maxY");
        tag.remove("minZ");
        tag.remove("maxZ");
    }

    public static boolean isRaw(ItemStack stack) {
        return stack.getDamageValue() == 0;
    }

    public static boolean setRaw(ItemStack stack) {
        if (isItemBlueprint(stack)) {
            stack.setDamageValue(0);
            return true;
        }
        return false;
    }

    public static boolean setPattern(ItemStack stack) {
        if (isItemBlueprint(stack)) {
            stack.setDamageValue(1);
            return true;
        }
        return false;
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        if (player != null) {
            if (!player.isCrouching()) {
                addPos(stack, context.getClickedPos());
            } else {
                removePos(stack);
                setRaw(stack);
            }
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    @Override
    public ActionResult<ItemStack> use(World pLevel, PlayerEntity player, Hand pHand) {
        if (player.isCrouching() && pHand == Hand.MAIN_HAND) {
            ItemStack stack = player.getItemInHand(pHand);
            removePos(stack);
            setRaw(stack);
        }
        return super.use(pLevel, player, pHand);
    }
}
