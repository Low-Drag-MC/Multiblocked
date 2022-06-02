package com.lowdragmc.multiblocked.api.block;

import com.lowdragmc.lowdraglib.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.tile.IComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

/**
 * Author: KilaBash
 * Date: 2022/04/23
 * Description:
 */
@ParametersAreNonnullByDefault
public class BlockComponent extends Block implements IBlockRendererProvider, EntityBlock {
    public ComponentDefinition definition;

    public BlockComponent(ComponentDefinition definition) {
        super(definition.getBlockProperties());
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        this.setRegistryName(definition.location);
        this.definition = definition;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(BlockStateProperties.FACING);
    }

    public IComponent getComponent(BlockGetter world, BlockPos pos) {
        BlockEntity instance = world.getBlockEntity(pos);
        return instance instanceof IComponent ? ((IComponent) instance) : null;
    }

    @Nonnull
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        IComponent instance = getComponent(world, pos);
        if (instance != null) {
            return instance.use(player, hand, hit);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        IComponent instance = getComponent(level, pos);
        if (instance != null) {
            instance.onNeighborChange();
        }
    }

    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pPos, @Nonnull BlockState pState, @Nullable LivingEntity placer, @Nonnull ItemStack pStack) {
        IComponent componentTileEntity = getComponent(level, pPos);
        if (componentTileEntity != null && placer != null) {
            Vec3 pos = placer.position();
            if (placer instanceof Player) {
                componentTileEntity.setOwner(placer.getUUID());
            }
            if (Math.abs(pos.x - (double)((float)pPos.getX() + 0.5F)) < 2.0D && Math.abs(pos.z - (double)((float)pPos.getZ() + 0.5F)) < 2.0D) {
                double d0 = pos.y + (double)placer.getEyeHeight();
                if (d0 - (double)pPos.getY() > 2.0D && componentTileEntity.isValidFrontFacing(Direction.UP)) {
                    componentTileEntity.setFrontFacing(Direction.UP);
                    return;
                }
                if ((double)pPos.getY() - d0 > 0.0D && componentTileEntity.isValidFrontFacing(Direction.DOWN)) {
                    componentTileEntity.setFrontFacing(Direction.DOWN);
                    return;
                }
            }
            componentTileEntity.setFrontFacing(placer.getDirection().getOpposite());
        }
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        IComponent instance = getComponent(level, pos);
        if (instance != null) {
            instance.rotateTo(direction);
        }
        return state;
    }

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootContext.Builder pBuilder) {
        LootContext context = pBuilder.withParameter(LootContextParams.BLOCK_STATE, pState).create(LootContextParamSets.BLOCK);
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        BlockEntity tileEntity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (tileEntity instanceof IComponent && entity instanceof Player) {
            NonNullList<ItemStack> drops = NonNullList.create();
            ((IComponent) tileEntity).onDrops(drops, (Player) entity);
            return drops;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) {
        IComponent instance = getComponent(world, pos);
        return instance != null && instance.canConnectRedstone(side == null ? null : side.getOpposite());
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean propagatesSkylightDown(BlockState state, BlockGetter pReader, BlockPos pPos) {
        return !state.canOcclude();
    }

    @Override
    public IRenderer getRenderer(BlockState state, BlockPos pos, BlockAndTintGetter blockReader) {
        BlockEntity tileEntity = blockReader.getBlockEntity(pos);
        if (tileEntity instanceof IComponent) {
            return ((IComponent) tileEntity).getRenderer();
        }
        return null;
    }

    @Override
    public int getLightingMap(BlockAndTintGetter world, BlockState state, BlockPos pos) {
        if (state.emissiveRendering(world, pos)) {
            return 15728880; // 15 << 20 | 15 << 4
        } else {
            int j = world.getBrightness(LightLayer.BLOCK, pos);
            int k = state.getLightEmission(world, pos);
            if (j < k) {
                j = k;
            }
            return 15 << 20 | j << 4; // 15 << 20
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return definition.createNewTileEntity(pPos, pState);
    }

}