package com.lowdragmc.multiblocked.api.block;

import com.lowdragmc.lowdraglib.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Author: KilaBash
 * Date: 2022/04/23
 * Description:
 */
public class BlockComponent extends Block implements IBlockRendererProvider {
    public ComponentDefinition definition;

    public BlockComponent(ComponentDefinition definition) {
        super(definition.properties.createBlock());
        this.setRegistryName(definition.location);
        this.definition = definition;
    }

    public ComponentTileEntity<?> getComponent(IBlockReader world, BlockPos pos) {
        TileEntity instance = world.getBlockEntity(pos);
        return instance instanceof ComponentTileEntity<?> ? ((ComponentTileEntity<?>) instance) : null;
    }

    @Nonnull
    @Override
    @ParametersAreNonnullByDefault
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ComponentTileEntity<?> instance = getComponent(world, pos);
        if (instance != null) {
            return instance.use(player, hand, hit);
        }
        return ActionResultType.PASS;
    }

    @Override
    public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor) {
        ComponentTileEntity<?> instance = getComponent(world, pos);
        if (instance != null) {
            instance.onNeighborChange();
        }
    }

    @Override
    public void setPlacedBy(@Nonnull World level, @Nonnull BlockPos pPos, @Nonnull BlockState pState, @Nullable LivingEntity placer, @Nonnull ItemStack pStack) {
        ComponentTileEntity<?> componentTileEntity = getComponent(level, pPos);
        if (componentTileEntity != null && placer != null) {
            Vector3d pos = placer.position();
            if (placer instanceof PlayerEntity) {
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
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction) {
        ComponentTileEntity<?> instance = getComponent(world, pos);
        if (instance != null) {
            instance.rotate(direction);
        }
        return state;
    }

    @Override
    public void destroy(IWorld level, BlockPos pPos, BlockState pState) {
        super.destroy(level, pPos, pState);
    }

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootContext.Builder pBuilder) {
        List<ItemStack> drops = super.getDrops(pState, pBuilder);
//        ComponentTileEntity<?> instance = componentBroken.get();
//        if (instance != null) {
//            instance.onDrops(drops, harvesters.get());
//        }
        return drops;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side) {
        ComponentTileEntity<?> instance = getComponent(world, pos);
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
        if (tileEntity instanceof ComponentTileEntity) {
            return ((ComponentTileEntity<?>) tileEntity).getRenderer();
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
}
