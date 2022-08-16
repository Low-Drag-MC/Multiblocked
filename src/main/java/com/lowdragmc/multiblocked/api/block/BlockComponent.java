package com.lowdragmc.multiblocked.api.block;

import com.lowdragmc.lowdraglib.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.tile.IComponent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootParameters;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

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
public class BlockComponent extends Block implements IBlockRendererProvider {
    public ComponentDefinition definition;

    public BlockComponent(ComponentDefinition definition) {
        super(definition.getBlockProperties());
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        this.setRegistryName(definition.location);
        this.definition = definition;
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(BlockStateProperties.FACING);
    }

    public IComponent getComponent(IBlockReader world, BlockPos pos) {
        TileEntity instance = world.getBlockEntity(pos);
        return instance instanceof IComponent ? ((IComponent) instance) : null;
    }

    @Nonnull
    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        IComponent instance = getComponent(world, pos);
        if (instance != null) {
            return instance.use(player, hand, hit);
        }
        return ActionResultType.PASS;
    }

    @Override
    public void neighborChanged(BlockState pState, World pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        super.neighborChanged(pState, pLevel, pPos, pBlock, pFromPos, pIsMoving);
        IComponent instance = getComponent(pLevel, pPos);
        if (instance != null) {
            instance.onNeighborChange();
        }
    }

    @Override
    public void setPlacedBy(@Nonnull World level, @Nonnull BlockPos pPos, @Nonnull BlockState pState, @Nullable LivingEntity placer, @Nonnull ItemStack pStack) {
        IComponent component = getComponent(level, pPos);
        if (component != null && placer != null) {
            Vector3d pos = placer.position();
            if (placer instanceof PlayerEntity) {
                component.setOwner(placer.getUUID());
            }
            if (Math.abs(pos.x - (double)((float)pPos.getX() + 0.5F)) < 2.0D && Math.abs(pos.z - (double)((float)pPos.getZ() + 0.5F)) < 2.0D) {
                double d0 = pos.y + (double)placer.getEyeHeight();
                if (d0 - (double)pPos.getY() > 2.0D && component.isValidFrontFacing(Direction.UP)) {
                    component.setFrontFacing(Direction.UP);
                    return;
                }
                if ((double)pPos.getY() - d0 > 0.0D && component.isValidFrontFacing(Direction.DOWN)) {
                    component.setFrontFacing(Direction.DOWN);
                    return;
                }
            }
            if (definition.properties.rotationState == CustomProperties.RotationState.Y_AXIS) {
                component.setFrontFacing(Direction.UP);
            } else {
                component.setFrontFacing(placer.getDirection().getOpposite());
            }
        }
    }

    @Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction) {
        IComponent instance = getComponent(world, pos);
        if (instance != null) {
            instance.rotateTo(direction);
        }
        return state;
    }

    @Override
    public void destroy(IWorld level, BlockPos pPos, BlockState pState) {
        super.destroy(level, pPos, pState);
    }

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootContext.Builder pBuilder) {
        LootContext context = pBuilder.withParameter(LootParameters.BLOCK_STATE, pState).create(LootParameterSets.BLOCK);
        Entity entity = context.getParamOrNull(LootParameters.THIS_ENTITY);
        TileEntity tileEntity = context.getParamOrNull(LootParameters.BLOCK_ENTITY);
        if (tileEntity instanceof IComponent && entity instanceof PlayerEntity) {
            NonNullList<ItemStack> drops = NonNullList.create();
            ((IComponent) tileEntity).onDrops(drops, (PlayerEntity) entity);
            return drops;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side) {
        IComponent instance = getComponent(world, pos);
        return instance != null && instance.canConnectRedstone(side == null ? null : side.getOpposite());
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean propagatesSkylightDown(BlockState state, IBlockReader pReader, BlockPos pPos) {
        return !state.canOcclude();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return definition.createNewTileEntity();
    }

    @Override
    public IRenderer getRenderer(BlockState state, BlockPos pos, IBlockDisplayReader blockReader) {
        TileEntity tileEntity = blockReader.getBlockEntity(pos);
        if (tileEntity instanceof IComponent) {
            return ((IComponent) tileEntity).getRenderer();
        }
        return null;
    }

    @Override
    public int getLightingMap(IBlockDisplayReader world, BlockState state, BlockPos pos) {
        if (state.emissiveRendering(world, pos)) {
            return 15728880; // 15 << 20 | 15 << 4
        } else {
            int j = world.getBrightness(LightType.BLOCK, pos);
            int k = state.getLightValue(world, pos);
            if (j < k) {
                j = k;
            }
            return 15 << 20 | j << 4; // 15 << 20
        }
    }

    @Override
    public VoxelShape getShape(BlockState pState, IBlockReader pLevel, BlockPos pPos, ISelectionContext pContext) {
        if (hasDynamicShape()) {
            TileEntity tileEntity = pLevel.getBlockEntity(pPos);
            if (tileEntity instanceof IComponent) {
                return ((IComponent) tileEntity).getDynamicShape();
            }
        }
        return definition.getBaseStatus().getShape(pState.getValue(BlockStateProperties.FACING));
    }

    @Override
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
        TileEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof IComponent) {
            IComponent component = (IComponent) tileEntity;
            return component.getDefinition().getStatus(component.getStatus()).getLightEmissive();
        } else {
            return super.getLightValue(state, world, pos);
        }
    }
}
