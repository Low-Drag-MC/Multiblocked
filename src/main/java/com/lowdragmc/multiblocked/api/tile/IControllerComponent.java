package com.lowdragmc.multiblocked.api.tile;

import com.lowdragmc.multiblocked.api.capability.ICapabilityProxyHolder;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.pattern.BlockPattern;
import com.lowdragmc.multiblocked.api.pattern.MultiblockState;
import com.lowdragmc.multiblocked.api.pattern.error.PatternError;
import com.lowdragmc.multiblocked.api.pattern.error.PatternStringError;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.Util.NIL_UUID;

/**
 * @author KilaBash
 * @date 2022/06/02
 * @implNote IControllerComponent
 */
public interface IControllerComponent extends IComponent, ICapabilityProxyHolder {

    @Override
    ControllerDefinition getDefinition();

    @Override
    default boolean isFormed() {
        return getMultiblockState() != null && getMultiblockState().isFormed();
    }

    @Override
    default boolean isValidFrontFacing(Direction up) {
        return IComponent.super.isValidFrontFacing(up) && up.getAxis() != Direction.Axis.Y;
    }

    default boolean checkPattern() {
        if (getMultiblockState() == null) return false;
        BlockPattern pattern = getPattern();
        return pattern != null && pattern.checkPatternAt(getMultiblockState(), false);
    }

    void onStructureFormed();

    void onStructureInvalid();

    MultiblockState getMultiblockState();

    void setMultiblockState(MultiblockState multiblockState);

    /**
     * is this machine formed without controller
     */
    default boolean hasOldBlock() {
        return false;
    }

    /**
     * back to the old block
     */
    default void resetOldBlock(Level world, BlockPos controllerPos) {

    }

    /**
     * save the old block when formed
     */
    default void saveOldBlock(BlockState oldState, CompoundTag oldNbt) {

    }

    default boolean checkCatalystPattern(Player player, InteractionHand hand, ItemStack held) {
        if (checkPattern()) { // formed
            if (!player.isCreative() && !getDefinition().consumeCatalyst.test(held)) {
                getMultiblockState().setError(new PatternStringError("catalyst failed"));
                return false;
            }
            player.swing(hand);
            Component formedMsg = new TranslatableComponent(getUnlocalizedName()).append(new TranslatableComponent("multiblocked.multiblock.formed"));
            player.sendMessage(formedMsg, NIL_UUID);
            MultiblockWorldSavedData.getOrCreate(self().getLevel()).addMapping(getMultiblockState());
            if (!needAlwaysUpdate()) {
                MultiblockWorldSavedData.getOrCreate(self().getLevel()).addLoading(this);
            }
            onStructureFormed();
            return true;
        }
        return false;
    }

    default RecipeLogic getRecipeLogic() {
        return null;
    }

    default BlockPattern getPattern() {
        return getDefinition().getBasePattern();
    }
}
