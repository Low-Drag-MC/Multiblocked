package com.lowdragmc.multiblocked.api.tile;

import com.lowdragmc.lowdraglib.LDLMod;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.lowdragmc.multiblocked.api.kubejs.events.UpdateRendererEvent;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;


import java.util.UUID;

/**
 * @author KilaBash
 * @date 2022/06/01
 * @implNote IComponent
 */
public interface IComponent {
    default BlockEntity self() {
        return (BlockEntity) this;
    }

    ComponentDefinition getDefinition();

    default InteractionResult use(Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    default void onNeighborChange() {}

    default void setOwner(UUID uuid) {}

    default boolean isValidFrontFacing(Direction up) {
        return getDefinition().properties.rotationState.test(up);
    }

    default void setFrontFacing(Direction facing) {
        Level level = self().getLevel();
        if (level != null && !level.isClientSide) {
            if (!isValidFrontFacing(facing)) return;
            if (self().getBlockState().getValue(BlockStateProperties.FACING) == facing) return;
            level.setBlock(self().getBlockPos(), self().getBlockState().setValue(BlockStateProperties.FACING, facing), 3);
        }
    }

    default String getSubID() {
        return getDefinition().getID();
    }

    default IMultiblockedRenderer updateCurrentRenderer() {
        IMultiblockedRenderer renderer = getDefinition().getStatus(getStatus()).getRenderer();
        if (Multiblocked.isKubeJSLoaded() && self().getLevel() != null) {
            UpdateRendererEvent event = new UpdateRendererEvent(this, renderer);
            event.post(ScriptType.of(self().getLevel()), UpdateRendererEvent.ID, getSubID());
            renderer = event.getRenderer();
        }
        return renderer;
    }

    default Direction getFrontFacing() {
        return self().getBlockState().getValue(BlockStateProperties.FACING);
    }

    default void rotateTo(Rotation direction) {
        setFrontFacing(direction.rotate(getFrontFacing()));
    }

    default void onDrops(NonNullList<ItemStack> drops, Player entity) {
        drops.add(getDefinition().getStackForm());
    }

    default boolean canConnectRedstone(Direction direction) {
        return false;
    }

    default void update() {

    }

    default String getUnlocalizedName() {
        return "block." + getDefinition().getID();
    }

    IRenderer getRenderer();

    boolean isFormed();

    void setRendererObject(Object o);

    Object getRendererObject();

    String getStatus();

    void setStatus(String status);

    default VoxelShape getDynamicShape() {
        return getDefinition().getStatus(getStatus()).getShape(getFrontFacing());
    }

    default boolean needAlwaysUpdate() {
        Level level = self().getLevel();
        return level != null && !level.isClientSide && (getDefinition().needUpdateTick() );
    }

    default void markAsDirty() {
        self().setChanged();
    }

    default boolean isRemote() {
        Level level = self().getLevel();
        return level == null ? LDLMod.isRemote() : level.isClientSide;
    }
}
