package com.lowdragmc.multiblocked.api.pattern;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.api.block.CustomProperties;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.pattern.error.PatternError;
import com.lowdragmc.multiblocked.api.pattern.error.PatternStringError;
import com.lowdragmc.multiblocked.api.pattern.error.SinglePredicateError;
import com.lowdragmc.multiblocked.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.multiblocked.api.pattern.util.PatternMatchContext;
import com.lowdragmc.multiblocked.api.pattern.util.RelativeDirection;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import com.lowdragmc.multiblocked.api.tile.part.IPartComponent;
import com.lowdragmc.multiblocked.client.renderer.impl.CycleBlockStateRenderer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BlockPattern {

    static Direction[] FACINGS = {Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN};
    static Direction[] FACINGS_H = {Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST};
    public final int[][] aisleRepetitions;
    public final RelativeDirection[] structureDir;
    protected final TraceabilityPredicate[][][] blockMatches; //[z][y][x]
    protected final int fingerLength; //z size
    protected final int thumbLength; //y size
    protected final int palmLength; //x size
    protected final int[] centerOffset; // x, y, z, minZ, maxZ

    public BlockPattern(TraceabilityPredicate[][][] predicatesIn, RelativeDirection[] structureDir, int[][] aisleRepetitions, int[] centerOffset) {
        this.blockMatches = predicatesIn;
        this.fingerLength = predicatesIn.length;
        this.structureDir = structureDir;
        this.aisleRepetitions = aisleRepetitions;

        if (this.fingerLength > 0) {
            this.thumbLength = predicatesIn[0].length;

            if (this.thumbLength > 0) {
                this.palmLength = predicatesIn[0][0].length;
            } else {
                this.palmLength = 0;
            }
        } else {
            this.thumbLength = 0;
            this.palmLength = 0;
        }

        this.centerOffset = centerOffset;
    }

    public boolean checkPatternAt(MultiblockState worldState, boolean savePredicate) {
        ControllerTileEntity controller = worldState.getController();
        if (controller == null) {
            worldState.setError(new PatternStringError("no controller found"));
            return false;
        }
        BlockPos centerPos = controller.getBlockPos();
        Direction frontFacing = controller.getFrontFacing();
        Set<MultiblockCapability<?>> inputCapabilities = controller.getDefinition().getRecipeMap().inputCapabilities;
        Set<MultiblockCapability<?>> outputCapabilities = controller.getDefinition().getRecipeMap().outputCapabilities;
        Direction[] facings = controller.getDefinition().properties.rotationState != CustomProperties.RotationState.NONE ? new Direction[]{frontFacing} : new Direction[]{Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};
        for (Direction facing : facings) {
            if (checkPatternAt(worldState, centerPos, facing, savePredicate, inputCapabilities, outputCapabilities)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkPatternAt(MultiblockState worldState, BlockPos centerPos, Direction facing, boolean savePredicate, Set<MultiblockCapability<?>> inputCapabilities, Set<MultiblockCapability<?>> outputCapabilities) {
        boolean findFirstAisle = false;
        int minZ = -centerOffset[4];
        worldState.clean();
        PatternMatchContext matchContext = worldState.matchContext;
        Map<SimplePredicate, Integer> globalCount = worldState.globalCount;
        //Checking aisles
        for (int c = 0, z = minZ++, r; c < this.fingerLength; c++) {
            //Checking repeatable slices
            loop:
            for (r = 0; (findFirstAisle ? r < aisleRepetitions[c][1] : z <= -centerOffset[3]); r++) {
                //Checking single slice
                for (int b = 0, y = -centerOffset[1]; b < this.thumbLength; b++, y++) {
                    for (int a = 0, x = -centerOffset[0]; a < this.palmLength; a++, x++) {
                        worldState.setError(null);
                        TraceabilityPredicate predicate = this.blockMatches[c][b][a];
                        BlockPos pos = setActualRelativeOffset(x, y, z, facing).offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                        if (!worldState.update(pos, predicate)) {
                            return false;
                        }
                        if (!predicate.isAny()) {
                            worldState.addPosCache(pos);
                            if (savePredicate) {
                                worldState.getMatchContext().getOrCreate("predicates", (Supplier<HashMap<BlockPos, TraceabilityPredicate>>) HashMap::new).put(pos, predicate);
                            }
                        }
                        boolean canPartShared = true;
                        BlockEntity tileEntity = worldState.getTileEntity();
                        if (tileEntity instanceof IPartComponent) { // add detected parts
                            if (!predicate.isAny()) {
                                IPartComponent part = (IPartComponent) tileEntity;
                                if (part.isFormed() && !part.canShared() && !part.hasController(worldState.controllerPos)) { // check part can be shared
                                    canPartShared = false;
                                    worldState.setError(new PatternStringError("multiblocked.pattern.error.share"));
                                } else {
                                    worldState.getMatchContext()
                                            .getOrCreate("parts", LongOpenHashSet::new)
                                            .add(worldState.getPos().asLong());
                                }
                            }
                        }
                        if (!predicate.test(worldState) || !canPartShared) { // matching failed
                            if (findFirstAisle) {
                                if (r < aisleRepetitions[c][0]) {//retreat to see if the first aisle can start later
                                    r = c = 0;
                                    z = minZ++;
                                    matchContext.reset();
                                    findFirstAisle = false;
                                }
                            } else {
                                z++;//continue searching for the first aisle
                            }
                            continue loop;
                        }
                        if (tileEntity != null && !predicate.isAny()) {
                            Map<Long, EnumMap<IO, Set<MultiblockCapability<?>>>> capabilities = worldState.getMatchContext().getOrCreate("capabilities", Long2ObjectOpenHashMap::new);
                            if (!capabilities.containsKey(worldState.getPos().asLong()) && worldState.io != null) {
                                // if predicate has no specific capability requirements. we will check abilities of every blocks
                                Set<MultiblockCapability<?>> bothFound = new HashSet<>();
                                for (MultiblockCapability<?> capability : inputCapabilities) { // IN
                                    if (worldState.io == IO.BOTH && outputCapabilities.contains(capability) && capability.isBlockHasCapability(IO.BOTH, tileEntity)) {
                                        bothFound.add(capability);
                                        capabilities.computeIfAbsent(worldState.getPos().asLong(), l-> new EnumMap<>(IO.class))
                                                .computeIfAbsent(IO.BOTH, xx->new HashSet<>())
                                                .add(capability);
                                    } else if (worldState.io != IO.OUT && capability.isBlockHasCapability(IO.IN, tileEntity)) {
                                        capabilities.computeIfAbsent(worldState.getPos().asLong(), l-> new EnumMap<>(IO.class))
                                                .computeIfAbsent(IO.IN, xx->new HashSet<>())
                                                .add(capability);
                                    }
                                }
                                if (worldState.io != IO.IN) {
                                    for (MultiblockCapability<?> capability : outputCapabilities) { // OUT
                                        if (!bothFound.contains(capability) && capability.isBlockHasCapability(IO.OUT, tileEntity)) {
                                            capabilities.computeIfAbsent(worldState.getPos().asLong(), l-> new EnumMap<>(IO.class))
                                                    .computeIfAbsent(IO.OUT, xx->new HashSet<>())
                                                    .add(capability);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                findFirstAisle = true;
                z++;
            }
            //Repetitions out of range
            if (r < aisleRepetitions[c][0] || !worldState.isFormed() || !findFirstAisle) {
                if (worldState.isFormed()) {
                    worldState.setError(new PatternError());
                }
                return false;
            }
        }

        //Check count matches amount
        for (Map.Entry<SimplePredicate, Integer> entry : globalCount.entrySet()) {
            if (entry.getValue() < entry.getKey().minCount) {
                worldState.setError(new SinglePredicateError(entry.getKey(), 1));
                return false;
            }
        }

        worldState.setError(null);
        return true;
    }

    public void autoBuild(Player player, MultiblockState worldState) {
        Level world = player.level;
        int minZ = -centerOffset[4];
        worldState.clean();
        ControllerTileEntity controller = worldState.getController();
        BlockPos centerPos = controller.getBlockPos();
        Direction facing = controller.getFrontFacing();
        Map<SimplePredicate, Integer> cacheGlobal = worldState.globalCount;
        Map<BlockPos, Object> blocks = new HashMap<>();
        blocks.put(centerPos, controller);
        for (int c = 0, z = minZ++, r; c < this.fingerLength; c++) {
            for (r = 0; r < aisleRepetitions[c][0]; r++) {
                for (int b = 0, y = -centerOffset[1]; b < this.thumbLength; b++, y++) {
                    for (int a = 0, x = -centerOffset[0]; a < this.palmLength; a++, x++) {
                        TraceabilityPredicate predicate = this.blockMatches[c][b][a];
                        BlockPos pos = setActualRelativeOffset(x, y, z, facing).offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                        worldState.update(pos, predicate);
                        if (!world.isEmptyBlock(pos)) {
                            blocks.put(pos, world.getBlockState(pos));
                            for (SimplePredicate limit : predicate.limited) {
                                limit.testLimited(worldState);
                            }
                        } else {
                            boolean find = false;
                            BlockInfo[] infos = new BlockInfo[0];
                            for (SimplePredicate limit : predicate.limited) {
                                if (limit.minCount > 0) {
                                    if (!cacheGlobal.containsKey(limit)) {
                                        cacheGlobal.put(limit, 1);
                                    } else if (cacheGlobal.get(limit) < limit.minCount && (limit.maxCount == -1 || cacheGlobal.get(limit) < limit.maxCount)) {
                                        cacheGlobal.put(limit, cacheGlobal.get(limit) + 1);
                                    } else {
                                        continue;
                                    }
                                } else {
                                    continue;
                                }
                                infos = limit.candidates == null ? null : limit.candidates.get();
                                find = true;
                                break;
                            }
                            if (!find) { // no limited
                                for (SimplePredicate limit : predicate.limited) {
                                    if (limit.maxCount != -1 && cacheGlobal.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxCount)
                                        continue;
                                    if (cacheGlobal.containsKey(limit)) {
                                        cacheGlobal.put(limit, cacheGlobal.get(limit) + 1);
                                    } else {
                                        cacheGlobal.put(limit, 1);
                                    }
                                    infos = ArrayUtils.addAll(infos, limit.candidates == null ? null : limit.candidates.get());
                                }
                                for (SimplePredicate common : predicate.common) {
                                    infos = ArrayUtils.addAll(infos, common.candidates == null ? null : common.candidates.get());
                                }
                            }

                            List<ItemStack> candidates = new ArrayList<>();
                            if (infos != null) {
                                for (BlockInfo info : infos) {
                                    if (info.getBlockState().getBlock() != Blocks.AIR) {
                                        BlockState blockState = info.getBlockState();
                                        if (blockState.getBlock() instanceof BlockComponent && ((BlockComponent) blockState.getBlock()).definition != null) {
                                            if (((BlockComponent) blockState.getBlock()).definition.getBaseRenderer() instanceof CycleBlockStateRenderer) {
                                                CycleBlockStateRenderer renderer = (CycleBlockStateRenderer) ((BlockComponent) blockState.getBlock()).definition.getBaseRenderer();
                                                for (BlockInfo blockInfo : renderer.blockInfos) {
                                                    candidates.add(blockInfo.getItemStackForm());
                                                }
                                            } else {
                                                candidates.add(info.getItemStackForm());
                                            }
                                        } else {
                                            candidates.add(info.getItemStackForm());
                                        }
                                    }
                                }
                            }

                            // check inventory
                            ItemStack found = null;
                            if (!player.isCreative()) {
                                for (ItemStack itemStack : player.getInventory().items) {
                                    if (candidates.stream().anyMatch(candidate -> candidate.equals(itemStack, false)) && !itemStack.isEmpty() && itemStack.getItem() instanceof BlockItem) {
                                        found = itemStack.copy();
                                        itemStack.setCount(itemStack.getCount() - 1);
                                        break;
                                    }
                                }
                            } else {
                                for (ItemStack candidate : candidates) {
                                    found = candidate.copy();
                                    if (!found.isEmpty() && found.getItem() instanceof BlockItem) {
                                        break;
                                    }
                                    found = null;
                                }
                            }
                            if (found == null) continue;
                            BlockItem itemBlock = (BlockItem) found.getItem();
                            BlockPlaceContext context = new BlockPlaceContext(world, player, InteractionHand.MAIN_HAND, found, BlockHitResult.miss(player.getEyePosition(0), Direction.UP, pos));
                            itemBlock.place(context);
                            BlockEntity tileEntity = world.getBlockEntity(pos);
                            if (tileEntity instanceof ComponentTileEntity) {
                                blocks.put(pos, tileEntity);
                            } else {
                                blocks.put(pos, world.getBlockState(pos));
                            }
                        }
                    }
                }
                z++;
            }
        }
        Direction frontFacing = controller.getFrontFacing();
        blocks.forEach((pos, block) -> { // adjust facing
            if (block instanceof BlockState) {
                resetFacing(pos, (BlockState) block, frontFacing, (p, f) -> {
                    Object object = blocks.get(p.relative(f));
                    return object == null || (object instanceof BlockState && ((BlockState) object).getBlock() == Blocks.AIR);
                }, state -> world.setBlock(pos, state, 3));
            } else if (block instanceof ComponentTileEntity) {
                resetFacing(pos, ((ComponentTileEntity<?>) block).getBlockState(), frontFacing, (p, f) -> {
                    Object object = blocks.get(p.relative(f));
                    if (object == null || (object instanceof BlockState && ((BlockState) object).getBlock() == Blocks.AIR)) {
                        return ((ComponentTileEntity<?>) block).isValidFrontFacing(f);
                    }
                    return false;
                }, state -> world.setBlock(pos, state, 3));
            }
        });
    }

    public BlockInfo[][][] getPreview(int[] repetition) {
        Map<SimplePredicate, Integer> cacheGlobal = new HashMap<>();
        Map<BlockPos, BlockInfo> blocks = new HashMap<>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int l = 0, x = 0; l < this.fingerLength; l++) {
            for (int r = 0; r < repetition[l]; r++) {
                //Checking single slice
                for (int y = 0; y < this.thumbLength; y++) {
                    for (int z = 0; z < this.palmLength; z++) {
                        TraceabilityPredicate predicate = this.blockMatches[l][y][z];
                        boolean find = false;
                        BlockInfo[] infos = null;
                        // check global and previewCount
                        for (SimplePredicate limit : predicate.limited) {
                            if (limit.minCount == -1 && limit.previewCount == -1) continue;
                            if (cacheGlobal.getOrDefault(limit, 0) < limit.previewCount) {
                                if (!cacheGlobal.containsKey(limit)) {
                                    cacheGlobal.put(limit, 1);
                                } else if (cacheGlobal.get(limit) < limit.previewCount) {
                                    cacheGlobal.put(limit, cacheGlobal.get(limit) + 1);
                                } else {
                                    continue;
                                }
                            } else if (limit.minCount > 0) {
                                if (!cacheGlobal.containsKey(limit)) {
                                    cacheGlobal.put(limit, 1);
                                } else if (cacheGlobal.get(limit) < limit.minCount) {
                                    cacheGlobal.put(limit, cacheGlobal.get(limit) + 1);
                                } else {
                                    continue;
                                }
                            } else {
                                continue;
                            }
                            infos = limit.candidates == null ? null : limit.candidates.get();
                            find = true;
                            break;
                        }
                        if (!find) { // check common with previewCount
                            for (SimplePredicate common : predicate.common) {
                                if (common.previewCount > 0) {
                                    if (!cacheGlobal.containsKey(common)) {
                                        cacheGlobal.put(common, 1);
                                    } else if (cacheGlobal.get(common) < common.previewCount) {
                                        cacheGlobal.put(common, cacheGlobal.get(common) + 1);
                                    } else {
                                        continue;
                                    }
                                } else {
                                    continue;
                                }
                                infos = common.candidates == null ? null : common.candidates.get();
                                find = true;
                                break;
                            }
                        }
                        if (!find) { // check without previewCount
                            for (SimplePredicate common : predicate.common) {
                                if (common.previewCount == -1) {
                                    infos = common.candidates == null ? null : common.candidates.get();
                                    find = true;
                                    break;
                                }
                            }
                        }
                        if (!find) { // check max
                            for (SimplePredicate limit : predicate.limited) {
                                if (limit.previewCount != -1) {
                                    continue;
                                } else if (limit.maxCount != -1) {
                                    if (cacheGlobal.getOrDefault(limit, 0) < limit.maxCount) {
                                        if (!cacheGlobal.containsKey(limit)) {
                                            cacheGlobal.put(limit, 1);
                                        } else {
                                            cacheGlobal.put(limit, cacheGlobal.get(limit) + 1);
                                        }
                                    } else {
                                        continue;
                                    }
                                }

                                infos = limit.candidates == null ? null : limit.candidates.get();
                                break;
                            }
                        }
                        BlockInfo info = infos == null || infos.length == 0 ? BlockInfo.EMPTY : infos[0];
                        BlockPos pos = setActualRelativeOffset(z, y, x, Direction.NORTH);

                        blocks.put(pos, info);
                        minX = Math.min(pos.getX(), minX);
                        minY = Math.min(pos.getY(), minY);
                        minZ = Math.min(pos.getZ(), minZ);
                        maxX = Math.max(pos.getX(), maxX);
                        maxY = Math.max(pos.getY(), maxY);
                        maxZ = Math.max(pos.getZ(), maxZ);
                    }
                }
                x++;
            }
        }
        BlockInfo[][][] result = (BlockInfo[][][]) Array.newInstance(BlockInfo.class, maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
        int finalMinX = minX;
        int finalMinY = minY;
        int finalMinZ = minZ;
        blocks.forEach((pos, info) -> {
            resetFacing(pos, info.getBlockState(), null, (p, f) -> {
                BlockInfo blockInfo = blocks.get(p.relative(f));
                if (blockInfo == null || blockInfo.getBlockState().getBlock() == Blocks.AIR) {
                    if (blocks.get(pos).getBlockState().getBlock() instanceof BlockComponent component) {
                        if (component.definition instanceof ControllerDefinition) {
                            return false;
                        } else {
                            BlockEntity te = component.newBlockEntity(BlockPos.ZERO, component.defaultBlockState());
                            if (te instanceof ComponentTileEntity) {
                                return ((ComponentTileEntity<?>) te).isValidFrontFacing(f);
                            }
                        }
                    }
                    return true;
                }
                return false;
            }, info::setBlockState);
            result[pos.getX() - finalMinX][pos.getY() - finalMinY][pos.getZ() - finalMinZ] = info;
        });
        return result;
    }

    private void resetFacing(BlockPos pos, BlockState blockState, Direction facing, BiFunction<BlockPos, Direction, Boolean> checker, Consumer<BlockState> consumer) {
        if (blockState.hasProperty(BlockStateProperties.FACING)) {
            tryFacings(blockState, pos, checker, consumer, BlockStateProperties.FACING, facing == null ? FACINGS : ArrayUtils.add(FACINGS, facing));
        } else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            tryFacings(blockState, pos, checker, consumer, BlockStateProperties.HORIZONTAL_FACING, facing == null || facing.getAxis() == Direction.Axis.Y ? FACINGS_H : ArrayUtils.add(FACINGS_H, facing));
        }
    }

    private void tryFacings(BlockState blockState, BlockPos pos, BiFunction<BlockPos, Direction, Boolean> checker, Consumer<BlockState> consumer, Property<Direction> property, Direction[] facings) {
        Direction found = null;
        for (Direction facing : facings) {
            if (checker.apply(pos, facing)) {
                found = facing;
                break;
            }
        }
        if (found == null) {
            found = Direction.NORTH;
        }
        consumer.accept(blockState.setValue(property, found));
    }

    private BlockPos setActualRelativeOffset(int x, int y, int z, Direction facing) {
        int[] c0 = new int[]{x, y, z}, c1 = new int[3];
        for (int i = 0; i < 3; i++) {
            switch (structureDir[i].getActualFacing(facing)) {
                case UP -> c1[1] = c0[i];
                case DOWN -> c1[1] = -c0[i];
                case WEST -> c1[0] = -c0[i];
                case EAST -> c1[0] = c0[i];
                case NORTH -> c1[2] = -c0[i];
                case SOUTH -> c1[2] = c0[i];
            }
        }
        return new BlockPos(c1[0], c1[1], c1[2]);
    }
}
